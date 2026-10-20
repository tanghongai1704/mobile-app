package com.example.onespace.feature.music.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.session.SessionCoordinator

import com.example.onespace.feature.music.presentation.*

object MusicNavigation {
    const val GRAPH = "music"
    const val FEED = "music/feed"
    const val NOW_PLAYING = "music/playing/{id}"
    fun nowPlaying(id: String) = "music/playing/" + android.net.Uri.encode(id)
}

fun NavGraphBuilder.musicGraph(nav: NavHostController, session: SessionCoordinator) {
    navigation(startDestination = MusicNavigation.FEED, route = MusicNavigation.GRAPH) {
        composable(MusicNavigation.FEED) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(MusicNavigation.GRAPH) }
            val vm: MusicViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { tracks ->
                MusicScreen(tracks) { id ->
                    tracks.find { it.id == id }?.let { session.playMusic(it.id, it.title) }
                    nav.navigate(MusicNavigation.nowPlaying(id)) { launchSingleTop = true }
                }
            }
        }
        composable(MusicNavigation.NOW_PLAYING, arguments = listOf(navArgument("id") { type = NavType.StringType })) { entry ->
            val owner = remember(entry) { nav.getBackStackEntry(MusicNavigation.GRAPH) }
            val vm: MusicViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val playback by session.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { tracks ->
                val track = tracks.find { it.id == entry.arguments?.getString("id") }
                NowPlayingScreen(track, playback.musicPlaying && playback.trackId == track?.id,
                    onToggle = {
                        track?.let {
                            if (playback.trackId == it.id) session.toggleMusic() else session.playMusic(it.id, it.title)
                        }
                    },
                    onNext = {
                        vm.nextTrack(track?.id)?.let { next ->
                            session.playMusic(next.id, next.title)
                            nav.navigate(MusicNavigation.nowPlaying(next.id)) {
                                popUpTo(MusicNavigation.NOW_PLAYING) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    })
            }
        }
    }
}
