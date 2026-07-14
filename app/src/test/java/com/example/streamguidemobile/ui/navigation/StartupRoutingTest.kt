package com.example.streamguidemobile.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class StartupRoutingTest {
    @Test
    fun `loading is shown until persisted content has been read`() {
        assertEquals(
            StartupContent.Loading,
            resolveStartupContent(isContentReady = false, hasPlaylists = true, addPlaylistRequested = false)
        )
    }

    @Test
    fun `existing playlist opens library after startup`() {
        assertEquals(
            StartupContent.Library,
            resolveStartupContent(isContentReady = true, hasPlaylists = true, addPlaylistRequested = false)
        )
    }

    @Test
    fun `setup remains available when explicitly requested`() {
        assertEquals(
            StartupContent.PlaylistSetup,
            resolveStartupContent(isContentReady = true, hasPlaylists = true, addPlaylistRequested = true)
        )
    }

    @Test
    fun `first run without playlist opens setup`() {
        assertEquals(
            StartupContent.PlaylistSetup,
            resolveStartupContent(isContentReady = true, hasPlaylists = false, addPlaylistRequested = false)
        )
    }
}
