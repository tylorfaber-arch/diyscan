package dev.diyscan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * "Night cluster" — graphite panels with amber backlight, like a dash at night.
 * Status colors are reserved for meaning: green = good, amber = attention, red = fault.
 */
object Palette {
    val Bg = Color(0xFF0B0D10)
    val Panel = Color(0xFF14181D)
    val PanelHigh = Color(0xFF1B2027)
    val Line = Color(0xFF262D36)
    val Track = Color(0xFF222932)

    val Amber = Color(0xFFFFB020)
    val AmberDim = Color(0xFF8A5E10)
    val AmberGlow = Color(0x33FFB020)

    val Text = Color(0xFFE9ECEF)
    val TextMuted = Color(0xFF8D96A1)
    val TextFaint = Color(0xFF5B636D)

    val Good = Color(0xFF3DDC97)
    val GoodBg = Color(0x1F3DDC97)
    val Warn = Color(0xFFFFB020)
    val WarnBg = Color(0x1FFFB020)
    val Fault = Color(0xFFFF5A5F)
    val FaultBg = Color(0x1FFF5A5F)
    val Info = Color(0xFF5AB0FF)
    val InfoBg = Color(0x1F5AB0FF)
}

private val scheme = darkColorScheme(
    primary = Palette.Amber,
    onPrimary = Color(0xFF1C1300),
    primaryContainer = Palette.AmberDim,
    onPrimaryContainer = Palette.Text,
    secondary = Palette.Info,
    background = Palette.Bg,
    onBackground = Palette.Text,
    surface = Palette.Panel,
    onSurface = Palette.Text,
    surfaceVariant = Palette.PanelHigh,
    onSurfaceVariant = Palette.TextMuted,
    surfaceContainer = Palette.Panel,
    surfaceContainerHigh = Palette.PanelHigh,
    surfaceContainerHighest = Palette.PanelHigh,
    surfaceContainerLow = Palette.Bg,
    outline = Palette.Line,
    outlineVariant = Palette.Line,
    error = Palette.Fault,
)

/** Numbers use tabular figures so values don't jitter as they change. */
val Numeric = TextStyle(fontFeatureSettings = "tnum", fontWeight = FontWeight.SemiBold)

private val type = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp, fontFeatureSettings = "tnum"),
    displayMedium = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp, fontFeatureSettings = "tnum"),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    headlineSmall = TextStyle(fontSize = 21.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.8.sp, fontFamily = FontFamily.Monospace),
)

@Composable
fun DiyScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, typography = type, content = content)
}
