@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.example.streamguidemobile.playback

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaQueueItem

/**
 * Keeps Media3's standard Cast metadata while describing linear channels as live streams.
 *
 * Media3 1.5.1's default converter marks every item as STREAM_TYPE_BUFFERED. Cast receivers use
 * STREAM_TYPE_LIVE for linear live television, so rebuild only those queue items with the correct
 * stream type and leave catch-up, movies and episodes unchanged.
 */
internal class StreamGuideMediaItemConverter(
    private val delegate: MediaItemConverter = DefaultMediaItemConverter()
) : MediaItemConverter {
    override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
        val queueItem = delegate.toMediaQueueItem(mediaItem)
        if (!isLiveCastMedia(mediaItem.mediaId)) return queueItem

        val original = requireNotNull(queueItem.media)
        val liveMedia = MediaInfo.Builder(original.contentId)
            .setStreamType(MediaInfo.STREAM_TYPE_LIVE)
            .setContentType(original.contentType)
            .setContentUrl(original.contentUrl ?: original.contentId)
            .setMetadata(original.metadata)
            .setCustomData(original.customData)
            .build()
        return MediaQueueItem.Builder(liveMedia).build()
    }

    override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem =
        delegate.toMediaItem(mediaQueueItem)
}

internal fun isLiveCastMedia(mediaId: String): Boolean = mediaId.startsWith("streamguide:live:")
