@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.example.streamguidemobile.playback

import android.content.Context
import androidx.annotation.MainThread
import androidx.media3.cast.CastPlayer
import androidx.media3.cast.SessionAvailabilityListener
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import com.google.android.gms.cast.MediaStatus
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Owns both playback endpoints and enforces the single-stream subscription invariant.
 * A remote load can only start after the local ExoPlayer has been stopped, cleared and released.
 */
class PlaybackCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(PlaybackCoordinatorState())
    val state: StateFlow<PlaybackCoordinatorState> = _state.asStateFlow()

    // Media3 1.5.1 requires an eagerly available CastContext for CastPlayer construction.
    @Suppress("DEPRECATION")
    private val castContext: CastContext? = runCatching { CastContext.getSharedInstance(appContext) }.getOrNull()
    private val castPlayer: CastPlayer? = castContext?.let { CastPlayer(appContext, it, androidx.media3.cast.DefaultMediaItemConverter(), 10_000L, 10_000L, 3_000L) }
    private var localPlayer: ExoPlayer? = null
    private var localListener: Player.Listener? = null
    private var localMedia: PlaybackMedia? = null
    private var pendingMedia: PlaybackMedia? = null
    private var castLoadJob: Job? = null
    private var pendingSessionEnd = SessionEndPurpose.NONE
    private var released = false

    private val castPlayerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY && _state.value.status == PlaybackCoordinatorStatus.CAST_STARTING) {
                transition(
                    PlaybackCoordinatorStatus.CAST_PLAYBACK,
                    _state.value.copy(
                        status = PlaybackCoordinatorStatus.CAST_PLAYBACK,
                        isPlaying = castPlayer?.isPlaying == true,
                        errorMessage = null,
                        localPlaybackAuthorized = false
                    )
                )
            }
            updateCastSnapshot()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) = updateCastSnapshot()
        override fun onTracksChanged(tracks: Tracks) = updateCastSnapshot()
        override fun onTimelineChanged(timeline: androidx.media3.common.Timeline, reason: Int) {
            recoverActiveCastMedia()
            updateCastSnapshot()
        }

        override fun onPlayerError(error: PlaybackException) {
            failCast("Afspelen op Chromecast is mislukt.")
        }
    }

    private val sessionAvailabilityListener = object : SessionAvailabilityListener {
        override fun onCastSessionAvailable() = handleCastSessionAvailable()

        override fun onCastSessionUnavailable() {
            when {
                pendingSessionEnd == SessionEndPurpose.DISCONNECT -> completeRouteDisconnect()
                pendingSessionEnd == SessionEndPurpose.NONE && _state.value.isCasting -> {
                    failCast("De Chromecast-sessie is beëindigd.")
                }
            }
        }
    }

    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) = Unit
        override fun onSessionStarted(session: CastSession, sessionId: String) = handleCastSessionAvailable()

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            if (localPlayer == null && pendingMedia != null) failCast("Afspelen op Chromecast is mislukt.")
        }

        override fun onSessionEnding(session: CastSession) {
            if (pendingSessionEnd == SessionEndPurpose.NONE) pendingSessionEnd = SessionEndPurpose.DISCONNECT
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            when {
                pendingSessionEnd == SessionEndPurpose.DISCONNECT -> completeRouteDisconnect()
                pendingSessionEnd == SessionEndPurpose.NONE && _state.value.isCasting -> {
                    castLoadJob?.cancel()
                    failCast("De Chromecast-sessie is beëindigd.")
                }
            }
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = handleCastSessionAvailable()

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            if (localPlayer == null && pendingMedia != null) failCast("De Chromecast-sessie kon niet worden hersteld.")
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            if (_state.value.isCasting) {
                _state.value = _state.value.copy(isPlaying = false, localPlaybackAuthorized = false)
            }
        }
    }

    init {
        castPlayer?.addListener(castPlayerListener)
        castPlayer?.setSessionAvailabilityListener(sessionAvailabilityListener)
        castContext?.sessionManager?.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        if (castPlayer?.isCastSessionAvailable == true) handleCastSessionAvailable()

        scope.launch {
            while (isActive) {
                if (castPlayer?.isCastSessionAvailable == true) updateCastSnapshot()
                delay(500L)
            }
        }
    }

    val isCastSessionAvailable: Boolean get() = castPlayer?.isCastSessionAvailable == true

    /** Keeps the item that should load if the user connects from a detail screen. */
    fun prepareCastCandidate(media: PlaybackMedia) {
        if (media.streamUrl.isNotBlank()) pendingMedia = media
    }

    /** Routes a new user playback request to Cast when a session is active. */
    fun playOnCastIfConnected(media: PlaybackMedia): Boolean {
        if (!isCastSessionAvailable && !_state.value.isCasting) {
            pendingMedia = media
            if (_state.value.status == PlaybackCoordinatorStatus.STOPPED && !_state.value.localPlaybackAuthorized) {
                transition(
                    PlaybackCoordinatorStatus.STOPPED,
                    _state.value.copy(media = media, errorMessage = null, localPlaybackAuthorized = true)
                )
            }
            return false
        }
        pendingMedia = media
        queueCastLoad(media, debounce = _state.value.status == PlaybackCoordinatorStatus.CAST_PLAYBACK)
        return true
    }

    /** Returns the one coordinator-owned local player, or null while local playback is forbidden. */
    @MainThread
    fun acquireLocalPlayer(media: PlaybackMedia): ExoPlayer? {
        if (released || media.streamUrl.isBlank()) return null
        if (isCastSessionAvailable || _state.value.blocksLocalPlayback) {
            pendingMedia = media
            if (isCastSessionAvailable && !_state.value.isCasting) queueCastLoad(media)
            return null
        }
        localPlayer?.let { existing ->
            if (localMedia?.mediaId == media.mediaId) return existing
            releaseLocalEndpoint(existing)
        }

        pendingMedia = media
        localMedia = media
        transition(
            PlaybackCoordinatorStatus.LOCAL_STARTING,
            PlaybackCoordinatorState(
                status = PlaybackCoordinatorStatus.LOCAL_STARTING,
                media = media,
                localPlaybackAuthorized = true
            )
        )
        val player = ExoPlayer.Builder(appContext)
            .setSeekBackIncrementMs(10_000L)
            .setSeekForwardIncrementMs(10_000L)
            .build()
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (player !== localPlayer) return
                if (playbackState == Player.STATE_READY && _state.value.status == PlaybackCoordinatorStatus.LOCAL_STARTING) {
                    transition(
                        PlaybackCoordinatorStatus.LOCAL_PLAYBACK,
                        _state.value.copy(
                            status = PlaybackCoordinatorStatus.LOCAL_PLAYBACK,
                            isPlaying = player.isPlaying,
                            localPlaybackAuthorized = true
                        )
                    )
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (player === localPlayer && _state.value.status in localStatuses) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                }
            }
        }
        localPlayer = player
        localListener = listener
        player.addListener(listener)
        if (media.isLive) player.setMediaItem(media.toMediaItem())
        else player.setMediaItem(media.toMediaItem(), media.startPositionMs.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = media.playWhenReady
        return player
    }

    /** Stops normal local viewing when the player screen is closed. */
    @MainThread
    fun releaseLocalPlayer(player: Player) {
        if (player !== localPlayer) return
        releaseLocalEndpoint(player)
        if (_state.value.status in localStatuses) {
            transition(
                PlaybackCoordinatorStatus.STOPPED,
                PlaybackCoordinatorState(localPlaybackAuthorized = true)
            )
        }
    }

    /** Stops locally before handing the URL to another Android player. */
    @MainThread
    fun stopLocalForExternalPlayback(player: Player) {
        if (player === localPlayer) releaseLocalEndpoint(player)
        transition(PlaybackCoordinatorStatus.STOPPED, PlaybackCoordinatorState(localPlaybackAuthorized = true))
    }

    fun play() {
        castPlayer?.takeIf { isCastSessionAvailable }?.play()
    }

    fun pause() {
        castPlayer?.takeIf { isCastSessionAvailable }?.pause()
    }

    fun seekTo(positionMs: Long) {
        castPlayer?.takeIf { isCastSessionAvailable && it.isCurrentMediaItemSeekable }
            ?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun retryCast() {
        val media = _state.value.media ?: pendingMedia ?: return
        if (!isCastSessionAvailable) {
            failCast("Kies opnieuw een Chromecast met de Cast-knop.")
            return
        }
        queueCastLoad(media)
    }

    fun continueOnPhone() {
        val media = _state.value.media ?: pendingMedia ?: return
        castLoadJob?.cancel()
        castLoadJob = scope.launch {
            val snapshot = snapshotCast()
            pendingSessionEnd = SessionEndPurpose.CONTINUE_LOCAL
            transition(
                PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL,
                _state.value.copy(
                    status = PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL,
                    media = media.withPlaybackSnapshot(snapshot),
                    localPlaybackAuthorized = false,
                    errorMessage = null
                )
            )
            if (!stopRemoteAndEndSession()) {
                pendingSessionEnd = SessionEndPurpose.NONE
                failCast("De Chromecast kon niet volledig worden gestopt.")
                return@launch
            }
            pendingSessionEnd = SessionEndPurpose.NONE
            val resumed = media.withPlaybackSnapshot(snapshot)
            pendingMedia = resumed
            transition(
                PlaybackCoordinatorStatus.LOCAL_STARTING,
                PlaybackCoordinatorState(
                    status = PlaybackCoordinatorStatus.LOCAL_STARTING,
                    media = resumed,
                    positionMs = resumed.startPositionMs,
                    isPlaying = resumed.playWhenReady,
                    localPlaybackAuthorized = true
                )
            )
        }
    }

    fun stopWatching() {
        castLoadJob?.cancel()
        castLoadJob = scope.launch {
            pendingSessionEnd = SessionEndPurpose.STOP
            if (!stopRemoteAndEndSession()) {
                pendingSessionEnd = SessionEndPurpose.NONE
                failCast("De Chromecast kon niet volledig worden gestopt.")
                return@launch
            }
            pendingSessionEnd = SessionEndPurpose.NONE
            pendingMedia = null
            transition(
                PlaybackCoordinatorStatus.STOPPED,
                PlaybackCoordinatorState(localPlaybackAuthorized = false)
            )
        }
    }

    fun cancelCastError() {
        castLoadJob?.cancel()
        pendingMedia = null
        transition(
            PlaybackCoordinatorStatus.STOPPED,
            PlaybackCoordinatorState(localPlaybackAuthorized = false)
        )
    }

    fun selectCastAudio(key: String) = selectCastTrack(key, C.TRACK_TYPE_AUDIO)
    fun selectCastSubtitle(key: String) = selectCastTrack(key, C.TRACK_TYPE_TEXT)

    fun disableCastSubtitles() {
        val player = castPlayer ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .clearOverridesOfType(C.TRACK_TYPE_TEXT)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
    }

    /** Releases sender resources but deliberately leaves active Cast playback running. */
    fun release() {
        released = true
        castLoadJob?.cancel()
        localPlayer?.let(::releaseLocalEndpoint)
        castContext?.sessionManager?.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        castPlayer?.setSessionAvailabilityListener(null)
        castPlayer?.removeListener(castPlayerListener)
        castPlayer?.release()
        scope.cancel()
    }

    private fun handleCastSessionAvailable() {
        if (released) return
        val sourceMedia = localMedia ?: pendingMedia
        if (sourceMedia != null) {
            val snapshot = localPlayer?.let(::snapshotLocal)
            val transferMedia = snapshot?.let(sourceMedia::withPlaybackSnapshot) ?: sourceMedia
            if (localPlayer != null) {
                transition(
                    PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
                    _state.value.copy(
                        status = PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
                        media = transferMedia,
                        positionMs = transferMedia.startPositionMs,
                        localPlaybackAuthorized = false,
                        errorMessage = null
                    )
                )
                localPlayer?.let(::releaseLocalEndpoint)
            }
            pendingMedia = transferMedia
            queueCastLoad(transferMedia)
        } else {
            recoverActiveCastMedia()
            if (castPlayer?.currentMediaItem != null) {
                transition(
                    PlaybackCoordinatorStatus.CAST_PLAYBACK,
                    _state.value.copy(
                        status = PlaybackCoordinatorStatus.CAST_PLAYBACK,
                        localPlaybackAuthorized = false,
                        castDeviceName = currentCastDeviceName()
                    )
                )
            }
        }
    }

    private fun queueCastLoad(media: PlaybackMedia, debounce: Boolean = false) {
        if (media.streamUrl.isBlank()) {
            failCast("Deze stream kan niet op Chromecast worden geladen.")
            return
        }
        castLoadJob?.cancel()
        castLoadJob = scope.launch {
            if (debounce) delay(CAST_SWITCH_DEBOUNCE_MS)
            if (localPlayer != null) {
                transition(
                    PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
                    _state.value.copy(
                        status = PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
                        media = media,
                        localPlaybackAuthorized = false
                    )
                )
                localPlayer?.let(::releaseLocalEndpoint)
            }
            check(localPlayer == null) { "Local player must be released before Cast can load." }
            yield()
            if (!isCastSessionAvailable) {
                failCast("Afspelen op Chromecast is mislukt.")
                return@launch
            }

            val remotePlayer = castPlayer ?: run {
                failCast("Google Cast is niet beschikbaar op dit toestel.")
                return@launch
            }
            if (remotePlayer.currentMediaItem != null || remotePlayer.playbackState != Player.STATE_IDLE) {
                remotePlayer.playWhenReady = false
                remotePlayer.stop()
                remotePlayer.clearMediaItems()
                if (!awaitRemoteIdle()) {
                    failCast("De vorige Cast-stream kon niet volledig worden gestopt.")
                    return@launch
                }
            }

            pendingMedia = media
            transition(
                PlaybackCoordinatorStatus.CAST_STARTING,
                PlaybackCoordinatorState(
                    status = PlaybackCoordinatorStatus.CAST_STARTING,
                    media = media,
                    castDeviceName = currentCastDeviceName(),
                    positionMs = media.startPositionMs,
                    isPlaying = false,
                    localPlaybackAuthorized = false
                )
            )
            applyPreferredLanguages(remotePlayer, media)
            if (media.isLive) remotePlayer.setMediaItem(media.toMediaItem())
            else remotePlayer.setMediaItem(media.toMediaItem(), media.startPositionMs.coerceAtLeast(0L))
            remotePlayer.prepare()
            remotePlayer.playWhenReady = media.playWhenReady

            val loaded = withTimeoutOrNull(CAST_LOAD_TIMEOUT_MS) {
                while (isActive) {
                    if (!isCastSessionAvailable) return@withTimeoutOrNull false
                    if (remotePlayer.playerError != null) return@withTimeoutOrNull false
                    if (remotePlayer.playbackState == Player.STATE_READY) return@withTimeoutOrNull true
                    delay(100L)
                }
                false
            } == true
            if (!loaded) failCast("Afspelen op Chromecast is mislukt.")
        }
    }

    private fun applyPreferredLanguages(player: CastPlayer, media: PlaybackMedia) {
        val builder = player.trackSelectionParameters.buildUpon()
        media.selectedAudioLanguage?.let(builder::setPreferredAudioLanguage)
        media.selectedSubtitleLanguage?.let {
            builder.setPreferredTextLanguage(it).setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
        }
        player.trackSelectionParameters = builder.build()
    }

    private fun releaseLocalEndpoint(player: Player) {
        if (player !== localPlayer) return
        localListener?.let(player::removeListener)
        localListener = null
        player.playWhenReady = false
        player.stop()
        player.clearMediaItems()
        player.release()
        localPlayer = null
        localMedia = null
    }

    private fun snapshotLocal(player: Player): PlaybackSnapshot = PlaybackSnapshot(
        positionMs = player.currentPosition.coerceAtLeast(0L),
        durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L,
        playWhenReady = player.playWhenReady,
        selectedAudioLanguage = player.selectedLanguage(C.TRACK_TYPE_AUDIO),
        selectedSubtitleLanguage = player.selectedLanguage(C.TRACK_TYPE_TEXT)
    )

    private fun snapshotCast(): PlaybackSnapshot {
        val player = castPlayer
        return PlaybackSnapshot(
            positionMs = player?.currentPosition?.coerceAtLeast(0L) ?: _state.value.positionMs,
            durationMs = player?.duration?.takeIf { it != C.TIME_UNSET && it > 0L } ?: _state.value.durationMs,
            playWhenReady = player?.playWhenReady ?: _state.value.isPlaying,
            selectedAudioLanguage = player?.selectedLanguage(C.TRACK_TYPE_AUDIO),
            selectedSubtitleLanguage = player?.selectedLanguage(C.TRACK_TYPE_TEXT)
        )
    }

    private suspend fun stopRemoteAndEndSession(): Boolean {
        castPlayer?.let { player ->
            player.playWhenReady = false
            player.stop()
            player.clearMediaItems()
        }
        awaitRemoteIdle()
        castContext?.sessionManager?.endCurrentSession(true)
        val sessionEnded = withTimeoutOrNull(CAST_STOP_TIMEOUT_MS) {
            while (isActive && isCastSessionAvailable) delay(100L)
            true
        } == true
        return sessionEnded && awaitRemoteIdle()
    }

    private suspend fun awaitRemoteIdle(): Boolean = withTimeoutOrNull(CAST_STOP_TIMEOUT_MS) {
            while (isActive) {
                val client = castContext?.sessionManager?.currentCastSession?.remoteMediaClient
                val playerState = client?.mediaStatus?.playerState
                if (client == null || playerState == null || playerState == MediaStatus.PLAYER_STATE_IDLE) return@withTimeoutOrNull true
                delay(100L)
            }
            false
        } == true

    private fun recoverActiveCastMedia() {
        val player = castPlayer ?: return
        val item = player.currentMediaItem
        val recovered = item?.toPlaybackMediaOrNull() ?: pendingMedia ?: _state.value.media
        if (recovered != null) {
            pendingMedia = recovered
            _state.value = _state.value.copy(
                media = recovered,
                castDeviceName = currentCastDeviceName(),
                localPlaybackAuthorized = false
            )
        }
    }

    private fun updateCastSnapshot() {
        val player = castPlayer ?: return
        if (!player.isCastSessionAvailable) return
        recoverActiveCastMedia()
        val trackSnapshot = player.currentTracks.toCastTracks()
        _state.value = _state.value.copy(
            castDeviceName = currentCastDeviceName(),
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L,
            isPlaying = player.isPlaying,
            canSeek = player.isCurrentMediaItemSeekable,
            audioTracks = trackSnapshot.first,
            subtitleTracks = trackSnapshot.second,
            localPlaybackAuthorized = false
        )
    }

    private fun selectCastTrack(key: String, type: Int) {
        val player = castPlayer ?: return
        val match = player.currentTracks.groups.asSequence()
            .filter { it.type == type }
            .flatMap { group -> (0 until group.length).asSequence().map { index -> group to index } }
            .firstOrNull { (group, index) -> group.trackKey(index) == key }
            ?: return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(type, false)
            .setOverrideForType(TrackSelectionOverride(match.first.mediaTrackGroup, match.second))
            .build()
    }

    private fun failCast(message: String) {
        castLoadJob?.cancel()
        localPlayer?.let(::releaseLocalEndpoint)
        transition(
            PlaybackCoordinatorStatus.ERROR,
            _state.value.copy(
                status = PlaybackCoordinatorStatus.ERROR,
                isPlaying = false,
                canSeek = false,
                errorMessage = message,
                localPlaybackAuthorized = false
            )
        )
    }

    private fun completeRouteDisconnect() {
        castLoadJob?.cancel()
        pendingSessionEnd = SessionEndPurpose.NONE
        pendingMedia = null
        if (_state.value.isCasting || _state.value.status == PlaybackCoordinatorStatus.ERROR) {
            transition(
                PlaybackCoordinatorStatus.STOPPED,
                PlaybackCoordinatorState(localPlaybackAuthorized = false)
            )
        }
    }

    private fun transition(to: PlaybackCoordinatorStatus, next: PlaybackCoordinatorState) {
        val from = _state.value.status
        check(PlaybackTransitionRules.canTransition(from, to)) { "Illegal playback transition: $from -> $to" }
        check(!(to in castOnlyStatuses && localPlayer != null)) { "Cast state cannot own a local player." }
        _state.value = next.copy(status = to)
    }

    private fun currentCastDeviceName(): String? =
        castContext?.sessionManager?.currentCastSession?.castDevice?.friendlyName

    private fun Player.selectedLanguage(type: Int): String? = currentTracks.groups.asSequence()
        .filter { it.type == type }
        .flatMap { group -> (0 until group.length).asSequence().map { index -> group to index } }
        .firstOrNull { (group, index) -> group.isTrackSelected(index) }
        ?.let { (group, index) -> group.getTrackFormat(index).language }

    private fun Tracks.toCastTracks(): Pair<List<CastTrack>, List<CastTrack>> {
        fun tracksFor(type: Int): List<CastTrack> = groups.asSequence()
            .filter { it.type == type }
            .flatMap { group ->
                (0 until group.length).asSequence().map { index ->
                    val format = group.getTrackFormat(index)
                    val fallback = if (type == C.TRACK_TYPE_AUDIO) "Audiotrack" else "Ondertiteling"
                    val name = format.label?.takeIf(String::isNotBlank)
                        ?: format.language?.let { Locale.forLanguageTag(it).displayLanguage.takeIf(String::isNotBlank) }
                        ?: fallback
                    CastTrack(group.trackKey(index), name, group.isTrackSelected(index))
                }
            }
            .distinctBy(CastTrack::key)
            .toList()
        return tracksFor(C.TRACK_TYPE_AUDIO) to tracksFor(C.TRACK_TYPE_TEXT)
    }

    private fun Tracks.Group.trackKey(index: Int): String = "${mediaTrackGroup.id}:$index"

    private fun MediaItem.toPlaybackMediaOrNull(): PlaybackMedia? {
        val parts = mediaId.split(':')
        if (parts.size != 3 || parts[0] != "streamguide") return null
        val type = when (parts[1]) {
            "live" -> PlaybackContentType.LIVE
            "catchup" -> PlaybackContentType.CATCH_UP
            "movie" -> PlaybackContentType.MOVIE
            "episode" -> PlaybackContentType.EPISODE
            else -> return null
        }
        val id = parts[2].toLongOrNull() ?: return null
        return PlaybackMedia(
            mediaId = mediaId,
            contentType = type,
            entityId = id,
            playlistId = 0L,
            streamUrl = localConfiguration?.uri?.toString().orEmpty(),
            title = mediaMetadata.title?.toString() ?: "Chromecast",
            subtitle = mediaMetadata.subtitle?.toString(),
            artworkUrl = mediaMetadata.artworkUri?.toString(),
            startPositionMs = castPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L,
            playWhenReady = castPlayer?.playWhenReady ?: true
        )
    }

    private enum class SessionEndPurpose { NONE, CONTINUE_LOCAL, STOP, DISCONNECT }

    private companion object {
        val localStatuses = setOf(PlaybackCoordinatorStatus.LOCAL_STARTING, PlaybackCoordinatorStatus.LOCAL_PLAYBACK)
        val castOnlyStatuses = setOf(PlaybackCoordinatorStatus.CAST_STARTING, PlaybackCoordinatorStatus.CAST_PLAYBACK)
        const val CAST_SWITCH_DEBOUNCE_MS = 350L
        const val CAST_LOAD_TIMEOUT_MS = 30_000L
        const val CAST_STOP_TIMEOUT_MS = 8_000L
    }
}
