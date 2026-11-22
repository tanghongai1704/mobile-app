package com.example.onespace.core.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ContentDatabase =
        Room.databaseBuilder(context, ContentDatabase::class.java, "onespace.db")
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            // Only versions older than the first shipped schema (v5) may be rebuilt from scratch.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2, 3, 4)
            .build()

    @Provides fun provideProgressDao(database: ContentDatabase): VideoProgressDao = database.videoProgressDao()
    @Provides fun provideWatchlistDao(database: ContentDatabase): WatchlistDao = database.watchlistDao()

    @Provides fun provideExtractedAudioDao(database: ContentDatabase): ExtractedAudioDao = database.extractedAudioDao()

    @Provides fun provideDownloadedContentDao(database: ContentDatabase): DownloadedContentDao = database.downloadedContentDao()
    @Provides fun providePointDao(database: ContentDatabase): PointDao = database.pointDao()
    @Provides fun provideDailyMissionDao(database: ContentDatabase): DailyMissionDao = database.dailyMissionDao()
    @Provides fun provideNewsArticleDao(database: ContentDatabase): NewsArticleDao = database.newsArticleDao()
    @Provides fun provideGameSnapshotDao(database: ContentDatabase): GameSnapshotDao = database.gameSnapshotDao()
    @Provides fun provideCatalogItemDao(database: ContentDatabase): CatalogItemDao = database.catalogItemDao()
    @Provides fun providePendingEventDao(database: ContentDatabase): PendingEventDao = database.pendingEventDao()
    @Provides fun providePlaylistSyncDao(database: ContentDatabase): PlaylistSyncDao = database.playlistSyncDao()
}
