package dev.diyscan.obd

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileTest {

    private fun ev(f: String, vararg b: Int) = Equation.parse(f).eval(b)

    @Test fun arithmetic() {
        assertEquals(258.0, ev("A*256+B", 1, 2), 1e-9)
        assertEquals(212.0, ev("(A-40)*1.8+32", 140), 1e-9)
        assertEquals(7.0, ev("1+2*3"), 1e-9)
        assertEquals(9.0, ev("(1+2)*3"), 1e-9)
        assertEquals(-5.0, ev("-A", 5), 1e-9)
        assertEquals(8.0, ev("2^3"), 1e-9)
        assertEquals(25.5, ev("a/10", 255), 1e-9) // lowercase variables are fine
    }

    @Test fun functionsAndBits() {
        assertEquals(-28.0, ev("SIGNED(A)/2", 200), 1e-9)
        assertEquals(1.0, ev("{B:3}", 0, 8), 1e-9)
        assertEquals(0.0, ev("{B:2}", 0, 8), 1e-9)
        assertEquals(3.0, ev("ABS(A-B)", 2, 5), 1e-9)
        assertEquals(2.0, ev("MIN(A,B)", 2, 5), 1e-9)
    }

    @Test fun twoLetterVariables() {
        val b = IntArray(30) { it }
        assertEquals(26.0, Equation.parse("AA").eval(b), 1e-9)
        assertEquals(27, Equation.parse("AA+B").bytesNeeded)
    }

    @Test fun shortReplyIsNaN() {
        assertTrue(ev("A*256+B", 1).isNaN())
    }

    @Test(expected = IllegalArgumentException::class) fun unknownFunction() { Equation.parse("FOO(A)") }
    @Test(expected = IllegalArgumentException::class) fun trailingJunk() { Equation.parse("A+") }

    @Test fun torqueCsv() {
        val csv = """
            Name,ShortName,ModeAndPID,Equation,Min Value,Max Value,Units,Header
            "Trans Fluid Temp","TFT","221940","A-40","-40","150","°C","7E1"
            "Weird, Name","W","0x22 11 BB","A*256+B",0,65535,"rpm",""
            "Broken","B","22XYZ","A",0,1,"",""
            "Bad eq","BE","221234","VAL{X}",0,1,"",""
        """.trimIndent()
        val p = PidProfileParser.parse("test", csv)
        assertEquals(2, p.pids.size)
        assertEquals(2, p.skipped.size)
        val tft = p.pids[0]
        assertEquals("Trans Fluid Temp", tft.name)
        assertEquals("221940", tft.request)
        assertEquals("7E1", tft.header)
        assertEquals(50.0, tft.decode(intArrayOf(90)), 1e-9)
        assertEquals("Weird, Name", p.pids[1].name)
        assertEquals("2211BB", p.pids[1].request)
        assertNull(p.pids[1].header)
    }

    @Test fun semicolonCsvWithoutHeader() {
        val p = PidProfileParser.parse("t", "Oil Pressure;OilP;221470;A*4;0;1000;kPa;7E0\n")
        assertEquals(1, p.pids.size)
        assertEquals("7E0", p.pids[0].header)
    }

    @Test fun customReplies() {
        assertArrayEquals(intArrayOf(0x5A), Parsing.customData(listOf("6219405A"), "221940", isCan = true))
        assertNull(Parsing.customData(listOf("7F2231"), "221940", isCan = true))
        assertArrayEquals(intArrayOf(1, 2, 3, 4, 5, 6, 7, 8), Parsing.customData(
            listOf("00B", "0:62F190010203", "1:0405060708"), "22F190", isCan = true))
    }
}
