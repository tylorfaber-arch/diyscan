package dev.diyscan.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParsingTest {

    @Test fun rpm() {
        val d = Parsing.mode01Data(listOf("410C1AF8"), 0x0C, 2)!!
        assertEquals(1726.0, Pids.RPM.decode(d), 0.01)
    }

    @Test fun coolantImperial() {
        val d = Parsing.mode01Data(listOf("41057B"), 0x05, 1)!!
        val c = Pids.COOLANT.decode(d)
        assertEquals(83.0, c, 0.01)
        assertEquals(181.4, Pids.COOLANT.toImperial(c), 0.1)
    }

    @Test fun supportedPids() {
        val s = Parsing.supportedPids(listOf("4100BE3EB811"), 0x00)
        assertTrue(0x01 in s); assertTrue(0x0C in s); assertTrue(0x20 in s)
        assertFalse(0x02 in s)
    }

    @Test fun dtcLegacyJ1850() {
        // Two ECU lines, 3 codes each, zero padded
        val codes = Parsing.dtcs(listOf("43012804560000", "43017403000000"), 0x43, isCan = false)
        assertEquals(listOf("P0128", "P0456", "P0174", "P0300"), codes)
    }

    @Test fun dtcCanSingleFrame() {
        assertEquals(listOf("P0171", "C0035"), Parsing.dtcs(listOf("4302017140 35".replace(" ", "")), 0x43, isCan = true))
    }

    @Test fun dtcCanMultiFrame() {
        val lines = listOf("00A", "0:430403010302", "1:03030304000000")
        assertEquals(listOf("P0301", "P0302", "P0303", "P0304"), Parsing.dtcs(lines, 0x43, isCan = true))
    }

    @Test fun noCodes() {
        assertTrue(Parsing.dtcs(listOf("43000000000000"), 0x43, isCan = false).isEmpty())
        assertTrue(Parsing.dtcs(listOf("4300"), 0x43, isCan = true).isEmpty())
    }

    @Test fun decodeLetters() {
        assertEquals("P0420", Parsing.decodeDtc(0x04, 0x20))
        assertEquals("C1234", Parsing.decodeDtc(0x52, 0x34))
        assertEquals("B0001", Parsing.decodeDtc(0x80, 0x01))
        assertEquals("U0100", Parsing.decodeDtc(0xC1, 0x00))
    }

    @Test fun vinLegacy() {
        val vin = "1D7HA18N05S000001"
        val bytes = listOf(0, 0, 0) + vin.map { it.code }
        val lines = bytes.chunked(4).mapIndexed { i, c -> "4902%02X".format(i + 1) + c.joinToString("") { "%02X".format(it) } }
        assertEquals(vin, Parsing.vin(lines, isCan = false))
    }

    @Test fun vinCan() {
        val vin = "1FTFW1ET5DFC10312"
        val hex = "490201" + vin.map { "%02X".format(it.code) }.joinToString("")
        val lines = listOf("014") + hex.chunked(14).mapIndexed { i, c -> "${i.toString(16).uppercase()}:$c" }
        assertEquals(vin, Parsing.vin(lines, isCan = true))
    }

    @Test fun readiness() {
        val r = Readiness.parse(intArrayOf(0x82, 0x07, 0x65, 0x04))
        assertTrue(r.milOn); assertEquals(2, r.dtcCount)
        assertEquals(1, r.incomplete)
        assertFalse(r.monitors.first { it.name == "EVAP system" }.complete)
    }
}
