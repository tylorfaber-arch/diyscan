package dev.diyscan.elm

import dev.diyscan.transport.Transport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** OBD protocols as reported by ATDPN. */
enum class ObdProtocol(val code: Char, val label: String, val isCan: Boolean) {
    AUTO('0', "Automatic", false),
    J1850_PWM('1', "SAE J1850 PWM (Ford)", false),
    J1850_VPW('2', "SAE J1850 VPW (GM / Chrysler)", false),
    ISO_9141('3', "ISO 9141-2", false),
    KWP_5BAUD('4', "ISO 14230 KWP2000 (5 baud init)", false),
    KWP_FAST('5', "ISO 14230 KWP2000 (fast init)", false),
    CAN_11_500('6', "ISO 15765 CAN 11-bit 500k", true),
    CAN_29_500('7', "ISO 15765 CAN 29-bit 500k", true),
    CAN_11_250('8', "ISO 15765 CAN 11-bit 250k", true),
    CAN_29_250('9', "ISO 15765 CAN 29-bit 250k", true),
    J1939('A', "SAE J1939 CAN (heavy duty)", true),
    UNKNOWN('?', "Unknown", false);

    companion object {
        fun fromDpn(s: String): ObdProtocol {
            // ATDPN returns e.g. "A2" (auto-detected, protocol 2) or "6"
            val c = s.trim().removePrefix("A").firstOrNull()?.uppercaseChar() ?: return UNKNOWN
            return entries.firstOrNull { it.code == c } ?: UNKNOWN
        }
    }
}

class ElmException(msg: String) : Exception(msg)

/**
 * Talks the ELM327 text protocol: send a command + '\r', read until the '>' prompt.
 * Works the same whether the vehicle is J1850, ISO 9141, KWP or CAN — the adapter
 * handles the bus, so this driver is protocol-agnostic.
 */
class Elm327(private val transport: Transport) {

    private val lock = Mutex()
    var protocol: ObdProtocol = ObdProtocol.UNKNOWN
        private set
    var adapterVersion: String = ""
        private set

    /** Raw command → cleaned response lines. One command at a time. */
    suspend fun send(cmd: String, timeoutMs: Long = 5_000): List<String> = lock.withLock { sendUnlocked(cmd, timeoutMs) }

    private suspend fun sendUnlocked(cmd: String, timeoutMs: Long = 5_000): List<String> = run {
        withContext(Dispatchers.IO) {
            drain() // drop any late bytes from a previous timed-out command
            transport.output.write("$cmd\r".toByteArray(Charsets.US_ASCII))
            transport.output.flush()
            val raw = readUntilPrompt(timeoutMs)
            raw.split('\r', '\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() && it != cmd && !it.startsWith("SEARCHING") }
        }
    }

    /**
     * Bluetooth socket reads block and ignore coroutine cancellation, so poll
     * available() against a deadline instead of relying on withTimeout.
     */
    private suspend fun readUntilPrompt(timeoutMs: Long): String {
        val sb = StringBuilder()
        val inp = transport.input
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            if (inp.available() > 0) {
                val b = inp.read()
                if (b == -1) throw ElmException("Adapter disconnected")
                val c = b.toChar()
                if (c == '>') return sb.toString()
                if (b != 0) sb.append(c) // some clones emit NUL bytes
            } else {
                if (System.currentTimeMillis() > deadline) throw ElmException("Adapter stopped responding (timeout)")
                delay(4)
            }
        }
    }

    private fun drain() {
        val inp = transport.input
        while (inp.available() > 0) if (inp.read() == -1) throw ElmException("Adapter disconnected")
    }

    /**
     * Bus monitor (ATMA): listen to every message on the bus, headers included,
     * until the calling coroutine is cancelled. Holds the adapter the whole time,
     * so other requests wait until monitoring stops.
     *
     * On a 2005 Ram this shows PCI bus (J1850 VPW) traffic between modules —
     * the raw material for working out manufacturer-specific messages.
     */
    suspend fun monitor(onLine: (String) -> Unit) = lock.withLock {
        withContext(Dispatchers.IO) {
            sendUnlocked("ATH1") // show headers: who sent it, who it's for
            drain()
            transport.output.write("ATMA\r".toByteArray(Charsets.US_ASCII))
            transport.output.flush()
            val inp = transport.input
            val sb = StringBuilder()
            try {
                while (isActive) {
                    if (inp.available() > 0) {
                        val b = inp.read()
                        if (b == -1) throw ElmException("Adapter disconnected")
                        val c = b.toChar()
                        when {
                            c == '\r' || c == '\n' -> {
                                val l = sb.toString().trim(); sb.clear()
                                if (l.isNotEmpty() && l != "ATMA") onLine(l)
                            }
                            c == '>' -> break // adapter stopped on its own (e.g. BUFFER FULL)
                            b != 0 -> sb.append(c)
                        }
                    } else delay(3)
                }
            } finally {
                withContext(NonCancellable) {
                    runCatching {
                        // Any character stops ATMA; adapter replies "STOPPED" then the prompt.
                        transport.output.write("\r".toByteArray()); transport.output.flush()
                        readUntilPrompt(2_000)
                    }
                    runCatching { sendUnlocked("ATH0") }
                }
            }
        }
    }

    /**
     * Standard init. Spaces off and headers off keep parsing simple.
     * ATSP0 lets the adapter auto-detect the bus — your 2005 Ram will come back as
     * J1850 VPW, a 2008+ vehicle as CAN, with no code changes.
     */
    suspend fun initialize() {
        send("ATZ", timeoutMs = 8_000)    // reset
        send("ATE0")                       // echo off
        send("ATL0")                       // linefeeds off
        send("ATS0")                       // spaces off
        send("ATH0")                       // headers off
        send("ATAT1")                      // adaptive timing (faster on slow buses)
        send("ATSP0")                      // auto protocol
        adapterVersion = send("ATI").firstOrNull().orEmpty()

        // First real request triggers protocol search. Slow buses (VPW/K-line) can take ~5-10 s.
        val r = send("0100", timeoutMs = 15_000)
        checkErrors(r)
        protocol = ObdProtocol.fromDpn(send("ATDPN").firstOrNull().orEmpty())
    }

    /**
     * Send an OBD request (e.g. "010C") and return the response lines as hex strings.
     * Throws on adapter errors; returns empty list on NO DATA.
     */
    suspend fun request(obd: String, timeoutMs: Long = 5_000): List<String> {
        val lines = send(obd, timeoutMs)
        if (lines.any { it.contains("NO DATA") }) return emptyList()
        checkErrors(lines)
        return lines
    }

    private fun checkErrors(lines: List<String>) {
        val errors = listOf("UNABLE TO CONNECT", "BUS INIT", "CAN ERROR", "BUS ERROR",
            "STOPPED", "ERROR", "?", "FB ERROR", "DATA ERROR", "BUFFER FULL")
        lines.firstOrNull { l -> errors.any { l.contains(it) } }?.let {
            throw ElmException("Adapter: $it")
        }
    }

    fun close() = transport.close()
}
