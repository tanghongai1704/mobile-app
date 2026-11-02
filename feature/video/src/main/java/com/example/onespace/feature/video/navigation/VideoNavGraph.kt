package com.example.onespace.feature.video.navigation

import com.example.onespace.core.designsystem.oneSpaceColors
import androidx.compose.foundation.background
import com.example.onespace.core.designsystem.tr
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
                empty = EmptySpec(Icons.Filled.PlayCircle, tr("Chưa có video nào", "No videos yet"), tr("Làm mới", "Refresh"), vm::retry)
            ) { videos ->
                val context = androidx.compose.ui.platform.LocalContext.current
                val downloads by vm.downloads.collectAsStateWithLifecycle()
                val watchLater by vm.watchLaterIds.collectAsStateWithLifecycle()
                val volume by session.videoVolume.collectAsStateWithLifecycle()
                var lastVolume by remember { mutableStateOf(1f) }
                var saveTarget by remember { mutableStateOf<com.example.onespace.domain.video.model.Video?>(null) }
                val systemVolume = rememberSystemVolume()
                val shareTemplate = tr("Xem \"%s\" trên OneSpace", "Watch \"%s\" on OneSpace")
                val msgShare = tr("Chia sẻ", "Share")
                val msgDownloadAdded = tr("Đã thêm vào Đã tải (Hồ sơ)", "Added to Downloaded (Profile)")
                val msgDownloadRemoved = tr("Đã bỏ khỏi Đã tải", "Removed from Downloaded")
                val msgWatchLaterRemoved = tr("Đã bỏ khỏi Xem sau", "Removed from Watch later")
                val msgWatchLaterAdded = tr("Đã thêm vào Xem sau", "Added to Watch later")
                val actions = remember(downloads, shareTemplate, msgShare, msgDownloadAdded, msgDownloadRemoved) {
                    VideoCardActions(
                        downloaded = { it.id in downloads },
                        onShare = { v ->
                            val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareTemplate.format(v.title))
                            }
                            context.startActivity(android.content.Intent.createChooser(send, msgShare))
                        },
                        onToggleDownload = { v ->
                            vm.toggleDownload(v.id, v.title, v.creator + " · " + v.duration)
                            android.widget.Toast.makeText(
                                context,
                                if (v.id !in downloads) msgDownloadAdded else msgDownloadRemoved,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onAddToPlaylist = { saveTarget = it }
                    )
                }
                VideoScreen(
                    videos, topics, history,
                    onOpen = { id ->
                        videos.find { it.id == id }?.let { session.playVideo(it.id, it.title) }
                        onOpenVideo(id)
                    },
                    onOpenChannel = onOpenArtist,
                    cardActions = actions,
                    volume = volume,
                    onToggleMute = {
                        if (volume > 0f) { lastVolume = volume; session.setVideoVolume(0f) }
                        else session.setVideoVolume(lastVolume.coerceAtLeast(0.2f))
                    },
                    systemVolume = systemVolume,
                    savedVideo = saveTarget,
                    watchLaterIds = watchLater,
                    onToggleWatchLater = { v ->
                        val wasSaved = v.id in watchLater
                        vm.toggleWatchLater(v.id, v.title)
                        android.widget.Toast.makeText(
                            context,
                            if (wasSaved) msgWatchLaterRemoved else msgWatchLaterAdded,
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    },
                    onDismissSave = { saveTarget = null }
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
                val relatedLabel = playlistName ?: tr("Liên quan", "Related")
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
                // Session volume lives on the coordinator, not per-video state.
                val volume by session.videoVolume.collectAsStateWithLifecycle()
                val quizCombo by vm.quizCombo.collectAsStateWithLifecycle()
                val quizRound by vm.quizRound.collectAsStateWithLifecycle()
                var mp3Offer by remember { mutableStateOf(false) }
                // Strings used inside click callbacks are resolved here, in composition.
                val msgDownloadAdded = tr("Đã thêm vào Đã tải (Hồ sơ)", "Added to Downloaded (Profile)")
                val msgDownloadRemoved = tr("Đã bỏ khỏi Đã tải", "Removed from Downloaded")
                val shareTemplate = tr("Xem \"%s\" trên OneSpace", "Watch \"%s\" on OneSpace")
                val msgShare = tr("Chia sẻ", "Share")
                val msgWatchLaterRemoved = tr("Đã bỏ khỏi Xem sau", "Removed from Watch later")
                val msgWatchLaterAdded = tr("Đã thêm vào Xem sau", "Added to Watch later")
                val msgMp3Saved = tr("Đã lưu vào thư viện Nhạc (mục Từ video của bạn)", "Saved to your Music library (From your videos)")
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
                        VideoSurface(
                            vm.player,
                            modifier.background(androidx.compose.material3.MaterialTheme.oneSpaceColors.mediaBackdrop),
                            onDoubleTap
                        )
                    },
                    audioOnly = audioOnly,
                    onAudioOnly = { vm.toggleAudioOnly() },
                    onSoundOnly = {
                        vm.setAudioOnly(true)
                        onBrowseNews()
                    },
                    speed = speed,
                    onSpeed = vm::setSpeed,
                    volume = volume,
                    onVolume = session::setVideoVolume,
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
                                if (v.id !in downloads) msgDownloadAdded else msgDownloadRemoved,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    // F-VID-10: holding the video opens this confirm, not a button.
                    onExtractMp3 = { mp3Offer = true },
                    onOpenArtist = onOpenArtist,
                    quizCombo = quizCombo,
                    quizRound = quizRound,
                    onQuizAnswered = vm::answerQuiz,
                    onShare = {
                        video?.let { v ->
                            val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(android.content.Intent.EXTRA_TEXT, shareTemplate.format(v.title))
                            }
                            context.startActivity(android.content.Intent.createChooser(sendIntent, msgShare))
                        }
                    },
                    onWatchLater = {
                        video?.let { v ->
                            val wasSaved = vm.watchLaterIds.value.contains(v.id)
                            vm.toggleWatchLater(v.id, v.title)
                            android.widget.Toast.makeText(
                                context,
                                if (wasSaved) msgWatchLaterRemoved else msgWatchLaterAdded,
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
                        title = { androidx.compose.material3.Text(tr("Trích xuất MP3?", "Extract MP3?")) },
                        text = { androidx.compose.material3.Text(tr("Lưu âm thanh của video này vào thư viện nhạc để nghe offline?", "Save this video's audio to your music library for offline listening?")) },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                mp3Offer = false
                                video?.let { v ->
                                    vm.addExtracted(v.id, v.title, v.creator, v.url, v.duration)
                                    android.widget.Toast.makeText(context, msgMp3Saved, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) { androidx.compose.material3.Text(tr("Trích xuất", "Extract")) }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = { mp3Offer = false }) { androidx.compose.material3.Text(tr("Hủy", "Cancel")) }
                        }
                    )
                }
            }
        }
    }
}



/**
 * The phone's media volume as 0..1, emitted only after it changes (null until then), so the
 * Video list can flash a level bar next to its speaker when the hardware keys are used.
 */
@androidx.compose.runtime.Composable
private fun rememberSystemVolume(): Float? {
    val context = androidx.compose.ui.platform.LocalContext.current
    var level by remember { mutableStateOf<Float?>(null) }
    androidx.compose.runtime.DisposableEffect(context) {
        val audio = context.getSystemService(android.content.Context.AUDIO_SERVICE) as? android.media.AudioManager
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context?, intent: android.content.Intent?) {
                val max = audio?.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC) ?: return
                val now = audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC)
                if (max > 0) level = now.toFloat() / max
            }
        }
        // VOLUME_CHANGED_ACTION is the long-standing broadcast for hardware key changes.
        val filter = android.content.IntentFilter("android.media.VOLUME_CHANGED_ACTION")
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return level
}
