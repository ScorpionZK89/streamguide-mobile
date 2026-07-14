package com.example.streamguidemobile.ui.live

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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.streamguidemobile.ChannelRowState
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.data.StreamResolution
import com.example.streamguidemobile.data.streamResolutionBadge
import com.example.streamguidemobile.ui.cast.CastRouteButton
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import com.example.streamguidemobile.ui.theme.StreamGuideMotion
import com.example.streamguidemobile.ui.theme.StreamGuideRadii
import com.example.streamguidemobile.ui.theme.StreamGuideSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

data class LiveCategoryItem(val key: String, val label: String)

data class ProgramSelection(
    val channel: ChannelEntity,
    val program: ProgramEntity?,
    val nextProgram: ProgramEntity? = null,
    val progress: Float = 0f,
    val qualityBadge: String? = null
)

@Composable
fun CinematicEmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.width(28.dp).height(2.dp)
                .background(CinematicColors.Gold.copy(alpha = 0.72f), RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.height(10.dp))
        Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
        Spacer(Modifier.height(3.dp))
        Text(description, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
    }
}

fun ChannelRowState.asProgramSelection(): ProgramSelection = ProgramSelection(
    channel = channel,
    program = currentProgram,
    nextProgram = nextProgram,
    progress = progress,
    qualityBadge = streamResolutionBadge(streamResolution)
)

@Composable
fun CinematicSearchField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var focused by remember { mutableStateOf(false) }
    val border by animateColorAsState(
        if (focused) CinematicColors.Gold.copy(alpha = 0.75f) else CinematicColors.Border,
        tween(StreamGuideMotion.Quick),
        label = "search-border"
    )
    Row(
        modifier = modifier
            .height(34.dp)
            .background(CinematicColors.PanelRaised.copy(alpha = 0.72f), RoundedCornerShape(StreamGuideRadii.Control))
            .border(1.dp, border, RoundedCornerShape(StreamGuideRadii.Control))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = CinematicColors.TextMuted, modifier = Modifier.size(16.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = CinematicColors.TextPrimary, fontSize = 12.sp, lineHeight = 15.sp),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 9.dp)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) Text(placeholder, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                    inner()
                }
            }
        )
        if (value.isNotBlank()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Zoekopdracht wissen",
                tint = CinematicColors.TextSecondary,
                modifier = Modifier.size(16.dp).clickable { onValueChange("") }
            )
        }
    }
}

@Composable
fun LiveCategoryRow(
    items: List<LiveCategoryItem>,
    selectedKey: String,
    onSelected: (LiveCategoryItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        items(items, key = { it.key }) { item ->
            val selected = item.key == selectedKey
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .widthIn(max = 156.dp)
                    .background(if (selected) CinematicColors.Gold.copy(alpha = 0.12f) else CinematicColors.Panel.copy(alpha = 0.7f))
                    .border(
                        1.dp,
                        if (selected) CinematicColors.Gold.copy(alpha = 0.72f) else CinematicColors.Border,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onSelected(item) }
                    .padding(horizontal = 9.dp, vertical = 4.dp)
            ) {
                Text(
                    item.label,
                    color = if (selected) CinematicColors.GoldBright else CinematicColors.TextSecondary,
                    style = CinematicTypography.Metadata,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CinematicIconAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(RoundedCornerShape(StreamGuideRadii.Control))
            .background(if (selected) CinematicColors.Gold.copy(alpha = 0.12f) else CinematicColors.PanelRaised.copy(alpha = 0.64f))
            .border(1.dp, if (selected) CinematicColors.Gold.copy(alpha = 0.65f) else CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Control))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (selected) CinematicColors.GoldBright else CinematicColors.TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun CompactChannelRow(
    row: ChannelRowState,
    selected: Boolean,
    showLogos: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    var focused by remember { mutableStateOf(false) }
    val emphasized = selected || focused
    val scale by animateFloatAsState(if (pressed) 0.995f else if (focused) 1.004f else 1f, tween(StreamGuideMotion.Quick), label = "channel-scale")
    val border by animateColorAsState(
        if (emphasized) CinematicColors.Gold.copy(alpha = if (focused) 0.68f else 0.28f) else CinematicColors.Border,
        tween(StreamGuideMotion.Standard),
        label = "channel-border"
    )
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused) 4.dp else 0.dp, shape, ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold)
            .background(if (selected) CinematicColors.Gold.copy(alpha = 0.025f) else CinematicColors.Panel.copy(alpha = 0.72f), shape)
            .border(1.dp, border, shape)
            .clip(shape)
            .onFocusChanged { focused = it.isFocused }
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource, indication = null, onClick = onSelect)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            (row.channel.sortOrder + 1).toString(),
            color = CinematicColors.TextMuted,
            style = CinematicTypography.Badge,
            modifier = Modifier.width(21.dp)
        )
        ChannelLogo(channel = row.channel, showLogos = showLogos, size = 32.dp)
        Column(
            modifier = Modifier.weight(1f).padding(horizontal = 7.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    row.channel.name,
                    color = CinematicColors.TextPrimary,
                    style = CinematicTypography.CardTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                MediaBadge("LIVE", CinematicColors.Live)
                qualityBadgeFor(row.streamResolution)?.let { MediaBadge(it, CinematicColors.Gold) }
            }
            Text(
                row.currentProgram?.title ?: "Geen programma-informatie",
                color = if (row.currentProgram == null) CinematicColors.TextMuted else CinematicColors.TextSecondary,
                style = CinematicTypography.Metadata,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    row.currentProgram?.let(::programTimeRange) ?: row.channel.groupTitle,
                    color = CinematicColors.TextMuted,
                    style = CinematicTypography.Badge,
                    maxLines = 1,
                    modifier = Modifier.width(72.dp)
                )
                Box(
                    Modifier.weight(1f).height(2.dp).background(Color.White.copy(alpha = 0.075f), RoundedCornerShape(2.dp))
                ) {
                    if (row.currentProgram != null) {
                        Box(
                            Modifier.fillMaxWidth(row.progress.coerceIn(0f, 1f)).height(2.dp)
                                .background(CinematicColors.Gold.copy(alpha = 0.86f), RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onFavorite),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (row.channel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (row.channel.isFavorite) "Uit favorieten verwijderen" else "Aan favorieten toevoegen",
                tint = if (row.channel.isFavorite) CinematicColors.GoldBright else CinematicColors.TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun ProgramDetailPanel(
    selection: ProgramSelection?,
    nowMillis: Long,
    showLogos: Boolean,
    onWatch: () -> Unit,
    onFavorite: () -> Unit,
    onInfo: () -> Unit,
    modifier: Modifier = Modifier,
    imageHeight: Dp = 154.dp
) {
    val shape = RoundedCornerShape(StreamGuideRadii.Hero)
    if (selection == null) {
        Box(
            modifier = modifier.background(CinematicColors.Panel.copy(alpha = 0.72f), shape).border(1.dp, CinematicColors.Border, shape),
            contentAlignment = Alignment.Center
        ) {
            Text("Selecteer een zender of programma", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        }
        return
    }
    Column(
        modifier = modifier
            .background(CinematicColors.Panel.copy(alpha = 0.9f), shape)
            .border(1.dp, CinematicColors.Border, shape)
            .clip(shape)
    ) {
        Box(Modifier.fillMaxWidth().height(imageHeight)) {
            ProgramArtwork(selection.channel, selection.program, Modifier.fillMaxSize())
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Transparent, CinematicColors.Panel.copy(alpha = 0.92f)))
                )
            )
            Row(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChannelLogo(selection.channel, showLogos, 42.dp)
                Column(Modifier.weight(1f).padding(start = 9.dp)) {
                    Text(selection.channel.name, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(selection.channel.groupTitle, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = 1)
                }
                selection.qualityBadge?.let { MediaBadge(it, CinematicColors.Gold) }
            }
        }
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                selection.program?.title ?: "Geen programma-informatie",
                color = CinematicColors.TextPrimary,
                style = CinematicTypography.SectionTitle,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            selection.program?.let { program ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(programTimeRange(program), color = CinematicColors.GoldBright, style = CinematicTypography.Metadata)
                    Text(remainingLabel(program, nowMillis), color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                }
                Box(Modifier.fillMaxWidth().height(3.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(2.dp))) {
                    Box(
                        Modifier.fillMaxWidth(programProgress(program, nowMillis)).height(3.dp)
                            .background(CinematicColors.Gold, RoundedCornerShape(2.dp))
                    )
                }
            }
            Text(
                selection.program?.description?.takeIf { it.isNotBlank() } ?: "Voor dit programma is geen beschrijving beschikbaar.",
                color = CinematicColors.TextSecondary,
                style = CinematicTypography.Body,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            selection.nextProgram?.let {
                Text("Straks: ${it.title}", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                CinematicTextAction("Nu kijken", Icons.Default.PlayArrow, primary = true, onClick = onWatch)
                CastRouteButton(Modifier.size(34.dp))
                CinematicIconAction(
                    icon = if (selection.channel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    description = if (selection.channel.isFavorite) "Uit favorieten verwijderen" else "Aan favorieten toevoegen",
                    selected = selection.channel.isFavorite,
                    onClick = onFavorite
                )
                CinematicIconAction(Icons.Default.Info, "Programma-informatie", onInfo)
            }
        }
    }
}

@Composable
fun ProgramInfoDialog(selection: ProgramSelection, nowMillis: Long, onWatch: () -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(StreamGuideRadii.Hero)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinematicColors.PanelRaised, shape)
                .border(1.dp, CinematicColors.BorderStrong, shape)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(selection.channel.name, color = CinematicColors.GoldBright, style = CinematicTypography.Metadata)
            Text(selection.program?.title ?: "Geen programma-informatie", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
            selection.program?.let {
                Text("${programTimeRange(it)}  |  ${remainingLabel(it, nowMillis)}", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                it.category?.takeIf(String::isNotBlank)?.let { category -> Text(category, color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata) }
            }
            Text(
                selection.program?.description?.takeIf(String::isNotBlank) ?: "Voor dit programma is geen beschrijving beschikbaar.",
                color = CinematicColors.TextSecondary,
                style = CinematicTypography.Body
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CinematicTextAction("Nu kijken", Icons.Default.PlayArrow, primary = true, onClick = onWatch)
                CinematicTextAction("Sluiten", Icons.Default.Close, onClick = onDismiss)
            }
        }
    }
}

@Composable
fun CategoryPickerDialog(
    title: String,
    items: List<LiveCategoryItem>,
    selectedKey: String,
    onSelected: (LiveCategoryItem) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        val shape = RoundedCornerShape(StreamGuideRadii.Hero)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CinematicColors.PanelRaised, shape)
                .border(1.dp, CinematicColors.BorderStrong, shape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle, modifier = Modifier.weight(1f))
                CinematicIconAction(Icons.Default.Close, "Sluiten", onDismiss)
            }
            LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                items(items, key = { it.key }) { item ->
                    val selected = item.key == selectedKey
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (selected) CinematicColors.Gold.copy(alpha = 0.1f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) CinematicColors.Gold.copy(alpha = 0.6f) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { onSelected(item) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.label, color = if (selected) CinematicColors.GoldBright else CinematicColors.TextSecondary, style = CinematicTypography.Metadata)
                    }
                }
            }
        }
    }
}

@Composable
fun ProgramArtwork(channel: ChannelEntity, program: ProgramEntity?, modifier: Modifier = Modifier) {
    val programArtwork = program?.iconUrl?.takeIf { it.isNotBlank() }
    val channelLogo = channel.logoUrl?.takeIf { it.isNotBlank() }
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(CinematicColors.PanelPressed, CinematicColors.Panel)
            )
        ),
        contentAlignment = Alignment.Center
    ) {
        if (channelLogo != null) {
            AsyncImage(
                model = channelLogo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(18.dp)
            )
        } else {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = CinematicColors.Gold.copy(alpha = 0.72f),
                modifier = Modifier.size(32.dp)
            )
        }
        if (programArtwork != null) {
            AsyncImage(
                model = programArtwork,
                contentDescription = program.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun ChannelLogo(channel: ChannelEntity, showLogos: Boolean, size: Dp) {
    Box(
        modifier = Modifier.size(size).background(CinematicColors.PanelRaised.copy(alpha = 0.9f), RoundedCornerShape(7.dp)).border(1.dp, CinematicColors.Border, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (showLogos && !channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(5.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(size * 0.48f))
        }
    }
}

@Composable
private fun MediaBadge(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = if (color == CinematicColors.Gold) 0.92f else 1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp)
    ) {
        Text(label, color = if (color == CinematicColors.Gold) CinematicColors.OnGold else Color.White, style = CinematicTypography.Badge)
    }
}

@Composable
private fun CinematicTextAction(label: String, icon: ImageVector, primary: Boolean = false, onClick: () -> Unit) {
    val background = if (primary) CinematicColors.Gold else CinematicColors.PanelRaised.copy(alpha = 0.8f)
    val content = if (primary) CinematicColors.OnGold else CinematicColors.TextPrimary
    Row(
        modifier = Modifier
            .height(33.dp)
            .clip(RoundedCornerShape(StreamGuideRadii.Control))
            .background(background)
            .border(1.dp, if (primary) Color.Transparent else CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Control))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(16.dp))
        Text(label, color = content, style = CinematicTypography.Metadata, fontWeight = FontWeight.SemiBold)
    }
}

fun qualityBadgeFor(resolution: StreamResolution?): String? = streamResolutionBadge(resolution)

fun programTimeRange(program: ProgramEntity): String = "${formatTime(program.startTime)} - ${formatTime(program.endTime)}"

fun programProgress(program: ProgramEntity, nowMillis: Long): Float {
    if (program.endTime <= program.startTime) return 0f
    return ((nowMillis - program.startTime).toFloat() / (program.endTime - program.startTime).toFloat()).coerceIn(0f, 1f)
}

fun remainingLabel(program: ProgramEntity, nowMillis: Long): String {
    if (nowMillis < program.startTime) {
        val minutes = ceil((program.startTime - nowMillis) / 60_000.0).toInt().coerceAtLeast(1)
        return "Over $minutes min"
    }
    if (nowMillis >= program.endTime) return "Afgelopen"
    val minutes = ceil((program.endTime - nowMillis) / 60_000.0).toInt().coerceAtLeast(1)
    return "$minutes min resterend"
}

private fun formatTime(timestamp: Long): String = timeFormatter.format(Instant.ofEpochMilli(timestamp))

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
