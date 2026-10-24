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

fun NavGraphBuilder.musicGraph(
    session: SessionCoordinator,
    getGraphEntry: () -> NavBackStackEntry,
    onOpenPlayer: (String) -> Unit
) {
    navigation<MusicRoute.Graph>(startDestination = MusicRoute.Feed) {
        composable<MusicRoute.Feed> { entry ->
            val owner = remember(entry) { getGraphEntry() }
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
                    onOpenPlayer(id)
                }
            }
        }
        composable<MusicRoute.NowPlaying> { entry ->
            val id = entry.toRoute<MusicRoute.NowPlaying>().id
            val owner = remember(entry) { getGraphEntry() }
            val vm: MusicViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val playback by session.music.collectAsStateWithLifecycle()
            // Active playback remains usable even if the catalog reload fails.
            val tracks = playback.queue.ifEmpty { state.items }
            // Follow the engine, not the route argument: auto-advance may have moved on.
                val current = tracks.firstOrNull { it.id == playback.trackId }
                    ?: tracks.firstOrNull { it.id == id }
                NowPlayingScreen(
                    track = current,
                    playback = playback,
                    onToggle = {
                        if (playback.trackId == current?.id) session.toggleMusic()
                        else current?.let { session.playMusic(it.id, it.title) }
                    },
                    onSeekTo = session::seekMusicTo,
                    onPrevious = session::previousTrack,
                    onNext = session::nextTrack)
        }
    }
}
