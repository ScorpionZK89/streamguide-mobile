package com.example.streamguidemobile.ui.movies

import com.example.streamguidemobile.data.MovieEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MovieModelsTest {
    @Test
    fun libraryUsesRealCategoriesAndLocalMetadataSearch() {
        val movies = listOf(movie(1, "Northern Light", "Drama", "A. Director"), movie(2, "Deep Current", "Adventure", null))

        val library = buildMovieLibrary(movies, "director", CATEGORY_ALL, MovieFilters(), MovieSort.TitleAscending)

        assertEquals(listOf("Alle films", "Drama", "Adventure").sorted().toSet(), library.categories.map { it.label }.toSet())
        assertEquals(listOf("Northern Light"), library.movies.map { it.title })
    }

    @Test
    fun continueAndWatchlistCategoriesOnlyExistWhenTheyHaveContent() {
        val movies = listOf(movie(1, "Started", "Drama", null, favorite = true, position = 12_000L))

        val library = buildMovieLibrary(movies, "", CATEGORY_ALL, MovieFilters(), MovieSort.RecentlyAdded)

        assertTrue(library.categories.any { it.key == CATEGORY_CONTINUE })
        assertTrue(library.categories.any { it.key == CATEGORY_WATCHLIST })
    }

    @Test
    fun filtersAndSortStayDeterministic() {
        val movies = listOf(movie(1, "Older", "Drama", null, year = 2020), movie(2, "Newer", "Drama", null, year = 2024))

        val library = buildMovieLibrary(movies, "", CATEGORY_ALL, MovieFilters(genre = "Drama"), MovieSort.YearDescending)

        assertEquals(listOf("Newer", "Older"), library.movies.map { it.title })
    }

    @Test
    fun hiddenMovieGroupsAreRemovedCaseInsensitively() {
        val movies = listOf(movie(1, "Visible", "Drama", null), movie(2, "Hidden", "Horror", null))

        val library = buildMovieLibrary(
            movies, "", CATEGORY_ALL, MovieFilters(), MovieSort.TitleAscending, hiddenGroups = setOf("hOrRoR")
        )

        assertEquals(listOf("Visible"), library.movies.map { it.title })
        assertEquals(listOf("Alle films", "Drama"), library.categories.map { it.label })
        assertEquals(listOf("Drama", "Horror"), library.allGroups)
    }

    @Test
    fun hidingEveryGroupKeepsSourceInformationForRecovery() {
        val movies = listOf(movie(1, "One", "Drama", null), movie(2, "Two", "Horror", null))

        val library = buildMovieLibrary(
            movies, "", CATEGORY_ALL, MovieFilters(), MovieSort.TitleAscending, hiddenGroups = setOf("Drama", "Horror")
        )

        assertEquals(2, library.sourceMovieCount)
        assertTrue(library.allMovies.isEmpty())
        assertEquals(setOf("Drama", "Horror"), library.hiddenGroups)
    }

    private fun movie(
        id: Long,
        title: String,
        genre: String,
        director: String?,
        favorite: Boolean = false,
        position: Long = 0L,
        year: Int = 2023
    ) = MovieEntity(
        id = id, playlistId = 1L, providerId = "vod-$id", title = title, originalTitle = null,
        streamUrl = "https://example.com/$id.mp4", categoryId = genre, categoryName = genre,
        posterUrl = null, backdropUrl = null, year = year, durationMinutes = 100, genre = genre,
        ageRating = null, description = null, rating = null, director = director, cast = null, trailerUrl = null,
        addedAt = null, containerExtension = "mp4", isFavorite = favorite,
        lastWatchedAt = position.takeIf { it > 0L }, playbackPositionMs = position,
        playbackDurationMs = if (position > 0L) 100_000L else 0L, isWatched = false,
        resolutionWidth = null, resolutionHeight = null, sortOrder = id.toInt()
    )
}
