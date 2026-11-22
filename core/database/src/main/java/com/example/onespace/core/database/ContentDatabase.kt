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

    /** Rows written after [since] (epoch ms), oldest first: what the sync worker still has to upload. */
    @Query("SELECT * FROM video_progress WHERE updatedAt > :since ORDER BY updatedAt ASC")
    suspend fun changedSince(since: Long): List<VideoProgressEntity>
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

/** Offline-first cache of the news feed: the UI reads this table, the network only refreshes it. */
@Entity(tableName = "news_articles")
data class NewsArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val readTime: String,
    val body: String,
    /** Quiz questions serialised as JSON by the news data module. */
    val quizJson: String,
    val publishedAt: Long,
    /** Position in the server feed so the cached order matches the last refresh. */
    val sortOrder: Int,
    val cachedAt: Long
)

@Dao
interface NewsArticleDao {
    @Query("SELECT * FROM news_articles ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<NewsArticleEntity>>

    @Query("SELECT COUNT(*) FROM news_articles")
    suspend fun count(): Int

    @Upsert
    suspend fun upsertAll(articles: List<NewsArticleEntity>)

    @Query("DELETE FROM news_articles WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<String>)
}

/** Unfinished game rounds (board, score, rules version) restored after process death. */
@Entity(tableName = "game_snapshots")
data class GameSnapshotEntity(
    @PrimaryKey val gameId: String,
    val version: Int,
    /** Board cells joined with commas, row-major. */
    val board: String,
    val score: Int,
    val reachedTarget: Boolean,
    val savedAt: Long
)

@Dao
interface GameSnapshotDao {
    @Query("SELECT * FROM game_snapshots WHERE gameId = :gameId")
    suspend fun find(gameId: String): GameSnapshotEntity?

    @Upsert
    suspend fun upsert(snapshot: GameSnapshotEntity)

    @Query("DELETE FROM game_snapshots WHERE gameId = :gameId")
    suspend fun delete(gameId: String)
}

/** Offline-first cache of the video and music catalogue (`type` = video | music). */
@Entity(tableName = "catalog_items")
data class CatalogItemEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val creator: String,
    val duration: String,
    val url: String,
    val topic: String,
    /** Lyric lines as a JSON array; "[]" when there are none. */
    val lyricsJson: String,
    val bpm: Int?,
    val sortOrder: Int,
    val cachedAt: Long
)

@Dao
interface CatalogItemDao {
    @Query("SELECT * FROM catalog_items WHERE type = :type ORDER BY sortOrder ASC")
    fun observeByType(type: String): Flow<List<CatalogItemEntity>>

    @Query("SELECT COUNT(*) FROM catalog_items WHERE type = :type")
    suspend fun count(type: String): Int

    @Query("SELECT * FROM catalog_items WHERE id = :id")
    suspend fun find(id: String): CatalogItemEntity?

    @Upsert
    suspend fun upsertAll(items: List<CatalogItemEntity>)

    @Query("DELETE FROM catalog_items WHERE type = :type AND id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(type: String, keepIds: List<String>)
}

/**
 * Outbox of scored actions not yet sent to `POST /me/events`. [eventId] is created on the device,
 * so the server counts a retried event once.
 */
@Entity(tableName = "pending_events")
data class PendingEventEntity(
    @PrimaryKey val eventId: String,
    val source: String,
    val amount: Int,
    val missionType: String?,
    val createdAt: Long
)

@Dao
interface PendingEventDao {
    @Query("SELECT * FROM pending_events ORDER BY createdAt ASC LIMIT :limit")
    suspend fun oldest(limit: Int): List<PendingEventEntity>

    @Query("SELECT COUNT(*) FROM pending_events")
    fun observeCount(): Flow<Int>

    @Upsert
    suspend fun insert(event: PendingEventEntity)

    @Query("DELETE FROM pending_events WHERE eventId = :eventId")
    suspend fun delete(eventId: String)
}

/** Server id and version of each playlist the device has synced; [itemsHash] detects local edits. */
@Entity(tableName = "playlist_sync")
data class PlaylistSyncEntity(
    @PrimaryKey val name: String,
    val remoteId: String,
    val version: Int,
    val itemsHash: String
)

@Dao
interface PlaylistSyncDao {
    @Query("SELECT * FROM playlist_sync")
    suspend fun all(): List<PlaylistSyncEntity>

    @Upsert
    suspend fun upsert(state: PlaylistSyncEntity)

    @Query("DELETE FROM playlist_sync WHERE name = :name")
    suspend fun delete(name: String)

    @Query("DELETE FROM playlist_sync")
    suspend fun clear()
}

@Database(
    entities = [
        VideoProgressEntity::class,
        WatchlistEntity::class,
        PointEntryEntity::class,
        DailyMissionEntity::class,
        ExtractedAudioEntity::class,
        DownloadedContentEntity::class,
        NewsArticleEntity::class,
        GameSnapshotEntity::class,
        CatalogItemEntity::class,
        PendingEventEntity::class,
        PlaylistSyncEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class ContentDatabase : RoomDatabase() {
    abstract fun videoProgressDao(): VideoProgressDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun extractedAudioDao(): ExtractedAudioDao
    abstract fun downloadedContentDao(): DownloadedContentDao
    abstract fun pointDao(): PointDao
    abstract fun dailyMissionDao(): DailyMissionDao
    abstract fun newsArticleDao(): NewsArticleDao
    abstract fun gameSnapshotDao(): GameSnapshotDao
    abstract fun catalogItemDao(): CatalogItemDao
    abstract fun pendingEventDao(): PendingEventDao
    abstract fun playlistSyncDao(): PlaylistSyncDao
}
