package com.example.streamguidemobile.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.SystemClock
import android.util.Rational
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.streamguidemobile.MainActivity
import com.example.streamguidemobile.StreamGuideState
import com.example.streamguidemobile.StreamGuideViewModel
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.MovieEntity
import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.SeriesEntity
import com.example.streamguidemobile.data.displayCode
import com.example.streamguidemobile.data.qualityBadge
import com.example.streamguidemobile.data.streamResolutionBadge
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PremiumPlayerScreen(
    channel: ChannelEntity,
    state: StreamGuideState,
    viewModel: StreamGuideViewModel,
    onBack: () -> Unit,
    onOpenChannel: (ChannelEntity) -> Unit,
    movie: MovieEntity? = null,
    episode: EpisodeEntity? = null,
    series: SeriesEntity? = null,
    nextEpisode: EpisodeEntity? = null,
    onPlayNextEpisode: (EpisodeEntity) -> Unit = {},
    startFromBeginning: Boolean = false
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val scope = rememberCoroutineScope()
    val row = state.homeRows.firstOrNull { it.channel.id == channel.id }
        ?: state.channelRows.firstOrNull { it.channel.id == channel.id }
    val currentChannel = row?.channel ?: channel
    val isMedia = movie != null || episode != null

    var controlsVisible by remember(channel.id) { mutableStateOf(true) }
    var activeSheet by remember(channel.id) { mutableStateOf<PlayerSheet?>(null) }
    var isPlaying by remember(channel.id) { mutableStateOf(true) }
    var playbackState by remember(channel.id) { mutableIntStateOf(Player.STATE_IDLE) }
    var playerFailure by remember(channel.id) { mutableStateOf<PlayerFailure?>(null) }
    var resizeMode by remember(channel.id) { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }
    var streamQuality by remember(channel.id) { mutableStateOf<String?>(null) }
    var measuredVideoSize by remember(channel.id) {
        mutableStateOf((movie?.resolutionWidth ?: episode?.resolutionWidth ?: 0) to (movie?.resolutionHeight ?: episode?.resolutionHeight ?: 0))
    }
    var tracks by remember(channel.id) { mutableStateOf(Tracks.EMPTY) }
    var position by remember(channel.id) { mutableLongStateOf(0L) }
    var duration by remember(channel.id) { mutableLongStateOf(0L) }
    var bufferedPosition by remember(channel.id) { mutableLongStateOf(0L) }
    var seekable by remember(channel.id) { mutableStateOf(false) }
    var interactionNonce by remember { mutableIntStateOf(0) }
    var scheduleExpanded by remember(channel.id) { mutableStateOf(false) }
    var sleepDeadline by remember(channel.id) { mutableStateOf<Long?>(null) }
    var gestureIndicator by remember { mutableStateOf<Pair<String, Float>?>(null) }
    var lastChannelSwitchAt by remember { mutableLongStateOf(0L) }
    var upNextCancelled by remember(channel.id) { mutableStateOf(false) }

    val player = remember(channel.id) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(SEEK_STEP_MS)
            .setSeekForwardIncrementMs(SEEK_STEP_MS)
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(channel.streamUrl))
                prepare()
                playWhenReady = true
            }
    }

    fun revealControls() {
        controlsVisible = true
        interactionNonce += 1
    }

    fun togglePlayback() {
        revealControls()
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(offset: Long) {
        if (!seekable) return
        revealControls()
        player.seekTo((player.currentPosition + offset).coerceIn(0L, duration.coerceAtLeast(0L)))
    }

    fun switchChannel(direction: Int) {
        if (isMedia) return
        val timestamp = SystemClock.elapsedRealtime()
        if (timestamp - lastChannelSwitchAt < CHANNEL_SWITCH_DEBOUNCE_MS) return
        val visibleChannels = state.homeRows.map { it.channel }.ifEmpty { state.channels }
        val currentIndex = visibleChannels.indexOfFirst { it.id == currentChannel.id }
        if (currentIndex < 0 || visibleChannels.isEmpty()) return
        lastChannelSwitchAt = timestamp
        val nextIndex = (currentIndex + direction + visibleChannels.size) % visibleChannels.size
        onOpenChannel(visibleChannels[nextIndex])
    }

    val pipSupported = activity?.packageManager?.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) == true
    val enterPip = remember(activity, player, pipSupported) {
        {
            val playerActivity = activity
            if (playerActivity != null && pipSupported && !playerActivity.isInPictureInPictureMode) {
                runCatching {
                    playerActivity.enterPictureInPictureMode(
                        PictureInPictureParams.Builder().setAspectRatio(player.videoSize.pipAspectRatio()).build()
                    )
                }
            }
        }
    }

    val externalIntent = remember(channel.streamUrl) {
        Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(channel.streamUrl), "video/*")
    }
    val canOpenExternal = remember(externalIntent) {
        externalIntent.resolveActivity(context.packageManager) != null
    }
    fun openExternal() {
        runCatching { context.startActivity(externalIntent) }
    }

    DisposableEffect(activity) {
        val previousOrientation = activity?.requestedOrientation
        val controller = activity?.window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        val systemBars = WindowInsetsCompat.Type.systemBars()
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(systemBars)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            controller?.show(systemBars)
            if (previousOrientation != null) activity?.requestedOrientation = previousOrientation
        }
    }

    DisposableEffect(activity, enterPip) {
        (activity as? MainActivity)?.setPictureInPictureCallbacks(
            enter = enterPip,
            onModeChanged = { isInPip -> controlsVisible = !isInPip }
        )
        onDispose { (activity as? MainActivity)?.setPictureInPictureCallbacks(null, null) }
    }

    DisposableEffect(player, channel.id, state.settings.autoResumeLastChannel) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing) controlsVisible = true
            }

            override fun onPlaybackStateChanged(stateValue: Int) {
                playbackState = stateValue
                if (stateValue == Player.STATE_READY) {
                    val videoSize = player.videoSize
                    if (videoSize.width > 0 && videoSize.height > 0) measuredVideoSize = videoSize.width to videoSize.height
                    streamQuality = measuredQualityLabel(videoSize)
                    if (movie != null) viewModel.updateMovieResolution(movie.id, videoSize.width, videoSize.height)
                    else if (episode != null) viewModel.updateEpisodeResolution(episode.id, videoSize.width, videoSize.height)
                    else viewModel.updateStreamResolution(channel, videoSize.width, videoSize.height)
                    playerFailure = null
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) measuredVideoSize = videoSize.width to videoSize.height
                streamQuality = measuredQualityLabel(videoSize)
                if (movie != null) viewModel.updateMovieResolution(movie.id, videoSize.width, videoSize.height)
                else if (episode != null) viewModel.updateEpisodeResolution(episode.id, videoSize.width, videoSize.height)
                else viewModel.updateStreamResolution(channel, videoSize.width, videoSize.height)
            }

            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
            }

            override fun onPlayerError(error: PlaybackException) {
                playerFailure = classifyPlayerFailure(error.errorCodeName)
                controlsVisible = true
            }
        }
        player.addListener(listener)
        onDispose {
            if (isMedia || state.settings.autoResumeLastChannel) {
                val savedDuration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
                if (movie != null) viewModel.saveMovieProgress(movie.id, player.currentPosition.coerceAtLeast(0L), savedDuration)
                else if (episode != null) viewModel.saveEpisodeProgress(episode.id, player.currentPosition.coerceAtLeast(0L), savedDuration)
                else viewModel.savePlaybackPosition(channel, player.currentPosition.coerceAtLeast(0L), savedDuration, player.isCurrentMediaItemLive)
            }
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, channel.id, state.settings.autoResumeLastChannel) {
        if (!isMedia && !state.settings.autoResumeLastChannel) return@LaunchedEffect
        val savedPosition = when {
            startFromBeginning -> 0L
            movie != null -> movie.playbackPositionMs
            episode != null -> episode.playbackPositionMs
            else -> viewModel.playbackPosition(channel)
        }
        if (savedPosition <= 0L) return@LaunchedEffect
        var attempts = 0
        while (isActive && attempts < 120) {
            val mediaDuration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
            if (mediaDuration > 0L) {
                if (!player.isCurrentMediaItemLive &&
                    player.isCurrentMediaItemSeekable &&
                    savedPosition < mediaDuration - 15_000L
                ) {
                    player.seekTo(savedPosition)
                }
                break
            }
            attempts += 1
            delay(250L)
        }
    }

    LaunchedEffect(player, channel.id, state.settings.autoResumeLastChannel) {
        if (!isMedia && !state.settings.autoResumeLastChannel) return@LaunchedEffect
        while (isActive) {
            delay(5_000L)
            val savedDuration = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            if (movie != null) viewModel.saveMovieProgress(movie.id, player.currentPosition.coerceAtLeast(0L), savedDuration)
            else if (episode != null) viewModel.saveEpisodeProgress(episode.id, player.currentPosition.coerceAtLeast(0L), savedDuration)
            else viewModel.savePlaybackPosition(channel, player.currentPosition.coerceAtLeast(0L), savedDuration, player.isCurrentMediaItemLive)
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            position = player.currentPosition.coerceAtLeast(0L)
            duration = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
            seekable = player.isCurrentMediaItemSeekable && duration > 0L
            delay(500L)
        }
    }

    val upNextVisible = episode != null && nextEpisode != null && !upNextCancelled && duration > 0L && duration - position in 0L..30_000L
    val upNextCountdown = ((duration - position).coerceAtLeast(0L) / 1_000L).toInt()
    LaunchedEffect(playbackState, nextEpisode, upNextCancelled, state.settings.autoPlayNextEpisode) {
        if (playbackState == Player.STATE_ENDED && nextEpisode != null && !upNextCancelled && state.settings.autoPlayNextEpisode) {
            onPlayNextEpisode(nextEpisode)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying, activeSheet, playerFailure, interactionNonce) {
        if (controlsVisible && isPlaying && activeSheet == null && playerFailure == null) {
            delay(CONTROLS_TIMEOUT_MS)
            controlsVisible = false
        }
    }

    LaunchedEffect(sleepDeadline) {
        val deadline = sleepDeadline ?: return@LaunchedEffect
        delay((deadline - System.currentTimeMillis()).coerceAtLeast(0L))
        player.pause()
        sleepDeadline = null
        controlsVisible = true
    }

    LaunchedEffect(gestureIndicator) {
        if (gestureIndicator != null) {
            delay(900L)
            gestureIndicator = null
        }
    }

    LaunchedEffect(channel.id) {
        controlsVisible = true
        interactionNonce += 1
    }

    BackHandler {
        when {
            activeSheet != null -> activeSheet = null
            playerFailure != null -> onBack()
            controlsVisible -> controlsVisible = false
            else -> onBack()
        }
    }

    val audioTracks = remember(tracks) { tracks.options(C.TRACK_TYPE_AUDIO) }
    val subtitleTracks = remember(tracks) { tracks.options(C.TRACK_TYPE_TEXT) }
    val videoTracks = remember(tracks) { tracks.options(C.TRACK_TYPE_VIDEO).distinctBy { Triple(it.format.width, it.format.height, it.format.bitrate) } }
    val selectedVideoFormat = remember(tracks) { tracks.selectedFormat(C.TRACK_TYPE_VIDEO) }
    val selectedAudioFormat = remember(tracks) { tracks.selectedFormat(C.TRACK_TYPE_AUDIO) }
    val technicalInfo = remember(movie?.containerExtension, episode?.containerExtension, selectedVideoFormat, selectedAudioFormat, measuredVideoSize) {
        buildPlayerTechnicalInfo(
            containerExtension = movie?.containerExtension ?: episode?.containerExtension,
            videoFormat = selectedVideoFormat,
            audioFormat = selectedAudioFormat,
            measuredWidth = measuredVideoSize.first,
            measuredHeight = measuredVideoSize.second
        )
    }
    val hasAudioMenu = audioTracks.size > 1
    val hasSubtitleMenu = subtitleTracks.isNotEmpty()
    val hasQualityMenu = videoTracks.size > 1

    fun selectTrack(option: PlayerTrackOption, type: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(option.group.mediaTrackGroup, option.index))
            .build()
        activeSheet = null
        revealControls()
    }

    fun selectAutomatic(type: Int) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(type)
            .setTrackTypeDisabled(type, false)
            .build()
        activeSheet = null
        revealControls()
    }

    fun retry() {
        playerFailure = null
        playbackState = Player.STATE_BUFFERING
        revealControls()
        player.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        player.prepare()
        player.playWhenReady = true
    }

    val gestureModifier = Modifier.playerTouchGestures(
        enabled = state.settings.playerGesturesEnabled,
        seekable = seekable,
        activity = activity,
        audioManager = audioManager,
        onTap = { controlsVisible = !controlsVisible; interactionNonce += 1 },
        onSeekBack = { seekBy(-SEEK_STEP_MS) },
        onSeekForward = { seekBy(SEEK_STEP_MS) },
        onGestureIndicator = { label, progressValue -> gestureIndicator = label to progressValue }
    )

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.MediaPlayPause, Key.Enter, Key.DirectionCenter -> { togglePlayback(); true }
                    Key.MediaPlay -> { player.play(); revealControls(); true }
                    Key.MediaPause -> { player.pause(); revealControls(); true }
                    Key.DirectionLeft -> { if (seekable) seekBy(-SEEK_STEP_MS) else revealControls(); true }
                    Key.DirectionRight -> { if (seekable) seekBy(SEEK_STEP_MS) else revealControls(); true }
                    Key.DirectionUp -> { if (!isMedia) switchChannel(-1) else revealControls(); true }
                    Key.DirectionDown -> { if (!isMedia) switchChannel(1) else revealControls(); true }
                    else -> false
                }
            }
    ) {
        val wideLayout = maxWidth >= 700.dp
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(it).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    keepScreenOn = true
                    this.player = player
                    this.resizeMode = resizeMode
                }
            },
            update = {
                it.player = player
                it.resizeMode = resizeMode
                it.keepScreenOn = true
            }
        )

        Box(gestureModifier.fillMaxSize())

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(tween(220)),
            exit = fadeOut(tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                PlayerOverlayScrims(compact = !wideLayout)

                PremiumPlayerTopBar(
                    channel = currentChannel,
                    program = row?.currentProgram,
                    quality = streamQuality ?: movie?.qualityBadge() ?: episode?.qualityBadge() ?: streamResolutionBadge(row?.streamResolution),
                    showLogos = state.settings.showLogos,
                    isFavorite = movie?.isFavorite ?: series?.isFavorite ?: currentChannel.isFavorite,
                    onBack = onBack,
                    onFavorite = {
                        if (movie != null) viewModel.toggleMovieFavorite(movie) else if (series != null) viewModel.toggleSeriesFavorite(series) else viewModel.toggleFavorite(currentChannel)
                        revealControls()
                    },
                    onMore = { activeSheet = PlayerSheet.More; revealControls() },
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                PremiumCenterControls(
                    isPlaying = isPlaying,
                    seekable = seekable,
                    onSeekBack = { seekBy(-SEEK_STEP_MS) },
                    onPlayPause = ::togglePlayback,
                    onSeekForward = { seekBy(SEEK_STEP_MS) },
                    seekBackIcon = Icons.Default.Replay10,
                    playIcon = Icons.Default.PlayArrow,
                    pauseIcon = Icons.Default.Pause,
                    seekForwardIcon = Icons.Default.Forward10,
                    modifier = Modifier.align(Alignment.Center)
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(if (wideLayout) 0.84f else 0.94f)
                        .padding(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (seekable) {
                        SeekableTimeline(
                            position = position,
                            duration = duration,
                            bufferedPosition = bufferedPosition,
                            onSeek = { player.seekTo(it); revealControls() },
                            onInteraction = ::revealControls
                        )
                    } else {
                        LiveProgramTimeline(row?.currentProgram, state.nowMillis)
                    }

                    val actions = buildList {
                        if (row?.currentProgram != null || row?.upcomingPrograms?.isNotEmpty() == true) {
                            add(PlayerAction("Info", Icons.Default.Info) { activeSheet = PlayerSheet.ProgramInfo; revealControls() })
                        }
                        if (hasSubtitleMenu) add(PlayerAction("Ondertitels", Icons.Default.ClosedCaption) { activeSheet = PlayerSheet.Subtitles; revealControls() })
                        if (hasAudioMenu) add(PlayerAction("Audio", Icons.Default.Audiotrack) { activeSheet = PlayerSheet.Audio; revealControls() })
                        if (hasQualityMenu) add(PlayerAction("Kwaliteit", Icons.Default.HighQuality) { activeSheet = PlayerSheet.Quality; revealControls() })
                        add(PlayerAction("Beeld", Icons.Default.AspectRatio) { activeSheet = PlayerSheet.AspectRatio; revealControls() })
                        add(PlayerAction("Slaap", Icons.Default.Bedtime, selected = sleepDeadline != null) { activeSheet = PlayerSheet.SleepTimer; revealControls() })
                        add(PlayerAction("Meer", Icons.Default.MoreHoriz) { activeSheet = PlayerSheet.More; revealControls() })
                    }
                    PlayerActionBar(actions, Modifier.align(Alignment.CenterHorizontally))

                    if (!seekable) {
                        LiveScheduleStrip(
                            programs = row?.upcomingPrograms.orEmpty(),
                            expanded = scheduleExpanded,
                            onToggle = { scheduleExpanded = !scheduleExpanded; revealControls() },
                            modifier = Modifier.align(Alignment.CenterHorizontally).widthIn(max = 620.dp).fillMaxWidth()
                        )
                    }
                }
            }
        }

        if (playbackState == Player.STATE_BUFFERING && playerFailure == null) {
            Column(
                Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(color = CinematicColors.Gold, strokeWidth = 2.dp)
                Text("Stream laden", color = Color.White.copy(alpha = 0.72f), style = CinematicTypography.Metadata)
            }
        }

        gestureIndicator?.let { (label, value) ->
            GestureIndicator(
                label = label,
                progress = value,
                modifier = Modifier
                    .align(if (label == "Volume") Alignment.CenterEnd else Alignment.CenterStart)
                    .padding(horizontal = 28.dp)
            )
        }

        if (upNextVisible && nextEpisode != null && series != null) {
            MediaUpNextCard(
                title = nextEpisode.title,
                metadata = "${series.title} · ${nextEpisode.displayCode()}",
                countdownSeconds = upNextCountdown,
                artwork = null,
                artworkUrl = nextEpisode.imageUrl ?: series.backdropUrl ?: series.posterUrl,
                onPlay = { onPlayNextEpisode(nextEpisode) },
                onCancel = { upNextCancelled = true; revealControls() },
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.76f).padding(bottom = 14.dp)
            )
        }

        playerFailure?.let {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.64f)), contentAlignment = Alignment.Center) {
                PlayerErrorPanel(
                    failure = it,
                    canOpenExternal = canOpenExternal,
                    onRetry = ::retry,
                    onExternal = ::openExternal,
                    onBack = onBack
                )
            }
        }
    }

    when (activeSheet) {
        PlayerSheet.Audio -> PlayerSelectionSheet(
            title = "Audiotrack",
            items = buildList {
                val autoSelected = !player.trackSelectionParameters.hasOverride(C.TRACK_TYPE_AUDIO)
                add(PlayerSelectionItem("audio-auto", "Automatisch", selected = autoSelected) { selectAutomatic(C.TRACK_TYPE_AUDIO) })
                audioTracks.forEach { option ->
                    add(PlayerSelectionItem(option.key, audioTrackLabel(option.format), selected = option.selected) { selectTrack(option, C.TRACK_TYPE_AUDIO) })
                }
            },
            onDismiss = { activeSheet = null }
        )
        PlayerSheet.Subtitles -> PlayerSelectionSheet(
            title = "Ondertitels",
            items = buildList {
                val off = C.TRACK_TYPE_TEXT in player.trackSelectionParameters.disabledTrackTypes
                add(PlayerSelectionItem("subtitle-off", "Uit", selected = off) {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                        .build()
                    activeSheet = null
                })
                subtitleTracks.forEach { option ->
                    add(PlayerSelectionItem(option.key, subtitleTrackLabel(option.format), selected = !off && option.selected) { selectTrack(option, C.TRACK_TYPE_TEXT) })
                }
            },
            onDismiss = { activeSheet = null }
        )
        PlayerSheet.Quality -> PlayerSelectionSheet(
            title = "Beeldkwaliteit",
            items = buildList {
                val autoSelected = !player.trackSelectionParameters.hasOverride(C.TRACK_TYPE_VIDEO)
                add(PlayerSelectionItem("quality-auto", "Automatisch", "Past zich aan de verbinding aan", autoSelected) { selectAutomatic(C.TRACK_TYPE_VIDEO) })
                videoTracks.sortedByDescending { it.format.height }.forEach { option ->
                    add(PlayerSelectionItem(option.key, videoTrackLabel(option.format), selected = option.selected) { selectTrack(option, C.TRACK_TYPE_VIDEO) })
                }
            },
            onDismiss = { activeSheet = null }
        )
        PlayerSheet.AspectRatio -> PlayerSelectionSheet(
            title = "Beeldverhouding",
            items = listOf(
                PlayerSelectionItem("fit", "Passend", "Volledig beeld zonder afsnijden", resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FIT) { resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; activeSheet = null },
                PlayerSelectionItem("zoom", "Beeldvullend", "Vult het scherm en kan randen afsnijden", resizeMode == AspectRatioFrameLayout.RESIZE_MODE_ZOOM) { resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM; activeSheet = null },
                PlayerSelectionItem("stretch", "Uitrekken", "Rekt het beeld tot het schermformaat", resizeMode == AspectRatioFrameLayout.RESIZE_MODE_FILL) { resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL; activeSheet = null }
            ),
            onDismiss = { activeSheet = null }
        )
        PlayerSheet.SleepTimer -> PlayerSelectionSheet(
            title = "Slaaptimer",
            items = buildList {
                add(PlayerSelectionItem("sleep-off", "Uit", selected = sleepDeadline == null) {
                    sleepDeadline = null
                    activeSheet = null
                })
                listOf(15, 30, 60).forEach { minutes ->
                    add(PlayerSelectionItem("sleep-$minutes", "$minutes minuten", selected = false) {
                        sleepDeadline = System.currentTimeMillis() + minutes * 60_000L
                        activeSheet = null
                    })
                }
            },
            onDismiss = { activeSheet = null }
        )
        PlayerSheet.ProgramInfo -> PlayerProgramInfoSheet(
            channel = currentChannel,
            current = row?.currentProgram,
            upcoming = row?.upcomingPrograms.orEmpty(),
            onDismiss = { activeSheet = null }
        )
        PlayerSheet.More -> PlayerSelectionSheet(
            title = "Meer opties",
            items = buildList {
                if (isMedia) add(
                    PlayerSelectionItem(
                        "technical-info",
                        "Technische informatie",
                        "Resolutie, container, video en audio",
                        selected = false
                    ) { activeSheet = PlayerSheet.TechnicalInfo }
                )
                if (pipSupported) add(PlayerSelectionItem("pip", "Mini-speler", "Picture-in-Picture", selected = false) { activeSheet = null; enterPip() })
                if (canOpenExternal) add(PlayerSelectionItem("external", "Externe speler", "Open deze stream in een andere app", selected = false) { activeSheet = null; openExternal() })
            }.ifEmpty {
                listOf(PlayerSelectionItem("none", "Geen extra opties beschikbaar", selected = false) { activeSheet = null })
            },
            onDismiss = { activeSheet = null }
        )
        PlayerSheet.TechnicalInfo -> PlayerTechnicalInfoSheet(
            title = movie?.title ?: series?.title ?: "Technische informatie",
            rows = technicalInfo,
            onDismiss = { activeSheet = null }
        )
        null -> Unit
    }
}

private data class PlayerTrackOption(
    val group: Tracks.Group,
    val index: Int,
    val format: Format,
    val selected: Boolean
) {
    val key: String = "${group.mediaTrackGroup.id}:$index"
}

private fun Tracks.options(type: Int): List<PlayerTrackOption> = groups.flatMap { group ->
    if (group.type != type) return@flatMap emptyList()
    (0 until group.length).mapNotNull { index ->
        if (!group.isTrackSupported(index)) null
        else PlayerTrackOption(group, index, group.getTrackFormat(index), group.isTrackSelected(index))
    }
}

private fun Tracks.selectedFormat(type: Int): Format? = groups.firstNotNullOfOrNull { group ->
    if (group.type != type) return@firstNotNullOfOrNull null
    (0 until group.length).firstOrNull(group::isTrackSelected)?.let(group::getTrackFormat)
}

private fun androidx.media3.common.TrackSelectionParameters.hasOverride(type: Int): Boolean =
    overrides.values.any { it.type == type }

private fun Modifier.playerTouchGestures(
    enabled: Boolean,
    seekable: Boolean,
    activity: Activity?,
    audioManager: AudioManager,
    onTap: () -> Unit,
    onSeekBack: () -> Unit,
    onSeekForward: () -> Unit,
    onGestureIndicator: (String, Float) -> Unit
): Modifier {
    val taps = this.pointerInput(enabled, seekable) {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { offset ->
                if (enabled && seekable) {
                    if (offset.x < size.width / 2f) onSeekBack() else onSeekForward()
                } else onTap()
            }
        )
    }
    if (!enabled) return taps
    return taps.pointerInput(activity, audioManager) {
        var leftSide = true
        var accumulated = 0f
        var startValue = 0.5f
        detectVerticalDragGestures(
            onDragStart = { offset ->
                leftSide = offset.x < size.width / 2f
                accumulated = 0f
                startValue = if (leftSide) {
                    activity?.window?.attributes?.screenBrightness?.takeIf { it >= 0f } ?: 0.5f
                } else {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
                }
            },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                accumulated -= dragAmount / size.height.coerceAtLeast(1)
                val value = (startValue + accumulated).coerceIn(0.02f, 1f)
                if (leftSide) {
                    activity?.window?.let { window ->
                        val attributes = window.attributes
                        attributes.screenBrightness = value
                        window.attributes = attributes
                    }
                    onGestureIndicator("Helderheid", value)
                } else {
                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (value * max).roundToInt(), 0)
                    onGestureIndicator("Volume", value)
                }
            }
        )
    }
}

private fun measuredQualityLabel(videoSize: VideoSize): String? = when (videoSize.height) {
    in 2160..Int.MAX_VALUE -> "4K"
    in 1080..2159 -> "FHD"
    in 720..1079 -> "HD"
    in 1..719 -> "SD"
    else -> null
}

private fun VideoSize.pipAspectRatio(): Rational = Rational(width.coerceAtLeast(16), height.coerceAtLeast(9))

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private const val SEEK_STEP_MS = 10_000L
private const val CONTROLS_TIMEOUT_MS = 4_500L
private const val CHANNEL_SWITCH_DEBOUNCE_MS = 700L
