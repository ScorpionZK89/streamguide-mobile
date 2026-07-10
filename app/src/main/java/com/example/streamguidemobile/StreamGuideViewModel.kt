package com.example.streamguidemobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.M3uParser
import com.example.streamguidemobile.data.PlaylistEntity
import com.example.streamguidemobile.data.StreamGuideDatabase
import com.example.streamguidemobile.data.TextSourceReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class StreamGuideViewModel(application: Application) : AndroidViewModel(application) {
    private val database = StreamGuideDatabase.get(application)
    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()
    private val reader = TextSourceReader(application)
    private val parser = M3uParser()
    private val query = MutableStateFlow("")
    private val showFavorites = MutableStateFlow(false)
    private val action = MutableStateFlow(ActionState())

    private val visibleChannels = combine(query, showFavorites) { queryValue, favorites -> queryValue to favorites }
        .flatMapLatest { (queryValue, favorites) ->
            when {
                favorites -> channelDao.observeFavorites()
                queryValue.isNotBlank() -> channelDao.search(queryValue.trim())
                else -> channelDao.observeAll()
            }
        }

    val state = combine(
        playlistDao.observeAll(),
        visibleChannels,
        channelDao.observeGroups(),
        query,
        showFavorites,
        action
    ) { playlists, channels, groups, queryValue, favorites, actionState ->
        StreamGuideState(
            playlists = playlists,
            channels = channels,
            groups = groups,
            query = queryValue,
            showFavorites = favorites,
            isLoading = actionState.isLoading,
            message = actionState.message,
            error = actionState.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreamGuideState())

    fun updateQuery(value: String) { query.value = value }
    fun setFavoritesOnly(value: Boolean) { showFavorites.value = value }
    fun clearMessage() { action.value = ActionState() }

    fun importPlaylist(name: String, url: String) {
        if (url.isBlank()) {
            action.value = ActionState(error = "Vul een M3U URL in.")
            return
        }
        viewModelScope.launch {
            action.value = ActionState(isLoading = true, message = "Playlist lezen")
            runCatching {
                val playlistId = playlistDao.insert(
                    PlaylistEntity(
                        name = name.ifBlank { "Mijn playlist" },
                        m3uUrl = url.trim(),
                        createdAt = System.currentTimeMillis(),
                        lastSyncAt = null
                    )
                )
                syncPlaylist(playlistId)
            }.onSuccess { count ->
                action.value = ActionState(message = "$count zenders geladen")
            }.onFailure { throwable ->
                action.value = ActionState(error = throwable.message ?: "Import mislukt")
            }
        }
    }

    fun syncFirstPlaylist() {
        viewModelScope.launch {
            val playlist = playlistDao.getAllOnce().firstOrNull()
            if (playlist == null) {
                action.value = ActionState(error = "Geen playlist gevonden.")
                return@launch
            }
            action.value = ActionState(isLoading = true, message = "Synchroniseren")
            runCatching { syncPlaylist(playlist.id) }
                .onSuccess { action.value = ActionState(message = "$it zenders bijgewerkt") }
                .onFailure { action.value = ActionState(error = it.message ?: "Sync mislukt") }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch { playlistDao.delete(id) }
    }

    fun toggleFavorite(channel: ChannelEntity) {
        viewModelScope.launch { channelDao.setFavorite(channel.id, !channel.isFavorite) }
    }

    fun markWatched(channel: ChannelEntity) {
        viewModelScope.launch { channelDao.markWatched(channel.id, System.currentTimeMillis()) }
    }

    suspend fun nextChannelId(currentId: Long, direction: Int): Long? = withContext(Dispatchers.IO) {
        val ids = channelDao.getChannelIds()
        if (ids.isEmpty()) return@withContext null
        val index = ids.indexOf(currentId).takeIf { it >= 0 } ?: return@withContext null
        ids[(index + direction + ids.size) % ids.size]
    }

    private suspend fun syncPlaylist(playlistId: Long): Int = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist niet gevonden.")
        val existing = channelDao.getForPlaylist(playlistId)
        val favoriteTvgIds = existing.filter { it.isFavorite }.mapNotNull { it.tvgId }.toSet()
        val favoriteUrls = existing.filter { it.isFavorite }.map { it.streamUrl }.toSet()
        val batch = mutableListOf<ChannelEntity>()
        var count = 0

        reader.readWithReader(playlist.m3uUrl) { stream ->
            val iterator = parser.parseSequence(stream).iterator()
            if (!iterator.hasNext()) return@readWithReader
            channelDao.deleteForPlaylist(playlistId)
            while (iterator.hasNext()) {
                val parsed = iterator.next()
                batch += ChannelEntity(
                    playlistId = playlistId,
                    name = parsed.name,
                    tvgId = parsed.tvgId,
                    tvgName = parsed.tvgName,
                    logoUrl = parsed.logoUrl,
                    groupTitle = parsed.groupTitle,
                    streamUrl = parsed.streamUrl,
                    isFavorite = parsed.tvgId in favoriteTvgIds || parsed.streamUrl in favoriteUrls,
                    sortOrder = count
                )
                count += 1
                if (batch.size >= 500) {
                    channelDao.insertAll(batch.toList())
                    batch.clear()
                    action.update { it.copy(message = "$count zenders opgeslagen") }
                }
            }
            if (batch.isNotEmpty()) channelDao.insertAll(batch.toList())
        }
        if (count == 0) error("Geen zenders gevonden in deze M3U-playlist.")
        playlistDao.updateLastSync(playlistId, System.currentTimeMillis())
        count
    }
}

data class StreamGuideState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val channels: List<ChannelEntity> = emptyList(),
    val groups: List<String> = emptyList(),
    val query: String = "",
    val showFavorites: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private data class ActionState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)
