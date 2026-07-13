package com.example.streamguidemobile.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SeriesMetadataTest {
    @Test
    fun refreshKeepsFavoriteAndSeriesProgress() {
        val previous = series(id = 42L, favorite = true, progressEpisodeId = 7L, progressOrder = 8)
        val parsed = ParsedSeries(providerId = "series-1", title = "Nieuwe titel", categoryId = "2", categoryName = "Drama")

        val refreshed = parsed.toSeriesEntity(1L, 3, ExistingSeriesMatcher(listOf(previous)).match(parsed))

        assertEquals(42L, refreshed.id)
        assertEquals(true, refreshed.isFavorite)
        assertEquals(7L, refreshed.progressEpisodeId)
        assertEquals(8, refreshed.progressOrder)
        assertEquals("Nieuwe titel", refreshed.title)
    }

    @Test
    fun normalizationOrdersNormalSeasonsBeforeSpecialsAndUnknown() {
        val parsed = listOf(
            parsedEpisode("special", 0, 1, 0),
            parsedEpisode("s2e1", 2, 1, 1),
            parsedEpisode("unknown", -1, null, 2),
            parsedEpisode("s1e2", 1, 2, 3),
            parsedEpisode("s1e1", 1, 1, 4)
        )

        val normalized = parsedEpisodesToEntities(10L, parsed, emptyList())

        assertEquals(listOf("s1e1", "s1e2", "s2e1", "special", "unknown"), normalized.map { it.providerId })
        assertEquals(listOf(0, 1, 2, 3, 4), normalized.map { it.sortOrder })
    }

    @Test
    fun episodeRefreshKeepsPlaybackAndMeasuredResolution() {
        val old = episode(id = 9L, providerId = "episode-2", season = 1, number = 2, position = 12_000L, watched = false)
            .copy(playbackDurationMs = 40_000L, resolutionWidth = 1920, resolutionHeight = 1080)

        val refreshed = parsedEpisodesToEntities(1L, listOf(parsedEpisode("episode-2", 1, 2, 0)), listOf(old)).single()

        assertEquals(9L, refreshed.id)
        assertEquals(12_000L, refreshed.playbackPositionMs)
        assertEquals(40_000L, refreshed.playbackDurationMs)
        assertEquals("FHD", refreshed.qualityBadge())
    }

    @Test
    fun nextEpisodeSkipsUnavailableStreams() {
        val current = episode(1L, "one", 1, 1, 0L, true)
        val unavailable = episode(2L, "two", 1, 2, 0L, false).copy(streamUrl = null)
        val nextSeason = episode(3L, "three", 2, 1, 0L, false)

        assertEquals(nextSeason, listOf(nextSeason, unavailable, current).nextPlayableEpisode(current))
        assertNull(listOf(current, unavailable).nextPlayableEpisode(current))
    }

    private fun parsedEpisode(providerId: String, season: Int, number: Int?, order: Int) = ParsedEpisode(
        providerId, season, labelFor(season), number, order, providerId,
        "https://example.com/$providerId.mp4", null, null, 45, "mp4", null
    )

    private fun series(id: Long, favorite: Boolean, progressEpisodeId: Long?, progressOrder: Int) = SeriesEntity(
        id = id, playlistId = 1L, providerId = "series-1", title = "Oud", categoryName = "Drama",
        isFavorite = favorite, progressEpisodeId = progressEpisodeId, progressOrder = progressOrder, sortOrder = 0
    )

    private fun episode(id: Long, providerId: String, season: Int, number: Int?, position: Long, watched: Boolean) = EpisodeEntity(
        id = id, seriesId = 1L, providerId = providerId, seasonNumber = season, seasonName = labelFor(season),
        episodeNumber = number, providerOrder = number ?: 0, title = providerId,
        streamUrl = "https://example.com/$providerId.mp4", playbackPositionMs = position,
        isWatched = watched, sortOrder = number ?: 0
    )

    private fun labelFor(season: Int) = when (season) { 0 -> "Specials"; -1 -> "Afleveringen"; else -> "Seizoen $season" }
}
