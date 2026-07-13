package com.example.streamguidemobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelMetadataTest {
    @Test
    fun resolutionBadgeUsesMeasuredHeight() {
        assertNull(streamResolutionBadge(null))
        assertEquals("SD", streamResolutionBadge(StreamResolution(720, 576)))
        assertEquals("HD", streamResolutionBadge(StreamResolution(1280, 720)))
        assertEquals("FHD", streamResolutionBadge(StreamResolution(1920, 1080)))
        assertEquals("4K", streamResolutionBadge(StreamResolution(3840, 2160)))
    }

    @Test
    fun refreshedChannelKeepsIdentityHistoryAndFavorites() {
        val previous = channel(
            id = 42,
            watchedAt = 1234,
            favorite = true
        )
        val parsed = ParsedChannel(
            name = "Nieuwe naam",
            tvgId = "npo-1",
            tvgName = "Nieuwe naam",
            logoUrl = "logo-new",
            groupTitle = "Nederland",
            streamUrl = "https://example.com/new-token"
        )

        val refreshed = parsed.toChannelEntity(
            playlistId = 1,
            sortOrder = 7,
            previous = ExistingChannelMatcher(listOf(previous)).match(parsed)
        )

        assertEquals(42L, refreshed.id)
        assertEquals(1234L, refreshed.lastWatchedAt)
        assertEquals(true, refreshed.isFavorite)
        assertEquals("Nieuwe naam", refreshed.name)
    }

    private fun channel(
        id: Long,
        watchedAt: Long?,
        favorite: Boolean
    ) = ChannelEntity(
        id = id,
        playlistId = 1,
        name = "NPO 1",
        tvgId = "npo-1",
        tvgName = "NPO 1",
        logoUrl = "logo-old",
        groupTitle = "Nederland",
        streamUrl = "https://example.com/old-token",
        isFavorite = favorite,
        lastWatchedAt = watchedAt,
        sortOrder = 0
    )
}
