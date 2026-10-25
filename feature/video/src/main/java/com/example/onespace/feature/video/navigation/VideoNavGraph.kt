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
import com.example.onespace.domain.session.port.PlaybackStatus
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle

import com.example.onespace.feature.video.presentation.*

fun NavGraphBuilder.videoGraph(
    session: SessionCoordinator,
    getGraphEntry: () -> NavBackStackEntry,
    onOpenVideo: (String) -> Unit,
    onBrowseNews: () -> Unit,
    onOpenArtist: (String) -> Unit = {},
    onOpenFromPlaylist: (String, String) -> Unit = { _, _ -> }
) {
    navigation<VideoRoute.Graph>(startDestination = VideoRoute.Feed) {
        composable<VideoRoute.Feed> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: VideoViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            val continueList by vm.continueWatching.collectAsStateWithLifecycle()
            val offline by vm.offline.collectAsStateWithLifecycle()
            val topics by vm.selectedTopics.collectAsStateWithLifecycle()
            val history by vm.history.collectAsStateWithLifecycle()
            CatalogContent(
                state,
                offline = offline,
                onRetry = vm::retry,
                empty = EmptySpec(Icons.Filled.PlayCircle, "Chưa có video nào", "Làm mới", vm::retry)
            ) { videos ->
                VideoScreen(
                    videos, topics, history,
                    onOpen = { id ->
                        videos.find { it.id == id }?.let { session.playVideo(it.id, it.title) }
                        onOpenVideo(id)
                    },
                    onOpenChannel = onOpenArtist
                )
            }
        }
        composable<VideoRoute.Artist> { entry ->
            val creator = entry.toRoute<VideoRoute.Artist>().creator
            val owner = remember(entry) { getGraphEntry() }
            val vm: VideoViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { videos ->
                val historyEntries by vm.history.collectAsStateWithLifecycle()
                val historyMap = remember(historyEntries) {
                    historyEntries.associate { it.videoId to it.positionMs }
                }
                ArtistProfileScreen(creator, videos, onOpen = { id ->
                    videos.find { it.id == id }?.let { session.playVideo(it.id, it.title) }
                    onOpenVideo(id)
                }, watchProgress = historyMap)
            }
        }
        composable<VideoRoute.Playlist> { entry ->
            val name = entry.toRoute<VideoRoute.Playlist>().name
            val owner = remember(entry) { getGraphEntry() }
            val vm: VideoViewModel = hiltViewModel(owner)
            val state by vm.uiState.collectAsStateWithLifecycle()
            CatalogContent(state) { videos ->
                val historyEntries by vm.history.collectAsStateWithLifecycle()
                val historyMap = remember(historyEntries) {
                    historyEntries.associate { it.videoId to it.positionMs }
                }
                VideoPlaylistScreen(
                    name, videos,
                    watchProgress = historyMap,
                    onOpen = { id ->
                        videos.find { it.id == id }?.let { session.playVideo(it.id, it.title) }
                        onOpenFromPlaylist(id, name)
                    },
                    onPlay = { items ->
                        val settings = com.example.onespace.core.designsystem.AppPlaylistStore.settingsOf(name)
                        val start = if (settings.order == "shuffle") items.randomOrNull() else items.firstOrNull()
                        start?.let {
                            session.playVideo(it.id, it.title)
                            onOpenFromPlaylist(it.id, name)
                        }
                    }
                )
            }
        }
        composable<VideoRoute.Detail> { entry ->
            val route = entry.toRoute<VideoRoute.Detail>()
            val id = route.id
            val playlistName = route.playlist
            val owner = remember(entry) { getGraphEntry() }
            val vm: VideoViewModel = hiltViewModel(owner)
            // F-VID-09: sound-only is a per-view choice — every new video
            // opens with the picture on, never inherits the previous state.
            val audioOnlyFlag by vm.audioOnly.collectAsStateWithLifecycle()
            LaunchedEffect(id) {
                if (vm.audioOnly.value) vm.setAudioOnly(false)
            }
            LaunchedEffect(audioOnlyFlag) {
                com.example.onespace.core.designsystem.AudioOnlyState.set(audioOnlyFlag)
            }
            val state by vm.uiState.collectAsStateWithLifecycle()
            val playback by session.video.collectAsStateWithLifecycle()
            CatalogContent(state) { videos ->
                val video = videos.find { it.id == id }
                val active = playback.videoId == video?.id
                // Inside a playlist the list below the player IS the playlist.
                val relatedList = if (playlistName != null)
                    videos.filter { it.id in (com.example.onespace.core.designsystem.AppPlaylistStore.playlists[playlistName]?.videoIds ?: emptyList()) }
                else videos
                val relatedLabel = playlistName ?: "Liên quan"
                // YouTube-style playlist autoplay: a finished video rolls
                // into the next one; only real playback (status ENDED) counts,
                // a restore parked at the end position must not trigger it.
                val playlistEnded = active && playlistName != null &&
                    playback.status == PlaybackStatus.ENDED
                LaunchedEffect(playlistEnded, id) {
                    if (playlistEnded && playlistName != null) {
                        val store = com.example.onespace.core.designsystem.AppPlaylistStore
                        val ids = store.playlists[playlistName]?.videoIds ?: emptyList()
                        val index = ids.indexOf(id)
                        val settings = store.settingsOf(playlistName)
                        // Order decides HOW the next one is picked; repeat only
                        // decides whether the end of the list wraps around.
                        val nextId = when {
                            settings.order == "shuffle" ->
                                ids.filter { it != id }.randomOrNull()
                            else -> ids.getOrNull(index + 1)
                                ?: if (settings.repeat && ids.isNotEmpty()) ids.first() else null
                        }
                        if (nextId != null) {
                            videos.find { it.id == nextId }?.let {
                                session.playVideo(it.id, it.title)
                                onOpenFromPlaylist(nextId, playlistName)
                            }
                        }
                    }
                }
                val audioOnly by vm.audioOnly.collectAsStateWithLifecycle()
                val watchLater by vm.watchLaterIds.collectAsStateWithLifecycle()
                val downloads by vm.downloads.collectAsStateWithLifecycle()
                val speed by vm.speed.collectAsStateWithLifecycle()
                val quality by vm.quality.collectAsStateWithLifecycle()
                var mp3Offer by remember { mutableStateOf(false) }
                val context = androidx.compose.ui.platform.LocalContext.current
                VideoDetailScreen(
                    video = video,
                    related = relatedList,
                    playing = active && playback.playRequested && playback.error == null,
                    muted = active && playback.muted,
                    buffering = active && playback.status == PlaybackStatus.BUFFERING,
                    suppressed = active && playback.suppressed,
                    error = playback.error.takeIf { active },
                    positionMs = if (active) playback.positionMs else 0,
                    totalMs = if (active) playback.totalMs else 0,
                    videoSurface = { modifier, onDoubleTap ->
                        VideoSurface(vm.player, modifier, onDoubleTap)
                    },
                    audioOnly = audioOnly,
                    onAudioOnly = { vm.toggleAudioOnly() },
                    onSoundOnly = {
                        vm.setAudioOnly(true)
                        onBrowseNews()
                    },
                    speed = speed,
                    onSpeed = vm::setSpeed,
                    quality = quality,
                    onQuality = vm::setQuality,
                    onSeekTo = session::seekVideoTo,
                    watchLaterActive = video?.id in watchLater,
                    downloaded = video?.id in downloads,
                    onToggleDownload = {
                        video?.let { v ->
                            vm.toggleDownload(
                                v.id,
                                v.title,
                                v.creator + " · " + v.duration
                            )
                            android.widget.Toast.makeText(
                                context,
                                if (v.id !in downloads) "Đã thêm vào Đã tải (Hồ sơ)" else "Đã bỏ khỏi Đã tải",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    // F-VID-10: holding the video opens this confirm, not a button.
                    onExtractMp3 = { mp3Offer = true },
                    onOpenArtist = onOpenArtist,
                    onQuizCorrect = { vm.awardEndOfVideoQuiz() },
                    onShare = {
                        video?.let { v ->
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, "Xem \"${v.title}\" trên OneSpace")
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, "Chia sẻ"))
                        }
                    },
                    onWatchLater = {
                        video?.let { v ->
                            vm.toggleWatchLater(v.id, v.title)
                            android.widget.Toast.makeText(
                                context,
                                if (vm.watchLaterIds.value.contains(v.id)) "Đã thêm vào Xem sau" else "Đã bỏ khỏi Xem sau",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    onToggle = {
                        if (active) session.toggleVideo()
                        else video?.let { session.playVideo(it.id, it.title) }
                    },
                    onPrevVideo = {
                        val idx = videos.indexOfFirst { it.id == video?.id }
                        if (idx > 0) {
                            val prev = videos[idx - 1]
                            session.playVideo(prev.id, prev.title)
                            onOpenVideo(prev.id)
                        }
                    },
                    onNextVideo = {
                        val idx = videos.indexOfFirst { it.id == video?.id }
                        if (idx >= 0 && idx < videos.size - 1) {
                            val next = videos[idx + 1]
                            session.playVideo(next.id, next.title)
                            onOpenVideo(next.id)
                        }
                    },
                    onSeekBy = session::seekVideoBy,
                    onMinimize = onBrowseNews,
                    relatedTitle = relatedLabel,
                    inPlaylist = playlistName != null,
                    playlistOrder = if (playlistName != null)
                        com.example.onespace.core.designsystem.AppPlaylistStore.settingsOf(playlistName).order
                    else "sequential",
                    playlistRepeat = playlistName != null &&
                        com.example.onespace.core.designsystem.AppPlaylistStore.settingsOf(playlistName).repeat,
                    onTogglePlaylistOrder = { playlistName?.let { com.example.onespace.core.designsystem.AppPlaylistStore.toggleOrder(it) } },
                    onTogglePlaylistRepeat = { playlistName?.let { com.example.onespace.core.designsystem.AppPlaylistStore.toggleRepeat(it) } },
                    onOpenRelated = { relatedId ->
                        videos.find { it.id == relatedId }?.let { session.playVideo(it.id, it.title) }
                        if (playlistName != null) onOpenFromPlaylist(relatedId, playlistName)
                        else onOpenVideo(relatedId)
                    }
                )
                if (mp3Offer) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { mp3Offer = false },
                        title = { androidx.compose.material3.Text("Trích xuất MP3?") },
                        text = { androidx.compose.material3.Text("Lưu âm thanh của video này vào thư viện nhạc để nghe offline?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                mp3Offer = false
                                video?.let { v ->
                                    vm.addExtracted(v.id, v.title, v.creator, v.url, v.duration)
                                    android.widget.Toast.makeText(context, "Đã lưu vào thư viện Nhạc (mục Từ video của bạn)", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) { androidx.compose.material3.Text("Trích xuất") }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { mp3Offer = false }) { androidx.compose.material3.Text("Hủy") }
                        }
                    )
                }
            }
        }
    }
}

