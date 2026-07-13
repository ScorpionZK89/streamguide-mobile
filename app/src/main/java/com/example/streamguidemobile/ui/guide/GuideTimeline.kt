package com.example.streamguidemobile.ui.guide

import com.example.streamguidemobile.data.ProgramEntity

internal data class TimelineSegment(
    val program: ProgramEntity,
    val startMinutes: Float,
    val durationMinutes: Float
)

internal fun buildTimelineSegments(
    programs: List<ProgramEntity>,
    dayStart: Long,
    dayEnd: Long
): List<TimelineSegment> {
    if (dayEnd <= dayStart) return emptyList()
    val clipped = programs.asSequence()
        .filter { it.endTime > it.startTime && it.endTime > dayStart && it.startTime < dayEnd }
        .map { program ->
            val clippedStart = program.startTime.coerceAtLeast(dayStart)
            val clippedEnd = program.endTime.coerceAtMost(dayEnd)
            ClippedProgram(program, clippedStart, clippedEnd)
        }
        .sortedWith(compareBy<ClippedProgram> { it.start }.thenBy { it.end })
        .toList()

    val normalized = mutableListOf<ClippedProgram>()
    clipped.forEach { candidate ->
        val previous = normalized.lastOrNull()
        if (previous != null && previous.end > candidate.start) {
            if (candidate.start > previous.start) {
                normalized[normalized.lastIndex] = previous.copy(end = candidate.start)
            } else {
                normalized.removeAt(normalized.lastIndex)
            }
        }
        if (candidate.end > candidate.start) normalized += candidate
    }

    return normalized.map { item ->
        TimelineSegment(
            program = item.program,
            startMinutes = (item.start - dayStart) / 60_000f,
            durationMinutes = ((item.end - item.start) / 60_000f).coerceAtLeast(1f)
        )
    }
}

internal fun currentTimeMinutes(nowMillis: Long, dayStart: Long, dayEnd: Long): Float? =
    nowMillis.takeIf { it in dayStart until dayEnd }?.let { (it - dayStart) / 60_000f }

private data class ClippedProgram(val program: ProgramEntity, val start: Long, val end: Long)
