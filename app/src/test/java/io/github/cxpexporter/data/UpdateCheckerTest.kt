package io.github.cxpexporter.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun newerWithLeadingVIsDetected() {
        assertTrue(isNewer("v1.1.0", "1.0.0"))
    }

    @Test
    fun equalVersionsAreNotNewer() {
        assertFalse(isNewer("1.0.0", "1.0.0"))
        assertFalse(isNewer("v1.0.0", "1.0.0"))
    }

    @Test
    fun olderVersionIsNotNewer() {
        assertFalse(isNewer("1.0.0", "1.1.0"))
    }

    @Test
    fun missingPartsCountAsZero() {
        assertTrue(isNewer("1.0.1", "1.0"))
        assertFalse(isNewer("1.0", "1.0.0"))
    }

    @Test
    fun garbageIsNeverNewer() {
        assertFalse(isNewer("garbage", "1.0.0"))
        assertFalse(isNewer("1.0.0", "garbage"))
        assertFalse(isNewer("", "1.0.0"))
        assertFalse(isNewer("1.x.0", "1.0.0"))
    }
}
