package com.example.onespace.feature.music.navigation

import com.example.onespace.core.designsystem.tr
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
import kotlinx.coroutines.launch

import com.example.onespace.feature.music.presentation.*

fun NavGraphBuilder.musicGraph(
    session: SessionCoordinator,
    getGraphEntry: () -> NavBackStackEntry,
    onOpenPlayer: (String) -> Unit,
    onOpenArtist: (String) -> Unit = {},
    onOpenGuessSong: () -> Unit = {},
    /** The session moved to a video (music → video): replace the player screen. */
    onFollowSessionVideo: (String, String) -> Unit = { _, _ -> },
    /** Back from a playlist goes to the playlist list, not the Music tab. */
    onBackToPlaylists: () -> Unit = {}
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
                empty = EmptySpec(Icons.Filled.MusicNote, tr("Kho nhạc trống", "The music library is empty"), tr("Làm mới", "Refresh"), vm::retry)
            ) { tracks ->
                val extracted by vm.extracted.collectAsStateWithLifecycle()
                val nowPlaying by session.music.collectAsStateWithLifecycle()
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
                    playingTrackId = nowPlaying.trackId,
                    onOpenGuessSong = onOpenGuessSong,
                    onAddToQueue = session::addToQueue
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
                // Mixed playlist session: when the session moves to a video,
                // follow it. Each index is handled once so going back to an
                // already-played song does not jump forward again.
                val playlistSession by session.playlistSession.collectAsStateWithLifecycle()
                var handledSessionIndex by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(-1) }
                LaunchedEffect(playlistSession) {
                    val active2 = playlistSession ?: return@LaunchedEffect
                    val current2 = active2.current ?: return@LaunchedEffect
                    if (!current2.isVideo || active2.index == handledSessionIndex) return@LaunchedEffect
                    handledSessionIndex = active2.index
                    onFollowSessionVideo(current2.id, active2.name)
                }
                val shuffle by session.musicShuffle.collectAsStateWithLifecycle()
                val repeat by session.musicRepeat.collectAsStateWithLifecycle()
                val musicSpeed by session.musicSpeed.collectAsStateWithLifecycle()
                val musicVolume by session.musicVolume.collectAsStateWithLifecycle()
                val favorites by vm.favorites.collectAsStateWithLifecycle()
                val downloads by vm.downloads.collectAsStateWithLifecycle()
                NowPlayingScreen(
                    track = current,
                    playback = playback,
                    shuffle = shuffle,
                    repeat = repeat,
                    favorite = (playback.trackId ?: id) in favorites,
                    downloaded = (playback.trackId ?: id) in downloads,
                    speed = musicSpeed,
                    onCycleSpeed = session::cycleMusicSpeed,
                    volume = musicVolume,
                    onVolumeChange = session::setMusicVolume,
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
                        // Inside a playlist session the queue is the session's run: jump within it.
                        if (session.playlistSession.value != null) session.jumpToPlaylistItem(qid)
                        else tracks.find { it.id == qid }?.let { session.playMusic(it.id, it.title) }
                    },
                    onOpenArtist = onOpenArtist,
                    onRemoveFromQueue = { qid -> session.removeFromQueue(qid) },
                    onMoveQueueItem = { from, to -> session.moveQueueItem(from, to) }
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
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            // System back also returns to the playlist list, like the top bar's arrow.
            androidx.activity.compose.BackHandler { onBackToPlaylists() }
            CatalogContent(state) { tracks ->
                MusicPlaylistScreen(
                    name, tracks,
                    onOpen = { id ->
                        // Tapping a song starts the whole playlist AT that item.
                        scope.launch {
                            session.startPlaylistSession(name, startId = id) ?: return@launch
                            onOpenPlayer(id)
                        }
                    },
                    onDelete = { com.example.onespace.core.designsystem.AppPlaylistStore.remove(it) },
                    onPlay = {
                        scope.launch {
                            val started = session.startPlaylistSession(name) ?: return@launch
                            val current = started.current ?: return@launch
                            onOpenPlayer(current.id)
                        }
                    }
                )
            }
        }
    }
}
