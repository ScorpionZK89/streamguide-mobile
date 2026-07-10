package com.example.streamguidemobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamguidemobile.data.AppSettings
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.M3uParser
import com.example.streamguidemobile.data.ParsedChannel
import com.example.streamguidemobile.data.PlaylistEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.data.SettingsRepository
import com.example.streamguidemobile.data.StreamGuideDatabase
import com.example.streamguidemobile.data.TextSourceReader
import com.example.streamguidemobile.data.XmltvParser
import com.example.streamguidemobile.data.XtreamClient
import com.example.streamguidemobile.data.XtreamSourceCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
    private val programDao = database.programDao()
    private val settingsRepository = SettingsRepository(application)
    private val reader = TextSourceReader(application)
    private val parser = M3uParser()
    private val xmltvParser = XmltvParser()
    private val xtreamClient = XtreamClient(reader)
    private val query = MutableStateFlow("")
    private val selectedGroup = MutableStateFlow<String?>(null)
    private val showFavorites = MutableStateFlow(false)
    private val showRecent = MutableStateFlow(false)
    private val action = MutableStateFlow(ActionState())
    private val now = MutableStateFlow(System.currentTimeMillis())

    init {
        viewModelScope.launch {
            while (true) {
                now.value = System.currentTimeMillis()
                delay(60_000)
            }
        }
    }

    private val filters = combine(query, selectedGroup, showFavorites, showRecent) { queryValue, group, favorites, recent ->
        ChannelFilters(
            query = queryValue.trim(),
            selectedGroup = group,
            showFavorites = favorites,
            showRecent = recent
        )
    }

    private val baseChannels = filters.flatMapLatest { filter ->
        val group = filter.selectedGroup
        when {
            filter.showFavorites -> channelDao.observeFavorites()
            filter.showRecent -> channelDao.observeRecent()
            group != null -> channelDao.observeByGroup(group)
            else -> channelDao.observeAll()
        }
    }

    private val programSnapshot = now.flatMapLatest { currentTime ->
        programDao.observeWindow(currentTime - ONE_HOUR, currentTime + TWELVE_HOURS)
    }.let { programs ->
        combine(now, programs) { currentTime, programList -> ProgramSnapshot(currentTime, programList) }
    }

    private val contentState = combine(
        playlistDao.observeAll(),
        baseChannels,
        programSnapshot,
        channelDao.observeGroups(),
        filters
    ) { playlists, channels, snapshot, groups, filter ->
        val rows = buildChannelRows(channels, snapshot.programs, filter, snapshot.now)
        StreamGuideState(
            playlists = playlists,
            channelRows = rows,
            channels = rows.map { it.channel },
            groups = groups,
            query = filter.query,
            selectedGroup = filter.selectedGroup,
            showFavorites = filter.showFavorites,
            showRecent = filter.showRecent,
            nowMillis = snapshot.now
        )
    }

    private val contentWithSettings = combine(contentState, settingsRepository.settings) { content, settings ->
        content.copy(settings = settings)
    }

    val state = combine(contentWithSettings, action) { content, actionState ->
        content.copy(
            isLoading = actionState.isLoading,
            message = actionState.message,
            error = actionState.error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StreamGuideState())

    fun updateQuery(value: String) { query.value = value }
    fun showAllChannels() {
        selectedGroup.value = null
        showFavorites.value = false
        showRecent.value = false
    }
    fun setFavoritesOnly(value: Boolean) {
        selectedGroup.value = null
        showRecent.value = false
        showFavorites.value = value
    }
    fun setRecentOnly(value: Boolean) {
        selectedGroup.value = null
        showFavorites.value = false
        showRecent.value = value
    }
    fun selectGroup(group: String) {
        selectedGroup.value = group
        showFavorites.value = false
        showRecent.value = false
    }
    fun clearMessage() { action.value = ActionState() }

    fun importPlaylist(name: String, url: String, epgUrl: String = "") {
        if (url.isBlank()) {
            action.value = ActionState(error = "Vul een M3U URL in.")
            return
        }
        viewModelScope.launch {
            action.value = ActionState(isLoading = true, message = "Playlist lezen")
            runCatching {
                val nowMillis = System.currentTimeMillis()
                val playlistId = playlistDao.insert(
                    PlaylistEntity(
                        name = name.ifBlank { "Mijn playlist" },
                        m3uUrl = url.trim(),
                        epgUrl = epgUrl.cleanOrNull(),
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        lastSyncAt = null
                    )
                )
                importResult(playlistId)
            }.onSuccess { result ->
                action.value = result.toActionState()
            }.onFailure { throwable ->
                action.value = ActionState(error = throwable.message ?: "Import mislukt")
            }
        }
    }

    fun importXtream(name: String, serverUrl: String, username: String, password: String, epgUrl: String = "") {
        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            action.value = ActionState(error = "Vul server, gebruikersnaam en wachtwoord in.")
            return
        }
        viewModelScope.launch {
            action.value = ActionState(isLoading = true, message = "Xtream Codes lezen")
            runCatching {
                val source = XtreamSourceCodec.encode(serverUrl, username, password)
                val nowMillis = System.currentTimeMillis()
                val playlistId = playlistDao.insert(
                    PlaylistEntity(
                        name = name.ifBlank { "Xtream Codes" },
                        m3uUrl = source,
                        epgUrl = epgUrl.cleanOrNull(),
                        createdAt = nowMillis,
                        updatedAt = nowMillis,
                        lastSyncAt = null
                    )
                )
                importResult(playlistId)
            }.onSuccess { result ->
                action.value = result.toActionState()
            }.onFailure { throwable ->
                action.value = ActionState(error = throwable.message ?: "Xtream import mislukt")
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
            runCatching { importResult(playlist.id) }
                .onSuccess { action.value = it.toActionState(prefix = "Bijgewerkt") }
                .onFailure { action.value = ActionState(error = it.message ?: "Sync mislukt") }
        }
    }

    fun syncEpgFirstPlaylist() {
        viewModelScope.launch {
            val playlist = playlistDao.getAllOnce().firstOrNull()
            if (playlist == null) {
                action.value = ActionState(error = "Geen playlist gevonden.")
                return@launch
            }
            action.value = ActionState(isLoading = true, message = "EPG synchroniseren")
            runCatching { syncEpgForPlaylist(playlist.id) }
                .onSuccess { count -> action.value = ActionState(message = "$count programma's geladen") }
                .onFailure { action.value = ActionState(error = it.message ?: "EPG sync mislukt") }
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

    fun setShowLogos(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowLogos(value) }
    }

    fun setCompactList(value: Boolean) {
        viewModelScope.launch { settingsRepository.setCompactList(value) }
    }

    fun setAutoResumeLastChannel(value: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoResumeLastChannel(value) }
    }

    fun setHardwareDecoding(value: Boolean) {
        viewModelScope.launch { settingsRepository.setHardwareDecoding(value) }
    }

    suspend fun nextChannelId(currentId: Long, direction: Int): Long? = withContext(Dispatchers.IO) {
        val ids = channelDao.getChannelIds()
        if (ids.isEmpty()) return@withContext null
        val index = ids.indexOf(currentId).takeIf { it >= 0 } ?: return@withContext null
        ids[(index + direction + ids.size) % ids.size]
    }

    private suspend fun importResult(playlistId: Long): ImportResult {
        val channelCount = syncPlaylist(playlistId)
        val epgOutcome = runCatching { syncEpgForPlaylist(playlistId) }
        return ImportResult(
            channelCount = channelCount,
            epgCount = epgOutcome.getOrDefault(0),
            epgError = epgOutcome.exceptionOrNull()?.message
        )
    }

    private suspend fun syncPlaylist(playlistId: Long): Int = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist niet gevonden.")
        val existing = channelDao.getForPlaylist(playlistId)
        val xtreamCredentials = XtreamSourceCodec.decode(playlist.m3uUrl)
        val count = if (xtreamCredentials != null) {
            action.update { it.copy(message = "Xtream zenders ophalen") }
            val channels = xtreamClient.loadLiveChannels(xtreamCredentials)
            saveParsedChannels(playlistId, existing, channels.asSequence())
        } else {
            reader.readWithReader(playlist.m3uUrl) { stream ->
                saveParsedChannels(playlistId, existing, parser.parseSequence(stream))
            }
        }

        if (count == 0) {
            if (xtreamCredentials != null) error("Geen live-zenders gevonden in Xtream Codes.")
            error("Geen zenders gevonden in deze M3U-playlist.")
        }
        playlistDao.updateLastSync(playlistId, System.currentTimeMillis())
        count
    }

    private suspend fun syncEpgForPlaylist(playlistId: Long): Int = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist niet gevonden.")
        val xtreamCredentials = XtreamSourceCodec.decode(playlist.m3uUrl)
        val epgUrl = playlist.epgUrl.cleanOrNull()
            ?: xtreamCredentials?.let { XtreamSourceCodec.xmltvUrl(it) }
            ?: return@withContext 0
        val knownTvgIds = channelDao.getForPlaylist(playlistId).mapNotNull { it.tvgId }.toSet()
        val fromTime = System.currentTimeMillis() - THREE_DAYS
        val toTime = System.currentTimeMillis() + SEVEN_DAYS

        val programs = reader.readWithReader(epgUrl) { stream -> xmltvParser.parse(stream) }
            .asSequence()
            .filter { it.endTime >= fromTime && it.startTime <= toTime }
            .filter { knownTvgIds.isEmpty() || it.channelTvgId in knownTvgIds }
            .map {
                ProgramEntity(
                    playlistId = playlistId,
                    channelTvgId = it.channelTvgId,
                    title = it.title,
                    description = it.description,
                    startTime = it.startTime,
                    endTime = it.endTime,
                    category = it.category,
                    iconUrl = it.iconUrl
                )
            }
            .toList()

        programDao.replaceForPlaylist(playlistId, programs)
        playlistDao.updateEpgUrl(playlistId, epgUrl, System.currentTimeMillis())
        programs.size
    }

    private suspend fun saveParsedChannels(
        playlistId: Long,
        existing: List<ChannelEntity>,
        parsedChannels: Sequence<ParsedChannel>
    ): Int {
        val favoriteTvgIds = existing.filter { it.isFavorite }.mapNotNull { it.tvgId }.toSet()
        val favoriteUrls = existing.filter { it.isFavorite }.map { it.streamUrl }.toSet()
        val batch = mutableListOf<ChannelEntity>()
        val iterator = parsedChannels.iterator()
        var count = 0

        if (!iterator.hasNext()) return 0
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
        return count
    }

    private fun buildChannelRows(
        channels: List<ChannelEntity>,
        programs: List<ProgramEntity>,
        filter: ChannelFilters,
        currentTime: Long
    ): List<ChannelRowState> {
        val programsByChannel = programs.groupBy { it.channelTvgId }
        val rows = channels.map { channel ->
            val channelPrograms = channel.tvgId?.let { programsByChannel[it] }.orEmpty()
            val current = channelPrograms.firstOrNull { it.startTime <= currentTime && it.endTime > currentTime }
            val next = channelPrograms.firstOrNull { it.startTime > currentTime }
            ChannelRowState(
                channel = channel,
                currentProgram = current,
                nextProgram = next,
                progress = current?.progressAt(currentTime) ?: 0f
            )
        }

        if (filter.query.isBlank()) return rows
        val queryText = filter.query.lowercase()
        return rows.filter { row ->
            row.channel.name.contains(queryText, ignoreCase = true) ||
                row.channel.groupTitle.contains(queryText, ignoreCase = true) ||
                row.currentProgram?.title?.contains(queryText, ignoreCase = true) == true ||
                row.nextProgram?.title?.contains(queryText, ignoreCase = true) == true
        }
    }

    private fun ProgramEntity.progressAt(currentTime: Long): Float {
        val duration = (endTime - startTime).coerceAtLeast(1L)
        return ((currentTime - startTime).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val ONE_HOUR = 60L * 60L * 1000L
        const val TWELVE_HOURS = 12L * ONE_HOUR
        const val THREE_DAYS = 3L * 24L * ONE_HOUR
        const val SEVEN_DAYS = 7L * 24L * ONE_HOUR
    }
}

data class StreamGuideState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val channelRows: List<ChannelRowState> = emptyList(),
    val channels: List<ChannelEntity> = emptyList(),
    val groups: List<String> = emptyList(),
    val query: String = "",
    val selectedGroup: String? = null,
    val showFavorites: Boolean = false,
    val showRecent: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val nowMillis: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

data class ChannelRowState(
    val channel: ChannelEntity,
    val currentProgram: ProgramEntity?,
    val nextProgram: ProgramEntity?,
    val progress: Float
)

private data class ChannelFilters(
    val query: String,
    val selectedGroup: String?,
    val showFavorites: Boolean,
    val showRecent: Boolean
)

private data class ProgramSnapshot(
    val now: Long,
    val programs: List<ProgramEntity>
)

private data class ImportResult(
    val channelCount: Int,
    val epgCount: Int,
    val epgError: String?
) {
    fun toActionState(prefix: String = "Geladen"): ActionState {
        val message = if (epgCount > 0) {
            "$prefix: $channelCount zenders en $epgCount programma's"
        } else {
            "$prefix: $channelCount zenders"
        }
        return ActionState(message = message, error = epgError?.let { "EPG niet geladen: $it" })
    }
}

private data class ActionState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)