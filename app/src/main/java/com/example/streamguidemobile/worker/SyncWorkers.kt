package com.example.streamguidemobile.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.streamguidemobile.data.ChannelEntity
import com.example.streamguidemobile.data.M3uParser
import com.example.streamguidemobile.data.ProgramEntity
import com.example.streamguidemobile.data.StreamGuideDatabase
import com.example.streamguidemobile.data.TextSourceReader
import com.example.streamguidemobile.data.XmltvParser
import com.example.streamguidemobile.data.XtreamClient
import com.example.streamguidemobile.data.XtreamSourceCodec
import java.io.IOException
import java.util.concurrent.TimeUnit

class PlaylistSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val database = StreamGuideDatabase.get(applicationContext)
        val playlists = database.playlistDao().getAllOnce()
        if (playlists.isEmpty()) return Result.success()
        var shouldRetry = false
        var failed = false

        playlists.forEach { playlist ->
            runCatching {
            val channels = database.channelDao()
            val existing = channels.getForPlaylist(playlist.id)
            val favoriteTvgIds = existing.filter { it.isFavorite }.mapNotNull { it.tvgId }.toSet()
            val favoriteUrls = existing.filter { it.isFavorite }.map { it.streamUrl }.toSet()
            val reader = TextSourceReader(applicationContext)
            val credentials = XtreamSourceCodec.decode(playlist.m3uUrl)
            if (credentials != null) {
                replaceChannels(
                    database = database,
                    playlistId = playlist.id,
                    favoriteTvgIds = favoriteTvgIds,
                    favoriteUrls = favoriteUrls,
                    parsed = XtreamClient(reader).loadLiveChannels(credentials).asSequence()
                )
            } else {
                reader.readWithReader(playlist.m3uUrl) { source ->
                    replaceChannels(
                        database = database,
                        playlistId = playlist.id,
                        favoriteTvgIds = favoriteTvgIds,
                        favoriteUrls = favoriteUrls,
                        parsed = M3uParser().parseSequence(source)
                    )
                }
            }
            database.playlistDao().updateLastSync(playlist.id, System.currentTimeMillis())
            }.onFailure { error ->
                if (error is IOException) shouldRetry = true else failed = true
            }
        }
        return when {
            shouldRetry -> Result.retry()
            failed -> Result.failure()
            else -> Result.success()
        }
    }

    private suspend fun replaceChannels(
        database: StreamGuideDatabase,
        playlistId: Long,
        favoriteTvgIds: Set<String>,
        favoriteUrls: Set<String>,
        parsed: Sequence<com.example.streamguidemobile.data.ParsedChannel>
    ) {
        val channelDao = database.channelDao()
        val iterator = parsed.iterator()
        if (!iterator.hasNext()) return

        channelDao.deleteForPlaylist(playlistId)
        val batch = mutableListOf<ChannelEntity>()
        var index = 0
        while (iterator.hasNext()) {
            val channel = iterator.next()
            batch += ChannelEntity(
                playlistId = playlistId,
                name = channel.name,
                tvgId = channel.tvgId,
                tvgName = channel.tvgName,
                logoUrl = channel.logoUrl,
                groupTitle = channel.groupTitle,
                streamUrl = channel.streamUrl,
                isFavorite = channel.tvgId in favoriteTvgIds || channel.streamUrl in favoriteUrls,
                sortOrder = index++
            )
            if (batch.size >= 500) {
                channelDao.insertAll(batch.toList())
                batch.clear()
            }
        }
        if (batch.isNotEmpty()) channelDao.insertAll(batch)
    }
}

class EpgSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val database = StreamGuideDatabase.get(applicationContext)
        val playlists = database.playlistDao().getAllOnce()
        if (playlists.isEmpty()) return Result.success()
        var shouldRetry = false
        var failed = false

        playlists.forEach { playlist ->
            val xtreamCredentials = XtreamSourceCodec.decode(playlist.m3uUrl)
            val epgUrl = playlist.epgUrl?.trim()?.takeIf { it.isNotEmpty() }
                ?: xtreamCredentials?.let { XtreamSourceCodec.xmltvUrl(it) }
                ?: return@forEach
            runCatching {
                val knownTvgIds = database.channelDao().getForPlaylist(playlist.id).mapNotNull { it.tvgId }.toSet()
                val now = System.currentTimeMillis()
                val fromTime = now - THREE_DAYS
                val toTime = now + SEVEN_DAYS
                val reader = TextSourceReader(applicationContext)
                val programs = reader.readWithReader(epgUrl) { XmltvParser().parse(it) }
                    .asSequence()
                    .filter { it.endTime >= fromTime && it.startTime <= toTime }
                    .filter { knownTvgIds.isEmpty() || it.channelTvgId in knownTvgIds }
                    .map {
                        ProgramEntity(
                            playlistId = playlist.id,
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
                database.programDao().replaceForPlaylist(playlist.id, programs)
            }.onFailure { error ->
                if (error is IOException) shouldRetry = true else failed = true
            }
        }
        return when {
            shouldRetry -> Result.retry()
            failed -> Result.failure()
            else -> Result.success()
        }
    }

    private companion object {
        const val ONE_HOUR = 60L * 60L * 1000L
        const val THREE_DAYS = 3L * 24L * ONE_HOUR
        const val SEVEN_DAYS = 7L * 24L * ONE_HOUR
    }
}

object SyncScheduler {
    fun schedule(context: Context) {
        val workManager = WorkManager.getInstance(context.applicationContext)
        val playlistRequest = PeriodicWorkRequestBuilder<PlaylistSyncWorker>(24, TimeUnit.HOURS).build()
        val epgRequest = PeriodicWorkRequestBuilder<EpgSyncWorker>(12, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("streamguide_playlist_sync", ExistingPeriodicWorkPolicy.KEEP, playlistRequest)
        workManager.enqueueUniquePeriodicWork("streamguide_epg_sync", ExistingPeriodicWorkPolicy.KEEP, epgRequest)
    }
}
