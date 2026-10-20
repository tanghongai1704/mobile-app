package com.example.onespace.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.home.navigation.*
import com.example.onespace.feature.video.navigation.videoGraph
import com.example.onespace.feature.music.navigation.musicGraph
import com.example.onespace.feature.news.navigation.newsGraph
import com.example.onespace.feature.games.navigation.gamesGraph
import com.example.onespace.feature.account.navigation.accountGraph

/** App wires feature graphs. Features own their destinations, arguments and ViewModels. */
@Composable
fun AppNavigation(
    nav: NavHostController,
    session: SessionCoordinator,
    onSection: (Section) -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(navController = nav, startDestination = HomeNavigation.GRAPH, modifier = modifier) {
        homeGraph(onSection)
        videoGraph(nav, session, onBrowseNews = { onSection(Section.NEWS) })
        musicGraph(nav, session)
        newsGraph(nav)
        gamesGraph(nav, session)
        accountGraph(nav)
    }
}
