package com.example.streamguidemobile.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GroupVisibilityTest {
    @Test
    fun hiddenGroupMatchingIsCaseInsensitive() {
        assertFalse(isGroupVisible("Sport", setOf("SPORT")))
    }

    @Test
    fun groupsNotInTheFilterRemainVisible() {
        assertTrue(isGroupVisible("Nieuws", setOf("Sport")))
    }
}
