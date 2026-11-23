package dev.diyscan.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Two looks from the Gemini design pass (design/gemini-mockup):
 *  - Workshop Daylight: warm paper canvas, ink text, signal-orange accent. Readable under shop lights.
 *  - Night Cockpit: obsidian panels with luminous telemetry. Easy on the eyes in a dark cab.
 * Status colors carry meaning only: good / warn / fault, with cobalt reserved for bus & imported data.
 */
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Daylight"), DARK("Night") }

private val WorkshopLightColorScheme = lightColorScheme(
    primary = SignalOrange,
    onPrimary = Color.White,
    primaryContainer = SignalOrangeContainer,
    onPrimaryContainer = SignalOrangeOnContainer,
    secondary = TelemetryCobalt,
    onSecondary = Color.White,
    secondaryContainer = TelemetryCobaltContainer,
    onSecondaryContainer = Color(0xFF0A2463),
    tertiary = EmeraldPass,
    onTertiary = Color.White,
    background = DaylightCanvas,
    onBackground = DaylightInk,
    surface = DaylightPlate,
    onSurface = DaylightInk,
    surfaceVariant = DaylightWell,
    onSurfaceVariant = DaylightInkMuted,
    surfaceContainer = DaylightPlate,
    surfaceContainerHigh = DaylightPlateElevated,
    surfaceContainerHighest = DaylightWell,
    surfaceContainerLow = DaylightCanvas,
    outline = DaylightBezel,
    outlineVariant = DaylightBezel,
    error = CrimsonFault,
    onError = Color.White,
    errorContainer = CrimsonFaultBg,
    onErrorContainer = Color(0xFF690A0A),
)

private val NightCockpitColorScheme = darkColorScheme(
    primary = NightSignalOrange,
    onPrimary = Color(0xFF1F0A03),
    primaryContainer = NightSignalOrangeContainer,
    onPrimaryContainer = NightSignalOrangeOnContainer,
    secondary = NightCobalt,
    onSecondary = Color(0xFF041D29),
    secondaryContainer = NightCobaltContainer,
    onSecondaryContainer = Color(0xFFC2EBFF),
    tertiary = NightEmerald,
    onTertiary = Color(0xFF032117),
    background = NightCanvas,
    onBackground = NightInk,
    surface = NightPlate,
    onSurface = NightInk,
    surfaceVariant = NightWell,
    onSurfaceVariant = NightInkMuted,
    surfaceContainer = NightPlate,
    surfaceContainerHigh = NightPlateElevated,
    surfaceContainerHighest = NightWell,
    surfaceContainerLow = NightCanvas,
    outline = NightBezel,
    outlineVariant = NightBezel,
    error = NightCrimson,
    onError = Color(0xFF2E0709),
    errorContainer = NightCrimsonBg,
    onErrorContainer = Color(0xFFFFD5D7),
)

private val DiyScanShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun DiyScanTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = isDarkTheme(mode)
    CompositionLocalProvider(LocalInstrumentColors provides if (dark) NightInstrumentColors else DaylightInstrumentColors) {
        MaterialTheme(
            colorScheme = if (dark) NightCockpitColorScheme else WorkshopLightColorScheme,
            typography = DiyScanTypography,
            shapes = DiyScanShapes,
            content = content,
        )
    }
}

/**
 * Semantic color names used across the screens, resolved from the active theme
 * (Workshop Daylight or Night Cockpit). Read them only inside composables.
 */
object Palette {
    private val c @Composable @ReadOnlyComposable get() = LocalInstrumentColors.current
    val Bg @Composable @ReadOnlyComposable get() = c.canvas
    val Panel @Composable @ReadOnlyComposable get() = c.plate
    val PanelHigh @Composable @ReadOnlyComposable get() = c.plateElevated
    val Well @Composable @ReadOnlyComposable get() = c.well
    val Line @Composable @ReadOnlyComposable get() = c.bezel
    val Track @Composable @ReadOnlyComposable get() = c.gaugeTrack
    /** Primary accent (signal orange). Kept as "Amber" so existing screens read naturally. */
    val Amber @Composable @ReadOnlyComposable get() = c.accent
    val AmberGlow @Composable @ReadOnlyComposable get() = c.accentBg
    val OnAccent @Composable @ReadOnlyComposable get() = if (c.isDark) Color(0xFF1F0A03) else Color.White
    val Text @Composable @ReadOnlyComposable get() = c.ink
    val TextMuted @Composable @ReadOnlyComposable get() = c.inkMuted
    val TextFaint @Composable @ReadOnlyComposable get() = c.inkFaint
    val Good @Composable @ReadOnlyComposable get() = c.good
    val GoodBg @Composable @ReadOnlyComposable get() = c.goodBg
    val Warn @Composable @ReadOnlyComposable get() = c.warn
    val WarnBg @Composable @ReadOnlyComposable get() = c.warnBg
    val Fault @Composable @ReadOnlyComposable get() = c.fault
    val FaultBg @Composable @ReadOnlyComposable get() = c.faultBg
    /** Cobalt: bus traffic and imported (non-standard) data. */
    val Info @Composable @ReadOnlyComposable get() = c.bus
    val InfoBg @Composable @ReadOnlyComposable get() = c.busBg
}

/** Numbers use tabular figures so values don't jitter as they change. */
val Numeric = TelemetryReadoutStyle
