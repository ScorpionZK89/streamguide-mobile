package com.example.streamguidemobile.ui.movies

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.streamguidemobile.data.MovieEntity
import com.example.streamguidemobile.data.qualityBadge
import com.example.streamguidemobile.ui.cast.CastRouteButton
import com.example.streamguidemobile.ui.live.CinematicEmptyState
import com.example.streamguidemobile.ui.live.CinematicIconAction
import com.example.streamguidemobile.ui.live.CinematicSearchField
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import com.example.streamguidemobile.ui.theme.StreamGuideMotion
import com.example.streamguidemobile.ui.theme.StreamGuideRadii
import com.example.streamguidemobile.ui.theme.StreamGuideSpacing
import com.example.streamguidemobile.ui.theme.StreamGuideTheme
import java.time.Instant
import java.time.ZoneId
import java.util.Locale

@Composable
fun MoviesScreen(
    library: MovieLibraryState,
    selectedMovieId: Long?,
    onMovieSelected: (Long?) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onFiltersChanged: (MovieFilters) -> Unit,
    onClearFilters: () -> Unit,
    onSortChanged: (MovieSort) -> Unit,
    onToggleFavorite: (MovieEntity) -> Unit,
    onLoadDetails: (Long) -> Unit,
    onPlay: (MovieEntity, Boolean) -> Unit,
    onPrepareCast: (MovieEntity) -> Unit,
    onSetWatched: (MovieEntity, Boolean) -> Unit,
    onGroupVisible: (String, Boolean) -> Unit,
    onShowAllGroups: () -> Unit,
    onHideAllGroups: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var queryText by rememberSaveable { mutableStateOf(library.query) }
    var searchVisible by rememberSaveable { mutableStateOf(library.query.isNotBlank()) }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSort by rememberSaveable { mutableStateOf(false) }
    var showGroups by rememberSaveable { mutableStateOf(false) }
    val gridState = rememberLazyGridState()
    val selectedMovie = selectedMovieId?.let { id -> library.allMovies.firstOrNull { it.id == id } }
    if (selectedMovie != null) {
        LaunchedEffect(selectedMovie.id) {
            onLoadDetails(selectedMovie.id)
            onPrepareCast(selectedMovie)
        }
        BackHandler { onMovieSelected(null) }
        MovieDetailScreen(
            movie = selectedMovie,
            loading = library.isLoading,
            error = library.error,
            onBack = { onMovieSelected(null) },
            onPlay = { restart -> onPlay(selectedMovie, restart) },
            onToggleFavorite = { onToggleFavorite(selectedMovie) },
            onSetWatched = { watched -> onSetWatched(selectedMovie, watched) },
            onRetry = { onLoadDetails(selectedMovie.id) },
            modifier = modifier
        )
        return
    }

    BackHandler(enabled = showFilters || showSort || showGroups || searchVisible) {
        when {
            showFilters -> showFilters = false
            showSort -> showSort = false
            showGroups -> showGroups = false
            queryText.isNotBlank() -> { queryText = ""; onQueryChange("") }
            else -> searchVisible = false
        }
    }

    BoxWithConstraints(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(CinematicColors.CanvasTop, CinematicColors.Canvas, CinematicColors.CanvasDeep))
        )
    ) {
        val wide = maxWidth >= 720.dp
        val pagePadding = if (wide) StreamGuideSpacing.Xl else 14.dp
        val showHero = wide || maxHeight >= 700.dp
        Column(Modifier.fillMaxSize()) {
            MovieToolbar(
                count = library.allMovies.size,
                searchVisible = searchVisible,
                activeFilters = library.filters.activeCount,
                hiddenGroupCount = library.allGroups.count { group ->
                    library.hiddenGroups.any { it.equals(group, ignoreCase = true) }
                },
                watchlistSelected = library.selectedCategory == CATEGORY_WATCHLIST,
                onSearch = { searchVisible = !searchVisible },
                onFilter = { showFilters = true },
                onSort = { showSort = true },
                onGroups = { showGroups = true },
                onWatchlist = { onCategorySelected(CATEGORY_WATCHLIST) },
                modifier = Modifier.padding(horizontal = pagePadding, vertical = 7.dp)
            )
            if (searchVisible) {
                CinematicSearchField(
                    value = queryText,
                    placeholder = "Zoek titel, genre, regisseur of acteur",
                    onValueChange = { queryText = it; onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = pagePadding, vertical = 4.dp)
                )
            }
            MovieCategoryRow(
                categories = library.categories,
                selected = library.selectedCategory,
                onSelected = onCategorySelected,
                modifier = Modifier.padding(horizontal = pagePadding, vertical = 5.dp)
            )
            when {
                library.isLoading && library.sourceMovieCount == 0 -> MovieSkeletonGrid(pagePadding)
                library.error != null && library.sourceMovieCount == 0 -> MovieStatus(
                    title = "Filmbibliotheek niet bereikbaar",
                    description = library.error,
                    action = "Opnieuw proberen",
                    onAction = onRetry
                )
                library.sourceMovieCount == 0 -> CinematicEmptyState(
                    title = "Geen films beschikbaar",
                    description = "Deze provider levert momenteel geen VOD-filmbibliotheek."
                )
                library.allMovies.isEmpty() -> MovieStatus(
                    title = "Alle filmgroepen zijn verborgen",
                    description = "Kies welke filmgroepen je weer wilt zien.",
                    action = "Groepen kiezen",
                    actionIcon = Icons.Default.Visibility,
                    onAction = { showGroups = true }
                )
                library.movies.isEmpty() -> MovieStatus(
                    title = "Geen films gevonden",
                    description = "Pas je zoekopdracht of filters aan.",
                    action = "Filters wissen",
                    onAction = { queryText = ""; onQueryChange(""); onClearFilters(); onCategorySelected(CATEGORY_ALL) }
                )
                else -> {
                    if (showHero) {
                        MovieHero(
                            movie = library.movies.first(),
                            compact = !wide,
                            onPlay = { onPlay(library.movies.first(), false) },
                            onInfo = { onMovieSelected(library.movies.first().id) },
                            onFavorite = { onToggleFavorite(library.movies.first()) },
                            modifier = Modifier.padding(horizontal = pagePadding, vertical = 4.dp)
                        )
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (wide) 128.dp else 112.dp),
                        state = gridState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = pagePadding, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(library.movies, key = { it.id }) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = { onMovieSelected(movie.id) },
                                onFavorite = { onToggleFavorite(movie) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        MovieFilterDialog(
            library = library,
            onApply = { onFiltersChanged(it); showFilters = false },
            onClear = { onClearFilters(); showFilters = false },
            onDismiss = { showFilters = false }
        )
    }
    if (showSort) {
        MovieSortDialog(
            available = MovieSort.entries.filter { it != MovieSort.RatingDescending || library.allMovies.any { movie -> movie.rating != null } },
            selected = library.sort,
            onSelected = { onSortChanged(it); showSort = false },
            onDismiss = { showSort = false }
        )
    }
    if (showGroups) {
        MovieGroupVisibilityDialog(
            groups = library.allGroups,
            hiddenGroups = library.hiddenGroups,
            onGroupVisible = onGroupVisible,
            onShowAll = onShowAllGroups,
            onHideAll = onHideAllGroups,
            onDismiss = { showGroups = false }
        )
    }
}

@Composable
private fun MovieToolbar(
    count: Int,
    searchVisible: Boolean,
    activeFilters: Int,
    hiddenGroupCount: Int,
    watchlistSelected: Boolean,
    onSearch: () -> Unit,
    onFilter: () -> Unit,
    onSort: () -> Unit,
    onGroups: () -> Unit,
    onWatchlist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Films", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
            Text("$count titels", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CinematicIconAction(Icons.Default.Search, "Zoeken", onSearch, selected = searchVisible)
            Box {
                CinematicIconAction(Icons.Default.FilterList, "Filteren", onFilter, selected = activeFilters > 0)
                if (activeFilters > 0) MovieCountBadge(activeFilters, Modifier.align(Alignment.TopEnd))
            }
            CinematicIconAction(Icons.AutoMirrored.Filled.Sort, "Sorteren", onSort)
            Box {
                CinematicIconAction(Icons.Default.Visibility, "Filmgroepen", onGroups, selected = hiddenGroupCount > 0)
                if (hiddenGroupCount > 0) MovieCountBadge(hiddenGroupCount, Modifier.align(Alignment.TopEnd))
            }
            CinematicIconAction(if (watchlistSelected) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", onWatchlist, selected = watchlistSelected)
        }
    }
}

@Composable
private fun MovieCategoryRow(categories: List<MovieCategory>, selected: String, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(categories, key = { it.key }) { category ->
            MovieChip(category.label, selected = category.key == selected) { onSelected(category.key) }
        }
    }
}

@Composable
private fun MovieChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (selected || focused) CinematicColors.Gold.copy(alpha = 0.78f) else CinematicColors.Border,
        tween(StreamGuideMotion.Quick), label = "movie-chip-border"
    )
    Box(
        Modifier.onFocusChanged { focused = it.isFocused }.clip(RoundedCornerShape(StreamGuideRadii.Control))
            .background(if (selected) CinematicColors.Gold.copy(alpha = 0.12f) else CinematicColors.Panel.copy(alpha = 0.72f))
            .border(1.dp, border, RoundedCornerShape(StreamGuideRadii.Control)).clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, color = if (selected || focused) CinematicColors.GoldBright else CinematicColors.TextSecondary, style = CinematicTypography.Metadata)
    }
}

@Composable
private fun MovieHero(movie: MovieEntity, compact: Boolean, onPlay: () -> Unit, onInfo: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(StreamGuideRadii.Hero)
    Box(modifier.fillMaxWidth().height(if (compact) 156.dp else 214.dp).clip(shape).border(1.dp, CinematicColors.BorderStrong, shape)) {
        MovieArtwork(movie.backdropUrl ?: movie.posterUrl, movie.title, ContentScale.Crop, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(CinematicColors.Canvas.copy(alpha = 0.98f), CinematicColors.Canvas.copy(alpha = 0.66f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, CinematicColors.Canvas.copy(alpha = 0.86f)))))
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth(if (compact) 0.86f else 0.64f).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(movie.title, color = CinematicColors.TextPrimary, style = CinematicTypography.HeroTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            MovieMetadataLine(movie)
            movie.description?.let { Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MovieAction(movie.primaryActionLabel(), Icons.Default.PlayArrow, primary = true, onClick = onPlay)
                MovieAction("Meer informatie", Icons.Default.Info, onClick = onInfo)
                MovieIconButton(if (movie.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", movie.isFavorite, onFavorite)
            }
        }
    }
}

@Composable
private fun MoviePosterCard(movie: MovieEntity, onClick: () -> Unit, onFavorite: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.985f else if (focused) 1.035f else 1f, tween(StreamGuideMotion.Standard), label = "movie-card-scale")
    val border by animateColorAsState(if (focused) CinematicColors.Gold else CinematicColors.Border, tween(StreamGuideMotion.Standard), label = "movie-card-border")
    val shape = RoundedCornerShape(StreamGuideRadii.Card)
    Column(
        Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.shadow(if (focused) 12.dp else 0.dp, shape, ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold)
            .background(CinematicColors.Panel, shape).border(1.dp, border, shape).clip(shape)
            .onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(2.dp)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            MovieArtwork(movie.posterUrl, movie.title, ContentScale.Crop, Modifier.fillMaxSize())
            movie.qualityBadge()?.let { MovieBadge(it, CinematicColors.Gold, Modifier.align(Alignment.TopStart).padding(6.dp)) }
            MovieIconButton(
                if (movie.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                "Mijn lijst",
                movie.isFavorite,
                onFavorite,
                Modifier.align(Alignment.TopEnd).padding(5.dp)
            )
            if (movie.isWatched) MovieBadge("BEKEKEN", CinematicColors.PanelPressed, Modifier.align(Alignment.BottomStart).padding(6.dp))
            val progress = movie.progressFraction()
            if (progress > 0f && !movie.isWatched) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(CinematicColors.BorderStrong)) {
                    Box(Modifier.fillMaxWidth(progress).height(3.dp).background(CinematicColors.Gold))
                }
            }
        }
        Column(Modifier.padding(horizontal = 6.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(movie.title, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(movie.year?.toString(), movie.genreTokens().firstOrNull()).joinToString(" • ").ifBlank { movie.categoryName }, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun MovieDetailScreen(
    movie: MovieEntity,
    loading: Boolean,
    error: String?,
    onBack: () -> Unit,
    onPlay: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onSetWatched: (Boolean) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    BoxWithConstraints(modifier.fillMaxSize().background(CinematicColors.Canvas)) {
        val wide = maxWidth >= 720.dp
        val padding = if (wide) StreamGuideSpacing.Xl else 14.dp
        LazyColumn(contentPadding = PaddingValues(bottom = StreamGuideSpacing.Xl), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Box(Modifier.fillMaxWidth().height(if (wide) 300.dp else 220.dp)) {
                    MovieArtwork(movie.backdropUrl ?: movie.posterUrl, movie.title, ContentScale.Crop, Modifier.fillMaxSize())
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicColors.Canvas.copy(alpha = 0.16f), CinematicColors.Canvas.copy(alpha = 0.96f)))))
                    MovieIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Terug", false, onBack, Modifier.padding(padding).align(Alignment.TopStart))
                }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = padding), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.width(if (wide) 132.dp else 96.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(StreamGuideRadii.Card)).border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Card))) {
                        MovieArtwork(movie.posterUrl, movie.title, ContentScale.Crop, Modifier.fillMaxSize())
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text(movie.title, color = CinematicColors.TextPrimary, style = CinematicTypography.HeroTitle, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        movie.originalTitle?.takeIf { !it.equals(movie.title, ignoreCase = true) }?.let { Text(it, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata) }
                        MovieMetadataLine(movie)
                        if (movie.playbackPositionMs > 0L && movie.playbackDurationMs > 0L) MovieDetailProgress(movie)
                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            MovieAction(movie.primaryActionLabel(), if (movie.isWatched) Icons.Default.RestartAlt else Icons.Default.PlayArrow, primary = true) { onPlay(movie.isWatched) }
                            CastRouteButton(Modifier.size(34.dp))
                            MovieIconButton(if (movie.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", movie.isFavorite, onToggleFavorite)
                        }
                    }
                }
            }
            movie.description?.let { description -> item { MovieDetailSection("Verhaal", description, padding) } }
            val credits = buildList {
                movie.director?.let { add("Regie" to it) }
                movie.cast?.let { add("Cast" to it) }
            }
            if (credits.isNotEmpty()) item { MovieCreditsSection(credits, padding) }
            item {
                Row(Modifier.padding(horizontal = padding), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MovieAction(if (movie.isWatched) "Markeer als niet bekeken" else "Markeer als bekeken", Icons.Default.CheckCircle) { onSetWatched(!movie.isWatched) }
                    movie.trailerUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }?.let { url ->
                        MovieAction("Trailer", Icons.Default.Movie) { uriHandler.openUri(url) }
                    }
                }
            }
            if (loading) item { MovieLoadingStrip(Modifier.padding(horizontal = padding)) }
            error?.let { message -> item { MovieInlineError(message, onRetry, Modifier.padding(horizontal = padding)) } }
        }
    }
}

@Composable
private fun MovieMetadataLine(movie: MovieEntity) {
    val metadata = buildList {
        movie.year?.let { add(it.toString()) }
        movie.durationMinutes?.let { add("${it} min") }
        movie.genre?.takeIf(String::isNotBlank)?.let(::add)
        movie.ageRating?.takeIf(String::isNotBlank)?.let(::add)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(metadata.joinToString(" • ").ifBlank { movie.categoryName }, color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        movie.qualityBadge()?.let { MovieBadge(it, CinematicColors.Gold) }
        movie.rating?.let { MovieBadge(String.format(Locale.getDefault(), "%.1f", it), CinematicColors.PanelPressed) }
    }
}

@Composable
private fun MovieDetailProgress(movie: MovieEntity) {
    val progress = movie.progressFraction()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.fillMaxWidth().height(3.dp).background(CinematicColors.BorderStrong)) {
            Box(Modifier.fillMaxWidth(progress).height(3.dp).background(CinematicColors.Gold))
        }
        val watchedMinutes = movie.playbackPositionMs / 60_000L
        val remainingMinutes = ((movie.playbackDurationMs - movie.playbackPositionMs).coerceAtLeast(0L)) / 60_000L
        Text("$watchedMinutes min bekeken • $remainingMinutes min resterend", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
    }
}

@Composable
private fun MovieAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    val background = if (primary) CinematicColors.Gold else if (focused) CinematicColors.PanelPressed else CinematicColors.PanelRaised.copy(alpha = 0.88f)
    val content = if (primary) CinematicColors.OnGold else if (focused) CinematicColors.GoldBright else CinematicColors.TextPrimary
    Row(
        Modifier.height(34.dp).widthIn(max = 190.dp).onFocusChanged { focused = it.isFocused }.clip(shape).background(background)
            .border(1.dp, if (focused) CinematicColors.GoldBright else if (primary) CinematicColors.Gold else CinematicColors.BorderStrong, shape)
            .clickable(onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
        Text(label, color = content, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun MovieIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    Box(
        modifier.size(32.dp).onFocusChanged { focused = it.isFocused }.clip(shape)
            .background(if (selected || focused) CinematicColors.Gold.copy(alpha = 0.16f) else CinematicColors.Panel.copy(alpha = 0.82f))
            .border(1.dp, if (focused) CinematicColors.Gold else CinematicColors.BorderStrong, shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (selected || focused) CinematicColors.GoldBright else CinematicColors.TextSecondary, modifier = Modifier.size(17.dp))
    }
}

@Composable
private fun MovieArtwork(url: String?, description: String?, scale: ContentScale, modifier: Modifier = Modifier) {
    Box(modifier.background(Brush.verticalGradient(listOf(CinematicColors.PanelPressed, CinematicColors.Panel))), contentAlignment = Alignment.Center) {
        if (!url.isNullOrBlank()) {
            AsyncImage(model = url, contentDescription = description, contentScale = scale, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Default.Movie, contentDescription = null, tint = CinematicColors.Gold.copy(alpha = 0.66f), modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun MovieBadge(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.background(color.copy(alpha = 0.92f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) {
        Text(label, color = if (color == CinematicColors.Gold) CinematicColors.OnGold else CinematicColors.TextPrimary, style = CinematicTypography.Badge)
    }
}

@Composable
private fun MovieCountBadge(count: Int, modifier: Modifier = Modifier) {
    Box(modifier.size(14.dp).background(CinematicColors.Gold, CircleShape), contentAlignment = Alignment.Center) {
        Text(count.coerceAtMost(9).toString(), color = CinematicColors.OnGold, style = CinematicTypography.Badge)
    }
}

@Composable
private fun MovieDetailSection(title: String, text: String, padding: Dp) {
    Column(Modifier.fillMaxWidth().padding(horizontal = padding), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
        Text(text, color = CinematicColors.TextSecondary, style = CinematicTypography.Body)
    }
}

@Composable
private fun MovieCreditsSection(credits: List<Pair<String, String>>, padding: Dp) {
    Column(Modifier.fillMaxWidth().padding(horizontal = padding), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("Cast en makers", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
        credits.forEach { (label, value) ->
            Row {
                Text(label, color = CinematicColors.Gold, style = CinematicTypography.Metadata, modifier = Modifier.width(62.dp))
                Text(value, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MovieStatus(
    title: String,
    description: String,
    action: String,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Default.Refresh,
    onAction: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Movie, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
        Text(description, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        Spacer(Modifier.height(12.dp))
        MovieAction(action, actionIcon, onClick = onAction)
    }
}

@Composable
private fun MovieSkeletonGrid(padding: Dp) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(112.dp),
        contentPadding = PaddingValues(horizontal = padding, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(10) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(CinematicColors.PanelRaised, RoundedCornerShape(StreamGuideRadii.Card)))
                Box(Modifier.fillMaxWidth(0.78f).height(10.dp).background(CinematicColors.PanelPressed, RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth(0.48f).height(7.dp).background(CinematicColors.Panel, RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable
private fun MovieLoadingStrip(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(CinematicColors.PanelRaised, RoundedCornerShape(StreamGuideRadii.Control)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).background(CinematicColors.Gold, CircleShape))
        Text("Filmgegevens laden", color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata)
    }
}

@Composable
private fun MovieInlineError(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().background(CinematicColors.PanelRaised, RoundedCornerShape(StreamGuideRadii.Control)).border(1.dp, CinematicColors.Live.copy(alpha = 0.5f), RoundedCornerShape(StreamGuideRadii.Control)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(message, color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata, modifier = Modifier.weight(1f))
        MovieAction("Opnieuw", Icons.Default.Refresh, onClick = onRetry)
    }
}

@Composable
private fun MovieGroupVisibilityDialog(
    groups: List<String>,
    hiddenGroups: Set<String>,
    onGroupVisible: (String, Boolean) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp).widthIn(max = 560.dp).heightIn(max = 650.dp)
                .background(CinematicColors.Panel, RoundedCornerShape(StreamGuideRadii.Hero))
                .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Hero)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier.size(36.dp).background(CinematicColors.GoldMuted, RoundedCornerShape(StreamGuideRadii.Control)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Filmgroepen", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                    Text("Bepaal welke groepen zichtbaar zijn", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                }
            }
            if (groups.isEmpty()) {
                Text("Er zijn nog geen filmgroepen gevonden.", color = CinematicColors.TextMuted, style = CinematicTypography.Body)
            } else {
                LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(groups, key = { it.lowercase() }) { group ->
                        val visible = hiddenGroups.none { it.equals(group, ignoreCase = true) }
                        MovieGroupVisibilityRow(group, visible) { onGroupVisible(group, !visible) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                MovieAction("Alles tonen", Icons.Default.Visibility, onClick = onShowAll)
                MovieAction("Alles verbergen", Icons.Default.VisibilityOff, onClick = onHideAll)
                MovieAction("Klaar", Icons.Default.Done, primary = true, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun MovieGroupVisibilityRow(group: String, visible: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    Row(
        Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.clip(shape)
            .background(if (focused) CinematicColors.PanelPressed else CinematicColors.PanelRaised.copy(alpha = 0.72f))
            .border(1.dp, if (focused) CinematicColors.Gold else CinematicColors.Border, shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
            contentDescription = if (visible) "Zichtbaar" else "Verborgen",
            tint = if (visible) CinematicColors.GoldBright else CinematicColors.TextMuted,
            modifier = Modifier.size(18.dp)
        )
        Text(group, color = CinematicColors.TextPrimary, style = CinematicTypography.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (visible) "Zichtbaar" else "Verborgen", color = if (visible) CinematicColors.Gold else CinematicColors.TextMuted, style = CinematicTypography.Metadata)
    }
}

@Composable
private fun MovieFilterDialog(library: MovieLibraryState, onApply: (MovieFilters) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) {
    var draft by remember(library.filters) { mutableStateOf(library.filters) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp).widthIn(max = 560.dp).heightIn(max = 640.dp)
                .background(CinematicColors.Panel, RoundedCornerShape(StreamGuideRadii.Hero))
                .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Hero)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Films filteren", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
            MovieFilterRow("Genre", library.genres, draft.genre) { draft = draft.copy(genre = it) }
            MovieFilterRow("Jaar", library.years.map(Int::toString), draft.year?.toString()) { draft = draft.copy(year = it?.toIntOrNull()) }
            MovieFilterRow("Leeftijd", library.ageRatings, draft.ageRating) { draft = draft.copy(ageRating = it) }
            MovieFilterRow("Kwaliteit", library.qualities, draft.quality) { draft = draft.copy(quality = it) }
            MovieBooleanFilter("Alleen niet bekeken", draft.onlyUnwatched) { draft = draft.copy(onlyUnwatched = it) }
            MovieBooleanFilter("Alleen Mijn lijst", draft.onlyWatchlist) { draft = draft.copy(onlyWatchlist = it) }
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                MovieAction("Filters wissen", Icons.Default.Clear, onClick = onClear)
                MovieAction("Toepassen", Icons.Default.CheckCircle, primary = true) { onApply(draft) }
            }
        }
    }
}

@Composable
private fun MovieFilterRow(title: String, values: List<String>, selected: String?, onSelected: (String?) -> Unit) {
    if (values.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, color = CinematicColors.TextSecondary, style = CinematicTypography.CardTitle)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            item { MovieChip("Alle", selected == null) { onSelected(null) } }
            items(values, key = { it }) { value -> MovieChip(value, value == selected) { onSelected(value) } }
        }
    }
}

@Composable
private fun MovieBooleanFilter(label: String, selected: Boolean, onSelected: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onSelected(!selected) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, modifier = Modifier.weight(1f))
        MovieChip(if (selected) "Aan" else "Uit", selected) { onSelected(!selected) }
    }
}

@Composable
private fun MovieSortDialog(available: List<MovieSort>, selected: MovieSort, onSelected: (MovieSort) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = CinematicColors.Gold) },
        title = { Text("Films sorteren", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                available.forEach { sort -> MovieChip(sort.label, sort == selected) { onSelected(sort) } }
            }
        },
        confirmButton = {},
        shape = RoundedCornerShape(StreamGuideRadii.Hero),
        containerColor = CinematicColors.Panel,
        tonalElevation = 0.dp
    )
}

@Preview(name = "Films phone", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable
private fun MoviesPhonePreview() = MoviePreview()

@Preview(name = "Films landscape", widthDp = 891, heightDp = 411, showBackground = true, backgroundColor = 0xFF04070B)
@Composable
private fun MoviesLandscapePreview() = MoviePreview()

@Preview(name = "Films tablet", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF04070B)
@Composable
private fun MoviesTabletPreview() = MoviePreview()

@Preview(name = "Film detail phone", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable
private fun MovieDetailPhonePreview() = MoviePreview(selectedMovieId = 1L)

@Preview(name = "Film detail tablet", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF04070B)
@Composable
private fun MovieDetailTabletPreview() = MoviePreview(selectedMovieId = 1L)

@Composable
private fun MoviePreview(selectedMovieId: Long? = null) {
    val movies = remember { previewMovies() }
    StreamGuideTheme {
        MoviesScreen(
            library = buildMovieLibrary(movies, "", CATEGORY_ALL, MovieFilters(), MovieSort.RecentlyAdded),
            selectedMovieId = selectedMovieId,
            onMovieSelected = {}, onQueryChange = {}, onCategorySelected = {}, onFiltersChanged = {}, onClearFilters = {},
            onSortChanged = {}, onToggleFavorite = {}, onLoadDetails = {}, onPlay = { _, _ -> }, onPrepareCast = {}, onSetWatched = { _, _ -> },
            onGroupVisible = { _, _ -> }, onShowAllGroups = {}, onHideAllGroups = {}, onRetry = {}
        )
    }
}

private fun previewMovies(): List<MovieEntity> = List(12) { index ->
    MovieEntity(
        id = index + 1L, playlistId = 1L, providerId = "movie-$index", title = listOf("Beyond the Valley", "Silent Current", "City After Dark", "Northern Light")[index % 4],
        originalTitle = null, streamUrl = "https://example.invalid/movie/$index.mp4", categoryId = "${index % 3}", categoryName = listOf("Drama", "Avontuur", "Thriller")[index % 3],
        posterUrl = null, backdropUrl = null, year = 2021 + index % 5, durationMinutes = 94 + index, genre = listOf("Drama", "Avontuur", "Thriller")[index % 3],
        ageRating = if (index % 2 == 0) "12" else null, description = "Een filmisch verhaal over keuzes, afstand en een onverwachte reis.", rating = if (index % 3 == 0) 7.4 else null,
        director = if (index == 0) "A. Morgan" else null, cast = if (index == 0) "R. Stone, M. Vale" else null, trailerUrl = null,
        addedAt = Instant.now().minusSeconds(index * 86_400L).toEpochMilli(), containerExtension = "mp4", isFavorite = index == 1,
        lastWatchedAt = if (index == 0) System.currentTimeMillis() else null, playbackPositionMs = if (index == 0) 2_100_000L else 0L,
        playbackDurationMs = if (index == 0) 6_000_000L else 0L, isWatched = index == 3, resolutionWidth = if (index < 2) 1920 else null,
        resolutionHeight = if (index < 2) 1080 else null, sortOrder = index
    )
}
