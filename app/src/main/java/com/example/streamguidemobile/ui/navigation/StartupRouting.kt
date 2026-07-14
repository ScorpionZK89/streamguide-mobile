package com.example.streamguidemobile.ui.navigation

internal enum class StartupContent {
    Loading,
    PlaylistSetup,
    Library
}

internal fun resolveStartupContent(
    isContentReady: Boolean,
    hasPlaylists: Boolean,
    addPlaylistRequested: Boolean
): StartupContent = when {
    !isContentReady -> StartupContent.Loading
    !hasPlaylists || addPlaylistRequested -> StartupContent.PlaylistSetup
    else -> StartupContent.Library
}
