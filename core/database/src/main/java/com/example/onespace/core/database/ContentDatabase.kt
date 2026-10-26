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

@Entity(tableName = "point_entries")
data class PointEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val source: String,
    val amount: Int,
    val timestamp: Long
)

@Entity(tableName = "daily_missions")
data class DailyMissionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val targetType: String,
    val target: Int,
    val progress: Int = 0,
    val completed: Boolean = false,
    val date: String
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

/** F-VID-10: audio "extracted" from a video — a reference played in
 * audio-only mode, not a real MP3 file on disk. */
@Entity(tableName = "extracted_audio")
data class ExtractedAudioEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val creator: String,
    val url: String,
    val duration: String,
    val extractedAt: Long
)

@Dao
interface ExtractedAudioDao {
    @Query("SELECT * FROM extracted_audio ORDER BY extractedAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<ExtractedAudioEntity>>

    @Upsert
    suspend fun upsert(entity: ExtractedAudioEntity)

    @Query("DELETE FROM extracted_audio WHERE videoId = :videoId")
    suspend fun delete(videoId: String)
}

/** F-VID-07 / F-MUS-12: "downloaded" flag per content item, one table for
 * all sections (video | music | news) so the profile page can list them together. */
@Entity(tableName = "downloaded_content")
data class DownloadedContentEntity(
    @PrimaryKey val contentId: String,
    val type: String,
    val title: String,
    val subtitle: String,
    val downloadedAt: Long
)

@Dao
interface DownloadedContentDao {
    @Query("SELECT * FROM downloaded_content ORDER BY downloadedAt DESC")
    fun observeAll(): kotlinx.coroutines.flow.Flow<List<DownloadedContentEntity>>

    @Query("SELECT * FROM downloaded_content WHERE type = :type ORDER BY downloadedAt DESC")
    fun observeByType(type: String): kotlinx.coroutines.flow.Flow<List<DownloadedContentEntity>>

    @Upsert
    suspend fun upsert(entity: DownloadedContentEntity)

    @Query("DELETE FROM downloaded_content WHERE contentId = :contentId")
    suspend fun delete(contentId: String)
}

@Dao
interface PointDao {
    @Query("SELECT COALESCE(SUM(amount), 0) FROM point_entries")
    fun observeTotal(): Flow<Int>

    @Query("SELECT * FROM point_entries ORDER BY timestamp DESC LIMIT 20")
    fun observeRecent(): Flow<List<PointEntryEntity>>

    @Upsert
    suspend fun insert(entity: PointEntryEntity)

    @Query("SELECT COALESCE(SUM(amount), 0) FROM point_entries")
    suspend fun total(): Int
}

@Dao
interface DailyMissionDao {
    @Query("SELECT * FROM daily_missions WHERE date = :date")
    fun observeForDate(date: String): Flow<List<DailyMissionEntity>>

    @Query("SELECT * FROM daily_missions WHERE date = :date")
    suspend fun forDate(date: String): List<DailyMissionEntity>

    @Upsert
    suspend fun upsertAll(missions: List<DailyMissionEntity>)

    @Query("UPDATE daily_missions SET progress = :progress, completed = :completed WHERE id = :id AND date = :date")
    suspend fun updateProgress(id: String, date: String, progress: Int, completed: Boolean)
}

@Database(
    entities = [
        VideoProgressEntity::class,
        WatchlistEntity::class,
        PointEntryEntity::class,
        DailyMissionEntity::class,
        ExtractedAudioEntity::class,
        DownloadedContentEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun videoProgressDao(): VideoProgressDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun extractedAudioDao(): ExtractedAudioDao
    abstract fun downloadedContentDao(): DownloadedContentDao
    abstract fun pointDao(): PointDao
    abstract fun dailyMissionDao(): DailyMissionDao
}
