package com.example.onespace.app.di

import com.example.onespace.data.auth.createAuthRepository
import com.example.onespace.domain.auth.repository.AuthRepository
import com.example.onespace.domain.auth.usecase.*
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.data.video.createVideoRepository
import com.example.onespace.data.video.createViewingSessionRepository
import com.example.onespace.data.music.createMusicRepository
import com.example.onespace.data.news.createNewsRepository
import com.example.onespace.data.games.createGamesRepository
import com.example.onespace.domain.video.repository.VideoRepository
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.domain.news.repository.NewsRepository
import com.example.onespace.domain.games.repository.GamesRepository
import com.example.onespace.domain.video.usecase.GetVideo
import com.example.onespace.domain.video.usecase.ObserveContinueWatching
import com.example.onespace.domain.video.usecase.SaveVideoProgress
import com.example.onespace.domain.video.usecase.WatchLater
import com.example.onespace.domain.music.usecase.GetMusic
import com.example.onespace.domain.news.usecase.GetNews
import com.example.onespace.domain.news.usecase.ObserveSavedArticles
import com.example.onespace.domain.news.usecase.ToggleBookmark
import com.example.onespace.domain.games.usecase.GetGames
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Composition root: the only place that chooses mock versus production repositories. */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton fun auth(preferences: SessionPreferencesRepository): AuthRepository = createAuthRepository(preferences)
    @Provides fun requestCode(repository: AuthRepository) = RequestSignInCode(repository)
    @Provides fun verifyCode(repository: AuthRepository) = VerifySignInCode(repository)
    @Provides fun guest(repository: AuthRepository) = ContinueAsGuest(repository)
    @Provides fun signOut(repository: AuthRepository) = SignOut(repository)
    @Provides @Singleton fun videos(): VideoRepository = createVideoRepository()
    @Provides @Singleton fun music(): MusicRepository = createMusicRepository()
    @Provides @Singleton fun news(): NewsRepository = createNewsRepository()
    @Provides @Singleton fun games(): GamesRepository = createGamesRepository()

    @Provides @Singleton
    fun viewingSession(database: ContentDatabase): ViewingSessionRepository =
        createViewingSessionRepository(database)

    @Provides fun getVideo(repository: VideoRepository) = GetVideo(repository)
    @Provides fun getMusic(repository: MusicRepository) = GetMusic(repository)
    @Provides fun getNews(repository: NewsRepository) = GetNews(repository)
    @Provides fun savedArticles(repository: NewsRepository) = ObserveSavedArticles(repository)
    @Provides fun toggleBookmark(repository: NewsRepository) = ToggleBookmark(repository)
    @Provides fun getGames(repository: GamesRepository) = GetGames(repository)
    @Provides fun observeContinue(repository: ViewingSessionRepository) = ObserveContinueWatching(repository)
    @Provides fun saveProgress(repository: ViewingSessionRepository) = SaveVideoProgress(repository)
    @Provides fun watchLater(repository: ViewingSessionRepository) = WatchLater(repository)
}
