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
import com.example.streamguidemobile.domain.isGroupVisible
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

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
    private val guideDayStart = MutableStateFlow(startOfTodayMillis())
    private val guideQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            while (true) {
                now.value = System.currentTimeMillis()
                delay(60_000)
            }
        }
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            val playlists = playlistDao.getAllOnce()
            if (playlists.isNotEmpty() && (settings.syncPlaylistsOnStart || settings.syncEpgOnStart)) {
                action.value = ActionState(isLoading = true, message = "Automatisch synchroniseren")
                val result = synchronizeAll(
                    playlists = playlists,
                    syncPlaylists = settings.syncPlaylistsOnStart,
                    syncEpg = settings.syncEpgOnStart
                )
                action.value = result.toActionState("Automatisch bijgewerkt")
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

    private val contentStateBase = combine(
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

    private val guidePrograms = guideDayStart.flatMapLatest { dayStart ->
        programDao.observeWindow(dayStart, dayStart + ONE_DAY - 1L)
    }

    private val guideRows = combine(channelDao.observeAll(), guidePrograms) { channels, programs ->
        buildGuideRows(channels, programs)
    }

    private val guideSnapshot = combine(guideRows, guideQuery) { rows, queryValue ->
        val cleanQuery = queryValue.trim()
        val filteredRows = if (cleanQuery.isBlank()) rows else rows.filter { row ->
            row.channel.name.contains(cleanQuery, ignoreCase = true) ||
                row.channel.groupTitle.contains(cleanQuery, ignoreCase = true) ||
                row.programs.any { program ->
                    program.title.contains(cleanQuery, ignoreCase = true) ||
                        program.category?.contains(cleanQuery, ignoreCase = true) == true ||
                        program.description?.contains(cleanQuery, ignoreCase = true) == true
                }
        }
        GuideSnapshot(filteredRows, cleanQuery)
    }

    private val contentState = combine(contentStateBase, guideSnapshot, guideDayStart) { content, guide, dayStart ->
        content.copy(guideRows = guide.rows, guideDayStart = dayStart, guideQuery = guide.query)
    }

    private val contentWithSettings = combine(contentState, settingsRepository.settings) { content, settings ->
        val visibleRows = content.channelRows.filter { isGroupVisible(it.channel.groupTitle, settings.hiddenGroups) }
        content.copy(
            channelRows = visibleRows,
            channels = visibleRows.map { it.channel },
            guideRows = content.guideRows.filter { isGroupVisible(it.channel.groupTitle, settings.hiddenGroups) },
            allGroups = content.groups,
            groups = content.groups.filter { isGroupVisible(it, settings.hiddenGroups) },
            settings = settings
        )
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
    fun selectGuideDay(dayStart: Long) { guideDayStart.value = dayStart }
    fun updateGuideQuery(value: String) { guideQuery.value = value }
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

    fun syncAllPlaylists() = launchSync(syncPlaylists = true, syncEpg = false, label = "Playlists synchroniseren")

    fun syncAllEpg() = launchSync(syncPlaylists = false, syncEpg = true, label = "EPG synchroniseren")

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

    fun setSyncPlaylistsOnStart(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSyncPlaylistsOnStart(value) }
    }

    fun setSyncEpgOnStart(value: Boolean) {
        viewModelScope.launch { settingsRepository.setSyncEpgOnStart(value) }
    }

    fun setGroupVisible(group: String, visible: Boolean) {
        if (!visible && selectedGroup.value.equals(group, ignoreCase = true)) showAllChannels()
        viewModelScope.launch { settingsRepository.setGroupVisible(group, visible) }
    }

    fun showAllGroups() {
        viewModelScope.launch { settingsRepository.showAllGroups() }
    }

    fun hideAllGroups(groups: List<String>) {
        showAllChannels()
        viewModelScope.launch { settingsRepository.hideGroups(groups) }
    }

    suspend fun nextChannelId(currentId: Long, direction: Int): Long? = withContext(Dispatchers.IO) {
        val ids = channelDao.getChannelIds()
        if (ids.isEmpty()) return@withContext null
        val index = ids.indexOf(currentId).takeIf { it >= 0 } ?: return@withContext null
        ids[(index + direction + ids.size) % ids.size]
    }

    private fun launchSync(syncPlaylists: Boolean, syncEpg: Boolean, label: String) {
        viewModelScope.launch {
            val playlists = playlistDao.getAllOnce()
            if (playlists.isEmpty()) {
                action.value = ActionState(error = "Geen playlist gevonden.")
                return@launch
            }
            action.value = ActionState(isLoading = true, message = label)
            action.value = synchronizeAll(playlists, syncPlaylists, syncEpg).toActionState("Bijgewerkt")
        }
    }

    private suspend fun synchronizeAll(
        playlists: List<PlaylistEntity>,
        syncPlaylists: Boolean,
        syncEpg: Boolean
    ): SyncSummary {
        var channelCount = 0
        var epgCount = 0
        val failures = mutableListOf<String>()

        playlists.forEachIndexed { index, playlist ->
            if (syncPlaylists) {
                action.update { it.copy(message = "${playlist.name}: zenders (${index + 1}/${playlists.size})") }
                runCatching { syncPlaylist(playlist.id) }
                    .onSuccess { channelCount += it }
                    .onFailure { failures += "${playlist.name} zenders: ${it.message ?: "mislukt"}" }
            }
            if (syncEpg) {
                action.update { it.copy(message = "${playlist.name}: EPG (${index + 1}/${playlists.size})") }
                runCatching { syncEpgForPlaylist(playlist.id) }
                    .onSuccess { epgCount += it }
                    .onFailure { failures += "${playlist.name} EPG: ${it.message ?: "mislukt"}" }
            }
        }
        return SyncSummary(channelCount, epgCount, failures)
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
        val programsByChannel = programs.groupBy { it.playlistId to it.channelTvgId }
        val rows = channels.map { channel ->
            val channelPrograms = channel.tvgId?.let { programsByChannel[channel.playlistId to it] }.orEmpty()
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

    private fun buildGuideRows(
        channels: List<ChannelEntity>,
        programs: List<ProgramEntity>
    ): List<GuideChannelState> {
        val programsByChannel = programs.groupBy { it.playlistId to it.channelTvgId }
        return channels.mapNotNull { channel ->
            val channelPrograms = channel.tvgId
                ?.let { programsByChannel[channel.playlistId to it] }
                .orEmpty()
                .sortedBy { it.startTime }
            channelPrograms.takeIf { it.isNotEmpty() }?.let { GuideChannelState(channel, it) }
        }
    }

    private fun ProgramEntity.progressAt(currentTime: Long): Float {
        val duration = (endTime - startTime).coerceAtLeast(1L)
        return ((currentTime - startTime).toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    }

    private fun String?.cleanOrNull(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val ONE_HOUR = 60L * 60L * 1000L
        const val ONE_DAY = 24L * ONE_HOUR
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
    val allGroups: List<String> = emptyList(),
    val guideRows: List<GuideChannelState> = emptyList(),
    val guideDayStart: Long = startOfTodayMillis(),
    val guideQuery: String = "",
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

data class GuideChannelState(
    val channel: ChannelEntity,
    val programs: List<ProgramEntity>
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

private data class GuideSnapshot(
    val rows: List<GuideChannelState>,
    val query: String
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

private data class SyncSummary(
    val channelCount: Int,
    val epgCount: Int,
    val failures: List<String>
) {
    fun toActionState(prefix: String): ActionState {
        val parts = buildList {
            if (channelCount > 0) add("$channelCount zenders")
            if (epgCount > 0) add("$epgCount programma's")
        }
        val message = if (parts.isEmpty()) prefix else "$prefix: ${parts.joinToString(" en ")}"
        return ActionState(
            message = message,
            error = failures.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")
        )
    }
}

private data class ActionState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private fun startOfTodayMillis(): Long = LocalDate.now()
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
