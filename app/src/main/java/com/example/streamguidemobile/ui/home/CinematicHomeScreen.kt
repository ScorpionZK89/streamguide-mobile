package com.example.streamguidemobile.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
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
import coil.compose.AsyncImage
import com.example.streamguidemobile.ChannelRowState
import com.example.streamguidemobile.StreamGuideState
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.MovieEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.data.qualityBadge
import com.example.streamguidemobile.ui.movies.progressFraction
import com.example.streamguidemobile.ui.series.SeriesCardModel
import com.example.streamguidemobile.ui.theme.StreamGuideColors
import com.example.streamguidemobile.ui.theme.StreamGuideMotion
import com.example.streamguidemobile.ui.theme.StreamGuideRadii
import com.example.streamguidemobile.ui.theme.StreamGuideSpacing
import com.example.streamguidemobile.ui.theme.StreamGuideTypography

@Composable
fun CinematicHomeScreen(
    state: StreamGuideState,
    onOpen: (ChannelEntity) -> Unit,
    onBrowseLive: () -> Unit = {},
    onOpenMovie: (MovieEntity) -> Unit = {},
    onOpenSeries: (SeriesCardModel) -> Unit = {},
    onRemoveChannelHistory: (ChannelEntity) -> Unit = {},
    onRemoveMovieProgress: (Long) -> Unit = {},
    onRemoveSeriesProgress: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val content = remember(state.homeRows, state.nowMillis) { buildHomeContent(state.homeRows) }
    val movieContinue = remember(state.movieLibrary.allMovies) {
        state.movieLibrary.allMovies.filter { it.playbackPositionMs > 0L && !it.isWatched }
            .sortedByDescending { it.lastWatchedAt }.distinctBy { it.id }.take(10)
    }
    val seriesContinue = remember(state.seriesLibrary.allSeries) {
        state.seriesLibrary.allSeries.filter { it.hasProgress && !it.isComplete }
            .sortedByDescending { it.series.lastWatchedAt }.take(10)
    }
    var infoRow by remember { mutableStateOf<ChannelRowState?>(null) }
    var pendingRemoval by remember { mutableStateOf<ContinueWatchingRemoval?>(null) }

    BoxWithConstraints(
        modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to StreamGuideColors.CanvasTop,
                0.34f to StreamGuideColors.Canvas,
                1f to StreamGuideColors.CanvasDeep
            )
        )
    ) {
        val wide = maxWidth >= 720.dp
        val compactHeight = maxHeight < 500.dp
        val horizontalPadding = if (wide) StreamGuideSpacing.Xl else 14.dp
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = StreamGuideSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeroBanner(
                    row = content.hero,
                    wide = wide,
                    compactHeight = compactHeight,
                    onWatch = { content.hero?.channel?.let(onOpen) },
                    onInfo = { infoRow = content.hero },
                    modifier = Modifier.padding(horizontal = horizontalPadding, vertical = 6.dp)
                )
            }
            if (content.continueWatching.isNotEmpty()) {
                item {
                    HomeSection("Verder kijken", horizontalPadding) {
                        items(content.continueWatching, key = { it.channel.id }) { row ->
                            ContinueCard(
                                row = row,
                                onOpen = onOpen,
                                onRemove = { pendingRemoval = ContinueWatchingRemoval.LiveChannel(row.channel) }
                            )
                        }
                    }
                }
            }
            if (movieContinue.isNotEmpty()) {
                item {
                    HomeSection("Films verder kijken", horizontalPadding) {
                        items(movieContinue, key = { "movie-${it.id}" }) { movie ->
                            MovieContinueCard(
                                movie = movie,
                                onOpen = onOpenMovie,
                                onRemove = { pendingRemoval = ContinueWatchingRemoval.Movie(movie) }
                            )
                        }
                    }
                }
            }
            if (seriesContinue.isNotEmpty()) {
                item {
                    HomeSection("Series verder kijken", horizontalPadding) {
                        items(seriesContinue, key = { "series-${it.series.id}" }) { card ->
                            SeriesContinueCard(
                                card = card,
                                onOpen = onOpenSeries,
                                onRemove = { pendingRemoval = ContinueWatchingRemoval.Series(card) }
                            )
                        }
                    }
                }
            }
            item {
                HomeSection("Nu live", horizontalPadding, trailing = "Alles bekijken", onTrailingClick = onBrowseLive) {
                    items(content.liveNow, key = { it.channel.id }) { row ->
                        LiveCard(row, onOpen)
                    }
                }
            }
            item {
                HomeSection("Aanbevolen voor jou", horizontalPadding) {
                    items(content.recommended, key = { it.channel.id }) { row ->
                        RecommendationCard(row, onOpen)
                    }
                }
            }
        }
    }

    infoRow?.let { row ->
        HomeInfoDialog(row = row, onWatch = { infoRow = null; onOpen(row.channel) }, onDismiss = { infoRow = null })
    }

    pendingRemoval?.let { removal ->
        ContinueWatchingRemovalDialog(
            title = removal.title,
            onConfirm = {
                pendingRemoval = null
                when (removal) {
                    is ContinueWatchingRemoval.LiveChannel -> onRemoveChannelHistory(removal.channel)
                    is ContinueWatchingRemoval.Movie -> onRemoveMovieProgress(removal.movie.id)
                    is ContinueWatchingRemoval.Series -> onRemoveSeriesProgress(removal.card.series.id)
                }
            },
            onDismiss = { pendingRemoval = null }
        )
    }
}

@Composable
private fun SeriesContinueCard(card: SeriesCardModel, onOpen: (SeriesCardModel) -> Unit, onRemove: () -> Unit) {
    MediaCardFrame(width = 112.dp, onClick = { onOpen(card) }, onLongClick = onRemove) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(StreamGuideColors.PanelPressed), contentAlignment = Alignment.Center) {
            if (!card.series.posterUrl.isNullOrBlank()) {
                AsyncImage(
                    model = card.series.posterUrl,
                    contentDescription = card.series.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else Icon(Icons.Default.Movie, contentDescription = null, tint = StreamGuideColors.Gold, modifier = Modifier.size(28.dp))
            card.quality?.let {
                Box(Modifier.align(Alignment.TopStart).padding(6.dp)) {
                    MediaBadge(it, StreamGuideColors.Gold)
                }
            }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(StreamGuideColors.BorderStrong)) {
                Box(Modifier.fillMaxWidth(card.progressFraction).height(3.dp).background(StreamGuideColors.Gold))
            }
        }
        Text(card.series.title, color = StreamGuideColors.TextPrimary, style = StreamGuideTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
        card.statusText?.let { Text(it, color = StreamGuideColors.TextMuted, style = StreamGuideTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp)) }
    }
}

@Composable
private fun MovieContinueCard(movie: MovieEntity, onOpen: (MovieEntity) -> Unit, onRemove: () -> Unit) {
    MediaCardFrame(width = 112.dp, onClick = { onOpen(movie) }, onLongClick = onRemove) {
        Box(Modifier.fillMaxWidth().aspectRatio(2f / 3f).background(StreamGuideColors.PanelPressed), contentAlignment = Alignment.Center) {
            if (!movie.posterUrl.isNullOrBlank()) {
                AsyncImage(model = movie.posterUrl, contentDescription = movie.title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StreamGuideColors.Gold, modifier = Modifier.size(30.dp))
            }
            movie.qualityBadge()?.let { Box(Modifier.align(Alignment.TopStart).padding(7.dp)) { MediaBadge(it, StreamGuideColors.Gold) } }
            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(3.dp).background(StreamGuideColors.BorderStrong)) {
                Box(Modifier.fillMaxWidth(movie.progressFraction()).height(3.dp).background(StreamGuideColors.Gold))
            }
        }
        Column(Modifier.padding(horizontal = 4.dp, vertical = 5.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(movie.title, color = StreamGuideColors.TextPrimary, style = StreamGuideTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Hervatten", color = StreamGuideColors.TextMuted, style = StreamGuideTypography.Metadata)
        }
    }
}

@Composable
private fun HeroBanner(
    row: ChannelRowState?,
    wide: Boolean,
    compactHeight: Boolean,
    onWatch: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(StreamGuideRadii.Hero)
    val title = row?.currentProgram?.title ?: row?.channel?.name ?: "Welkom bij StreamGuide"
    val category = row?.currentProgram?.category ?: row?.channel?.groupTitle ?: "Jouw persoonlijke televisie"
    val description = row?.currentProgram?.description
        ?: row?.let { "Kijk nu live naar ${it.channel.name} en ontdek wat er straks wordt uitgezonden." }
        ?: "Voeg een eigen legale bron toe om je live aanbod hier te zien."

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(
                when {
                    compactHeight -> 21f / 9f
                    wide -> 2.2f
                    else -> 16f / 9f
                }
            )
            .clip(shape)
            .border(1.dp, StreamGuideColors.Border, shape)
    ) {
        MediaArtwork(
            row = row,
            contentDescription = title,
            mode = ArtworkMode.Hero,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    colors = listOf(StreamGuideColors.Scrim, StreamGuideColors.Canvas.copy(alpha = 0.78f), Color.Transparent),
                    start = Offset.Zero,
                    end = Offset(if (wide) 1100f else 780f, 0f)
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color.Transparent, StreamGuideColors.Canvas.copy(alpha = 0.72f)))
            )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (wide) 0.52f else 0.82f)
                .padding(if (wide && !compactHeight) 24.dp else 13.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                MediaBadge("LIVE", StreamGuideColors.Live)
                row?.let { qualityBadge(it)?.let { badge -> MediaBadge(badge, StreamGuideColors.Gold) } }
                Text(category, color = StreamGuideColors.TextSecondary, style = StreamGuideTypography.Metadata, maxLines = 1)
            }
            Text(title, color = StreamGuideColors.TextPrimary, style = StreamGuideTypography.HeroTitle, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                description,
                color = StreamGuideColors.TextSecondary,
                style = StreamGuideTypography.Body,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CinematicActionButton("Nu kijken", Icons.Default.PlayArrow, primary = true, enabled = row != null, onClick = onWatch)
                CinematicActionButton("Meer informatie", Icons.Default.Info, enabled = row != null, onClick = onInfo)
            }
        }
    }
}

@Composable
private fun HomeSection(
    title: String,
    horizontalPadding: Dp,
    trailing: String? = null,
    onTrailingClick: () -> Unit = {},
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, color = StreamGuideColors.TextPrimary, style = StreamGuideTypography.SectionTitle, modifier = Modifier.weight(1f))
            trailing?.let {
                Text(
                    it,
                    color = StreamGuideColors.Gold,
                    style = StreamGuideTypography.Metadata,
                    modifier = Modifier.clickable(onClick = onTrailingClick).padding(vertical = 6.dp)
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm),
            content = content
        )
    }
}

@Composable
private fun ContinueCard(row: ChannelRowState, onOpen: (ChannelEntity) -> Unit, onRemove: () -> Unit) {
    MediaCardFrame(width = 164.dp, onClick = { onOpen(row.channel) }, onLongClick = onRemove) {
        Box {
            MediaArtwork(row, row.channel.name, ArtworkMode.Live, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            if (row.progress > 0f) {
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.2f))) {
                    Box(Modifier.fillMaxWidth(row.progress).height(3.dp).background(StreamGuideColors.Gold))
                }
            }
        }
        CardText(row, showNext = true)
    }
}

@Composable
private fun LiveCard(row: ChannelRowState, onOpen: (ChannelEntity) -> Unit) {
    MediaCardFrame(width = 128.dp, onClick = { onOpen(row.channel) }) {
        Box {
            MediaArtwork(row, row.channel.name, ArtworkMode.Live, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, StreamGuideColors.Canvas.copy(alpha = 0.34f)))
                )
            )
            Row(
                modifier = Modifier.align(Alignment.TopStart).padding(7.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MediaBadge("LIVE", StreamGuideColors.Live)
                qualityBadge(row)?.let { MediaBadge(it, StreamGuideColors.Gold) }
            }
        }
        CardText(row, showNext = true)
    }
}

@Composable
private fun RecommendationCard(row: ChannelRowState, onOpen: (ChannelEntity) -> Unit) {
    MediaCardFrame(width = 104.dp, onClick = { onOpen(row.channel) }) {
        Box {
            MediaArtwork(row, row.channel.name, ArtworkMode.Poster, Modifier.fillMaxWidth().aspectRatio(2f / 3f))
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Transparent, StreamGuideColors.Canvas.copy(alpha = 0.48f))
                    )
                )
            )
            qualityBadge(row)?.let {
                Box(Modifier.align(Alignment.TopStart).padding(7.dp)) { MediaBadge(it, StreamGuideColors.Gold) }
            }
        }
        CardText(row)
    }
}

@Composable
private fun CardText(row: ChannelRowState, showNext: Boolean = false) {
    val metadata = if (showNext) {
        row.nextProgram?.title?.let { "Straks: $it" } ?: row.channel.groupTitle
    } else {
        row.currentProgram?.category?.takeIf { it.isNotBlank() } ?: row.channel.groupTitle
    }
    Column(Modifier.padding(top = 5.dp, start = 3.dp, end = 3.dp, bottom = 3.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            row.currentProgram?.title ?: row.channel.name,
            color = StreamGuideColors.TextPrimary,
            style = StreamGuideTypography.CardTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            metadata,
            color = StreamGuideColors.TextMuted,
            style = StreamGuideTypography.Metadata,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MediaCardFrame(
    width: Dp,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = when { pressed -> 0.985f; focused -> 1.035f; else -> 1f },
        animationSpec = tween(StreamGuideMotion.Standard),
        label = "media-card-scale"
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) StreamGuideColors.Gold else StreamGuideColors.Border,
        animationSpec = tween(StreamGuideMotion.Standard),
        label = "media-card-border"
    )
    val shape = RoundedCornerShape(StreamGuideRadii.Card)
    val inputModifier = if (onLongClick == null) {
        Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
    } else {
        Modifier.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onLongClickLabel = "Uit Verder kijken verwijderen",
            onLongClick = onLongClick,
            onClick = onClick
        )
    }
    Column(
        modifier = Modifier
            .width(width)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 12.dp else 0.dp, shape, ambientColor = StreamGuideColors.Gold, spotColor = StreamGuideColors.Gold)
            .background(StreamGuideColors.Panel, shape)
            .border(1.dp, borderColor, shape)
            .clip(shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interactionSource)
            .then(inputModifier)
            .padding(2.dp),
        content = content
    )
}

private sealed interface ContinueWatchingRemoval {
    val title: String

    data class LiveChannel(val channel: ChannelEntity) : ContinueWatchingRemoval {
        override val title: String = channel.name
    }

    data class Movie(val movie: MovieEntity) : ContinueWatchingRemoval {
        override val title: String = movie.title
    }

    data class Series(val card: SeriesCardModel) : ContinueWatchingRemoval {
        override val title: String = card.series.title
    }
}

private enum class ArtworkMode { Hero, Live, Poster }

@Composable
private fun MediaArtwork(
    row: ChannelRowState?,
    contentDescription: String?,
    mode: ArtworkMode,
    modifier: Modifier = Modifier
) {
    val programArtwork = row?.currentProgram?.iconUrl?.takeIf { it.isNotBlank() }
    val channelLogo = row?.channel?.logoUrl?.takeIf { it.isNotBlank() }
    val logoPadding = when (mode) {
        ArtworkMode.Hero -> 52.dp
        ArtworkMode.Live -> 16.dp
        ArtworkMode.Poster -> 20.dp
    }
    Box(
        modifier.background(
            Brush.verticalGradient(
                listOf(StreamGuideColors.PanelPressed, StreamGuideColors.Panel)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (channelLogo != null) {
            AsyncImage(
                model = channelLogo,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(logoPadding)
            )
        } else {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = StreamGuideColors.Gold.copy(alpha = 0.72f),
                modifier = Modifier.size(if (mode == ArtworkMode.Hero) 44.dp else 30.dp)
            )
        }
        if (programArtwork != null) {
            AsyncImage(
                model = programArtwork,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun MediaBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = if (color == StreamGuideColors.Gold) 0.92f else 1f), RoundedCornerShape(5.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(label, color = if (color == StreamGuideColors.Gold) StreamGuideColors.OnGold else Color.White, style = StreamGuideTypography.Badge)
    }
}

@Composable
private fun CinematicActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    primary: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val background = if (primary) StreamGuideColors.Gold else StreamGuideColors.PanelRaised.copy(alpha = 0.76f)
    val content = if (primary) StreamGuideColors.OnGold else StreamGuideColors.TextPrimary
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(StreamGuideRadii.Control))
            .background(if (enabled) background else background.copy(alpha = 0.45f))
            .border(1.dp, if (primary) Color.Transparent else StreamGuideColors.Border, RoundedCornerShape(StreamGuideRadii.Control))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(15.dp))
        Text(label, color = content, fontWeight = FontWeight.SemiBold, style = StreamGuideTypography.Metadata)
    }
}

@Composable
private fun HomeInfoDialog(row: ChannelRowState, onWatch: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(StreamGuideRadii.Hero)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StreamGuideColors.PanelRaised, shape)
                .border(1.dp, StreamGuideColors.BorderStrong, shape)
                .padding(StreamGuideSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md)
        ) {
            Text(row.currentProgram?.title ?: row.channel.name, color = StreamGuideColors.TextPrimary, style = StreamGuideTypography.SectionTitle)
            Text(row.currentProgram?.description ?: "Live op ${row.channel.name}.", color = StreamGuideColors.TextSecondary)
            Text(row.channel.groupTitle, color = StreamGuideColors.Gold, style = StreamGuideTypography.Metadata)
            Row(horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm)) {
                CinematicActionButton("Nu kijken", Icons.Default.PlayArrow, primary = true, enabled = true, onClick = onWatch)
                CinematicActionButton("Sluiten", Icons.Default.Info, enabled = true, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun ContinueWatchingRemovalDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(StreamGuideRadii.Hero)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StreamGuideColors.PanelRaised, shape)
                .border(1.dp, StreamGuideColors.BorderStrong, shape)
                .padding(StreamGuideSpacing.Xl),
            verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md)
        ) {
            Text("Uit Verder kijken verwijderen?", color = StreamGuideColors.TextPrimary, style = StreamGuideTypography.SectionTitle)
            Text(
                "De kijkvoortgang van $title wordt gewist. De titel zelf en je favorieten blijven behouden.",
                color = StreamGuideColors.TextSecondary,
                style = StreamGuideTypography.Body
            )
            Row(horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm)) {
                CinematicActionButton("Verwijderen", Icons.Default.Delete, primary = true, enabled = true, onClick = onConfirm)
                CinematicActionButton("Annuleren", Icons.Default.Close, enabled = true, onClick = onDismiss)
            }
        }
    }
}

@Preview(name = "Home portrait", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun CinematicHomePortraitPreview() {
    CinematicHomeScreen(state = previewState(), onOpen = {})
}

@Preview(name = "Home landscape", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun CinematicHomeLandscapePreview() {
    CinematicHomeScreen(state = previewState(), onOpen = {})
}

private fun previewState(): StreamGuideState {
    val now = System.currentTimeMillis()
    val rows = listOf(
        previewRow(1, "Horizon Live", "Beyond the Valley", "Documentaire", now, watchedAt = now - 10_000, progress = 0.64f, favorite = true),
        previewRow(2, "World News", "Global Perspective", "Nieuws", now, progress = 0.38f),
        previewRow(3, "Arena Sports", "Live Championship", "Sport", now, progress = 0.77f),
        previewRow(4, "Nature One", "Wild Planet", "Natuur", now, watchedAt = now - 30_000, progress = 0.42f),
        previewRow(5, "Cinema Select", "Night Drive", "Film", now, favorite = true),
        previewRow(6, "Discovery Lab", "Engineering Wonders", "Documentaire", now)
    )
    return StreamGuideState(homeRows = rows, channelRows = rows, channels = rows.map { it.channel }, nowMillis = now)
}

private fun previewRow(
    id: Long,
    channelName: String,
    title: String,
    category: String,
    now: Long,
    watchedAt: Long? = null,
    progress: Float = 0.5f,
    favorite: Boolean = false
): ChannelRowState = ChannelRowState(
    channel = ChannelEntity(
        id = id,
        playlistId = 1,
        name = channelName,
        tvgId = "preview-$id",
        tvgName = channelName,
        logoUrl = null,
        groupTitle = category,
        streamUrl = "https://example.com/$id",
        isFavorite = favorite,
        lastWatchedAt = watchedAt,
        sortOrder = id.toInt()
    ),
    currentProgram = ProgramEntity(
        id = id,
        playlistId = 1,
        channelTvgId = "preview-$id",
        title = title,
        description = "Een originele selectie uit je persoonlijke live aanbod, helder gepresenteerd in StreamGuide.",
        startTime = now - 30 * 60 * 1000,
        endTime = now + 30 * 60 * 1000,
        category = category,
        iconUrl = null
    ),
    nextProgram = null,
    progress = progress
)
