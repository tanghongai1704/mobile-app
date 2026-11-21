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
