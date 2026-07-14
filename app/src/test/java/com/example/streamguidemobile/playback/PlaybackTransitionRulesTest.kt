package com.example.streamguidemobile.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackTransitionRulesTest {
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

    private fun media(type: PlaybackContentType) = PlaybackMedia(
        mediaId = "streamguide:${type.name.lowercase()}:1",
        contentType = type,
        entityId = 1L,
        playlistId = 1L,
        streamUrl = "https://example.invalid/stream.m3u8",
        title = "Test"
    )
}
