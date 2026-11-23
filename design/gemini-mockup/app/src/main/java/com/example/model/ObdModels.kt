package com.example.model

enum class AdapterType(val badge: String) {
    SIMULATOR("DEMO"),
    BLUETOOTH_SPP("BT SPP"),
    USB_OTG("USB-OTG"),
}

data class AdapterDevice(
    val id: String,
    val name: String,
    val subtitle: String,
    val type: AdapterType,
    val protocol: String,
    val firmware: String,
    val vin: String,
    val vehicleLabel: String,
    val ecuId: String,
    val isChryslerPci: Boolean = false,
)

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data class Handshaking(
        val device: AdapterDevice,
        val currentStep: String,
        val logLines: List<String>,
        val progress: Float,
    ) : ConnectionState

    data class Connected(
        val device: AdapterDevice,
        val batteryVolts: Double,
        val milOn: Boolean,
        val lastRefreshed: String,
    ) : ConnectionState
}

enum class DriveMode(val label: String, val rpmTarget: Double, val speedTargetKph: Double) {
    IDLE("Idle", 780.0, 0.0),
    CRUISE("Cruise", 2350.0, 96.0),
    SPIRITED("Spirited", 4850.0, 134.0),
}

data class PidSpec(
    val id: String,
    val codeHex: String,
    val name: String,
    val shortName: String,
    val metricUnit: String,
    val imperialUnit: String,
    val metricMin: Double,
    val metricMax: Double,
    val imperialMin: Double,
    val imperialMax: Double,
    val redlineMetric: Double? = null,
    val isCustomProfile: Boolean = false,
    val profileId: String? = null,
    val toImperial: (Double) -> Double = { it },
    val decimals: Int = 1,
) {
    fun displayValue(metric: Double, imperial: Boolean): Double =
        if (imperial) toImperial(metric) else metric

    fun displayUnit(imperial: Boolean): String = if (imperial) imperialUnit else metricUnit

    fun displayMin(imperial: Boolean): Double = if (imperial) imperialMin else metricMin

    fun displayMax(imperial: Boolean): Double = if (imperial) imperialMax else metricMax

    fun formatValue(metric: Double?, imperial: Boolean): String {
        if (metric == null) return "—"
        val v = displayValue(metric, imperial)
        return when (decimals) {
            0 -> "%.0f".format(v)
            2 -> "%.2f".format(v)
            else -> "%.1f".format(v)
        }
    }
}

enum class DtcSeverity(val label: String) {
    HIGH("Fix soon"),
    MEDIUM("Check it"),
    LOW("Minor"),
}

enum class DtcCategory(val label: String, val helper: String) {
    STORED(
        "Stored",
        "Confirmed faults detected across two drive cycles. These typically illuminate the check engine light."
    ),
    PENDING(
        "Pending",
        "Seen during the current or last drive cycle. Early warning before a fault becomes confirmed."
    ),
    PERMANENT(
        "Permanent",
        "2010+ OBD-II requirement. Cannot be erased by a scan tool; clears automatically after repair verification."
    ),
}

data class DtcEntry(
    val code: String,
    val title: String,
    val subsystem: String,
    val severity: DtcSeverity,
    val category: DtcCategory,
    val tip: String,
    val causes: List<String>,
)

data class FreezeFrame(
    val triggerCode: String,
    val rpm: Double,
    val speedKph: Double,
    val coolantC: Double,
    val loadPct: Double,
    val stftPct: Double,
    val ltftPct: Double,
    val mafGs: Double,
    val fuelSystem: String,
)

enum class MonitorState(val label: String) {
    READY("Ready"),
    INCOMPLETE("Incomplete"),
    UNSUPPORTED("N/A"),
}

data class ReadinessMonitor(
    val name: String,
    val shortCode: String,
    val state: MonitorState,
    val continuous: Boolean = false,
)

data class BusFrame(
    val arbId: String,
    val moduleHint: String,
    val bytes: List<Int>,
    val prevBytes: List<Int>,
    val count: Int,
    val rateHz: Double,
    val changedMask: List<Boolean>,
)

data class PidProfileItem(
    val id: String,
    val fileName: String,
    val makeLabel: String,
    val sensorCount: Int,
    val enabled: Boolean,
    val protocolHint: String,
)

data class DrbCommand(
    val id: String,
    val name: String,
    val requestHex: String,
    val decodeFormula: String,
    val unit: String,
    val category: String,
)

data class DrbModule(
    val id: Int,
    val name: String,
    val shortCode: String,
    val busType: String,
    val category: String,
    val isPci: Boolean,
    val isFavorite: Boolean,
    val commands: List<DrbCommand>,
)

data class ReplayRequest(
    val module: String,
    val requestHex: String,
    val label: String,
    val readOnly: Boolean = true,
)

data class ReplayResult(
    val module: String,
    val requestHex: String,
    val label: String,
    val status: ReplayStatus,
    val payloadHex: String? = null,
    val note: String? = null,
)

enum class ReplayStatus {
    ANSWERED,
    NO_ANSWER,
    REFUSED,
}
