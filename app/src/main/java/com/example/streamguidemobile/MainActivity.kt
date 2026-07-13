package com.example.streamguidemobile

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.MovieEntity
import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.asPlaybackChannel
import com.example.streamguidemobile.data.nextPlayableEpisode
import com.example.streamguidemobile.ui.theme.StreamGuideTheme
import com.example.streamguidemobile.ui.home.CinematicHomeScreen
import com.example.streamguidemobile.ui.guide.ProgramGuideScreen
import com.example.streamguidemobile.ui.live.LiveTvScreen
import com.example.streamguidemobile.ui.navigation.AppDestination
import com.example.streamguidemobile.ui.navigation.StreamGuideBottomNavigation
import com.example.streamguidemobile.ui.navigation.StreamGuideNavigationRail
import com.example.streamguidemobile.ui.movies.MoviesScreen
import com.example.streamguidemobile.ui.series.SeriesScreen
import com.example.streamguidemobile.ui.player.PremiumPlayerScreen
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import com.example.streamguidemobile.ui.theme.StreamGuideRadii
import com.example.streamguidemobile.ui.theme.StreamGuideSpacing
import com.example.streamguidemobile.ui.theme.StreamGuideMotion
import com.example.streamguidemobile.update.AppRelease
import com.example.streamguidemobile.update.AppUpdateState
import com.example.streamguidemobile.worker.SyncScheduler
import java.io.File
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<StreamGuideViewModel>()
    private var enterPictureInPicture: (() -> Unit)? = null
    private var pictureInPictureModeChanged: ((Boolean) -> Unit)? = null
    private var pendingUpdateFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(6, 9, 13))
        )
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

    override fun onResume() {
        super.onResume()
        val file = pendingUpdateFile ?: return
        if (canInstallPackages()) launchPackageInstaller(file)
    }

    fun requestUpdateInstallation(file: File) {
        pendingUpdateFile = file
        if (canInstallPackages()) {
            launchPackageInstaller(file)
            return
        }
        runCatching {
            startActivity(
                Intent(
                    AndroidSettings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
            )
        }.onFailure {
            Toast.makeText(this, "Open Instellingen en sta installatie door StreamGuide toe.", Toast.LENGTH_LONG).show()
        }
    }

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()

    private fun launchPackageInstaller(file: File) {
        runCatching {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
            pendingUpdateFile = null
        }.onFailure {
            Toast.makeText(this, "Android kon het installatiescherm niet openen.", Toast.LENGTH_LONG).show()
        }
    }
}

@Composable
private fun StreamGuideApp(viewModel: StreamGuideViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? MainActivity
    var selectedChannel by remember { mutableStateOf<ChannelEntity?>(null) }
    var selectedMoviePlayback by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    var selectedEpisodePlayback by remember { mutableStateOf<Pair<Long, Boolean>?>(null) }
    var selectedMovieId by rememberSaveable { mutableStateOf<Long?>(null) }
    var selectedSeriesId by rememberSaveable { mutableStateOf<Long?>(null) }
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var showAddPlaylist by remember { mutableStateOf(false) }

    AppUpdateDialog(
        state = appUpdateState,
        onDownload = viewModel::downloadAppUpdate,
        onInstall = { file -> activity?.requestUpdateInstallation(file) },
        onDismiss = viewModel::dismissAppUpdate
    )

    selectedChannel?.let { channel ->
        PremiumPlayerScreen(
            channel = channel,
            state = state,
            viewModel = viewModel,
            onBack = { selectedChannel = null },
            onOpenChannel = {
                viewModel.markWatched(it)
                selectedChannel = it
            }
        )
        return
    }

    selectedMoviePlayback?.let { (movieId, restart) ->
        val movie = state.movieLibrary.allMovies.firstOrNull { it.id == movieId }
        if (movie != null) {
            PremiumPlayerScreen(
                channel = movie.asPlaybackChannel(),
                movie = movie,
                startFromBeginning = restart,
                state = state,
                viewModel = viewModel,
                onBack = { selectedMoviePlayback = null },
                onOpenChannel = {}
            )
            return
        } else {
            LaunchedEffect(movieId) { selectedMoviePlayback = null }
        }
    }

    selectedEpisodePlayback?.let { (episodeId, restart) ->
        val episode = state.seriesLibrary.allEpisodes.firstOrNull { it.id == episodeId }
        val card = episode?.let { item -> state.seriesLibrary.allSeries.firstOrNull { it.series.id == item.seriesId } }
        if (episode != null && card != null && !episode.streamUrl.isNullOrBlank()) {
            PremiumPlayerScreen(
                channel = episode.asPlaybackChannel(card.series), episode = episode, series = card.series,
                nextEpisode = card.episodes.nextPlayableEpisode(episode), startFromBeginning = restart,
                state = state, viewModel = viewModel, onBack = { selectedEpisodePlayback = null }, onOpenChannel = {},
                onPlayNextEpisode = { next -> selectedEpisodePlayback = next.id to false }
            )
            return
        } else LaunchedEffect(episodeId) { selectedEpisodePlayback = null }
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
            destination = destination,
            selectedMovieId = selectedMovieId,
            selectedSeriesId = selectedSeriesId,
            onDestinationChange = { newDestination ->
                destination = newDestination
                if (newDestination != AppDestination.Movies) selectedMovieId = null
                if (newDestination != AppDestination.Series) selectedSeriesId = null
            },
            onMovieSelected = { selectedMovieId = it },
            onPlayMovie = { movie, restart -> selectedMoviePlayback = movie.id to restart },
            onSeriesSelected = { selectedSeriesId = it },
            onPlayEpisode = { episode, restart -> selectedEpisodePlayback = episode.id to restart },
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

@Composable
private fun HomeScreen(
    state: StreamGuideState,
    viewModel: StreamGuideViewModel,
    destination: AppDestination,
    selectedMovieId: Long?,
    selectedSeriesId: Long?,
    onDestinationChange: (AppDestination) -> Unit,
    onMovieSelected: (Long?) -> Unit,
    onPlayMovie: (MovieEntity, Boolean) -> Unit,
    onSeriesSelected: (Long?) -> Unit,
    onPlayEpisode: (EpisodeEntity, Boolean) -> Unit,
    onAddPlaylist: () -> Unit,
    onOpen: (ChannelEntity) -> Unit
) {
    var showSettings by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = showSettings || (destination != AppDestination.Home && selectedMovieId == null && selectedSeriesId == null)) {
        showSettings = false
        onDestinationChange(AppDestination.Home)
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(CinematicColors.Canvas)) {
        val useRail = maxWidth >= 600.dp || maxWidth > maxHeight
        Row(Modifier.fillMaxSize()) {
            if (useRail) {
                StreamGuideNavigationRail(
                    selected = destination,
                    onSelected = { onDestinationChange(it); showSettings = false },
                    onSettings = { showSettings = true }
                )
            }
            Column(Modifier.weight(1f).fillMaxSize()) {
                StreamGuideHeader(
                    title = if (showSettings) "Instellingen" else destination.label,
                    onAddPlaylist = onAddPlaylist,
                    onSync = viewModel::syncAllPlaylists,
                    onSettings = { showSettings = !showSettings }
                )
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when {
                        showSettings -> SettingsScreen(state, viewModel, onAddPlaylist)
                        destination == AppDestination.Home -> CinematicHomeScreen(
                            state = state,
                            onOpen = onOpen,
                            onBrowseLive = { onDestinationChange(AppDestination.Live) },
                            onOpenMovie = { movie -> onDestinationChange(AppDestination.Movies); onMovieSelected(movie.id) },
                            onOpenSeries = { card -> onDestinationChange(AppDestination.Series); onSeriesSelected(card.series.id) }
                        )
                        destination == AppDestination.Live -> LiveTvScreen(
                            state = state,
                            onQueryChange = viewModel::updateQuery,
                            onShowAll = viewModel::showAllChannels,
                            onFavoritesOnly = { viewModel.setFavoritesOnly(true) },
                            onRecentOnly = { viewModel.setRecentOnly(true) },
                            onGroupSelected = viewModel::selectGroup,
                            onToggleFavorite = { viewModel.toggleFavorite(it.channel) },
                            onWatch = { onOpen(it.channel) },
                            onOpenGuide = { onDestinationChange(AppDestination.Guide) }
                        )
                        destination == AppDestination.Guide -> ProgramGuideScreen(
                            state = state,
                            onQueryChange = viewModel::updateGuideQuery,
                            onDaySelected = viewModel::selectGuideDay,
                            onToggleFavorite = viewModel::toggleFavorite,
                            onWatch = onOpen,
                            onOpenLive = { onDestinationChange(AppDestination.Live) }
                        )
                        destination == AppDestination.Movies -> MoviesScreen(
                            library = state.movieLibrary,
                            selectedMovieId = selectedMovieId,
                            onMovieSelected = onMovieSelected,
                            onQueryChange = viewModel::updateMovieQuery,
                            onCategorySelected = viewModel::selectMovieCategory,
                            onFiltersChanged = viewModel::updateMovieFilters,
                            onClearFilters = viewModel::clearMovieFilters,
                            onSortChanged = viewModel::updateMovieSort,
                            onToggleFavorite = viewModel::toggleMovieFavorite,
                            onLoadDetails = viewModel::loadMovieDetails,
                            onPlay = onPlayMovie,
                            onSetWatched = viewModel::setMovieWatched,
                            onGroupVisible = viewModel::setMovieGroupVisible,
                            onShowAllGroups = viewModel::showAllMovieGroups,
                            onHideAllGroups = viewModel::hideAllMovieGroups,
                            onRetry = viewModel::syncAllPlaylists
                        )
                        destination == AppDestination.Series -> SeriesScreen(
                            library = state.seriesLibrary, selectedSeriesId = selectedSeriesId, onSeriesSelected = onSeriesSelected,
                            onQueryChange = viewModel::updateSeriesQuery, onCategorySelected = viewModel::selectSeriesCategory,
                            onFiltersChanged = viewModel::updateSeriesFilters, onClearFilters = viewModel::clearSeriesFilters,
                            onSortChanged = viewModel::updateSeriesSort, onToggleFavorite = viewModel::toggleSeriesFavorite,
                            onLoadDetails = viewModel::loadSeriesDetails, onPlayEpisode = onPlayEpisode,
                            onSetEpisodeWatched = viewModel::setEpisodeWatched, onSetSeasonWatched = viewModel::setSeasonWatched,
                            onSetSeriesWatched = viewModel::setSeriesWatched, onClearProgress = viewModel::clearSeriesProgress,
                            onGroupVisible = viewModel::setSeriesGroupVisible,
                            onShowAllGroups = viewModel::showAllSeriesGroups,
                            onHideAllGroups = viewModel::hideAllSeriesGroups,
                            onRetry = viewModel::syncAllPlaylists
                        )
                    }
                }
                if (!useRail) {
                    StreamGuideBottomNavigation(
                        selected = destination,
                        onSelected = { onDestinationChange(it); showSettings = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamGuideHeader(
    title: String,
    onAddPlaylist: () -> Unit,
    onSync: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CinematicColors.CanvasTop.copy(alpha = 0.96f))
            .statusBarsPadding()
            .height(44.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(StreamGuideRadii.Control),
            color = CinematicColors.GoldMuted
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CinematicColors.Gold)
            }
        }
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Row {
                Text("StreamGuide", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                Text(" TV", color = CinematicColors.Gold, style = CinematicTypography.SectionTitle)
            }
            Text(title.uppercase(Locale.getDefault()), color = CinematicColors.TextMuted, style = CinematicTypography.Badge)
        }
        HeaderAction(Icons.Default.Add, "Playlist toevoegen", onAddPlaylist)
        HeaderAction(Icons.Default.Refresh, "Alles synchroniseren", onSync)
        HeaderAction(Icons.Default.Settings, "Instellingen", onSettings)
    }
}

@Composable
private fun HeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(28.dp)
            .clip(RoundedCornerShape(StreamGuideRadii.Control))
            .background(CinematicColors.PanelRaised.copy(alpha = 0.34f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = CinematicColors.TextSecondary, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun LibraryPlaceholder(title: String) {
    Box(Modifier.fillMaxSize().padding(StreamGuideSpacing.Xl), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm)) {
            Box(
                Modifier.size(44.dp)
                    .background(CinematicColors.Gold.copy(alpha = 0.10f), RoundedCornerShape(StreamGuideRadii.Card))
                    .border(1.dp, CinematicColors.Gold.copy(alpha = 0.28f), RoundedCornerShape(StreamGuideRadii.Card)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.LiveTv, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(24.dp))
            }
            Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
            Text("Deze bron bevat momenteel alleen live-zenders.", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        }
    }
}

@Composable
private fun SettingsScreen(state: StreamGuideState, viewModel: StreamGuideViewModel, onAddPlaylist: () -> Unit) {
    var showGroupFilter by remember { mutableStateOf(false) }
    val appUpdateState by viewModel.appUpdateState.collectAsStateWithLifecycle()
    val activity = LocalContext.current as? MainActivity
    val hiddenGroupCount = state.allGroups.count { group ->
        state.settings.hiddenGroups.any { it.equals(group, ignoreCase = true) }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 760.dp
        val pagePadding = if (wideLayout) StreamGuideSpacing.Xl else StreamGuideSpacing.Lg
        LazyColumn(
            contentPadding = PaddingValues(horizontal = pagePadding, vertical = StreamGuideSpacing.Md),
            verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md)
        ) {
            item { SettingsHeading() }
            item {
                SettingsSection("Playlists", Icons.Default.LiveTv, "Bronnen en programmagidsen") {
                    state.playlists.forEach { playlist ->
                        PlaylistSettingsRow(
                            name = playlist.name,
                            epgConfigured = !playlist.epgUrl.isNullOrBlank(),
                            onDelete = { viewModel.deletePlaylist(playlist.id) }
                        )
                    }
                    SettingsAction(
                        label = "Playlist toevoegen",
                        icon = Icons.Default.Add,
                        primary = true,
                        onClick = onAddPlaylist
                    )
                }
            }
            item {
                SettingsSectionPair(
                    wide = wideLayout,
                    first = {
                        SettingsSection("Synchronisatie", Icons.Default.Sync, "Automatisch en handmatig bijwerken") {
                            SettingsSwitch("Playlists bij starten", "Nieuwe en gewijzigde zenders ophalen", state.settings.syncPlaylistsOnStart, viewModel::setSyncPlaylistsOnStart)
                            SettingsSwitch("EPG bij starten", "De tv-gids automatisch verversen", state.settings.syncEpgOnStart, viewModel::setSyncEpgOnStart)
                            SettingsAction("Playlists synchroniseren", Icons.Default.Refresh, onClick = viewModel::syncAllPlaylists)
                            SettingsAction("EPG synchroniseren", Icons.Default.DateRange, onClick = viewModel::syncAllEpg)
                        }
                    },
                    second = {
                        SettingsSection("Zendergroepen", Icons.Default.FilterList, "Bepaal wat in lijsten zichtbaar is") {
                            SettingsSummary(
                                if (hiddenGroupCount == 0) "Alle groepen zijn zichtbaar" else "$hiddenGroupCount groepen verborgen"
                            )
                            SettingsAction("Groepen kiezen", Icons.Default.Tune, onClick = { showGroupFilter = true })
                        }
                    }
                )
            }
            item {
                SettingsSectionPair(
                    wide = wideLayout,
                    first = {
                        SettingsSection("Speler", Icons.Default.PlayArrow, "Afspeelgedrag en bediening") {
                            SettingsSwitch("Afspeelpositie hervatten", "Onthoud niet-live media", state.settings.autoResumeLastChannel, viewModel::setAutoResumeLastChannel)
                            SettingsSwitch("Hardware decoding", "Gebruik de videodecoder van het toestel", state.settings.hardwareDecoding, viewModel::setHardwareDecoding)
                            SettingsSwitch("Spelergebaren", "Dubbel tikken, helderheid en volume", state.settings.playerGesturesEnabled, viewModel::setPlayerGesturesEnabled)
                            SettingsSwitch("Volgende aflevering", "Speel automatisch verder na een aflevering", state.settings.autoPlayNextEpisode, viewModel::setAutoPlayNextEpisode)
                        }
                    },
                    second = {
                        SettingsSection("Weergave", Icons.Default.Visibility, "Dichtheid en zenderinformatie") {
                            SettingsSwitch("Logo's tonen", "Toon zenderlogo's in lijsten", state.settings.showLogos, viewModel::setShowLogos)
                            SettingsSwitch("Compacte zenderlijst", "Meer zenders tegelijk in beeld", state.settings.compactList, viewModel::setCompactList)
                        }
                    }
                )
            }
            item {
                SettingsSectionPair(
                    wide = wideLayout,
                    first = {
                        SettingsSection("App-updates", Icons.Default.SystemUpdate, "Automatisch via GitHub Releases") {
                            SettingsSummary(appUpdateStatusText(appUpdateState))
                            when (val update = appUpdateState) {
                                is AppUpdateState.Available -> SettingsAction(
                                    "Versie ${update.release.versionName} downloaden",
                                    Icons.Default.Download,
                                    primary = true,
                                    onClick = viewModel::downloadAppUpdate
                                )
                                is AppUpdateState.Downloaded -> SettingsAction(
                                    "Update installeren",
                                    Icons.Default.SystemUpdate,
                                    primary = true,
                                    onClick = { activity?.requestUpdateInstallation(update.file) }
                                )
                                is AppUpdateState.Downloading -> Unit
                                else -> SettingsAction(
                                    "Nu controleren",
                                    Icons.Default.Refresh,
                                    onClick = { viewModel.checkForAppUpdate(manual = true) }
                                )
                            }
                        }
                    },
                    second = {
                        SettingsSection("Over StreamGuide", Icons.Default.Info, "Versie ${BuildConfig.VERSION_NAME}") {
                            Text(
                                "StreamGuide Mobile levert geen zenders, streams of playlists mee. Gebruik alleen bronnen waarvoor je toestemming hebt.",
                                color = CinematicColors.TextSecondary,
                                style = CinematicTypography.Body
                            )
                        }
                    }
                )
            }
            item { StatusBlock(state) }
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

private fun appUpdateStatusText(state: AppUpdateState): String = when (state) {
    AppUpdateState.Idle -> "Geinstalleerde versie: ${BuildConfig.VERSION_NAME}"
    is AppUpdateState.Checking -> "GitHub wordt gecontroleerd..."
    AppUpdateState.UpToDate -> "Je gebruikt de nieuwste versie (${BuildConfig.VERSION_NAME})"
    is AppUpdateState.Available -> "Nieuwe versie ${state.release.versionName} is beschikbaar"
    is AppUpdateState.Downloading -> "Update downloaden: ${state.progress}%"
    is AppUpdateState.Downloaded -> "Versie ${state.release.versionName} is klaar voor installatie"
    is AppUpdateState.Error -> state.message
}

@Composable
private fun AppUpdateDialog(
    state: AppUpdateState,
    onDownload: () -> Unit,
    onInstall: (File) -> Unit,
    onDismiss: () -> Unit
) {
    val release = when (state) {
        is AppUpdateState.Available -> state.release
        is AppUpdateState.Downloading -> state.release
        is AppUpdateState.Downloaded -> state.release
        else -> return
    }
    val downloading = state is AppUpdateState.Downloading
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        icon = {
            Box(
                Modifier.size(42.dp)
                    .background(CinematicColors.GoldMuted, RoundedCornerShape(StreamGuideRadii.Control))
                    .border(1.dp, CinematicColors.Gold.copy(alpha = 0.38f), RoundedCornerShape(StreamGuideRadii.Control)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(22.dp))
            }
        },
        title = { Text("Nieuwe StreamGuide-versie", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md)) {
                UpdateVersionRow(release)
                when (state) {
                    is AppUpdateState.Downloading -> {
                        Text("De APK wordt veilig van GitHub gedownload.", color = CinematicColors.TextSecondary, style = CinematicTypography.Body)
                        LinearProgressIndicator(
                            progress = { state.progress / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                            color = CinematicColors.Gold,
                            trackColor = CinematicColors.PanelPressed
                        )
                        Text("${state.progress}%", color = CinematicColors.GoldBright, style = CinematicTypography.Metadata)
                    }
                    is AppUpdateState.Downloaded -> Text(
                        "De download is klaar. Android vraagt hierna nog om jouw toestemming voor de installatie.",
                        color = CinematicColors.TextSecondary,
                        style = CinematicTypography.Body
                    )
                    else -> {
                        Text("Deze update is beschikbaar via de officiele StreamGuide-repository.", color = CinematicColors.TextSecondary, style = CinematicTypography.Body)
                        if (release.notes.isNotBlank()) {
                            Text(
                                release.notes,
                                modifier = Modifier.heightIn(max = 150.dp).verticalScroll(rememberScrollState()),
                                color = CinematicColors.TextMuted,
                                style = CinematicTypography.Metadata
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            when (state) {
                is AppUpdateState.Available -> SettingsAction("Downloaden", Icons.Default.Download, modifier = Modifier.width(132.dp), primary = true, onClick = onDownload)
                is AppUpdateState.Downloaded -> SettingsAction("Installeren", Icons.Default.SystemUpdate, modifier = Modifier.width(132.dp), primary = true, onClick = { onInstall(state.file) })
                else -> Unit
            }
        },
        dismissButton = {
            if (!downloading) TextButton(onClick = onDismiss) { Text("Later", color = CinematicColors.TextSecondary) }
        },
        containerColor = CinematicColors.PanelRaised,
        tonalElevation = 0.dp
    )
}

@Composable
private fun UpdateVersionRow(release: AppRelease) {
    Row(
        Modifier.fillMaxWidth()
            .background(CinematicColors.Panel.copy(alpha = 0.72f), RoundedCornerShape(StreamGuideRadii.Control))
            .border(1.dp, CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Control))
            .padding(horizontal = StreamGuideSpacing.Md, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm)
    ) {
        Text("v${BuildConfig.VERSION_NAME}", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        Text("->", color = CinematicColors.Gold, style = CinematicTypography.CardTitle)
        Text("v${release.versionName}", color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle)
        Spacer(Modifier.weight(1f))
        if (release.assetSizeBytes > 0L) {
            Text("${release.assetSizeBytes / 1_048_576L} MB", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        }
    }
}

@Composable
private fun SettingsHeading() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md)) {
        Box(
            Modifier.size(38.dp)
                .background(CinematicColors.Gold.copy(alpha = 0.12f), RoundedCornerShape(StreamGuideRadii.Card))
                .border(1.dp, CinematicColors.Gold.copy(alpha = 0.30f), RoundedCornerShape(StreamGuideRadii.Card)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Settings, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(20.dp))
        }
        Column {
            Text("Instellingen", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
            Text("Bronnen, gids, speler en weergave", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        }
    }
}

@Composable
private fun SettingsSectionPair(wide: Boolean, first: @Composable () -> Unit, second: @Composable () -> Unit) {
    if (wide) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) { first() }
            Box(Modifier.weight(1f)) { second() }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md)) {
            first()
            second()
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(StreamGuideRadii.Card),
        border = BorderStroke(1.dp, CinematicColors.Border),
        colors = CardDefaults.cardColors(containerColor = CinematicColors.PanelRaised.copy(alpha = 0.82f))
    ) {
        Column(Modifier.padding(StreamGuideSpacing.Md), verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(30.dp).background(CinematicColors.GoldMuted, RoundedCornerShape(StreamGuideRadii.Control)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(16.dp))
                }
                Column(Modifier.padding(start = StreamGuideSpacing.Sm)) {
                    Text(title, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                    subtitle?.let { Text(it, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata) }
                }
            }
            content()
        }
    }
}

@Composable
private fun SettingsSwitch(label: String, supportingText: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (focused) CinematicColors.Gold.copy(alpha = 0.70f) else CinematicColors.Border,
        tween(StreamGuideMotion.Quick),
        label = "settings-toggle-border"
    )
    Row(
        Modifier.fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(StreamGuideRadii.Control))
            .background(if (focused) CinematicColors.PanelPressed.copy(alpha = 0.72f) else CinematicColors.Panel.copy(alpha = 0.54f))
            .border(1.dp, borderColor, RoundedCornerShape(StreamGuideRadii.Control))
            .clickable { onChanged(!checked) }
            .padding(horizontal = StreamGuideSpacing.Md, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(label, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle)
            Text(supportingText, color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        }
        SettingsToggleIndicator(checked)
    }
}

@Composable
private fun SettingsToggleIndicator(checked: Boolean) {
    val trackColor by animateColorAsState(
        if (checked) CinematicColors.Gold else CinematicColors.PanelPressed,
        tween(StreamGuideMotion.Quick),
        label = "settings-toggle-track"
    )
    val thumbOffset by animateDpAsState(if (checked) 18.dp else 3.dp, tween(StreamGuideMotion.Quick), label = "settings-toggle-thumb")
    Box(Modifier.width(36.dp).height(20.dp).background(trackColor, CircleShape)) {
        Box(
            Modifier.offset(x = thumbOffset, y = 3.dp).size(14.dp)
                .background(if (checked) MaterialTheme.colorScheme.onPrimary else CinematicColors.TextSecondary, CircleShape)
        )
    }
}

@Composable
private fun SettingsAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier.fillMaxWidth(),
    primary: Boolean = false,
    onClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    val borderColor by animateColorAsState(
        when {
            focused -> CinematicColors.GoldBright
            primary -> CinematicColors.Gold.copy(alpha = 0.75f)
            else -> CinematicColors.BorderStrong
        },
        tween(StreamGuideMotion.Quick),
        label = "settings-action-border"
    )
    val background = if (primary) CinematicColors.Gold else if (focused) CinematicColors.PanelPressed else CinematicColors.Panel
    val contentColor = if (primary) MaterialTheme.colorScheme.onPrimary else if (focused) CinematicColors.GoldBright else CinematicColors.TextSecondary
    Row(
        modifier.onFocusChanged { focused = it.isFocused }.height(36.dp).clip(shape)
            .background(background).border(1.dp, borderColor, shape).clickable(onClick = onClick)
            .padding(horizontal = StreamGuideSpacing.Md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm)
    ) {
        Icon(icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(16.dp))
        Text(label, color = contentColor, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun PlaylistSettingsRow(name: String, epgConfigured: Boolean, onDelete: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(name, color = CinematicColors.TextPrimary, style = CinematicTypography.CardTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (epgConfigured) "EPG ingesteld" else "EPG via bron of niet ingesteld", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
        }
        SettingsIconAction(Icons.Default.Delete, "Playlist verwijderen", onDelete)
    }
}

@Composable
private fun SettingsIconAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(StreamGuideRadii.Control)
    Box(
        Modifier.size(32.dp).onFocusChanged { focused = it.isFocused }.clip(shape)
            .background(if (focused) MaterialTheme.colorScheme.error.copy(alpha = 0.14f) else CinematicColors.Panel)
            .border(1.dp, if (focused) MaterialTheme.colorScheme.error.copy(alpha = 0.70f) else CinematicColors.Border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = description, tint = if (focused) MaterialTheme.colorScheme.error else CinematicColors.TextMuted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun SettingsSummary(text: String) {
    Text(
        text,
        modifier = Modifier.fillMaxWidth().background(CinematicColors.Panel.copy(alpha = 0.54f), RoundedCornerShape(StreamGuideRadii.Control))
            .border(1.dp, CinematicColors.Border, RoundedCornerShape(StreamGuideRadii.Control))
            .padding(horizontal = StreamGuideSpacing.Md, vertical = 10.dp),
        color = CinematicColors.TextSecondary,
        style = CinematicTypography.Body
    )
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
        icon = {
            Box(
                Modifier.size(36.dp).background(CinematicColors.GoldMuted, RoundedCornerShape(StreamGuideRadii.Control)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null, tint = CinematicColors.Gold, modifier = Modifier.size(18.dp))
            }
        },
        title = { Text("Zendergroepen", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle) },
        text = {
            if (groups.isEmpty()) {
                Text("Er zijn nog geen zendergroepen gevonden.", color = CinematicColors.TextMuted, style = CinematicTypography.Body)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Xs)) {
                    items(groups, key = { it }) { group ->
                        val visible = hiddenGroups.none { it.equals(group, ignoreCase = true) }
                        SettingsSwitch(group, if (visible) "Zichtbaar" else "Verborgen", visible) {
                            onGroupVisible(group, it)
                        }
                    }
                }
            }
        },
        confirmButton = {
            SettingsAction("Klaar", Icons.Default.Done, modifier = Modifier.width(94.dp), primary = true, onClick = onDismiss)
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Xs)) {
                SettingsAction("Alles tonen", Icons.Default.Visibility, modifier = Modifier.width(112.dp), onClick = onShowAll)
                SettingsAction("Verbergen", Icons.Default.VisibilityOff, modifier = Modifier.width(112.dp), onClick = onHideAll)
            }
        },
        shape = RoundedCornerShape(StreamGuideRadii.Hero),
        containerColor = CinematicColors.Panel,
        iconContentColor = CinematicColors.Gold,
        titleContentColor = CinematicColors.TextPrimary,
        textContentColor = CinematicColors.TextSecondary,
        tonalElevation = 0.dp
    )
}
@Composable
private fun StatusBlock(state: StreamGuideState) {
    if (state.isLoading || state.message != null || state.error != null) {
        val borderColor = if (state.error != null) MaterialTheme.colorScheme.error.copy(alpha = 0.48f) else CinematicColors.Border
        Row(
            Modifier.fillMaxWidth().padding(horizontal = StreamGuideSpacing.Lg, vertical = StreamGuideSpacing.Sm)
                .background(CinematicColors.PanelRaised.copy(alpha = 0.88f), RoundedCornerShape(StreamGuideRadii.Control))
                .border(1.dp, borderColor, RoundedCornerShape(StreamGuideRadii.Control))
                .padding(horizontal = StreamGuideSpacing.Md, vertical = StreamGuideSpacing.Sm),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Sm)
        ) {
            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CinematicColors.Gold)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                state.message?.let { Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Metadata) }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = CinematicTypography.Metadata) }
            }
        }
    }
}

@Preview(name = "Settings phone", widthDp = 411, heightDp = 891, showBackground = true, backgroundColor = 0xFF04070B)
@Composable
private fun SettingsPhonePreview() {
    SettingsVisualPreview()
}

@Preview(name = "Settings tablet", widthDp = 1280, heightDp = 800, showBackground = true, backgroundColor = 0xFF04070B)
@Composable
private fun SettingsTabletPreview() {
    SettingsVisualPreview()
}

@Composable
private fun SettingsVisualPreview() {
    StreamGuideTheme {
        BoxWithConstraints(Modifier.fillMaxSize().background(CinematicColors.Canvas)) {
            val wide = maxWidth >= 760.dp
            val padding = if (wide) StreamGuideSpacing.Xl else StreamGuideSpacing.Lg
            LazyColumn(
                contentPadding = PaddingValues(padding),
                verticalArrangement = Arrangement.spacedBy(StreamGuideSpacing.Md)
            ) {
                item { SettingsHeading() }
                item {
                    SettingsSection("Playlists", Icons.Default.LiveTv, "Bronnen en programmagidsen") {
                        PlaylistSettingsRow("Mijn IPTV", epgConfigured = true, onDelete = {})
                        SettingsAction("Playlist toevoegen", Icons.Default.Add, primary = true, onClick = {})
                    }
                }
                item {
                    SettingsSectionPair(
                        wide = wide,
                        first = {
                            SettingsSection("Synchronisatie", Icons.Default.Sync, "Automatisch en handmatig bijwerken") {
                                SettingsSwitch("Playlists bij starten", "Nieuwe en gewijzigde zenders ophalen", true) {}
                                SettingsSwitch("EPG bij starten", "De tv-gids automatisch verversen", true) {}
                                SettingsAction("Playlists synchroniseren", Icons.Default.Refresh, onClick = {})
                            }
                        },
                        second = {
                            SettingsSection("Speler", Icons.Default.PlayArrow, "Afspeelgedrag en bediening") {
                                SettingsSwitch("Afspeelpositie hervatten", "Onthoud niet-live media", true) {}
                                SettingsSwitch("Hardware decoding", "Gebruik de videodecoder van het toestel", true) {}
                                SettingsSwitch("Spelergebaren", "Dubbel tikken, helderheid en volume", false) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

private enum class SourceMode { Xtream, M3u }
