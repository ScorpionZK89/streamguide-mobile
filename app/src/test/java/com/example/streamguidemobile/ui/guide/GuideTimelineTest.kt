package com.example.streamguidemobile.ui.guide

import com.example.streamguidemobile.data.ProgramEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuideTimelineTest {
    @Test
    fun clipsProgramsThatCrossMidnight() {
        val dayStart = 1_000_000L
        val dayEnd = dayStart + 24L * 60L * 60L * 1000L
        val programs = listOf(
            program(1, dayStart - 30 * 60_000L, dayStart + 30 * 60_000L),
            program(2, dayEnd - 45 * 60_000L, dayEnd + 30 * 60_000L)
        )

        val segments = buildTimelineSegments(programs, dayStart, dayEnd)

        assertEquals(2, segments.size)
        assertEquals(0f, segments[0].startMinutes)
        assertEquals(30f, segments[0].durationMinutes)
        assertEquals(24f * 60f - 45f, segments[1].startMinutes)
        assertEquals(45f, segments[1].durationMinutes)
    }

    @Test
    fun ignoresInvalidAndOutOfWindowPrograms() {
        val dayStart = 5_000_000L
        val dayEnd = dayStart + 60 * 60_000L
        val programs = listOf(
            program(1, dayStart + 10_000L, dayStart + 10_000L),
            program(2, dayStart - 60_000L, dayStart - 1L),
            program(3, dayEnd + 1L, dayEnd + 60_000L)
        )

        assertEquals(emptyList<TimelineSegment>(), buildTimelineSegments(programs, dayStart, dayEnd))
    }

    @Test
    fun clipsOverlappingProviderProgramsAtTheNextStart() {
        val dayStart = 1_000_000L
        val dayEnd = dayStart + 4 * 60 * 60_000L
        val programs = listOf(
            program(1, dayStart, dayStart + 120 * 60_000L),
            program(2, dayStart + 60 * 60_000L, dayStart + 90 * 60_000L),
            program(3, dayStart + 90 * 60_000L, dayStart + 150 * 60_000L)
        )

        val segments = buildTimelineSegments(programs, dayStart, dayEnd)

        assertEquals(listOf(60f, 30f, 60f), segments.map { it.durationMinutes })
        assertEquals(listOf(0f, 60f, 90f), segments.map { it.startMinutes })
    }

    @Test
    fun currentTimeOnlyExistsInsideSelectedDay() {
        val dayStart = 10_000L
        val dayEnd = dayStart + 60 * 60_000L

        assertEquals(30f, currentTimeMinutes(dayStart + 30 * 60_000L, dayStart, dayEnd))
        assertNull(currentTimeMinutes(dayEnd, dayStart, dayEnd))
    }

    private fun program(id: Long, start: Long, end: Long) = ProgramEntity(
        id = id,
        playlistId = 1,
        channelTvgId = "channel",
        title = "Program $id",
        description = null,
        startTime = start,
        endTime = end,
        category = null,
        iconUrl = null
    )
}
