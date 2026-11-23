package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MockVehicleCatalog
import com.example.model.AdapterDevice
import com.example.model.BusFrame
import com.example.model.ConnectionState
import com.example.model.DrbModule
import com.example.model.DriveMode
import com.example.model.DtcCategory
import com.example.model.DtcEntry
import com.example.model.FreezeFrame
import com.example.model.MonitorState
import com.example.model.PidProfileItem
import com.example.model.PidSpec
import com.example.model.ReadinessMonitor
import com.example.model.ReplayRequest
import com.example.model.ReplayResult
import com.example.model.ReplayStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class MainTab(val label: String) {
    OVERVIEW("Overview"),
    LIVE("Live"),
    CODES("Codes"),
    BUS("Bus"),
    TOOLS("Tools"),
}

enum class SubScreen {
    NONE,
    IMPORTS,
    DRB_BROWSER,
    REPLAY,
}

data class ScanUiState(
    val darkTheme: Boolean = false,
    val imperialUnits: Boolean = true,
    val connection: ConnectionState = ConnectionState.Disconnected,
    val adapters: List<AdapterDevice> = MockVehicleCatalog.adapters,
    val activeTab: MainTab = MainTab.OVERVIEW,
    val subScreen: SubScreen = SubScreen.NONE,
    val driveMode: DriveMode = DriveMode.CRUISE,
    val liveRunning: Boolean = true,
    val liveValues: Map<String, Double> = emptyMap(),
    val liveHistory: Map<String, List<Double>> = emptyMap(),
    val selectedPidIds: Set<String> = setOf(
        "coolant", "load", "stft", "ltft", "throttle", "maf", "timing", "o2b1s1", "batt", "trans_temp", "oil_psi"
    ),
    val loggingActive: Boolean = false,
    val logSeconds: Int = 0,
    val logSamples: Int = 0,
    val dtcs: List<DtcEntry> = MockVehicleCatalog.initialDtcs,
    val freezeFrame: FreezeFrame? = MockVehicleCatalog.initialFreezeFrame,
    val readiness: List<ReadinessMonitor> = MockVehicleCatalog.initialReadiness,
    val readingCodes: Boolean = false,
    val busRunning: Boolean = false,
    val busFrames: List<BusFrame> = MockVehicleCatalog.initialBusFrames(false),
    val busTotalFrames: Int = 2818,
    val busSearchQuery: String = "",
    val pidProfiles: List<PidProfileItem> = MockVehicleCatalog.initialProfiles,
    val drbModules: List<DrbModule> = MockVehicleCatalog.initialDrbModules,
    val drbImported: Boolean = true,
    val replayPlanName: String? = null,
    val replayPlan: List<ReplayRequest> = emptyList(),
    val replayResults: List<ReplayResult> = emptyList(),
    val replayRunning: Boolean = false,
    val replayDone: Int = 0,
    val replayStatus: String? = null,
    val snackbarMessage: String? = null,
) {
    val isConnected: Boolean get() = connection is ConnectionState.Connected

    val activeProfileIds: Set<String>
        get() = pidProfiles.filter { it.enabled }.map { it.id }.toSet()

    val availablePids: List<PidSpec>
        get() = MockVehicleCatalog.standardPids +
            MockVehicleCatalog.customProfilePids.filter { it.profileId in activeProfileIds }

    val dashboardTiles: List<PidSpec>
        get() = availablePids.filter { it.id != "rpm" && it.id != "speed" && it.id in selectedPidIds }

    val activeFaultCount: Int
        get() = dtcs.count { it.category == DtcCategory.STORED || it.category == DtcCategory.PENDING }
}

class ScanMockViewModel : ViewModel() {

    private val _state = MutableStateFlow(ScanUiState())
    val state: StateFlow<ScanUiState> = _state.asStateFlow()

    private var handshakeJob: Job? = null
    private var liveJob: Job? = null
    private var busJob: Job? = null
    private var replayJob: Job? = null
    private var phase = 0.0

    init {
        seedInitialTelemetry()
    }

    private fun seedInitialTelemetry() {
        val initial = mapOf(
            "rpm" to 2280.0,
            "speed" to 88.0,
            "coolant" to 91.0,
            "load" to 38.4,
            "stft" to 3.2,
            "ltft" to 9.4,
            "throttle" to 24.5,
            "maf" to 21.8,
            "timing" to 22.5,
            "iat" to 31.0,
            "o2b1s1" to 0.72,
            "batt" to 14.24,
            "trans_temp" to 78.0,
            "oil_psi" to 310.0,
            "boost" to 28.0,
            "knock_ret" to 0.0,
        )
        val histories = initial.mapValues { (k, v) ->
            List(28) { idx ->
                val wave = sin(idx * 0.35 + k.hashCode() % 7) * (v * 0.04 + 0.6)
                (v + wave).coerceAtLeast(if (k == "stft" || k == "ltft" || k == "boost") -25.0 else 0.0)
            }
        }
        _state.update { it.copy(liveValues = initial, liveHistory = histories) }
    }

    fun toggleDarkTheme() {
        _state.update { it.copy(darkTheme = !it.darkTheme) }
    }

    fun setDarkTheme(dark: Boolean) {
        _state.update { it.copy(darkTheme = dark) }
    }

    fun setImperialUnits(imperial: Boolean) {
        _state.update { it.copy(imperialUnits = imperial) }
    }

    fun selectTab(tab: MainTab) {
        _state.update { it.copy(activeTab = tab, subScreen = SubScreen.NONE) }
        if (tab == MainTab.LIVE && _state.value.isConnected && _state.value.liveRunning) {
            startLiveLoop()
        }
        if (tab == MainTab.BUS && _state.value.isConnected && !_state.value.busRunning) {
            startBusMonitor()
        } else if (tab != MainTab.BUS && _state.value.busRunning) {
            stopBusMonitor()
        }
    }

    fun openSubScreen(sub: SubScreen) {
        _state.update { it.copy(subScreen = sub) }
    }

    fun closeSubScreen() {
        _state.update { it.copy(subScreen = SubScreen.NONE) }
    }

    fun consumeSnackbar() {
        _state.update { it.copy(snackbarMessage = null) }
    }

    fun connect(device: AdapterDevice) {
        handshakeJob?.cancel()
        handshakeJob = viewModelScope.launch {
            val steps = listOf(
                Triple("ATZ · Resetting ELM327 interface…", "> ATZ\n  ${device.firmware}", 0.22f),
                Triple("ATE0 / ATL0 · Configuring framing…", "> ATE0 ATL0 ATH1\n  OK", 0.48f),
                Triple("ATSP0 · Detecting vehicle bus…", "> ATSP0 / 0100\n  BUS: ${device.protocol}", 0.74f),
                Triple("0902 · Reading VIN & MIL status…", "> 0902\n  VIN: ${device.vin}", 0.96f),
            )
            val logs = mutableListOf<String>()
            for ((step, log, prog) in steps) {
                logs += log
                _state.update {
                    it.copy(
                        connection = ConnectionState.Handshaking(
                            device = device,
                            currentStep = step,
                            logLines = logs.toList(),
                            progress = prog,
                        )
                    )
                }
                delay(240)
            }
            _state.update {
                it.copy(
                    connection = ConnectionState.Connected(
                        device = device,
                        batteryVolts = 14.24,
                        milOn = it.dtcs.any { d -> d.category == DtcCategory.STORED },
                        lastRefreshed = "Just now",
                    ),
                    activeTab = MainTab.OVERVIEW,
                    subScreen = SubScreen.NONE,
                    busFrames = MockVehicleCatalog.initialBusFrames(device.isChryslerPci),
                    liveRunning = true,
                    snackbarMessage = "Connected to ${device.vehicleLabel}",
                )
            }
            startLiveLoop()
        }
    }

    fun disconnect() {
        handshakeJob?.cancel()
        liveJob?.cancel()
        busJob?.cancel()
        replayJob?.cancel()
        _state.update {
            it.copy(
                connection = ConnectionState.Disconnected,
                busRunning = false,
                loggingActive = false,
                replayRunning = false,
                subScreen = SubScreen.NONE,
                snackbarMessage = "Adapter disconnected",
            )
        }
    }

    fun refreshOverview() {
        viewModelScope.launch {
            delay(280)
            _state.update { s ->
                val conn = s.connection as? ConnectionState.Connected ?: return@update s
                val freshVolts = 14.15 + Random.nextDouble(-0.08, 0.14)
                s.copy(
                    connection = conn.copy(batteryVolts = freshVolts, lastRefreshed = "Updated just now"),
                    snackbarMessage = "ECU handshake & readiness monitors refreshed",
                )
            }
        }
    }

    fun setDriveMode(mode: DriveMode) {
        _state.update { it.copy(driveMode = mode) }
    }

    fun toggleLivePolling() {
        val next = !_state.value.liveRunning
        _state.update { it.copy(liveRunning = next) }
        if (next) startLiveLoop() else liveJob?.cancel()
    }

    fun togglePidSelected(pidId: String) {
        _state.update { s ->
            val next = if (pidId in s.selectedPidIds) s.selectedPidIds - pidId else s.selectedPidIds + pidId
            s.copy(selectedPidIds = next)
        }
    }

    fun toggleLogging() {
        val currently = _state.value.loggingActive
        if (currently) {
            val samples = _state.value.logSamples
            _state.update {
                it.copy(
                    loggingActive = false,
                    snackbarMessage = "Saved drive log CSV ($samples samples)",
                )
            }
        } else {
            _state.update {
                it.copy(
                    loggingActive = true,
                    logSeconds = 0,
                    logSamples = 0,
                    liveRunning = true,
                    snackbarMessage = "Recording CSV drive telemetry…",
                )
            }
            startLiveLoop()
        }
    }

    private fun startLiveLoop() {
        if (liveJob?.isActive == true) return
        liveJob = viewModelScope.launch {
            var tick = 0
            while (true) {
                delay(240)
                val s = _state.value
                if (!s.isConnected || !s.liveRunning) continue
                tick++
                phase += 0.22
                val mode = s.driveMode
                val cur = s.liveValues

                val rpmBase = mode.rpmTarget + sin(phase * 0.9) * when (mode) {
                    DriveMode.IDLE -> 28.0
                    DriveMode.CRUISE -> 185.0
                    DriveMode.SPIRITED -> 980.0
                } + Random.nextDouble(-18.0, 18.0)

                val prevRpm = cur["rpm"] ?: mode.rpmTarget
                val nextRpm = (prevRpm * 0.62 + rpmBase * 0.38).coerceIn(640.0, 6750.0)

                val speedTarget = mode.speedTargetKph + sin(phase * 0.5) * when (mode) {
                    DriveMode.IDLE -> 0.0
                    DriveMode.CRUISE -> 7.0
                    DriveMode.SPIRITED -> 18.0
                }
                val prevSpeed = cur["speed"] ?: mode.speedTargetKph
                val nextSpeed = (prevSpeed * 0.72 + speedTarget * 0.28).coerceIn(0.0, 195.0)

                val load = when (mode) {
                    DriveMode.IDLE -> 18.0 + sin(phase) * 2.2
                    DriveMode.CRUISE -> 38.0 + sin(phase * 0.8) * 7.5
                    DriveMode.SPIRITED -> 74.0 + sin(phase * 1.2) * 16.0
                }.coerceIn(10.0, 99.0)

                val throttle = when (mode) {
                    DriveMode.IDLE -> 11.5 + abs(sin(phase)) * 1.4
                    DriveMode.CRUISE -> 25.0 + sin(phase * 0.9) * 6.5
                    DriveMode.SPIRITED -> 66.0 + sin(phase * 1.3) * 24.0
                }.coerceIn(8.0, 98.0)

                val coolant = (90.5 + sin(phase * 0.08) * 2.4 + if (mode == DriveMode.SPIRITED) 4.2 else 0.0)
                    .coerceIn(82.0, 108.0)
                val stft = (3.4 + sin(phase * 1.5) * 4.8 + Random.nextDouble(-0.6, 0.6)).coerceIn(-15.0, 18.0)
                val ltft = (9.2 + sin(phase * 0.15) * 0.9).coerceIn(-12.0, 20.0)
                val maf = (nextRpm / 1000.0 * (load / 40.0) * 9.5).coerceIn(2.6, 152.0)
                val timing = (12.0 + (nextRpm / 220.0) - (load / 8.5)).coerceIn(4.0, 42.0)
                val iat = (29.0 + sin(phase * 0.06) * 2.2).coerceIn(18.0, 56.0)
                val o2 = (0.46 + sin(phase * 2.7) * 0.37).coerceIn(0.08, 0.88)
                val batt = (14.22 + sin(phase * 0.4) * 0.11).coerceIn(13.6, 14.6)
                val transTemp = (76.0 + sin(phase * 0.05) * 2.0 + if (mode == DriveMode.SPIRITED) 7.0 else 0.0)
                val oilPsi = (165.0 + (nextRpm / 7000.0) * 310.0).coerceIn(120.0, 490.0)
                val boost = (when (mode) {
                    DriveMode.IDLE -> -52.0
                    DriveMode.CRUISE -> 18.0 + sin(phase) * 14.0
                    DriveMode.SPIRITED -> 108.0 + sin(phase * 1.2) * 38.0
                }).coerceIn(-65.0, 155.0)
                val knock = if (mode == DriveMode.SPIRITED && sin(phase * 1.4) > 0.65) {
                    Random.nextDouble(1.0, 3.8)
                } else 0.0

                val nextValues = mapOf(
                    "rpm" to nextRpm,
                    "speed" to nextSpeed,
                    "coolant" to coolant,
                    "load" to load,
                    "stft" to stft,
                    "ltft" to ltft,
                    "throttle" to throttle,
                    "maf" to maf,
                    "timing" to timing,
                    "iat" to iat,
                    "o2b1s1" to o2,
                    "batt" to batt,
                    "trans_temp" to transTemp,
                    "oil_psi" to oilPsi,
                    "boost" to boost,
                    "knock_ret" to knock,
                )

                val nextHistories = s.liveHistory.toMutableMap()
                nextValues.forEach { (k, v) ->
                    val old = nextHistories[k].orEmpty()
                    nextHistories[k] = (old + v).takeLast(32)
                }

                _state.update { stateNow ->
                    val conn = stateNow.connection as? ConnectionState.Connected
                    stateNow.copy(
                        liveValues = nextValues,
                        liveHistory = nextHistories,
                        connection = conn?.copy(batteryVolts = batt) ?: stateNow.connection,
                        logSamples = if (stateNow.loggingActive) stateNow.logSamples + 1 else stateNow.logSamples,
                        logSeconds = if (stateNow.loggingActive && tick % 4 == 0) stateNow.logSeconds + 1 else stateNow.logSeconds,
                    )
                }
            }
        }
    }

    fun readCodes() {
        viewModelScope.launch {
            _state.update { it.copy(readingCodes = true) }
            delay(480)
            _state.update { s ->
                s.copy(
                    readingCodes = false,
                    snackbarMessage = "Scanned Modes 03, 07 & 0A · ${s.dtcs.size} codes reported",
                )
            }
        }
    }

    fun clearCodes() {
        viewModelScope.launch {
            _state.update { it.copy(readingCodes = true) }
            delay(450)
            _state.update { s ->
                val remainingPermanent = s.dtcs.filter { it.category == DtcCategory.PERMANENT }
                val resetReadiness = s.readiness.map { m ->
                    if (!m.continuous && m.state == MonitorState.READY) m.copy(state = MonitorState.INCOMPLETE) else m
                }
                val conn = s.connection as? ConnectionState.Connected
                s.copy(
                    readingCodes = false,
                    dtcs = remainingPermanent,
                    freezeFrame = null,
                    readiness = resetReadiness,
                    connection = conn?.copy(milOn = false) ?: s.connection,
                    snackbarMessage = "Mode 04 sent · Stored & pending codes cleared; readiness monitors reset",
                )
            }
        }
    }

    fun restoreDemoFaults() {
        _state.update { s ->
            val conn = s.connection as? ConnectionState.Connected
            s.copy(
                dtcs = MockVehicleCatalog.initialDtcs,
                freezeFrame = MockVehicleCatalog.initialFreezeFrame,
                readiness = MockVehicleCatalog.initialReadiness,
                connection = conn?.copy(milOn = true) ?: s.connection,
                snackbarMessage = "Demo faults restored for testing",
            )
        }
    }

    fun startBusMonitor() {
        if (busJob?.isActive == true) return
        _state.update { it.copy(busRunning = true) }
        busJob = viewModelScope.launch {
            while (true) {
                delay(320)
                _state.update { s ->
                    if (!s.busRunning) return@update s
                    var deltaFrames = 0
                    val updated = s.busFrames.map { frame ->
                        if (Random.nextFloat() < 0.72f) {
                            val newBytes = frame.bytes.toMutableList()
                            val mask = MutableList(newBytes.size) { false }
                            val numChanges = Random.nextInt(1, 3)
                            repeat(numChanges) {
                                val idx = Random.nextInt(newBytes.size)
                                newBytes[idx] = (newBytes[idx] + Random.nextInt(-6, 9)) and 0xFF
                                mask[idx] = newBytes[idx] != frame.bytes[idx]
                            }
                            val bump = Random.nextInt(1, 6)
                            deltaFrames += bump
                            frame.copy(
                                prevBytes = frame.bytes,
                                bytes = newBytes,
                                count = frame.count + bump,
                                changedMask = mask,
                            )
                        } else {
                            frame.copy(changedMask = List(frame.bytes.size) { false })
                        }
                    }
                    s.copy(busFrames = updated, busTotalFrames = s.busTotalFrames + deltaFrames)
                }
            }
        }
    }

    fun stopBusMonitor() {
        busJob?.cancel()
        busJob = null
        _state.update { it.copy(busRunning = false) }
    }

    fun setBusSearch(query: String) {
        _state.update { it.copy(busSearchQuery = query) }
    }

    fun saveBusCapture() {
        val count = _state.value.busTotalFrames
        _state.update { it.copy(snackbarMessage = "Saved ATMA bus capture ($count frames) to logs/") }
    }

    fun togglePidProfile(profileId: String, enabled: Boolean) {
        _state.update { s ->
            val updated = s.pidProfiles.map {
                if (it.id == profileId) it.copy(enabled = enabled) else it
            }
            val addedPids = if (enabled) {
                MockVehicleCatalog.customProfilePids.filter { it.profileId == profileId }.map { it.id }.toSet()
            } else emptySet()
            s.copy(
                pidProfiles = updated,
                selectedPidIds = s.selectedPidIds + addedPids,
                snackbarMessage = if (enabled) "Profile enabled — sensors added to Live" else "Profile disabled",
            )
        }
    }

    fun addSampleProfile() {
        val already = _state.value.pidProfiles.any { it.id == "prof_toyota" }
        if (already) {
            _state.update { it.copy(snackbarMessage = "Sample Toyota profile already imported") }
            return
        }
        val item = PidProfileItem(
            id = "prof_toyota",
            fileName = "Toyota_Hybrid_HV.csv",
            makeLabel = "Toyota / Lexus",
            sensorCount = 14,
            enabled = true,
            protocolHint = "Mode 21 · HV ECU",
        )
        _state.update {
            it.copy(
                pidProfiles = it.pidProfiles + item,
                snackbarMessage = "Imported Toyota_Hybrid_HV.csv (14 sensors)",
            )
        }
    }

    fun removePidProfile(profileId: String) {
        _state.update { s ->
            s.copy(
                pidProfiles = s.pidProfiles.filterNot { it.id == profileId },
                snackbarMessage = "Removed PID profile",
            )
        }
    }

    fun toggleDrbFavorite(moduleId: Int) {
        _state.update { s ->
            s.copy(
                drbModules = s.drbModules.map {
                    if (it.id == moduleId) it.copy(isFavorite = !it.isFavorite) else it
                }
            )
        }
    }

    fun toggleDrbDatabaseLoaded(loaded: Boolean) {
        _state.update {
            it.copy(
                drbImported = loaded,
                snackbarMessage = if (loaded) "Loaded Chrysler DRB-III database.mem (6 demo modules)" else "Removed DRB-III database from device",
            )
        }
    }

    fun loadDemoReplayPlan() {
        replayJob?.cancel()
        _state.update {
            it.copy(
                replayPlanName = "shop_scan_capture.txt",
                replayPlan = MockVehicleCatalog.demoReplayPlan,
                replayResults = emptyList(),
                replayDone = 0,
                replayRunning = false,
                replayStatus = "Ready · Read-only filter armed",
            )
        }
    }

    fun loadMode22SweepPlan() {
        replayJob?.cancel()
        _state.update {
            it.copy(
                replayPlanName = "Mode 22 Sweep (7E0 + 7E1)",
                replayPlan = MockVehicleCatalog.mode22SweepPlan,
                replayResults = emptyList(),
                replayDone = 0,
                replayRunning = false,
                replayStatus = "Ready · 10 CAN Mode 22 targets",
            )
        }
    }

    fun runReplayPlan() {
        val plan = _state.value.replayPlan
        if (plan.isEmpty() || replayJob?.isActive == true) return
        replayJob = viewModelScope.launch {
            val out = mutableListOf<ReplayResult>()
            _state.update {
                it.copy(
                    replayRunning = true,
                    replayResults = emptyList(),
                    replayDone = 0,
                    replayStatus = "Executing read-only plan…",
                )
            }
            for ((index, req) in plan.withIndex()) {
                delay(240)
                val result = if (!req.readOnly) {
                    ReplayResult(
                        module = req.module,
                        requestHex = req.requestHex,
                        label = req.label,
                        status = ReplayStatus.REFUSED,
                        note = "Blocked by safety guard (write / clear / seed-key)",
                    )
                } else if (index == 6 && plan.size >= 10) {
                    ReplayResult(
                        module = req.module,
                        requestHex = req.requestHex,
                        label = req.label,
                        status = ReplayStatus.NO_ANSWER,
                        note = "7F 22 31 · Request out of range",
                    )
                } else {
                    val b1 = Random.nextInt(0x10, 0xEF).toString(16).uppercase().padStart(2, '0')
                    val b2 = Random.nextInt(0x00, 0xFF).toString(16).uppercase().padStart(2, '0')
                    val b3 = Random.nextInt(0x00, 0xFF).toString(16).uppercase().padStart(2, '0')
                    val replyService = if (req.requestHex.startsWith("22")) "62" else "41"
                    val pidTail = req.requestHex.drop(2).trim()
                    ReplayResult(
                        module = req.module,
                        requestHex = req.requestHex,
                        label = req.label,
                        status = ReplayStatus.ANSWERED,
                        payloadHex = "$replyService $pidTail $b1 $b2 $b3",
                    )
                }
                out += result
                _state.update {
                    it.copy(
                        replayResults = out.toList(),
                        replayDone = index + 1,
                    )
                }
            }
            val answered = out.count { it.status == ReplayStatus.ANSWERED }
            val refused = out.count { it.status == ReplayStatus.REFUSED }
            _state.update {
                it.copy(
                    replayRunning = false,
                    replayStatus = "Complete · $answered answered" + if (refused > 0) " · $refused refused" else "",
                )
            }
        }
    }

    fun stopReplayPlan() {
        replayJob?.cancel()
        replayJob = null
        _state.update { it.copy(replayRunning = false, replayStatus = "Stopped after ${it.replayDone}") }
    }

    fun saveReplayCsv() {
        val count = _state.value.replayResults.size
        _state.update { it.copy(snackbarMessage = "Exported $count replay replies to replay.csv") }
    }
}
