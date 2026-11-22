package dev.diyscan

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.net.Uri
import dev.diyscan.drb.DrbCommand
import dev.diyscan.drb.DrbDatabase
import dev.diyscan.drb.DrbModule
import dev.diyscan.elm.Elm327
import dev.diyscan.elm.ObdProtocol
import dev.diyscan.obd.DtcSet
import dev.diyscan.obd.FreezeFrame
import dev.diyscan.obd.ObdService
import dev.diyscan.obd.Pid
import dev.diyscan.obd.Pids
import dev.diyscan.obd.Readiness
import dev.diyscan.transport.BluetoothClassicTransport
import dev.diyscan.transport.SimulatorTransport
import dev.diyscan.transport.Transport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed interface Conn {
    data object Disconnected : Conn
    data class Connecting(val step: String) : Conn
    data class Connected(val name: String, val protocol: ObdProtocol, val adapter: String, val simulated: Boolean) : Conn
    data class Failed(val message: String) : Conn
}

data class DeviceItem(val name: String, val address: String)

/** One message ID seen on the bus, with when each byte last changed. */
data class BusId(
    val id: String,
    val data: List<String>,      // hex bytes
    val changedAt: List<Long>,   // per byte, ms timestamp of last change
    val count: Int,
    val lastMs: Long,
)

data class UiState(
    val conn: Conn = Conn.Disconnected,
    val devices: List<DeviceItem> = emptyList(),
    val supported: Set<Int> = emptySet(),
    val dashboard: List<Pid> = Pids.defaultDashboard,
    val live: Map<Int, Double> = emptyMap(),          // metric values
    val history: Map<Int, List<Float>> = emptyMap(),  // metric, last HISTORY samples
    val liveRunning: Boolean = false,
    val refreshHz: Float = 0f,
    val dtcs: DtcSet? = null,
    val readiness: Readiness? = null,
    val freezeFrame: FreezeFrame? = null,
    val vin: String? = null,
    val busy: String? = null,
    val message: String? = null,
    val imperial: Boolean = true,
    val logFile: String? = null,
    // bus monitor
    val monitorRunning: Boolean = false,
    val busIds: List<BusId> = emptyList(),
    val busRaw: List<String> = emptyList(),   // newest first
    val busTotal: Int = 0,
    val busRate: Float = 0f,
    // Chrysler DRB-III database (user-supplied)
    val drbModules: List<DrbModule> = emptyList(),
    val drbCommandTotal: Int = 0,
    val drbStatus: String? = null,      // loading / error text
    val drbOpen: Boolean = false,       // browser screen showing
) {
    val connected get() = conn is Conn.Connected
    /** Dashboard PIDs the vehicle actually supports (all of them if we haven't asked yet). */
    val visibleDashboard get() = if (supported.isEmpty()) dashboard else dashboard.filter { it.code in supported }
    val availablePids get() = Pids.all.filter { supported.isEmpty() || it.code in supported }
}

const val HISTORY = 120

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var elm: Elm327? = null
    private var obd: ObdService? = null
    private var liveJob: Job? = null
    private var log: FileWriter? = null
    private var monitorJob: Job? = null
    private val capture = ArrayList<String>()
    private var drb: DrbDatabase? = null
    private val drbFile get() = File(getApplication<Application>().filesDir, "drb/database.mem")

    init { loadSavedDrb() }

    // ------------------------------------------------------------ connection

    @SuppressLint("MissingPermission")
    fun refreshDevices() {
        val mgr = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val list = try {
            mgr.adapter?.bondedDevices.orEmpty()
                .map { DeviceItem(it.name ?: "Unnamed", it.address) }
                .sortedByDescending { looksLikeObd(it.name) }
        } catch (e: SecurityException) {
            emptyList()
        }
        _state.update { it.copy(devices = list) }
    }

    private fun looksLikeObd(n: String) = listOf("OBD", "ELM", "VLINK", "VGATE", "OBDLINK", "V-LINK", "ICAR", "KONNWEI")
        .any { n.uppercase().contains(it) }

    @SuppressLint("MissingPermission")
    fun connect(item: DeviceItem) {
        val mgr = getApplication<Application>().getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val dev: BluetoothDevice = mgr.adapter?.getRemoteDevice(item.address) ?: return
        start(BluetoothClassicTransport(dev), item.name, simulated = false)
    }

    fun connectSimulator() = start(SimulatorTransport(), "Simulated 2005 Ram 1500", simulated = true)

    private fun start(t: Transport, name: String, simulated: Boolean) {
        disconnect()
        viewModelScope.launch {
            try {
                _state.update { it.copy(conn = Conn.Connecting("Opening Bluetooth link…")) }
                withContext(Dispatchers.IO) { t.connect() }
                val e = Elm327(t)
                _state.update { it.copy(conn = Conn.Connecting("Talking to adapter…")) }
                _state.update { it.copy(conn = Conn.Connecting("Searching for vehicle protocol…")) }
                e.initialize()
                elm = e
                val o = ObdService(e)
                obd = o
                _state.update { it.copy(conn = Conn.Connecting("Reading supported sensors…")) }
                val sup = o.supportedPids()
                _state.update {
                    it.copy(conn = Conn.Connected(name, e.protocol, e.adapterVersion, simulated), supported = sup)
                }
                // Pull the basics right away so the home screen has something to show.
                refreshOverview()
            } catch (ex: Exception) {
                runCatching { t.close() }
                elm = null; obd = null
                _state.update { it.copy(conn = Conn.Failed(friendly(ex))) }
            }
        }
    }

    fun disconnect() {
        stopMonitor()
        stopLive()
        elm?.close()
        elm = null; obd = null
        _state.update {
            UiState(devices = it.devices, imperial = it.imperial, dashboard = it.dashboard,
                drbModules = it.drbModules, drbCommandTotal = it.drbCommandTotal, drbOpen = it.drbOpen)
        }
    }

    private fun friendly(e: Exception): String = when {
        e.message?.contains("UNABLE TO CONNECT") == true ->
            "Adapter is fine but the truck didn't answer. Is the key ON (engine off or running)?"
        e.message?.contains("BUS INIT") == true ->
            "Bus init failed. Key ON and retry; cheap clone adapters often fail on older (J1850 / K-line) vehicles."
        e is kotlinx.coroutines.TimeoutCancellationException -> "Adapter stopped responding (timeout)."
        e.message?.contains("read failed") == true || e is java.io.IOException ->
            "Couldn't open Bluetooth link. Make sure the adapter is plugged in and paired."
        else -> e.message ?: e.javaClass.simpleName
    }

    // ------------------------------------------------------------ live data

    fun startLive() {
        val o = obd ?: return
        if (liveJob?.isActive == true) return
        _state.update { it.copy(liveRunning = true) }
        liveJob = viewModelScope.launch {
            var cycles = 0
            var windowStart = System.currentTimeMillis()
            while (isActive) {
                val pids = state.value.visibleDashboard
                val values = mutableMapOf<Int, Double>()
                for (p in pids) {
                    if (!isActive) break
                    try {
                        o.read(p)?.let { v ->
                            values[p.code] = v
                            _state.update { s ->
                                val h = (s.history[p.code].orEmpty() + v.toFloat()).takeLast(HISTORY)
                                s.copy(live = s.live + (p.code to v), history = s.history + (p.code to h))
                            }
                        }
                    } catch (e: Exception) {
                        _state.update { it.copy(message = friendly(e)) }
                        delay(500)
                    }
                }
                writeLog(pids, values)
                cycles++
                val now = System.currentTimeMillis()
                if (now - windowStart > 2000) {
                    val hz = cycles * 1000f / (now - windowStart)
                    _state.update { it.copy(refreshHz = hz) }
                    cycles = 0; windowStart = now
                }
                if (pids.isEmpty()) delay(500)
            }
        }
    }

    fun stopLive() {
        liveJob?.cancel(); liveJob = null
        _state.update { it.copy(liveRunning = false) }
    }

    fun toggleDashboardPid(p: Pid) = _state.update { s ->
        s.copy(dashboard = if (p in s.dashboard) s.dashboard - p else s.dashboard + p)
    }

    // ------------------------------------------------------------ diagnostics

    fun refreshOverview() = job("Reading vehicle status…") { o ->
        val r = o.readiness()
        val d = o.dtcs()
        val v = state.value.vin ?: o.vin()
        _state.update { it.copy(readiness = r, dtcs = d, vin = v) }
    }

    fun readCodes() = job("Reading trouble codes…") { o ->
        val d = o.dtcs()
        val f = if (d.stored.isNotEmpty()) o.freezeFrame(state.value.supported) else null
        val r = o.readiness()
        _state.update { it.copy(dtcs = d, freezeFrame = f, readiness = r) }
    }

    /** Caller MUST confirm with the user first — this resets readiness monitors (smog). */
    fun clearCodes() = job("Clearing codes…") { o ->
        val ok = o.clearDtcs()
        delay(800)
        val d = o.dtcs()
        val r = o.readiness()
        _state.update {
            it.copy(dtcs = d, readiness = r, freezeFrame = null,
                message = if (ok) "Codes cleared. Readiness monitors were reset." else "Vehicle refused the clear request. Try key ON, engine OFF.")
        }
    }

    private fun job(label: String, block: suspend (ObdService) -> Unit) {
        val o = obd ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = label) }
            try { block(o) } catch (e: Exception) { _state.update { it.copy(message = friendly(e)) } }
            _state.update { it.copy(busy = null) }
        }
    }

    // ------------------------------------------------------------ bus monitor

    fun startMonitor() {
        val e = elm ?: return
        if (monitorJob?.isActive == true) return
        stopLive()
        val isCan = e.protocol.isCan
        val ids = LinkedHashMap<String, BusId>()
        val raw = ArrayDeque<String>()
        val lock = Any()
        var total = 0
        synchronized(capture) { capture.clear() }
        _state.update { it.copy(monitorRunning = true, busIds = emptyList(), busRaw = emptyList(), busTotal = 0, busRate = 0f) }

        fun publish(rate: Float?) = synchronized(lock) {
            val list = ids.values.sortedBy { it.id }
            val r = raw.toList()
            val t = total
            _state.update { it.copy(busIds = list, busRaw = r, busTotal = t, busRate = rate ?: it.busRate) }
        }

        monitorJob = viewModelScope.launch {
            val publisher = launch {
                var lastTotal = 0
                var lastMs = System.currentTimeMillis()
                while (isActive) {
                    delay(250)
                    val now = System.currentTimeMillis()
                    val t = synchronized(lock) { total }
                    val rate = (t - lastTotal) * 1000f / (now - lastMs).coerceAtLeast(1)
                    lastTotal = t; lastMs = now
                    publish(rate)
                }
            }
            try {
                e.monitor { line ->
                    val now = System.currentTimeMillis()
                    synchronized(capture) { if (capture.size < 200_000) capture += "$now,$line" }
                    synchronized(lock) {
                        total++
                        raw.addFirst(line); while (raw.size > 200) raw.removeLast()
                        splitFrame(line, isCan)?.let { (id, bytes) ->
                            val old = ids[id]
                            val changed = bytes.indices.map { i ->
                                val prev = old?.data?.getOrNull(i)
                                if (old == null || prev == bytes[i]) old?.changedAt?.getOrNull(i) ?: 0L else now
                            }
                            ids[id] = BusId(id, bytes, changed, (old?.count ?: 0) + 1, now)
                        }
                    }
                }
            } catch (ex: Exception) {
                if (ex !is kotlinx.coroutines.CancellationException) _state.update { it.copy(message = friendly(ex)) }
            } finally {
                publisher.cancel()
                publish(null)
                _state.update { it.copy(monitorRunning = false) }
            }
        }
    }

    fun stopMonitor() {
        monitorJob?.cancel(); monitorJob = null
    }

    /** Header/ID + data bytes. Non-hex lines (STOPPED, BUFFER FULL…) return null. */
    private fun splitFrame(line: String, isCan: Boolean): Pair<String, List<String>>? {
        val l = line.replace(" ", "").uppercase()
        if (l.length < 4 || !l.all { it in '0'..'9' || it in 'A'..'F' }) return null
        val idLen = when {
            !isCan -> 6                 // J1850 / ISO: 3 header bytes (priority, target, source)
            l.length % 2 == 1 -> 3      // CAN 11-bit
            else -> 8                   // CAN 29-bit
        }
        if (l.length <= idLen) return null
        return l.take(idLen) to l.drop(idLen).chunked(2)
    }

    fun saveCapture() {
        val lines = synchronized(capture) { capture.toList() }
        if (lines.isEmpty()) { _state.update { it.copy(message = "Nothing captured yet.") }; return }
        viewModelScope.launch(Dispatchers.IO) {
            val dir = File(getApplication<Application>().getExternalFilesDir(null), "logs").apply { mkdirs() }
            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
            val f = File(dir, "bus-$stamp.csv")
            f.writeText("time_ms,frame\n" + lines.joinToString("\n") + "\n")
            _state.update { it.copy(message = "Saved ${lines.size} frames: ${f.absolutePath}") }
        }
    }

    // ------------------------------------------------------------ Chrysler DRB-III database

    private fun loadSavedDrb() {
        if (!drbFile.exists()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(drbStatus = "Loading Chrysler database…") }
            runCatching { DrbDatabase.parse(drbFile.readBytes()) }
                .onSuccess(::setDrb)
                .onFailure { e -> _state.update { it.copy(drbStatus = "Saved database couldn't be read: ${e.message}") } }
        }
    }

    private fun setDrb(db: DrbDatabase) {
        drb = db
        _state.update { it.copy(drbModules = db.modules, drbCommandTotal = db.commandTotal, drbStatus = null) }
    }

    /** Import a database.mem, or a .zip containing one or more (newest entry wins). Stays on the device. */
    fun importDrb(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            _state.update { it.copy(drbStatus = "Importing…") }
            try {
                val bytes = app.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw java.io.IOException("Couldn't open the file")
                val mem = if (bytes.size > 4 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()) pickFromZip(bytes) else bytes
                val db = DrbDatabase.parse(mem) // validate before saving
                drbFile.parentFile?.mkdirs()
                drbFile.writeBytes(mem)
                setDrb(db)
                _state.update { it.copy(message = "Imported ${db.modules.size} modules, ${db.commandTotal} commands.") }
            } catch (e: Exception) {
                _state.update { it.copy(drbStatus = null, message = "Import failed: ${e.message}") }
            }
        }
    }

    private fun pickFromZip(zip: ByteArray): ByteArray {
        var best: ByteArray? = null
        var bestTime = Long.MIN_VALUE
        java.util.zip.ZipInputStream(zip.inputStream()).use { z ->
            while (true) {
                val e = z.nextEntry ?: break
                if (!e.isDirectory && e.name.lowercase().endsWith(".mem") && e.time >= bestTime) {
                    best = z.readBytes(); bestTime = e.time
                }
            }
        }
        return best ?: throw java.io.IOException("No .mem file inside the zip")
    }

    fun removeDrb() {
        drb = null
        drbFile.delete()
        _state.update { it.copy(drbModules = emptyList(), drbCommandTotal = 0, drbOpen = false, message = "Chrysler database removed from this device.") }
    }

    fun drbCommands(moduleId: Int): List<DrbCommand> = drb?.commands(moduleId).orEmpty()
    fun openDrb(open: Boolean) = _state.update { it.copy(drbOpen = open) }

    // ------------------------------------------------------------ logging & settings

    fun setImperial(on: Boolean) = _state.update { it.copy(imperial = on) }
    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun toggleLogging() {
        if (log != null) {
            runCatching { log?.close() }
            log = null
            val f = state.value.logFile
            _state.update { it.copy(logFile = null, message = "Log saved: $f") }
            return
        }
        val dir = File(getApplication<Application>().getExternalFilesDir(null), "logs").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val f = File(dir, "drive-$stamp.csv")
        log = FileWriter(f).apply {
            write("time_ms," + state.value.visibleDashboard.joinToString(",") { "${it.shortName} (${it.unit})" } + "\n")
        }
        _state.update { it.copy(logFile = f.absolutePath) }
        if (!state.value.liveRunning) startLive()
    }

    private fun writeLog(pids: List<Pid>, values: Map<Int, Double>) {
        val w = log ?: return
        runCatching {
            w.write("${System.currentTimeMillis()}," + pids.joinToString(",") { values[it.code]?.let { v -> "%.2f".format(Locale.US, v) } ?: "" } + "\n")
            w.flush()
        }
    }

    override fun onCleared() {
        runCatching { log?.close() }
        disconnect()
    }
}

// ---------------------------------------------------------------- display helpers

fun Pid.display(metric: Double, imperial: Boolean): Double = if (imperial) toImperial(metric) else metric
fun Pid.unitFor(imperial: Boolean) = if (imperial) imperialUnit else unit
fun Pid.rangeFor(imperial: Boolean) = if (imperial) imperialMin..imperialMax else min..max
fun Pid.format(metric: Double?, imperial: Boolean): String {
    if (metric == null) return "—"
    val v = display(metric, imperial)
    return when {
        unit == "V" -> "%.2f".format(v)
        code == Pids.RPM.code || code == Pids.RUN_TIME.code -> "%.0f".format(v)
        kotlin.math.abs(v) < 100 && unit != "%" -> "%.1f".format(v)
        else -> "%.0f".format(v)
    }
}
