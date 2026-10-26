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
import com.example.onespace.data.games.repository.createPointRepository
import com.example.onespace.data.games.repository.createMissionRepository
import com.example.onespace.data.games.repository.createStreakRepository
import com.example.onespace.data.games.repository.createPetRepository
import com.example.onespace.data.games.repository.createShopRepository
import com.example.onespace.domain.session.repository.MissionRepository
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.domain.session.repository.StreakRepository
import com.example.onespace.domain.session.repository.PetRepository
import com.example.onespace.domain.session.repository.ShopRepository
import com.example.onespace.domain.session.usecase.AddPoints
import com.example.onespace.domain.session.usecase.ObserveTotalPoints
import com.example.onespace.domain.session.usecase.SpendPoints
import com.example.onespace.domain.session.usecase.TrackMissionAction
import com.example.onespace.domain.session.usecase.ObserveDailyMissions
import com.example.onespace.domain.session.usecase.EnsureTodayMissions
import com.example.onespace.domain.session.usecase.ObserveStreak
import com.example.onespace.domain.session.usecase.UpdateStreak
import com.example.onespace.domain.session.usecase.ObservePet
import com.example.onespace.domain.session.usecase.FeedPet
import com.example.onespace.domain.session.usecase.PurchaseItem
import com.example.onespace.domain.session.usecase.ObserveShop
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
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Composition root: the only place that chooses mock versus production repositories. */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides @Singleton fun auth(preferences: SessionPreferencesRepository): AuthRepository = createAuthRepository(preferences)
    @Provides fun requestCode(repository: AuthRepository) = RequestSignInCode(repository)
    @Provides fun verifyCode(repository: AuthRepository) = VerifySignInCode(repository)
    @Provides fun passwordSignIn(repository: AuthRepository) = SignInWithPassword(repository)
    @Provides fun guest(repository: AuthRepository) = ContinueAsGuest(repository)
    @Provides fun signOut(repository: AuthRepository) = SignOut(repository)
    @Provides @Singleton fun videos(): VideoRepository = createVideoRepository()
    @Provides @Singleton fun music(): MusicRepository = createMusicRepository()
    @Provides @Singleton fun news(): NewsRepository = createNewsRepository()
    @Provides @Singleton fun games(): GamesRepository = createGamesRepository()

    @Provides @Singleton
    fun points(database: ContentDatabase): PointRepository =
        createPointRepository(database)

    @Provides fun addPoints(repository: PointRepository) = AddPoints(repository)
    @Provides fun observeTotalPoints(repository: PointRepository) = ObserveTotalPoints(repository)
    @Provides fun spendPoints(repository: PointRepository) = SpendPoints(repository)

    @Provides @Singleton
    fun missions(database: ContentDatabase): MissionRepository =
        createMissionRepository(database)

    @Provides @Singleton
    fun streak(@ApplicationContext context: android.content.Context): StreakRepository =
        createStreakRepository(context)

    @Provides @Singleton
    fun pet(@ApplicationContext context: android.content.Context, points: PointRepository): PetRepository =
        createPetRepository(context, points)

    @Provides @Singleton
    fun shop(@ApplicationContext context: android.content.Context): ShopRepository =
        createShopRepository(context)

    @Provides fun trackMissionAction(missions: MissionRepository, points: PointRepository, pet: PetRepository) = TrackMissionAction(missions, points, pet)
    @Provides fun observeDailyMissions(repository: MissionRepository) = ObserveDailyMissions(repository)
    @Provides fun ensureTodayMissions(repository: MissionRepository) = EnsureTodayMissions(repository)
    @Provides fun observeStreak(repository: StreakRepository) = ObserveStreak(repository)
    @Provides fun updateStreak(repository: StreakRepository) = UpdateStreak(repository)
    @Provides fun observePet(repository: PetRepository, points: PointRepository) = ObservePet(repository, points)
    @Provides fun feedPet(pet: PetRepository, points: PointRepository) = FeedPet(pet, points)
    @Provides fun purchaseItem(points: PointRepository, shop: ShopRepository) = PurchaseItem(points, shop)

    // TTS use cases
    @Provides fun listenArticle(engine: com.example.onespace.domain.news.repository.TtsEngine) =
        com.example.onespace.domain.news.usecase.ListenArticle(engine)
    @Provides fun pauseTts(engine: com.example.onespace.domain.news.repository.TtsEngine) =
        com.example.onespace.domain.news.usecase.PauseTts(engine)
    @Provides fun stopTts(engine: com.example.onespace.domain.news.repository.TtsEngine) =
        com.example.onespace.domain.news.usecase.StopTts(engine)
    @Provides fun observeShop(repository: ShopRepository) = ObserveShop(repository)

    // Guess Song use case
    @Provides fun startGuessSong(points: PointRepository) =
        com.example.onespace.domain.games.usecase.StartGuessSong(points)

    @Provides @Singleton
    fun viewingSession(database: ContentDatabase): ViewingSessionRepository =
        createViewingSessionRepository(database)

    @Provides @Singleton
    fun extractedAudio(database: ContentDatabase): com.example.onespace.domain.music.repository.ExtractedAudioRepository =
        com.example.onespace.data.music.createExtractedAudioRepository(database)

    @Provides @Singleton
    fun downloadedContent(database: ContentDatabase): com.example.onespace.domain.session.repository.DownloadedContentRepository =
        com.example.onespace.data.games.repository.createDownloadedContentRepository(database)

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
