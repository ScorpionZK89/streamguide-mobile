package com.example.streamguidemobile.ui.movies

import com.example.streamguidemobile.data.MovieEntity
import com.example.streamguidemobile.data.qualityBadge

enum class MovieSort(val label: String) {
    RecentlyAdded("Recent toegevoegd"),
    TitleAscending("Titel A-Z"),
    TitleDescending("Titel Z-A"),
    YearDescending("Jaar nieuw naar oud"),
    YearAscending("Jaar oud naar nieuw"),
    RatingDescending("Beoordeling"),
    LastWatched("Laatst bekeken")
}

data class MovieFilters(
    val genre: String? = null,
    val year: Int? = null,
    val ageRating: String? = null,
    val quality: String? = null,
    val onlyUnwatched: Boolean = false,
    val onlyWatchlist: Boolean = false
) {
    val activeCount: Int
        get() = listOfNotNull(genre, year, ageRating, quality).size +
            listOf(onlyUnwatched, onlyWatchlist).count { it }
}

data class MovieCategory(val key: String, val label: String)

data class MovieLibraryState(
    val sourceMovieCount: Int = 0,
    val allMovies: List<MovieEntity> = emptyList(),
    val movies: List<MovieEntity> = emptyList(),
    val allGroups: List<String> = emptyList(),
    val hiddenGroups: Set<String> = emptySet(),
    val categories: List<MovieCategory> = emptyList(),
    val selectedCategory: String = CATEGORY_ALL,
    val query: String = "",
    val filters: MovieFilters = MovieFilters(),
    val sort: MovieSort = MovieSort.RecentlyAdded,
    val genres: List<String> = emptyList(),
    val years: List<Int> = emptyList(),
    val ageRatings: List<String> = emptyList(),
    val qualities: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

fun buildMovieLibrary(
    movies: List<MovieEntity>,
    query: String,
    selectedCategory: String,
    filters: MovieFilters,
    sort: MovieSort,
    hiddenGroups: Set<String> = emptySet()
): MovieLibraryState {
    val allGroups = movies.map { it.categoryName.trim() }.filter(String::isNotEmpty)
        .distinctBy(String::lowercase).sortedWith(String.CASE_INSENSITIVE_ORDER)
    val visibleMovies = movies.filter { movie ->
        hiddenGroups.none { it.equals(movie.categoryName, ignoreCase = true) }
    }
    val categories = buildList {
        add(MovieCategory(CATEGORY_ALL, "Alle films"))
        if (visibleMovies.any { it.playbackPositionMs > 0L && !it.isWatched }) add(MovieCategory(CATEGORY_CONTINUE, "Verder kijken"))
        if (visibleMovies.any { it.isFavorite }) add(MovieCategory(CATEGORY_WATCHLIST, "Mijn lijst"))
        if (visibleMovies.any { it.addedAt != null }) add(MovieCategory(CATEGORY_RECENT, "Recent toegevoegd"))
        visibleMovies.map { it.categoryName.trim() }.filter(String::isNotEmpty).distinctBy(String::lowercase)
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .forEach { add(MovieCategory(categoryKey(it), it)) }
    }
    val validCategory = selectedCategory.takeIf { key -> categories.any { it.key == key } } ?: CATEGORY_ALL
    val cleanQuery = query.trim()
    val filtered = visibleMovies.asSequence()
        .filter { movie ->
            when (validCategory) {
                CATEGORY_CONTINUE -> movie.playbackPositionMs > 0L && !movie.isWatched
                CATEGORY_WATCHLIST -> movie.isFavorite
                CATEGORY_RECENT -> movie.addedAt != null
                CATEGORY_ALL -> true
                else -> categoryKey(movie.categoryName) == validCategory
            }
        }
        .filter { movie ->
            cleanQuery.isBlank() || listOfNotNull(movie.title, movie.originalTitle, movie.genre, movie.director, movie.cast)
                .any { it.contains(cleanQuery, ignoreCase = true) }
        }
        .filter { filters.genre == null || it.genreTokens().any { genre -> genre.equals(filters.genre, ignoreCase = true) } }
        .filter { filters.year == null || it.year == filters.year }
        .filter { filters.ageRating == null || it.ageRating.equals(filters.ageRating, ignoreCase = true) }
        .filter { filters.quality == null || it.qualityBadge() == filters.quality }
        .filter { !filters.onlyUnwatched || !it.isWatched }
        .filter { !filters.onlyWatchlist || it.isFavorite }
        .toList()
    val sorted = when (sort) {
        MovieSort.RecentlyAdded -> filtered.sortedWith(compareByDescending<MovieEntity> { it.addedAt ?: Long.MIN_VALUE }.thenBy { it.title.lowercase() })
        MovieSort.TitleAscending -> filtered.sortedBy { it.title.lowercase() }
        MovieSort.TitleDescending -> filtered.sortedByDescending { it.title.lowercase() }
        MovieSort.YearDescending -> filtered.sortedWith(compareByDescending<MovieEntity> { it.year ?: Int.MIN_VALUE }.thenBy { it.title.lowercase() })
        MovieSort.YearAscending -> filtered.sortedWith(compareBy<MovieEntity> { it.year ?: Int.MAX_VALUE }.thenBy { it.title.lowercase() })
        MovieSort.RatingDescending -> filtered.sortedWith(compareByDescending<MovieEntity> { it.rating ?: Double.NEGATIVE_INFINITY }.thenBy { it.title.lowercase() })
        MovieSort.LastWatched -> filtered.sortedWith(compareByDescending<MovieEntity> { it.lastWatchedAt ?: Long.MIN_VALUE }.thenBy { it.title.lowercase() })
    }
    return MovieLibraryState(
        sourceMovieCount = movies.size,
        allMovies = visibleMovies,
        movies = sorted,
        allGroups = allGroups,
        hiddenGroups = hiddenGroups,
        categories = categories,
        selectedCategory = validCategory,
        query = query,
        filters = filters,
        sort = sort,
        genres = visibleMovies.flatMap(MovieEntity::genreTokens).distinctBy(String::lowercase).sortedWith(String.CASE_INSENSITIVE_ORDER),
        years = visibleMovies.mapNotNull { it.year }.distinct().sortedDescending(),
        ageRatings = visibleMovies.mapNotNull { it.ageRating?.trim()?.takeIf(String::isNotEmpty) }.distinctBy(String::lowercase).sortedWith(String.CASE_INSENSITIVE_ORDER),
        qualities = visibleMovies.mapNotNull(MovieEntity::qualityBadge).distinct().sortedBy { qualityOrder(it) }
    )
}

fun MovieEntity.progressFraction(): Float = when {
    isWatched -> 1f
    playbackDurationMs <= 0L -> 0f
    else -> (playbackPositionMs.toFloat() / playbackDurationMs.toFloat()).coerceIn(0f, 1f)
}

fun MovieEntity.primaryActionLabel(): String = when {
    isWatched -> "Opnieuw afspelen"
    playbackPositionMs > 0L -> "Hervatten"
    else -> "Nu kijken"
}

fun MovieEntity.genreTokens(): List<String> = genre.orEmpty().split(',', '/', '|')
    .map(String::trim).filter(String::isNotEmpty)

fun categoryKey(category: String): String = "category:${category.trim().lowercase()}"

private fun qualityOrder(value: String): Int = when (value) {
    "SD" -> 0
    "HD" -> 1
    "FHD" -> 2
    "4K" -> 3
    else -> 4
}

const val CATEGORY_ALL = "__all"
const val CATEGORY_CONTINUE = "__continue"
const val CATEGORY_WATCHLIST = "__watchlist"
const val CATEGORY_RECENT = "__recent"
