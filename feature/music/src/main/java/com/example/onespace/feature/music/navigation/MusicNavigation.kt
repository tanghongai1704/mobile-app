package com.example.onespace.feature.music.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.onespace.core.designsystem.CatalogContent
import com.example.onespace.core.designsystem.EmptySpec
import com.example.onespace.core.session.SessionCoordinator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote

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
            val offline by vm.offline.collectAsStateWithLifecycle()
            CatalogContent(
                state,
                offline = offline,
                onRetry = vm::retry,
                empty = EmptySpec(Icons.Filled.MusicNote, "Kho nhạc trống", "Làm mới", vm::retry)
            ) { tracks ->
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
            val playback by session.music.collectAsStateWithLifecycle()
            CatalogContent(state) { tracks ->
                // Follow the engine, not the route argument: auto-advance may have moved on.
                val current = tracks.firstOrNull { it.id == playback.trackId }
                    ?: tracks.firstOrNull { it.id == entry.arguments?.getString("id") }
                NowPlayingScreen(
                    track = current,
                    playback = playback,
                    onToggle = {
                        if (playback.trackId == current?.id) session.toggleMusic()
                        else current?.let { session.playMusic(it.id, it.title) }
                    },
                    onSeekTo = session::seekMusicTo,
                    onPrevious = session::previousTrack,
                    onNext = {
                        vm.nextTrack(playback.trackId ?: current?.id)?.let { next ->
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
