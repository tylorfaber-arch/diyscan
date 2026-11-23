package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

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
fun DiyScanMockupTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) NightCockpitColorScheme else WorkshopLightColorScheme
    val instrumentColors = if (darkTheme) NightInstrumentColors else DaylightInstrumentColors

    CompositionLocalProvider(LocalInstrumentColors provides instrumentColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DiyScanTypography,
            shapes = DiyScanShapes,
            content = content,
        )
    }
}
