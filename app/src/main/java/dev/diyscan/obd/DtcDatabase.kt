package dev.diyscan.obd

enum class Severity { LOW, MEDIUM, HIGH }

data class DtcInfo(val code: String, val description: String, val severity: Severity, val tip: String? = null)

/**
 * Generic SAE codes (P0xxx are the same on every make). Not exhaustive — unknown codes
 * fall back to a category description. Add manufacturer P1xxx codes per vehicle later.
 */
object DtcDatabase {

    private val S = Severity.LOW
    private val M = Severity.MEDIUM
    private val H = Severity.HIGH

    private val codes: Map<String, DtcInfo> = listOf(
        DtcInfo("P0016", "Crankshaft/camshaft position correlation (Bank 1)", H, "Check timing chain stretch and cam/crank sensors."),
        DtcInfo("P0100", "Mass air flow circuit malfunction", M),
        DtcInfo("P0101", "Mass air flow range/performance", M, "Clean MAF, check for intake leaks after the sensor."),
        DtcInfo("P0106", "MAP sensor range/performance", M, "Check MAP vacuum source and connector."),
        DtcInfo("P0107", "MAP sensor circuit low", M),
        DtcInfo("P0108", "MAP sensor circuit high", M),
        DtcInfo("P0113", "Intake air temperature sensor circuit high", S),
        DtcInfo("P0117", "Coolant temperature sensor circuit low", M),
        DtcInfo("P0118", "Coolant temperature sensor circuit high", M, "Often an open sensor or connector."),
        DtcInfo("P0121", "Throttle position sensor range/performance", M),
        DtcInfo("P0122", "Throttle position sensor circuit low", M),
        DtcInfo("P0123", "Throttle position sensor circuit high", M),
        DtcInfo("P0125", "Insufficient coolant temp for closed loop", S, "Commonly a stuck-open thermostat."),
        DtcInfo("P0128", "Coolant temperature below thermostat regulating temperature", S, "Thermostat stuck open — very common on Rams."),
        DtcInfo("P0131", "O2 sensor circuit low voltage (B1S1)", M),
        DtcInfo("P0133", "O2 sensor slow response (B1S1)", M, "Aging upstream O2 sensor."),
        DtcInfo("P0135", "O2 sensor heater circuit (B1S1)", M),
        DtcInfo("P0141", "O2 sensor heater circuit (B1S2)", S),
        DtcInfo("P0155", "O2 sensor heater circuit (B2S1)", M),
        DtcInfo("P0161", "O2 sensor heater circuit (B2S2)", S),
        DtcInfo("P0171", "System too lean (Bank 1)", M, "Look for vacuum leaks, weak fuel pressure, dirty MAF."),
        DtcInfo("P0172", "System too rich (Bank 1)", M),
        DtcInfo("P0174", "System too lean (Bank 2)", M, "Lean on both banks points to a shared cause: vacuum leak or fuel pressure."),
        DtcInfo("P0175", "System too rich (Bank 2)", M),
        DtcInfo("P0300", "Random/multiple cylinder misfire", H, "Avoid hard driving; misfires can overheat the catalytic converter."),
        DtcInfo("P0301", "Cylinder 1 misfire", H),
        DtcInfo("P0302", "Cylinder 2 misfire", H),
        DtcInfo("P0303", "Cylinder 3 misfire", H),
        DtcInfo("P0304", "Cylinder 4 misfire", H),
        DtcInfo("P0305", "Cylinder 5 misfire", H),
        DtcInfo("P0306", "Cylinder 6 misfire", H),
        DtcInfo("P0307", "Cylinder 7 misfire", H),
        DtcInfo("P0308", "Cylinder 8 misfire", H),
        DtcInfo("P0325", "Knock sensor circuit (Bank 1)", M),
        DtcInfo("P0335", "Crankshaft position sensor circuit", H, "Can cause stalling or no-start."),
        DtcInfo("P0340", "Camshaft position sensor circuit", H),
        DtcInfo("P0400", "EGR flow malfunction", M),
        DtcInfo("P0401", "EGR flow insufficient", M),
        DtcInfo("P0420", "Catalyst efficiency below threshold (Bank 1)", M, "Rule out O2 sensors and exhaust leaks before replacing the cat."),
        DtcInfo("P0430", "Catalyst efficiency below threshold (Bank 2)", M),
        DtcInfo("P0440", "EVAP system malfunction", S),
        DtcInfo("P0441", "EVAP incorrect purge flow", S),
        DtcInfo("P0442", "EVAP small leak detected", S, "Check gas cap seal first."),
        DtcInfo("P0443", "EVAP purge solenoid circuit", S),
        DtcInfo("P0455", "EVAP large leak detected", S, "Loose or missing gas cap is the #1 cause."),
        DtcInfo("P0456", "EVAP very small leak detected", S),
        DtcInfo("P0457", "EVAP leak — fuel cap loose/off", S),
        DtcInfo("P0500", "Vehicle speed sensor malfunction", M),
        DtcInfo("P0505", "Idle air control system", M, "Clean throttle body / IAC passage."),
        DtcInfo("P0506", "Idle RPM lower than expected", S),
        DtcInfo("P0507", "Idle RPM higher than expected", S, "Vacuum leak or dirty throttle body."),
        DtcInfo("P0562", "System voltage low", M, "Test battery and alternator."),
        DtcInfo("P0563", "System voltage high", M),
        DtcInfo("P0600", "Serial communication link", H),
        DtcInfo("P0601", "Control module memory checksum error", H),
        DtcInfo("P0700", "Transmission control system malfunction", H, "Read the transmission module for the specific code."),
        DtcInfo("P0715", "Input/turbine speed sensor circuit", H),
        DtcInfo("P0720", "Output speed sensor circuit", H),
        DtcInfo("P0740", "Torque converter clutch circuit", M),
        DtcInfo("P0750", "Shift solenoid A", H),
        DtcInfo("P0755", "Shift solenoid B", H),
    ).associateBy { it.code }

    fun lookup(code: String): DtcInfo = codes[code] ?: fallback(code)

    private fun fallback(code: String): DtcInfo {
        val system = when (code.first()) {
            'P' -> "Powertrain"; 'C' -> "Chassis (ABS/steering)"; 'B' -> "Body"; else -> "Network"
        }
        val oem = code.getOrNull(1) == '1' || code.getOrNull(1) == '3'
        val sub = if (code.first() == 'P') when (code.getOrNull(2)) {
            '0', '1', '2' -> "fuel & air metering"
            '3' -> "ignition / misfire"
            '4' -> "emissions controls"
            '5' -> "speed & idle control"
            '6' -> "computer & output circuits"
            '7', '8', '9' -> "transmission"
            else -> "general"
        } else "general"
        val desc = if (oem) "$system — manufacturer-specific ($sub)" else "$system — $sub"
        return DtcInfo(code, desc, Severity.MEDIUM, if (oem) "Look this code up for your make — it's not a generic SAE code." else null)
    }
}
