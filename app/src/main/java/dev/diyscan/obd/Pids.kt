package dev.diyscan.obd

/**
 * Standard SAE J1979 mode 01 PIDs. Decoders return metric; [toImperial] converts
 * for display. These are identical on J1850, ISO 9141, KWP and CAN vehicles.
 */
data class Pid(
    val code: Int,
    val name: String,
    val shortName: String,
    val unit: String,
    val bytes: Int,
    val min: Double,
    val max: Double,
    val imperialUnit: String = unit,
    val imperialMin: Double = min,
    val imperialMax: Double = max,
    val toImperial: (Double) -> Double = { it },
    val decode: (IntArray) -> Double,
) {
    val hex: String get() = "%02X".format(code)
}

private val cToF: (Double) -> Double = { it * 9.0 / 5.0 + 32.0 }
private val kphToMph: (Double) -> Double = { it * 0.621371 }
private val kpaToPsi: (Double) -> Double = { it * 0.145038 }
private val gsToLbMin: (Double) -> Double = { it * 0.132277 }

object Pids {
    val ENGINE_LOAD = Pid(0x04, "Calculated engine load", "Load", "%", 1, 0.0, 100.0) { it[0] * 100.0 / 255 }
    val COOLANT = Pid(0x05, "Coolant temperature", "Coolant", "°C", 1, -40.0, 130.0,
        "°F", -40.0, 266.0, cToF) { it[0] - 40.0 }
    val STFT_B1 = Pid(0x06, "Short term fuel trim — Bank 1", "STFT B1", "%", 1, -25.0, 25.0) { (it[0] - 128) * 100.0 / 128 }
    val LTFT_B1 = Pid(0x07, "Long term fuel trim — Bank 1", "LTFT B1", "%", 1, -25.0, 25.0) { (it[0] - 128) * 100.0 / 128 }
    val STFT_B2 = Pid(0x08, "Short term fuel trim — Bank 2", "STFT B2", "%", 1, -25.0, 25.0) { (it[0] - 128) * 100.0 / 128 }
    val LTFT_B2 = Pid(0x09, "Long term fuel trim — Bank 2", "LTFT B2", "%", 1, -25.0, 25.0) { (it[0] - 128) * 100.0 / 128 }
    val MAP = Pid(0x0B, "Intake manifold pressure", "MAP", "kPa", 1, 0.0, 105.0,
        "psi", 0.0, 15.2, kpaToPsi) { it[0].toDouble() }
    val RPM = Pid(0x0C, "Engine speed", "RPM", "rpm", 2, 0.0, 6000.0) { (it[0] * 256 + it[1]) / 4.0 }
    val SPEED = Pid(0x0D, "Vehicle speed", "Speed", "km/h", 1, 0.0, 200.0,
        "mph", 0.0, 120.0, kphToMph) { it[0].toDouble() }
    val TIMING = Pid(0x0E, "Ignition timing advance", "Timing", "°", 1, -10.0, 50.0) { it[0] / 2.0 - 64 }
    val IAT = Pid(0x0F, "Intake air temperature", "IAT", "°C", 1, -40.0, 80.0,
        "°F", -40.0, 176.0, cToF) { it[0] - 40.0 }
    val MAF = Pid(0x10, "Mass air flow", "MAF", "g/s", 2, 0.0, 300.0,
        "lb/min", 0.0, 40.0, gsToLbMin) { (it[0] * 256 + it[1]) / 100.0 }
    val THROTTLE = Pid(0x11, "Throttle position", "Throttle", "%", 1, 0.0, 100.0) { it[0] * 100.0 / 255 }
    val O2_B1S1 = Pid(0x14, "O2 sensor voltage — B1S1", "O2 B1S1", "V", 2, 0.0, 1.1) { it[0] / 200.0 }
    val O2_B1S2 = Pid(0x15, "O2 sensor voltage — B1S2", "O2 B1S2", "V", 2, 0.0, 1.1) { it[0] / 200.0 }
    val O2_B2S1 = Pid(0x18, "O2 sensor voltage — B2S1", "O2 B2S1", "V", 2, 0.0, 1.1) { it[0] / 200.0 }
    val O2_B2S2 = Pid(0x19, "O2 sensor voltage — B2S2", "O2 B2S2", "V", 2, 0.0, 1.1) { it[0] / 200.0 }
    val RUN_TIME = Pid(0x1F, "Run time since engine start", "Run time", "s", 2, 0.0, 3600.0) { (it[0] * 256 + it[1]).toDouble() }
    val FUEL_LEVEL = Pid(0x2F, "Fuel level", "Fuel", "%", 1, 0.0, 100.0) { it[0] * 100.0 / 255 }
    val BARO = Pid(0x33, "Barometric pressure", "Baro", "kPa", 1, 70.0, 105.0,
        "psi", 10.0, 15.2, kpaToPsi) { it[0].toDouble() }
    val MODULE_VOLTAGE = Pid(0x42, "Control module voltage", "Voltage", "V", 2, 10.0, 16.0) { (it[0] * 256 + it[1]) / 1000.0 }
    val AMBIENT = Pid(0x46, "Ambient air temperature", "Ambient", "°C", 1, -40.0, 60.0,
        "°F", -40.0, 140.0, cToF) { it[0] - 40.0 }
    val OIL_TEMP = Pid(0x5C, "Engine oil temperature", "Oil temp", "°C", 1, -40.0, 150.0,
        "°F", -40.0, 302.0, cToF) { it[0] - 40.0 }

    val all: List<Pid> = listOf(
        RPM, SPEED, COOLANT, ENGINE_LOAD, THROTTLE, MAP, MAF, TIMING, IAT,
        STFT_B1, LTFT_B1, STFT_B2, LTFT_B2, O2_B1S1, O2_B1S2, O2_B2S1, O2_B2S2,
        FUEL_LEVEL, BARO, MODULE_VOLTAGE, AMBIENT, OIL_TEMP, RUN_TIME,
    )
    val byCode: Map<Int, Pid> = all.associateBy { it.code }

    /** Sensible default dashboard for a gas truck. */
    val defaultDashboard = listOf(RPM, SPEED, COOLANT, ENGINE_LOAD, THROTTLE, TIMING, LTFT_B1, LTFT_B2)
}
