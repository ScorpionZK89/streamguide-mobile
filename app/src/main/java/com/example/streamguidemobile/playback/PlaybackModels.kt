package com.example.streamguidemobile.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.MovieEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.data.SeriesEntity
import com.example.streamguidemobile.data.displayCode

enum class PlaybackCoordinatorStatus {
    STOPPED,
    LOCAL_STARTING,
    LOCAL_PLAYBACK,
    TRANSFERRING_TO_CAST,
    CAST_STARTING,
    CAST_PLAYBACK,
    TRANSFERRING_TO_LOCAL,
    ERROR
}

enum class PlaybackContentType { LIVE, CATCH_UP, MOVIE, EPISODE }

data class PlaybackMedia(
    val mediaId: String,
    val contentType: PlaybackContentType,
    val entityId: Long,
    val playlistId: Long,
    val streamUrl: String,
    val title: String,
    val subtitle: String? = null,
    val artworkUrl: String? = null,
    val startPositionMs: Long = 0L,
    val playWhenReady: Boolean = true,
    val selectedAudioLanguage: String? = null,
    val selectedSubtitleLanguage: String? = null
) {
    val isLive: Boolean get() = contentType == PlaybackContentType.LIVE

    fun withPlaybackSnapshot(snapshot: PlaybackSnapshot): PlaybackMedia = copy(
        startPositionMs = if (isLive) 0L else snapshot.positionMs.coerceAtLeast(0L),
        playWhenReady = snapshot.playWhenReady,
        selectedAudioLanguage = snapshot.selectedAudioLanguage,
        selectedSubtitleLanguage = snapshot.selectedSubtitleLanguage
    )

    /** Lets ExoPlayer inspect the response just as it did before Cast support was added. */
    fun toLocalMediaItem(): MediaItem = buildMediaItem(includeInferredMimeType = false)

    /** Gives the Cast receiver an explicit content type because it cannot sniff through the sender. */
    fun toCastMediaItem(): MediaItem = buildMediaItem(includeInferredMimeType = true)

    private fun buildMediaItem(includeInferredMimeType: Boolean): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setArtworkUri(artworkUrl?.takeIf(String::isNotBlank)?.let(Uri::parse))
            .setMediaType(
                when (contentType) {
                    PlaybackContentType.LIVE, PlaybackContentType.CATCH_UP -> MediaMetadata.MEDIA_TYPE_TV_SHOW
                    PlaybackContentType.MOVIE -> MediaMetadata.MEDIA_TYPE_MOVIE
                    PlaybackContentType.EPISODE -> MediaMetadata.MEDIA_TYPE_TV_SHOW
                }
            )
            .build()
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setUri(streamUrl)
            .apply {
                if (includeInferredMimeType) setMimeType(streamMimeType(streamUrl))
            }
            .setMediaMetadata(metadata)
            .build()
    }
}

data class PlaybackSnapshot(
    val positionMs: Long,
    val durationMs: Long,
    val playWhenReady: Boolean,
    val selectedAudioLanguage: String? = null,
    val selectedSubtitleLanguage: String? = null
)

internal data class LocalEndpointReleaseOutcome(
    val stopConfirmed: Boolean,
    val releaseCompleted: Boolean,
    val failure: Throwable? = null
)

internal fun releaseLocalEndpointSafely(
    stopAndClear: () -> Boolean,
    release: () -> Unit
): LocalEndpointReleaseOutcome {
    var failure: Throwable? = null
    val stopConfirmed = runCatching(stopAndClear)
        .onFailure { failure = it }
        .getOrDefault(false)
    val releaseCompleted = runCatching(release)
        .onFailure { if (failure == null) failure = it }
        .isSuccess
    return LocalEndpointReleaseOutcome(stopConfirmed, releaseCompleted, failure)
}

data class CastTrack(
    val key: String,
    val label: String,
    val selected: Boolean
)

data class PlaybackCoordinatorState(
    val status: PlaybackCoordinatorStatus = PlaybackCoordinatorStatus.STOPPED,
    val media: PlaybackMedia? = null,
    val castDeviceName: String? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isPlaying: Boolean = false,
    val canSeek: Boolean = false,
    val audioTracks: List<CastTrack> = emptyList(),
    val subtitleTracks: List<CastTrack> = emptyList(),
    val errorMessage: String? = null,
    val localPlaybackAuthorized: Boolean = true
) {
    val isCasting: Boolean get() = status in castStatuses
    val blocksLocalPlayback: Boolean get() = isCasting || status == PlaybackCoordinatorStatus.ERROR || !localPlaybackAuthorized

    companion object {
        private val castStatuses = setOf(
            PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
            PlaybackCoordinatorStatus.CAST_STARTING,
            PlaybackCoordinatorStatus.CAST_PLAYBACK,
            PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL
        )
    }
}

fun ChannelEntity.toLivePlaybackMedia(program: ProgramEntity? = null): PlaybackMedia = PlaybackMedia(
    mediaId = "streamguide:live:$id",
    contentType = PlaybackContentType.LIVE,
    entityId = id,
    playlistId = playlistId,
    streamUrl = streamUrl,
    title = name,
    subtitle = program?.title ?: groupTitle,
    artworkUrl = program?.iconUrl ?: logoUrl
)

fun ChannelEntity.toCatchUpPlaybackMedia(
    program: ProgramEntity,
    catchUpStreamUrl: String,
    startPositionMs: Long = 0L
): PlaybackMedia = PlaybackMedia(
    mediaId = "streamguide:catchup:${program.id}",
    contentType = PlaybackContentType.CATCH_UP,
    entityId = program.id,
    playlistId = playlistId,
    streamUrl = catchUpStreamUrl,
    title = program.title,
    subtitle = name,
    artworkUrl = program.iconUrl ?: logoUrl,
    startPositionMs = startPositionMs
)

fun MovieEntity.toPlaybackMedia(restart: Boolean = false): PlaybackMedia = PlaybackMedia(
    mediaId = "streamguide:movie:$id",
    contentType = PlaybackContentType.MOVIE,
    entityId = id,
    playlistId = playlistId,
    streamUrl = streamUrl,
    title = title,
    subtitle = listOfNotNull(year?.toString(), genre).joinToString(" · ").ifBlank { categoryName },
    artworkUrl = backdropUrl ?: posterUrl,
    startPositionMs = if (restart) 0L else playbackPositionMs
)

fun EpisodeEntity.toPlaybackMedia(series: SeriesEntity, restart: Boolean = false): PlaybackMedia = PlaybackMedia(
    mediaId = "streamguide:episode:$id",
    contentType = PlaybackContentType.EPISODE,
    entityId = id,
    playlistId = series.playlistId,
    streamUrl = streamUrl.orEmpty(),
    title = title,
    subtitle = "${series.title} · ${displayCode()}",
    artworkUrl = imageUrl ?: series.backdropUrl ?: series.posterUrl,
    startPositionMs = if (restart) 0L else playbackPositionMs
)

internal fun streamMimeType(url: String): String? {
    val extension = url.substringBefore('#').substringBefore('?').substringAfterLast('.', "").lowercase()
    return when (extension) {
        "m3u8" -> MimeTypes.APPLICATION_M3U8
        "mpd" -> MimeTypes.APPLICATION_MPD
        "ts", "m2ts" -> MimeTypes.VIDEO_MP2T
        "mp4", "m4v" -> MimeTypes.VIDEO_MP4
        "webm" -> MimeTypes.VIDEO_WEBM
        else -> null
    }
}
