package com.example.streamguidemobile.ui.cast

import android.util.Log
import android.view.ContextThemeWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.mediarouter.app.MediaRouteButton
import coil.compose.AsyncImage
import com.example.streamguidemobile.R
import com.example.streamguidemobile.playback.CastTrack
import com.example.streamguidemobile.playback.PlaybackContentType
import com.example.streamguidemobile.playback.PlaybackCoordinatorState
import com.example.streamguidemobile.playback.PlaybackCoordinatorStatus
import com.example.streamguidemobile.ui.player.formatPlaybackTime
import com.example.streamguidemobile.ui.theme.CinematicColors
import com.example.streamguidemobile.ui.theme.CinematicTypography
import com.google.android.gms.cast.framework.CastButtonFactory

private val LocalCastRouteLauncher = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun CastRouteProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val hostState = remember { CastRouteHostState() }
    var hostInitialized by remember { mutableStateOf(false) }
    var pickerPending by remember { mutableStateOf(false) }
    var setupReady by remember { mutableStateOf(false) }
    var setupError by remember { mutableStateOf<Throwable?>(null) }

    val launchCastPicker: () -> Unit = {
        val button = hostState.routeButton
        when {
            setupError != null -> showCastFailure(context, setupError)
            setupReady && button?.isAttachedToWindow == true -> runCatching { button.performClick() }
                .onFailure { error -> showCastFailure(context, error) }
            else -> {
                hostInitialized = true
                pickerPending = true
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (hostInitialized) {
            val button = remember(context) {
                val themedContext = ContextThemeWrapper(
                    context,
                    R.style.ThemeOverlay_StreamGuideMobile_CastButton
                )
                MediaRouteButton(themedContext).apply {
                    contentDescription = "Afspelen op Chromecast"
                    @Suppress("DEPRECATION")
                    setAlwaysVisible(true)
                }
            }
            DisposableEffect(button) {
                hostState.routeButton = button
                onDispose {
                    if (hostState.routeButton === button) hostState.routeButton = null
                }
            }
            LaunchedEffect(button) {
                runCatching {
                    CastButtonFactory.setUpMediaRouteButton(
                        context.applicationContext,
                        ContextCompat.getMainExecutor(context),
                        button
                    ).addOnSuccessListener {
                        setupReady = true
                    }.addOnFailureListener { error ->
                        setupError = error
                        pickerPending = false
                        Log.w(CAST_UI_TAG, "Google Cast button setup failed.", error)
                    }
                }.onFailure { error ->
                    setupError = error
                    pickerPending = false
                    Log.w(CAST_UI_TAG, "Google Cast button setup failed.", error)
                }
            }
            LaunchedEffect(pickerPending, setupReady, button) {
                if (pickerPending && setupReady) {
                    button.post {
                        if (pickerPending && button.isAttachedToWindow) {
                            pickerPending = false
                            runCatching { button.performClick() }
                                .onFailure { error -> showCastFailure(context, error) }
                        }
                    }
                }
            }
            LaunchedEffect(setupError) {
                setupError?.let { error -> showCastFailure(context, error) }
            }
            AndroidView(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(1.dp)
                    .alpha(0.01f),
                factory = { button }
            )
        }
        CompositionLocalProvider(LocalCastRouteLauncher provides launchCastPicker) {
            content()
        }
    }
}

private class CastRouteHostState {
    var routeButton: MediaRouteButton? = null
}

@Composable
fun CastRouteButton(modifier: Modifier = Modifier) {
    val launchCastPicker = LocalCastRouteLauncher.current
    IconButton(
        modifier = modifier.size(46.dp),
        onClick = launchCastPicker
    ) {
        Icon(
            imageVector = Icons.Default.Cast,
            contentDescription = "Afspelen op Chromecast",
            tint = Color.White
        )
    }
}

private fun showCastFailure(context: android.content.Context, error: Throwable?) {
    error?.let { Log.w(CAST_UI_TAG, "Google Cast picker failed.", it) }
    Toast.makeText(
        context,
        "Google Cast kon niet worden gestart. Werk Google Play-services bij en probeer opnieuw.",
        Toast.LENGTH_LONG
    ).show()
}

private const val CAST_UI_TAG = "CastRouteButton"

@Composable
fun CastPlaybackScreen(
    state: PlaybackCoordinatorState,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onRetry: () -> Unit,
    onContinueOnPhone: () -> Unit,
    onStopWatching: () -> Unit,
    onCancel: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String) -> Unit,
    onDisableSubtitles: () -> Unit,
    onCloseAppKeepCasting: () -> Unit
) {
    val media = state.media
    Box(Modifier.fillMaxSize().background(CinematicColors.Canvas)) {
        media?.artworkUrl?.let { artwork ->
            AsyncImage(
                model = artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.28f),
                    0.48f to CinematicColors.Canvas.copy(alpha = 0.82f),
                    1f to CinematicColors.Canvas
                )
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(shape = CircleShape, color = CinematicColors.Gold.copy(alpha = 0.16f)) {
                    Icon(Icons.Default.CastConnected, null, tint = CinematicColors.GoldBright, modifier = Modifier.padding(9.dp).size(22.dp))
                }
                Column {
                    Text("AFSPELEN OP", color = CinematicColors.TextMuted, style = CinematicTypography.Badge)
                    Text(state.castDeviceName ?: "Chromecast", color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle)
                }
            }
            CastRouteButton()
        }

        Column(
            modifier = Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (media?.contentType == PlaybackContentType.LIVE) {
                Surface(shape = RoundedCornerShape(6.dp), color = CinematicColors.Live) {
                    Text("LIVE", color = Color.White, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
            }
            Text(
                media?.title ?: "Chromecast",
                color = CinematicColors.TextPrimary,
                style = CinematicTypography.HeroTitle,
                textAlign = TextAlign.Center
            )
            media?.subtitle?.let {
                Text(it, color = CinematicColors.TextSecondary, style = CinematicTypography.Body, textAlign = TextAlign.Center)
            }

            when (state.status) {
                PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
                PlaybackCoordinatorStatus.CAST_STARTING,
                PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL -> {
                    CircularProgressIndicator(color = CinematicColors.Gold, strokeWidth = 2.dp)
                    Text(
                        if (state.status == PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL) "Chromecast volledig stoppen" else "Stream veilig overdragen",
                        color = CinematicColors.TextSecondary,
                        style = CinematicTypography.Metadata
                    )
                }
                PlaybackCoordinatorStatus.ERROR -> CastErrorControls(
                    message = state.errorMessage ?: "Afspelen op Chromecast is mislukt.",
                    onRetry = onRetry,
                    onContinueOnPhone = onContinueOnPhone,
                    onCancel = onCancel
                )
                PlaybackCoordinatorStatus.CAST_PLAYBACK -> {
                    if (media?.contentType != PlaybackContentType.LIVE && state.durationMs > 0L) {
                        Slider(
                            value = state.positionMs.coerceIn(0L, state.durationMs).toFloat(),
                            onValueChangeFinished = {},
                            onValueChange = { onSeek(it.toLong()) },
                            valueRange = 0f..state.durationMs.toFloat(),
                            modifier = Modifier.widthIn(max = 620.dp).fillMaxWidth()
                        )
                        Row(Modifier.widthIn(max = 620.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(formatPlaybackTime(state.positionMs), color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                            Text(formatPlaybackTime(state.durationMs), color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        onPrevious?.let { action -> CastControlButton(Icons.Default.SkipPrevious, "Vorige", action) }
                        CastControlButton(
                            icon = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            description = if (state.isPlaying) "Pauzeren" else "Afspelen",
                            onClick = onPlayPause,
                            prominent = true
                        )
                        onNext?.let { action -> CastControlButton(Icons.Default.SkipNext, "Volgende", action) }
                    }
                    CastTrackMenus(state.audioTracks, state.subtitleTracks, onSelectAudio, onSelectSubtitle, onDisableSubtitles)
                }
                else -> Unit
            }
        }

        if (state.status == PlaybackCoordinatorStatus.CAST_PLAYBACK) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onContinueOnPhone,
                    colors = ButtonDefaults.buttonColors(containerColor = CinematicColors.Gold, contentColor = CinematicColors.Canvas),
                    modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth()
                ) {
                    Icon(Icons.Default.PhoneAndroid, null)
                    Spacer(Modifier.size(8.dp))
                    Text("Verder kijken op telefoon")
                }
                Row(Modifier.widthIn(max = 420.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onStopWatching, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Stop, null)
                        Spacer(Modifier.size(6.dp))
                        Text("Stoppen met kijken")
                    }
                    OutlinedButton(onClick = onCloseAppKeepCasting, modifier = Modifier.weight(1f)) {
                        Text("App sluiten")
                    }
                }
                Text("App sluiten laat de Chromecast bewust doorspelen.", color = CinematicColors.TextMuted, style = CinematicTypography.Metadata)
            }
        }
    }
}

@Composable
private fun CastErrorControls(
    message: String,
    onRetry: () -> Unit,
    onContinueOnPhone: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(shape = RoundedCornerShape(18.dp), color = CinematicColors.Panel.copy(alpha = 0.96f)) {
        Column(
            Modifier.widthIn(max = 520.dp).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(message, color = CinematicColors.TextPrimary, style = CinematicTypography.SectionTitle, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Opnieuw proberen") }
            OutlinedButton(onClick = onContinueOnPhone) { Text("Verder kijken op telefoon") }
            OutlinedButton(onClick = onCancel) { Text("Annuleren") }
        }
    }
}

@Composable
private fun CastTrackMenus(
    audioTracks: List<CastTrack>,
    subtitleTracks: List<CastTrack>,
    onSelectAudio: (String) -> Unit,
    onSelectSubtitle: (String) -> Unit,
    onDisableSubtitles: () -> Unit
) {
    var audioExpanded by remember { mutableStateOf(false) }
    var subtitleExpanded by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (audioTracks.size > 1) {
            Box {
                OutlinedButton(onClick = { audioExpanded = true }) {
                    Icon(Icons.Default.Audiotrack, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Audio")
                }
                DropdownMenu(expanded = audioExpanded, onDismissRequest = { audioExpanded = false }) {
                    audioTracks.forEach { track ->
                        DropdownMenuItem(
                            text = { Text(if (track.selected) "• ${track.label}" else track.label) },
                            onClick = { onSelectAudio(track.key); audioExpanded = false }
                        )
                    }
                }
            }
        }
        if (subtitleTracks.isNotEmpty()) {
            Box {
                OutlinedButton(onClick = { subtitleExpanded = true }) {
                    Icon(Icons.Default.ClosedCaption, null)
                    Spacer(Modifier.size(6.dp))
                    Text("Ondertitels")
                }
                DropdownMenu(expanded = subtitleExpanded, onDismissRequest = { subtitleExpanded = false }) {
                    DropdownMenuItem(text = { Text("Uit") }, onClick = { onDisableSubtitles(); subtitleExpanded = false })
                    subtitleTracks.forEach { track ->
                        DropdownMenuItem(
                            text = { Text(if (track.selected) "• ${track.label}" else track.label) },
                            onClick = { onSelectSubtitle(track.key); subtitleExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CastControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    prominent: Boolean = false
) {
    Surface(
        shape = CircleShape,
        color = if (prominent) CinematicColors.Gold else CinematicColors.Panel,
        shadowElevation = if (prominent) 10.dp else 2.dp
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(if (prominent) 68.dp else 52.dp)) {
            Icon(
                icon,
                contentDescription = description,
                tint = if (prominent) CinematicColors.Canvas else CinematicColors.TextPrimary,
                modifier = Modifier.size(if (prominent) 34.dp else 26.dp)
            )
        }
    }
}
