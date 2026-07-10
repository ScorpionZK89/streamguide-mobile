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

@Database(entities = [PlaylistEntity::class, ChannelEntity::class, ProgramEntity::class], version = 2, exportSchema = false)
abstract class StreamGuideDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao
    abstract fun programDao(): ProgramDao

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

        fun get(context: Context): StreamGuideDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StreamGuideDatabase::class.java, "streamguide.db")
                .addMigrations(migration1To2)
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}