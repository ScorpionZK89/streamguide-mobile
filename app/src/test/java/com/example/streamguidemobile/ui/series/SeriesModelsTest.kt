package com.example.streamguidemobile.ui.series

import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.SeriesEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeriesModelsTest {
    @Test
    fun libraryUsesProviderCategoriesAndSearchesRealMetadata() {
        val source = listOf(
            series(1L, "Northern Signal", "Drama", "A. Maker"),
            series(2L, "Deep Current", "Documentaire", null)
        )

        val library = buildSeriesLibrary(source, emptyList(), "maker", SERIES_ALL, SeriesFilters(), SeriesSort.TitleAscending)

        assertEquals(setOf("Alle series", "Drama", "Documentaire"), library.categories.map { it.label }.toSet())
        assertEquals(listOf("Northern Signal"), library.series.map { it.series.title })
    }

    @Test
    fun specialsDoNotInflateReliableSeasonCount() {
        val episodes = listOf(episode(1L, 0, 1), episode(2L, 1, 1), episode(3L, 2, 1))

        assertEquals(2, SeriesCardModel(series(1L, "Show", "Drama", null), episodes).seasonCount)
    }

    @Test
    fun completedEpisodeSelectsNextUnwatchedEpisode() {
        val episodes = listOf(
            episode(1L, 1, 1).copy(isWatched = true, lastWatchedAt = 10L),
            episode(2L, 1, 2)
        )
        val card = SeriesCardModel(series(1L, "Show", "Drama", null).copy(progressEpisodeId = 1L, progressOrder = 0), episodes)

        assertEquals(2L, card.primaryEpisode?.id)
        assertEquals("Volgende aflevering", card.actionLabel)
        assertFalse(card.isComplete)
        assertTrue(card.hasProgress)
    }

    @Test
    fun hiddenSeriesGroupsAreRemovedCaseInsensitively() {
        val source = listOf(
            series(1L, "Visible", "Drama", null),
            series(2L, "Hidden", "Misdaad", null)
        )
        val episodes = listOf(episode(1L, 1, 1), episode(2L, 1, 1).copy(seriesId = 2L))

        val library = buildSeriesLibrary(
            source, episodes, "", SERIES_ALL, SeriesFilters(), SeriesSort.TitleAscending,
            hiddenGroups = setOf("mIsDaAd")
        )

        assertEquals(listOf("Visible"), library.allSeries.map { it.series.title })
        assertEquals(listOf(1L), library.allEpisodes.map { it.seriesId })
        assertEquals(listOf("Alle series", "Drama"), library.categories.map { it.label })
        assertEquals(listOf("Drama", "Misdaad"), library.allGroups)
    }

    @Test
    fun hidingEverySeriesGroupKeepsSourceInformationForRecovery() {
        val source = listOf(series(1L, "One", "Drama", null), series(2L, "Two", "Misdaad", null))

        val library = buildSeriesLibrary(
            source, emptyList(), "", SERIES_ALL, SeriesFilters(), SeriesSort.TitleAscending,
            hiddenGroups = setOf("Drama", "Misdaad")
        )

        assertEquals(2, library.sourceSeriesCount)
        assertTrue(library.allSeries.isEmpty())
        assertEquals(setOf("Drama", "Misdaad"), library.hiddenGroups)
        assertEquals(listOf("Drama", "Misdaad"), library.allGroups)
    }

    private fun series(id: Long, title: String, category: String, cast: String?) = SeriesEntity(
        id = id, playlistId = 1L, providerId = "series-$id", title = title, categoryName = category,
        genre = category, cast = cast, sortOrder = id.toInt()
    )

    private fun episode(id: Long, season: Int, number: Int) = EpisodeEntity(
        id = id, seriesId = 1L, providerId = "episode-$id", seasonNumber = season,
        seasonName = "Seizoen $season", episodeNumber = number, providerOrder = number,
        title = "Aflevering $number", streamUrl = "https://example.com/$id.mp4", sortOrder = number
    )
}
