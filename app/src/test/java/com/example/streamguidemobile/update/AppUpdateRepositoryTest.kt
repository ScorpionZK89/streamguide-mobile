package com.example.streamguidemobile.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateRepositoryTest {
    @Test
    fun newerSemanticVersionsAreDetected() {
        assertTrue(isNewerVersion("0.2.1", "0.2.0"))
        assertTrue(isNewerVersion("v1.0.0", "0.9.9"))
        assertTrue(isNewerVersion("1.10.0", "1.9.9"))
    }

    @Test
    fun equalOlderAndInvalidVersionsAreRejected() {
        assertFalse(isNewerVersion("0.2.0", "0.2.0"))
        assertFalse(isNewerVersion("0.1.9", "0.2.0"))
        assertFalse(isNewerVersion("nieuw", "0.2.0"))
    }

    @Test
    fun stableVersionIsNewerThanPrerelease() {
        assertTrue(isNewerVersion("1.0.0", "1.0.0-beta1"))
        assertFalse(isNewerVersion("1.0.0-beta1", "1.0.0"))
    }
}
