package com.example.streamguidemobile.ui.home

import com.example.streamguidemobile.ChannelRowState
import com.example.streamguidemobile.data.streamResolutionBadge

data class HomeContent(
    val hero: ChannelRowState?,
    val continueWatching: List<ChannelRowState>,
    val liveNow: List<ChannelRowState>,
    val recommended: List<ChannelRowState>
)

fun buildHomeContent(rows: List<ChannelRowState>): HomeContent {
    val recent = rows
        .filter { it.channel.lastWatchedAt != null }
        .sortedByDescending { it.channel.lastWatchedAt }
        .take(10)
    val live = rows.filter { it.currentProgram != null }.take(12).ifEmpty { rows.take(12) }
    val recommended = (rows.filter { it.channel.isFavorite } + live + rows)
        .distinctBy { it.channel.id }
        .filterNot { candidate -> recent.any { it.channel.id == candidate.channel.id } }
        .take(12)
    val hero = live.firstOrNull { it.currentProgram?.iconUrl != null }
        ?: recent.firstOrNull()
        ?: live.firstOrNull()
        ?: rows.firstOrNull()

    return HomeContent(
        hero = hero,
        continueWatching = recent,
        liveNow = live,
        recommended = recommended
    )
}

fun qualityBadge(row: ChannelRowState): String? {
    return streamResolutionBadge(row.streamResolution)
}
