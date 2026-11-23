package dev.diyscan.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diyscan.ui.theme.MonoFontFamily
import dev.diyscan.ui.theme.Palette
import dev.diyscan.ui.theme.TelemetryReadoutStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * Shared building blocks, drawn in the Gemini "instrument" style (see design/gemini-mockup):
 * flat plates with a hairline bezel, signal-orange accent, mono tabular readouts.
 * Names and parameters are kept stable so every screen picks up the look unchanged.
 */

val PanelShape = InstrumentShape

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier
        .clip(PanelShape)
        .background(Palette.Panel)
        .border(1.dp, accent?.copy(alpha = 0.7f) ?: Palette.Line, PanelShape)
        .let { if (onClick != null) it.clickable(onClick = onClick) else it }
        .padding(padding)
    Column(m, content = content)
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, color: Color = Palette.TextMuted) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelMedium, color = color, modifier = modifier)
}

@Composable
fun Pill(text: String, fg: Color, bg: Color, modifier: Modifier = Modifier, dot: Boolean = true) {
    HardwarePill(text = text, fg = fg, bg = bg, modifier = modifier, showDot = dot)
}

private val ButtonShape = RoundedCornerShape(12.dp)

/** Primary call-to-action in the accent color. */
@Composable
fun AmberButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val bg = if (enabled) Palette.Amber else Palette.Well
    val fg = if (enabled) Palette.OnAccent else Palette.TextFaint
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(ButtonShape)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Secondary outlined button. */
@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color = Palette.Text, enabled: Boolean = true, onClick: () -> Unit) {
    val fg = if (enabled) tint else Palette.TextFaint
    Row(
        modifier
            .heightIn(min = 48.dp)
            .clip(ButtonShape)
            .background(Palette.Panel)
            .border(1.dp, if (enabled && tint != Palette.Text) tint.copy(alpha = 0.6f) else Palette.Line, ButtonShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Precision arc gauge: 260° sweep with graduated ticks, a redline band, the value arc and a needle tip.
 * [fraction] is 0..1 of the range; [redFrom] is where the red band starts (0..1).
 */
@Composable
fun ArcGauge(
    fraction: Float,
    valueText: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    redFrom: Float? = null,
    code: String? = null,
) {
    val f by animateFloatAsState(
        fraction.coerceIn(0f, 1f),
        spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "gauge",
    )
    val inRed = redFrom != null && f >= redFrom
    val color = if (inRed) Palette.Fault else Palette.Amber
    val track = Palette.Track
    val fault = Palette.Fault
    val major = Palette.TextMuted.copy(alpha = 0.65f)
    val minor = Palette.TextFaint.copy(alpha = 0.35f)
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Palette.TextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (code != null) Text(code, style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint)
        }
        Box(Modifier.fillMaxWidth().aspectRatio(1.18f), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val strokeW = size.minDimension * 0.085f
                val pad = strokeW * 1.4f
                val d = size.minDimension - pad * 2f
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f + size.minDimension * 0.06f)
                val sz = Size(d, d)
                val c = Offset(tl.x + d / 2f, tl.y + d / 2f)
                val r = d / 2f
                val start = 140f
                val sweep = 260f
                drawArc(track, start, sweep, false, tl, sz, style = Stroke(strokeW, cap = StrokeCap.Butt))
                if (redFrom != null) {
                    drawArc(fault.copy(alpha = 0.35f), start + sweep * redFrom, sweep * (1f - redFrom), false, tl, sz,
                        style = Stroke(strokeW, cap = StrokeCap.Butt))
                }
                drawArc(color, start, sweep * f, false, tl, sz, style = Stroke(strokeW, cap = StrokeCap.Butt))
                for (i in 0..24) {
                    val deg = start + sweep * i / 24f
                    val rad = deg * PI / 180.0
                    val isMajor = i % 4 == 0
                    val outer = r - strokeW * 0.65f
                    val inner = outer - if (isMajor) strokeW * 0.95f else strokeW * 0.45f
                    drawLine(
                        if (isMajor) major else minor,
                        Offset((c.x + cos(rad) * outer).toFloat(), (c.y + sin(rad) * outer).toFloat()),
                        Offset((c.x + cos(rad) * inner).toFloat(), (c.y + sin(rad) * inner).toFloat()),
                        strokeWidth = if (isMajor) 2.2f else 1.2f, cap = StrokeCap.Round,
                    )
                }
                val nRad = (start + sweep * f) * PI / 180.0
                drawLine(
                    color,
                    Offset((c.x + cos(nRad) * (r - strokeW * 1.45f)).toFloat(), (c.y + sin(nRad) * (r - strokeW * 1.45f)).toFloat()),
                    Offset((c.x + cos(nRad) * (r + strokeW * 0.42f)).toFloat(), (c.y + sin(nRad) * (r + strokeW * 0.42f)).toFloat()),
                    strokeWidth = 3.5f, cap = StrokeCap.Round,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 14.dp)) {
                Text(valueText, style = TelemetryReadoutStyle.copy(fontSize = 26.sp, letterSpacing = (-0.8).sp),
                    color = if (inRed) Palette.Fault else Palette.Text, maxLines = 1)
                Text(unit.uppercase(), style = MaterialTheme.typography.labelSmall, color = Palette.TextMuted)
            }
        }
    }
}

/** Small line chart with a fading fill and a baseline hairline. */
@Composable
fun Sparkline(values: List<Float>, min: Float, max: Float, modifier: Modifier = Modifier, color: Color = Palette.Amber) {
    val base = Palette.Line.copy(alpha = 0.65f)
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val lo = minOf(min, values.min())
        val hi = maxOf(max, values.max())
        val span = (hi - lo).coerceAtLeast(0.01f)
        val line = Path()
        val fill = Path()
        values.forEachIndexed { i, v ->
            val x = i.toFloat() / (values.size - 1) * size.width
            val y = size.height - ((v - lo) / span).coerceIn(0f, 1f) * size.height * 0.85f - size.height * 0.08f
            if (i == 0) { line.moveTo(x, y); fill.moveTo(x, size.height); fill.lineTo(x, y) }
            else { line.lineTo(x, y); fill.lineTo(x, y) }
        }
        fill.lineTo(size.width, size.height); fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.24f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.4f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(base, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1f)
    }
}

/** Sensor tile: label + PID, big mono readout, sparkline. Imported (profile) sensors use cobalt. */
@Composable
fun SensorTile(
    label: String,
    valueText: String,
    unit: String,
    history: List<Float>,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
    code: String? = null,
    custom: Boolean = false,
) {
    val accent = if (custom) Palette.Info else Palette.Amber
    Panel(modifier, padding = 14.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), style = MaterialTheme.typography.labelMedium,
                color = if (custom) Palette.Info else Palette.TextMuted,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            if (code != null) Text(code, style = MaterialTheme.typography.labelSmall, color = Palette.TextFaint, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(valueText, style = TelemetryReadoutStyle.copy(fontSize = 24.sp), color = Palette.Text, maxLines = 1)
            Spacer(Modifier.width(5.dp))
            Text(unit, style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily),
                color = Palette.TextMuted, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(10.dp))
        Sparkline(history, min, max, Modifier.fillMaxWidth().height(34.dp), color = accent)
    }
}

/** A small round status glyph. */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}

/** Mono label style used for PID codes, hex bytes and VINs. */
@Composable
fun monoSmall() = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp)
