package com.example.streamguidemobile.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val m3uUrl: String,
    val epgUrl: String? = null,
    val createdAt: Long,
    @ColumnInfo(defaultValue = "0") val updatedAt: Long = createdAt,
    val lastSyncAt: Long?
)

@Entity(
    tableName = "channels",
    foreignKeys = [ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId"), Index("groupTitle"), Index("tvgId"), Index("streamUrl"), Index("lastWatchedAt")]
)
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val name: String,
    val tvgId: String?,
    val tvgName: String?,
    val logoUrl: String?,
    val groupTitle: String,
    val streamUrl: String,
    val isFavorite: Boolean = false,
    val lastWatchedAt: Long? = null,
    val sortOrder: Int
)

@Entity(
    tableName = "programs",
    foreignKeys = [ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId"), Index("channelTvgId"), Index("startTime"), Index("endTime")]
)
data class ProgramEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val channelTvgId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val category: String?,
    val iconUrl: String?
)

@Entity(
    tableName = "movies",
    foreignKeys = [ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [
        Index("playlistId"),
        Index("categoryName"),
        Index("addedAt"),
        Index("lastWatchedAt"),
        Index(value = ["playlistId", "providerId"], unique = true)
    ]
)
data class MovieEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val providerId: String?,
    val title: String,
    val originalTitle: String?,
    val streamUrl: String,
    val categoryId: String?,
    val categoryName: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val year: Int?,
    val durationMinutes: Int?,
    val genre: String?,
    val ageRating: String?,
    val description: String?,
    val rating: Double?,
    val director: String?,
    val cast: String?,
    val trailerUrl: String?,
    val addedAt: Long?,
    val containerExtension: String?,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    val lastWatchedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val playbackPositionMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val playbackDurationMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val isWatched: Boolean = false,
    val resolutionWidth: Int? = null,
    val resolutionHeight: Int? = null,
    val sortOrder: Int
)

@Entity(
    tableName = "series",
    foreignKeys = [ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId"), Index("categoryName"), Index("addedAt"), Index("lastWatchedAt"), Index(value = ["playlistId", "providerId"], unique = true)]
)
data class SeriesEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val playlistId: Long,
    val providerId: String,
    val title: String,
    val originalTitle: String? = null,
    val categoryId: String? = null,
    val categoryName: String,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val genre: String? = null,
    val ageRating: String? = null,
    val description: String? = null,
    val rating: Double? = null,
    val director: String? = null,
    val cast: String? = null,
    val trailerUrl: String? = null,
    val addedAt: Long? = null,
    val updatedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val isFavorite: Boolean = false,
    val lastWatchedAt: Long? = null,
    val progressEpisodeId: Long? = null,
    @ColumnInfo(defaultValue = "-1") val progressOrder: Int = -1,
    val detailLoadedAt: Long? = null,
    val sortOrder: Int
)

@Entity(
    tableName = "episodes",
    foreignKeys = [ForeignKey(entity = SeriesEntity::class, parentColumns = ["id"], childColumns = ["seriesId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("seriesId"), Index("seasonNumber"), Index("lastWatchedAt"), Index(value = ["seriesId", "providerId"], unique = true)]
)
data class EpisodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seriesId: Long,
    val providerId: String,
    val seasonNumber: Int,
    val seasonName: String,
    val episodeNumber: Int? = null,
    val providerOrder: Int,
    val title: String,
    val streamUrl: String? = null,
    val imageUrl: String? = null,
    val description: String? = null,
    val durationMinutes: Int? = null,
    val containerExtension: String? = null,
    val addedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val playbackPositionMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val playbackDurationMs: Long = 0L,
    @ColumnInfo(defaultValue = "0") val isWatched: Boolean = false,
    val lastWatchedAt: Long? = null,
    val resolutionWidth: Int? = null,
    val resolutionHeight: Int? = null,
    val sortOrder: Int
)

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC") fun observeAll(): Flow<List<PlaylistEntity>>
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC") suspend fun getAllOnce(): List<PlaylistEntity>
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1") suspend fun getById(id: Long): PlaylistEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(playlist: PlaylistEntity): Long
    @Query("UPDATE playlists SET lastSyncAt = :lastSyncAt, updatedAt = :lastSyncAt WHERE id = :id") suspend fun updateLastSync(id: Long, lastSyncAt: Long)
    @Query("UPDATE playlists SET epgUrl = :epgUrl, updatedAt = :updatedAt WHERE id = :id") suspend fun updateEpgUrl(id: Long, epgUrl: String?, updatedAt: Long)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY sortOrder ASC, name COLLATE NOCASE ASC") fun observeAll(): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC") fun observeFavorites(): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels WHERE groupTitle = :groupTitle ORDER BY sortOrder ASC, name COLLATE NOCASE ASC") fun observeByGroup(groupTitle: String): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels WHERE lastWatchedAt IS NOT NULL ORDER BY lastWatchedAt DESC LIMIT 100") fun observeRecent(): Flow<List<ChannelEntity>>
    @Query("SELECT DISTINCT groupTitle FROM channels ORDER BY groupTitle COLLATE NOCASE ASC") fun observeGroups(): Flow<List<String>>
    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1") suspend fun getById(id: Long): ChannelEntity?
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId") suspend fun getForPlaylist(playlistId: Long): List<ChannelEntity>
    @Query("SELECT id FROM channels ORDER BY sortOrder ASC, name COLLATE NOCASE ASC") suspend fun getChannelIds(): List<Long>
    @Query("UPDATE channels SET isFavorite = :favorite WHERE id = :id") suspend fun setFavorite(id: Long, favorite: Boolean)
    @Query("UPDATE channels SET lastWatchedAt = :timestamp WHERE id = :id") suspend fun markWatched(id: Long, timestamp: Long)
    @Query("UPDATE channels SET lastWatchedAt = NULL WHERE id = :id") suspend fun clearWatchHistory(id: Long)
    @Query("UPDATE channels SET lastWatchedAt = NULL") suspend fun clearAllWatchHistory()
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(channels: List<ChannelEntity>)
    @Query("DELETE FROM channels WHERE playlistId = :playlistId") suspend fun deleteForPlaylist(playlistId: Long)
    @Transaction suspend fun replaceForPlaylist(playlistId: Long, channels: List<ChannelEntity>) {
        deleteForPlaylist(playlistId)
        if (channels.isNotEmpty()) insertAll(channels)
    }
}

@Dao
interface ProgramDao {
    @Query("SELECT * FROM programs WHERE endTime >= :fromTime AND startTime <= :toTime ORDER BY startTime ASC") fun observeWindow(fromTime: Long, toTime: Long): Flow<List<ProgramEntity>>
    @Query("SELECT * FROM programs WHERE playlistId = :playlistId AND endTime >= :fromTime AND startTime <= :toTime ORDER BY channelTvgId COLLATE NOCASE ASC, startTime ASC") suspend fun getWindowForPlaylist(playlistId: Long, fromTime: Long, toTime: Long): List<ProgramEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(programs: List<ProgramEntity>)
    @Query("DELETE FROM programs WHERE playlistId = :playlistId") suspend fun deleteForPlaylist(playlistId: Long)
    @Transaction suspend fun replaceForPlaylist(playlistId: Long, programs: List<ProgramEntity>) {
        deleteForPlaylist(playlistId)
        programs.chunked(500).forEach { insertAll(it) }
    }
}

@Dao
interface MovieDao {
    @Query("SELECT * FROM movies ORDER BY sortOrder ASC, title COLLATE NOCASE ASC") fun observeAll(): Flow<List<MovieEntity>>
    @Query("SELECT DISTINCT categoryName FROM movies WHERE TRIM(categoryName) != '' ORDER BY categoryName COLLATE NOCASE ASC") suspend fun getAllGroups(): List<String>
    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1") fun observeById(id: Long): Flow<MovieEntity?>
    @Query("SELECT * FROM movies WHERE id = :id LIMIT 1") suspend fun getById(id: Long): MovieEntity?
    @Query("SELECT * FROM movies WHERE playlistId = :playlistId") suspend fun getForPlaylist(playlistId: Long): List<MovieEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(movies: List<MovieEntity>)
    @Update suspend fun update(movie: MovieEntity)
    @Query("UPDATE movies SET isFavorite = :favorite WHERE id = :id") suspend fun setFavorite(id: Long, favorite: Boolean)
    @Query("UPDATE movies SET playbackPositionMs = :positionMs, playbackDurationMs = :durationMs, isWatched = :watched, lastWatchedAt = :watchedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, positionMs: Long, durationMs: Long, watched: Boolean, watchedAt: Long?)
    @Query("UPDATE movies SET playbackPositionMs = 0, playbackDurationMs = 0, isWatched = 0, lastWatchedAt = NULL")
    suspend fun clearAllProgress()
    @Query("UPDATE movies SET resolutionWidth = :width, resolutionHeight = :height WHERE id = :id")
    suspend fun updateResolution(id: Long, width: Int, height: Int)
    @Query("DELETE FROM movies WHERE playlistId = :playlistId") suspend fun deleteForPlaylist(playlistId: Long)
}

@Dao
interface SeriesDao {
    @Query("SELECT * FROM series ORDER BY sortOrder ASC, title COLLATE NOCASE ASC") fun observeAll(): Flow<List<SeriesEntity>>
    @Query("SELECT * FROM series WHERE id = :id LIMIT 1") suspend fun getById(id: Long): SeriesEntity?
    @Query("SELECT * FROM series WHERE playlistId = :playlistId") suspend fun getForPlaylist(playlistId: Long): List<SeriesEntity>
    @Query("SELECT DISTINCT categoryName FROM series WHERE TRIM(categoryName) != '' ORDER BY categoryName COLLATE NOCASE ASC") suspend fun getAllGroups(): List<String>
    @Upsert suspend fun upsertAll(series: List<SeriesEntity>)
    @Update suspend fun update(series: SeriesEntity)
    @Query("UPDATE series SET isFavorite = :favorite WHERE id = :id") suspend fun setFavorite(id: Long, favorite: Boolean)
    @Query("UPDATE series SET progressEpisodeId = :episodeId, progressOrder = :episodeOrder, lastWatchedAt = :watchedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, episodeId: Long?, episodeOrder: Int, watchedAt: Long?)
    @Query("UPDATE series SET progressEpisodeId = NULL, progressOrder = -1, lastWatchedAt = NULL")
    suspend fun clearAllProgress()
    @Query("DELETE FROM series WHERE playlistId = :playlistId AND id NOT IN (:keptIds)") suspend fun deleteNotIn(playlistId: Long, keptIds: List<Long>)
    @Query("DELETE FROM series WHERE playlistId = :playlistId") suspend fun deleteForPlaylist(playlistId: Long)
}

@Dao
interface EpisodeDao {
    @Query("SELECT * FROM episodes ORDER BY seriesId ASC, sortOrder ASC") fun observeAll(): Flow<List<EpisodeEntity>>
    @Query("SELECT * FROM episodes WHERE id = :id LIMIT 1") suspend fun getById(id: Long): EpisodeEntity?
    @Query("SELECT * FROM episodes WHERE seriesId = :seriesId ORDER BY sortOrder ASC") suspend fun getForSeries(seriesId: Long): List<EpisodeEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(episodes: List<EpisodeEntity>)
    @Query("DELETE FROM episodes WHERE seriesId = :seriesId") suspend fun deleteForSeries(seriesId: Long)
    @Query("UPDATE episodes SET playbackPositionMs = :positionMs, playbackDurationMs = :durationMs, isWatched = :watched, lastWatchedAt = :watchedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, positionMs: Long, durationMs: Long, watched: Boolean, watchedAt: Long?)
    @Query("UPDATE episodes SET resolutionWidth = :width, resolutionHeight = :height WHERE id = :id")
    suspend fun updateResolution(id: Long, width: Int, height: Int)
    @Query("UPDATE episodes SET isWatched = :watched, playbackPositionMs = CASE WHEN :watched THEN CASE WHEN playbackDurationMs > 0 THEN playbackDurationMs ELSE 1 END ELSE 0 END, playbackDurationMs = CASE WHEN :watched THEN CASE WHEN playbackDurationMs > 0 THEN playbackDurationMs ELSE 1 END ELSE 0 END, lastWatchedAt = CASE WHEN :watched THEN :watchedAt ELSE NULL END WHERE id = :id")
    suspend fun setWatched(id: Long, watched: Boolean, watchedAt: Long)
    @Query("UPDATE episodes SET isWatched = :watched, playbackPositionMs = CASE WHEN :watched THEN CASE WHEN playbackDurationMs > 0 THEN playbackDurationMs ELSE 1 END ELSE 0 END, playbackDurationMs = CASE WHEN :watched THEN CASE WHEN playbackDurationMs > 0 THEN playbackDurationMs ELSE 1 END ELSE 0 END, lastWatchedAt = CASE WHEN :watched THEN :watchedAt ELSE NULL END WHERE seriesId = :seriesId AND seasonNumber = :seasonNumber")
    suspend fun setSeasonWatched(seriesId: Long, seasonNumber: Int, watched: Boolean, watchedAt: Long)
    @Query("UPDATE episodes SET isWatched = :watched, playbackPositionMs = CASE WHEN :watched THEN CASE WHEN playbackDurationMs > 0 THEN playbackDurationMs ELSE 1 END ELSE 0 END, playbackDurationMs = CASE WHEN :watched THEN CASE WHEN playbackDurationMs > 0 THEN playbackDurationMs ELSE 1 END ELSE 0 END, lastWatchedAt = CASE WHEN :watched THEN :watchedAt ELSE NULL END WHERE seriesId = :seriesId")
    suspend fun setSeriesWatched(seriesId: Long, watched: Boolean, watchedAt: Long)
    @Query("UPDATE episodes SET playbackPositionMs = 0, playbackDurationMs = 0, isWatched = 0, lastWatchedAt = NULL")
    suspend fun clearAllProgress()
}

@Database(entities = [PlaylistEntity::class, ChannelEntity::class, ProgramEntity::class, MovieEntity::class, SeriesEntity::class, EpisodeEntity::class], version = 4, exportSchema = false)
abstract class StreamGuideDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun programDao(): ProgramDao
    abstract fun movieDao(): MovieDao
    abstract fun seriesDao(): SeriesDao
    abstract fun episodeDao(): EpisodeDao

    companion object {
        @Volatile private var instance: StreamGuideDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE playlists ADD COLUMN epgUrl TEXT")
                database.execSQL("ALTER TABLE playlists ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS programs (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "playlistId INTEGER NOT NULL, " +
                        "channelTvgId TEXT NOT NULL, " +
                        "title TEXT NOT NULL, " +
                        "description TEXT, " +
                        "startTime INTEGER NOT NULL, " +
                        "endTime INTEGER NOT NULL, " +
                        "category TEXT, " +
                        "iconUrl TEXT, " +
                        "FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_programs_playlistId ON programs(playlistId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_programs_channelTvgId ON programs(channelTvgId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_programs_startTime ON programs(startTime)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_programs_endTime ON programs(endTime)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_channels_lastWatchedAt ON channels(lastWatchedAt)")
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS movies (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "playlistId INTEGER NOT NULL, providerId TEXT, title TEXT NOT NULL, originalTitle TEXT, " +
                        "streamUrl TEXT NOT NULL, categoryId TEXT, categoryName TEXT NOT NULL, posterUrl TEXT, backdropUrl TEXT, " +
                        "year INTEGER, durationMinutes INTEGER, genre TEXT, ageRating TEXT, description TEXT, rating REAL, " +
                        "director TEXT, `cast` TEXT, trailerUrl TEXT, addedAt INTEGER, containerExtension TEXT, " +
                        "isFavorite INTEGER NOT NULL DEFAULT 0, lastWatchedAt INTEGER, playbackPositionMs INTEGER NOT NULL DEFAULT 0, " +
                        "playbackDurationMs INTEGER NOT NULL DEFAULT 0, isWatched INTEGER NOT NULL DEFAULT 0, " +
                        "resolutionWidth INTEGER, resolutionHeight INTEGER, sortOrder INTEGER NOT NULL, " +
                        "FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_movies_playlistId ON movies(playlistId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_movies_categoryName ON movies(categoryName)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_movies_addedAt ON movies(addedAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_movies_lastWatchedAt ON movies(lastWatchedAt)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_movies_playlistId_providerId ON movies(playlistId, providerId)")
            }
        }

        private val migration3To4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS series (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, playlistId INTEGER NOT NULL, providerId TEXT NOT NULL, title TEXT NOT NULL, originalTitle TEXT, categoryId TEXT, categoryName TEXT NOT NULL, posterUrl TEXT, backdropUrl TEXT, year INTEGER, genre TEXT, ageRating TEXT, description TEXT, rating REAL, director TEXT, `cast` TEXT, trailerUrl TEXT, addedAt INTEGER, updatedAt INTEGER, isFavorite INTEGER NOT NULL DEFAULT 0, lastWatchedAt INTEGER, progressEpisodeId INTEGER, progressOrder INTEGER NOT NULL DEFAULT -1, detailLoadedAt INTEGER, sortOrder INTEGER NOT NULL, FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_series_playlistId ON series(playlistId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_series_categoryName ON series(categoryName)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_series_addedAt ON series(addedAt)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_series_lastWatchedAt ON series(lastWatchedAt)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_series_playlistId_providerId ON series(playlistId, providerId)")
                database.execSQL("CREATE TABLE IF NOT EXISTS episodes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, seriesId INTEGER NOT NULL, providerId TEXT NOT NULL, seasonNumber INTEGER NOT NULL, seasonName TEXT NOT NULL, episodeNumber INTEGER, providerOrder INTEGER NOT NULL, title TEXT NOT NULL, streamUrl TEXT, imageUrl TEXT, description TEXT, durationMinutes INTEGER, containerExtension TEXT, addedAt INTEGER, playbackPositionMs INTEGER NOT NULL DEFAULT 0, playbackDurationMs INTEGER NOT NULL DEFAULT 0, isWatched INTEGER NOT NULL DEFAULT 0, lastWatchedAt INTEGER, resolutionWidth INTEGER, resolutionHeight INTEGER, sortOrder INTEGER NOT NULL, FOREIGN KEY(seriesId) REFERENCES series(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_seriesId ON episodes(seriesId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_seasonNumber ON episodes(seasonNumber)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_episodes_lastWatchedAt ON episodes(lastWatchedAt)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_episodes_seriesId_providerId ON episodes(seriesId, providerId)")
            }
        }

        fun get(context: Context): StreamGuideDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StreamGuideDatabase::class.java, "streamguide.db")
                .addMigrations(migration1To2, migration2To3, migration3To4)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
