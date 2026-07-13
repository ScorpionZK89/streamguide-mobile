package com.example.streamguidemobile.ui.home

import com.example.streamguidemobile.ChannelRowState
import com.example.streamguidemobile.data.ChannelEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeModelsTest {
    @Test
    fun recentItemsAreOrderedByLastWatchedTime() {
        val rows = listOf(row(1, watchedAt = 10), row(2, watchedAt = 30), row(3, watchedAt = 20))

        val content = buildHomeContent(rows)

        assertEquals(listOf(2L, 3L, 1L), content.continueWatching.map { it.channel.id })
    }

    @Test
    fun recommendationsDoNotRepeatRecentItems() {
        val rows = listOf(row(1, watchedAt = 10, favorite = true), row(2, favorite = true), row(3))

        val content = buildHomeContent(rows)

        assertTrue(content.recommended.none { it.channel.id == 1L })
        assertEquals(2L, content.recommended.first().channel.id)
    }

    private fun row(id: Long, watchedAt: Long? = null, favorite: Boolean = false) = ChannelRowState(
        channel = ChannelEntity(
            id = id,
            playlistId = 1,
            name = "Kanaal $id",
            tvgId = "channel-$id",
            tvgName = null,
            logoUrl = null,
            groupTitle = "Live TV",
            streamUrl = "https://example.com/$id",
            isFavorite = favorite,
            lastWatchedAt = watchedAt,
            sortOrder = id.toInt()
        ),
        currentProgram = null,
        nextProgram = null,
        progress = 0f
    )
}
