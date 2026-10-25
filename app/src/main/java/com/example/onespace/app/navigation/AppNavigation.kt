package com.example.onespace.app.navigation

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
    downloadedContent: com.example.onespace.domain.session.repository.DownloadedContentRepository? = null
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
            inOnboarding && preferences?.onboarded == true ->
                replaceRoot(if (current.hasAccess) AppRoute.Main else AuthRoute.Graph)
        }
    }

    NavHost(navController = nav, startDestination = AppRoute.Splash,
        route = AppRoute.Root::class, modifier = modifier) {
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
                onOpenArticle = {
                    session.enter(Section.NEWS)
                    appState.openArticle(it)
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
                onOpenArticle = appState::openArticle
            )
            gamesGraph(
                session = session,
                getGraphEntry = { appState.graphEntry(GamesRoute.Graph) },
                onOpenGame = appState::openGame
            )
            accountGraph(
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
