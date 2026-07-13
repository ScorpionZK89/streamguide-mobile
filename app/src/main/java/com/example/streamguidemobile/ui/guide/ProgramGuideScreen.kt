package com.example.streamguidemobile.ui.guide

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.streamguidemobile.GuideChannelState
import com.example.streamguidemobile.StreamGuideState
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.ui.live.CategoryPickerDialog
import com.example.streamguidemobile.ui.live.ChannelLogo
import com.example.streamguidemobile.ui.live.CinematicEmptyState
import com.example.streamguidemobile.ui.live.CinematicIconAction
import com.example.streamguidemobile.ui.live.CinematicSearchField
import com.example.streamguidemobile.ui.live.LiveCategoryItem
import com.example.streamguidemobile.ui.live.LiveCategoryRow
import com.example.streamguidemobile.ui.live.ProgramDetailPanel
import com.example.streamguidemobile.ui.live.ProgramInfoDialog
import com.example.streamguidemobile.ui.live.ProgramSelection
import com.example.streamguidemobile.ui.live.programProgress
import com.example.streamguidemobile.ui.live.programTimeRange
import com.example.streamguidemobile.data.streamResolutionBadge
import com.example.streamguidemobile.ui.preview.liveGuidePreviewState
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import com.example.streamguidemobile.ui.theme.StreamGuideMotion
import com.example.streamguidemobile.ui.theme.StreamGuideRadii
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgramGuideScreen(
    state: StreamGuideState,
    onQueryChange: (String) -> Unit,
    onDaySelected: (Long) -> Unit,
    onToggleFavorite: (ChannelEntity) -> Unit,
    onWatch: (ChannelEntity) -> Unit,
    onPrepareCast: (ChannelEntity) -> Unit,
    onOpenLive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val days = remember(today) { (-1..6).map { today.plusDays(it.toLong()) } }
    val categories = remember(state.groups) {
        buildList {
            add(LiveCategoryItem(ALL_KEY, "Alle zenders"))
            add(LiveCategoryItem(FAVORITES_KEY, "Favorieten"))
            state.groups.distinctBy { it.lowercase() }.forEach { add(LiveCategoryItem("group:$it", it)) }
        }
    }
    var selectedCategoryKey by rememberSaveable { mutableStateOf(ALL_KEY) }
    var selectedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedProgramId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }

    val visibleRows = remember(state.guideRows, selectedCategoryKey) {
        when {
            selectedCategoryKey == FAVORITES_KEY -> state.guideRows.filter { it.channel.isFavorite }
            selectedCategoryKey.startsWith("group:") -> {
                val group = selectedCategoryKey.removePrefix("group:")
                state.guideRows.filter { it.channel.groupTitle.equals(group, ignoreCase = true) }
            }
            else -> state.guideRows
        }
    }

    LaunchedEffect(visibleRows, state.guideDayStart) {
        val currentSelectionExists = visibleRows.any { row ->
            row.channel.id == selectedChannelId && (selectedProgramId == null || row.programs.any { it.id == selectedProgramId })
        }
        if (!currentSelectionExists) {
            val firstRow = visibleRows.firstOrNull()
            val firstProgram = firstRow?.programs?.firstOrNull { it.startTime <= state.nowMillis && it.endTime > state.nowMillis }
                ?: firstRow?.programs?.firstOrNull()
            selectedChannelId = firstRow?.channel?.id
            selectedProgramId = firstProgram?.id
        }
    }

    val selectedGuideRow = visibleRows.firstOrNull { it.channel.id == selectedChannelId }
        ?: visibleRows.firstOrNull()
    val selectedProgram = selectedGuideRow?.programs?.firstOrNull { it.id == selectedProgramId }
        ?: selectedGuideRow?.programs?.firstOrNull {
            it.startTime <= state.nowMillis && it.endTime > state.nowMillis
        }
        ?: selectedGuideRow?.programs?.firstOrNull()
    val selectedNext = selectedGuideRow?.programs?.firstOrNull { selectedProgram != null && it.startTime >= selectedProgram.endTime }
    val selected = selectedGuideRow?.let { row ->
        ProgramSelection(
            channel = row.channel,
            program = selectedProgram,
            nextProgram = selectedNext,
            progress = selectedProgram?.let { programProgress(it, state.nowMillis) } ?: 0f,
            qualityBadge = streamResolutionBadge(row.streamResolution)
        )
    }
    LaunchedEffect(selected?.channel?.id) { selected?.channel?.let(onPrepareCast) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(0f to CinematicColors.CanvasTop, 0.34f to CinematicColors.Canvas, 1f to Color(0xFF020407))
        )
    ) {
        val wide = maxWidth >= 720.dp || maxWidth > maxHeight
        val compactHeight = maxHeight < 500.dp
        val detailWidth = if (maxWidth >= 1000.dp) 330.dp else 270.dp
        Column(Modifier.fillMaxSize()) {
            GuideToolbar(
                query = state.guideQuery,
                compact = compactHeight,
                onQueryChange = onQueryChange,
                onFilter = { showCategoryPicker = true },
                onLive = onOpenLive
            )
            DaySelector(
                days = days,
                today = today,
                selectedDayStart = state.guideDayStart,
                onSelected = onDaySelected,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp)
            )
            if (!compactHeight) {
                LiveCategoryRow(
                    items = categories,
                    selectedKey = selectedCategoryKey,
                    onSelected = { selectedCategoryKey = it.key },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
                )
            }
            GuideStatusStrip(state)
            if (wide) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GuideGrid(
                        rows = visibleRows,
                        loading = state.isLoading,
                        dayStart = state.guideDayStart,
                        nowMillis = state.nowMillis,
                        selectedProgramId = selectedProgramId,
                        selectedChannelId = selectedChannelId,
                        showLogos = state.settings.showLogos,
                        onChannelSelected = { channel, program ->
                            selectedChannelId = channel.id
                            selectedProgramId = program?.id
                        },
                        onProgramFocused = { channel, program ->
                            selectedChannelId = channel.id
                            selectedProgramId = program.id
                        },
                        onProgramClicked = { channel, program ->
                            selectedChannelId = channel.id
                            selectedProgramId = program.id
                        },
                        modifier = Modifier.weight(1f)
                    )
                    ProgramDetailPanel(
                        selection = selected,
                        nowMillis = state.nowMillis,
                        showLogos = state.settings.showLogos,
                        onWatch = { selected?.channel?.let(onWatch) },
                        onFavorite = { selected?.channel?.let(onToggleFavorite) },
                        onInfo = { if (selected != null) showInfo = true },
                        imageHeight = if (compactHeight) 90.dp else 146.dp,
                        modifier = Modifier.width(detailWidth).heightIn(min = 210.dp)
                    )
                }
            } else {
                GuideGrid(
                    rows = visibleRows,
                    loading = state.isLoading,
                    dayStart = state.guideDayStart,
                    nowMillis = state.nowMillis,
                    selectedProgramId = selectedProgramId,
                    selectedChannelId = selectedChannelId,
                    showLogos = state.settings.showLogos,
                    onChannelSelected = { channel, program ->
                        selectedChannelId = channel.id
                        selectedProgramId = program?.id
                        showDetails = true
                    },
                    onProgramFocused = { channel, program ->
                        selectedChannelId = channel.id
                        selectedProgramId = program.id
                    },
                    onProgramClicked = { channel, program ->
                        selectedChannelId = channel.id
                        selectedProgramId = program.id
                        showDetails = true
                    },
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            title = "Gidscategorie",
            items = categories,
            selectedKey = selectedCategoryKey,
            onSelected = {
                selectedCategoryKey = it.key
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showDetails && selected != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetails = false },
            containerColor = CinematicColors.PanelRaised,
            contentColor = CinematicColors.TextPrimary,
            dragHandle = {
                Box(Modifier.padding(vertical = 9.dp).width(38.dp).height(3.dp).background(CinematicColors.BorderStrong, RoundedCornerShape(2.dp)))
            }
        ) {
            ProgramDetailPanel(
                selection = selected,
                nowMillis = state.nowMillis,
                showLogos = state.settings.showLogos,
                onWatch = {
                    showDetails = false
                    onWatch(selected.channel)
                },
                onFavorite = { onToggleFavorite(selected.channel) },
                onInfo = { showInfo = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showInfo && selected != null) {
        ProgramInfoDialog(
            selection = selected,
            nowMillis = state.nowMillis,
            onWatch = {
                showInfo = false
                showDetails = false
                onWatch(selected.channel)
            },
            onDismiss = { showInfo = false }
        )
    }
}

@Composable
private fun GuideToolbar(
    query: String,
    compact: Boolean,
    onQueryChange: (String) -> Unit,
    onFilter: () -> Unit,
    onLive: () -> Unit
) {
    if (compact) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("Programmagids", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
            CinematicSearchField(query, "Zoek in de gids", onQueryChange, Modifier.weight(1f))
            CinematicIconAction(Icons.Default.FilterList, "Categorie filteren", onFilter)
            CinematicIconAction(Icons.AutoMirrored.Filled.List, "Zenderlijst openen", onLive)
        }
    } else {
        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Programmagids", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                    Text("Programma's per dag", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CinematicIconAction(Icons.Default.FilterList, "Categorie filteren", onFilter)
                    CinematicIconAction(Icons.AutoMirrored.Filled.List, "Zenderlijst openen", onLive)
                }
            }
            CinematicSearchField(query, "Zoek programma, zender of categorie", onQueryChange, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DaySelector(
    days: List<LocalDate>,
    today: LocalDate,
    selectedDayStart: Long,
    onSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        items(days, key = { it.toEpochDay() }) { day ->
            val dayStart = dayStartMillis(day)
            val selected = dayStart == selectedDayStart
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) CinematicColors.Gold.copy(alpha = 0.12f) else CinematicColors.Panel.copy(alpha = 0.72f))
                    .border(1.dp, if (selected) CinematicColors.Gold.copy(alpha = 0.7f) else CinematicColors.Border, RoundedCornerShape(8.dp))
                    .clickable { onSelected(dayStart) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    dateLabel(day, today),
                    color = if (selected) CinematicColors.GoldBright else CinematicColors.TextSecondary,
                    style = CinematicTypography.Metadata,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun GuideGrid(
    rows: List<GuideChannelState>,
    loading: Boolean,
    dayStart: Long,
    nowMillis: Long,
    selectedProgramId: Long?,
    selectedChannelId: Long?,
    showLogos: Boolean,
    onChannelSelected: (ChannelEntity, ProgramEntity?) -> Unit,
    onProgramFocused: (ChannelEntity, ProgramEntity) -> Unit,
    onProgramClicked: (ChannelEntity, ProgramEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) {
        CinematicEmptyState(
            title = if (loading) "Programmagids wordt geladen" else "Geen programma's gevonden",
            description = if (loading) "De gidsgegevens worden voorbereid" else "Pas je dag, zoekopdracht of filter aan",
            modifier = modifier
        )
        return
    }
    key(dayStart) {
        GuideGridForDay(
            rows = rows,
            dayStart = dayStart,
            nowMillis = nowMillis,
            selectedProgramId = selectedProgramId,
            selectedChannelId = selectedChannelId,
            showLogos = showLogos,
            onChannelSelected = onChannelSelected,
            onProgramFocused = onProgramFocused,
            onProgramClicked = onProgramClicked,
            modifier = modifier
        )
    }
}

@Composable
private fun GuideGridForDay(
    rows: List<GuideChannelState>,
    dayStart: Long,
    nowMillis: Long,
    selectedProgramId: Long?,
    selectedChannelId: Long?,
    showLogos: Boolean,
    onChannelSelected: (ChannelEntity, ProgramEntity?) -> Unit,
    onProgramFocused: (ChannelEntity, ProgramEntity) -> Unit,
    onProgramClicked: (ChannelEntity, ProgramEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val dayEnd = dayStart + DAY_MILLIS
    val density = LocalDensity.current
    val initialMinute = currentTimeMinutes(nowMillis, dayStart, dayEnd)?.minus(25f)?.coerceAtLeast(0f) ?: 0f
    val initialScroll = with(density) { (MINUTE_WIDTH * initialMinute).roundToPx() }
    val horizontalState = rememberScrollState(initial = initialScroll)
    val scope = rememberCoroutineScope()
    val timelineWidth = MINUTE_WIDTH * MINUTES_PER_DAY.toFloat()
    val currentMinute = currentTimeMinutes(nowMillis, dayStart, dayEnd)

    Column(modifier.background(CinematicColors.Panel.copy(alpha = 0.42f), RoundedCornerShape(6.dp)).border(1.dp, CinematicColors.Border, RoundedCornerShape(6.dp))) {
        Row(Modifier.fillMaxWidth().height(TIME_HEADER_HEIGHT)) {
            Box(
                Modifier.width(CHANNEL_COLUMN_WIDTH).fillMaxHeight().background(CinematicColors.PanelRaised.copy(alpha = 0.86f)),
                contentAlignment = Alignment.CenterStart
            ) {
                Text("Zenders", color = CinematicColors.TextMuted, style = CinematicTypography.Badge, modifier = Modifier.padding(start = 8.dp))
            }
            Box(Modifier.weight(1f).fillMaxHeight().horizontalScroll(horizontalState, reverseScrolling = false)) {
                TimelineHeader(dayStart, timelineWidth, currentMinute)
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(rows, key = { it.channel.id }) { row ->
                val segments = remember(row.programs, dayStart) { buildTimelineSegments(row.programs, dayStart, dayEnd) }
                Row(Modifier.fillMaxWidth().height(CHANNEL_ROW_HEIGHT)) {
                    GuideChannelCell(
                        row = row,
                        selected = row.channel.id == selectedChannelId,
                        showLogos = showLogos,
                        onClick = {
                            val current = row.programs.firstOrNull { it.startTime <= nowMillis && it.endTime > nowMillis }
                                ?: row.programs.firstOrNull()
                            onChannelSelected(row.channel, current)
                        }
                    )
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        if (segments.isEmpty()) {
                            Box(Modifier.fillMaxSize().background(CinematicColors.Panel.copy(alpha = 0.62f)), contentAlignment = Alignment.CenterStart) {
                                Text("Geen programma-informatie", color = CinematicColors.TextMuted, style = CinematicTypography.TimelineMeta, modifier = Modifier.padding(start = 10.dp))
                            }
                        } else {
                            Box(Modifier.fillMaxSize().horizontalScroll(horizontalState, reverseScrolling = false)) {
                                Box(Modifier.width(timelineWidth).fillMaxHeight()) {
                                    segments.forEach { segment ->
                                        key(segment.program.id) {
                                            TimelineProgramBlock(
                                                segment = segment,
                                                nowMillis = nowMillis,
                                                selected = segment.program.id == selectedProgramId,
                                                onFocused = {
                                                    onProgramFocused(row.channel, segment.program)
                                                    val startPx = with(density) { (MINUTE_WIDTH * segment.startMinutes).roundToPx() }
                                                    val endPx = with(density) { (MINUTE_WIDTH * (segment.startMinutes + segment.durationMinutes)).roundToPx() }
                                                    val marginPx = with(density) { 20.dp.roundToPx() }
                                                    val viewportStart = horizontalState.value
                                                    val viewportEnd = viewportStart + horizontalState.viewportSize
                                                    val target = when {
                                                        startPx < viewportStart + marginPx -> startPx - marginPx
                                                        endPx > viewportEnd - marginPx -> endPx - horizontalState.viewportSize + marginPx
                                                        else -> null
                                                    }
                                                    target?.let {
                                                        scope.launch {
                                                            horizontalState.animateScrollTo(it.coerceIn(0, horizontalState.maxValue))
                                                        }
                                                    }
                                                },
                                                onClick = { onProgramClicked(row.channel, segment.program) }
                                            )
                                        }
                                    }
                                    currentMinute?.let {
                                        Box(
                                            Modifier.offset(x = MINUTE_WIDTH * it).width(1.dp).fillMaxHeight()
                                                .background(CinematicColors.GoldBright.copy(alpha = 0.72f))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineHeader(dayStart: Long, timelineWidth: Dp, currentMinute: Float?) {
    Box(Modifier.width(timelineWidth).fillMaxHeight().background(CinematicColors.PanelRaised.copy(alpha = 0.8f))) {
        Row(Modifier.fillMaxSize()) {
            repeat(48) { halfHour ->
                val timestamp = dayStart + halfHour * 30L * 60_000L
                Box(
                    Modifier.width(MINUTE_WIDTH * 30).fillMaxHeight()
                        .background(if (halfHour % 2 == 0) CinematicColors.Panel.copy(alpha = 0.16f) else Color.Transparent),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(formatTime(timestamp), color = CinematicColors.TextSecondary, style = CinematicTypography.TimelineMeta, modifier = Modifier.padding(start = 6.dp))
                    Box(Modifier.align(Alignment.CenterEnd).width(0.5.dp).fillMaxHeight().background(CinematicColors.BorderStrong.copy(alpha = 0.45f)))
                }
            }
        }
        currentMinute?.let {
            Box(Modifier.offset(x = MINUTE_WIDTH * it).width(1.dp).fillMaxHeight().background(CinematicColors.GoldBright))
            Box(
                Modifier.offset(x = MINUTE_WIDTH * it - 2.5.dp, y = 1.dp).size(6.dp)
                    .graphicsLayer { rotationZ = 45f }
                    .background(CinematicColors.GoldBright)
            )
        }
    }
}

@Composable
private fun GuideChannelCell(row: GuideChannelState, selected: Boolean, showLogos: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(CHANNEL_COLUMN_WIDTH)
            .fillMaxHeight()
            .background(if (selected) CinematicColors.Gold.copy(alpha = 0.015f) else CinematicColors.Panel.copy(alpha = 0.84f))
            .border(0.5.dp, if (selected) CinematicColors.Gold.copy(alpha = 0.2f) else CinematicColors.Border)
            .clickable(onClick = onClick)
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChannelLogo(row.channel, showLogos, 26.dp)
        Column(Modifier.weight(1f).padding(start = 5.dp)) {
            Text(row.channel.name, color = CinematicColors.TextPrimary, style = CinematicTypography.TimelineTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text((row.channel.sortOrder + 1).toString(), color = CinematicColors.TextMuted, style = CinematicTypography.Badge)
        }
    }
}

@Composable
private fun TimelineProgramBlock(
    segment: TimelineSegment,
    nowMillis: Long,
    selected: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit
) {
    val program = segment.program
    val current = program.startTime <= nowMillis && program.endTime > nowMillis
    val past = program.endTime <= nowMillis
    val interactionSource = remember { MutableInteractionSource() }
    var focused by remember { mutableStateOf(false) }
    val emphasized = selected || focused
    val blockWidth = (MINUTE_WIDTH * segment.durationMinutes).coerceAtLeast(2.dp)
    val narrow = blockWidth < 18.dp
    val shape = RoundedCornerShape(if (narrow) 2.dp else 5.dp)
    val background = when {
        selected -> CinematicColors.Gold.copy(alpha = if (narrow) 0.1f else 0.07f)
        current -> CinematicColors.PanelRaised.copy(alpha = 0.86f)
        past -> CinematicColors.Panel.copy(alpha = 0.38f)
        else -> CinematicColors.PanelRaised.copy(alpha = 0.7f)
    }
    val border by animateColorAsState(
        if (emphasized) CinematicColors.Gold.copy(alpha = if (focused) 0.88f else 0.7f)
        else if (current) CinematicColors.Gold.copy(alpha = 0.14f)
        else CinematicColors.Border,
        tween(StreamGuideMotion.Quick),
        label = "program-border"
    )
    val scale by animateFloatAsState(if (focused) 1.002f else 1f, tween(StreamGuideMotion.Quick), label = "program-scale")
    val horizontalInset = if (narrow) 0.25.dp else 0.75.dp
    val innerWidth = (blockWidth - if (narrow) 0.5.dp else 1.5.dp).coerceAtLeast(1.dp)
    Column(
        modifier = Modifier
            .offset(x = MINUTE_WIDTH * segment.startMinutes)
            .padding(horizontal = horizontalInset, vertical = 1.5.dp)
            .width(innerWidth)
            .fillMaxHeight()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(if (focused && !narrow) 4.dp else 0.dp, shape, ambientColor = CinematicColors.Gold, spotColor = CinematicColors.Gold)
            .background(background, shape)
            .then(
                if (narrow && !emphasized) Modifier
                else Modifier.border(if (narrow) 1.dp else 0.75.dp, border, shape)
            )
            .clip(shape)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            }
            .focusable(interactionSource = interactionSource)
            .clickable(interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = if (blockWidth >= 36.dp) 5.dp else 0.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        if (blockWidth >= 32.dp) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                program.title,
                color = if (past) CinematicColors.TextMuted else CinematicColors.TextPrimary,
                style = CinematicTypography.TimelineTitle,
                fontWeight = if (current || emphasized) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (current && blockWidth >= 96.dp) {
                Box(Modifier.background(CinematicColors.Live, RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp)) {
                    Text("LIVE", color = Color.White, style = CinematicTypography.Badge)
                }
            }
        }
        if (blockWidth >= 68.dp) {
            Text(programTimeRange(program), color = CinematicColors.TextMuted, style = CinematicTypography.TimelineMeta, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun GuideStatusStrip(state: StreamGuideState) {
    val text = when {
        state.isLoading -> state.message ?: "EPG wordt geladen"
        state.error != null -> state.error
        else -> null
    } ?: return
    Text(
        text,
        color = if (state.error != null) Color(0xFFFFB4AB) else CinematicColors.TextSecondary,
        style = CinematicTypography.Metadata,
        modifier = Modifier.fillMaxWidth().background(CinematicColors.PanelRaised.copy(alpha = 0.72f)).padding(horizontal = 12.dp, vertical = 6.dp),
        maxLines = 2
    )
}

private fun dayStartMillis(day: LocalDate): Long = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun dateLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today.minusDays(1) -> "Gisteren"
    today -> "Vandaag"
    today.plusDays(1) -> "Morgen"
    else -> day.format(dayFormatter)
}

private fun formatTime(timestamp: Long): String = timeFormatter.format(Instant.ofEpochMilli(timestamp))

private const val ALL_KEY = "all"
private const val FAVORITES_KEY = "favorites"
private const val MINUTES_PER_DAY = 1440
private const val DAY_MILLIS = 24L * 60L * 60L * 1000L
private val MINUTE_WIDTH = 2.dp
private val CHANNEL_COLUMN_WIDTH = 96.dp
private val CHANNEL_ROW_HEIGHT = 52.dp
private val TIME_HEADER_HEIGHT = 28.dp
private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("nl-NL"))

@Preview(name = "Guide - phone portrait", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun ProgramGuidePhonePreview() {
    ProgramGuideScreen(
        state = liveGuidePreviewState(),
        onQueryChange = {},
        onDaySelected = {},
        onToggleFavorite = {},
        onWatch = {},
        onPrepareCast = {},
        onOpenLive = {}
    )
}

@Preview(name = "Guide - phone landscape", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun ProgramGuideLandscapePreview() {
    ProgramGuidePhonePreview()
}

@Preview(name = "Guide - tablet", widthDp = 1100, heightDp = 720, showBackground = true)
@Composable
private fun ProgramGuideTabletPreview() {
    ProgramGuidePhonePreview()
}
