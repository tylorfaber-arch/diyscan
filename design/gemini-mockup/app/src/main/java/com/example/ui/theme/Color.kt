package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// --- Workshop Daylight Palette (Braun / Swiss Industrial Telemetry) ---
val DaylightCanvas = Color(0xFFF3F1EA)
val DaylightPlate = Color(0xFFFFFFFF)
val DaylightWell = Color(0xFFE7E4DA)
val DaylightPlateElevated = Color(0xFFFAF9F5)
val DaylightBezel = Color(0xFFD4CFC2)
val DaylightInk = Color(0xFF13171D)
val DaylightInkMuted = Color(0xFF5A626F)
val DaylightInkFaint = Color(0xFF8A919E)

val SignalOrange = Color(0xFFE8481C)
val SignalOrangeContainer = Color(0xFFFCE6DF)
val SignalOrangeOnContainer = Color(0xFF5A1504)

val TelemetryCobalt = Color(0xFF1A56DB)
val TelemetryCobaltContainer = Color(0xFFE1ECFF)

val EmeraldPass = Color(0xFF0E875B)
val EmeraldPassBg = Color(0xFFE0F5EC)

val AmberCaution = Color(0xFFC27200)
val AmberCautionBg = Color(0xFFFFF1D6)

val CrimsonFault = Color(0xFFD62828)
val CrimsonFaultBg = Color(0xFFFDE4E4)

// --- Night Cockpit Palette (Anodized Obsidian & Luminous Telemetry) ---
val NightCanvas = Color(0xFF0C0F14)
val NightPlate = Color(0xFF141921)
val NightWell = Color(0xFF1B222C)
val NightPlateElevated = Color(0xFF1E2631)
val NightBezel = Color(0xFF2B3544)
val NightInk = Color(0xFFF1F4F8)
val NightInkMuted = Color(0xFF93A0B2)
val NightInkFaint = Color(0xFF606D80)

val NightSignalOrange = Color(0xFFFF6436)
val NightSignalOrangeContainer = Color(0xFF3A1A11)
val NightSignalOrangeOnContainer = Color(0xFFFFD8CC)

val NightCobalt = Color(0xFF38BDF8)
val NightCobaltContainer = Color(0xFF102A3A)

val NightEmerald = Color(0xFF34D399)
val NightEmeraldBg = Color(0xFF103127)

val NightAmber = Color(0xFFFBBF24)
val NightAmberBg = Color(0xFF33260C)

val NightCrimson = Color(0xFFFF5E63)
val NightCrimsonBg = Color(0xFF3B1518)

// --- Blueprint Hero Dark Surface (consistent visual anchor across themes) ---
val BlueprintSlate = Color(0xFF131920)
val BlueprintBorder = Color(0xFF283444)

@Immutable
data class InstrumentColors(
    val canvas: Color,
    val plate: Color,
    val plateElevated: Color,
    val well: Color,
    val bezel: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val accent: Color,
    val accentBg: Color,
    val bus: Color,
    val busBg: Color,
    val good: Color,
    val goodBg: Color,
    val warn: Color,
    val warnBg: Color,
    val fault: Color,
    val faultBg: Color,
    val gaugeTrack: Color,
    val isDark: Boolean,
)

val DaylightInstrumentColors = InstrumentColors(
    canvas = DaylightCanvas,
    plate = DaylightPlate,
    plateElevated = DaylightPlateElevated,
    well = DaylightWell,
    bezel = DaylightBezel,
    ink = DaylightInk,
    inkMuted = DaylightInkMuted,
    inkFaint = DaylightInkFaint,
    accent = SignalOrange,
    accentBg = SignalOrangeContainer,
    bus = TelemetryCobalt,
    busBg = TelemetryCobaltContainer,
    good = EmeraldPass,
    goodBg = EmeraldPassBg,
    warn = AmberCaution,
    warnBg = AmberCautionBg,
    fault = CrimsonFault,
    faultBg = CrimsonFaultBg,
    gaugeTrack = Color(0xFFE1DDD2),
    isDark = false,
)

val NightInstrumentColors = InstrumentColors(
    canvas = NightCanvas,
    plate = NightPlate,
    plateElevated = NightPlateElevated,
    well = NightWell,
    bezel = NightBezel,
    ink = NightInk,
    inkMuted = NightInkMuted,
    inkFaint = NightInkFaint,
    accent = NightSignalOrange,
    accentBg = NightSignalOrangeContainer,
    bus = NightCobalt,
    busBg = NightCobaltContainer,
    good = NightEmerald,
    goodBg = NightEmeraldBg,
    warn = NightAmber,
    warnBg = NightAmberBg,
    fault = NightCrimson,
    faultBg = NightCrimsonBg,
    gaugeTrack = Color(0xFF242E3B),
    isDark = true,
)

val LocalInstrumentColors = staticCompositionLocalOf { DaylightInstrumentColors }
