package com.example.streamguidemobile.ui.series

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.SeriesEntity
import com.example.streamguidemobile.data.displayCode
import com.example.streamguidemobile.data.progressFraction
import com.example.streamguidemobile.data.qualityBadge
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
        val pagePadding = if (wide) StreamGuideSpacing.Xl else 14.dp
        val showHero = wide || maxHeight >= 700.dp
        Column(Modifier.fillMaxSize()) {
            SeriesToolbar(library.allSeries.size, searchVisible, library.filters.activeCount,
                library.allGroups.count { group -> library.hiddenGroups.any { it.equals(group, ignoreCase = true) } },
                library.selectedCategory == SERIES_WATCHLIST,
                { searchVisible = !searchVisible }, { showFilters = true }, { showSort = true }, { showGroups = true }, { onCategorySelected(SERIES_WATCHLIST) },
                Modifier.padding(horizontal = pagePadding, vertical = 7.dp))
            if (searchVisible) CinematicSearchField(queryText, "Zoek serie, genre of acteur", { queryText = it; onQueryChange(it) }, Modifier.fillMaxWidth().padding(horizontal = pagePadding, vertical = 4.dp))
            SeriesCategoryRow(library.categories, library.selectedCategory, onCategorySelected, Modifier.padding(horizontal = pagePadding, vertical = 5.dp))
            when {
                library.isLoading && library.sourceSeriesCount == 0 -> SeriesSkeletonGrid(pagePadding)
                library.error != null && library.sourceSeriesCount == 0 -> SeriesStatus("Seriesbibliotheek niet bereikbaar", library.error, "Opnieuw proberen", onRetry)
                library.sourceSeriesCount == 0 -> CinematicEmptyState("Geen series beschikbaar", "Deze provider levert momenteel geen Xtream-seriesbibliotheek.")
                library.allSeries.isEmpty() -> SeriesStatus(
                    "Alle seriegroepen zijn verborgen", "Kies welke seriegroepen je weer wilt zien.", "Groepen kiezen",
                    { showGroups = true }
                )
                library.series.isEmpty() -> SeriesStatus("Geen series gevonden", "Pas je zoekopdracht of filters aan.", "Filters wissen") { queryText = ""; onQueryChange(""); onClearFilters(); onCategorySelected(SERIES_ALL) }
                else -> {
                    if (showHero) SeriesHero(library.series.first(), !wide, { library.series.first().primaryEpisode?.let { onPlayEpisode(it, it.isWatched) } }, { onSeriesSelected(library.series.first().series.id) }, { onToggleFavorite(library.series.first().series) }, Modifier.padding(horizontal = pagePadding, vertical = 4.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(if (wide) 128.dp else 112.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        state = gridState,
                        contentPadding = PaddingValues(horizontal = pagePadding, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(library.series, key = { it.series.id }) { card -> SeriesPosterCard(card, { onSeriesSelected(card.series.id) }, { onToggleFavorite(card.series) }) }
                    }
                }
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

@Composable private fun SeriesChip(label: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(if (selected || focused) CinematicColors.Gold.copy(alpha = .8f) else CinematicColors.Border, tween(StreamGuideMotion.Quick), label = "series-chip")
    Box(Modifier.onFocusChanged { focused = it.isFocused }.clip(RoundedCornerShape(StreamGuideRadii.Control)).background(if (selected) CinematicColors.Gold.copy(alpha = .12f) else CinematicColors.Panel.copy(alpha = .72f)).border(1.dp, border, RoundedCornerShape(StreamGuideRadii.Control)).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(label, color = if (selected || focused) CinematicColors.GoldBright else CinematicColors.TextSecondary, style = CinematicTypography.Metadata)
    }
}

@Composable private fun SeriesHero(card: SeriesCardModel, compact: Boolean, onPlay: () -> Unit, onInfo: () -> Unit, onFavorite: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(StreamGuideRadii.Hero)
    Box(modifier.fillMaxWidth().height(if (compact) 156.dp else 214.dp).clip(shape).border(1.dp, CinematicColors.BorderStrong, shape)) {
        SeriesArtwork(card.series.backdropUrl ?: card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(CinematicColors.Canvas.copy(alpha = .98f), CinematicColors.Canvas.copy(alpha = .64f), Color.Transparent))))
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, CinematicColors.Canvas.copy(alpha = .88f)))))
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(if (compact) .88f else .66f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(card.series.title, color = CinematicColors.TextPrimary, style = CinematicTypography.HeroTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            SeriesMetadataLine(card)
            card.series.description?.let { Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis) }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { SeriesAction(card.actionLabel, Icons.Default.PlayArrow, true, onPlay); SeriesAction("Meer informatie", Icons.Default.Info, onClick = onInfo); SeriesIconButton(if (card.series.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", card.series.isFavorite, onFavorite) }
        }
    }
}

@Composable private fun SeriesPosterCard(card: SeriesCardModel, onClick: () -> Unit, onFavorite: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }; val pressed by interaction.collectIsPressedAsState(); var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) .985f else if (focused) 1.035f else 1f, tween(StreamGuideMotion.Standard), label = "series-scale")
    val border by animateColorAsState(if (focused) CinematicColors.Gold else CinematicColors.Border, tween(StreamGuideMotion.Standard), label = "series-border")
    val shape = RoundedCornerShape(StreamGuideRadii.Card)
    Column(Modifier.graphicsLayer { scaleX = scale; scaleY = scale }.shadow(if (focused) 12.dp else 0.dp, shape, ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold).background(CinematicColors.Panel, shape).border(1.dp, border, shape).clip(shape).onFocusChanged { focused = it.isFocused }.focusable(interactionSource = interaction).clickable(interactionSource = interaction, indication = null, onClick = onClick).padding(2.dp)) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            SeriesArtwork(card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.fillMaxSize())
            card.quality?.let { SeriesBadge(it, CinematicColors.Gold, Modifier.align(Alignment.TopStart).padding(6.dp)) }
            SeriesIconButton(if (card.series.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", card.series.isFavorite, onFavorite, Modifier.align(Alignment.TopEnd).padding(5.dp))
            if (card.progressFraction > 0f) Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(CinematicColors.BorderStrong)) { Box(Modifier.fillMaxWidth(card.progressFraction).height(3.dp).background(CinematicColors.Gold)) }
        }
        Column(Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) { Text(card.series.title, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(card.statusText ?: listOfNotNull(card.series.year?.toString(), card.series.genreTokens().firstOrNull()).joinToString(" · "), color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable private fun SeriesDetailScreen(card: SeriesCardModel, loading: Boolean, error: String?, onBack: () -> Unit, onRetry: () -> Unit, onToggleFavorite: () -> Unit, onPlayEpisode: (EpisodeEntity, Boolean) -> Unit, onSetEpisodeWatched: (EpisodeEntity, Boolean) -> Unit, onSetSeasonWatched: (Long, Int, Boolean) -> Unit, onSetSeriesWatched: (Long, Boolean) -> Unit, onClearProgress: (Long) -> Unit, modifier: Modifier) {
    val seasons = remember(card.episodes) { card.orderedEpisodes.groupBy { it.seasonNumber } }
    var selectedSeason by rememberSaveable(card.series.id) { mutableIntStateOf(seasons.keys.firstOrNull() ?: 1) }
    LaunchedEffect(seasons.keys) { if (selectedSeason !in seasons.keys && seasons.isNotEmpty()) selectedSeason = seasons.keys.first() }
    var confirmation by remember { mutableStateOf<BulkSeriesAction?>(null) }
    val episodes = seasons[selectedSeason].orEmpty()
    val episodeListState = rememberLazyListState()
    BoxWithConstraints(modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicColors.CanvasTop, CinematicColors.Canvas, Color(0xFF020407))))) {
        val wide = maxWidth >= 720.dp; val padding = if (wide) StreamGuideSpacing.Xl else 14.dp
        LazyColumn(state = episodeListState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { SeriesDetailHero(card, wide, onBack, onToggleFavorite) { card.primaryEpisode?.let { onPlayEpisode(it, it.isWatched) } } }
            if (card.series.description != null || card.series.originalTitle != null || card.series.director != null || card.series.cast != null) {
                item { SeriesAbout(card.series, Modifier.padding(horizontal = padding)) }
            }
            if (loading) item { LoadingStrip(Modifier.padding(horizontal = padding)) }
            error?.let { item { InlineError(it, onRetry, Modifier.padding(horizontal = padding)) } }
            if (card.episodes.isEmpty() && !loading) item { SeriesStatus("Geen afleveringen beschikbaar", "De provider leverde geen bruikbare afleveringslijst.", "Opnieuw proberen", onRetry) }
            if (seasons.isNotEmpty()) item {
                Column(Modifier.padding(horizontal = padding), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Text("Afleveringen", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle, modifier = Modifier.weight(1f)); SeriesAction(if (episodes.all { it.isWatched }) "Seizoen niet bekeken" else "Seizoen bekeken", Icons.Default.CheckCircle, onClick = { confirmation = BulkSeriesAction.Season(selectedSeason, !episodes.all { it.isWatched }) }) }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(seasons.entries.toList(), key = { it.key }) { (number, values) -> SeriesChip(values.firstOrNull()?.seasonName ?: seasonLabel(number), number == selectedSeason) { selectedSeason = number } } }
                }
            }
            items(episodes, key = { it.id }) { episode -> EpisodeRow(episode, wide, { if (episode.streamUrl.isNullOrBlank()) Unit else onPlayEpisode(episode, episode.isWatched) }, { onSetEpisodeWatched(episode, !episode.isWatched) }, Modifier.padding(horizontal = padding)) }
            if (card.episodes.isNotEmpty()) item {
                Row(Modifier.padding(horizontal = padding), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
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

@Composable private fun SeriesAbout(series: SeriesEntity, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
    Box(Modifier.fillMaxWidth().height(if (wide) 290.dp else 260.dp)) {
        SeriesArtwork(card.series.backdropUrl ?: card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(CinematicColors.Canvas.copy(alpha = .20f), CinematicColors.Canvas.copy(alpha = .96f)))))
        SeriesIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Terug", false, onBack, Modifier.align(Alignment.TopStart).padding(14.dp))
        Row(Modifier.align(Alignment.BottomStart).padding(16.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SeriesArtwork(card.series.posterUrl, card.series.title, ContentScale.Crop, Modifier.width(if (wide) 112.dp else 82.dp).aspectRatio(2f / 3f).clip(RoundedCornerShape(StreamGuideRadii.Card)).border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Card)))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(card.series.title, color = CinematicColors.TextPrimary, style = CinematicTypography.HeroTitle, maxLines = 2); SeriesMetadataLine(card); card.series.description?.let { Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, maxLines = if (wide) 4 else 2, overflow = TextOverflow.Ellipsis) }; Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { SeriesAction(card.actionLabel, Icons.Default.PlayArrow, true, onPlay); SeriesIconButton(if (card.series.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Mijn lijst", card.series.isFavorite, onFavorite) } }
        }
    }
}

@Composable private fun EpisodeRow(episode: EpisodeEntity, wide: Boolean, onPlay: () -> Unit, onWatched: () -> Unit, modifier: Modifier) {
    var focused by remember { mutableStateOf(false) }; val shape = RoundedCornerShape(StreamGuideRadii.Card)
    Row(modifier.fillMaxWidth().height(if (wide) 112.dp else 94.dp).onFocusChanged { focused = it.isFocused }.clip(shape).background(if (focused) CinematicColors.PanelPressed else CinematicColors.Panel).border(1.dp, if (focused) CinematicColors.Gold else CinematicColors.Border, shape).clickable(onClick = onPlay).padding(7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SeriesArtwork(episode.imageUrl, episode.title, ContentScale.Crop, Modifier.width(if (wide) 150.dp else 112.dp).fillMaxHeight().clip(RoundedCornerShape(7.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text(episode.displayCode(), color = CinematicColors.GoldBright, style = CinematicTypography.Badge); episode.qualityBadge()?.let { SeriesBadge(it, CinematicColors.Gold) }; if (episode.streamUrl.isNullOrBlank()) SeriesBadge("NIET BESCHIKBAAR", CinematicColors.Live) }
            Text(episode.title, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(listOfNotNull(episode.durationMinutes?.let { "$it min" }, episode.description).joinToString(" · ").ifBlank { "Geen beschrijving beschikbaar" }, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = if (wide) 2 else 1, overflow = TextOverflow.Ellipsis)
            val progress = episode.progressFraction(); if (progress > 0f) Box(Modifier.fillMaxWidth().height(3.dp).background(CinematicColors.BorderStrong)) { Box(Modifier.fillMaxWidth(progress).height(3.dp).background(CinematicColors.Gold)) }
        }
        SeriesIconButton(if (episode.isWatched) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, if (episode.isWatched) "Bekeken" else "Niet bekeken", episode.isWatched, onWatched)
    }
}

private sealed interface BulkSeriesAction { data class Season(val number: Int, val watched: Boolean) : BulkSeriesAction; data class Series(val watched: Boolean) : BulkSeriesAction; data object Clear : BulkSeriesAction }

@Composable private fun ConfirmBulkDialog(action: BulkSeriesAction, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val text = when (action) { is BulkSeriesAction.Season -> if (action.watched) "Dit volledige seizoen als bekeken markeren?" else "Dit volledige seizoen als niet bekeken markeren?"; is BulkSeriesAction.Series -> if (action.watched) "De volledige serie als bekeken markeren?" else "De volledige serie als niet bekeken markeren?"; BulkSeriesAction.Clear -> "Alle kijkvoortgang van deze serie wissen?" }
    AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.Default.Warning, null, tint = CinematicColors.Gold) }, title = { Text("Bevestigen", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle) }, text = { Text(text, color = CinematicColors.TextSecondary, style = CinematicTypography.Body) }, confirmButton = { SeriesAction("Bevestigen", Icons.Default.Done, true, onConfirm) }, dismissButton = { SeriesAction("Annuleren", Icons.Default.Close, onClick = onDismiss) }, containerColor = CinematicColors.Panel, tonalElevation = 0.dp, shape = RoundedCornerShape(StreamGuideRadii.Hero))
}

@Composable private fun SeriesMetadataLine(card: SeriesCardModel) { Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { listOfNotNull(card.series.year?.toString(), card.series.genreTokens().firstOrNull(), card.series.ageRating, card.seasonCount.takeIf { it > 0 }?.let { if (it == 1) "1 seizoen" else "$it seizoenen" }).forEach { Text(it, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata) }; card.quality?.let { SeriesBadge(it, CinematicColors.Gold) }; card.series.rating?.let { Text("★ ${"%.1f".format(it)}", color = CinematicColors.GoldBright, style = CinematicTypography.Metadata) } } }

@Composable private fun SeriesAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, primary: Boolean = false, onClick: () -> Unit) { var focused by remember { mutableStateOf(false) }; val shape = RoundedCornerShape(StreamGuideRadii.Control); val bg = if (primary) CinematicColors.Gold else if (focused) CinematicColors.PanelPressed else CinematicColors.PanelRaised.copy(alpha = .88f); val color = if (primary) Color(0xFF241500) else if (focused) CinematicColors.GoldBright else CinematicColors.TextPrimary; Row(Modifier.height(34.dp).widthIn(max = 190.dp).onFocusChanged { focused = it.isFocused }.clip(shape).background(bg).border(1.dp, if (focused) CinematicColors.GoldBright else if (primary) CinematicColors.Gold else CinematicColors.BorderStrong, shape).clickable(onClick = onClick).padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Icon(icon, null, tint = color, modifier = Modifier.size(15.dp)); Text(label, color = color, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
@Composable private fun SeriesIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) { var focused by remember { mutableStateOf(false) }; val shape = RoundedCornerShape(StreamGuideRadii.Control); Box(modifier.size(32.dp).onFocusChanged { focused = it.isFocused }.clip(shape).background(if (selected || focused) CinematicColors.Gold.copy(alpha = .16f) else CinematicColors.Panel.copy(alpha = .82f)).border(1.dp, if (focused) CinematicColors.Gold else CinematicColors.BorderStrong, shape).clickable(onClick = onClick), contentAlignment = Alignment.Center) { Icon(icon, description, tint = if (selected || focused) CinematicColors.GoldBright else CinematicColors.TextSecondary, modifier = Modifier.size(17.dp)) } }
@Composable private fun SeriesArtwork(url: String?, description: String?, scale: ContentScale, modifier: Modifier) { Box(modifier.background(Brush.verticalGradient(listOf(CinematicColors.PanelPressed, CinematicColors.Panel))), contentAlignment = Alignment.Center) { if (!url.isNullOrBlank()) AsyncImage(model = url, contentDescription = description, modifier = Modifier.fillMaxSize(), contentScale = scale) else Icon(Icons.Default.VideoLibrary, null, tint = CinematicColors.Gold.copy(alpha = .66f), modifier = Modifier.size(34.dp)) } }
@Composable private fun SeriesBadge(label: String, color: Color, modifier: Modifier = Modifier) { Box(modifier.background(color.copy(alpha = .92f), RoundedCornerShape(5.dp)).padding(horizontal = 5.dp, vertical = 2.dp)) { Text(label, color = if (color == CinematicColors.Gold) Color(0xFF241500) else CinematicColors.TextPrimary, style = CinematicTypography.Badge) } }
@Composable private fun CountBadge(count: Int, modifier: Modifier) { Box(modifier.size(14.dp).background(CinematicColors.Gold, CircleShape), contentAlignment = Alignment.Center) { Text(count.coerceAtMost(9).toString(), color = Color(0xFF241500), style = CinematicTypography.Badge) } }
@Composable private fun SeriesStatus(title: String, description: String, action: String, onAction: () -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.VideoLibrary, null, tint = CinematicColors.Gold, modifier = Modifier.size(34.dp)); Spacer(Modifier.height(10.dp)); Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle); Text(description, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata); Spacer(Modifier.height(12.dp)); SeriesAction(action, Icons.Default.Refresh, onClick = onAction) } }
@Composable private fun LoadingStrip(modifier: Modifier) { Row(modifier.fillMaxWidth().background(CinematicColors.PanelRaised, RoundedCornerShape(StreamGuideRadii.Control)).padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Box(Modifier.size(8.dp).background(CinematicColors.Gold, CircleShape)); Text("Afleveringen laden", color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata) } }
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
@Composable private fun SeriesSkeletonGrid(padding: Dp) { LazyVerticalGrid(GridCells.Adaptive(112.dp), contentPadding = PaddingValues(horizontal = padding, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(10) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(CinematicColors.PanelRaised, RoundedCornerShape(StreamGuideRadii.Card))); Box(Modifier.fillMaxWidth(.78f).height(10.dp).background(CinematicColors.PanelPressed, RoundedCornerShape(4.dp))) } } } }

@Composable private fun SeriesFilterDialog(library: SeriesLibraryState, onApply: (SeriesFilters) -> Unit, onClear: () -> Unit, onDismiss: () -> Unit) { var draft by remember(library.filters) { mutableStateOf(library.filters) }; Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) { Column(Modifier.fillMaxWidth().padding(18.dp).widthIn(max = 560.dp).heightIn(max = 620.dp).background(CinematicColors.Panel, RoundedCornerShape(StreamGuideRadii.Hero)).border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(StreamGuideRadii.Hero)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Series filteren", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle); FilterRow("Genre", library.genres, draft.genre) { draft = draft.copy(genre = it) }; FilterRow("Jaar", library.years.map(Int::toString), draft.year?.toString()) { draft = draft.copy(year = it?.toIntOrNull()) }; FilterRow("Leeftijd", library.ageRatings, draft.ageRating) { draft = draft.copy(ageRating = it) }; BooleanFilter("Alleen niet afgerond", draft.onlyUnfinished) { draft = draft.copy(onlyUnfinished = it) }; BooleanFilter("Alleen Mijn lijst", draft.onlyWatchlist) { draft = draft.copy(onlyWatchlist = it) }; BooleanFilter("Met niet-bekeken afleveringen", draft.onlyWithUnwatched) { draft = draft.copy(onlyWithUnwatched = it) }; Spacer(Modifier.weight(1f)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) { SeriesAction("Filters wissen", Icons.Default.Clear, onClick = onClear); SeriesAction("Toepassen", Icons.Default.CheckCircle, true) { onApply(draft) } } } } }
@Composable private fun FilterRow(title: String, values: List<String>, selected: String?, onSelected: (String?) -> Unit) { if (values.isEmpty()) return; Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(title, color = CinematicColors.TextSecondary, style = CinematicTypography.CardTitle); LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) { item { SeriesChip("Alle", selected == null) { onSelected(null) } }; items(values, key = { it }) { SeriesChip(it, it == selected) { onSelected(it) } } } } }
@Composable private fun BooleanFilter(label: String, selected: Boolean, onSelected: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth().clickable { onSelected(!selected) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(label, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, modifier = Modifier.weight(1f)); SeriesChip(if (selected) "Aan" else "Uit", selected) { onSelected(!selected) } } }
@Composable private fun SeriesSortDialog(available: List<SeriesSort>, selected: SeriesSort, onSelected: (SeriesSort) -> Unit, onDismiss: () -> Unit) { AlertDialog(onDismissRequest = onDismiss, icon = { Icon(Icons.AutoMirrored.Filled.Sort, null, tint = CinematicColors.Gold) }, title = { Text("Series sorteren", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle) }, text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { available.forEach { SeriesChip(it.label, it == selected) { onSelected(it) } } } }, confirmButton = {}, containerColor = CinematicColors.Panel, tonalElevation = 0.dp, shape = RoundedCornerShape(StreamGuideRadii.Hero)) }
private fun seasonLabel(number: Int) = when (number) { 0 -> "Specials"; -1 -> "Afleveringen"; else -> "Seizoen $number" }

@Preview(name = "Series phone", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Preview(name = "Series phone landscape", widthDp = 891, heightDp = 411, showBackground = true, backgroundColor = 0xFF04070B)
@Preview(name = "Series tablet landscape", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF04070B)
@Composable private fun SeriesPreview() { val series = SeriesEntity(1,1,"s1","Northern Signal",categoryName="Drama",year=2025,genre="Drama",description="Een rechercheur ontdekt een signaal dat haar stad voorgoed verandert.",sortOrder=0); val episodes=(1..8).map { EpisodeEntity(it.toLong(),1,"e$it",1,"Seizoen 1",it,it-1,"Aflevering $it","https://example.com/$it.mp4",durationMinutes=48,playbackPositionMs=if(it==3)120000 else 0,playbackDurationMs=if(it==3)2800000 else 0,sortOrder=it-1) }; StreamGuideTheme { SeriesScreen(buildSeriesLibrary(listOf(series),episodes,"",SERIES_ALL,SeriesFilters(),SeriesSort.RecentlyAdded),null,{}, {},{}, {},{}, {},{}, {_,_->}, {_,_->}, {_,_->}, {_,_,_->}, {_,_->}, {}, {_,_->}, {}, {}, {}) } }
