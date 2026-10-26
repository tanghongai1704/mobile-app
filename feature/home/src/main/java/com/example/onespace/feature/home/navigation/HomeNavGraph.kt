package com.example.onespace.feature.home.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.feature.home.presentation.HomeScreen
import com.example.onespace.feature.home.presentation.HomeViewModel

fun NavGraphBuilder.homeGraph(
    onOpenVideo: (id: String, title: String) -> Unit,
    onPlayTrack: (id: String, title: String) -> Unit,
    onOpenArticle: (id: String) -> Unit,
    onMissionClick: (String) -> Unit = {}
) {
    navigation<HomeRoute.Graph>(startDestination = HomeRoute.Feed) {
        composable<HomeRoute.Feed> {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            val gamification by vm.gamification.collectAsStateWithLifecycle()
            val offline by vm.offline.collectAsStateWithLifecycle()
            val watchLater by vm.watchLater.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                gamification = gamification,
                offline = offline,
                watchLater = watchLater,
                onOpenVideo = onOpenVideo,
                onPlayTrack = onPlayTrack,
                onOpenArticle = onOpenArticle,
                onMissionClick = onMissionClick
            )
        }
    }
}
