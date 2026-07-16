package com.example.streamguidemobile.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.streamGuideSettings by preferencesDataStore(name = "streamguide_settings")

data class AppSettings(
    val showLogos: Boolean = true,
    val compactList: Boolean = false,
    val autoResumeLastChannel: Boolean = true,
    val hardwareDecoding: Boolean = true,
    val syncPlaylistsOnStart: Boolean = true,
    val syncEpgOnStart: Boolean = true,
    val playerGesturesEnabled: Boolean = true,
    val autoPlayNextEpisode: Boolean = true,
    val hiddenGroups: Set<String> = emptySet(),
    val hiddenMovieGroups: Set<String> = emptySet(),
    val hiddenSeriesGroups: Set<String> = emptySet(),
    val dismissedUpdateVersion: String? = null
)

data class StreamResolution(val width: Int, val height: Int)

fun streamResolutionBadge(resolution: StreamResolution?): String? {
    val height = resolution?.height ?: return null
    if (resolution.width <= 0 || height <= 0) return null
    return when {
        height >= 2000 -> "4K"
        height >= 1000 -> "FHD"
        height >= 700 -> "HD"
        else -> "SD"
    }
}

fun ChannelEntity.streamMetadataKey(): String {
    val identity = tvgId.normalizedKey()
        ?: "${name.normalizedKey().orEmpty()}|${groupTitle.normalizedKey().orEmpty()}"
    return "$playlistId|$identity"
}

class SettingsRepository(private val context: Context) {
    val settings: Flow<AppSettings> = context.streamGuideSettings.data.map { values ->
        AppSettings(
            showLogos = values[Keys.showLogos] ?: true,
            compactList = values[Keys.compactList] ?: false,
            autoResumeLastChannel = values[Keys.autoResumeLastChannel] ?: true,
            hardwareDecoding = values[Keys.hardwareDecoding] ?: true,
            syncPlaylistsOnStart = values[Keys.syncPlaylistsOnStart] ?: true,
            syncEpgOnStart = values[Keys.syncEpgOnStart] ?: true,
            playerGesturesEnabled = values[Keys.playerGesturesEnabled] ?: true,
            autoPlayNextEpisode = values[Keys.autoPlayNextEpisode] ?: true,
            hiddenGroups = values[Keys.hiddenGroups]?.toSet().orEmpty(),
            hiddenMovieGroups = values[Keys.hiddenMovieGroups]?.toSet().orEmpty(),
            hiddenSeriesGroups = values[Keys.hiddenSeriesGroups]?.toSet().orEmpty(),
            dismissedUpdateVersion = values[Keys.dismissedUpdateVersion]
        )
    }

    val streamResolutions: Flow<Map<String, StreamResolution>> = context.streamGuideSettings.data.map { values ->
        decodeResolutions(values[Keys.streamResolutions])
    }

    suspend fun setShowLogos(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.showLogos] = value }
    }

    suspend fun setCompactList(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.compactList] = value }
    }

    suspend fun setAutoResumeLastChannel(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.autoResumeLastChannel] = value }
    }

    suspend fun setHardwareDecoding(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.hardwareDecoding] = value }
    }

    suspend fun setSyncPlaylistsOnStart(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.syncPlaylistsOnStart] = value }
    }

    suspend fun setSyncEpgOnStart(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.syncEpgOnStart] = value }
    }

    suspend fun setPlayerGesturesEnabled(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.playerGesturesEnabled] = value }
    }

    suspend fun setAutoPlayNextEpisode(value: Boolean) {
        context.streamGuideSettings.edit { it[Keys.autoPlayNextEpisode] = value }
    }

    suspend fun setGroupVisible(group: String, visible: Boolean) {
        context.streamGuideSettings.edit { preferences ->
            val hidden = preferences[Keys.hiddenGroups].orEmpty().toMutableSet()
            if (visible) hidden.removeAll { it.equals(group, ignoreCase = true) } else hidden += group
            preferences[Keys.hiddenGroups] = hidden
        }
    }

    suspend fun showAllGroups() {
        context.streamGuideSettings.edit { it.remove(Keys.hiddenGroups) }
    }

    suspend fun hideGroups(groups: Collection<String>) {
        context.streamGuideSettings.edit { preferences ->
            val hidden = preferences[Keys.hiddenGroups].orEmpty().toMutableSet()
            hidden += groups.map { it.trim() }.filter { it.isNotEmpty() }
            preferences[Keys.hiddenGroups] = hidden
        }
    }

    suspend fun setMovieGroupVisible(group: String, visible: Boolean) {
        context.streamGuideSettings.edit { preferences ->
            val hidden = preferences[Keys.hiddenMovieGroups].orEmpty().toMutableSet()
            if (visible) hidden.removeAll { it.equals(group, ignoreCase = true) } else hidden += group
            preferences[Keys.hiddenMovieGroups] = hidden
        }
    }

    suspend fun showAllMovieGroups() {
        context.streamGuideSettings.edit { it.remove(Keys.hiddenMovieGroups) }
    }

    suspend fun hideMovieGroups(groups: Collection<String>) {
        context.streamGuideSettings.edit { preferences ->
            val hidden = preferences[Keys.hiddenMovieGroups].orEmpty().toMutableSet()
            hidden += groups.map { it.trim() }.filter { it.isNotEmpty() }
            preferences[Keys.hiddenMovieGroups] = hidden
        }
    }

    suspend fun setSeriesGroupVisible(group: String, visible: Boolean) {
        context.streamGuideSettings.edit { preferences ->
            val hidden = preferences[Keys.hiddenSeriesGroups].orEmpty().toMutableSet()
            if (visible) hidden.removeAll { it.equals(group, ignoreCase = true) } else hidden += group
            preferences[Keys.hiddenSeriesGroups] = hidden
        }
    }

    suspend fun showAllSeriesGroups() {
        context.streamGuideSettings.edit { it.remove(Keys.hiddenSeriesGroups) }
    }

    suspend fun hideSeriesGroups(groups: Collection<String>) {
        context.streamGuideSettings.edit { preferences ->
            val hidden = preferences[Keys.hiddenSeriesGroups].orEmpty().toMutableSet()
            hidden += groups.map { it.trim() }.filter { it.isNotEmpty() }
            preferences[Keys.hiddenSeriesGroups] = hidden
        }
    }

    suspend fun setDismissedUpdateVersion(version: String?) {
        context.streamGuideSettings.edit { preferences ->
            if (version.isNullOrBlank()) {
                preferences.remove(Keys.dismissedUpdateVersion)
            } else {
                preferences[Keys.dismissedUpdateVersion] = version
            }
        }
    }

    suspend fun setStreamResolution(channel: ChannelEntity, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        context.streamGuideSettings.edit { preferences ->
            val resolutions = decodeResolutions(preferences[Keys.streamResolutions]).toMutableMap()
            val key = channel.streamMetadataKey()
            val resolution = StreamResolution(width, height)
            if (resolutions[key] != resolution) {
                resolutions[key] = resolution
                preferences[Keys.streamResolutions] = encodeResolutions(resolutions)
            }
        }
    }

    suspend fun playbackPosition(channel: ChannelEntity): Long {
        val values = context.streamGuideSettings.data.first()
        return decodePlaybackPositions(values[Keys.playbackPositions])[channel.streamMetadataKey()] ?: 0L
    }

    suspend fun setPlaybackPosition(channel: ChannelEntity, positionMs: Long) {
        context.streamGuideSettings.edit { preferences ->
            val positions = decodePlaybackPositions(preferences[Keys.playbackPositions]).toMutableMap()
            val key = channel.streamMetadataKey()
            positions.remove(key)
            if (positionMs > 0L) positions[key] = positionMs
            while (positions.size > MAX_PLAYBACK_POSITIONS) positions.remove(positions.keys.first())
            if (positions.isEmpty()) {
                preferences.remove(Keys.playbackPositions)
            } else {
                preferences[Keys.playbackPositions] = encodePlaybackPositions(positions)
            }
        }
    }

    suspend fun clearPlaybackPositions() {
        context.streamGuideSettings.edit { it.remove(Keys.playbackPositions) }
    }

    private object Keys {
        val showLogos = booleanPreferencesKey("show_logos")
        val compactList = booleanPreferencesKey("compact_list")
        val autoResumeLastChannel = booleanPreferencesKey("auto_resume_last_channel")
        val hardwareDecoding = booleanPreferencesKey("hardware_decoding")
        val syncPlaylistsOnStart = booleanPreferencesKey("sync_playlists_on_start")
        val syncEpgOnStart = booleanPreferencesKey("sync_epg_on_start")
        val playerGesturesEnabled = booleanPreferencesKey("player_gestures_enabled")
        val autoPlayNextEpisode = booleanPreferencesKey("auto_play_next_episode")
        val hiddenGroups = stringSetPreferencesKey("hidden_groups")
        val hiddenMovieGroups = stringSetPreferencesKey("hidden_movie_groups")
        val hiddenSeriesGroups = stringSetPreferencesKey("hidden_series_groups")
        val dismissedUpdateVersion = stringPreferencesKey("dismissed_update_version")
        val streamResolutions = stringPreferencesKey("stream_resolutions")
        val playbackPositions = stringPreferencesKey("playback_positions")
    }
}

private fun decodeResolutions(value: String?): Map<String, StreamResolution> {
    if (value.isNullOrBlank()) return emptyMap()
    return runCatching {
        val root = JSONObject(value)
        buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val resolution = root.optJSONObject(key) ?: continue
                val width = resolution.optInt("width")
                val height = resolution.optInt("height")
                if (width > 0 && height > 0) put(key, StreamResolution(width, height))
            }
        }
    }.getOrDefault(emptyMap())
}

private fun encodeResolutions(resolutions: Map<String, StreamResolution>): String {
    val root = JSONObject()
    resolutions.forEach { (key, resolution) ->
        root.put(key, JSONObject().put("width", resolution.width).put("height", resolution.height))
    }
    return root.toString()
}

private fun decodePlaybackPositions(value: String?): Map<String, Long> {
    if (value.isNullOrBlank()) return emptyMap()
    return runCatching {
        val root = JSONObject(value)
        buildMap {
            val keys = root.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                root.optLong(key).takeIf { it > 0L }?.let { put(key, it) }
            }
        }
    }.getOrDefault(emptyMap())
}

private fun encodePlaybackPositions(positions: Map<String, Long>): String {
    val root = JSONObject()
    positions.forEach { (key, position) -> root.put(key, position) }
    return root.toString()
}

private fun String?.normalizedKey(): String? = this?.trim()?.lowercase()?.takeIf(String::isNotEmpty)

private const val MAX_PLAYBACK_POSITIONS = 200
