package dev.diyscan.transport

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.LinkedBlockingQueue
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * A fake ELM327 plugged into a fake 2005 Dodge Ram 1500 (5.7 Hemi, J1850 VPW).
 * It speaks the exact same text protocol as a real adapter, so the whole app —
 * driver, parsers, UI — is exercised on the tablet without a vehicle.
 *
 * Simulated state: engine idling and warming up, a few blips of throttle, two stored
 * codes (P0128 thermostat, P0456 EVAP) and one pending (P0174 lean bank 2).
 */
class SimulatorTransport : Transport {

    private val rx = LinkedBlockingQueue<Int>()
    private val lineBuf = StringBuilder()
    private val startMs = System.currentTimeMillis()
    private var searched = false

    private var stored = mutableListOf("P0128", "P0456")
    private var pending = mutableListOf("P0174")
    private var cleared = false

    override val input: InputStream = object : InputStream() {
        override fun read(): Int = rx.take()
        override fun available(): Int = rx.size
    }

    override val output: OutputStream = object : OutputStream() {
        override fun write(b: Int) {
            if (monitoring) { stopMonitor(); return } // like a real ELM327: any byte stops ATMA
            val c = b.toChar()
            if (c == '\r') {
                val cmd = lineBuf.toString().trim().uppercase().replace(" ", "")
                lineBuf.clear()
                respond(cmd)
            } else lineBuf.append(c)
        }
    }

    override suspend fun connect() { Thread.sleep(600) }
    override fun close() { rx.put(-1) }

    // ---------------------------------------------------------------- protocol

    // ---------------------------------------------------------------- bus monitor (ATMA)

    @Volatile private var monitoring = false
    private var monitorThread: Thread? = null

    /**
     * Fake PCI-bus broadcast traffic. These IDs and byte meanings are MADE UP for
     * exercising the monitor UI — they are not real Chrysler messages.
     */
    private fun startMonitor() {
        monitoring = true
        monitorThread = Thread {
            var n = 0
            while (monitoring) {
                val s = sim()
                val t = t()
                val doorOpen = (t % 14.0) < 3.0
                val frames = listOf(
                    "2A8010" + "%04X".format((s.rpm * 4).toInt()) + "%02X".format(s.speed.toInt()),
                    "2C8011" + "%02X".format((s.coolant + 40).toInt()) + "%02X".format((s.iat + 40).toInt()),
                    "3A8024" + (if (doorOpen) "01" else "00") + "00",
                    "5280A0" + "%02X".format(((sin(t / 2) + 1) * 100).toInt()),
                    "6C8028" + "%02X".format(158),
                    "10A022" + "%06X".format(214_388 + (t / 30).toInt()),
                )
                val f = frames[n % frames.size]
                n++
                val crc = f.chunked(2).fold(0) { acc, h -> acc xor h.toInt(16) }
                (f + "%02X".format(crc) + "\r").forEach { rx.put(it.code) }
                Thread.sleep(if (n % frames.size == 0) 40 else 12)
            }
        }.apply { isDaemon = true; start() }
    }

    private fun stopMonitor() {
        monitoring = false
        monitorThread?.join(500)
        monitorThread = null
        "STOPPED\r\r>".forEach { rx.put(it.code) }
    }

    private fun respond(cmd: String) {
        if (cmd == "ATMA") { startMonitor(); return }
        val reply: String = when {
            cmd == "ATZ" -> { Thread.sleep(300); "ELM327 v1.5" }
            cmd == "ATI" -> "ELM327 v1.5 (simulator)"
            cmd == "ATDPN" -> "A2"
            cmd == "ATRV" -> "%.1fV".format(13.9 + sin(t() / 3) * 0.2)
            cmd.startsWith("AT") -> "OK"
            else -> { Thread.sleep(70); obd(cmd) } // J1850 VPW is slow; feel it.
        }
        val out = reply + "\r\r>"
        out.forEach { rx.put(it.code) }
    }

    private fun obd(cmd: String): String {
        if (!searched) { searched = true; Thread.sleep(1500); return "SEARCHING...\r" + obd(cmd) }
        val mode = cmd.take(2)
        val pid = cmd.drop(2).take(2).toIntOrNull(16)
        return when (mode) {
            "01" -> pid?.let(::mode01) ?: "NO DATA"
            "02" -> pid?.let(::mode02) ?: "NO DATA"
            "03" -> dtcLines("43", stored)
            "07" -> dtcLines("47", pending)
            "0A" -> "NO DATA" // permanent codes: 2010+ only
            "04" -> { stored.clear(); pending.clear(); cleared = true; "44" }
            "09" -> if (pid == 0x02) vinLines() else "NO DATA"
            "21", "22" -> enhanced(cmd)
            else -> "NO DATA"
        }
    }

    /**
     * Manufacturer-style requests (mode 21/22). Any ID answers with 4 bytes that drift
     * slowly, derived from the ID, so an imported PID profile shows moving values.
     * Not real vehicle data.
     */
    private fun enhanced(cmd: String): String {
        if (cmd.length < 4 || cmd.length % 2 != 0) return "7F" + cmd.take(2) + "12"
        val seed = cmd.drop(2).take(6).toInt(16)
        val t = t()
        val wave = { k: Int -> ((sin(t / (3.0 + k) + seed * 0.37 + k) + 1) * 0.5) }
        val bytes = (0 until 4).map { k -> (40 + wave(k) * 120).toInt().coerceIn(0, 255) }
        val mode = cmd.take(2).toInt(16) + 0x40
        return "%02X".format(mode) + cmd.drop(2) + bytes.joinToString("") { "%02X".format(it) }
    }

    private val supported = setOf(0x01, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x0B, 0x0C,
        0x0D, 0x0E, 0x0F, 0x11, 0x13, 0x14, 0x15, 0x18, 0x19, 0x1C, 0x1F, 0x20, 0x2F, 0x33)

    private fun mode01(pid: Int): String {
        if (pid % 0x20 == 0) {
            val bits = (1..32).fold(0L) { acc, i -> if (pid + i in supported) acc or (1L shl (32 - i)) else acc }
            return if (pid == 0 || pid in supported) "41%02X%08X".format(pid, bits) else "NO DATA"
        }
        if (pid !in supported) return "NO DATA"
        return "41%02X".format(pid) + data(pid, live = true)
    }

    private fun mode02(pid: Int): String {
        if (stored.isEmpty()) return "NO DATA"
        if (pid == 0x02) return "420200" + dtcHex(stored.first())
        if (pid !in supported) return "NO DATA"
        return "42%02X00".format(pid) + data(pid, live = false)
    }

    /** Data bytes as hex. live=false returns the frozen snapshot from when P0128 set. */
    private fun data(pid: Int, live: Boolean): String {
        val s = if (live) sim() else frozen
        fun b(v: Double) = "%02X".format(v.roundToInt().coerceIn(0, 255))
        fun w(v: Double) = "%04X".format(v.roundToInt().coerceIn(0, 65535))
        return when (pid) {
            0x01 -> readiness()
            0x03 -> "0200"
            0x04 -> b(s.load * 2.55)
            0x05 -> b(s.coolant + 40)
            0x06 -> b(s.stft1 * 1.28 + 128)
            0x07 -> b(3.1 * 1.28 + 128)
            0x08 -> b(s.stft2 * 1.28 + 128)
            0x09 -> b(9.4 * 1.28 + 128)  // bank 2 running lean → matches P0174
            0x0B -> b(s.map)
            0x0C -> w(s.rpm * 4)
            0x0D -> b(s.speed)
            0x0E -> b((s.timing + 64) * 2)
            0x0F -> b(s.iat + 40)
            0x11 -> b(s.throttle * 2.55)
            0x13 -> "33"
            0x14 -> b(s.o2 * 200) + "FF"
            0x15 -> b(0.68 * 200) + "FF"
            0x18 -> b((0.9 - s.o2) * 200) + "FF"
            0x19 -> b(0.62 * 200) + "FF"
            0x1C -> "01"
            0x1F -> w((System.currentTimeMillis() - startMs) / 1000.0)
            0x2F -> b(62.0 * 2.55)
            0x33 -> b(99.0)
            else -> "00"
        }
    }

    private fun readiness(): String {
        val count = stored.size
        val a = (if (stored.any { it == "P0456" || it == "P0128" }) 0x80 else 0) or count
        // continuous: misfire/fuel/components supported & complete
        val b = 0x07
        // supported: catalyst, EVAP, O2, O2 heater. Incomplete: EVAP (or everything after a clear)
        val c = 0x65
        val d = if (cleared) 0x65 else 0x04
        return "%02X%02X%02X%02X".format(a, b, c, d)
    }

    private fun dtcLines(prefix: String, codes: List<String>): String {
        if (codes.isEmpty()) return prefix + "000000000000"
        return codes.chunked(3).joinToString("\r") { chunk ->
            prefix + chunk.joinToString("") { dtcHex(it) } + "0000".repeat(3 - chunk.size)
        }
    }

    private fun dtcHex(code: String): String {
        val letter = "PCBU".indexOf(code[0])
        val a = (letter shl 6) or (code[1].digitToInt() shl 4) or code[2].digitToInt(16)
        return "%02X%s".format(a, code.substring(3))
    }

    private fun vinLines(): String {
        val vin = "1D7HA18N05S000001" // made-up VIN with a Ram-style prefix
        val bytes = listOf(0, 0, 0) + vin.map { it.code }
        return bytes.chunked(4).mapIndexed { i, c ->
            "4902%02X".format(i + 1) + c.joinToString("") { "%02X".format(it) }
        }.joinToString("\r")
    }

    // ---------------------------------------------------------------- engine model

    private data class State(
        val rpm: Double, val speed: Double, val coolant: Double, val iat: Double, val load: Double,
        val throttle: Double, val map: Double, val timing: Double, val stft1: Double, val stft2: Double, val o2: Double,
    )

    private val frozen = State(1450.0, 31.0, 71.0, 24.0, 28.0, 14.0, 41.0, 22.0, 1.6, 4.7, 0.45)

    private fun t() = (System.currentTimeMillis() - startMs) / 1000.0

    private fun sim(): State {
        val t = t()
        // a throttle blip every ~20 s lasting ~3 s
        val phase = t % 20.0
        val blip = if (phase in 12.0..15.0) sin((phase - 12.0) / 3.0 * PI) else 0.0
        val throttle = 3.5 + blip * 38
        val rpm = 680 + sin(t * 2.1) * 12 + blip * 2100
        val coolant = min(93.0, 58 + t * 0.35) + sin(t / 5) * 0.4   // warms up over ~100 s
        val load = 18 + blip * 42 + sin(t * 1.3)
        val map = 34 + blip * 48
        val timing = 14 + blip * 16 + sin(t * 1.7) * 1.5
        val o2 = 0.45 + sin(t * 4.4) * 0.38
        return State(rpm, 0.0, coolant, 24 + t * 0.01, load, throttle, map, timing,
            stft1 = sin(t * 1.9) * 3.2, stft2 = 2.4 + sin(t * 1.6) * 3.5, o2 = max(0.05, o2))
    }
}
