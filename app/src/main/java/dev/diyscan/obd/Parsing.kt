package dev.diyscan.obd

/**
 * Response parsing. ELM327 is configured with spaces off and headers off, so a
 * response line looks like "410C1AF8". On CAN, long replies come as ISO-TP frames:
 *   "014"          <- total byte count
 *   "0:490201314437"
 *   "1:48413138..."
 * On J1850/ISO 9141, long replies come as several complete messages, one per line.
 */
object Parsing {

    fun hexToBytes(hex: String): IntArray {
        val clean = hex.filter { it.isLetterOrDigit() }
        return IntArray(clean.length / 2) { clean.substring(it * 2, it * 2 + 2).toInt(16) }
    }

    /** Strip CAN ISO-TP framing ("0:", "1:", length line) and join into one hex string. */
    fun joinCanFrames(lines: List<String>): String {
        val framed = lines.any { it.length > 2 && it[1] == ':' }
        if (!framed) return lines.joinToString("")
        return lines
            .filter { it.contains(':') }
            .joinToString("") { it.substringAfter(':') }
    }

    /** Find the data bytes for a mode 01 PID reply. Returns null if not present. */
    fun mode01Data(lines: List<String>, pid: Int, bytes: Int): IntArray? {
        val prefix = "41%02X".format(pid)
        val line = lines.firstOrNull { it.uppercase().startsWith(prefix) } ?: return null
        val data = hexToBytes(line.substring(prefix.length))
        return if (data.size >= bytes) data.copyOf(bytes) else null
    }

    /**
     * Reply bytes for any request (e.g. "221940" → reply "621940" + data).
     * Returns null on no reply or a negative response (7F xx xx).
     */
    fun customData(lines: List<String>, request: String, isCan: Boolean): IntArray? {
        val mode = request.take(2).toIntOrNull(16) ?: return null
        val echo = "%02X".format(mode + 0x40) + request.drop(2).uppercase()
        val clean = lines.map { it.uppercase().filter { c -> !c.isWhitespace() } }
        clean.firstOrNull { it.startsWith(echo) }?.let { return hexToBytes(it.substring(echo.length)) }
        if (isCan) {
            val joined = joinCanFrames(clean)
            val i = joined.indexOf(echo)
            if (i >= 0) return hexToBytes(joined.substring(i + echo.length))
        }
        return null
    }

    /** Mode 02 freeze frame reply: "42" + PID + frame# + data. */
    fun mode02Data(lines: List<String>, pid: Int, bytes: Int): IntArray? {
        val prefix = "42%02X00".format(pid)
        val line = lines.firstOrNull { it.uppercase().startsWith(prefix) } ?: return null
        val data = hexToBytes(line.substring(prefix.length))
        return if (data.size >= bytes) data.copyOf(bytes) else null
    }

    /** Supported-PID bitmask from 0100 / 0120 / 0140 … */
    fun supportedPids(lines: List<String>, base: Int): Set<Int> {
        val mask = mode01Data(lines, base, 4) ?: return emptySet()
        val out = mutableSetOf<Int>()
        for (i in 0 until 32) {
            val byte = mask[i / 8]
            if (byte and (0x80 shr (i % 8)) != 0) out += base + i + 1
        }
        return out
    }

    /**
     * DTCs from mode 03 (stored), 07 (pending) or 0A (permanent).
     * @param responseMode 0x43, 0x47 or 0x4A
     */
    fun dtcs(lines: List<String>, responseMode: Int, isCan: Boolean): List<String> {
        val prefix = "%02X".format(responseMode)
        val codes = mutableListOf<String>()
        if (isCan) {
            // CAN: one message per ECU = "43" + count byte + 2 bytes per code.
            // With headers off, multiple ECUs show as separate single frames or framed blocks.
            val blocks = splitCanBlocks(lines)
            for (b in blocks) {
                val up = b.uppercase()
                val idx = up.indexOf(prefix)
                if (idx < 0) continue
                val data = hexToBytes(up.substring(idx + 2))
                if (data.isEmpty()) continue
                val count = data[0]
                for (i in 0 until count) {
                    val o = 1 + i * 2
                    if (o + 1 < data.size) decodeDtc(data[o], data[o + 1])?.let(codes::add)
                }
            }
        } else {
            // J1850 / ISO 9141 / KWP: each line is "43" + exactly 3 codes (6 bytes), zero padded.
            for (l in lines) {
                val up = l.uppercase()
                if (!up.startsWith(prefix)) continue
                val data = hexToBytes(up.substring(2))
                var i = 0
                while (i + 1 < data.size) {
                    decodeDtc(data[i], data[i + 1])?.let(codes::add)
                    i += 2
                }
            }
        }
        return codes.distinct()
    }

    private fun splitCanBlocks(lines: List<String>): List<String> {
        val out = mutableListOf<String>()
        val cur = StringBuilder()
        for (l in lines) {
            when {
                l.length > 2 && l[1] == ':' -> cur.append(l.substringAfter(':'))
                l.length <= 3 && l.all { it.isLetterOrDigit() } -> { // ISO-TP length line starts a new block
                    if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() }
                }
                else -> { if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() }; out += l }
            }
        }
        if (cur.isNotEmpty()) out += cur.toString()
        return out
    }

    /** Two raw bytes → "P0301" etc. Returns null for 0000 padding. */
    fun decodeDtc(a: Int, b: Int): String? {
        if (a == 0 && b == 0) return null
        val letter = "PCBU"[(a shr 6) and 0x03]
        val d1 = (a shr 4) and 0x03
        val d2 = a and 0x0F
        return "%c%d%X%02X".format(letter, d1, d2, b)
    }

    /** Mode 09 PID 02 — VIN. Handles both CAN framing and legacy multi-line replies. */
    fun vin(lines: List<String>, isCan: Boolean): String? {
        val bytes: IntArray = if (isCan) {
            val joined = joinCanFrames(lines).uppercase()
            val idx = joined.indexOf("4902")
            if (idx < 0) return null
            // "4902" + count(01) + 17 chars
            hexToBytes(joined.substring(idx + 6))
        } else {
            // Legacy: 5 lines of "4902" + seq + 4 bytes. First line carries 3 padding zeros.
            lines.filter { it.uppercase().startsWith("4902") }
                .sortedBy { it.substring(4, 6) }
                .flatMap { hexToBytes(it.substring(6)).toList() }
                .toIntArray()
        }
        val s = bytes.filter { it in 0x30..0x5A }.map { it.toChar() }.joinToString("")
        return s.takeLast(17).takeIf { it.length == 17 }
    }
}

/** Mode 01 PID 01: MIL status and emissions readiness monitors. */
data class Readiness(
    val milOn: Boolean,
    val dtcCount: Int,
    val monitors: List<Monitor>,
) {
    data class Monitor(val name: String, val complete: Boolean)

    val incomplete: Int get() = monitors.count { !it.complete }

    companion object {
        fun parse(d: IntArray): Readiness {
            val (a, b, c, dd) = listOf(d[0], d[1], d[2], d[3])
            val mons = mutableListOf<Monitor>()
            // Continuous monitors: B bits 0-2 supported, 4-6 incomplete
            listOf("Misfire", "Fuel system", "Components").forEachIndexed { i, n ->
                if (b and (1 shl i) != 0) mons += Monitor(n, b and (1 shl (i + 4)) == 0)
            }
            val diesel = b and 0x08 != 0
            val names = if (diesel)
                listOf("NMHC catalyst", "NOx/SCR", "—", "Boost pressure", "—", "Exhaust gas sensor", "PM filter", "EGR/VVT")
            else
                listOf("Catalyst", "Heated catalyst", "EVAP system", "Secondary air", "A/C refrigerant", "O2 sensor", "O2 sensor heater", "EGR system")
            for (i in 0 until 8) {
                if (c and (1 shl i) != 0 && names[i] != "—") mons += Monitor(names[i], dd and (1 shl i) == 0)
            }
            return Readiness(milOn = a and 0x80 != 0, dtcCount = a and 0x7F, monitors = mons)
        }
    }
}
