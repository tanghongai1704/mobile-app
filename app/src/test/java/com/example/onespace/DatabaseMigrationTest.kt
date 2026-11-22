package com.example.onespace

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.database.GameSnapshotEntity
import com.example.onespace.core.database.CatalogItemEntity
import com.example.onespace.core.database.MIGRATION_5_6
import com.example.onespace.core.database.MIGRATION_6_7
import com.example.onespace.core.database.PendingEventEntity
import com.example.onespace.core.database.NewsArticleEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Upgrading from the shipped schema v5 keeps the user's data and adds the v6 and v7 tables. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class DatabaseMigrationTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun v5DataSurvivesTheUpgradeToV7() = runBlocking {
        val name = "migration-test.db"
        context.deleteDatabase(name)
        // A v5 database as the previous release left it (one table is enough to prove data stays).
        SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name).apply { parentFile?.mkdirs() }, null).use { db ->
            db.execSQL("CREATE TABLE IF NOT EXISTS `point_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `source` TEXT NOT NULL, `amount` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS `video_progress` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `positionMs` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `watchlist` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `savedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `daily_missions` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `targetType` TEXT NOT NULL, `target` INTEGER NOT NULL, `progress` INTEGER NOT NULL, `completed` INTEGER NOT NULL, `date` TEXT NOT NULL, PRIMARY KEY(`id`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `extracted_audio` (`videoId` TEXT NOT NULL, `title` TEXT NOT NULL, `creator` TEXT NOT NULL, `url` TEXT NOT NULL, `duration` TEXT NOT NULL, `extractedAt` INTEGER NOT NULL, PRIMARY KEY(`videoId`))")
            db.execSQL("CREATE TABLE IF NOT EXISTS `downloaded_content` (`contentId` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `subtitle` TEXT NOT NULL, `downloadedAt` INTEGER NOT NULL, PRIMARY KEY(`contentId`))")
            db.execSQL("INSERT INTO point_entries (source, amount, timestamp) VALUES ('game_2048', 40, 1)")
            db.version = 5
        }

        val upgraded = Room.databaseBuilder(context, ContentDatabase::class.java, name)
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .allowMainThreadQueries()
            .build()
        assertEquals(40, upgraded.pointDao().observeTotal().first())

        upgraded.newsArticleDao().upsertAll(listOf(NewsArticleEntity("n1", "T", "C", "1 phút", "B", "[]", 0, 0, 0)))
        upgraded.gameSnapshotDao().upsert(GameSnapshotEntity("g1", 1, "2,0,0,2", 4, false, 0))
        assertEquals(1, upgraded.newsArticleDao().count())
        assertEquals("2,0,0,2", upgraded.gameSnapshotDao().find("g1")!!.board)

        upgraded.catalogItemDao().upsertAll(listOf(CatalogItemEntity("m1", "music", "T", "A", "1:00", "u", "", "[]", 100, 0, 0)))
        upgraded.pendingEventDao().insert(PendingEventEntity("e1", "game", 5, null, 0))
        assertEquals(1, upgraded.catalogItemDao().count("music"))
        assertEquals(listOf("e1"), upgraded.pendingEventDao().oldest(10).map { it.eventId })
        upgraded.close()
    }
}
