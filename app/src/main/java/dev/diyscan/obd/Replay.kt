package dev.diyscan.obd

/** One request to send on the bus: a module header (null = the protocol's default) and the bytes. */
data class ReplayRequest(val module: String?, val request: String)

/** What came back for one request, or why nothing was sent. */
data class ReplayResult(
    val module: String?,
    val request: String,
    val lines: List<String> = emptyList(),
    val error: String? = null,
    val skipped: String? = null,
) {
    val answered get() = lines.isNotEmpty()
    /** The reply as one hex string, which is what a PID formula would consume. */
    val payload get() = lines.joinToString("") { it.filter { c -> c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f' } }.uppercase()
}

/**
 * Replaying the requests a scan tool made, and sweeping for PIDs it never showed.
 *
 * Only read services are allowed through. A replay must never be able to clear codes, start a
 * routine, write a value, or answer a seed/key handshake — those change the module, and the
 * point here is only to read what the tool read.
 */
object Replay {

    /** Services that only read data: current data, freeze frame, codes, VIN, enhanced PIDs. */
    private val READ_SERVICES = setOf("01", "02", "03", "05", "06", "07", "09", "0A", "21", "22")

    fun isReadOnly(request: String): Boolean =
        request.length >= 2 && request.take(2).uppercase() in READ_SERVICES

    /**
     * Read the plan written by `tools/obd_catalog.py` (replay.txt):
     *
     *   7E1 221940      header 7E1, request 22 19 40
     *   010C            no header: the protocol's broadcast header
     *   # comment
     */
    fun parsePlan(text: String): List<ReplayRequest> {
        val out = mutableListOf<ReplayRequest>()
        for (raw in text.lineSequence()) {
            val line = raw.substringBefore('#').trim()
            if (line.isEmpty()) continue
            val parts = line.split(Regex("\\s+"))
            val (module, request) = when (parts.size) {
                1 -> null to parts[0]
                else -> parts[0].uppercase() to parts[1]
            }
            val req = request.uppercase().removePrefix("0X")
            if (req.length < 2 || req.length % 2 != 0 || !req.all { it in '0'..'9' || it in 'A'..'F' }) continue
            if (module != null && module.length !in setOf(3, 6, 8)) continue
            out += ReplayRequest(module, req)
        }
        return out.distinct()
    }

    /** A sweep asks mode 22 for every PID in a range on the given CAN modules. */
    fun sweep(modules: List<String>, from: Int, to: Int): List<ReplayRequest> {
        if (from > to) return emptyList()
        return modules.flatMap { m ->
            (from..to).map { pid -> ReplayRequest(m, "22%04X".format(pid)) }
        }
    }

    /** Results as a CSV that opens next to a capture and keeps the request next to the answer. */
    fun toCsv(results: List<ReplayResult>): String {
        val sb = StringBuilder("module,request,reply,error,skipped\n")
        for (r in results) {
            sb.append(cell(r.module ?: "")).append(',')
                .append(r.request).append(',')
                .append(cell(r.lines.joinToString(" | "))).append(',')
                .append(cell(r.error ?: "")).append(',')
                .append(cell(r.skipped ?: ""))
                .append('\n')
        }
        return sb.toString()
    }

    private fun cell(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
