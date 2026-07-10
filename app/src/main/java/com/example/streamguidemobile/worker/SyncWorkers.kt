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
        val playlist = database.playlistDao().getAllOnce().firstOrNull() ?: return Result.success()
        return runCatching {
            val channels = database.channelDao()
            val existing = channels.getForPlaylist(playlist.id)
            val favoriteTvgIds = existing.filter { it.isFavorite }.mapNotNull { it.tvgId }.toSet()
            val favoriteUrls = existing.filter { it.isFavorite }.map { it.streamUrl }.toSet()
            val reader = TextSourceReader(applicationContext)
            val parsed = XtreamSourceCodec.decode(playlist.m3uUrl)?.let { XtreamClient(reader).loadLiveChannels(it) }
                ?: reader.readWithReader(playlist.m3uUrl) { M3uParser().parseSequence(it).toList() }

            val entities = parsed.mapIndexed { index, channel ->
                ChannelEntity(
                    playlistId = playlist.id,
                    name = channel.name,
                    tvgId = channel.tvgId,
                    tvgName = channel.tvgName,
                    logoUrl = channel.logoUrl,
                    groupTitle = channel.groupTitle,
                    streamUrl = channel.streamUrl,
                    isFavorite = channel.tvgId in favoriteTvgIds || channel.streamUrl in favoriteUrls,
                    sortOrder = index
                )
            }
            channels.replaceForPlaylist(playlist.id, entities)
            database.playlistDao().updateLastSync(playlist.id, System.currentTimeMillis())
            Result.success()
        }.getOrElse { error ->
            if (error is IOException) Result.retry() else Result.failure()
        }
    }
}

class EpgSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val database = StreamGuideDatabase.get(applicationContext)
        val playlist = database.playlistDao().getAllOnce().firstOrNull() ?: return Result.success()
        val xtreamCredentials = XtreamSourceCodec.decode(playlist.m3uUrl)
        val epgUrl = playlist.epgUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: xtreamCredentials?.let { XtreamSourceCodec.xmltvUrl(it) }
            ?: return Result.success()
        return runCatching {
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
            Result.success()
        }.getOrElse { error ->
            if (error is IOException) Result.retry() else Result.failure()
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