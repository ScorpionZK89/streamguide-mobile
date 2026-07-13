package com.example.streamguidemobile.data

import java.util.ArrayDeque

internal class ExistingSeriesMatcher(series: List<SeriesEntity>) {
    private val byProvider = series.associateBy { it.providerId.trim().lowercase() }
    fun match(item: ParsedSeries): SeriesEntity? = byProvider[item.providerId.trim().lowercase()]
}

internal fun ParsedSeries.toSeriesEntity(playlistId: Long, sortOrder: Int, previous: SeriesEntity?): SeriesEntity =
    SeriesEntity(
        id = previous?.id ?: 0L,
        playlistId = playlistId,
        providerId = providerId,
        title = title,
        originalTitle = originalTitle ?: previous?.originalTitle,
        categoryId = categoryId,
        categoryName = categoryName,
        posterUrl = posterUrl ?: previous?.posterUrl,
        backdropUrl = backdropUrl ?: previous?.backdropUrl,
        year = year ?: previous?.year,
        genre = genre ?: previous?.genre,
        ageRating = ageRating ?: previous?.ageRating,
        description = description ?: previous?.description,
        rating = rating ?: previous?.rating,
        director = director ?: previous?.director,
        cast = cast ?: previous?.cast,
        trailerUrl = trailerUrl ?: previous?.trailerUrl,
        addedAt = addedAt ?: previous?.addedAt,
        updatedAt = updatedAt ?: previous?.updatedAt,
        isFavorite = previous?.isFavorite ?: false,
        lastWatchedAt = previous?.lastWatchedAt,
        progressEpisodeId = previous?.progressEpisodeId,
        progressOrder = previous?.progressOrder ?: -1,
        detailLoadedAt = previous?.detailLoadedAt,
        sortOrder = sortOrder
    )

internal fun SeriesEntity.withDetails(details: ParsedSeriesDetails, loadedAt: Long): SeriesEntity = copy(
    originalTitle = details.originalTitle ?: originalTitle,
    posterUrl = details.posterUrl ?: posterUrl,
    backdropUrl = details.backdropUrl ?: backdropUrl,
    year = details.year ?: year,
    genre = details.genre ?: genre,
    ageRating = details.ageRating ?: ageRating,
    description = details.description ?: description,
    rating = details.rating ?: rating,
    director = details.director ?: director,
    cast = details.cast ?: cast,
    trailerUrl = details.trailerUrl ?: trailerUrl,
    updatedAt = details.updatedAt ?: updatedAt,
    detailLoadedAt = loadedAt
)

internal fun parsedEpisodesToEntities(
    seriesId: Long,
    parsed: List<ParsedEpisode>,
    previous: List<EpisodeEntity>
): List<EpisodeEntity> {
    val matcher = ExistingEpisodeMatcher(previous)
    return parsed.distinctBy { it.providerId }.sortedWith(parsedEpisodeComparator).mapIndexed { index, item ->
        val old = matcher.match(item)
        EpisodeEntity(
            id = old?.id ?: 0L,
            seriesId = seriesId,
            providerId = item.providerId,
            seasonNumber = item.seasonNumber,
            seasonName = item.seasonName,
            episodeNumber = item.episodeNumber,
            providerOrder = item.providerOrder,
            title = item.title,
            streamUrl = item.streamUrl,
            imageUrl = item.imageUrl,
            description = item.description,
            durationMinutes = item.durationMinutes,
            containerExtension = item.containerExtension,
            addedAt = item.addedAt,
            playbackPositionMs = old?.playbackPositionMs ?: 0L,
            playbackDurationMs = old?.playbackDurationMs ?: 0L,
            isWatched = old?.isWatched ?: false,
            lastWatchedAt = old?.lastWatchedAt,
            resolutionWidth = old?.resolutionWidth,
            resolutionHeight = old?.resolutionHeight,
            sortOrder = index
        )
    }
}

private class ExistingEpisodeMatcher(episodes: List<EpisodeEntity>) {
    private val buckets = mutableMapOf<String, ArrayDeque<EpisodeEntity>>()
    init { episodes.forEach { buckets.getOrPut(it.providerId, ::ArrayDeque).addLast(it) } }
    fun match(item: ParsedEpisode): EpisodeEntity? = buckets[item.providerId]?.pollFirst()
}

fun EpisodeEntity.qualityBadge(): String? = streamResolutionBadge(
    resolutionWidth?.let { width -> resolutionHeight?.let { height -> StreamResolution(width, height) } }
)

fun EpisodeEntity.progressFraction(): Float = when {
    isWatched -> 1f
    playbackDurationMs <= 0L -> 0f
    else -> (playbackPositionMs.toFloat() / playbackDurationMs.toFloat()).coerceIn(0f, 1f)
}

fun EpisodeEntity.displayCode(): String = when {
    seasonNumber == 0 && episodeNumber != null -> "Special ${episodeNumber}"
    seasonNumber > 0 && episodeNumber != null -> "S$seasonNumber · A$episodeNumber"
    episodeNumber != null -> "A$episodeNumber"
    seasonNumber == 0 -> "Special"
    else -> "Aflevering"
}

fun EpisodeEntity.asPlaybackChannel(series: SeriesEntity): ChannelEntity = ChannelEntity(
    id = -(9_000_000L + id.coerceAtLeast(1L)),
    playlistId = series.playlistId,
    name = "${series.title} - ${displayCode()} - $title",
    tvgId = "episode:$id",
    tvgName = title,
    logoUrl = imageUrl ?: series.posterUrl,
    groupTitle = series.title,
    streamUrl = streamUrl.orEmpty(),
    isFavorite = series.isFavorite,
    lastWatchedAt = lastWatchedAt,
    sortOrder = sortOrder
)

fun List<EpisodeEntity>.orderedEpisodes(): List<EpisodeEntity> = sortedWith(
    compareBy<EpisodeEntity>({ seasonSortKey(it.seasonNumber) }, { it.episodeNumber ?: Int.MAX_VALUE }, { it.providerOrder }, { it.title.lowercase() })
)

fun List<EpisodeEntity>.nextPlayableEpisode(current: EpisodeEntity): EpisodeEntity? {
    val ordered = orderedEpisodes()
    val index = ordered.indexOfFirst { it.id == current.id }
    if (index < 0) return null
    return ordered.drop(index + 1).firstOrNull { !it.streamUrl.isNullOrBlank() }
}

private val parsedEpisodeComparator = compareBy<ParsedEpisode>(
    { seasonSortKey(it.seasonNumber) }, { it.episodeNumber ?: Int.MAX_VALUE }, { it.providerOrder }, { it.title.lowercase() }
)

private fun seasonSortKey(number: Int): Int = when {
    number > 0 -> number
    number == 0 -> 100_000
    else -> 100_001
}
