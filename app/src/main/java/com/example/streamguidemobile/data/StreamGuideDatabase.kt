package com.example.streamguidemobile.data

import android.content.Context
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
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val m3uUrl: String,
    val createdAt: Long,
    val lastSyncAt: Long?
)

@Entity(
    tableName = "channels",
    foreignKeys = [ForeignKey(entity = PlaylistEntity::class, parentColumns = ["id"], childColumns = ["playlistId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("playlistId"), Index("groupTitle"), Index("tvgId"), Index("streamUrl")]
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

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC") fun observeAll(): Flow<List<PlaylistEntity>>
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC") suspend fun getAllOnce(): List<PlaylistEntity>
    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1") suspend fun getById(id: Long): PlaylistEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(playlist: PlaylistEntity): Long
    @Query("UPDATE playlists SET lastSyncAt = :lastSyncAt WHERE id = :id") suspend fun updateLastSync(id: Long, lastSyncAt: Long)
    @Query("DELETE FROM playlists WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY sortOrder ASC, name COLLATE NOCASE ASC") fun observeAll(): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name COLLATE NOCASE ASC") fun observeFavorites(): Flow<List<ChannelEntity>>
    @Query("SELECT * FROM channels WHERE name LIKE '%' || :query || '%' OR groupTitle LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE ASC") fun search(query: String): Flow<List<ChannelEntity>>
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

@Database(entities = [PlaylistEntity::class, ChannelEntity::class], version = 1, exportSchema = false)
abstract class StreamGuideDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun channelDao(): ChannelDao

    companion object {
        @Volatile private var instance: StreamGuideDatabase? = null
        fun get(context: Context): StreamGuideDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, StreamGuideDatabase::class.java, "streamguide.db")
                .fallbackToDestructiveMigration()
                .build()
                .also { instance = it }
        }
    }
}
