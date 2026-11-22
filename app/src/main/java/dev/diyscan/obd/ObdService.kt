package dev.diyscan.obd

import dev.diyscan.elm.Elm327

data class DtcSet(
    val stored: List<String> = emptyList(),
    val pending: List<String> = emptyList(),
    val permanent: List<String> = emptyList(),
) {
    val total get() = (stored + pending + permanent).distinct().size
}

data class FreezeFrame(val dtc: String?, val values: List<Pair<Pid, Double>>)

/** High-level OBD operations on top of the ELM327 driver. */
class ObdService(private val elm: Elm327) {

    private val isCan get() = elm.protocol.isCan

    suspend fun supportedPids(): Set<Int> {
        val out = mutableSetOf<Int>()
        var base = 0x00
        while (base <= 0x80) {
            val set = Parsing.supportedPids(elm.request("01%02X".format(base)), base)
            if (set.isEmpty()) break
            out += set
            if ((base + 0x20) !in set) break // next range not supported
            base += 0x20
        }
        return out
    }

    suspend fun read(pid: Pid): Double? {
        if (pid.request != null) {
            val data = Parsing.customData(elm.request(pid.request, header = pid.header), pid.request, isCan) ?: return null
            return pid.decode(data).takeIf { !it.isNaN() && !it.isInfinite() }
        }
        val data = Parsing.mode01Data(elm.request("01${pid.hex}"), pid.code, pid.bytes) ?: return null
        return pid.decode(data)
    }

    suspend fun readiness(): Readiness? =
        Parsing.mode01Data(elm.request("0101"), 0x01, 4)?.let(Readiness::parse)

    suspend fun dtcs(): DtcSet = DtcSet(
        stored = Parsing.dtcs(elm.request("03", 8_000), 0x43, isCan),
        pending = Parsing.dtcs(elm.request("07", 8_000), 0x47, isCan),
        // Mode 0A is 2010+ only; older vehicles answer NO DATA, which is fine.
        permanent = runCatching { Parsing.dtcs(elm.request("0A", 8_000), 0x4A, isCan) }.getOrDefault(emptyList()),
    )

    /** Mode 04: clears codes, freeze frame, and resets readiness monitors. */
    suspend fun clearDtcs(): Boolean = elm.request("04", 10_000).any { it.uppercase().startsWith("44") }

    suspend fun freezeFrame(supported: Set<Int>): FreezeFrame? {
        val dtcData = Parsing.mode02Data(elm.request("020200"), 0x02, 2)
        val dtc = dtcData?.let { Parsing.decodeDtc(it[0], it[1]) }
        if (dtc == null) return null
        val wanted = listOf(Pids.RPM, Pids.SPEED, Pids.COOLANT, Pids.ENGINE_LOAD, Pids.THROTTLE,
            Pids.MAP, Pids.STFT_B1, Pids.LTFT_B1, Pids.STFT_B2, Pids.LTFT_B2, Pids.IAT, Pids.TIMING)
            .filter { it.code in supported }
        val values = wanted.mapNotNull { p ->
            Parsing.mode02Data(elm.request("02${p.hex}00"), p.code, p.bytes)?.let { p to p.decode(it) }
        }
        return FreezeFrame(dtc, values)
    }

    suspend fun vin(): String? = runCatching { Parsing.vin(elm.request("0902", 8_000), isCan) }.getOrNull()
}
