package com.example.onespace.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.home.navigation.HomeNavigation
import com.example.onespace.feature.home.navigation.homeGraph
import com.example.onespace.feature.music.navigation.MusicNavigation
import com.example.onespace.feature.music.navigation.musicGraph
import com.example.onespace.feature.news.navigation.NewsNavigation
import com.example.onespace.feature.news.navigation.newsGraph
import com.example.onespace.feature.games.navigation.gamesGraph
import com.example.onespace.feature.account.navigation.accountGraph
import com.example.onespace.feature.onboarding.navigation.OnboardingNavigation
import com.example.onespace.feature.onboarding.navigation.onboardingGraph
import com.example.onespace.feature.video.navigation.VideoNavigation
import com.example.onespace.feature.video.navigation.videoGraph

/** App wires feature graphs. Features own their destinations, arguments and ViewModels. */
@Composable
fun AppNavigation(
    nav: NavHostController,
    session: SessionCoordinator,
    onSection: (Section) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController = nav, startDestination = HomeNavigation.GRAPH, modifier = modifier) {
        homeGraph(
            onOpenVideo = { id, title ->
                session.enter(Section.VIDEO)
                session.playVideo(id, title)
                nav.navigate(VideoNavigation.detail(id)) { launchSingleTop = true }
            },
            onPlayTrack = { id, title ->
                session.enter(Section.MUSIC)
                session.playMusic(id, title)
                nav.navigate(MusicNavigation.nowPlaying(id)) { launchSingleTop = true }
            },
            onOpenArticle = {
                session.enter(Section.NEWS)
                nav.navigate(NewsNavigation.article(it)) { launchSingleTop = true }
            }
        )
        videoGraph(nav, session, onBrowseNews = { onSection(Section.NEWS) })
        musicGraph(nav, session)
        newsGraph(nav)
        gamesGraph(nav, session)
        accountGraph(
            nav,
            onExitAccount = {
                nav.navigate(OnboardingNavigation.LOGIN) {
                    popUpTo(0) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
        onboardingGraph(
            nav,
            onDone = {
                nav.navigate(HomeNavigation.GRAPH) {
                    popUpTo(OnboardingNavigation.GRAPH) { inclusive = true }
                    launchSingleTop = true
                }
            }
        )
    }
}
