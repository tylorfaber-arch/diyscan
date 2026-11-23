package dev.diyscan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateCheckerTest {
    @Test fun buildTags() {
        assertEquals(12, UpdateChecker.buildNumber("build-12"))
        assertEquals(140, UpdateChecker.buildNumber("build-140"))
        assertNull(UpdateChecker.buildNumber("v1.2"))
        assertNull(UpdateChecker.buildNumber("12"))
        assertNull(UpdateChecker.buildNumber("build-"))
    }
}
