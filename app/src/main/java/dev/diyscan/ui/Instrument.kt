package dev.diyscan.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diyscan.ui.theme.LocalInstrumentColors
import dev.diyscan.ui.theme.MonoFontFamily
import dev.diyscan.ui.theme.TelemetryReadoutStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val InstrumentShape = RoundedCornerShape(16.dp)

@Composable
fun InstrumentCard(
    modifier: Modifier = Modifier,
    accentBorder: Color? = null,
    padding: Dp = 16.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = LocalInstrumentColors.current
    Surface(
        modifier = modifier
            .clip(InstrumentShape)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        shape = InstrumentShape,
        color = c.plate,
        border = BorderStroke(1.dp, accentBorder ?: c.bezel),
        tonalElevation = 0.dp,
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
fun SectionMicroHeader(
    title: String,
    modifier: Modifier = Modifier,
    badge: String? = null,
) {
    val c = LocalInstrumentColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = c.inkMuted,
        )
        if (badge != null) {
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = c.inkFaint,
            )
        }
    }
}

@Composable
fun HardwarePill(
    text: String,
    fg: Color,
    bg: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showDot) {
            Box(
                Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 0.4.sp,
            ),
            color = fg,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "segment",
) {
    val c = LocalInstrumentColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.well)
            .border(1.dp, c.bezel, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { idx, label ->
            val selected = idx == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (selected) c.plate else Color.Transparent)
                    .let {
                        if (selected) it.border(1.dp, c.bezel, RoundedCornerShape(9.dp)) else it
                    }
                    .clickable { onSelect(idx) }
                    .padding(vertical = 10.dp, horizontal = 6.dp)
                    .testTag("${testTagPrefix}_$idx"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    ),
                    color = if (selected) c.ink else c.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun PrimaryInstrumentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color? = null,
    testTag: String? = null,
) {
    val c = LocalInstrumentColors.current
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: c.accent,
            contentColor = Color.White,
        ),
        modifier = modifier
            .height(48.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun OutlineInstrumentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color? = null,
    testTag: String? = null,
) {
    val c = LocalInstrumentColors.current
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accentColor ?: c.bezel),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accentColor ?: c.ink,
        ),
        modifier = modifier
            .height(48.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Braun / Weissach-inspired precision radial arc gauge with graduated ticks,
 * redline hazard arc, sweeping value arc, and physical needle indicator.
 */
@Composable
fun PrecisionArcGauge(
    label: String,
    pidCode: String,
    value: Double,
    formattedValue: String,
    unit: String,
    min: Double,
    max: Double,
    redlineStart: Double? = null,
    accentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    val activeAccent = accentColor ?: c.accent
    val rawFrac = ((value - min) / (max - min).coerceAtLeast(0.001)).toFloat().coerceIn(0f, 1f)
    val animatedFrac by animateFloatAsState(
        targetValue = rawFrac,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "gauge_sweep",
    )
    val isInRedline = redlineStart != null && value >= redlineStart
    val displayColor = if (isInRedline) c.fault else activeAccent

    InstrumentCard(modifier = modifier, padding = 14.dp) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = c.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = pidCode,
                style = MaterialTheme.typography.labelSmall,
                color = c.inkFaint,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.18f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeW = size.minDimension * 0.085f
                val pad = strokeW * 1.4f
                val diameter = size.minDimension - pad * 2f
                val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f + size.minDimension * 0.06f)
                val arcSize = Size(diameter, diameter)
                val center = Offset(topLeft.x + diameter / 2f, topLeft.y + diameter / 2f)
                val radius = diameter / 2f

                val startAngle = 140f
                val sweepTotal = 260f

                // Track arc
                drawArc(
                    color = c.gaugeTrack,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                )

                // Redline hazard band
                if (redlineStart != null && redlineStart < max) {
                    val redFrac = ((redlineStart - min) / (max - min)).toFloat().coerceIn(0f, 1f)
                    drawArc(
                        color = c.fault.copy(alpha = 0.35f),
                        startAngle = startAngle + sweepTotal * redFrac,
                        sweepAngle = sweepTotal * (1f - redFrac),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                    )
                }

                // Active sweep arc
                drawArc(
                    color = displayColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * animatedFrac,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt),
                )

                // Precision tick marks around the inner perimeter
                val tickCount = 24
                for (i in 0..tickCount) {
                    val frac = i.toFloat() / tickCount
                    val deg = startAngle + sweepTotal * frac
                    val rad = deg * PI / 180.0
                    val isMajor = i % 4 == 0
                    val outerR = radius - strokeW * 0.65f
                    val innerR = outerR - if (isMajor) strokeW * 0.95f else strokeW * 0.45f
                    val p1 = Offset(
                        (center.x + cos(rad) * outerR).toFloat(),
                        (center.y + sin(rad) * outerR).toFloat(),
                    )
                    val p2 = Offset(
                        (center.x + cos(rad) * innerR).toFloat(),
                        (center.y + sin(rad) * innerR).toFloat(),
                    )
                    drawLine(
                        color = if (isMajor) c.inkMuted.copy(alpha = 0.65f) else c.inkFaint.copy(alpha = 0.35f),
                        start = p1,
                        end = p2,
                        strokeWidth = if (isMajor) 2.2f else 1.2f,
                        cap = StrokeCap.Round,
                    )
                }

                // Precision pointer tip along the outer arc
                val needleDeg = startAngle + sweepTotal * animatedFrac
                val needleRad = needleDeg * PI / 180.0
                val needleStartR = radius - strokeW * 1.45f
                val needleEndR = radius + strokeW * 0.42f
                drawLine(
                    color = displayColor,
                    start = Offset(
                        (center.x + cos(needleRad) * needleStartR).toFloat(),
                        (center.y + sin(needleRad) * needleStartR).toFloat(),
                    ),
                    end = Offset(
                        (center.x + cos(needleRad) * needleEndR).toFloat(),
                        (center.y + sin(needleRad) * needleEndR).toFloat(),
                    ),
                    strokeWidth = 3.5f,
                    cap = StrokeCap.Round,
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 14.dp),
            ) {
                Text(
                    text = formattedValue,
                    style = TelemetryReadoutStyle.copy(fontSize = 26.sp, letterSpacing = (-0.8).sp),
                    color = if (isInRedline) c.fault else c.ink,
                    maxLines = 1,
                )
                Text(
                    text = unit.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.inkMuted,
                )
            }
        }
    }
}

@Composable
fun TelemetrySparklineCard(
    shortName: String,
    pidCode: String,
    formattedValue: String,
    unit: String,
    history: List<Double>,
    minBound: Double,
    maxBound: Double,
    isCustom: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val c = LocalInstrumentColors.current
    val strokeColor = if (isCustom) c.bus else c.accent

    InstrumentCard(modifier = modifier, padding = 14.dp) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = shortName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = if (isCustom) c.bus else c.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = pidCode,
                style = MaterialTheme.typography.labelSmall,
                color = c.inkFaint,
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = formattedValue,
                style = TelemetryReadoutStyle.copy(fontSize = 24.sp),
                color = c.ink,
                maxLines = 1,
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = c.inkMuted,
                modifier = Modifier.padding(bottom = 3.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
        ) {
            if (history.size < 2) return@Canvas
            val localMin = minOf(minBound, history.min())
            val localMax = maxOf(maxBound, history.max())
            val span = (localMax - localMin).coerceAtLeast(0.01)

            val linePath = Path()
            val fillPath = Path()

            history.forEachIndexed { idx, value ->
                val x = (idx.toFloat() / (history.size - 1)) * size.width
                val normY = ((value - localMin) / span).toFloat().coerceIn(0f, 1f)
                val y = size.height - (normY * size.height * 0.85f) - size.height * 0.08f
                if (idx == 0) {
                    linePath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(strokeColor.copy(alpha = 0.24f), Color.Transparent),
                ),
            )
            drawPath(
                path = linePath,
                color = strokeColor,
                style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Baseline hairline
            drawLine(
                color = c.bezel.copy(alpha = 0.65f),
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1f,
            )
        }
    }
}
