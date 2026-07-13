package com.example.streamguidemobile.data

import java.util.ArrayDeque

internal class ExistingMovieMatcher(movies: List<MovieEntity>) {
    private val usedIds = mutableSetOf<Long>()
    private val byProviderId = movies.bucketBy { it.providerId.normalizedMovieKey() }
    private val byStreamUrl = movies.bucketBy { it.streamUrl.trim().takeIf(String::isNotEmpty) }
    private val byTitleAndCategory = movies.bucketBy {
        "${it.title.normalizedMovieKey()}|${it.categoryName.normalizedMovieKey()}"
    }

    fun match(movie: ParsedMovie): MovieEntity? {
        val titleAndCategory = "${movie.title.normalizedMovieKey()}|${movie.categoryName.normalizedMovieKey()}"
        return take(movie.providerId.normalizedMovieKey()?.let(byProviderId::get))
            ?: take(movie.streamUrl.trim().takeIf(String::isNotEmpty)?.let(byStreamUrl::get))
            ?: take(byTitleAndCategory[titleAndCategory])
    }

    private fun take(candidates: ArrayDeque<MovieEntity>?): MovieEntity? {
        while (candidates?.isNotEmpty() == true) {
            val candidate = candidates.removeFirst()
            if (usedIds.add(candidate.id)) return candidate
        }
        return null
    }
}

internal fun ParsedMovie.toMovieEntity(playlistId: Long, sortOrder: Int, previous: MovieEntity?): MovieEntity =
    MovieEntity(
        id = previous?.id ?: 0L,
        playlistId = playlistId,
        providerId = providerId,
        title = title,
        originalTitle = originalTitle ?: previous?.originalTitle,
        streamUrl = streamUrl,
        categoryId = categoryId,
        categoryName = categoryName,
        posterUrl = posterUrl ?: previous?.posterUrl,
        backdropUrl = backdropUrl ?: previous?.backdropUrl,
        year = year ?: previous?.year,
        durationMinutes = durationMinutes ?: previous?.durationMinutes,
        genre = genre ?: previous?.genre,
        ageRating = ageRating ?: previous?.ageRating,
        description = description ?: previous?.description,
        rating = rating ?: previous?.rating,
        director = director ?: previous?.director,
        cast = cast ?: previous?.cast,
        trailerUrl = trailerUrl ?: previous?.trailerUrl,
        addedAt = addedAt ?: previous?.addedAt,
        containerExtension = containerExtension ?: previous?.containerExtension,
        isFavorite = previous?.isFavorite ?: false,
        lastWatchedAt = previous?.lastWatchedAt,
        playbackPositionMs = previous?.playbackPositionMs ?: 0L,
        playbackDurationMs = previous?.playbackDurationMs ?: 0L,
        isWatched = previous?.isWatched ?: false,
        resolutionWidth = previous?.resolutionWidth,
        resolutionHeight = previous?.resolutionHeight,
        sortOrder = sortOrder
    )

internal fun ParsedChannel.toParsedMovie(): ParsedMovie = ParsedMovie(
    providerId = tvgId,
    title = name,
    streamUrl = streamUrl,
    categoryId = null,
    categoryName = groupTitle,
    posterUrl = logoUrl,
    year = year,
    genre = genre,
    description = description,
    containerExtension = streamUrl.substringAfterLast('.', "").substringBefore('?').takeIf { it.length in 2..5 }
)

internal fun MovieEntity.withDetails(details: ParsedMovieDetails): MovieEntity = copy(
    originalTitle = details.originalTitle ?: originalTitle,
    posterUrl = details.posterUrl ?: posterUrl,
    backdropUrl = details.backdropUrl ?: backdropUrl,
    year = details.year ?: year,
    durationMinutes = details.durationMinutes ?: durationMinutes,
    genre = details.genre ?: genre,
    ageRating = details.ageRating ?: ageRating,
    description = details.description ?: description,
    rating = details.rating ?: rating,
    director = details.director ?: director,
    cast = details.cast ?: cast,
    trailerUrl = details.trailerUrl ?: trailerUrl
)

fun MovieEntity.qualityBadge(): String? = streamResolutionBadge(
    resolutionWidth?.let { width -> resolutionHeight?.let { height -> StreamResolution(width, height) } }
)

fun MovieEntity.asPlaybackChannel(): ChannelEntity = ChannelEntity(
    id = -id.coerceAtLeast(1L),
    playlistId = playlistId,
    name = title,
    tvgId = "movie:$id",
    tvgName = title,
    logoUrl = posterUrl,
    groupTitle = categoryName,
    streamUrl = streamUrl,
    isFavorite = isFavorite,
    lastWatchedAt = lastWatchedAt,
    sortOrder = sortOrder
)

private fun List<MovieEntity>.bucketBy(keySelector: (MovieEntity) -> String?): Map<String, ArrayDeque<MovieEntity>> {
    val buckets = mutableMapOf<String, ArrayDeque<MovieEntity>>()
    forEach { movie -> keySelector(movie)?.let { key -> buckets.getOrPut(key, ::ArrayDeque).addLast(movie) } }
    return buckets
}

private fun String?.normalizedMovieKey(): String? = this?.trim()?.lowercase()?.takeIf(String::isNotEmpty)
