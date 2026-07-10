package com.example.streamguidemobile

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Rational
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.ui.theme.StreamGuideTheme
import com.example.streamguidemobile.worker.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<StreamGuideViewModel>()
    private var enterPictureInPicture: (() -> Unit)? = null
    private var pictureInPictureModeChanged: ((Boolean) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncScheduler.schedule(this)
        setContent {
            StreamGuideTheme {
                StreamGuideApp(viewModel)
            }
        }
    }

    fun setPictureInPictureCallbacks(
        enter: (() -> Unit)?,
        onModeChanged: ((Boolean) -> Unit)?
    ) {
        enterPictureInPicture = enter
        pictureInPictureModeChanged = onModeChanged
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPictureInPicture?.invoke()
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: android.content.res.Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pictureInPictureModeChanged?.invoke(isInPictureInPictureMode)
    }
}

@Composable
private fun StreamGuideApp(viewModel: StreamGuideViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var showAddPlaylist by remember { mutableStateOf(false) }

    selectedChannel?.let { channel ->
        PlayerScreen(
            channel = channel,
            state = state,
            viewModel = viewModel,
            onBack = { selectedChannel = null },
            onOpenChannel = { selectedChannel = it }
        )
        return
    }

    val addBack: (() -> Unit)? = if (state.playlists.isEmpty()) null else ({ showAddPlaylist = false })
    if (state.playlists.isEmpty() || showAddPlaylist) {
        AddPlaylistScreen(
            state = state,
            viewModel = viewModel,
            onBack = addBack
        )
    } else {
        HomeScreen(
            state = state,
            viewModel = viewModel,
            onAddPlaylist = { showAddPlaylist = true },
            onOpen = { channel ->
                viewModel.markWatched(channel)
                selectedChannel = channel
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPlaylistScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onBack: (() -> Unit)? = null) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(SourceMode.Xtream) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var epgUrl by remember { mutableStateOf("") }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val localName = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: "Lokale playlist"
            viewModel.importPlaylist(name.ifBlank { localName }, uri.toString(), epgUrl)
        }
    }
    val back = onBack

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bron toevoegen", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (back != null) {
                        IconButton(onClick = back) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug") }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LiveTv, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("Jouw live tv", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Voeg een eigen legale bron toe",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = mode == SourceMode.Xtream,
                        onClick = { mode = SourceMode.Xtream },
                        label = { Text("Xtream Codes") }
                    )
                    FilterChip(
                        selected = mode == SourceMode.M3u,
                        onClick = { mode = SourceMode.M3u },
                        label = { Text("M3U") }
                    )
                }
            }
            item { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Playlist naam") }, modifier = Modifier.fillMaxWidth()) }

            if (mode == SourceMode.Xtream) {
                item { OutlinedTextField(value = serverUrl, onValueChange = { serverUrl = it }, label = { Text("Server URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Gebruikersnaam") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Wachtwoord") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                item { OutlinedTextField(value = epgUrl, onValueChange = { epgUrl = it }, label = { Text("EPG URL optioneel - bij Xtream meestal leeg laten") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Button(
                        onClick = { viewModel.importXtream(name, serverUrl, username, password, epgUrl) },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Xtream laden")
                    }
                }
            } else {
                item { OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("M3U URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = epgUrl, onValueChange = { epgUrl = it }, label = { Text("XMLTV EPG URL optioneel") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Button(onClick = { viewModel.importPlaylist(name, url, epgUrl) }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Sync, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Opslaan en laden")
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            documentPicker.launch(
                                arrayOf(
                                    "application/vnd.apple.mpegurl",
                                    "application/x-mpegURL",
                                    "audio/x-mpegurl",
                                    "text/plain",
                                    "*/*"
                                )
                            )
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("M3U-bestand kiezen")
                    }
                }
            }

            item { StatusBlock(state) }
            item { Text("Gebruik uitsluitend bronnen waarvoor je toestemming hebt.", color = MaterialTheme.colorScheme.secondary) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: StreamGuideState,
    viewModel: StreamGuideViewModel,
    onAddPlaylist: () -> Unit,
    onOpen: (ChannelEntity) -> Unit
) {
    var section by remember { mutableStateOf(HomeSection.Channels) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            modifier = Modifier.size(34.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Column(Modifier.padding(start = 10.dp)) {
                            Row {
                                Text("StreamGuide", style = MaterialTheme.typography.titleLarge)
                                Text(" TV", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("MOBILE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onAddPlaylist) { Icon(Icons.Default.Add, contentDescription = "Playlist toevoegen") }
                    IconButton(onClick = viewModel::syncAllPlaylists) { Icon(Icons.Default.Refresh, contentDescription = "Alles synchroniseren") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = section == HomeSection.Channels,
                    onClick = { section = HomeSection.Channels },
                    icon = { Icon(Icons.Default.LiveTv, contentDescription = null) },
                    label = { Text("Zenders") }
                )
                NavigationBarItem(
                    selected = section == HomeSection.Guide,
                    onClick = { section = HomeSection.Guide },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Gids") }
                )
                NavigationBarItem(
                    selected = section == HomeSection.Settings,
                    onClick = { section = HomeSection.Settings },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    label = { Text("Instellingen") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (section) {
                HomeSection.Channels -> ChannelListScreen(state, viewModel, onOpen)
                HomeSection.Guide -> EpgScreen(state, viewModel, onOpen)
                HomeSection.Settings -> SettingsScreen(state, viewModel, onAddPlaylist)
            }
        }
    }
}

@Composable
private fun ChannelListScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onOpen: (ChannelEntity) -> Unit) {
    var showGroupFilter by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text("Live tv", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${state.channelRows.size} zichtbare zenders",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { showGroupFilter = true },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = "Zendergroepen filteren")
            }
        }
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Zoek zender, groep of programma") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        FilterRow(state, viewModel)
        StatusBlock(state)
        val continueRow = state.channelRows.firstOrNull { it.channel.lastWatchedAt != null }
        if (continueRow != null && !state.showRecent && state.query.isBlank()) {
            ContinueWatchingCard(continueRow, onOpen)
        }
        if (state.channelRows.isEmpty()) {
            EmptyState("Geen zenders binnen dit filter.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.channelRows, key = { it.channel.id }) { row ->
                    ChannelRow(
                        row = row,
                        showLogos = state.settings.showLogos,
                        compact = state.settings.compactList,
                        onOpen = { onOpen(row.channel) },
                        onFavorite = { viewModel.toggleFavorite(row.channel) }
                    )
                }
            }
        }
    }

    if (showGroupFilter) {
        GroupVisibilityDialog(
            groups = state.allGroups,
            hiddenGroups = state.settings.hiddenGroups,
            onGroupVisible = viewModel::setGroupVisible,
            onShowAll = viewModel::showAllGroups,
            onHideAll = { viewModel.hideAllGroups(state.allGroups) },
            onDismiss = { showGroupFilter = false }
        )
    }
}

@Composable
private fun FilterRow(state: StreamGuideState, viewModel: StreamGuideViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !state.showFavorites && !state.showRecent && state.selectedGroup == null,
            onClick = viewModel::showAllChannels,
            label = { Text("Alles") },
            leadingIcon = { Icon(Icons.Default.LiveTv, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        FilterChip(
            selected = state.showFavorites,
            onClick = { viewModel.setFavoritesOnly(true) },
            label = { Text("Favorieten") },
            leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        FilterChip(
            selected = state.showRecent,
            onClick = { viewModel.setRecentOnly(true) },
            label = { Text("Recent") },
            leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) }
        )
        state.groups.forEach { group ->
            FilterChip(selected = state.selectedGroup == group, onClick = { viewModel.selectGroup(group) }, label = { Text(group, maxLines = 1) })
        }
    }
}

@Composable
private fun ContinueWatchingCard(row: ChannelRowState, onOpen: (ChannelEntity) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = { onOpen(row.channel) }).padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(row.channel, showLogos = true)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("VERDER KIJKEN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(row.channel.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                row.currentProgram?.let { Text(it.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            IconButton(
                onClick = { onOpen(row.channel) },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Verder kijken")
            }
        }
    }

}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelRow(row: ChannelRowState, showLogos: Boolean, compact: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    val verticalPadding = if (compact) 8.dp else 12.dp
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = { showInfo = true }),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ChannelLogo(row.channel, showLogos)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val program = row.currentProgram
                if (program != null) {
                    Text(program.title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    LinearProgressIndicator(progress = { row.progress }, modifier = Modifier.fillMaxWidth().height(3.dp))
                } else {
                    Text(row.channel.groupTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (row.channel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Favoriet",
                    tint = if (row.channel.isFavorite) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showInfo) {
        ChannelInfoDialog(row, onDismiss = { showInfo = false }, onFavorite = onFavorite)
    }
}

@Composable
private fun ChannelLogo(channel: ChannelEntity, showLogos: Boolean, size: Dp = 48.dp) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (showLogos && !channel.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(6.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ChannelInfoDialog(row: ChannelRowState, onDismiss: () -> Unit, onFavorite: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text(row.channel.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Groep: ${row.channel.groupTitle}")
                row.currentProgram?.let { Text("Nu: ${it.title}") }
                row.nextProgram?.let { Text("Straks: ${it.title}") }
            }
        },
        confirmButton = { TextButton(onClick = { onFavorite(); onDismiss() }) { Text(if (row.channel.isFavorite) "Favoriet verwijderen" else "Favoriet maken") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}

@Composable
private fun EpgScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onOpen: (ChannelEntity) -> Unit) {
    val today = remember { LocalDate.now() }
    val days = remember(today) { (-1..6).map { today.plusDays(it.toLong()) } }
    var selectedProgram by remember { mutableStateOf<Pair<ChannelEntity, ProgramEntity>?>(null) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text("Tv-gids", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Programma's per dag",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            days.forEach { day ->
                val dayStart = dayStartMillis(day)
                FilterChip(
                    selected = state.guideDayStart == dayStart,
                    onClick = { viewModel.selectGuideDay(dayStart) },
                    label = { Text(dateChipLabel(day, today)) }
                )
            }
        }
        OutlinedTextField(
            value = state.guideQuery,
            onValueChange = viewModel::updateGuideQuery,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            placeholder = { Text("Zoek in tv-gids") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
            singleLine = true,
            shape = RoundedCornerShape(8.dp)
        )
        StatusBlock(state)
        if (state.guideRows.isEmpty()) {
            EmptyState("Geen programma's gevonden voor deze dag. Synchroniseer de EPG bij Instellingen.")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.guideRows, key = { it.channel.id }) { row ->
                    EpgRow(
                        row = row,
                        guideDayStart = state.guideDayStart,
                        nowMillis = state.nowMillis,
                        showLogos = state.settings.showLogos,
                        onOpen = onOpen,
                        onProgramSelected = { program -> selectedProgram = row.channel to program }
                    )
                }
            }
        }
    }

    selectedProgram?.let { (channel, program) ->
        ProgramDetailDialog(
            channel = channel,
            program = program,
            onWatch = { selectedProgram = null; onOpen(channel) },
            onDismiss = { selectedProgram = null }
        )
    }

}

@Composable
private fun EpgRow(
    row: GuideChannelState,
    guideDayStart: Long,
    nowMillis: Long,
    showLogos: Boolean,
    onOpen: (ChannelEntity) -> Unit,
    onProgramSelected: (ProgramEntity) -> Unit
) {
    var expanded by remember(row.channel.id, guideDayStart) { mutableStateOf(false) }
    val visiblePrograms = if (expanded) row.programs else row.programs.take(2)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(row.channel, showLogos = showLogos, size = 36.dp)
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(row.channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(row.channel.groupTitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                IconButton(onClick = { onOpen(row.channel) }, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Zender afspelen", tint = MaterialTheme.colorScheme.primary)
                }
            }
            visiblePrograms.forEachIndexed { index, program ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                ProgramLine(
                    program = program,
                    isCurrent = program.startTime <= nowMillis && program.endTime > nowMillis,
                    onClick = { onProgramSelected(program) }
                )
            }
            if (row.programs.size > 2) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.End)) {
                    Text(if (expanded) "Minder tonen" else "Nog ${row.programs.size - 2} programma's")
                }
            }
        }
    }
}

@Composable
private fun ProgramLine(program: ProgramEntity, isCurrent: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.width(54.dp)) {
            Text(
                if (isCurrent) "NU" else formatTime(program.startTime),
                style = MaterialTheme.typography.labelMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Text(formatTime(program.endTime), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.weight(1f)) {
            Text(program.title, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            program.category?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
private fun ProgramDetailDialog(
    channel: ChannelEntity,
    program: ProgramEntity,
    onWatch: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
        title = { Text(program.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(channel.name, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("${formatTime(program.startTime)} - ${formatTime(program.endTime)}")
                program.category?.takeIf { it.isNotBlank() }?.let { Text(it, color = MaterialTheme.colorScheme.secondary) }
                program.description?.takeIf { it.isNotBlank() }?.let { Text(it) }
            }
        },
        confirmButton = {
            Button(onClick = onWatch) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Kijken")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Sluiten") } }
    )
}

@Composable
private fun SettingsScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onAddPlaylist: () -> Unit) {
    var showGroupFilter by remember { mutableStateOf(false) }
    val hiddenGroupCount = state.allGroups.count { group ->
        state.settings.hiddenGroups.any { it.equals(group, ignoreCase = true) }
    }

    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(Modifier.padding(bottom = 4.dp)) {
                Text("Instellingen", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Beheer bronnen, gids en weergave",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsSection(title = "Playlists", icon = Icons.Default.LiveTv) {
                state.playlists.forEach { playlist ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (playlist.epgUrl.isNullOrBlank()) "Geen losse EPG URL" else "EPG ingesteld", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { viewModel.deletePlaylist(playlist.id) }) { Text("Verwijderen") }
                    }
                }
                Button(onClick = onAddPlaylist, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Playlist toevoegen")
                }
            }
        }
        item {
            SettingsSection(title = "Synchronisatie", icon = Icons.Default.Sync) {
                SettingsSwitch(
                    label = "Playlists bij starten",
                    supportingText = "Nieuwe en gewijzigde zenders automatisch ophalen",
                    checked = state.settings.syncPlaylistsOnStart,
                    onChanged = viewModel::setSyncPlaylistsOnStart
                )
                SettingsSwitch(
                    label = "EPG bij starten",
                    supportingText = "De tv-gids automatisch verversen",
                    checked = state.settings.syncEpgOnStart,
                    onChanged = viewModel::setSyncEpgOnStart
                )
                OutlinedButton(onClick = viewModel::syncAllPlaylists, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Alle playlists nu synchroniseren")
                }
                OutlinedButton(onClick = viewModel::syncAllEpg, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Alle EPG nu synchroniseren")
                }
            }
        }
        item {
            SettingsSection(title = "Zendergroepen", icon = Icons.Default.FilterList) {
                Text(
                    if (hiddenGroupCount == 0) "Alle groepen zijn zichtbaar" else "$hiddenGroupCount groepen verborgen",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(onClick = { showGroupFilter = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Tune, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Groepen kiezen")
                }
            }
        }
        item {
            SettingsSection(title = "Player", icon = Icons.Default.PlayArrow) {
                SettingsSwitch(
                    "Automatisch laatste kanaal hervatten",
                    "Onthoud de laatst bekeken zender",
                    state.settings.autoResumeLastChannel,
                    viewModel::setAutoResumeLastChannel
                )
                SettingsSwitch(
                    "Hardware decoding",
                    "Gebruik de videodecoder van het toestel",
                    state.settings.hardwareDecoding,
                    viewModel::setHardwareDecoding
                )
            }
        }
        item {
            SettingsSection(title = "Weergave", icon = Icons.Default.Visibility) {
                SettingsSwitch("Logo's tonen", "Toon zenderlogo's in lijsten", state.settings.showLogos, viewModel::setShowLogos)
                SettingsSwitch("Compacte zenderlijst", "Meer zenders tegelijk in beeld", state.settings.compactList, viewModel::setCompactList)
            }
        }
        item {
            SettingsSection(title = "Over de app", icon = Icons.Default.Info) {
                Text("StreamGuide Mobile levert geen zenders, streams of playlists mee. Gebruik alleen bronnen waarvoor je toestemming hebt.")
            }
        }
        item { StatusBlock(state) }
    }

    if (showGroupFilter) {
        GroupVisibilityDialog(
            groups = state.allGroups,
            hiddenGroups = state.settings.hiddenGroups,
            onGroupVisible = viewModel::setGroupVisible,
            onShowAll = viewModel::showAllGroups,
            onHideAll = { viewModel.hideAllGroups(state.allGroups) },
            onDismiss = { showGroupFilter = false }
        )
    }
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, supportingText: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label)
            Text(supportingText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun GroupVisibilityDialog(
    groups: List<String>,
    hiddenGroups: Set<String>,
    onGroupVisible: (String, Boolean) -> Unit,
    onShowAll: () -> Unit,
    onHideAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FilterList, contentDescription = null) },
        title = { Text("Zendergroepen") },
        text = {
            if (groups.isEmpty()) {
                Text("Er zijn nog geen zendergroepen gevonden.")
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(groups, key = { it }) { group ->
                        val visible = hiddenGroups.none { it.equals(group, ignoreCase = true) }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { onGroupVisible(group, !visible) }.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = if (visible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(group, modifier = Modifier.weight(1f).padding(horizontal = 12.dp), maxLines = 2)
                            Switch(checked = visible, onCheckedChange = { onGroupVisible(group, it) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Icon(Icons.Default.Done, contentDescription = null)
                Spacer(Modifier.size(6.dp))
                Text("Klaar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onShowAll) { Text("Alles tonen") }
                TextButton(onClick = onHideAll) { Text("Alles verbergen") }
            }
        }
    )
}

@Composable
private fun PlayerScreen(channel: ChannelEntity, state: StreamGuideState, viewModel: StreamGuideViewModel, onBack: () -> Unit, onOpenChannel: (ChannelEntity) -> Unit) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val row = state.channelRows.firstOrNull { it.channel.id == channel.id }
    var muted by remember(channel.id) { mutableStateOf(false) }
    var controlsVisible by remember(channel.id) { mutableStateOf(true) }
    var isPlaying by remember(channel.id) { mutableStateOf(true) }
    var isBuffering by remember(channel.id) { mutableStateOf(false) }
    var playerError by remember(channel.id) { mutableStateOf<String?>(null) }
    var resizeMode by remember(channel.id) { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var streamQuality by remember(channel.id) { mutableStateOf<String?>(null) }
    val player = remember(channel.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    player.volume = if (muted) 0f else 1f
    val enterPip = remember(activity, player) {
        {
            val playerActivity = activity
            if (playerActivity != null && !playerActivity.isInPictureInPictureMode) {
                playerActivity.enterPictureInPictureMode(
                    PictureInPictureParams.Builder()
                        .setAspectRatio(player.videoSize.pipAspectRatio())
                        .build()
                )
            }
        }
    }

    DisposableEffect(activity) {
        val controller = activity?.window?.let { window -> WindowInsetsControllerCompat(window, window.decorView) }
        val systemBars = WindowInsetsCompat.Type.systemBars()
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(systemBars)
        onDispose { controller?.show(systemBars) }
    }

    DisposableEffect(activity, enterPip) {
        (activity as? MainActivity)?.setPictureInPictureCallbacks(
            enter = enterPip,
            onModeChanged = { isInPip -> controlsVisible = !isInPip }
        )
        onDispose {
            (activity as? MainActivity)?.setPictureInPictureCallbacks(null, null)
        }
    }

    LaunchedEffect(controlsVisible, playerError) {
        if (controlsVisible && playerError == null) {
            delay(4500)
            controlsVisible = false
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    streamQuality = qualityLabel(player.videoSize)
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                streamQuality = qualityLabel(videoSize)
            }

            override fun onPlayerError(error: PlaybackException) {
                playerError = "Stream kan niet worden afgespeeld."
                controlsVisible = true
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    PlayerView(it).apply {
                        useController = false
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                        this.player = player
                        this.resizeMode = resizeMode
                    }
                },
                update = {
                    it.player = player
                    it.resizeMode = resizeMode
                }
            )
            Box(Modifier.fillMaxSize().clickable { controlsVisible = !controlsVisible })

            if (isBuffering && playerError == null) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (controlsVisible || playerError != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.62f)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Terug", tint = Color.White) }
                        ChannelLogo(channel, showLogos = state.settings.showLogos)
                        Text(
                            channel.name,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(formatTime(System.currentTimeMillis()), color = Color.White)
                        streamQuality?.let {
                            Spacer(Modifier.size(8.dp))
                            StreamQualityBadge(it)
                        }
                    }
                    row?.currentProgram?.let { Text(it.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    row?.nextProgram?.let { Text("Straks: ${it.title}", color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.62f)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerControl(Icons.Default.SkipPrevious, "Vorige") {
                        controlsVisible = true
                        scope.launch {
                            val id = viewModel.nextChannelId(channel.id, -1)
                            val next = state.channels.firstOrNull { it.id == id }
                            if (next != null) onOpenChannel(next)
                        }
                    }
                    PlayerControl(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pauze" else "Afspelen") {
                        controlsVisible = true
                        if (player.isPlaying) player.pause() else player.play()
                    }
                    PlayerControl(if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp, if (muted) "Geluid" else "Mute") {
                        controlsVisible = true
                        muted = !muted
                    }
                    PlayerControl(Icons.Default.PictureInPictureAlt, "Mini-speler") {
                        enterPip()
                    }
                    PlayerControl(Icons.Default.AspectRatio, resizeLabel(resizeMode)) {
                        controlsVisible = true
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }
                    PlayerControl(Icons.Default.SkipNext, "Volgende") {
                        controlsVisible = true
                        scope.launch {
                            val id = viewModel.nextChannelId(channel.id, 1)
                            val next = state.channels.firstOrNull { it.id == id }
                            if (next != null) onOpenChannel(next)
                        }
                    }
                }
            }

            playerError?.let { message ->
                Card(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp)
                ) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(message, fontWeight = FontWeight.Bold)
                        Text("Controleer de zender of probeer opnieuw.")
                        Button(onClick = {
                            playerError = null
                            isBuffering = true
                            controlsVisible = true
                            player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
                            player.prepare()
                            player.playWhenReady = true
                        }) {
                            Text("Opnieuw proberen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StreamQualityBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(5.dp),
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PlayerControl(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(Modifier.width(64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = Color.White)
        }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

@Composable
private fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusBlock(state: StreamGuideState) {
    if (state.isLoading || state.message != null || state.error != null) {
        Card(Modifier.fillMaxWidth().padding(16.dp)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.isLoading) CircularProgressIndicator()
                state.message?.let { Text(it) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String = timeFormatter.format(Instant.ofEpochMilli(timestamp))

private fun qualityLabel(videoSize: VideoSize): String? = when (videoSize.height) {
    in 2160..Int.MAX_VALUE -> "4K UHD"
    in 1440..2159 -> "1440p"
    in 1080..1439 -> "1080p"
    in 720..1079 -> "720p HD"
    in 1..719 -> "${videoSize.height}p"
    else -> null
}

private fun VideoSize.pipAspectRatio(): Rational {
    val width = width.coerceAtLeast(16)
    val height = height.coerceAtLeast(9)
    return Rational(width, height)
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun dayStartMillis(day: LocalDate): Long = day
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()

private fun dateChipLabel(day: LocalDate, today: LocalDate): String = when (day) {
    today.minusDays(1) -> "Gisteren"
    today -> "Vandaag"
    today.plusDays(1) -> "Morgen"
    else -> day.format(dayFormatter)
}

private fun resizeLabel(mode: Int): String = when (mode) {
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
    else -> "Fit"
}

private enum class SourceMode { Xtream, M3u }
private enum class HomeSection { Channels, Guide, Settings }

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
private val dayFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.forLanguageTag("nl-NL"))
