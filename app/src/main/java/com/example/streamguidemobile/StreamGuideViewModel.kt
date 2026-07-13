package com.example.streamguidemobile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.streamguidemobile.data.AppSettings
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.ExistingChannelMatcher
import com.example.streamguidemobile.data.M3uParser
import com.example.streamguidemobile.data.ExistingMovieMatcher
import com.example.streamguidemobile.data.MovieEntity
import com.example.streamguidemobile.data.EpisodeEntity
import com.example.streamguidemobile.data.ExistingSeriesMatcher
import com.example.streamguidemobile.data.ParsedChannel
import com.example.streamguidemobile.data.ParsedContentType
import com.example.streamguidemobile.data.ParsedMovie
import com.example.streamguidemobile.data.ParsedSeries
import com.example.streamguidemobile.data.PlaylistEntity
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.data.SettingsRepository
import com.example.streamguidemobile.data.SeriesEntity
import com.example.streamguidemobile.data.StreamGuideDatabase
import com.example.streamguidemobile.data.StreamResolution
import com.example.streamguidemobile.data.streamMetadataKey
import com.example.streamguidemobile.data.TextSourceReader
import com.example.streamguidemobile.data.XmltvParser
import com.example.streamguidemobile.data.XtreamClient
import com.example.streamguidemobile.data.XtreamSourceCodec
import com.example.streamguidemobile.data.toChannelEntity
import com.example.streamguidemobile.data.toMovieEntity
import com.example.streamguidemobile.data.toParsedMovie
import com.example.streamguidemobile.data.toSeriesEntity
import com.example.streamguidemobile.data.parsedEpisodesToEntities
import com.example.streamguidemobile.data.withDetails
import com.example.streamguidemobile.domain.isGroupVisible
import com.example.streamguidemobile.ui.player.normalizedResumePosition
import com.example.streamguidemobile.playback.PlaybackCoordinator
import com.example.streamguidemobile.ui.movies.CATEGORY_ALL
import com.example.streamguidemobile.ui.movies.MovieFilters
import com.example.streamguidemobile.ui.movies.MovieLibraryState
import com.example.streamguidemobile.ui.movies.MovieSort
import com.example.streamguidemobile.ui.movies.buildMovieLibrary
import com.example.streamguidemobile.ui.movies.categoryKey
import com.example.streamguidemobile.ui.series.SERIES_ALL
import com.example.streamguidemobile.ui.series.SeriesFilters
import com.example.streamguidemobile.ui.series.SeriesLibraryState
import com.example.streamguidemobile.ui.series.SeriesSort
import com.example.streamguidemobile.ui.series.buildSeriesLibrary
import com.example.streamguidemobile.ui.series.seriesCategoryKey
import com.example.streamguidemobile.update.AppUpdateRepository
import com.example.streamguidemobile.update.AppUpdateState
import com.example.streamguidemobile.update.isNewerVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class StreamGuideViewModel(application: Application) : AndroidViewModel(application) {
    val playbackCoordinator = PlaybackCoordinator(application)
    val playbackState = playbackCoordinator.state
    private val database = StreamGuideDatabase.get(application)
    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()
    private val programDao = database.programDao()
    private val movieDao = database.movieDao()
    private val seriesDao = database.seriesDao()
    private val episodeDao = database.episodeDao()
    private val settingsRepository = SettingsRepository(application)
    private val reader = TextSourceReader(application)
    private val parser = M3uParser()
    private val xmltvParser = XmltvParser()
    private val xtreamClient = XtreamClient(reader)
    private val appUpdateRepository = AppUpdateRepository(application)
    private val _appUpdateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val appUpdateState = _appUpdateState.asStateFlow()
    private val query = MutableStateFlow("")
    private val selectedGroup = MutableStateFlow<String?>(null)
    private val showFavorites = MutableStateFlow(false)
    private val showRecent = MutableStateFlow(false)
    private val action = MutableStateFlow(ActionState())
    private val now = MutableStateFlow(System.currentTimeMillis())
    private val guideDayStart = MutableStateFlow(startOfTodayMillis())
    private val guideQuery = MutableStateFlow("")
    private val movieQueryInput = MutableStateFlow("")
    private val movieCategory = MutableStateFlow(CATEGORY_ALL)
    private val movieFilters = MutableStateFlow(MovieFilters())
    private val movieSort = MutableStateFlow(MovieSort.RecentlyAdded)
    private val movieLoading = MutableStateFlow(false)
    private val movieError = MutableStateFlow<String?>(null)
    private val seriesQueryInput = MutableStateFlow("")
    private val seriesCategory = MutableStateFlow(SERIES_ALL)
    private val seriesFilters = MutableStateFlow(SeriesFilters())
    private val seriesSort = MutableStateFlow(SeriesSort.RecentlyAdded)
    private val seriesLoading = MutableStateFlow(false)
    private val seriesDetailLoading = MutableStateFlow(false)
    private val seriesError = MutableStateFlow<String?>(null)

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
        checkForAppUpdate(manual = false)
    }

    override fun onCleared() {
        playbackCoordinator.release()
        super.onCleared()
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

    private val channelSnapshot = combine(baseChannels, settingsRepository.streamResolutions) { channels, resolutions ->
        ChannelSnapshot(channels, resolutions)
    }

    private val contentStateBase = combine(
        playlistDao.observeAll(),
        channelSnapshot,
        programSnapshot,
        channelDao.observeGroups(),
        filters
    ) { playlists, channelData, snapshot, groups, filter ->
        val rows = buildChannelRows(channelData.channels, snapshot.programs, filter, snapshot.now, channelData.resolutions)
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

    private val guideRows = combine(channelDao.observeAll(), guidePrograms, settingsRepository.streamResolutions) { channels, programs, resolutions ->
        buildGuideRows(channels, programs, resolutions)
    }

    private val homeRows = combine(channelDao.observeAll(), programSnapshot, settingsRepository.streamResolutions) { channels, snapshot, resolutions ->
        buildChannelRows(
            channels = channels,
            programs = snapshot.programs,
            filter = ChannelFilters("", null, false, false),
            currentTime = snapshot.now,
            resolutions = resolutions
        )
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

    private val contentState = combine(contentStateBase, guideSnapshot, guideDayStart, homeRows) { content, guide, dayStart, home ->
        content.copy(guideRows = guide.rows, guideDayStart = dayStart, guideQuery = guide.query, homeRows = home)
    }

    private val contentWithSettings = combine(contentState, settingsRepository.settings) { content, settings ->
        val visibleRows = content.channelRows.filter { isGroupVisible(it.channel.groupTitle, settings.hiddenGroups) }
        content.copy(
            channelRows = visibleRows,
            channels = visibleRows.map { it.channel },
            guideRows = content.guideRows.filter { isGroupVisible(it.channel.groupTitle, settings.hiddenGroups) },
            homeRows = content.homeRows.filter { isGroupVisible(it.channel.groupTitle, settings.hiddenGroups) },
            allGroups = content.groups,
            groups = content.groups.filter { isGroupVisible(it, settings.hiddenGroups) },
            settings = settings
        )
    }

    private val movieQuery = movieQueryInput.debounce(250L)

    private val movieSource = combine(movieDao.observeAll(), settingsRepository.settings) { movies, settings ->
        MovieSource(movies, settings.hiddenMovieGroups)
    }

    private val movieLibrary = combine(
        movieSource,
        movieQuery,
        movieCategory,
        movieFilters,
        movieSort
    ) { source, queryValue, category, filtersValue, sortValue ->
        buildMovieLibrary(source.movies, queryValue, category, filtersValue, sortValue, source.hiddenGroups)
    }.let { library ->
        combine(library, movieLoading, movieError) { value, loading, error ->
            value.copy(isLoading = loading, error = error)
        }
    }

    private val seriesQuery = seriesQueryInput.debounce(250L)
    private val seriesSource = combine(seriesDao.observeAll(), episodeDao.observeAll(), settingsRepository.settings) { series, episodes, settings ->
        SeriesSource(series, episodes, settings.hiddenSeriesGroups)
    }
    private val seriesLibrary = combine(seriesSource, seriesQuery, seriesCategory, seriesFilters, seriesSort) { source, queryValue, category, filtersValue, sortValue ->
        buildSeriesLibrary(source.series, source.episodes, queryValue, category, filtersValue, sortValue, source.hiddenGroups)
    }.let { library ->
        combine(library, seriesLoading, seriesDetailLoading, seriesError) { value, loading, detailLoading, error ->
            value.copy(isLoading = loading, detailLoading = detailLoading, error = error)
        }
    }

    val state = combine(contentWithSettings, movieLibrary, seriesLibrary, action) { content, movies, series, actionState ->
        content.copy(
            movieLibrary = movies.copy(
                isLoading = movies.isLoading || (actionState.isLoading && movies.allMovies.isEmpty())
            ),
            seriesLibrary = series.copy(isLoading = series.isLoading || (actionState.isLoading && series.allSeries.isEmpty())),
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
    fun updateMovieQuery(value: String) { movieQueryInput.value = value }
    fun selectMovieCategory(value: String) { movieCategory.value = value }
    fun updateMovieFilters(value: MovieFilters) { movieFilters.value = value }
    fun clearMovieFilters() { movieFilters.value = MovieFilters() }
    fun updateMovieSort(value: MovieSort) { movieSort.value = value }
    fun setMovieGroupVisible(group: String, visible: Boolean) {
        if (!visible && movieCategory.value == categoryKey(group)) movieCategory.value = CATEGORY_ALL
        viewModelScope.launch { settingsRepository.setMovieGroupVisible(group, visible) }
    }
    fun showAllMovieGroups() {
        viewModelScope.launch { settingsRepository.showAllMovieGroups() }
    }
    fun hideAllMovieGroups() {
        movieCategory.value = CATEGORY_ALL
        viewModelScope.launch {
            val groups = movieDao.getAllGroups()
            settingsRepository.hideMovieGroups(groups)
        }
    }
    fun updateSeriesQuery(value: String) { seriesQueryInput.value = value }
    fun selectSeriesCategory(value: String) { seriesCategory.value = value }
    fun updateSeriesFilters(value: SeriesFilters) { seriesFilters.value = value }
    fun clearSeriesFilters() { seriesFilters.value = SeriesFilters() }
    fun updateSeriesSort(value: SeriesSort) { seriesSort.value = value }
    fun setSeriesGroupVisible(group: String, visible: Boolean) {
        if (!visible && seriesCategory.value == seriesCategoryKey(group)) seriesCategory.value = SERIES_ALL
        viewModelScope.launch { settingsRepository.setSeriesGroupVisible(group, visible) }
    }
    fun showAllSeriesGroups() {
        viewModelScope.launch { settingsRepository.showAllSeriesGroups() }
    }
    fun hideAllSeriesGroups() {
        seriesCategory.value = SERIES_ALL
        viewModelScope.launch {
            val groups = seriesDao.getAllGroups()
            settingsRepository.hideSeriesGroups(groups)
        }
    }
    fun toggleSeriesFavorite(series: SeriesEntity) {
        viewModelScope.launch { seriesDao.setFavorite(series.id, !series.isFavorite) }
    }
    fun loadSeriesDetails(seriesId: Long, force: Boolean = false) {
        viewModelScope.launch {
            val series = seriesDao.getById(seriesId) ?: return@launch
            val cachedEpisodes = episodeDao.getForSeries(seriesId)
            if (!force && series.detailLoadedAt != null && series.detailLoadedAt > System.currentTimeMillis() - SERIES_DETAIL_CACHE_MS && cachedEpisodes.isNotEmpty()) return@launch
            val playlist = playlistDao.getById(series.playlistId) ?: return@launch
            val credentials = XtreamSourceCodec.decode(playlist.m3uUrl) ?: return@launch
            seriesDetailLoading.value = true
            seriesError.value = null
            runCatching { xtreamClient.loadSeriesDetails(credentials, series.providerId) }
                .onSuccess { details ->
                    val loadedAt = System.currentTimeMillis()
                    seriesDao.update(series.withDetails(details, loadedAt))
                    val episodes = parsedEpisodesToEntities(series.id, details.episodes, cachedEpisodes)
                    episodeDao.deleteForSeries(series.id)
                    episodes.chunked(300).forEach { episodeDao.insertAll(it) }
                    reconcileSeriesProgress(series.id)
                }
                .onFailure { seriesError.value = it.message ?: "Seriegegevens konden niet worden geladen." }
            seriesDetailLoading.value = false
        }
    }
    fun saveEpisodeProgress(episodeId: Long, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        viewModelScope.launch {
            val episode = episodeDao.getById(episodeId) ?: return@launch
            val watched = positionMs.toDouble() / durationMs.toDouble() >= MOVIE_WATCHED_THRESHOLD
            val storedPosition = if (watched) durationMs else positionMs.coerceIn(0L, durationMs)
            val watchedAt = if (storedPosition >= MIN_MOVIE_PROGRESS_MS) System.currentTimeMillis() else null
            episodeDao.updateProgress(episode.id, storedPosition, durationMs, watched, watchedAt)
            val series = seriesDao.getById(episode.seriesId) ?: return@launch
            if (watchedAt != null && (episode.sortOrder >= series.progressOrder || series.progressEpisodeId == episode.id)) {
                seriesDao.updateProgress(series.id, episode.id, episode.sortOrder, watchedAt)
            }
        }
    }
    fun restartEpisode(episodeId: Long) {
        viewModelScope.launch { episodeDao.updateProgress(episodeId, 0L, 0L, false, null) }
    }
    fun setEpisodeWatched(episode: EpisodeEntity, watched: Boolean) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            episodeDao.setWatched(episode.id, watched, timestamp)
            val series = seriesDao.getById(episode.seriesId) ?: return@launch
            if (watched && episode.sortOrder >= series.progressOrder) seriesDao.updateProgress(series.id, episode.id, episode.sortOrder, timestamp)
            else if (!watched && series.progressEpisodeId == episode.id) reconcileSeriesProgress(series.id, force = true)
        }
    }
    fun setSeasonWatched(seriesId: Long, seasonNumber: Int, watched: Boolean) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            episodeDao.setSeasonWatched(seriesId, seasonNumber, watched, timestamp)
            if (!watched) {
                val series = seriesDao.getById(seriesId) ?: return@launch
                val currentSeason = series.progressEpisodeId?.let { episodeDao.getById(it)?.seasonNumber }
                if (currentSeason == seasonNumber) reconcileSeriesProgress(seriesId, force = true)
                return@launch
            }
            episodeDao.getForSeries(seriesId).filter { it.seasonNumber == seasonNumber }.maxByOrNull { it.sortOrder }?.let {
                val series = seriesDao.getById(seriesId) ?: return@launch
                if (it.sortOrder >= series.progressOrder) seriesDao.updateProgress(seriesId, it.id, it.sortOrder, timestamp)
            }
        }
    }
    fun setSeriesWatched(seriesId: Long, watched: Boolean) {
        viewModelScope.launch {
            val timestamp = System.currentTimeMillis()
            episodeDao.setSeriesWatched(seriesId, watched, timestamp)
            if (watched) {
                episodeDao.getForSeries(seriesId).maxByOrNull { it.sortOrder }?.let { seriesDao.updateProgress(seriesId, it.id, it.sortOrder, timestamp) }
            } else seriesDao.updateProgress(seriesId, null, -1, null)
        }
    }
    fun clearSeriesProgress(seriesId: Long) { setSeriesWatched(seriesId, false) }

    private suspend fun reconcileSeriesProgress(seriesId: Long, force: Boolean = false) {
        val series = seriesDao.getById(seriesId) ?: return
        val episodes = episodeDao.getForSeries(seriesId)
        if (!force && episodes.any { it.id == series.progressEpisodeId }) return
        val latest = episodes.filter { it.isWatched || it.playbackPositionMs > 0L }
            .maxWithOrNull(compareBy<EpisodeEntity> { it.sortOrder }.thenBy { it.lastWatchedAt ?: Long.MIN_VALUE })
        seriesDao.updateProgress(seriesId, latest?.id, latest?.sortOrder ?: -1, latest?.lastWatchedAt)
    }

    fun updateEpisodeResolution(episodeId: Long, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        viewModelScope.launch { episodeDao.updateResolution(episodeId, width, height) }
    }
    fun clearMovieError() { movieError.value = null }
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

    fun toggleMovieFavorite(movie: MovieEntity) {
        viewModelScope.launch { movieDao.setFavorite(movie.id, !movie.isFavorite) }
    }

    fun loadMovieDetails(movieId: Long) {
        viewModelScope.launch {
            val movie = movieDao.getById(movieId) ?: return@launch
            if (movie.providerId.isNullOrBlank()) return@launch
            val playlist = playlistDao.getById(movie.playlistId) ?: return@launch
            val credentials = XtreamSourceCodec.decode(playlist.m3uUrl) ?: return@launch
            movieLoading.value = true
            movieError.value = null
            runCatching { xtreamClient.loadVodDetails(credentials, movie.providerId) }
                .onSuccess { movieDao.update(movie.withDetails(it)) }
                .onFailure { movieError.value = it.message ?: "Filmgegevens konden niet worden geladen." }
            movieLoading.value = false
        }
    }

    fun saveMovieProgress(movieId: Long, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0L) return
        viewModelScope.launch {
            val watched = positionMs.toDouble() / durationMs.toDouble() >= MOVIE_WATCHED_THRESHOLD
            val storedPosition = if (watched) durationMs else positionMs.coerceIn(0L, durationMs)
            movieDao.updateProgress(
                id = movieId,
                positionMs = storedPosition,
                durationMs = durationMs,
                watched = watched,
                watchedAt = if (storedPosition >= MIN_MOVIE_PROGRESS_MS) System.currentTimeMillis() else null
            )
        }
    }

    fun restartMovie(movieId: Long) {
        viewModelScope.launch { movieDao.updateProgress(movieId, 0L, 0L, false, null) }
    }

    fun setMovieWatched(movie: MovieEntity, watched: Boolean) {
        viewModelScope.launch {
            movieDao.updateProgress(
                movie.id,
                if (watched) movie.playbackDurationMs.coerceAtLeast(1L) else 0L,
                if (watched) movie.playbackDurationMs.coerceAtLeast(1L) else 0L,
                watched,
                if (watched) System.currentTimeMillis() else null
            )
        }
    }

    fun updateMovieResolution(movieId: Long, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        viewModelScope.launch { movieDao.updateResolution(movieId, width, height) }
    }

    fun markWatched(channel: ChannelEntity) {
        viewModelScope.launch { channelDao.markWatched(channel.id, System.currentTimeMillis()) }
    }

    fun updateStreamResolution(channel: ChannelEntity, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        viewModelScope.launch { settingsRepository.setStreamResolution(channel, width, height) }
    }

    suspend fun playbackPosition(channel: ChannelEntity): Long = settingsRepository.playbackPosition(channel)

    fun savePlaybackPosition(
        channel: ChannelEntity,
        positionMs: Long,
        durationMs: Long,
        isLive: Boolean
    ) {
        val savedPosition = normalizedResumePosition(positionMs, durationMs, isLive) ?: return
        viewModelScope.launch { settingsRepository.setPlaybackPosition(channel, savedPosition) }
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

    fun setPlayerGesturesEnabled(value: Boolean) {
        viewModelScope.launch { settingsRepository.setPlayerGesturesEnabled(value) }
    }

    fun setAutoPlayNextEpisode(value: Boolean) {
        viewModelScope.launch { settingsRepository.setAutoPlayNextEpisode(value) }
    }

    fun checkForAppUpdate(manual: Boolean = true) {
        if (_appUpdateState.value is AppUpdateState.Checking || _appUpdateState.value is AppUpdateState.Downloading) return
        viewModelScope.launch {
            _appUpdateState.value = AppUpdateState.Checking(manual)
            runCatching { appUpdateRepository.latestRelease() }
                .onSuccess { release ->
                    val dismissedVersion = settingsRepository.settings.first().dismissedUpdateVersion
                    _appUpdateState.value = when {
                        !isNewerVersion(release.versionName, BuildConfig.VERSION_NAME) -> {
                            if (manual) AppUpdateState.UpToDate else AppUpdateState.Idle
                        }
                        !manual && release.versionName.equals(dismissedVersion, ignoreCase = true) -> AppUpdateState.Idle
                        else -> AppUpdateState.Available(release)
                    }
                }
                .onFailure { error ->
                    _appUpdateState.value = if (manual) {
                        AppUpdateState.Error(error.message ?: "Controleren op updates is mislukt.")
                    } else {
                        AppUpdateState.Idle
                    }
                }
        }
    }

    fun dismissAppUpdate() {
        val release = when (val current = _appUpdateState.value) {
            is AppUpdateState.Available -> current.release
            is AppUpdateState.Downloaded -> current.release
            else -> null
        }
        _appUpdateState.value = AppUpdateState.Idle
        if (release != null) {
            viewModelScope.launch { settingsRepository.setDismissedUpdateVersion(release.versionName) }
        }
    }

    fun downloadAppUpdate() {
        val release = (_appUpdateState.value as? AppUpdateState.Available)?.release ?: return
        viewModelScope.launch {
            _appUpdateState.value = AppUpdateState.Downloading(release, 0)
            runCatching {
                appUpdateRepository.download(release) { progress ->
                    _appUpdateState.value = AppUpdateState.Downloading(release, progress)
                }
            }.onSuccess { file ->
                settingsRepository.setDismissedUpdateVersion(null)
                _appUpdateState.value = AppUpdateState.Downloaded(release, file)
            }.onFailure { error ->
                _appUpdateState.value = AppUpdateState.Error(error.message ?: "De update kon niet worden gedownload.")
            }
        }
    }

    fun clearAppUpdateStatus() {
        if (_appUpdateState.value !is AppUpdateState.Downloading) _appUpdateState.value = AppUpdateState.Idle
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
        var movieCount = 0
        var seriesCount = 0
        var epgCount = 0
        val failures = mutableListOf<String>()

        playlists.forEachIndexed { index, playlist ->
            if (syncPlaylists) {
                action.update { it.copy(message = "${playlist.name}: zenders (${index + 1}/${playlists.size})") }
                runCatching { syncPlaylist(playlist.id) }
                    .onSuccess { result -> channelCount += result.channelCount; movieCount += result.movieCount; seriesCount += result.seriesCount }
                    .onFailure { failures += "${playlist.name} zenders: ${it.message ?: "mislukt"}" }
            }
            if (syncEpg) {
                action.update { it.copy(message = "${playlist.name}: EPG (${index + 1}/${playlists.size})") }
                runCatching { syncEpgForPlaylist(playlist.id) }
                    .onSuccess { epgCount += it }
                    .onFailure { failures += "${playlist.name} EPG: ${it.message ?: "mislukt"}" }
            }
        }
        return SyncSummary(channelCount, movieCount, seriesCount, epgCount, failures)
    }

    private suspend fun importResult(playlistId: Long): ImportResult {
        val syncResult = syncPlaylist(playlistId)
        val epgOutcome = runCatching { syncEpgForPlaylist(playlistId) }
        return ImportResult(
            channelCount = syncResult.channelCount,
            movieCount = syncResult.movieCount,
            seriesCount = syncResult.seriesCount,
            epgCount = epgOutcome.getOrDefault(0),
            epgError = epgOutcome.exceptionOrNull()?.message
        )
    }

    private suspend fun syncPlaylist(playlistId: Long): PlaylistSyncResult = withContext(Dispatchers.IO) {
        val playlist = playlistDao.getById(playlistId) ?: error("Playlist niet gevonden.")
        val existing = channelDao.getForPlaylist(playlistId)
        val existingMovies = movieDao.getForPlaylist(playlistId)
        val existingSeries = seriesDao.getForPlaylist(playlistId)
        val xtreamCredentials = XtreamSourceCodec.decode(playlist.m3uUrl)
        val result = if (xtreamCredentials != null) {
            action.update { it.copy(message = "Xtream zenders ophalen") }
            val channels = xtreamClient.loadLiveChannels(xtreamCredentials)
            val channelCount = saveParsedChannels(playlistId, existing, channels.asSequence())
            action.update { it.copy(message = "Xtream films ophalen") }
            val movieOutcome = runCatching { xtreamClient.loadVodMovies(xtreamCredentials) }
            val movieCount = movieOutcome.fold(
                onSuccess = { movies ->
                    movieError.value = null
                    saveParsedMovies(playlistId, existingMovies, movies.asSequence())
                },
                onFailure = { throwable ->
                    movieError.value = throwable.message ?: "Filmbibliotheek kon niet worden bijgewerkt."
                    existingMovies.size
                }
            )
            action.update { it.copy(message = "Xtream series ophalen") }
            val seriesOutcome = runCatching { xtreamClient.loadSeries(xtreamCredentials) }
            val seriesCount = seriesOutcome.fold(
                onSuccess = { parsed ->
                    seriesError.value = null
                    saveParsedSeries(playlistId, existingSeries, parsed)
                },
                onFailure = { throwable ->
                    seriesError.value = throwable.message ?: "Seriesbibliotheek kon niet worden bijgewerkt."
                    existingSeries.size
                }
            )
            PlaylistSyncResult(channelCount, movieCount, seriesCount)
        } else {
            reader.readWithReader(playlist.m3uUrl) { stream ->
                saveParsedMedia(playlistId, existing, existingMovies, parser.parseSequence(stream))
            }
        }

        if (result.channelCount == 0 && result.movieCount == 0 && result.seriesCount == 0) {
            if (xtreamCredentials != null) error("Geen live-zenders, films of series gevonden in Xtream Codes.")
            error("Geen zenders of films gevonden in deze M3U-playlist.")
        }
        playlistDao.updateLastSync(playlistId, System.currentTimeMillis())
        result
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
        val matcher = ExistingChannelMatcher(existing)
        val batch = mutableListOf<ChannelEntity>()
        val iterator = parsedChannels.iterator()
        var count = 0

        if (!iterator.hasNext()) return 0
        channelDao.deleteForPlaylist(playlistId)
        while (iterator.hasNext()) {
            val parsed = iterator.next()
            batch += parsed.toChannelEntity(
                playlistId = playlistId,
                sortOrder = count,
                previous = matcher.match(parsed)
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

    private suspend fun saveParsedMovies(
        playlistId: Long,
        existing: List<MovieEntity>,
        parsedMovies: Sequence<ParsedMovie>
    ): Int {
        val matcher = ExistingMovieMatcher(existing)
        val movies = parsedMovies.mapIndexed { index, movie ->
            movie.toMovieEntity(playlistId, index, matcher.match(movie))
        }.toList()
        movieDao.deleteForPlaylist(playlistId)
        movies.chunked(300).forEach { movieDao.insertAll(it) }
        return movies.size
    }

    private suspend fun saveParsedSeries(playlistId: Long, existing: List<SeriesEntity>, parsed: List<ParsedSeries>): Int {
        if (parsed.isEmpty()) {
            seriesDao.deleteForPlaylist(playlistId)
            return 0
        }
        val matcher = ExistingSeriesMatcher(existing)
        val entities = parsed.mapIndexed { index, item -> item.toSeriesEntity(playlistId, index, matcher.match(item)) }
        seriesDao.upsertAll(entities)
        val keptIds = seriesDao.getForPlaylist(playlistId).filter { current -> parsed.any { it.providerId == current.providerId } }.map { it.id }
        if (keptIds.isEmpty()) seriesDao.deleteForPlaylist(playlistId) else seriesDao.deleteNotIn(playlistId, keptIds)
        return parsed.size
    }

    private suspend fun saveParsedMedia(
        playlistId: Long,
        existingChannels: List<ChannelEntity>,
        existingMovies: List<MovieEntity>,
        parsedItems: Sequence<ParsedChannel>
    ): PlaylistSyncResult {
        val channelMatcher = ExistingChannelMatcher(existingChannels)
        val movieMatcher = ExistingMovieMatcher(existingMovies)
        val channelBatch = ArrayList<ChannelEntity>(500)
        val movieBatch = ArrayList<MovieEntity>(300)
        var channelCount = 0
        var movieCount = 0
        channelDao.deleteForPlaylist(playlistId)
        movieDao.deleteForPlaylist(playlistId)
        for (item in parsedItems) {
            if (item.contentType == ParsedContentType.Movie) {
                val movie = item.toParsedMovie()
                movieBatch += movie.toMovieEntity(playlistId, movieCount++, movieMatcher.match(movie))
                if (movieBatch.size == 300) {
                    movieDao.insertAll(movieBatch)
                    movieBatch.clear()
                }
            } else {
                channelBatch += item.toChannelEntity(playlistId, channelCount++, channelMatcher.match(item))
                if (channelBatch.size == 500) {
                    channelDao.insertAll(channelBatch)
                    channelBatch.clear()
                }
            }
        }
        if (channelBatch.isNotEmpty()) channelDao.insertAll(channelBatch)
        if (movieBatch.isNotEmpty()) movieDao.insertAll(movieBatch)
        return PlaylistSyncResult(channelCount, movieCount, 0)
    }

    private fun buildChannelRows(
        channels: List<ChannelEntity>,
        programs: List<ProgramEntity>,
        filter: ChannelFilters,
        currentTime: Long,
        resolutions: Map<String, StreamResolution>
    ): List<ChannelRowState> {
        val programsByChannel = programs.groupBy { it.playlistId to it.channelTvgId }
        val rows = channels.map { channel ->
            val channelPrograms = channel.tvgId?.let { programsByChannel[channel.playlistId to it] }.orEmpty()
            val current = channelPrograms.firstOrNull { it.startTime <= currentTime && it.endTime > currentTime }
            val upcoming = channelPrograms.filter { it.startTime > currentTime }.take(3)
            val next = upcoming.firstOrNull()
            ChannelRowState(
                channel = channel,
                currentProgram = current,
                nextProgram = next,
                progress = current?.progressAt(currentTime) ?: 0f,
                streamResolution = resolutions[channel.streamMetadataKey()],
                upcomingPrograms = upcoming
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
        programs: List<ProgramEntity>,
        resolutions: Map<String, StreamResolution>
    ): List<GuideChannelState> {
        val programsByChannel = programs.groupBy { it.playlistId to it.channelTvgId }
        return channels.map { channel ->
            val channelPrograms = channel.tvgId
                ?.let { programsByChannel[channel.playlistId to it] }
                .orEmpty()
                .filter { it.endTime > it.startTime }
                .sortedBy { it.startTime }
            GuideChannelState(channel, channelPrograms, resolutions[channel.streamMetadataKey()])
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
        const val MOVIE_WATCHED_THRESHOLD = 0.93
        const val MIN_MOVIE_PROGRESS_MS = 10_000L
        const val SERIES_DETAIL_CACHE_MS = 6L * 60L * 60L * 1000L
    }
}

data class StreamGuideState(
    val playlists: List<PlaylistEntity> = emptyList(),
    val channelRows: List<ChannelRowState> = emptyList(),
    val channels: List<ChannelEntity> = emptyList(),
    val groups: List<String> = emptyList(),
    val allGroups: List<String> = emptyList(),
    val guideRows: List<GuideChannelState> = emptyList(),
    val homeRows: List<ChannelRowState> = emptyList(),
    val movieLibrary: MovieLibraryState = MovieLibraryState(),
    val seriesLibrary: SeriesLibraryState = SeriesLibraryState(),
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
    val progress: Float,
    val streamResolution: StreamResolution? = null,
    val upcomingPrograms: List<ProgramEntity> = emptyList()
)

data class GuideChannelState(
    val channel: ChannelEntity,
    val programs: List<ProgramEntity>,
    val streamResolution: StreamResolution? = null
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

private data class ChannelSnapshot(
    val channels: List<ChannelEntity>,
    val resolutions: Map<String, StreamResolution>
)

private data class GuideSnapshot(
    val rows: List<GuideChannelState>,
    val query: String
)

private data class MovieSource(
    val movies: List<MovieEntity>,
    val hiddenGroups: Set<String>
)

private data class SeriesSource(
    val series: List<SeriesEntity>,
    val episodes: List<EpisodeEntity>,
    val hiddenGroups: Set<String>
)

private data class ImportResult(
    val channelCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val epgCount: Int,
    val epgError: String?
) {
    fun toActionState(prefix: String = "Geladen"): ActionState {
        val message = if (epgCount > 0) {
            "$prefix: $channelCount zenders, $movieCount films, $seriesCount series en $epgCount programma's"
        } else {
            "$prefix: $channelCount zenders, $movieCount films en $seriesCount series"
        }
        return ActionState(message = message, error = epgError?.let { "EPG niet geladen: $it" })
    }
}

private data class SyncSummary(
    val channelCount: Int,
    val movieCount: Int,
    val seriesCount: Int,
    val epgCount: Int,
    val failures: List<String>
) {
    fun toActionState(prefix: String): ActionState {
        val parts = buildList {
            if (channelCount > 0) add("$channelCount zenders")
            if (movieCount > 0) add("$movieCount films")
            if (seriesCount > 0) add("$seriesCount series")
            if (epgCount > 0) add("$epgCount programma's")
        }
        val message = if (parts.isEmpty()) prefix else "$prefix: ${parts.joinToString(" en ")}"
        return ActionState(
            message = message,
            error = failures.takeIf { it.isNotEmpty() }?.joinToString(separator = "\n")
        )
    }
}

private data class PlaylistSyncResult(val channelCount: Int, val movieCount: Int, val seriesCount: Int = 0)

private data class ActionState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

private fun startOfTodayMillis(): Long = LocalDate.now()
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
