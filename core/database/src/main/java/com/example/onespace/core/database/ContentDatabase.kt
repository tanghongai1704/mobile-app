package com.example.onespace.core.database

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "video_progress")
data class VideoProgressEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val positionMs: Long,
    val updatedAt: Long
)

@Entity(tableName = "watchlist")
data class WatchlistEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val savedAt: Long
)

@Dao
interface VideoProgressDao {
    @Query("SELECT * FROM video_progress ORDER BY updatedAt DESC LIMIT 10")
    fun observeRecent(): Flow<List<VideoProgressEntity>>

    @Query("SELECT * FROM video_progress WHERE videoId = :videoId")
    suspend fun find(videoId: String): VideoProgressEntity?

    @Query("SELECT positionMs FROM video_progress WHERE videoId = :videoId")
    suspend fun positionOf(videoId: String): Long?

    @Upsert
    suspend fun upsert(entity: VideoProgressEntity)

    @Query("DELETE FROM video_progress WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}

@Dao
interface WatchlistDao {
    @Query("SELECT * FROM watchlist ORDER BY savedAt DESC")
    fun observeAll(): Flow<List<WatchlistEntity>>

    @Upsert
    suspend fun upsert(entity: WatchlistEntity)

    @Query("DELETE FROM watchlist WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}

@Database(
    entities = [VideoProgressEntity::class, WatchlistEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun videoProgressDao(): VideoProgressDao
    abstract fun watchlistDao(): WatchlistDao
}
