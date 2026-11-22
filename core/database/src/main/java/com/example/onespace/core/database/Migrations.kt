package com.example.onespace.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v5 -> v6 adds the offline news cache and saved game rounds; existing progress, points and
 * library rows are kept.
 */
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `news_articles` (
                `id` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `category` TEXT NOT NULL,
                `readTime` TEXT NOT NULL,
                `body` TEXT NOT NULL,
                `quizJson` TEXT NOT NULL,
                `publishedAt` INTEGER NOT NULL,
                `sortOrder` INTEGER NOT NULL,
                `cachedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `game_snapshots` (
                `gameId` TEXT NOT NULL,
                `version` INTEGER NOT NULL,
                `board` TEXT NOT NULL,
                `score` INTEGER NOT NULL,
                `reachedTarget` INTEGER NOT NULL,
                `savedAt` INTEGER NOT NULL,
                PRIMARY KEY(`gameId`)
            )
            """.trimIndent()
        )
    }
}

/**
 * v6 -> v7 adds the catalogue cache (offline-first video/music), the event outbox for
 * `POST /me/events` and the playlist sync state. No existing table changes.
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `catalog_items` (
                `id` TEXT NOT NULL,
                `type` TEXT NOT NULL,
                `title` TEXT NOT NULL,
                `creator` TEXT NOT NULL,
                `duration` TEXT NOT NULL,
                `url` TEXT NOT NULL,
                `topic` TEXT NOT NULL,
                `lyricsJson` TEXT NOT NULL,
                `bpm` INTEGER,
                `sortOrder` INTEGER NOT NULL,
                `cachedAt` INTEGER NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `pending_events` (
                `eventId` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `amount` INTEGER NOT NULL,
                `missionType` TEXT,
                `createdAt` INTEGER NOT NULL,
                PRIMARY KEY(`eventId`)
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `playlist_sync` (
                `name` TEXT NOT NULL,
                `remoteId` TEXT NOT NULL,
                `version` INTEGER NOT NULL,
                `itemsHash` TEXT NOT NULL,
                PRIMARY KEY(`name`)
            )
            """.trimIndent()
        )
    }
}
