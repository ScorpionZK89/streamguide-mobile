package com.example.streamguidemobile.ui.series

import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.SeriesEntity
import com.example.streamguidemobile.data.displayCode
import com.example.streamguidemobile.data.orderedEpisodes
import com.example.streamguidemobile.data.qualityBadge

enum class SeriesSort(val label: String) {
    RecentlyAdded("Recent toegevoegd"), TitleAscending("Titel A-Z"), TitleDescending("Titel Z-A"),
    YearDescending("Jaar nieuw naar oud"), LastWatched("Laatst bekeken"), RecentlyUpdated("Recent bijgewerkt"), RatingDescending("Beoordeling")
}

data class SeriesFilters(
    val genre: String? = null,
    val year: Int? = null,
    val ageRating: String? = null,
    val onlyUnfinished: Boolean = false,
    val onlyWatchlist: Boolean = false,
    val onlyWithUnwatched: Boolean = false
) {
    val activeCount: Int get() = listOfNotNull(genre, year, ageRating).size + listOf(onlyUnfinished, onlyWatchlist, onlyWithUnwatched).count { it }
}

data class SeriesCategory(val key: String, val label: String)

data class SeriesCardModel(val series: SeriesEntity, val episodes: List<EpisodeEntity>) {
    val orderedEpisodes: List<EpisodeEntity> = episodes.orderedEpisodes()
    val playableEpisodes: List<EpisodeEntity> = orderedEpisodes.filter { !it.streamUrl.isNullOrBlank() }
    val watchedCount: Int = episodes.count { it.isWatched }
    val isComplete: Boolean = episodes.isNotEmpty() && episodes.all { it.isWatched }
    val hasProgress: Boolean = episodes.any { it.isWatched || it.playbackPositionMs > 0L }
    val hasUnwatched: Boolean = episodes.any { !it.isWatched }
    val seasonCount: Int = episodes.map { it.seasonNumber }.filter { it > 0 }.distinct().size
    val progressEpisode: EpisodeEntity? = series.progressEpisodeId?.let { id -> episodes.firstOrNull { it.id == id } }
        ?: episodes.filter { it.lastWatchedAt != null }.maxByOrNull { it.lastWatchedAt ?: Long.MIN_VALUE }
    val primaryEpisode: EpisodeEntity? = when {
        progressEpisode == null -> playableEpisodes.firstOrNull()
        progressEpisode.playbackPositionMs > 0L && !progressEpisode.isWatched && !progressEpisode.streamUrl.isNullOrBlank() -> progressEpisode
        else -> orderedEpisodes.dropWhile { it.id != progressEpisode.id }.drop(1).firstOrNull { !it.streamUrl.isNullOrBlank() && !it.isWatched }
            ?: playableEpisodes.firstOrNull()
    }
    val quality: String? = episodes.mapNotNull(EpisodeEntity::qualityBadge).maxByOrNull(::qualityOrder)
    val progressFraction: Float = if (episodes.isEmpty()) 0f else (watchedCount + episodes.filter { !it.isWatched }.sumOf { episode ->
        if (episode.playbackDurationMs > 0L) episode.playbackPositionMs.toDouble() / episode.playbackDurationMs else 0.0
    }).toFloat() / episodes.size
    val statusText: String? = when {
        progressEpisode != null -> "Verder bij ${progressEpisode.displayCode()}"
        episodes.isNotEmpty() && watchedCount > 0 -> "$watchedCount van ${episodes.size} bekeken"
        seasonCount > 0 -> if (seasonCount == 1) "1 seizoen" else "$seasonCount seizoenen"
        else -> null
    }
    val actionLabel: String = when {
        episodes.isEmpty() -> "Afleveringen laden"
        isComplete -> "Opnieuw bekijken"
        progressEpisode?.playbackPositionMs ?: 0L > 0L && progressEpisode?.isWatched == false -> "Hervat ${progressEpisode.displayCode()}"
        hasProgress -> "Volgende aflevering"
        else -> "Start serie"
    }
}

data class SeriesLibraryState(
    val sourceSeriesCount: Int = 0,
    val allSeries: List<SeriesCardModel> = emptyList(),
    val series: List<SeriesCardModel> = emptyList(),
    val allEpisodes: List<EpisodeEntity> = emptyList(),
    val allGroups: List<String> = emptyList(),
    val hiddenGroups: Set<String> = emptySet(),
    val categories: List<SeriesCategory> = emptyList(),
    val selectedCategory: String = SERIES_ALL,
    val query: String = "",
    val filters: SeriesFilters = SeriesFilters(),
    val sort: SeriesSort = SeriesSort.RecentlyAdded,
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val ageRatings: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val detailLoading: Boolean = false,
    val error: String? = null
)

fun buildSeriesLibrary(
    source: List<SeriesEntity>, episodes: List<EpisodeEntity>, query: String, selectedCategory: String,
    filters: SeriesFilters, sort: SeriesSort, hiddenGroups: Set<String> = emptySet()
): SeriesLibraryState {
    val allGroups = source.map { it.categoryName.trim() }.filter(String::isNotEmpty)
        .distinctBy(String::lowercase).sortedWith(String.CASE_INSENSITIVE_ORDER)
    val visibleSource = source.filter { item ->
        hiddenGroups.none { it.equals(item.categoryName, ignoreCase = true) }
    }
    val visibleSeriesIds = visibleSource.mapTo(mutableSetOf()) { it.id }
    val visibleEpisodes = episodes.filter { it.seriesId in visibleSeriesIds }
    val bySeries = visibleEpisodes.groupBy { it.seriesId }
    val cards = visibleSource.map { SeriesCardModel(it, bySeries[it.id].orEmpty()) }
    val categories = buildList {
        add(SeriesCategory(SERIES_ALL, "Alle series"))
        if (cards.any { it.hasProgress && !it.isComplete }) add(SeriesCategory(SERIES_CONTINUE, "Verder kijken"))
        if (cards.any { it.series.isFavorite }) add(SeriesCategory(SERIES_WATCHLIST, "Mijn lijst"))
        if (cards.any { it.series.addedAt != null }) add(SeriesCategory(SERIES_RECENT, "Recent toegevoegd"))
        visibleSource.map { it.categoryName.trim() }.filter(String::isNotEmpty).distinctBy(String::lowercase).sortedWith(String.CASE_INSENSITIVE_ORDER)
            .forEach { add(SeriesCategory(seriesCategoryKey(it), it)) }
    }
    val validCategory = selectedCategory.takeIf { key -> categories.any { it.key == key } } ?: SERIES_ALL
    val cleanQuery = query.trim()
    val filtered = cards.asSequence().filter { card ->
        when (validCategory) {
            SERIES_CONTINUE -> card.hasProgress && !card.isComplete
            SERIES_WATCHLIST -> card.series.isFavorite
            SERIES_RECENT -> card.series.addedAt != null
            SERIES_ALL -> true
            else -> seriesCategoryKey(card.series.categoryName) == validCategory
        }
    }.filter { card ->
        cleanQuery.isBlank() || listOfNotNull(card.series.title, card.series.originalTitle, card.series.genre, card.series.cast)
            .any { it.contains(cleanQuery, ignoreCase = true) }
    }.filter { filters.genre == null || it.series.genreTokens().any { genre -> genre.equals(filters.genre, true) } }
        .filter { filters.year == null || it.series.year == filters.year }
        .filter { filters.ageRating == null || it.series.ageRating.equals(filters.ageRating, true) }
        .filter { !filters.onlyUnfinished || !it.isComplete }
        .filter { !filters.onlyWatchlist || it.series.isFavorite }
        .filter { !filters.onlyWithUnwatched || it.hasUnwatched }
        .toList()
    val sorted = when (sort) {
        SeriesSort.RecentlyAdded -> filtered.sortedWith(compareByDescending<SeriesCardModel> { it.series.addedAt ?: Long.MIN_VALUE }.thenBy { it.series.title.lowercase() })
        SeriesSort.TitleAscending -> filtered.sortedBy { it.series.title.lowercase() }
        SeriesSort.TitleDescending -> filtered.sortedByDescending { it.series.title.lowercase() }
        SeriesSort.YearDescending -> filtered.sortedWith(compareByDescending<SeriesCardModel> { it.series.year ?: Int.MIN_VALUE }.thenBy { it.series.title.lowercase() })
        SeriesSort.LastWatched -> filtered.sortedWith(compareByDescending<SeriesCardModel> { it.series.lastWatchedAt ?: Long.MIN_VALUE }.thenBy { it.series.title.lowercase() })
        SeriesSort.RecentlyUpdated -> filtered.sortedWith(compareByDescending<SeriesCardModel> { it.series.updatedAt ?: Long.MIN_VALUE }.thenBy { it.series.title.lowercase() })
        SeriesSort.RatingDescending -> filtered.sortedWith(compareByDescending<SeriesCardModel> { it.series.rating ?: Double.NEGATIVE_INFINITY }.thenBy { it.series.title.lowercase() })
    }
    return SeriesLibraryState(
        sourceSeriesCount = source.size, allSeries = cards, series = sorted, allEpisodes = visibleEpisodes,
        allGroups = allGroups, hiddenGroups = hiddenGroups, categories = categories, selectedCategory = validCategory,
        query = query, filters = filters, sort = sort,
        genres = visibleSource.flatMap(SeriesEntity::genreTokens).distinctBy(String::lowercase).sortedWith(String.CASE_INSENSITIVE_ORDER),
        years = visibleSource.mapNotNull { it.year }.distinct().sortedDescending(),
        ageRatings = visibleSource.mapNotNull { it.ageRating?.trim()?.takeIf(String::isNotEmpty) }.distinctBy(String::lowercase).sortedWith(String.CASE_INSENSITIVE_ORDER)
    )
}

fun SeriesEntity.genreTokens(): List<String> = genre.orEmpty().split(',', '/', '|').map(String::trim).filter(String::isNotEmpty)
fun seriesCategoryKey(value: String): String = "category:${value.trim().lowercase()}"
private fun qualityOrder(value: String): Int = when (value) { "SD" -> 0; "HD" -> 1; "FHD" -> 2; "4K" -> 3; else -> -1 }

const val SERIES_ALL = "__series_all"
const val SERIES_CONTINUE = "__series_continue"
const val SERIES_WATCHLIST = "__series_watchlist"
const val SERIES_RECENT = "__series_recent"
