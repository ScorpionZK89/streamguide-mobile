package com.example.streamguidemobile.data

import org.junit.Assert.assertEquals
import org.junit.Test

class MovieMetadataTest {
    @Test
    fun refreshedMovieKeepsFavoriteAndPlaybackProgress() {
        val previous = movie(id = 44L, favorite = true, position = 42_000L)
        val parsed = ParsedMovie(
            providerId = "vod-7",
            title = "Updated title",
            streamUrl = "https://example.com/new-token.mp4",
            categoryId = "3",
            categoryName = "Drama",
            posterUrl = "poster-new"
        )

        val refreshed = parsed.toMovieEntity(1L, 2, ExistingMovieMatcher(listOf(previous)).match(parsed))

        assertEquals(44L, refreshed.id)
        assertEquals(true, refreshed.isFavorite)
        assertEquals(42_000L, refreshed.playbackPositionMs)
        assertEquals("Updated title", refreshed.title)
    }

    private fun movie(id: Long, favorite: Boolean, position: Long) = MovieEntity(
        id = id, playlistId = 1L, providerId = "vod-7", title = "Old title", originalTitle = null,
        streamUrl = "https://example.com/old-token.mp4", categoryId = "3", categoryName = "Drama",
        posterUrl = "poster-old", backdropUrl = null, year = 2023, durationMinutes = 100, genre = "Drama",
        ageRating = null, description = null, rating = null, director = null, cast = null, trailerUrl = null,
        addedAt = null, containerExtension = "mp4", isFavorite = favorite, lastWatchedAt = 1_000L,
        playbackPositionMs = position, playbackDurationMs = 6_000_000L, isWatched = false,
        resolutionWidth = null, resolutionHeight = null, sortOrder = 0
    )
}
