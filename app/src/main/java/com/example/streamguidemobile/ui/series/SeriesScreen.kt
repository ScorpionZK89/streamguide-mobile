package com.example.streamguidemobile.ui.series

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.streamguidemobile.R
import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.SeriesEntity
import com.example.streamguidemobile.data.displayCode
import com.example.streamguidemobile.data.progressFraction
import com.example.streamguidemobile.data.qualityBadge
import com.example.streamguidemobile.ui.cast.CastRouteButton
import com.example.streamguidemobile.ui.live.CinematicEmptyState
import com.example.streamguidemobile.ui.live.CinematicIconAction
import com.example.streamguidemobile.ui.live.CinematicSearchField
import com.example.streamguidemobile.ui.theme.*

@Composable
fun SeriesScreen(
    library: SeriesLibraryState,
    selectedSeriesId: Long?,
    onSeriesSelected: (Long?) -> Unit,
    onQueryChange: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onFiltersChanged: (SeriesFilters) -> Unit,
    onClearFilters: () -> Unit,
    onSortChanged: (SeriesSort) -> Unit,
    onToggleFavorite: (SeriesEntity) -> Unit,
    onLoadDetails: (Long, Boolean) -> Unit,
    onPlayEpisode: (EpisodeEntity, Boolean) -> Unit,
    onPrepareCast: (EpisodeEntity) -> Unit,
    onSetEpisodeWatched: (EpisodeEntity, Boolean) -> Unit,
    onSetSeasonWatched: (Long, Int, Boolean) -> Unit,
    onSetSeriesWatched: (Long, Boolean) -> Unit,
    onClearProgress: (Long) -> Unit,
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
    val selected = selectedSeriesId?.let { id -> library.allSeries.firstOrNull { it.series.id == id } }
    if (selected != null) {
        LaunchedEffect(selected.series.id) { onLoadDetails(selected.series.id, false) }
        BackHandler { onSeriesSelected(null) }
        SeriesDetailScreen(
            card = selected, loading = library.detailLoading, error = library.error,
            onBack = { onSeriesSelected(null) }, onRetry = { onLoadDetails(selected.series.id, true) },
            onToggleFavorite = { onToggleFavorite(selected.series) }, onPlayEpisode = onPlayEpisode,
            onPrepareCast = onPrepareCast,
            onSetEpisodeWatched = onSetEpisodeWatched, onSetSeasonWatched = onSetSeasonWatched,
            onSetSeriesWatched = onSetSeriesWatched, onClearProgress = onClearProgress, modifier = modifier
        )
        return
    }
    BackHandler(enabled = showFilters || showSort || showGroups || searchVisible) {
        when { showFilters -> showFilters = false; showSort -> showSort = false; showGroups -> showGroups = false; queryText.isNotBlank() -> { queryText = ""; onQueryChange("") }; else -> searchVisible = false }
    }
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicColors.CanvasTop, CinematicColors.Canvas, Color(0xFF020407))))) {
        val wide = maxWidth >= 720.dp
        val compactLandscape = maxHeight < 520.dp
        val pagePadding = if (wide) StreamGuideSpacing.Xl else 14.dp
        val showHero = !compactLandscape && (wide || maxHeight >= 700.dp)
        val gridColumns = when {
            maxWidth >= 1180.dp -> 7
            maxWidth >= 900.dp -> 6
            maxWidth >= 720.dp -> 5
            maxWidth >= 560.dp -> 4
            else -> 3
        }
        Column(Modifier.fillMaxSize()) {
            SeriesToolbar(library.allSeries.size, searchVisible, library.filters.activeCount,
                library.allGroups.count { group -> library.hiddenGroups.any { it.equals(group, ignoreCase = true) } },
                library.selectedCategory == SERIES_WATCHLIST,
                { searchVisible = !searchVisible }, { showFilters = true }, { showSort = true }, { showGroups = true }, { onCategorySelected(SERIES_WATCHLIST) },
                Modifier.padding(horizontal = pagePadding, vertical = 7.dp))
            if (searchVisible) CinematicSearchField(queryText, "Zoek serie, genre of acteur", { queryText = it; onQueryChange(it) }, Modifier.fillMaxWidth().padding(horizontal = pagePadding, vertical = 4.dp))
            SeriesCategoryRow(library.categories, library.selectedCategory, onCategorySelected, Modifier.padding(horizontal = pagePadding, vertical = 5.dp))
            when {
                library.isLoading && library.sourceSeriesCount == 0 -> SeriesSkeletonGrid(pagePadding, gridColumns, showHero)
                library.error != null && library.sourceSeriesCount == 0 -> SeriesStatus("Seriesbibliotheek niet bereikbaar", library.error, "Opnieuw proberen", onRetry)
                library.sourceSeriesCount == 0 -> CinematicEmptyState("Geen series beschikbaar", "Deze provider levert momenteel geen Xtream-seriesbibliotheek.")
                library.allSeries.isEmpty() -> SeriesStatus(
                    "Alle seriegroepen zijn verborgen", "Kies welke seriegroepen je weer wilt zien.", "Groepen kiezen",
                    { showGroups = true }
                )
                library.series.isEmpty() -> SeriesStatus("Geen series gevonden", "Pas je zoekopdracht of filters aan.", "Filters wissen") { queryText = ""; onQueryChange(""); onClearFilters(); onCategorySelected(SERIES_ALL) }
                else -> SeriesLibraryGrid(
                    library = library,
                    columns = gridColumns,
                    wide = wide,
                    showHero = showHero,
                    showShelves = !compactLandscape,
                    pagePadding = pagePadding,
                    gridState = gridState,
                    onSeriesSelected = onSeriesSelected,
                    onPlayEpisode = onPlayEpisode,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier.weight(1f).fillMaxWidth()
                )
            }
        }
    }
    if (showFilters) SeriesFilterDialog(library, { onFiltersChanged(it); showFilters = false }, { onClearFilters(); showFilters = false }, { showFilters = false })
    if (showSort) SeriesSortDialog(SeriesSort.entries.filter { sort ->
        when (sort) { SeriesSort.RatingDescending -> library.allSeries.any { it.series.rating != null }; SeriesSort.RecentlyUpdated -> library.allSeries.any { it.series.updatedAt != null }; else -> true }
    }, library.sort, { onSortChanged(it); showSort = false }, { showSort = false })
    if (showGroups) SeriesGroupVisibilityDialog(
        groups = library.allGroups, hiddenGroups = library.hiddenGroups,
        onGroupVisible = onGroupVisible, onShowAll = onShowAllGroups, onHideAll = onHideAllGroups,
        onDismiss = { showGroups = false }
    )
}

@Composable private fun SeriesToolbar(count: Int, searching: Boolean, filters: Int, hiddenGroups: Int, watchlist: Boolean, onSearch: () -> Unit, onFilter: () -> Unit, onSort: () -> Unit, onGroups: () -> Unit, onWatchlist: () -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text("Series", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle); Text("$count titels", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata) }
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            CinematicIconAction(Icons.Default.Search, "Zoeken", onSearch, selected = searching)
            Box { CinematicIconAction(Icons.Default.FilterList, "Filteren", onFilter, selected = filters > 0); if (filters > 0) CountBadge(filters, Modifier.align(Alignment.TopEnd)) }
            CinematicIconAction(Icons.AutoMirrored.Filled.Sort, "Sorteren", onSort)
            Box { CinematicIconAction(Icons.Default.Visibility, "Groepen", onGroups, selected = hiddenGroups > 0); if (hiddenGroups > 0) CountBadge(hiddenGroups, Modifier.align(Alignment.TopEnd)) }
            CinematicIconAction(if (watchlist) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", onWatchlist, selected = watchlist)
        }
    }
}

@Composable private fun SeriesCategoryRow(categories: List<SeriesCategory>, selected: String, onSelected: (String) -> Unit, modifier: Modifier) {
    LazyRow(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(categories, key = { it.key }) { SeriesChip(it.label, it.key == selected) { onSelected(it.key) } } }
}

@Composable
private fun SeriesLibraryGrid(
    library: SeriesLibraryState,
    columns: Int,
    wide: Boolean,
    showHero: Boolean,
    showShelves: Boolean,
    pagePadding: Dp,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onSeriesSelected: (Long?) -> Unit,
    onPlayEpisode: (EpisodeEntity, Boolean) -> Unit,
    onToggleFavorite: (SeriesEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val discoveryMode = library.selectedCategory == SERIES_ALL && library.query.isBlank() && library.filters.activeCount == 0
    val continueWatching = remember(library.allSeries) {
        library.allSeries.filter { it.hasProgress && !it.isComplete }
            .sortedByDescending { it.series.lastWatchedAt ?: Long.MIN_VALUE }
            .take(12)
    }
    val watchlist = remember(library.allSeries) {
        library.allSeries.filter { it.series.isFavorite }.take(14)
    }
    val sectionLabel = library.categories.firstOrNull { it.key == library.selectedCategory }?.label ?: "Series"
    val horizontalGap = if (wide) 12.dp else 8.dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        state = gridState,
        contentPadding = PaddingValues(horizontal = pagePadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(horizontalGap),
        verticalArrangement = Arrangement.spacedBy(if (wide) 16.dp else 12.dp)
    ) {
        if (showHero && discoveryMode) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "series-hero") {
                val hero = library.series.first()
                SeriesHero(
                    card = hero,
                    compact = !wide,
                    onPlay = { hero.primaryEpisode?.let { onPlayEpisode(it, it.isWatched) } },
                    onInfo = { onSeriesSelected(hero.series.id) },
                    onFavorite = { onToggleFavorite(hero.series) },
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        if (showShelves && discoveryMode && continueWatching.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "continue-shelf") {
                SeriesContinueShelf(continueWatching, wide, onPlayEpisode, onSeriesSelected)
            }
        }
        if (showShelves && discoveryMode && watchlist.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "watchlist-shelf") {
                SeriesWatchlistShelf(watchlist, wide, onSeriesSelected, onToggleFavorite)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }, key = "series-grid-heading") {
            SeriesSectionHeader(
                title = if (discoveryMode) "Alle series" else sectionLabel,
                count = library.series.size,
                modifier = Modifier.padding(top = if (discoveryMode) 2.dp else 0.dp)
            )
        }
        items(library.series, key = { "series-${it.series.id}" }) { card ->
            SeriesPosterCard(
                card = card,
                onClick = { onSeriesSelected(card.series.id) },
                onFavorite = { onToggleFavorite(card.series) }
            )
        }
    }
}

@Composable
private fun SeriesSectionHeader(title: String, count: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle, modifier = Modifier.weight(1f))
        Text("$count titels", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
    }
}

@Composable
private fun SeriesContinueShelf(
    cards: List<SeriesCardModel>,
    wide: Boolean,
    onPlayEpisode: (EpisodeEntity, Boolean) -> Unit,
    onSeriesSelected: (Long?) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SeriesSectionHeader("Verder kijken", cards.size)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(if (wide) 10.dp else 8.dp)
        ) {
            items(cards, key = { "continue-${it.series.id}" }) { card ->
                SeriesContinueCard(
                    card = card,
                    wide = wide,
                    onPlay = { card.primaryEpisode?.let { onPlayEpisode(it, it.isWatched) } },
                    onInfo = { onSeriesSelected(card.series.id) }
                )
            }
        }
    }
}

@Composable
private fun SeriesWatchlistShelf(
    cards: List<SeriesCardModel>,
    wide: Boolean,
    onSeriesSelected: (Long?) -> Unit,
    onToggleFavorite: (SeriesEntity) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        SeriesSectionHeader("Mijn lijst", cards.size)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(if (wide) 10.dp else 8.dp)
        ) {
            items(cards, key = { "watchlist-${it.series.id}" }) { card ->
                SeriesPosterCard(
                    card = card,
                    onClick = { onSeriesSelected(card.series.id) },
                    onFavorite = { onToggleFavorite(card.series) },
                    modifier = Modifier.width(if (wide) 112.dp else 96.dp)
                )
            }
        }
    }
}

@Composable
private fun SeriesContinueCard(
    card: SeriesCardModel,
    wide: Boolean,
    onPlay: () -> Unit,
    onInfo: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.985f else if (focused) 1.025f else 1f, tween(StreamGuideMotion.Standard), label = "series-continue-scale")
    val shape = RoundedCornerShape(StreamGuideRadii.Card)
    val episode = card.progressEpisode ?: card.primaryEpisode
    Column(
        Modifier.width(if (wide) 206.dp else 168.dp).graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 12.dp else 0.dp, shape, ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold)
            .clip(shape).background(CinematicColors.Panel)
            .border(1.dp, if (focused) CinematicColors.Gold else CinematicColors.Border, shape)
            .onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = if (episode?.streamUrl.isNullOrBlank()) onInfo else onPlay)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            SeriesArtwork(episode?.imageUrl ?: card.series.backdropUrl ?: card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, CinematicColors.Canvas.copy(alpha = 0.78f)))))
            episode?.qualityBadge()?.let { SeriesBadge(it, CinematicColors.Gold, Modifier.align(Alignment.TopStart).padding(6.dp)) }
            SeriesIconButton(Icons.Default.PlayArrow, "Verder kijken", false, onPlay, Modifier.align(Alignment.Center))
            val progress = episode?.progressFraction() ?: 0f
            if (progress > 0f) SeriesProgressBar(progress, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
        }
        Column(Modifier.fillMaxWidth().height(49.dp).padding(horizontal = 7.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.series.title, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(episode?.let { "${it.displayCode()}  |  ${it.title}" } ?: card.statusText.orEmpty(), color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun SeriesChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (selected || focused) CinematicColors.Gold.copy(alpha = .82f) else CinematicColors.Border, tween(StreamGuideMotion.Quick), label = "series-chip-border")
    val background by animateColorAsState(
        when {
            pressed -> CinematicColors.Gold.copy(alpha = .20f)
            selected -> CinematicColors.Gold.copy(alpha = .12f)
            focused -> CinematicColors.PanelPressed
            else -> CinematicColors.Panel.copy(alpha = .72f)
        }, tween(StreamGuideMotion.Quick), label = "series-chip-background"
    )
    Box(Modifier.onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction).clip(RoundedCornerShape(StreamGuideRadii.Control)).background(background).border(1.dp, border, RoundedCornerShape(StreamGuideRadii.Control)).clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(label, color = if (selected || focused) CinematicColors.GoldBright else CinematicColors.TextSecondary, style = CinematicTypography.Metadata)
    }
}

@Composable private fun SeriesHero(card: SeriesCardModel, compact: Boolean, onPlay: () -> Unit, onInfo: () -> Unit, onFavorite: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(StreamGuideRadii.Hero)
    Box(modifier.fillMaxWidth().height(if (compact) 164.dp else 218.dp).clip(shape).border(1.dp, CinematicColors.BorderStrong, shape)) {
        SeriesArtwork(card.series.backdropUrl ?: card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(CinematicColors.Canvas.copy(alpha = .99f), CinematicColors.Canvas.copy(alpha = .76f), CinematicColors.Canvas.copy(alpha = .18f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicColors.Canvas.copy(alpha = .08f), Color.Transparent, CinematicColors.Canvas.copy(alpha = .94f)))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(if (compact) .91f else .64f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(card.series.title, color = CinematicColors.TextPrimary, style = CinematicTypography.HeroTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            SeriesMetadataLine(card)
            card.series.description?.let { Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { SeriesAction(card.actionLabel, Icons.Default.PlayArrow, true, onPlay); SeriesAction("Meer informatie", Icons.Default.Info, onClick = onInfo); SeriesIconButton(if (card.series.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", card.series.isFavorite, onFavorite) }
        }
    }
}

@Composable private fun SeriesPosterCard(card: SeriesCardModel, onClick: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }; val pressed by interaction.collectIsPressedAsState(); var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .985f else if (focused) 1.035f else 1f, tween(StreamGuideMotion.Standard), label = "series-scale")
    val border by animateColorAsState(if (focused) CinematicColors.Gold else CinematicColors.Border, tween(StreamGuideMotion.Standard), label = "series-border")
    val shape = RoundedCornerShape(StreamGuideRadii.Card)
    Column(modifier.graphicsLayer { scaleX = scale; scaleY = scale }.shadow(if (focused) 12.dp else 0.dp, shape, ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold).background(CinematicColors.Panel, shape).border(1.dp, border, shape).clip(shape).onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction).clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(2.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            SeriesArtwork(card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.fillMaxSize())
            card.quality?.let { SeriesBadge(it, CinematicColors.Gold, Modifier.align(Alignment.TopStart).padding(6.dp)) }
            SeriesIconButton(if (card.series.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", card.series.isFavorite, onFavorite, Modifier.align(Alignment.TopEnd).padding(5.dp))
            if (card.isComplete) SeriesBadge("BEKEKEN", CinematicColors.PanelPressed, Modifier.align(Alignment.BottomStart).padding(6.dp))
            if (card.progressFraction > 0f && !card.isComplete) SeriesProgressBar(card.progressFraction, Modifier.align(Alignment.BottomCenter).fillMaxWidth())
        }
        Column(Modifier.fillMaxWidth().height(51.dp).padding(horizontal = 6.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(card.series.title, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(card.statusText ?: listOfNotNull(card.series.year?.toString(), card.series.genreTokens().firstOrNull()).joinToString(" | "), color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable private fun SeriesDetailScreen(card: SeriesCardModel, loading: Boolean, error: String?, onBack: () -> Unit, onRetry: () -> Unit, onToggleFavorite: () -> Unit, onPlayEpisode: (EpisodeEntity, Boolean) -> Unit, onPrepareCast: (EpisodeEntity) -> Unit, onSetEpisodeWatched: (EpisodeEntity, Boolean) -> Unit, onSetSeasonWatched: (Long, Int, Boolean) -> Unit, onSetSeriesWatched: (Long, Boolean) -> Unit, onClearProgress: (Long) -> Unit, modifier: Modifier) {
    val seasons = remember(card.episodes) { card.orderedEpisodes.groupBy { it.seasonNumber } }
    LaunchedEffect(card.primaryEpisode?.id) { card.primaryEpisode?.let(onPrepareCast) }
    var selectedSeason by rememberSaveable(card.series.id) { mutableIntStateOf(seasons.keys.firstOrNull() ?: 1) }
    LaunchedEffect(seasons.keys) { if (selectedSeason !in seasons.keys && seasons.isNotEmpty()) selectedSeason = seasons.keys.first() }
    var confirmation by remember { mutableStateOf<BulkSeriesAction?>(null) }
    val episodes = seasons[selectedSeason].orEmpty()
    val episodeListState = rememberLazyListState()
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicColors.CanvasTop, CinematicColors.Canvas, Color(0xFF020407))))) {
        val wide = maxWidth >= 720.dp
        val padding = if (wide) StreamGuideSpacing.Xl else 14.dp
        LazyColumn(
            state = episodeListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = StreamGuideSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(if (wide) 10.dp else 8.dp)
        ) {
            item { SeriesDetailHero(card, wide, onBack, onToggleFavorite) { card.primaryEpisode?.let { onPlayEpisode(it, it.isWatched) } } }
            if (card.series.description != null || card.series.originalTitle != null || card.series.director != null || card.series.cast != null) {
                item { SeriesAbout(card.series, Modifier.padding(horizontal = padding)) }
            }
            if (loading && card.episodes.isEmpty()) item { EpisodeSkeletonList(wide, Modifier.padding(horizontal = padding)) }
            else if (loading) item { LoadingStrip(Modifier.padding(horizontal = padding)) }
            error?.let { item { InlineError(it, onRetry, Modifier.padding(horizontal = padding)) } }
            if (card.episodes.isEmpty() && !loading) item { SeriesStatus("Geen afleveringen beschikbaar", "De provider leverde geen bruikbare afleveringslijst.", "Opnieuw proberen", onRetry) }
            if (seasons.isNotEmpty()) item {
                SeriesSeasonSelector(
                    seasons = seasons,
                    selectedSeason = selectedSeason,
                    onSeasonSelected = { selectedSeason = it },
                    onToggleWatched = { confirmation = BulkSeriesAction.Season(selectedSeason, !episodes.all { it.isWatched }) },
                    modifier = Modifier.padding(horizontal = padding)
                )
            }
            items(episodes, key = { it.id }) { episode -> EpisodeRow(episode, wide, { if (episode.streamUrl.isNullOrBlank()) Unit else onPlayEpisode(episode, episode.isWatched) }, { onSetEpisodeWatched(episode, !episode.isWatched) }, Modifier.padding(horizontal = padding)) }
            if (card.episodes.isNotEmpty()) item {
                Row(Modifier.padding(horizontal = padding, vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SeriesAction(if (card.isComplete) "Serie niet bekeken" else "Serie bekeken", Icons.Default.DoneAll, onClick = { confirmation = BulkSeriesAction.Series(!card.isComplete) })
                    if (card.hasProgress) SeriesAction("Voortgang wissen", Icons.Default.RestartAlt, onClick = { confirmation = BulkSeriesAction.Clear })
                }
            }
        }
    }
    confirmation?.let { action -> ConfirmBulkDialog(action, { confirmation = null }, {
        when (action) { is BulkSeriesAction.Season -> onSetSeasonWatched(card.series.id, action.number, action.watched); is BulkSeriesAction.Series -> onSetSeriesWatched(card.series.id, action.watched); BulkSeriesAction.Clear -> onClearProgress(card.series.id) }; confirmation = null
    }) }
}

@Composable
private fun SeriesSeasonSelector(
    seasons: Map<Int, List<EpisodeEntity>>,
    selectedSeason: Int,
    onSeasonSelected: (Int) -> Unit,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedEpisodes = seasons[selectedSeason].orEmpty()
    val watchedCount = selectedEpisodes.count { it.isWatched }
    Column(
        modifier.fillMaxWidth().background(CinematicColors.Panel.copy(alpha = .58f), RoundedCornerShape(StreamGuideRadii.Card))
            .border(1.dp, CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Card)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(seasonLabel(selectedSeason), color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                Text("${selectedEpisodes.size} afleveringen | $watchedCount bekeken", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
            }
            SeriesAction(
                if (selectedEpisodes.isNotEmpty() && watchedCount == selectedEpisodes.size) "Niet bekeken" else "Alles bekeken",
                Icons.Default.CheckCircle,
                onClick = onToggleWatched
            )
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(seasons.entries.toList(), key = { it.key }) { (number, values) ->
                SeriesChip(values.firstOrNull()?.seasonName?.takeIf(String::isNotBlank) ?: seasonLabel(number), number == selectedSeason) {
                    onSeasonSelected(number)
                }
            }
        }
    }
}

@Composable private fun SeriesAbout(series: SeriesEntity, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().background(CinematicColors.Panel.copy(alpha = .54f), RoundedCornerShape(StreamGuideRadii.Card))
            .border(1.dp, CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Card)).padding(11.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text("Over deze serie", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
        series.description?.let {
            Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Body)
        }
        series.originalTitle?.takeIf { it != series.title }?.let {
            SeriesCreditLine("Originele titel", it)
        }
        series.director?.let { SeriesCreditLine("Makers", it) }
        series.cast?.let { SeriesCreditLine("Cast", it) }
    }
}

@Composable private fun SeriesCreditLine(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        Text(value, color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata, modifier = Modifier.weight(1f))
    }
}

@Composable private fun SeriesDetailHero(card: SeriesCardModel, wide: Boolean, onBack: () -> Unit, onFavorite: () -> Unit, onPlay: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(if (wide) 278.dp else 238.dp)) {
        SeriesArtwork(card.series.backdropUrl ?: card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(CinematicColors.Canvas.copy(alpha = .90f), CinematicColors.Canvas.copy(alpha = .46f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicColors.Canvas.copy(alpha = .16f), Color.Transparent, CinematicColors.Canvas.copy(alpha = .98f)))))
        SeriesIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Terug", false, onBack, Modifier.align(Alignment.TopStart).padding(14.dp))
        Row(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = if (wide) 24.dp else 14.dp, vertical = 14.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SeriesArtwork(card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.width(if (wide) 112.dp else 78.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(StreamGuideRadii.Card)).border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Card)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(card.series.title, color = CinematicColors.TextPrimary, style = CinematicTypography.HeroTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
                SeriesMetadataLine(card)
                card.series.description?.let { Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, maxLines = if (wide) 3 else 2, overflow = TextOverflow.Ellipsis) }
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SeriesAction(card.actionLabel, Icons.Default.PlayArrow, true, onPlay)
                    CastRouteButton(Modifier.size(34.dp))
                    SeriesIconButton(if (card.series.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", card.series.isFavorite, onFavorite)
                }
            }
        }
    }
}

@Composable private fun EpisodeRow(episode: EpisodeEntity, wide: Boolean, onPlay: () -> Unit, onWatched: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Card)
    val scale by animateFloatAsState(if (pressed) .99f else if (focused) 1.012f else 1f, tween(StreamGuideMotion.Quick), label = "episode-scale")
    val border by animateColorAsState(if (focused) CinematicColors.Gold else CinematicColors.Border, tween(StreamGuideMotion.Quick), label = "episode-border")
    Row(modifier.fillMaxWidth().height(if (wide) 108.dp else 98.dp).graphicsLayer { scaleX = scale; scaleY = scale }.onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction).clip(shape).background(if (focused) CinematicColors.PanelPressed else CinematicColors.Panel).border(1.dp, border, shape).clickable(interactionSource = interaction, indication = null, onClick = onPlay).padding(7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (wide) 11.dp else 8.dp)) {
        Box(Modifier.width(if (wide) 168.dp else 118.dp).aspectRatio(16f / 9f).clip(RoundedCornerShape(7.dp)).background(CinematicColors.PanelRaised)) {
            SeriesArtwork(episode.imageUrl, episode.title, ContentScale.Crop, Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, CinematicColors.Canvas.copy(alpha = .42f)))))
            if (episode.isWatched) SeriesBadge("BEKEKEN", CinematicColors.PanelPressed, Modifier.align(Alignment.BottomStart).padding(5.dp))
            if (episode.streamUrl.isNullOrBlank()) Box(Modifier.fillMaxSize().background(CinematicColors.Canvas.copy(alpha = .58f)), contentAlignment = Alignment.Center) { Icon(Icons.Default.CloudOff, "Niet beschikbaar", tint = CinematicColors.TextSecondary, modifier = Modifier.size(20.dp)) }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(episode.displayCode(), color = CinematicColors.GoldBright, style = CinematicTypography.Badge); episode.qualityBadge()?.let { SeriesBadge(it, CinematicColors.Gold) }; if (episode.streamUrl.isNullOrBlank()) SeriesBadge("NIET BESCHIKBAAR", CinematicColors.Live) }
            Text(episode.title, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(episode.durationMinutes?.let { "$it min" }, episode.description?.takeIf(String::isNotBlank)).joinToString(" | ").ifBlank { "Geen beschrijving beschikbaar" }, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = if (wide) 2 else 1, overflow = TextOverflow.Ellipsis)
            val progress = episode.progressFraction()
            if (progress > 0f && !episode.isWatched) SeriesProgressBar(progress, Modifier.fillMaxWidth())
        }
        SeriesIconButton(if (episode.isWatched) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, if (episode.isWatched) "Bekeken" else "Niet bekeken", episode.isWatched, onWatched)
    }
}

private sealed interface BulkSeriesAction { data class Season(val number: Int, val watched: Boolean) : BulkSeriesAction; data class Series(val watched: Boolean) : BulkSeriesAction; data object Clear : BulkSeriesAction }

@Composable private fun ConfirmBulkDialog(action: BulkSeriesAction, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val text = when (action) { is BulkSeriesAction.Season -> if (action.watched) "Dit volledige seizoen als bekeken markeren?" else "Dit volledige seizoen als niet bekeken markeren?"; is BulkSeriesAction.Series -> if (action.watched) "De volledige serie als bekeken markeren?" else "De volledige serie als niet bekeken markeren?"; BulkSeriesAction.Clear -> "Alle kijkvoortgang van deze serie wissen?" }
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Default.Warning, null, tint = CinematicColors.Gold) }, title = { Text("Bevestigen", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle) }, text = { Text(text, color = CinematicColors.TextSecondary, style = CinematicTypography.Body) }, confirmButton = { SeriesAction("Bevestigen", Icons.Default.Done, true, onConfirm) }, dismissButton = { SeriesAction("Annuleren", Icons.Default.Close, onClick = onDismiss) }, containerColor = CinematicColors.Panel, tonalElevation = 0.dp, shape = RoundedCornerShape(StreamGuideRadii.Hero))
}

@Composable private fun SeriesMetadataLine(card: SeriesCardModel) {
    val metadata = listOfNotNull(
        card.series.year?.toString(),
        card.series.genreTokens().firstOrNull(),
        card.series.ageRating,
        card.seasonCount.takeIf { it > 0 }?.let { if (it == 1) "1 seizoen" else "$it seizoenen" }
    ).joinToString(" | ")
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(metadata.ifBlank { card.series.categoryName }, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        card.quality?.let { SeriesBadge(it, CinematicColors.Gold) }
        card.series.rating?.let { SeriesBadge(String.format("%.1f", it), CinematicColors.PanelPressed) }
    }
}

@Composable private fun SeriesAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    val bg by animateColorAsState(
        when {
            primary && (focused || pressed) -> CinematicColors.GoldBright
            primary -> CinematicColors.Gold
            pressed -> CinematicColors.PanelPressed
            focused -> CinematicColors.PanelPressed
            else -> CinematicColors.PanelRaised.copy(alpha = .88f)
        }, tween(StreamGuideMotion.Quick), label = "series-action-background"
    )
    val color = if (primary) Color(0xFF241500) else if (focused) CinematicColors.GoldBright else CinematicColors.TextPrimary
    Row(
        Modifier.height(36.dp).widthIn(max = 190.dp).onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction)
            .clip(shape).background(bg).border(1.dp, if (focused) CinematicColors.GoldBright else if (primary) CinematicColors.Gold else CinematicColors.BorderStrong, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(15.dp))
        Text(label, color = color, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable private fun SeriesIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    Box(
        modifier.size(36.dp).onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction).clip(shape)
            .background(if (selected || focused || pressed) CinematicColors.Gold.copy(alpha = .18f) else CinematicColors.Panel.copy(alpha = .84f))
            .border(1.dp, if (focused) CinematicColors.Gold else CinematicColors.BorderStrong, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, description, tint = if (selected || focused) CinematicColors.GoldBright else CinematicColors.TextSecondary, modifier = Modifier.size(18.dp))
    }
}
@Composable private fun SeriesArtwork(url: String?, description: String?, scale: ContentScale, modifier: Modifier) {
    val previewArtwork = when (url) {
        PREVIEW_POSTER_ALPINE -> R.drawable.streamguide_poster_alpine
        PREVIEW_POSTER_CITY -> R.drawable.streamguide_poster_city
        PREVIEW_POSTER_OCEAN -> R.drawable.streamguide_poster_ocean
        PREVIEW_BACKDROP -> R.drawable.streamguide_cinematic_hero
        PREVIEW_BACKDROP_SPORTS -> R.drawable.streamguide_hero_sports
        else -> null
    }
    Box(modifier.background(Brush.verticalGradient(listOf(CinematicColors.PanelPressed, CinematicColors.Panel))), contentAlignment = Alignment.Center) {
        when {
            previewArtwork != null -> Image(painterResource(previewArtwork), description, Modifier.fillMaxSize(), contentScale = scale)
            !url.isNullOrBlank() -> AsyncImage(model = url, contentDescription = description, modifier = Modifier.fillMaxSize(), contentScale = scale)
            else -> Icon(Icons.Default.VideoLibrary, null, tint = CinematicColors.Gold.copy(alpha = .66f), modifier = Modifier.size(34.dp))
        }
    }
}
@Composable private fun SeriesBadge(label: String, color: Color, modifier: Modifier = Modifier) { Box(modifier.background(color.copy(alpha = .92f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(label, color = if (color == CinematicColors.Gold) Color(0xFF241500) else CinematicColors.TextPrimary, style = CinematicTypography.Badge) } }
@Composable private fun SeriesProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier.height(3.dp).background(CinematicColors.BorderStrong)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(CinematicColors.Gold))
    }
}
@Composable private fun CountBadge(count: Int, modifier: Modifier) { Box(modifier.size(14.dp).background(CinematicColors.Gold, CircleShape), contentAlignment = Alignment.Center) { Text(count.coerceAtMost(9).toString(), color = Color(0xFF241500), style = CinematicTypography.Badge) } }
@Composable private fun SeriesStatus(title: String, description: String, action: String, onAction: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Column(
            Modifier.widthIn(max = 420.dp).background(CinematicColors.Panel.copy(alpha = .72f), RoundedCornerShape(StreamGuideRadii.Hero))
                .border(1.dp, CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Hero)).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.VideoLibrary, null, tint = CinematicColors.Gold, modifier = Modifier.size(32.dp))
            Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
            Text(description, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
            SeriesAction(action, Icons.Default.Refresh, onClick = onAction)
        }
    }
}
@Composable private fun LoadingStrip(modifier: Modifier) {
    val transition = rememberInfiniteTransition(label = "series-loading")
    val alpha by transition.animateFloat(.34f, 1f, infiniteRepeatable(tween(760), RepeatMode.Reverse), label = "series-loading-alpha")
    Row(modifier.fillMaxWidth().background(CinematicColors.PanelRaised, RoundedCornerShape(StreamGuideRadii.Control)).border(1.dp, CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Control)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(8.dp).graphicsLayer { this.alpha = alpha }.background(CinematicColors.Gold, CircleShape))
        Text("Afleveringen laden", color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata)
    }
}
@Composable private fun InlineError(message: String, retry: () -> Unit, modifier: Modifier) { Row(modifier.fillMaxWidth().background(CinematicColors.PanelRaised, RoundedCornerShape(StreamGuideRadii.Control)).border(1.dp, CinematicColors.Live.copy(alpha = .5f), RoundedCornerShape(StreamGuideRadii.Control)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(message, color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata, modifier = Modifier.weight(1f)); SeriesAction("Opnieuw", Icons.Default.Refresh, onClick = retry) } }
@Composable private fun SeriesGroupVisibilityDialog(groups: List<String>, hiddenGroups: Set<String>, onGroupVisible: (String, Boolean) -> Unit, onShowAll: () -> Unit, onHideAll: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp).widthIn(max = 560.dp).heightIn(max = 650.dp)
                .background(CinematicColors.Panel, RoundedCornerShape(StreamGuideRadii.Hero))
                .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Hero)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(36.dp).background(CinematicColors.GoldMuted, RoundedCornerShape(StreamGuideRadii.Control)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(18.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text("Seriegroepen", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                    Text("Bepaal welke groepen zichtbaar zijn", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                }
            }
            if (groups.isEmpty()) {
                Text("Er zijn nog geen seriegroepen gevonden.", color = CinematicColors.TextMuted, style = CinematicTypography.Body)
            } else {
                LazyColumn(Modifier.heightIn(max = 430.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(groups, key = { it.lowercase() }) { group ->
                        val visible = hiddenGroups.none { it.equals(group, ignoreCase = true) }
                        SeriesGroupVisibilityRow(group, visible) { onGroupVisible(group, !visible) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                SeriesAction("Alles tonen", Icons.Default.Visibility, onClick = onShowAll)
                SeriesAction("Alles verbergen", Icons.Default.VisibilityOff, onClick = onHideAll)
                SeriesAction("Klaar", Icons.Default.Done, primary = true, onClick = onDismiss)
            }
        }
    }
}
@Composable private fun SeriesGroupVisibilityRow(group: String, visible: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    Row(
        Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused }.clip(shape)
            .background(if (focused) CinematicColors.PanelPressed else CinematicColors.PanelRaised.copy(alpha = .72f))
            .border(1.dp, if (focused) CinematicColors.Gold else CinematicColors.Border, shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = if (visible) "Zichtbaar" else "Verborgen", tint = if (visible) CinematicColors.GoldBright else CinematicColors.TextMuted, modifier = Modifier.size(18.dp))
        Text(group, color = CinematicColors.TextPrimary, style = CinematicTypography.Body, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (visible) "Zichtbaar" else "Verborgen", color = if (visible) CinematicColors.Gold else CinematicColors.TextMuted, style = CinematicTypography.Metadata)
    }
}
@Composable private fun SeriesSkeletonGrid(padding: Dp, columns: Int, showHero: Boolean) {
    val transition = rememberInfiniteTransition(label = "series-grid-skeleton")
    val alpha by transition.animateFloat(.34f, .74f, infiniteRepeatable(tween(920), RepeatMode.Reverse), label = "series-grid-skeleton-alpha")
    val block = CinematicColors.PanelRaised.copy(alpha = alpha)
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        contentPadding = PaddingValues(horizontal = padding, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showHero) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().height(164.dp).background(block, RoundedCornerShape(StreamGuideRadii.Hero)))
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.width(128.dp).height(14.dp).background(block, RoundedCornerShape(4.dp)))
        }
        items(12) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(block, RoundedCornerShape(StreamGuideRadii.Card)))
                Box(Modifier.fillMaxWidth(.78f).height(10.dp).background(block, RoundedCornerShape(4.dp)))
                Box(Modifier.fillMaxWidth(.52f).height(8.dp).background(block.copy(alpha = alpha * .72f), RoundedCornerShape(4.dp)))
            }
        }
    }
}

@Composable private fun EpisodeSkeletonList(wide: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "episode-skeleton")
    val alpha by transition.animateFloat(.34f, .72f, infiniteRepeatable(tween(860), RepeatMode.Reverse), label = "episode-skeleton-alpha")
    val block = CinematicColors.PanelRaised.copy(alpha = alpha)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(4) {
            Row(
                Modifier.fillMaxWidth().height(if (wide) 108.dp else 98.dp).background(CinematicColors.Panel.copy(alpha = .74f), RoundedCornerShape(StreamGuideRadii.Card))
                    .border(1.dp, CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Card)).padding(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(Modifier.width(if (wide) 168.dp else 118.dp).aspectRatio(16f / 9f).background(block, RoundedCornerShape(7.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Box(Modifier.fillMaxWidth(.34f).height(8.dp).background(block, RoundedCornerShape(4.dp)))
                    Box(Modifier.fillMaxWidth(.76f).height(12.dp).background(block, RoundedCornerShape(4.dp)))
                    Box(Modifier.fillMaxWidth(.58f).height(8.dp).background(block.copy(alpha = alpha * .72f), RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}

@Composable private fun SeriesFilterDialog(library: SeriesLibraryState, onApply: (SeriesFilters) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) { var draft by remember(library.filters) { mutableStateOf(library.filters) }; Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Column(Modifier.fillMaxWidth().padding(18.dp).widthIn(max = 560.dp).heightIn(max = 620.dp).background(CinematicColors.Panel, RoundedCornerShape(StreamGuideRadii.Hero)).border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Hero)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Series filteren", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle); FilterRow("Genre", library.genres, draft.genre) { draft = draft.copy(genre = it) }; FilterRow("Jaar", library.years.map(Int::toString), draft.year?.toString()) { draft = draft.copy(year = it?.toIntOrNull()) }; FilterRow("Leeftijd", library.ageRatings, draft.ageRating) { draft = draft.copy(ageRating = it) }; BooleanFilter("Alleen niet afgerond", draft.onlyUnfinished) { draft = draft.copy(onlyUnfinished = it) }; BooleanFilter("Alleen Mijn lijst", draft.onlyWatchlist) { draft = draft.copy(onlyWatchlist = it) }; BooleanFilter("Met niet-bekeken afleveringen", draft.onlyWithUnwatched) { draft = draft.copy(onlyWithUnwatched = it) }; Spacer(Modifier.weight(1f)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { SeriesAction("Filters wissen", Icons.Default.Clear, onClick = onClear); SeriesAction("Toepassen", Icons.Default.CheckCircle, true) { onApply(draft) } } } } }
@Composable private fun FilterRow(title: String, values: List<String>, selected: String?, onSelected: (String?) -> Unit) { if (values.isEmpty()) return; Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, color = CinematicColors.TextSecondary, style = CinematicTypography.CardTitle); LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { item { SeriesChip("Alle", selected == null) { onSelected(null) } }; items(values, key = { it }) { SeriesChip(it, it == selected) { onSelected(it) } } } } }
@Composable private fun BooleanFilter(label: String, selected: Boolean, onSelected: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().clickable { onSelected(!selected) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, modifier = Modifier.weight(1f)); SeriesChip(if (selected) "Aan" else "Uit", selected) { onSelected(!selected) } } }
@Composable private fun SeriesSortDialog(available: List<SeriesSort>, selected: SeriesSort, onSelected: (SeriesSort) -> Unit, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = CinematicColors.Gold) }, title = { Text("Series sorteren", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle) }, text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { available.forEach { SeriesChip(it.label, it == selected) { onSelected(it) } } } }, confirmButton = {}, containerColor = CinematicColors.Panel, tonalElevation = 0.dp, shape = RoundedCornerShape(StreamGuideRadii.Hero)) }
private fun seasonLabel(number: Int) = when (number) { 0 -> "Specials"; -1 -> "Afleveringen"; else -> "Seizoen $number" }

private const val PREVIEW_POSTER_ALPINE = "android.resource://com.example.streamguidemobile/drawable/streamguide_poster_alpine"
private const val PREVIEW_POSTER_CITY = "android.resource://com.example.streamguidemobile/drawable/streamguide_poster_city"
private const val PREVIEW_POSTER_OCEAN = "android.resource://com.example.streamguidemobile/drawable/streamguide_poster_ocean"
private const val PREVIEW_BACKDROP = "android.resource://com.example.streamguidemobile/drawable/streamguide_cinematic_hero"
private const val PREVIEW_BACKDROP_SPORTS = "android.resource://com.example.streamguidemobile/drawable/streamguide_hero_sports"

private fun seriesPreviewData(): Pair<List<SeriesEntity>, List<EpisodeEntity>> {
    val titles = listOf(
        "De noordelijke grens", "Onder de stille zee", "Stad zonder schaduw", "Archief negen",
        "De laatste vuurtoren aan de rand van de wereld", "Nachtlijn", "Verborgen frequentie", "Boven het ijs",
        "De verzamelaars", "Kustwacht", "Echo van morgen", "Het lange seizoen"
    )
    val posters = listOf(PREVIEW_POSTER_ALPINE, PREVIEW_POSTER_OCEAN, PREVIEW_POSTER_CITY)
    val categories = listOf("Drama", "Documentaire", "Mysterie")
    val series = titles.mapIndexed { index, title ->
        val id = (index + 1).toLong()
        SeriesEntity(
            id = id,
            playlistId = 1,
            providerId = "preview-series-$id",
            title = title,
            originalTitle = if (index == 4) "The Last Lighthouse at the Edge of the World" else null,
            categoryName = categories[index % categories.size],
            posterUrl = posters[index % posters.size],
            backdropUrl = if (index % 2 == 0) PREVIEW_BACKDROP else PREVIEW_BACKDROP_SPORTS,
            year = 2026 - (index % 5),
            genre = if (index % 2 == 0) "Drama, Mysterie" else "Documentaire, Avontuur",
            ageRating = if (index % 3 == 0) "12+" else "9+",
            description = "Een filmisch verhaal over mensen die onverwachte aanwijzingen volgen en ontdekken dat hun vertrouwde wereld groter is dan gedacht.",
            rating = 7.2 + (index % 7) * .2,
            director = "M. de Vries",
            cast = "A. Jansen, L. Smit, N. Vermeer",
            addedAt = 1_780_000_000_000L - index * 86_400_000L,
            isFavorite = index in setOf(0, 2, 5),
            lastWatchedAt = if (index in 0..2) 1_780_000_000_000L - index * 3_600_000L else null,
            progressEpisodeId = if (index in 0..2) id * 100 + 3 else null,
            sortOrder = index
        )
    }
    val episodes = series.flatMapIndexed { seriesIndex, item ->
        val seasonCount = when (seriesIndex) { 0 -> 4; 1 -> 2; else -> 1 }
        (1..seasonCount).flatMap { season ->
            (1..7).map { number ->
                val sequence = (season - 1) * 7 + number
                val id = item.id * 100 + sequence
                val watched = seriesIndex in 0..2 && sequence <= 2
                val inProgress = seriesIndex in 0..2 && sequence == 3
                val height = when (seriesIndex % 3) { 0 -> 1080; 1 -> 2160; else -> 720 }
                EpisodeEntity(
                    id = id,
                    seriesId = item.id,
                    providerId = "preview-episode-$id",
                    seasonNumber = season,
                    seasonName = "Seizoen $season",
                    episodeNumber = number,
                    providerOrder = sequence - 1,
                    title = if (number == 4) "De onverwacht lange afleveringstitel die toch volledig herkenbaar moet blijven" else "Hoofdstuk $number: het ontbrekende spoor",
                    streamUrl = "https://preview.invalid/series/${item.id}/$sequence.m3u8",
                    imageUrl = when (number % 3) { 0 -> PREVIEW_BACKDROP_SPORTS; 1 -> PREVIEW_BACKDROP; else -> PREVIEW_POSTER_OCEAN },
                    description = "Nieuwe informatie zet de verhoudingen op scherp terwijl de volgende stap steeds moeilijker te voorspellen wordt.",
                    durationMinutes = 46 + number,
                    containerExtension = "mkv",
                    playbackPositionMs = if (inProgress) 1_140_000L else if (watched) 2_760_000L else 0L,
                    playbackDurationMs = if (inProgress || watched) 2_760_000L else 0L,
                    isWatched = watched,
                    lastWatchedAt = if (inProgress || watched) 1_780_000_000_000L - sequence * 60_000L else null,
                    resolutionWidth = when (height) { 2160 -> 3840; 1080 -> 1920; else -> 1280 },
                    resolutionHeight = height,
                    sortOrder = sequence - 1
                )
            }
        }
    }
    return series to episodes
}

@Composable private fun SeriesPreview(selectedSeriesId: Long? = null, stateTransform: (SeriesLibraryState) -> SeriesLibraryState = { it }) {
    val (series, episodes) = remember { seriesPreviewData() }
    val library = stateTransform(buildSeriesLibrary(series, episodes, "", SERIES_ALL, SeriesFilters(), SeriesSort.RecentlyAdded))
    StreamGuideTheme {
        SeriesScreen(
            library = library,
            selectedSeriesId = selectedSeriesId,
            onSeriesSelected = {}, onQueryChange = {}, onCategorySelected = {}, onFiltersChanged = {}, onClearFilters = {}, onSortChanged = {},
            onToggleFavorite = {}, onLoadDetails = { _, _ -> }, onPlayEpisode = { _, _ -> }, onPrepareCast = {}, onSetEpisodeWatched = { _, _ -> },
            onSetSeasonWatched = { _, _, _ -> }, onSetSeriesWatched = { _, _ -> }, onClearProgress = {}, onGroupVisible = { _, _ -> },
            onShowAllGroups = {}, onHideAllGroups = {}, onRetry = {}
        )
    }
}

@Preview(name = "1 Series - phone portrait", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesPhonePortraitPreview() = SeriesPreview()

@Preview(name = "2 Series - phone landscape", widthDp = 891, heightDp = 411, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesPhoneLandscapePreview() = SeriesPreview()

@Preview(name = "3 Series - tablet landscape", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesTabletLandscapePreview() = SeriesPreview()

@Preview(name = "4 Series detail - phone", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesDetailPhonePreview() = SeriesPreview(selectedSeriesId = 1)

@Preview(name = "5 Series detail - multiple seasons", widthDp = 1100, heightDp = 720, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesMultipleSeasonsPreview() = SeriesPreview(selectedSeriesId = 1)

@Preview(name = "6 Series detail - long episode list", widthDp = 800, heightDp = 1100, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesLongEpisodeListPreview() = SeriesPreview(selectedSeriesId = 1)

@Preview(name = "7 Series detail - viewing progress", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesEpisodeProgressPreview() = SeriesPreview(selectedSeriesId = 2)

@Preview(name = "Series - loading", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesLoadingPreview() = SeriesPreview(stateTransform = { SeriesLibraryState(isLoading = true) })

@Preview(name = "Series - error", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesErrorPreview() = SeriesPreview(stateTransform = { SeriesLibraryState(error = "De seriesbibliotheek kon niet worden geladen.") })
