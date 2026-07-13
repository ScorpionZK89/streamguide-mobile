package com.example.streamguidemobile.ui.live

import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.data.StreamResolution
import org.junit.Assert.assertEquals
import org.junit.Test

class LiveGuideComponentsTest {
    @Test
    fun usesMeasuredResolutionInsteadOfChannelName() {
        assertEquals(null, qualityBadgeFor(null))
        assertEquals("HD", qualityBadgeFor(StreamResolution(1280, 720)))
        assertEquals("FHD", qualityBadgeFor(StreamResolution(1920, 1080)))
        assertEquals("4K", qualityBadgeFor(StreamResolution(3840, 2160)))
    }

    @Test
    fun clampsProgramProgress() {
        val program = program(1_000L, 2_000L)

        assertEquals(0f, programProgress(program, 500L))
        assertEquals(0.5f, programProgress(program, 1_500L))
        assertEquals(1f, programProgress(program, 3_000L))
    }

    @Test
    fun reportsRemainingAndUpcomingMinutes() {
        val program = program(60_000L, 180_000L)

        assertEquals("Over 1 min", remainingLabel(program, 1L))
        assertEquals("2 min resterend", remainingLabel(program, 60_001L))
        assertEquals("Afgelopen", remainingLabel(program, 180_000L))
    }

    private fun program(start: Long, end: Long) = ProgramEntity(
        id = 1,
        playlistId = 1,
        channelTvgId = "channel",
        title = "Program",
        description = null,
        startTime = start,
        endTime = end,
        category = null,
        iconUrl = null
    )
}
