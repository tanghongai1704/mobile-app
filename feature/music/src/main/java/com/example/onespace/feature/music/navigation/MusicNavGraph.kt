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
    onOpenPlayer: (String) -> Unit,
    onOpenArtist: (String) -> Unit = {}
) {
    navigation<MusicRoute.Graph>(startDestination = MusicRoute.Feed) {
        composable<MusicRoute.Feed> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: MusicViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val offline by vm.offline.collectAsStateWithLifecycle()
            val favorites by vm.favorites.collectAsStateWithLifecycle()
            val downloads by vm.downloads.collectAsStateWithLifecycle()
            CatalogContent(
                state,
                offline = offline,
                onRetry = vm::retry,
                empty = EmptySpec(Icons.Filled.MusicNote, "Kho nhạc trống", "Làm mới", vm::retry)
            ) { tracks ->
                val extracted by vm.extracted.collectAsStateWithLifecycle()
                MusicScreen(
                    tracks,
                    favorites,
                    downloads,
                    extracted,
                    onOpen = { id ->
                        tracks.find { it.id == id }?.let { session.playMusic(it.id, it.title) }
                        onOpenPlayer(id)
                    },
                    onOpenArtist = onOpenArtist,
                    onOpenExtracted = { entry ->
                        // An extracted entry IS audio: it plays through the
                        // music engine like any other track.
                        val track = com.example.onespace.domain.music.model.Track(
                            id = "ext-" + entry.videoId,
                            title = entry.title,
                            artist = entry.creator,
                            duration = entry.duration,
                            url = entry.url
                        )
                        session.playMusicPlaylist(track, listOf(track))
                        onOpenPlayer(track.id)
                    },
                    onToggleFavorite = vm::toggleFavorite,
                    onToggleDownload = { id, title, subtitle -> vm.toggleDownload(id, title, subtitle) },
                )
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
                val shuffle by session.musicShuffle.collectAsStateWithLifecycle()
                val repeat by session.musicRepeat.collectAsStateWithLifecycle()
                val favorites by vm.favorites.collectAsStateWithLifecycle()
                val downloads by vm.downloads.collectAsStateWithLifecycle()
                NowPlayingScreen(
                    track = current,
                    playback = playback,
                    shuffle = shuffle,
                    repeat = repeat,
                    favorite = (playback.trackId ?: id) in favorites,
                    downloaded = (playback.trackId ?: id) in downloads,
                    onToggle = {
                        if (playback.trackId == current?.id) session.toggleMusic()
                        else current?.let { session.playMusic(it.id, it.title) }
                    },
                    onSeekTo = session::seekMusicTo,
                    onPrevious = session::previousTrack,
                    onNext = session::nextTrack,
                    onToggleShuffle = session::toggleShuffle,
                    onToggleRepeat = session::toggleRepeat,
                    onToggleFavorite = { vm.toggleFavorite(playback.trackId ?: id) },
                    onToggleDownload = {
                        current?.let { vm.toggleDownload(it.id, it.title, it.artist + " · " + it.duration) }
                    },
                    onPlayQueueItem = { qid ->
                        tracks.find { it.id == qid }?.let { session.playMusic(it.id, it.title) }
                    },
                    onOpenArtist = onOpenArtist,
                    onRemoveFromQueue = { qid -> session.removeFromQueue(qid) },
                    onMoveQueueItem = { from, to -> session.moveQueueItem(from, to) },
                    onAddToPlaylist = { tid ->
                        val existing = com.example.onespace.core.designsystem.AppPlaylistStore.playlists.keys.firstOrNull()
                        if (existing != null) com.example.onespace.core.designsystem.AppPlaylistStore.addTrack(existing, tid)
                        else com.example.onespace.core.designsystem.AppPlaylistStore.create("Nhạc của tôi", trackIds = listOf(tid))
                    }
                )
        }
        composable<MusicRoute.Artist> { entry ->
            val artist = entry.toRoute<MusicRoute.Artist>().artist
            val owner = remember(entry) { getGraphEntry() }
            val vm: MusicViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { tracks ->
                MusicArtistScreen(artist, tracks) { id ->
                    tracks.find { it.id == id }?.let { session.playMusic(it.id, it.title) }
                    onOpenPlayer(id)
                }
            }
        }
        composable<MusicRoute.Playlist> { entry ->
            val name = entry.toRoute<MusicRoute.Playlist>().name
            val owner = remember(entry) { getGraphEntry() }
            val vm: MusicViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { tracks ->
                MusicPlaylistScreen(
                    name, tracks,
                    onOpen = { id ->
                        tracks.find { it.id == id }?.let { session.playMusic(it.id, it.title) }
                        onOpenPlayer(id)
                    },
                    onDelete = { com.example.onespace.core.designsystem.AppPlaylistStore.remove(it) },
                    shuffle = session.musicShuffle.collectAsStateWithLifecycle().value,
                    repeat = session.musicRepeat.collectAsStateWithLifecycle().value,
                    onToggleShuffle = session::toggleShuffle,
                    onToggleRepeat = session::toggleRepeat,
                    onPlay = { items ->
                        val wantShuffle = session.musicShuffle.value
                        val start = if (wantShuffle) items.random() else items.first()
                        session.playMusicPlaylist(start, if (wantShuffle) items.shuffled() else items)
                        onOpenPlayer(start.id)
                    }
                )
            }
        }
    }
}
