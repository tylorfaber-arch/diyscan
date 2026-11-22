package dev.diyscan.obd

/** A set of extra (usually manufacturer mode 22) PIDs imported from a Torque-style CSV. */
data class PidProfile(val name: String, val pids: List<Pid>, val skipped: List<String>)

/**
 * Reads the CSV format Torque Pro uses for custom PIDs, which is what most forum PID lists use:
 *
 *   Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header
 *   "Trans Fluid Temp","TFT","221940","A-40","-40","150","°C","7E1"
 *
 * Column order is taken from the header row if present, otherwise the order above.
 * Rows with formulas we can't evaluate are skipped and reported, not guessed at.
 */
object PidProfileParser {

    const val CUSTOM_BASE = 0x10000

    fun parse(name: String, text: String): PidProfile {
        val rows = csvRows(text).filter { r -> r.any { it.isNotBlank() } && !r[0].trimStart().startsWith("#") && !r[0].trimStart().startsWith("//") }
        if (rows.isEmpty()) return PidProfile(name, emptyList(), listOf("File is empty"))

        val first = rows[0].map { it.trim().lowercase().replace(" ", "") }
        val hasHeader = "modeandpid" in first || first.firstOrNull() == "name"
        fun col(key: String, fallback: Int): Int =
            if (hasHeader) first.indexOfFirst { it == key }.takeIf { it >= 0 } ?: -1 else fallback
        val iName = col("name", 0); val iShort = col("shortname", 1); val iPid = col("modeandpid", 2)
        val iEq = col("equation", 3); val iMin = col("minvalue", 4); val iMax = col("maxvalue", 5)
        val iUnit = col("units", 6); val iHdr = col("header", 7)
        if (iPid < 0 || iEq < 0) return PidProfile(name, emptyList(), listOf("Header row has no ModeAndPID / Equation columns"))

        val pids = mutableListOf<Pid>()
        val skipped = mutableListOf<String>()
        for (r in if (hasHeader) rows.drop(1) else rows) {
            fun get(i: Int) = if (i >= 0) r.getOrNull(i)?.trim().orEmpty() else ""
            val label = get(iName).ifBlank { get(iPid) }
            val req = get(iPid).uppercase().removePrefix("0X").filter { !it.isWhitespace() }
            if (req.length < 4 || req.length % 2 != 0 || !req.all { it in '0'..'9' || it in 'A'..'F' }) {
                skipped += "$label: bad ModeAndPID \"${get(iPid)}\""; continue
            }
            val eq = try { Equation.parse(get(iEq)) } catch (e: IllegalArgumentException) {
                skipped += "$label: ${e.message}"; continue
            }
            val hdr = get(iHdr).uppercase().filter { it in '0'..'9' || it in 'A'..'F' }.takeIf { it.length in setOf(3, 6, 8) }
            var min = get(iMin).toDoubleOrNull() ?: 0.0
            var max = get(iMax).toDoubleOrNull() ?: 100.0
            if (max <= min) { min = 0.0; max = 100.0 }
            pids += Pid(
                code = CUSTOM_BASE + pids.size,
                name = label,
                shortName = get(iShort).ifBlank { label.take(14) },
                unit = get(iUnit),
                bytes = eq.bytesNeeded.coerceAtLeast(1),
                min = min, max = max,
                request = req, header = hdr,
            ) { bytes -> eq.eval(bytes) }
        }
        return PidProfile(name, pids, skipped)
    }

    /** Minimal RFC 4180 CSV: quotes, escaped quotes, commas or semicolons, CRLF. */
    fun csvRows(text: String): List<List<String>> {
        val firstLine = text.lineSequence().firstOrNull().orEmpty()
        val sep = if (firstLine.count { it == ';' } > firstLine.count { it == ',' }) ';' else ','
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var q = false
        var i = 0
        val t = text.removePrefix("﻿")
        while (i < t.length) {
            val c = t[i]
            when {
                q && c == '"' && t.getOrNull(i + 1) == '"' -> { cell.append('"'); i++ }
                c == '"' -> q = !q
                !q && c == sep -> { row += cell.toString(); cell.clear() }
                !q && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && t.getOrNull(i + 1) == '\n') i++
                    row += cell.toString(); cell.clear(); rows += row; row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) { row += cell.toString(); rows += row }
        return rows
    }
}
