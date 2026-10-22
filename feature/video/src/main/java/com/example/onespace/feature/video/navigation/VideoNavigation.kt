package com.example.onespace.feature.video.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.designsystem.EmptySpec
import com.example.onespace.core.media.VideoSurface
import com.example.onespace.core.session.SessionCoordinator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle

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
            val continueList by vm.continueWatching.collectAsStateWithLifecycle()
            val offline by vm.offline.collectAsStateWithLifecycle()
            CatalogContent(
                state,
                offline = offline,
                onRetry = vm::retry,
                empty = EmptySpec(Icons.Filled.PlayCircle, "Chưa có video nào", "Làm mới", vm::retry)
            ) { videos ->
                VideoScreen(videos, continueList) { id ->
                    videos.find { it.id == id }?.let { session.playVideo(it.id, it.title) }
                    nav.navigate(VideoNavigation.detail(id)) { launchSingleTop = true }
                }
            }
        }
        composable(VideoNavigation.DETAIL, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(VideoNavigation.GRAPH) }
            val vm: VideoViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val playback by session.video.collectAsStateWithLifecycle()
            CatalogContent(state) { videos ->
                val video = videos.find { it.id == entry.arguments?.getString("id") }
                val active = playback.videoId == video?.id
                VideoDetailScreen(
                    video = video,
                    related = videos,
                    playing = active && playback.playing,
                    muted = active && playback.muted,
                    positionMs = if (active) playback.positionMs else 0,
                    totalMs = if (active) playback.totalMs else 0,
                    videoSurface = { modifier, onDoubleTap ->
                        VideoSurface(vm.player, modifier, onDoubleTap)
                    },
                    onToggle = {
                        if (active) session.toggleVideo()
                        else video?.let { session.playVideo(it.id, it.title) }
                    },
                    onSeekBy = session::seekVideoBy,
                    onMinimize = onBrowseNews,
                    onOpenRelated = { id ->
                        videos.find { it.id == id }?.let { session.playVideo(it.id, it.title) }
                        nav.navigate(VideoNavigation.detail(id)) { launchSingleTop = true }
                    }
                )
            }
        }
    }
}
