package com.example.onespace.feature.home.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.feature.home.presentation.HomeScreen
import com.example.onespace.feature.home.presentation.HomeViewModel

object HomeNavigation {
    const val GRAPH = "home"
    const val FEED = "home/feed"
}

/**
 * Cross-content deeplinks are injected from the app level so the home module
 * never depends on the other feature graphs.
 */
fun NavGraphBuilder.homeGraph(
    onOpenVideo: (id: String, title: String) -> Unit,
    onPlayTrack: (id: String, title: String) -> Unit,
    onOpenArticle: (id: String) -> Unit
) {
    navigation(startDestination = HomeNavigation.FEED, route = HomeNavigation.GRAPH) {
        composable(HomeNavigation.FEED) {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            HomeScreen(state, onOpenVideo, onPlayTrack, onOpenArticle)
        }
    }
}
