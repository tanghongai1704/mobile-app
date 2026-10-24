package com.example.onespace.feature.games.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.designsystem.EmptySpec
import com.example.onespace.core.session.SessionCoordinator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SportsEsports

import com.example.onespace.feature.games.presentation.*

fun NavGraphBuilder.gamesGraph(
    session: SessionCoordinator,
    getGraphEntry: () -> NavBackStackEntry,
    onOpenGame: (String) -> Unit
) {
    navigation<GamesRoute.Graph>(startDestination = GamesRoute.Feed) {
        composable<GamesRoute.Feed> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: GamesViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val offline by vm.offline.collectAsStateWithLifecycle()
            CatalogContent(
                state,
                offline = offline,
                onRetry = vm::retry,
                empty = EmptySpec(Icons.Filled.SportsEsports, "Chưa mở khoá game nào", "Chơi thử ngay", vm::retry)
            ) { games ->
                GamesScreen(games, onOpenGame)
            }
        }
        composable<GamesRoute.Game> { entry ->
            val id = entry.toRoute<GamesRoute.Game>().id
            val owner = remember(entry) { getGraphEntry() }
            val vm: GamesViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val playback by session.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(entry) { session.resumeGame() }
            CatalogContent(state) { games ->
                GameScreen(games.find { it.id == id },
                    playback.gamePaused, session::pauseGame)
            }
        }
    }
}
