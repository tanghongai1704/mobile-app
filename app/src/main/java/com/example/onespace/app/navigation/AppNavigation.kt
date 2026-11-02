package com.example.onespace.app.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.example.onespace.app.ui.*
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.home.navigation.*
import com.example.onespace.feature.music.navigation.*
import com.example.onespace.feature.news.navigation.*
import com.example.onespace.feature.games.navigation.GamesRoute
import com.example.onespace.feature.games.navigation.gamesGraph
import com.example.onespace.feature.account.navigation.AccountRoute
import com.example.onespace.feature.account.navigation.accountGraph
import com.example.onespace.feature.onboarding.navigation.*
import com.example.onespace.feature.auth.navigation.*
import com.example.onespace.feature.video.navigation.*

/** Composition root: splash → onboarding/auth → main; features own their nested graphs. */
@Composable
fun AppNavigation(
    appState: OneSpaceAppState,
    session: SessionCoordinator,
    onSection: (Section) -> Unit,
    modifier: Modifier = Modifier,
    startup: StartupViewModel = hiltViewModel(),
    downloadedContent: com.example.onespace.domain.session.repository.DownloadedContentRepository? = null,
    videoPlayer: androidx.media3.common.Player? = null
) {
    val nav = appState.navController
    fun replaceRoot(route: Any) {
        nav.navigate(route) {
            popUpTo<AppRoute.Root> { inclusive = false }
            launchSingleTop = true
        }
    }
    val preferences by startup.preferences.collectAsStateWithLifecycle()
    val auth by startup.authSession.collectAsStateWithLifecycle()
    val entry by nav.currentBackStackEntryAsState()
    val inMain = entry?.destination?.hierarchy?.any { it.hasRoute<AppRoute.Main>() } == true
    val inAuth = entry?.destination?.hierarchy?.any { it.hasRoute<AuthRoute.Graph>() } == true
    val inOnboarding = entry?.destination?.hierarchy?.any { it.hasRoute<OnboardingRoute.Graph>() } == true

    // Persisted state owns root transitions. A ViewModel never retains an Activity's NavController.
    LaunchedEffect(auth?.hasAccess, preferences?.onboarded, inMain, inAuth, inOnboarding) {
        val current = auth ?: return@LaunchedEffect
        when {
            inMain && !current.hasAccess -> {
                session.cancelPendingRestore()
                session.closeMusic()
                session.closeVideo()
                replaceRoot(AuthRoute.Graph)
            }
            inAuth && current.hasAccess -> replaceRoot(AppRoute.Main)
            // Settings can replay the first-launch flow: the flag flips
            // and the root moves back to onboarding (auth session kept).
            inMain && preferences?.onboarded == false -> replaceRoot(OnboardingRoute.Graph)
            inOnboarding && preferences?.onboarded == true ->
                replaceRoot(if (current.hasAccess) AppRoute.Main else AuthRoute.Graph)
        }
    }

    // Short cross-fades with a hint of depth; the default fade is slow enough to feel laggy.
    NavHost(navController = nav, startDestination = AppRoute.Splash,
        route = AppRoute.Root::class, modifier = modifier,
        enterTransition = { fadeIn(tween(220)) + scaleIn(tween(220), initialScale = 0.98f) },
        exitTransition = { fadeOut(tween(120)) },
        popEnterTransition = { fadeIn(tween(220)) },
        popExitTransition = { fadeOut(tween(120)) + scaleOut(tween(120), targetScale = 0.98f) }) {
        composable<AppRoute.Splash> {
            val state by startup.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(state) {
                (state as? StartupUiState.Ready)?.let {
                    replaceRoot(when (it.destination) {
                        StartupDestination.ONBOARDING -> OnboardingRoute.Graph
                        StartupDestination.AUTH -> AuthRoute.Graph
                        StartupDestination.MAIN -> AppRoute.Main
                    })
                }
            }
            StartupScreen(state, startup::load)
        }
        onboardingGraph()
        authGraph(
            getGraphEntry = { appState.graphEntry(AuthRoute.Graph) },
            onVerifyEmail = appState::verifyEmail,
            onBack = appState::navigateBack
        )
        navigation<AppRoute.Main>(startDestination = HomeRoute.Graph) {
            homeGraph(
                onOpenVideo = { id, title ->
                    session.enter(Section.VIDEO)
                    session.playVideo(id, title)
                    appState.openVideo(id)
                },
                onPlayTrack = { id, title ->
                    session.enter(Section.MUSIC)
                    session.playMusic(id, title)
                    appState.openMusicPlayer(id)
                },
                // Feed posts play in place: the card is the player; leaving
                // Home is what turns playback into floating/dock (C1/C2 apply).
                onPlayVideoAtHome = { id, title -> session.playVideo(id, title) },
                onPlayMusicAtHome = { id, title -> session.playMusic(id, title) },
                videoPlayer = videoPlayer,
                onToggleVideo = session::toggleVideo,
                onToggleMusic = session::toggleMusic,
                onSeekVideo = session::seekVideoTo,
                onSeekMusic = session::seekMusicTo,
                session = session,
                onOpenArticle = {
                    session.enter(Section.NEWS)
                    appState.openArticle(it)
                },
                onOpenPlaylists = appState::openPlaylists,
                onMissionClick = { id ->
                    // F-COM-05: a mission deep-links to the tab where it can be done.
                    onSection(
                        when (id) {
                            "listen" -> Section.MUSIC
                            "read" -> Section.NEWS
                            "play" -> Section.GAMES
                            else -> Section.HOME
                        }
                    )
                }
            )
            videoGraph(
                session = session,
                getGraphEntry = { appState.graphEntry(VideoRoute.Graph) },
                onOpenVideo = appState::openVideo,
                onBrowseNews = { onSection(Section.NEWS) },
                onOpenArtist = appState::openVideoArtist,
                onOpenFromPlaylist = appState::openVideoInPlaylist
            )
            musicGraph(
                session = session,
                getGraphEntry = { appState.graphEntry(MusicRoute.Graph) },
                onOpenPlayer = appState::openMusicPlayer,
                onOpenArtist = appState::openMusicArtist
            )
            newsGraph(
                getGraphEntry = { appState.graphEntry(NewsRoute.Graph) },
                onOpenArticle = appState::openArticle,
                onListenTts = session::toggleMusicOff
            )
            gamesGraph(
                session = session,
                getGraphEntry = { appState.graphEntry(GamesRoute.Graph) },
                onOpenGame = appState::openGame
            )
            accountGraph(
                getGraphEntry = { appState.graphEntry(AccountRoute.Graph) },
                onOpenSettings = appState::openSettings,
                onOpenWrapped = appState::openWrapped,
                onOpenNotifications = appState::openNotifications,
                onOpenPlaylists = appState::openPlaylists,
                onDownloads = appState::openDownloads,
                onBadges = appState::openBadges,
                onOpenVideoPlaylist = appState::openVideoPlaylist,
                onOpenMusicPlaylist = appState::openMusicPlaylist,
                onOpenVideo = appState::openVideo,
                onOpenMusic = appState::openMusicPlayer,
                onOpenArticle = appState::openArticle,
                downloadedContent = downloadedContent
            )
        }
    }
}
