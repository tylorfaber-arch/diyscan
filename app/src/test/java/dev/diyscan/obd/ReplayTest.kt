package dev.diyscan.obd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayTest {

    @Test fun parsesTheCatalogToolOutput() {
        val plan = Replay.parsePlan(
            """
            # module request
            7E1 221940
            7E0 010C
            010C
            """.trimIndent(),
        )
        assertEquals(3, plan.size)
        assertEquals(ReplayRequest("7E1", "221940"), plan[0])
        assertEquals(ReplayRequest(null, "010C"), plan[2])
    }

    @Test fun ignoresJunkAndDuplicates() {
        val plan = Replay.parsePlan(
            """
            7E1 221940
            7E1 221940
            nonsense
            zz 221940
            7E1 22194
            """.trimIndent(),
        )
        assertEquals(listOf(ReplayRequest("7E1", "221940")), plan)
    }

    @Test fun onlyReadServicesAreAllowed() {
        listOf("22F190", "010C", "0902", "03", "020200", "21FF").forEach {
            assertTrue(it, Replay.isReadOnly(it))
        }
        // clear codes, actuator control, write, session, seed/key
        listOf("04", "2EF19001", "2711", "3101A1", "1003", "3E").forEach {
            assertFalse(it, Replay.isReadOnly(it))
        }
    }

    @Test fun sweepBuildsOneRequestPerPidPerModule() {
        val plan = Replay.sweep(listOf("7E0", "7E1"), 0x1900, 0x1902)
        assertEquals(6, plan.size)
        assertEquals(ReplayRequest("7E0", "221900"), plan[0])
        assertEquals(ReplayRequest("7E1", "221902"), plan[5])
        assertEquals("2219FF", Replay.sweep(listOf("7E0"), 0x19FF, 0x19FF)[0].request)
    }

    @Test fun emptyRangeBuildsNothing() {
        assertTrue(Replay.sweep(listOf("7E0"), 0x10, 0x0F).isEmpty())
    }

    @Test fun payloadJoinsReplyLines() {
        val r = ReplayResult("7E0", "0902", lines = listOf("49 02 01", "31 47 31"))
        assertTrue(r.answered)
        assertEquals("490201314731", r.payload)
    }

    @Test fun csvQuotesRepliesWithPipesAndMissingAnswers() {
        val csv = Replay.toCsv(listOf(
            ReplayResult("7E1", "221940", lines = listOf("6219405A")),
            ReplayResult("7E0", "010C", error = "no answer"),
        ))
        val lines = csv.trim().lines()
        assertEquals("module,request,reply,error,skipped", lines[0])
        assertEquals("7E1,221940,6219405A,,", lines[1])
        assertEquals("7E0,010C,,no answer,", lines[2])
    }
}
