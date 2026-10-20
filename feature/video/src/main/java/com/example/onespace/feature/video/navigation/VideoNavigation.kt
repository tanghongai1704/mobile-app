package com.example.onespace.feature.video.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.session.SessionCoordinator

import com.example.onespace.feature.video.presentation.*

object VideoNavigation {
    const val GRAPH = "video"
    const val FEED = "video/feed"
    const val DETAIL = "video/watch/{id}"
    fun detail(id: String) = "video/watch/" + android.net.Uri.encode(id)
}

fun NavGraphBuilder.videoGraph(nav: NavHostController, session: SessionCoordinator, onBrowseNews: () -> Unit) {
    navigation(startDestination = VideoNavigation.FEED, route = VideoNavigation.GRAPH) {
        composable(VideoNavigation.FEED) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(VideoNavigation.GRAPH) }
            val vm: VideoViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { videos ->
                VideoScreen(videos) { id ->
                    videos.find { it.id == id }?.let { session.playVideo(it.id, it.title) }
                    nav.navigate(VideoNavigation.detail(id)) { launchSingleTop = true }
                }
            }
        }
        composable(VideoNavigation.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(VideoNavigation.GRAPH) }
            val vm: VideoViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val playback by session.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { videos ->
                val video = videos.find { it.id == entry.arguments?.getString("id") }
                VideoDetailScreen(video, playback.videoPlaying && playback.videoId == video?.id,
                    onToggle = {
                        video?.let {
                            if (playback.videoId == it.id) session.toggleVideo() else session.playVideo(it.id, it.title)
                        }
                    },
                    onMinimize = onBrowseNews)
            }
        }
    }
}
