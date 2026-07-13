package com.example.streamguidemobile.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.streamguidemobile.ChannelRowState
import com.example.streamguidemobile.StreamGuideState
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import com.example.streamguidemobile.ui.theme.StreamGuideSpacing
import com.example.streamguidemobile.ui.preview.liveGuidePreviewState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTvScreen(
    state: StreamGuideState,
    onQueryChange: (String) -> Unit,
    onShowAll: () -> Unit,
    onFavoritesOnly: () -> Unit,
    onRecentOnly: () -> Unit,
    onGroupSelected: (String) -> Unit,
    onToggleFavorite: (ChannelRowState) -> Unit,
    onWatch: (ChannelRowState) -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = remember(state.groups) {
        buildList {
            add(LiveCategoryItem(ALL_KEY, "Alle zenders"))
            add(LiveCategoryItem(FAVORITES_KEY, "Favorieten"))
            add(LiveCategoryItem(RECENT_KEY, "Recent"))
            state.groups.distinctBy { it.lowercase() }.forEach { add(LiveCategoryItem("group:$it", it)) }
        }
    }
    val selectedCategoryKey = when {
        state.showFavorites -> FAVORITES_KEY
        state.showRecent -> RECENT_KEY
        state.selectedGroup != null -> "group:${state.selectedGroup}"
        else -> ALL_KEY
    }
    var selectedChannelId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var showCategoryPicker by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.channelRows) {
        if (state.channelRows.none { it.channel.id == selectedChannelId }) {
            selectedChannelId = state.channelRows.firstOrNull()?.channel?.id
        }
    }
    val selectedRow = state.channelRows.firstOrNull { it.channel.id == selectedChannelId }
        ?: state.channelRows.firstOrNull()

    fun selectCategory(item: LiveCategoryItem) {
        when (item.key) {
            ALL_KEY -> onShowAll()
            FAVORITES_KEY -> onFavoritesOnly()
            RECENT_KEY -> onRecentOnly()
            else -> item.key.removePrefix("group:").takeIf { it.isNotBlank() }?.let(onGroupSelected)
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize().background(
            Brush.verticalGradient(
                0f to CinematicColors.CanvasTop,
                0.32f to CinematicColors.Canvas,
                1f to Color(0xFF020407)
            )
        )
    ) {
        val wide = maxWidth >= 720.dp || maxWidth > maxHeight
        val compactHeight = maxHeight < 500.dp
        val detailWidth = if (maxWidth >= 1000.dp) 340.dp else 292.dp
        Column(Modifier.fillMaxSize()) {
            LiveToolbar(
                channelCount = state.channelRows.size,
                query = state.query,
                favoritesSelected = state.showFavorites,
                onQueryChange = onQueryChange,
                onFilter = { showCategoryPicker = true },
                onFavorites = { if (state.showFavorites) onShowAll() else onFavoritesOnly() },
                onGuide = onOpenGuide
            )
            LiveCategoryRow(
                items = categories,
                selectedKey = selectedCategoryKey,
                onSelected = ::selectCategory,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp)
            )
            StatusStrip(state)
            if (wide) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ChannelList(
                        rows = state.channelRows,
                        loading = state.isLoading,
                        selectedId = selectedChannelId,
                        showLogos = state.settings.showLogos,
                        onSelected = { selectedChannelId = it.channel.id },
                        onFavorite = onToggleFavorite,
                        modifier = Modifier.weight(1f)
                    )
                    ProgramDetailPanel(
                        selection = selectedRow?.asProgramSelection(),
                        nowMillis = state.nowMillis,
                        showLogos = state.settings.showLogos,
                        onWatch = { selectedRow?.let(onWatch) },
                        onFavorite = { selectedRow?.let(onToggleFavorite) },
                        onInfo = { if (selectedRow != null) showInfo = true },
                        imageHeight = if (compactHeight) 96.dp else 154.dp,
                        modifier = Modifier.width(detailWidth).heightIn(min = 220.dp)
                    )
                }
            } else {
                ChannelList(
                    rows = state.channelRows,
                    loading = state.isLoading,
                    selectedId = selectedChannelId,
                    showLogos = state.settings.showLogos,
                    onSelected = {
                        selectedChannelId = it.channel.id
                        showDetails = true
                    },
                    onFavorite = onToggleFavorite,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            title = "Zendercategorie",
            items = categories,
            selectedKey = selectedCategoryKey,
            onSelected = {
                selectCategory(it)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    if (showDetails && selectedRow != null) {
        ModalBottomSheet(
            onDismissRequest = { showDetails = false },
            containerColor = CinematicColors.PanelRaised,
            contentColor = CinematicColors.TextPrimary,
            dragHandle = {
                Box(
                    Modifier.padding(vertical = 9.dp).width(38.dp).heightIn(min = 3.dp)
                        .background(CinematicColors.BorderStrong)
                )
            }
        ) {
            ProgramDetailPanel(
                selection = selectedRow.asProgramSelection(),
                nowMillis = state.nowMillis,
                showLogos = state.settings.showLogos,
                onWatch = {
                    showDetails = false
                    onWatch(selectedRow)
                },
                onFavorite = { onToggleFavorite(selectedRow) },
                onInfo = { showInfo = true },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            )
            Box(Modifier.padding(bottom = 20.dp))
        }
    }

    if (showInfo && selectedRow != null) {
        ProgramInfoDialog(
            selection = selectedRow.asProgramSelection(),
            nowMillis = state.nowMillis,
            onWatch = {
                showInfo = false
                showDetails = false
                onWatch(selectedRow)
            },
            onDismiss = { showInfo = false }
        )
    }
}

@Composable
private fun LiveToolbar(
    channelCount: Int,
    query: String,
    favoritesSelected: Boolean,
    onQueryChange: (String) -> Unit,
    onFilter: () -> Unit,
    onFavorites: () -> Unit,
    onGuide: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 3.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Live TV", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                Text("$channelCount zenders", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CinematicIconAction(Icons.Default.FilterList, "Categorie filteren", onFilter)
                CinematicIconAction(Icons.Default.Favorite, "Alleen favorieten", onFavorites, selected = favoritesSelected)
                CinematicIconAction(Icons.Default.DateRange, "Programmagids openen", onGuide)
            }
        }
        CinematicSearchField(
            value = query,
            placeholder = "Zoek zender, groep of programma",
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ChannelList(
    rows: List<ChannelRowState>,
    loading: Boolean,
    selectedId: Long?,
    showLogos: Boolean,
    onSelected: (ChannelRowState) -> Unit,
    onFavorite: (ChannelRowState) -> Unit,
    modifier: Modifier = Modifier
) {
    if (rows.isEmpty()) {
        CinematicEmptyState(
            title = if (loading) "Zenders worden geladen" else "Geen zenders gevonden",
            description = if (loading) "Even geduld" else "Pas je zoekopdracht of filter aan",
            modifier = modifier
        )
        return
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items(rows, key = { it.channel.id }) { row ->
            CompactChannelRow(
                row = row,
                selected = row.channel.id == selectedId,
                showLogos = showLogos,
                onSelect = { onSelected(row) },
                onFavorite = { onFavorite(row) }
            )
        }
    }
}

@Composable
private fun StatusStrip(state: StreamGuideState) {
    val text = when {
        state.isLoading -> state.message ?: "EPG wordt geladen"
        state.error != null -> state.error
        else -> null
    } ?: return
    Text(
        text,
        color = if (state.error != null) Color(0xFFFFB4AB) else CinematicColors.TextSecondary,
        style = CinematicTypography.Metadata,
        modifier = Modifier.fillMaxWidth().background(CinematicColors.PanelRaised.copy(alpha = 0.72f)).padding(horizontal = 14.dp, vertical = 7.dp),
        maxLines = 2
    )
}

private const val ALL_KEY = "all"
private const val FAVORITES_KEY = "favorites"
private const val RECENT_KEY = "recent"

@Preview(name = "Live TV - phone portrait", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun LiveTvPhonePreview() {
    LiveTvScreen(
        state = liveGuidePreviewState(),
        onQueryChange = {},
        onShowAll = {},
        onFavoritesOnly = {},
        onRecentOnly = {},
        onGroupSelected = {},
        onToggleFavorite = {},
        onWatch = {},
        onOpenGuide = {}
    )
}

@Preview(name = "Live TV - phone landscape", widthDp = 844, heightDp = 390, showBackground = true)
@Composable
private fun LiveTvLandscapePreview() {
    LiveTvPhonePreview()
}

@Preview(name = "Live TV - tablet", widthDp = 1100, heightDp = 720, showBackground = true)
@Composable
private fun LiveTvTabletPreview() {
    LiveTvPhonePreview()
}
