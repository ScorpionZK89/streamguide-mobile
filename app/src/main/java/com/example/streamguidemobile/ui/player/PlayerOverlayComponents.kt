package com.example.streamguidemobile.ui.player

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.ui.live.ChannelLogo
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import com.example.streamguidemobile.ui.theme.StreamGuideMotion
import coil.compose.AsyncImage
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class PlayerAction(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean = false,
    val onClick: () -> Unit
)

internal data class PlayerSelectionItem(
    val key: String,
    val title: String,
    val supporting: String? = null,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
internal fun PlayerOverlayScrims(compact: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.TopCenter).fillMaxWidth().height(if (compact) 138.dp else 164.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.88f),
                        0.42f to Color.Black.copy(alpha = 0.58f),
                        1f to Color.Transparent
                    )
                )
        )
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(if (compact) 270.dp else 300.dp)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.36f to Color.Black.copy(alpha = 0.52f),
                        1f to Color.Black.copy(alpha = 0.96f)
                    )
                )
        )
    }
}

@Composable
internal fun PremiumPlayerTopBar(
    channel: ChannelEntity,
    program: ProgramEntity?,
    quality: String?,
    showLogos: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onFavorite: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Terug", onBack)
        val channelNumber = channel.sortOrder + 1
        if (channelNumber in 1..9999) {
            Text(
                channelNumber.toString(),
                color = CinematicColors.TextSecondary,
                style = CinematicTypography.Metadata.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.9f), Offset(0f, 1f), 4f)
                ),
                modifier = Modifier.padding(start = 5.dp, end = 8.dp)
            )
        } else {
            Spacer(Modifier.width(4.dp))
        }
        ChannelLogo(channel, showLogos, 36.dp)
        Column(Modifier.weight(1f).padding(horizontal = 9.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                channel.name,
                color = Color.White,
                style = CinematicTypography.SectionTitle.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.94f), Offset(0f, 1f), 5f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                program?.title ?: channel.groupTitle,
                color = Color.White.copy(alpha = 0.8f),
                style = CinematicTypography.Metadata.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.94f), Offset(0f, 1f), 4f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        quality?.let { PlayerBadge(it, CinematicColors.Gold) }
        Spacer(Modifier.width(5.dp))
        PlayerIconButton(
            icon = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            description = if (isFavorite) "Uit favorieten" else "Aan favorieten",
            onClick = onFavorite,
            selected = isFavorite
        )
        PlayerIconButton(Icons.Default.MoreVert, "Meer opties", onMore)
    }
}

@Composable
internal fun PremiumMediaTopBar(
    title: String,
    subtitle: String,
    quality: String?,
    onBack: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PlayerIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Terug", onBack)
        Column(Modifier.weight(1f).padding(horizontal = 10.dp), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                title,
                color = Color.White,
                style = CinematicTypography.SectionTitle.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.94f), Offset(0f, 1f), 5f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = Color.White.copy(alpha = 0.8f),
                style = CinematicTypography.Metadata.copy(
                    shadow = Shadow(Color.Black.copy(alpha = 0.94f), Offset(0f, 1f), 4f)
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        quality?.let { PlayerBadge(it, CinematicColors.Gold) }
        Spacer(Modifier.width(5.dp))
        PlayerIconButton(Icons.Default.MoreVert, "Meer opties", onMore)
    }
}

@Composable
internal fun PremiumCenterControls(
    isPlaying: Boolean,
    seekable: Boolean,
    onSeekBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekForward: () -> Unit,
    seekBackIcon: ImageVector,
    playIcon: ImageVector,
    pauseIcon: ImageVector,
    seekForwardIcon: ImageVector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (seekable) PlayerRoundControl(seekBackIcon, "10 seconden terug", onSeekBack, 44.dp)
        PlayerRoundControl(
            icon = if (isPlaying) pauseIcon else playIcon,
            description = if (isPlaying) "Pauzeren" else "Afspelen",
            onClick = onPlayPause,
            size = 52.dp,
            primary = true
        )
        if (seekable) PlayerRoundControl(seekForwardIcon, "10 seconden vooruit", onSeekForward, 44.dp)
    }
}

@Composable
internal fun SeekableTimeline(
    position: Long,
    duration: Long,
    bufferedPosition: Long,
    onSeek: (Long) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = duration.coerceAtLeast(1L)
    var scrubValue by remember(position, safeDuration) { mutableStateOf(position.coerceIn(0L, safeDuration).toFloat()) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(1.dp)) {
        val playedFraction = (scrubValue / safeDuration).coerceIn(0f, 1f)
        val bufferedFraction = (bufferedPosition.toFloat() / safeDuration).coerceIn(0f, 1f)
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .pointerInput(safeDuration) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        onInteraction()
                        fun updateScrub(x: Float) {
                            scrubValue = (x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f) * safeDuration
                        }
                        updateScrub(down.position.x)
                        var change = down
                        while (change.pressed) {
                            val event = awaitPointerEvent()
                            change = event.changes.firstOrNull { it.id == down.id } ?: break
                            updateScrub(change.position.x)
                            change.consume()
                        }
                        onSeek(scrubValue.toLong())
                    }
                }
        ) {
            val centerY = size.height / 2f
            val stroke = 4.dp.toPx()
            drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, centerY), Offset(size.width, centerY), stroke, StrokeCap.Round)
            drawLine(Color.White.copy(alpha = 0.38f), Offset(0f, centerY), Offset(size.width * bufferedFraction, centerY), stroke, StrokeCap.Round)
            drawLine(CinematicColors.Gold, Offset(0f, centerY), Offset(size.width * playedFraction, centerY), stroke, StrokeCap.Round)
            drawCircle(CinematicColors.GoldBright, radius = 6.dp.toPx(), center = Offset(size.width * playedFraction, centerY))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(formatPlaybackTime(scrubValue.toLong()), color = Color.White.copy(alpha = 0.76f), style = CinematicTypography.Badge)
            Spacer(Modifier.weight(1f))
            Text(formatPlaybackTime(safeDuration), color = Color.White.copy(alpha = 0.76f), style = CinematicTypography.Badge)
        }
    }
}

@Composable
internal fun LiveProgramTimeline(
    program: ProgramEntity?,
    nowMillis: Long,
    modifier: Modifier = Modifier
) {
    val progress = program?.let {
        ((nowMillis - it.startTime).toFloat() / (it.endTime - it.startTime).coerceAtLeast(1L)).coerceIn(0f, 1f)
    } ?: 0f
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))) {
            Box(
                Modifier.fillMaxWidth(progress).fillMaxHeight()
                    .shadow(5.dp, RoundedCornerShape(2.dp), ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold)
                    .background(CinematicColors.Gold, RoundedCornerShape(2.dp))
            )
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(program?.let { formatEpgTime(it.startTime) } ?: "--:--", color = Color.White.copy(alpha = 0.72f), style = CinematicTypography.Badge)
            Spacer(Modifier.weight(1f))
            PlayerBadge("LIVE", CinematicColors.Live)
            Spacer(Modifier.weight(1f))
            Text(program?.let { formatEpgTime(it.endTime) } ?: "--:--", color = Color.White.copy(alpha = 0.72f), style = CinematicTypography.Badge)
        }
    }
}

@Composable
internal fun PlayerActionBar(
    actions: List<PlayerAction>,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(actions, key = { it.label }) { action ->
            PlayerActionButton(action)
        }
    }
}

@Composable
private fun PlayerActionButton(action: PlayerAction) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val border by animateColorAsState(
        if (focused || action.selected) CinematicColors.Gold.copy(alpha = 0.78f) else CinematicColors.BorderStrong,
        tween(StreamGuideMotion.Quick),
        label = "player-action-border"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.96f
            focused -> 1.05f
            else -> 1f
        },
        animationSpec = tween(StreamGuideMotion.Quick),
        label = "player-action-scale"
    )
    Column(
        modifier = Modifier
            .width(66.dp)
            .height(50.dp)
            .scale(scale)
            .shadow(if (focused) 8.dp else 0.dp, RoundedCornerShape(9.dp), ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold)
            .clip(RoundedCornerShape(9.dp))
            .background(CinematicColors.PanelRaised.copy(alpha = 0.76f))
            .border(1.dp, border, RoundedCornerShape(9.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(interactionSource = interactionSource, indication = null, onClick = action.onClick)
            .padding(vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(action.icon, contentDescription = action.label, tint = if (focused || action.selected) CinematicColors.GoldBright else Color.White, modifier = Modifier.size(18.dp))
        Text(action.label, color = Color.White.copy(alpha = 0.82f), style = CinematicTypography.Badge, maxLines = 1)
    }
}

@Composable
internal fun LiveScheduleStrip(
    programs: List<ProgramEntity>,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (programs.isEmpty()) return
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CinematicColors.Panel.copy(alpha = 0.74f))
            .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(8.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Straks", color = CinematicColors.GoldBright, style = CinematicTypography.Badge)
            Spacer(Modifier.width(8.dp))
            Text(programs.first().title, color = Color.White, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(if (expanded) "Inklappen" else "Meer", color = Color.White.copy(alpha = 0.62f), style = CinematicTypography.Badge)
        }
        if (expanded) {
            programs.take(3).forEachIndexed { index, program ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(if (index == 0) "Volgend" else "Daarna", color = Color.White.copy(alpha = 0.52f), style = CinematicTypography.Badge, modifier = Modifier.width(46.dp))
                    Text(program.title, color = Color.White.copy(alpha = 0.86f), style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formatEpgTime(program.startTime), color = Color.White.copy(alpha = 0.52f), style = CinematicTypography.Badge)
                }
            }
        }
    }
}

@Composable
internal fun PlayerSelectionSheet(
    title: String,
    items: List<PlayerSelectionItem>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f))) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            PlayerSelectionPanel(
                title = title,
                items = items,
                modifier = Modifier.align(Alignment.CenterEnd).windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}

@Composable
internal fun PlayerSelectionPanel(
    title: String,
    items: List<PlayerSelectionItem>,
    modifier: Modifier = Modifier
) {
    val blockClicks = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .widthIn(min = 310.dp, max = 390.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CinematicColors.PanelRaised.copy(alpha = 0.98f), CinematicColors.Panel.copy(alpha = 0.98f))
                )
            )
            .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            .clickable(interactionSource = blockClicks, indication = null) {}
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Box(Modifier.width(30.dp).height(2.dp).background(CinematicColors.Gold, RoundedCornerShape(2.dp)))
        Text(title, color = Color.White, style = CinematicTypography.SectionTitle, modifier = Modifier.padding(top = 9.dp, bottom = 12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(items, key = { it.key }) { item ->
                var focused by remember { mutableStateOf(false) }
                val border by animateColorAsState(
                    if (focused || item.selected) CinematicColors.Gold.copy(alpha = 0.72f) else CinematicColors.Border,
                    tween(StreamGuideMotion.Quick),
                    label = "player-selection-border"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(if (item.selected) CinematicColors.Gold.copy(alpha = 0.11f) else CinematicColors.PanelPressed.copy(alpha = 0.38f))
                        .border(1.dp, border, RoundedCornerShape(9.dp))
                        .onFocusChanged { focused = it.isFocused }
                        .focusable()
                        .clickable(onClick = item.onClick)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(10.dp)
                            .background(if (item.selected) CinematicColors.GoldBright else CinematicColors.BorderStrong, CircleShape)
                    )
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(item.title, color = Color.White, style = CinematicTypography.Metadata, fontWeight = if (item.selected) FontWeight.SemiBold else FontWeight.Normal)
                        item.supporting?.let { Text(it, color = Color.White.copy(alpha = 0.58f), style = CinematicTypography.Badge, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PlayerTechnicalInfoSheet(
    title: String,
    rows: List<PlayerTechnicalInfoRow>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f))) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            PlayerTechnicalInfoPanel(
                title = title,
                rows = rows,
                modifier = Modifier.align(Alignment.CenterEnd).windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}

@Composable
internal fun PlayerTechnicalInfoPanel(
    title: String,
    rows: List<PlayerTechnicalInfoRow>,
    modifier: Modifier = Modifier
) {
    val blockClicks = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .widthIn(min = 330.dp, max = 420.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CinematicColors.PanelRaised.copy(alpha = 0.98f), CinematicColors.Panel.copy(alpha = 0.98f))
                )
            )
            .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            .clickable(interactionSource = blockClicks, indication = null) {}
            .padding(horizontal = 18.dp, vertical = 16.dp)
    ) {
        Box(Modifier.width(30.dp).height(2.dp).background(CinematicColors.Gold, RoundedCornerShape(2.dp)))
        Text(title, color = Color.White, style = CinematicTypography.SectionTitle, modifier = Modifier.padding(top = 9.dp))
        Text(
            "Actuele gegevens van de geselecteerde filmstream",
            color = Color.White.copy(alpha = 0.56f),
            style = CinematicTypography.Badge,
            modifier = Modifier.padding(top = 3.dp, bottom = 12.dp)
        )
        LazyColumn(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            items(rows, key = { it.label }) { row ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                        .background(CinematicColors.PanelPressed.copy(alpha = 0.38f))
                        .border(1.dp, CinematicColors.Border, RoundedCornerShape(8.dp))
                        .padding(horizontal = 11.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(row.label, color = Color.White.copy(alpha = 0.56f), style = CinematicTypography.Badge, modifier = Modifier.width(106.dp))
                    Text(row.value, color = Color.White, style = CinematicTypography.Metadata, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun PlayerProgramInfoSheet(
    channel: ChannelEntity,
    current: ProgramEntity?,
    upcoming: List<ProgramEntity>,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.48f))) {
            Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
            PlayerProgramInfoPanel(
                channel = channel,
                current = current,
                upcoming = upcoming,
                modifier = Modifier.align(Alignment.CenterEnd).windowInsetsPadding(WindowInsets.safeDrawing)
            )
        }
    }
}

@Composable
internal fun PlayerProgramInfoPanel(
    channel: ChannelEntity,
    current: ProgramEntity?,
    upcoming: List<ProgramEntity>,
    modifier: Modifier = Modifier
) {
    val blockClicks = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .widthIn(min = 330.dp, max = 420.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            .background(
                Brush.verticalGradient(
                    listOf(CinematicColors.PanelRaised.copy(alpha = 0.98f), CinematicColors.Panel.copy(alpha = 0.98f))
                )
            )
            .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
            .clickable(interactionSource = blockClicks, indication = null) {}
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.width(30.dp).height(2.dp).background(CinematicColors.Gold, RoundedCornerShape(2.dp)))
        Text(channel.name, color = CinematicColors.GoldBright, style = CinematicTypography.Badge)
        Text(current?.title ?: "Geen programma-informatie", color = Color.White, style = CinematicTypography.SectionTitle)
        current?.let {
            Text("${formatEpgTime(it.startTime)} - ${formatEpgTime(it.endTime)}", color = Color.White.copy(alpha = 0.64f), style = CinematicTypography.Metadata)
            it.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(description, color = Color.White.copy(alpha = 0.76f), style = CinematicTypography.Body, maxLines = 7, overflow = TextOverflow.Ellipsis)
            }
        }
        if (upcoming.isNotEmpty()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(CinematicColors.BorderStrong))
            Text("Hierna", color = Color.White.copy(alpha = 0.56f), style = CinematicTypography.Badge)
            upcoming.take(3).forEach { program ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatEpgTime(program.startTime), color = CinematicColors.GoldBright, style = CinematicTypography.Badge, modifier = Modifier.width(44.dp))
                    Text(program.title, color = Color.White.copy(alpha = 0.86f), style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun MediaUpNextCard(
    title: String,
    metadata: String,
    countdownSeconds: Int,
    artwork: Painter?,
    artworkUrl: String? = null,
    onPlay: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .widthIn(max = 680.dp)
            .height(92.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(CinematicColors.Panel.copy(alpha = 0.92f))
            .border(1.dp, CinematicColors.Gold.copy(alpha = 0.38f), RoundedCornerShape(11.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!artworkUrl.isNullOrBlank()) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxHeight().aspectRatio(16f / 10f),
                contentScale = ContentScale.Crop
            )
        } else if (artwork != null) {
            Image(
                painter = artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxHeight().aspectRatio(16f / 10f),
                contentScale = ContentScale.Crop
            )
        }
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 13.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text("VOLGENDE", color = CinematicColors.GoldBright, style = CinematicTypography.Badge)
            Text(title, color = Color.White, style = CinematicTypography.SectionTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(metadata, color = Color.White.copy(alpha = 0.62f), style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(
            modifier = Modifier.padding(end = 11.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Start over ${countdownSeconds.coerceAtLeast(0)} s", color = Color.White.copy(alpha = 0.62f), style = CinematicTypography.Badge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PlayerTextButton("Annuleren", onClick = onCancel)
                PlayerTextButton("Nu afspelen", primary = true, onClick = onPlay)
            }
        }
    }
}

@Composable
internal fun PlayerErrorPanel(
    failure: PlayerFailure,
    canOpenExternal: Boolean,
    onRetry: () -> Unit,
    onExternal: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(360.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CinematicColors.PanelRaised.copy(alpha = 0.94f))
            .border(1.dp, CinematicColors.BorderStrong, RoundedCornerShape(10.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Box(Modifier.width(30.dp).height(2.dp).background(CinematicColors.Live, RoundedCornerShape(2.dp)))
        Text(failure.title, color = Color.White, style = CinematicTypography.SectionTitle)
        Text(failure.message, color = Color.White.copy(alpha = 0.68f), style = CinematicTypography.Body)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            PlayerTextButton("Opnieuw proberen", primary = true, onClick = onRetry)
            if (canOpenExternal) PlayerTextButton("Externe speler", onClick = onExternal)
            PlayerTextButton("Zenderlijst", onClick = onBack)
        }
    }
}

@Composable
internal fun GestureIndicator(label: String, progress: Float, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(168.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(CinematicColors.Panel.copy(alpha = 0.9f))
            .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = Color.White, style = CinematicTypography.Metadata)
            Spacer(Modifier.weight(1f))
            Text("${(progress.coerceIn(0f, 1f) * 100).toInt()}%", color = CinematicColors.GoldBright, style = CinematicTypography.Badge)
        }
        Box(Modifier.fillMaxWidth().height(4.dp).background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(2.dp))) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).fillMaxHeight().background(CinematicColors.Gold, RoundedCornerShape(2.dp)))
        }
    }
}

@Composable
internal fun PlayerBadge(label: String, color: Color) {
    Box(
        Modifier
            .shadow(4.dp, RoundedCornerShape(5.dp), ambientColor = color, spotColor = color)
            .background(color.copy(alpha = 0.94f), RoundedCornerShape(5.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(label, color = if (color == CinematicColors.Gold) Color(0xFF231500) else Color.White, style = CinematicTypography.Badge)
    }
}

@Composable
private fun PlayerIconButton(icon: ImageVector, description: String, onClick: () -> Unit, selected: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else if (focused) 1.06f else 1f,
        tween(StreamGuideMotion.Quick),
        label = "player-icon-scale"
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) CinematicColors.Gold.copy(alpha = 0.14f) else Color.Black.copy(alpha = 0.36f))
                .border(1.dp, if (focused || selected) CinematicColors.Gold.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = description, tint = if (focused || selected) CinematicColors.GoldBright else Color.White, modifier = Modifier.size(19.dp))
        }
    }
}

@Composable
private fun PlayerRoundControl(icon: ImageVector, description: String, onClick: () -> Unit, size: androidx.compose.ui.unit.Dp, primary: Boolean = false) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else if (focused) 1.07f else 1f,
        tween(StreamGuideMotion.Quick),
        label = "player-control-scale"
    )
    Box(
        modifier = Modifier
            .size(size)
            .scale(scale)
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(if (primary) 44.dp else 36.dp)
                .shadow(if (primary || focused) 10.dp else 0.dp, CircleShape, ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold)
                .background(Color.Black.copy(alpha = if (primary) 0.68f else 0.56f), CircleShape)
                .border(
                    width = if (primary) 1.5.dp else 1.dp,
                    color = if (primary || focused) CinematicColors.GoldBright.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.18f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (primary || focused) CinematicColors.GoldBright else Color.White,
                modifier = Modifier.size(if (primary) 23.dp else 19.dp)
            )
        }
    }
}

@Composable
private fun PlayerTextButton(label: String, primary: Boolean = false, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(if (primary) CinematicColors.Gold else CinematicColors.PanelPressed)
            .border(1.dp, if (primary || focused) CinematicColors.GoldBright.copy(alpha = 0.72f) else CinematicColors.BorderStrong, RoundedCornerShape(7.dp))
            .onFocusChanged { focused = it.isFocused }
            .focusable()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(label, color = if (primary) Color(0xFF241500) else Color.White, style = CinematicTypography.Badge)
    }
}

private fun formatEpgTime(timestamp: Long): String = playerTimeFormatter.format(Instant.ofEpochMilli(timestamp))

private val playerTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
