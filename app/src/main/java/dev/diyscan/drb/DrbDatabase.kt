/*
 * Reader for Chrysler DRB-III / StarSCAN "database.mem" files.
 *
 * File format logic ported from malykh-drb3-reader
 *   https://github.com/malykhcom/malykh-drb3-reader
 *   Copyright (c) 2022-2026 Anton Malykh — MIT License (see THIRD_PARTY_NOTICES.md)
 * which is itself a simplified version of MWisBest's DRBDBReader.
 *
 * The database file itself is Chrysler's and is NOT included with this app.
 * Users import their own copy; it stays on the device.
 */
package dev.diyscan.drb

import java.nio.ByteBuffer
import java.nio.ByteOrder

enum class DrbProtocol(val code: Int, val label: String) {
    J1850(1, "J1850 (PCI)"), CCD(53, "CCD"), SCI(60, "SCI"), ISO(103, "ISO 9141"),
    KWP(155, "KWP2000"), MULTIMETER(159, "Multimeter"), J2190(160, "J2190"), UNKNOWN(-1, "Unknown");

    companion object {
        fun of(code: Int) = entries.firstOrNull { it.code == code } ?: UNKNOWN
    }
}

/** How a module's reply is turned into a value. */
sealed interface DrbDecode {
    data class Numeric(val slope: Float?, val offset: Float?, val unit: String?, val metricUnit: String?,
                       val metricSlope: Float, val metricOffset: Float) : DrbDecode
    data class States(val mask: Int?, val default: String?, val states: List<Pair<Int, String>>) : DrbDecode
    data class Binary(val whenTrue: String, val whenFalse: String, val mask: Int?, val operator: String?) : DrbDecode
    data class Unknown(val type: Int) : DrbDecode
}

data class DrbModule(
    val id: Int,
    val name: String,
    val category: String,
    val commandCount: Int,
    val protocols: Set<DrbProtocol>,
)

data class DrbCommand(
    val id: Int,
    val name: String,
    val category: String,
    val protocol: DrbProtocol,
    val request: ByteArray,
    val requestLen: Int,
    val responseLen: Int,
    val decode: DrbDecode,
) {
    val requestHex: String get() = request.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
}

class DrbFormatException(msg: String) : Exception(msg)

class DrbDatabase private constructor(private val b: ByteArray) {

    val isStarScan: Boolean
    private val order: ByteOrder

    // raw tables
    private val moduleRows = HashMap<Int, IntArray>()                 // id -> [svc, nameId]
    private val serviceCats = HashMap<Int, Int>()                     // svcId -> nameId
    private val data = HashMap<Int, IntArray>()                       // dataId -> [protocol, reqLen, respLen]
    private val moduleTx = HashMap<Int, MutableList<Int>>()           // moduleId -> txIds
    private val strings = HashMap<Int, Int>()                         // stringId -> location
    private val states = HashMap<Int, MutableList<Pair<Int, Int>>>()  // stateId -> (value, stringId)
    private val defaultStates = HashMap<Int, Int>()
    private val binaryStates = HashMap<Int, IntArray>()               // id -> [trueStr, falseStr]
    private val masks = HashMap<Int, IntArray>()                      // id -> [mask, operator]
    private val numerics = HashMap<Int, FloatArray>()                 // id -> [slope, offset]
    private val units = HashMap<Int, FloatArray>()                    // id -> [metricSlope, metricOffset]
    private val unitStrings = HashMap<Int, IntArray>()                // id -> [defaultStr, metricStr]
    private val txs = HashMap<Int, RawTx>()
    private val texts = arrayOf("", "")

    private class RawTx(val conv: ByteArray, val dataId: Int, val bytes: ByteArray, val nameId: Int, val svc: Int)

    init {
        if (b.size < 64) throw DrbFormatException("File is too small to be a database.mem")
        val markOff = b.size - 0x17
        isStarScan = String(b, markOff, 8, Charsets.US_ASCII) == "StarSCAN"
        order = if (isStarScan) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN

        val hdr = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN)
        hdr.int; hdr.short // size, id (unused)
        val tableCount = hdr.short.toInt() and 0xFFFF
        if (tableCount !in 20..64) throw DrbFormatException("Not a DRB-III database (table count $tableCount)")
        repeat(tableCount) { index ->
            val off = hdr.int
            val rows = hdr.short.toInt() and 0xFFFF
            val rowSize = hdr.short.toInt() and 0xFFFF
            val colCount = hdr.get().toInt() and 0xFF
            val colBytes = ByteArray(27).also { hdr.get(it) }
            if (colCount > 27) throw DrbFormatException("Bad column count in table $index")
            val cols = colBytes.take(colCount).map { it.toInt() and 0xFF }.filter { it != 0 }.toIntArray()
            if (off < 0 || off.toLong() + rows.toLong() * rowSize > b.size) throw DrbFormatException("Table $index is out of bounds")
            loadTable(index, off, rows, rowSize, cols)
        }
        if (moduleRows.isEmpty() || txs.isEmpty()) throw DrbFormatException("No modules found — is this a DRB-III database.mem?")
    }

    private fun uint(off: Int, len: Int): Int {
        var v = 0
        if (order == ByteOrder.BIG_ENDIAN) for (i in 0 until len) v = (v shl 8) or (b[off + i].toInt() and 0xFF)
        else for (i in len - 1 downTo 0) v = (v shl 8) or (b[off + i].toInt() and 0xFF)
        return v
    }

    private fun uintIn(arr: ByteArray, off: Int, len: Int): Int {
        var v = 0
        if (order == ByteOrder.BIG_ENDIAN) for (i in 0 until len) v = (v shl 8) or (arr[off + i].toInt() and 0xFF)
        else for (i in len - 1 downTo 0) v = (v shl 8) or (arr[off + i].toInt() and 0xFF)
        return v
    }

    private fun loadTable(index: Int, off: Int, rows: Int, rowSize: Int, cols: IntArray) {
        if (index == 26 || index == 27) {
            texts[index - 26] = String(b, off, rows * rowSize, Charsets.ISO_8859_1)
            return
        }
        val colOff = IntArray(cols.size + 1).also { for (i in cols.indices) it[i + 1] = it[i] + cols[i] }
        fun handled(i: Int) = i in setOf(0, 2, 4, 5, 6, 8, 10, 13, 15, 16, 17, 23)
        if (!handled(index)) return
        for (r in 0 until rows) {
            val base = off + r * rowSize
            fun u(c: Int) = uint(base + colOff[c], cols[c])
            fun u8(c: Int) = b[base + colOff[c]].toInt() and 0xFF
            fun f(c: Int) = java.lang.Float.intBitsToFloat(u(c))
            fun raw(c: Int) = b.copyOfRange(base + colOff[c], base + colOff[c] + cols[c])
            when (index) {
                0 -> moduleRows[u(0)] = intArrayOf(u(1), u(3))
                2 -> binaryStates[u(0)] = intArrayOf(u(1), u(2))
                4 -> masks[u(0)] = intArrayOf(u(1), u8(2))
                5 -> numerics[u(0)] = floatArrayOf(f(1), f(2))
                6 -> serviceCats[u(3)] = u(1)
                8 -> data[u(0)] = intArrayOf(u(10), u(1), u(3))
                10 -> moduleTx.getOrPut(u(0)) { mutableListOf() } += u(1)
                13 -> u(1).takeIf { it != 0 }?.let { defaultStates[u(0)] = it }
                15 -> states.getOrPut(u(3)) { mutableListOf() } += u(1) to u(0)
                16 -> strings[u(0)] = u(1)
                17 -> { units[u(0)] = floatArrayOf(f(2), f(4)); unitStrings[u(0)] = intArrayOf(u(6), u(3)) }
                23 -> {
                    val bytesRaw = raw(6)
                    val n = (bytesRaw[0].toInt() and 0xFF).coerceAtMost(bytesRaw.size - 1)
                    txs[u(0)] = RawTx(raw(1), u(2), bytesRaw.copyOfRange(1, 1 + n), u(8), u(14) shr 8)
                }
            }
        }
    }

    fun string(id: Int): String {
        val loc = strings[id] ?: return ""
        val textNum = (loc ushr 24) and 0xFF
        val start = loc and 0xFFFFFF
        val text = texts.getOrNull(textNum) ?: return ""
        if (start >= text.length) return ""
        val end = text.indexOf('\u0000', start)
        return if (end < 0) "" else text.substring(start, end).trim()
    }

    private fun category(svc: Int) = serviceCats[svc]?.let(::string).orEmpty()

    // ------------------------------------------------------------ public API

    val modules: List<DrbModule> by lazy {
        moduleRows.map { (id, row) ->
            val tx = moduleTx[id].orEmpty()
            DrbModule(
                id = id,
                name = string(row[1]).ifBlank { "Module 0x%04X".format(id) },
                category = category(row[0]),
                commandCount = tx.size,
                protocols = tx.mapNotNull { txs[it]?.let { t -> data[t.dataId]?.get(0) } }.map(DrbProtocol::of).toSet(),
            )
        }.sortedBy { it.name.uppercase() }
    }

    val commandTotal: Int get() = txs.size

    fun commands(moduleId: Int): List<DrbCommand> = moduleTx[moduleId].orEmpty().mapNotNull { txId ->
        val t = txs[txId] ?: return@mapNotNull null
        val d = data[t.dataId]
        DrbCommand(
            id = txId,
            name = string(t.nameId).ifBlank { "Command 0x%X".format(txId) },
            category = category(t.svc),
            protocol = DrbProtocol.of(d?.get(0) ?: -1),
            request = t.bytes,
            requestLen = d?.get(1) ?: 0,
            responseLen = d?.get(2) ?: 0,
            decode = decode(t.conv),
        )
    }

    private fun decode(conv: ByteArray): DrbDecode {
        if (conv.size < 6) return DrbDecode.Unknown(-1)
        val type = conv[0].toInt() and 0xFF
        val ds = uintIn(conv, 2, 2)
        val cf = uintIn(conv, 4, 2)
        fun maskOf(id: Int?) = id?.let { masks[it] }
        return when (type) {
            0x00, 0x02 -> {
                val bs = binaryStates[ds]
                val m = if (type == 0x00) maskOf(cf) else null
                DrbDecode.Binary(
                    whenTrue = bs?.let { string(it[0]) }.orEmpty().ifBlank { "True" },
                    whenFalse = bs?.let { string(it[1]) }.orEmpty().ifBlank { "False" },
                    mask = m?.get(0), operator = m?.get(1)?.let(::operatorName),
                )
            }
            0x11, 0x12 -> {
                val n = if (type == 0x11) numerics[cf] else null
                val us = unitStrings[ds]; val um = units[ds]
                DrbDecode.Numeric(
                    slope = n?.get(0), offset = n?.get(1),
                    unit = us?.get(0)?.takeIf { it != 0 }?.let(::string),
                    metricUnit = us?.get(1)?.takeIf { it != 0 }?.let(::string),
                    metricSlope = um?.get(0) ?: 1f, metricOffset = um?.get(1) ?: 0f,
                )
            }
            0x20, 0x22 -> {
                val m = if (type == 0x20) maskOf(cf) else null
                DrbDecode.States(
                    mask = m?.get(0)?.takeIf { it != 0 },
                    default = defaultStates[ds]?.let(::string),
                    states = states[ds].orEmpty().sortedBy { it.first }.map { (v, s) -> v to string(s) },
                )
            }
            else -> DrbDecode.Unknown(type)
        }
    }

    private fun operatorName(op: Int) = when (op) {
        0x21 -> "≠"; 0x30 -> "mask = 0"; 0x39 -> "mask ≠ 0"; 0x3C -> "<"; 0x3D -> "="; 0x3E -> ">"
        else -> "op 0x%02X".format(op)
    }

    companion object {
        fun parse(bytes: ByteArray): DrbDatabase = DrbDatabase(bytes)
    }
}
