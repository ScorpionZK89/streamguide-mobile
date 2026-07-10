package com.example.streamguidemobile

import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.worker.SyncScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<StreamGuideViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SyncScheduler.schedule(this)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
                StreamGuideApp(viewModel)
            }
        }
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
                title = { Text("StreamGuide Mobile") },
                navigationIcon = {
                    if (back != null) {
                        IconButton(onClick = back) { Icon(Icons.Default.ArrowBack, contentDescription = "Terug") }
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Voeg je eigen legale IPTV-bron toe. StreamGuide levert zelf geen content.") }
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
                    ) { Text("Xtream laden") }
                }
            } else {
                item { OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("M3U URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item { OutlinedTextField(value = epgUrl, onValueChange = { epgUrl = it }, label = { Text("XMLTV EPG URL optioneel") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
                item {
                    Button(onClick = { viewModel.importPlaylist(name, url, epgUrl) }, enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
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
                title = { Text("StreamGuide Mobile") },
                actions = {
                    IconButton(onClick = onAddPlaylist) { Icon(Icons.Default.Add, contentDescription = "Playlist toevoegen") }
                    IconButton(onClick = viewModel::syncFirstPlaylist) { Icon(Icons.Default.Refresh, contentDescription = "Sync") }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = section == HomeSection.Channels,
                    onClick = { section = HomeSection.Channels },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
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
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (section) {
                HomeSection.Channels -> ChannelListScreen(state, viewModel, onOpen)
                HomeSection.Guide -> EpgScreen(state, onOpen)
                HomeSection.Settings -> SettingsScreen(state, viewModel, onAddPlaylist)
            }
        }
    }
}

@Composable
private fun ChannelListScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onOpen: (ChannelEntity) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        FilterRow(state, viewModel)
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::updateQuery,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Zoeken") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            singleLine = true
        )
        StatusBlock(state)
        val continueRow = state.channelRows.firstOrNull { it.channel.lastWatchedAt != null }
        if (continueRow != null && !state.showRecent && state.query.isBlank()) {
            ContinueWatchingCard(continueRow, onOpen)
        }
        LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
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

@Composable
private fun FilterRow(state: StreamGuideState, viewModel: StreamGuideViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(selected = !state.showFavorites && !state.showRecent && state.selectedGroup == null, onClick = viewModel::showAllChannels, label = { Text("Alle zenders") })
        FilterChip(selected = state.showFavorites, onClick = { viewModel.setFavoritesOnly(true) }, label = { Text("Favorieten") })
        FilterChip(selected = state.showRecent, onClick = { viewModel.setRecentOnly(true) }, label = { Text("Laatst bekeken") })
        state.groups.forEach { group ->
            FilterChip(selected = state.selectedGroup == group, onClick = { viewModel.selectGroup(group) }, label = { Text(group, maxLines = 1) })
        }
    }
}

@Composable
private fun ContinueWatchingCard(row: ChannelRowState, onOpen: (ChannelEntity) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = { onOpen(row.channel) }).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Verder kijken", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                Text(row.channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                row.currentProgram?.let { Text(it.title, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelRow(row: ChannelRowState, showLogos: Boolean, compact: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit) {
    var showInfo by remember { mutableStateOf(false) }
    val verticalPadding = if (compact) 8.dp else 12.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onOpen, onLongClick = { showInfo = true })
            .padding(horizontal = 16.dp, vertical = verticalPadding),
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
            Icon(if (row.channel.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favoriet")
        }
    }

    if (showInfo) {
        ChannelInfoDialog(row, onDismiss = { showInfo = false }, onFavorite = onFavorite)
    }
}

@Composable
private fun ChannelLogo(channel: ChannelEntity, showLogos: Boolean) {
    Box(
        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
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
private fun EpgScreen(state: StreamGuideState, onOpen: (ChannelEntity) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = true, onClick = {}, label = { Text("Nu op tv") })
            FilterChip(selected = false, onClick = {}, label = { Text("Vandaag") })
        }
        StatusBlock(state)
        val rows = state.channelRows.filter { it.currentProgram != null || it.nextProgram != null }
        if (rows.isEmpty()) {
            EmptyState("Nog geen EPG gevonden. Bij Xtream kun je meestal gewoon EPG synchroniseren zonder losse EPG-link.")
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rows, key = { it.channel.id }) { row ->
                    EpgRow(row, onOpen)
                }
            }
        }
    }
}

@Composable
private fun EpgRow(row: ChannelRowState, onOpen: (ChannelEntity) -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = { onOpen(row.channel) })) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ChannelLogo(row.channel, showLogos = true)
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(row.channel.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(row.channel.groupTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            row.currentProgram?.let { ProgramLine("Nu", it, row.progress) }
            row.nextProgram?.let { ProgramLine("Straks", it, null) }
        }
    }
}

@Composable
private fun ProgramLine(label: String, program: ProgramEntity, progress: Float?) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$label  ${formatTime(program.startTime)} - ${formatTime(program.endTime)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
        Text(program.title, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(3.dp))
    }
}

@Composable
private fun SettingsScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onAddPlaylist: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SettingsSection(title = "Playlists") {
                state.playlists.forEach { playlist ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(playlist.name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(if (playlist.epgUrl.isNullOrBlank()) "Geen losse EPG URL" else "EPG ingesteld", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { viewModel.deletePlaylist(playlist.id) }) { Text("Verwijderen") }
                    }
                }
                Button(onClick = onAddPlaylist, modifier = Modifier.fillMaxWidth()) { Text("Playlist toevoegen") }
                OutlinedButton(onClick = viewModel::syncFirstPlaylist, modifier = Modifier.fillMaxWidth()) { Text("Playlist synchroniseren") }
                OutlinedButton(onClick = viewModel::syncEpgFirstPlaylist, modifier = Modifier.fillMaxWidth()) { Text("EPG synchroniseren") }
            }
        }
        item {
            SettingsSection(title = "Player") {
                SettingsSwitch("Automatisch laatste kanaal hervatten", state.settings.autoResumeLastChannel, viewModel::setAutoResumeLastChannel)
                SettingsSwitch("Hardware decoding", state.settings.hardwareDecoding, viewModel::setHardwareDecoding)
            }
        }
        item {
            SettingsSection(title = "Weergave") {
                SettingsSwitch("Logo's tonen", state.settings.showLogos, viewModel::setShowLogos)
                SettingsSwitch("Compacte zenderlijst", state.settings.compactList, viewModel::setCompactList)
            }
        }
        item {
            SettingsSection(title = "Over de app") {
                Text("StreamGuide Mobile levert geen zenders, streams of playlists mee. Gebruik alleen bronnen waarvoor je toestemming hebt.")
            }
        }
        item { StatusBlock(state) }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun PlayerScreen(channel: ChannelEntity, state: StreamGuideState, viewModel: StreamGuideViewModel, onBack: () -> Unit, onOpenChannel: (ChannelEntity) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val row = state.channelRows.firstOrNull { it.channel.id == channel.id }
    var muted by remember(channel.id) { mutableStateOf(false) }
    var controlsVisible by remember(channel.id) { mutableStateOf(true) }
    var isPlaying by remember(channel.id) { mutableStateOf(true) }
    var isBuffering by remember(channel.id) { mutableStateOf(false) }
    var playerError by remember(channel.id) { mutableStateOf<String?>(null) }
    var resizeMode by remember(channel.id) { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    val player = remember(channel.id) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.streamUrl))
            prepare()
            playWhenReady = true
        }
    }
    player.volume = if (muted) 0f else 1f

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
                        IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = Color.White) }
                        Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatTime(System.currentTimeMillis()), color = Color.White)
                    }
                    row?.currentProgram?.let { Text(it.title, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    row?.nextProgram?.let { Text("Straks: ${it.title}", color = Color.White.copy(alpha = 0.78f), maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }

                Row(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.62f)).padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = {
                        controlsVisible = true
                        scope.launch {
                            val id = viewModel.nextChannelId(channel.id, -1)
                            val next = state.channels.firstOrNull { it.id == id }
                            if (next != null) onOpenChannel(next)
                        }
                    }) { Text("Vorige") }
                    TextButton(onClick = {
                        controlsVisible = true
                        if (player.isPlaying) player.pause() else player.play()
                    }) { Text(if (isPlaying) "Pauze" else "Afspelen") }
                    TextButton(onClick = {
                        controlsVisible = true
                        muted = !muted
                    }) { Text(if (muted) "Geluid" else "Mute") }
                    TextButton(onClick = {
                        controlsVisible = true
                        resizeMode = when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    }) { Text(resizeLabel(resizeMode)) }
                    TextButton(onClick = {
                        controlsVisible = true
                        scope.launch {
                            val id = viewModel.nextChannelId(channel.id, 1)
                            val next = state.channels.firstOrNull { it.id == id }
                            if (next != null) onOpenChannel(next)
                        }
                    }) { Text("Volgende") }
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

private fun resizeLabel(mode: Int): String = when (mode) {
    AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Stretch"
    AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
    else -> "Fit"
}

private enum class SourceMode { Xtream, M3u }
private enum class HomeSection { Channels, Guide, Settings }

private val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
