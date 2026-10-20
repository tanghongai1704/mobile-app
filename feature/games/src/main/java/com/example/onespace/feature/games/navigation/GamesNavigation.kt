package com.example.onespace.feature.games.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.session.SessionCoordinator

import com.example.onespace.feature.games.presentation.*

object GamesNavigation {
    const val GRAPH = "games"
    const val FEED = "games/feed"
    const val GAME = "games/play/{id}"
    fun game(id: String) = "games/play/" + android.net.Uri.encode(id)
}

fun NavGraphBuilder.gamesGraph(nav: NavHostController, session: SessionCoordinator) {
    navigation(startDestination = GamesNavigation.FEED, route = GamesNavigation.GRAPH) {
        composable(GamesNavigation.FEED) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(GamesNavigation.GRAPH) }
            val vm: GamesViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { games ->
                GamesScreen(games) { nav.navigate(GamesNavigation.game(it)) }
            }
        }
        composable(GamesNavigation.GAME, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(GamesNavigation.GRAPH) }
            val vm: GamesViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val playback by session.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(entry) { session.resumeGame() }
            CatalogContent(state) { games ->
                GameScreen(games.find { it.id == entry.arguments?.getString("id") },
                    playback.gamePaused, session::pauseGame)
            }
        }
    }
}
