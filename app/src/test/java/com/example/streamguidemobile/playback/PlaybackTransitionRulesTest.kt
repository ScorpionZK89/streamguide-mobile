package com.example.streamguidemobile.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTransitionRulesTest {
    @Test
    fun `released local endpoint is safe even while stop state is still settling`() {
        var released = false

        val outcome = releaseLocalEndpointSafely(
            stopAndClear = { false },
            release = { released = true }
        )

        assertFalse(outcome.stopConfirmed)
        assertTrue(outcome.releaseCompleted)
        assertTrue(released)
    }

    @Test
    fun `local endpoint release is still attempted after a stop exception`() {
        var released = false

        val outcome = releaseLocalEndpointSafely(
            stopAndClear = { error("stop failed") },
            release = { released = true }
        )

        assertFalse(outcome.stopConfirmed)
        assertTrue(outcome.releaseCompleted)
        assertTrue(released)
        assertEquals("stop failed", outcome.failure?.message)
    }

    @Test
    fun `failed local release keeps the transition blocked`() {
        val outcome = releaseLocalEndpointSafely(
            stopAndClear = { true },
            release = { error("release failed") }
        )

        assertTrue(outcome.stopConfirmed)
        assertFalse(outcome.releaseCompleted)
        assertEquals("release failed", outcome.failure?.message)
    }

    @Test
    fun replacingLocalMediaStopsOldEndpointBeforeStartingNewEndpoint() {
        assertTrue(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.LOCAL_PLAYBACK,
                PlaybackCoordinatorStatus.STOPPED
            )
        )
        assertTrue(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.STOPPED,
                PlaybackCoordinatorStatus.LOCAL_STARTING
            )
        )
    }

    @Test
    fun `local to cast must pass through transfer state`() {
        assertFalse(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.LOCAL_STARTING,
                PlaybackCoordinatorStatus.CAST_STARTING
            )
        )
        assertTrue(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.LOCAL_STARTING,
                PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST
            )
        )
        assertFalse(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.LOCAL_PLAYBACK,
                PlaybackCoordinatorStatus.CAST_STARTING
            )
        )
        assertTrue(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.LOCAL_PLAYBACK,
                PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST
            )
        )
        assertTrue(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST,
                PlaybackCoordinatorStatus.CAST_STARTING
            )
        )
    }

    @Test
    fun `cast to local must pass through transfer state`() {
        assertFalse(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.CAST_PLAYBACK,
                PlaybackCoordinatorStatus.LOCAL_STARTING
            )
        )
        assertFalse(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.ERROR,
                PlaybackCoordinatorStatus.LOCAL_STARTING
            )
        )
        assertTrue(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.CAST_PLAYBACK,
                PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL
            )
        )
        assertTrue(
            PlaybackTransitionRules.canTransition(
                PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL,
                PlaybackCoordinatorStatus.LOCAL_STARTING
            )
        )
    }

    @Test
    fun `cast and error states always block local playback`() {
        PlaybackCoordinatorStatus.entries
            .filter {
                it == PlaybackCoordinatorStatus.ERROR ||
                    it == PlaybackCoordinatorStatus.TRANSFERRING_TO_CAST ||
                    it == PlaybackCoordinatorStatus.CAST_STARTING ||
                    it == PlaybackCoordinatorStatus.CAST_PLAYBACK ||
                    it == PlaybackCoordinatorStatus.TRANSFERRING_TO_LOCAL
            }
            .forEach { status ->
                assertTrue(
                    "Expected $status to block local playback",
                    PlaybackCoordinatorState(status = status, localPlaybackAuthorized = true).blocksLocalPlayback
                )
            }
        assertTrue(
            PlaybackCoordinatorState(
                status = PlaybackCoordinatorStatus.STOPPED,
                localPlaybackAuthorized = false
            ).blocksLocalPlayback
        )
        assertFalse(
            PlaybackCoordinatorState(
                status = PlaybackCoordinatorStatus.STOPPED,
                localPlaybackAuthorized = true
            ).blocksLocalPlayback
        )
    }

    @Test
    fun `snapshots resume on demand media but never seek live tv`() {
        val snapshot = PlaybackSnapshot(
            positionMs = 42_000L,
            durationMs = 120_000L,
            playWhenReady = false,
            selectedAudioLanguage = "nl",
            selectedSubtitleLanguage = "en"
        )
        val live = media(PlaybackContentType.LIVE).withPlaybackSnapshot(snapshot)
        val movie = media(PlaybackContentType.MOVIE).withPlaybackSnapshot(snapshot)
        val catchUp = media(PlaybackContentType.CATCH_UP).withPlaybackSnapshot(snapshot)

        assertEquals(0L, live.startPositionMs)
        assertEquals(42_000L, movie.startPositionMs)
        assertEquals(42_000L, catchUp.startPositionMs)
        assertFalse(movie.playWhenReady)
        assertEquals("nl", movie.selectedAudioLanguage)
        assertEquals("en", movie.selectedSubtitleLanguage)
    }

    @Test
    fun `only linear channels use the Cast live stream type`() {
        assertTrue(isLiveCastMedia("streamguide:live:410"))
        assertFalse(isLiveCastMedia("streamguide:catchup:410"))
        assertFalse(isLiveCastMedia("streamguide:movie:410"))
        assertFalse(isLiveCastMedia("streamguide:episode:410"))
    }

    @Test
    fun `Xtream transport stream uses HLS only on Cast`() {
        val source = "https://provider.invalid/live/user/password/410.ts"

        assertEquals(
            "https://provider.invalid/live/user/password/410.m3u8",
            castCompatibleStreamUrl(source, isLive = true)
        )
        assertEquals(source, castCompatibleStreamUrl(source, isLive = false))
    }

    @Test
    fun `Cast HLS conversion preserves query and fragment`() {
        assertEquals(
            "https://provider.invalid/live/u/p/410.m3u8?token=abc#live",
            castCompatibleStreamUrl(
                "https://provider.invalid/live/u/p/410.ts?token=abc#live",
                isLive = true
            )
        )
    }

    @Test
    fun `non Xtream transport streams stay unchanged`() {
        val source = "https://provider.invalid/archive/410.ts"
        assertEquals(source, castCompatibleStreamUrl(source, isLive = true))
    }

    @Test
    fun `Cast probe recognizes manifests and transport streams without retaining content`() {
        assertEquals("hls", classifyCastProbeBody("#EXTM3U\n#EXT-X-VERSION:3".toByteArray()))
        val transportStream = ByteArray(377).also {
            it[0] = 0x47
            it[188] = 0x47
        }
        assertEquals("mpeg_ts", classifyCastProbeBody(transportStream))
        assertEquals("other", classifyCastProbeBody("<html>blocked</html>".toByteArray()))
        assertEquals("empty", classifyCastProbeBody(byteArrayOf()))
    }

    private fun media(type: PlaybackContentType) = PlaybackMedia(
        mediaId = "streamguide:${type.name.lowercase()}:1",
        contentType = type,
        entityId = 1L,
        playlistId = 1L,
        streamUrl = "https://example.invalid/stream.m3u8",
        title = "Test"
    )
}
