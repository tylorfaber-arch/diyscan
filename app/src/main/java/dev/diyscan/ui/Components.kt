package dev.diyscan.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.diyscan.ui.theme.Numeric
import dev.diyscan.ui.theme.Palette
import kotlin.math.cos
import kotlin.math.sin

val PanelShape = RoundedCornerShape(18.dp)

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val m = modifier
        .clip(PanelShape)
        .background(
            Brush.verticalGradient(listOf(Palette.PanelHigh, Palette.Panel))
        )
        .border(1.dp, accent?.copy(alpha = 0.45f) ?: Palette.Line, PanelShape)
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
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(fg))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Big amber call-to-action. */
@Composable
fun AmberButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (enabled) Brush.verticalGradient(listOf(Color(0xFFFFC24D), Palette.Amber))
                else Brush.verticalGradient(listOf(Palette.Track, Palette.Track))
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (enabled) Color(0xFF1C1300) else Palette.TextFaint
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

/** Quiet outlined button. */
@Composable
fun GhostButton(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color = Palette.Text, enabled: Boolean = true, onClick: () -> Unit) {
    Row(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, Palette.Line, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        val fg = if (enabled) tint else Palette.TextFaint
        if (icon != null) { Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge)
    }
}

/**
 * Arc gauge — 240° sweep with tick marks, an amber value arc with a soft glow,
 * and an optional red zone (e.g. RPM redline, coolant overheat).
 */
@Composable
fun ArcGauge(
    fraction: Float,
    valueText: String,
    unit: String,
    label: String,
    modifier: Modifier = Modifier,
    redFrom: Float? = null,
) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(220), label = "gauge")
    val inRed = redFrom != null && f >= redFrom
    val color = if (inRed) Palette.Fault else Palette.Amber
    Box(modifier.aspectRatio(1.15f), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = size.minDimension * 0.075f
            val d = size.minDimension - stroke * 2
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2 + d * 0.06f)
            val sz = Size(d, d)
            val start = 150f
            val sweep = 240f
            // track
            drawArc(Palette.Track, start, sweep, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
            // red zone
            if (redFrom != null) {
                drawArc(Palette.Fault.copy(alpha = 0.35f), start + sweep * redFrom, sweep * (1 - redFrom), false, tl, sz,
                    style = Stroke(stroke, cap = StrokeCap.Butt))
            }
            // glow + value
            if (f > 0.002f) {
                drawArc(color.copy(alpha = 0.18f), start, sweep * f, false, tl, sz, style = Stroke(stroke * 2.2f, cap = StrokeCap.Round))
                drawArc(color, start, sweep * f, false, tl, sz, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            // ticks
            val c = Offset(tl.x + d / 2, tl.y + d / 2)
            val rOuter = d / 2 - stroke * 1.4f
            for (i in 0..20) {
                val a = Math.toRadians((start + sweep * i / 20).toDouble())
                val major = i % 5 == 0
                val len = if (major) stroke * 1.1f else stroke * 0.55f
                val p1 = Offset(c.x + (rOuter * cos(a)).toFloat(), c.y + (rOuter * sin(a)).toFloat())
                val p2 = Offset(c.x + ((rOuter - len) * cos(a)).toFloat(), c.y + ((rOuter - len) * sin(a)).toFloat())
                drawLine(if (major) Palette.TextMuted else Palette.TextFaint, p1, p2, strokeWidth = if (major) 3f else 2f, cap = StrokeCap.Round)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 10.dp)) {
            Text(valueText, style = MaterialTheme.typography.displayMedium, color = if (inRed) Palette.Fault else Palette.Text)
            Text(unit, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted)
            Spacer(Modifier.height(6.dp))
            SectionLabel(label)
        }
    }
}

/** Tiny line chart with a fading fill — recent history for a sensor. */
@Composable
fun Sparkline(values: List<Float>, min: Float, max: Float, modifier: Modifier = Modifier, color: Color = Palette.Amber) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val span = (max - min).takeIf { it > 0f } ?: 1f
        val step = size.width / (values.size - 1).coerceAtLeast(1)
        fun y(v: Float) = size.height - ((v - min) / span).coerceIn(0f, 1f) * size.height
        val line = Path().apply {
            moveTo(0f, y(values[0]))
            values.forEachIndexed { i, v -> if (i > 0) lineTo(i * step, y(v)) }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo((values.size - 1) * step, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        drawCircle(color, 4.5f, Offset((values.size - 1) * step, y(values.last())))
    }
}

/** Compact sensor tile: label, big number, unit, sparkline underneath. */
@Composable
fun SensorTile(
    label: String,
    valueText: String,
    unit: String,
    history: List<Float>,
    min: Float,
    max: Float,
    modifier: Modifier = Modifier,
) {
    Panel(modifier, padding = 16.dp) {
        SectionLabel(label)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(valueText, style = Numeric.copy(fontSize = 30.sp), color = Palette.Text)
            Spacer(Modifier.width(5.dp))
            Text(unit, style = MaterialTheme.typography.bodySmall, color = Palette.TextMuted, modifier = Modifier.padding(bottom = 5.dp))
        }
        Spacer(Modifier.height(10.dp))
        Sparkline(history, min, max, Modifier.fillMaxWidth().height(34.dp))
    }
}

/** A small round status glyph (check / dash / x). */
@Composable
fun StatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier.size(size).clip(CircleShape).background(color))
}
