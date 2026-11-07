package com.example.onespace.app

import com.example.onespace.core.designsystem.tr
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import com.example.onespace.app.ui.RecordFlow
import com.example.onespace.app.ui.RecordSource
import androidx.compose.material3.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.example.onespace.app.navigation.*
import com.example.onespace.app.ui.*
import com.example.onespace.core.designsystem.ReadingState
import com.example.onespace.core.designsystem.FloatingArticleCard
import com.example.onespace.core.designsystem.ScrubBar
import com.example.onespace.core.designsystem.oneSpaceColors
import com.example.onespace.core.media.VideoSurface
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.Section
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneSpaceApp() {
    // One coordinator for the entire shell; catalog ViewModels live inside feature graphs.
    val coordinator: SessionCoordinator = hiltViewModel()
    val media: MediaViewModel = hiltViewModel()
    val tts: TtsControlsViewModel = hiltViewModel()
    val downloadedContent: com.example.onespace.domain.session.repository.DownloadedContentRepository =
        hiltViewModel<DownloadsViewModel>().repository
    val musicState by coordinator.music.collectAsStateWithLifecycle()
    val videoState by coordinator.video.collectAsStateWithLifecycle()
    val conflict by coordinator.conflict.collectAsStateWithLifecycle()
    val videoConflict by coordinator.videoConflict.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var floatingPlayerHeight by remember { mutableIntStateOf(0) }
    val floatingPosition = rememberFloatingPosition()
    // The floating window's speaker button: mute at once, restore on the next tap.
    var lastVideoVolume by remember { mutableStateOf(1f) }
    val nav = rememberNavController()
    val appState = rememberOneSpaceAppState(nav)
    val entry by nav.currentBackStackEntryAsState()
    val selected = TopLevelDestination.entries.find { it.contains(entry?.destination) }
    val inMain = entry?.destination?.hierarchy?.any { it.hasRoute<AppRoute.Main>() } == true
    val screen = entry?.destination.contextScreen()
    // A playlist detail owns its own title ("Danh sách phát: X"), not the section's.
    val playlistName = entry.playlistName()
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    // Snackbar texts are used in callbacks, so they are resolved here in composition.
    val msgMusicStopped = tr("Đã dừng phát nhạc", "Music stopped")
    val msgUndo = tr("Hoàn tác", "Undo")
    val msgSavedWatchLater = tr("Đã lưu vào Xem sau", "Saved to Watch later")
    // One search per top-level section; the field drops down under the top bar.
    val musicSpeed by coordinator.musicSpeed.collectAsStateWithLifecycle()
    val videoSpeed by coordinator.videoSpeed.collectAsStateWithLifecycle()
    val videoVolume by coordinator.videoVolume.collectAsStateWithLifecycle()
    // TTS truth for the reading card: keeps flowing while the reader is on other tabs.
    val ttsState by tts.engine.state.collectAsStateWithLifecycle()
    var recordSource by remember { mutableStateOf<RecordSource?>(null) }
    val searches = remember { Section.entries.associateWith { com.example.onespace.core.designsystem.SearchState() } }
    val activeSearch = selected?.let { searches[it.section] }
    fun select(section: Section) {
        searches.values.forEach { it.close() }
        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.SegmentTick)
        coordinator.enter(section)
        appState.navigateToSection(section)
    }

    LaunchedEffect(inMain) {
        if (inMain) coordinator.restoredSection.collect { restored ->
            if (restored != null) {
                select(restored)
                coordinator.consumeRestoredSection()
            }
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, coordinator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) coordinator.background()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val onHome = selected?.section == Section.HOME && screen == null
    val videoId = videoState.videoId
    val trackId = musicState.trackId
    // F-GAME-08: inside a game the video renders as a split top half, not the floating window.
    // Feed-in-place: on Home the feed card is the player surface, so the
    // floating window / music dock only take over on the other tabs.
    val videoDocked = inMain && videoId != null && screen != ContextScreen.VIDEO && screen != ContextScreen.GAMES && !onHome
    val showFloat = videoDocked
    // Music dock stays in games (Beat Tiles plays along) but in read-only mode:
    // progress + speed visible, no pause/close.
    val showMini = inMain && trackId != null && screen != ContextScreen.MUSIC && !onHome
    val inGame = screen == ContextScreen.GAMES
    val showArticleFloat = inMain && ReadingState.articleId != null && screen != ContextScreen.NEWS && screen != ContextScreen.GAMES

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            if (inMain) Column { TopAppBar(
                title = { Text(playlistName?.let { tr("Danh sách phát: ", "Playlist: ") + it } ?: (screen?.title() ?: selected?.label() ?: "")) },
                navigationIcon = {
                    if (screen != null || playlistName != null) IconButton(onClick = {
                        // Back from a playlist goes to the playlist list, not the section tab.
                        if (playlistName != null) {
                            if (!appState.backToPlaylists()) appState.navigateBack()
                        } else appState.navigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Quay lại", "Back"))
                    }
                },
                actions = {
                    // Karaoke / Record on the player screens, next to the back arrow's row.
                    if (screen == ContextScreen.VIDEO && videoId != null) IconButton(onClick = {
                        recordSource = RecordSource(videoState.title.ifBlank { "Video" }, isVideo = true)
                    }) {
                        Icon(Icons.Filled.Mic, contentDescription = tr("Ghi âm / Karaoke", "Record / Karaoke"))
                    }
                    if (screen == ContextScreen.MUSIC && trackId != null) {
                        IconButton(onClick = {
                            recordSource = RecordSource(musicState.title.ifBlank { "Nhạc" }, isVideo = false)
                        }) {
                            Icon(Icons.Filled.Mic, contentDescription = tr("Ghi âm / Karaoke", "Record / Karaoke"))
                        }
                        // Beat Tiles with this song; off when the song has no beat data.
                        val beat = musicState.queue.firstOrNull { it.id == trackId }?.bpm
                        IconButton(enabled = beat != null, onClick = { appState.openGame("g6", trackId) }) {
                            Icon(Icons.Filled.GridOn, contentDescription = tr("Chơi Beat Tiles với bài này", "Play Beat Tiles with this song"))
                        }
                    }
                    // Search is a magnifier on the section roots; the field opens under the bar.
                    // Always shows the magnifier icon (no X) — tapping the icon toggles, tapping content closes.
                    if (screen == null && activeSearch != null) IconButton(onClick = activeSearch::toggle) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = tr("Tìm kiếm", "Search")
                        )
                    }
                    // Guess-song shortcut: a music-note icon next to the search magnifier, Music tab only.
                    if (screen == null && selected?.section == com.example.onespace.domain.session.Section.MUSIC) {
                        IconButton(onClick = {
                            coordinator.cancelPendingRestore()
                            appState.openGame("g3")
                        }) {
                            Icon(
                                androidx.compose.material.icons.Icons.Filled.MusicNote,
                                contentDescription = tr("Chơi Đoán bài hát", "Play Guess the Song")
                            )
                        }
                    }
                    // Profile is secondary: reachable from Home only, not every screen.
                    if (onHome) IconButton(onClick = {
                        coordinator.cancelPendingRestore()
                        appState.openProfile()
                    }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = tr("Tài khoản", "Account"))
                    }
                }
            )
                if (screen == null && activeSearch?.open == true) {
                    val hint = when (selected?.section) {
                        Section.HOME -> tr("Tìm trong OneSpace", "Search OneSpace")
                        Section.VIDEO -> tr("Tìm video hoặc kênh", "Search videos or channels")
                        Section.MUSIC -> tr("Tìm bài hát hoặc nghệ sĩ", "Search songs or artists")
                        Section.NEWS -> tr("Tìm bài báo", "Search articles")
                        else -> tr("Tìm trò chơi", "Search games")
                    }
                    com.example.onespace.core.designsystem.AppSearchField(activeSearch, hint)
                }
            }
        },
        bottomBar = {
            if (inMain) Column {
                // Hierarchy: content > mini player > bottom navigation.
                if (showMini) MusicMiniPlayer(
                    title = musicState.title,
                    playback = musicState,
                    onExpand = {
                        // The Beat Tiles backing track is not expandable mid-round.
                        if (!com.example.onespace.core.designsystem.GameMusicState.active) {
                            trackId?.let { appState.openMusicPlayer(it) }
                        }
                    },
                    onToggle = coordinator::toggleMusic,
                    speed = musicSpeed,
                    onCycleSpeed = coordinator::cycleMusicSpeed,
                    onSeekTo = coordinator::seekMusicTo,
                    // A song picked for the Beat Tiles round (or sent from Now Playing)
                    // is read-only: progress + speed visible, pause/close hidden.
                    // Music the user was already listening to keeps full controls —
                    // in every game, not just outside them.
                    showProgress = true,
                    interactive = !com.example.onespace.core.designsystem.GameMusicState.active,
                    onClose = {
                        // Undo pattern: the user can bring the music back within 4 seconds.
                        val lastId = trackId
                        val lastTitle = musicState.title
                        coordinator.closeMusic()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                msgMusicStopped, actionLabel = msgUndo,
                                // With an action the default is Indefinite — it
                                // would never leave; give it ~10s instead.
                                duration = androidx.compose.material3.SnackbarDuration.Long)
                            if (result == SnackbarResult.ActionPerformed && lastId != null) {
                                coordinator.playMusic(lastId, lastTitle.ifBlank { lastId })
                            }
                        }
                    }
                )
                if (showArticleFloat) {
                    FloatingArticleCard(
                        title = ReadingState.title,
                        ttsActive = ttsState.totalSegments > 0,
                        ttsPlaying = ttsState.isPlaying,
                        ttsProgress = ttsState.progress,
                        onToggleTts = {
                            if (ttsState.isPlaying) scope.launch { tts.engine.pause() }
                            else scope.launch { tts.engine.play() }
                        },
                        onSeekTts = { fraction -> scope.launch { tts.engine.seekTo(fraction) } },
                        onExpand = { ReadingState.articleId?.let { appState.openArticle(it) } },
                        // Closing the card also silences the voice — otherwise the
                        // reading keeps playing with no way left to stop it.
                        onClose = {
                            scope.launch { tts.engine.stop() }
                            ReadingState.close()
                        }
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { select(tab.section) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label()) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // Tap anywhere in the content area closes an open search field.
                .pointerInput(activeSearch?.open) {
                    if (activeSearch?.open == true) {
                        detectTapGestures {
                            activeSearch?.close()
                        }
                    }
                }
                // Swipe left/right flips between the five sections —
                // continuous browsing, even from a player screen where
                // content keeps running. Games opt out: they own swipes.
                .pointerInput(inMain, screen, selected) {
                    // Games own their swipes (Snake, tiles); everywhere else
                    // the horizontal swipe keeps the five sections flowing.
                    if (inMain && screen != ContextScreen.GAMES && selected != null) {
                        val order = TopLevelDestination.entries
                        val idx = order.indexOfFirst { it.section == selected!!.section }
                        val next = order.getOrNull(idx + 1)?.section
                        val prev = order.getOrNull(idx - 1)?.section
                        val threshold = 80.dp.toPx()
                        var acc = 0f
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                when {
                                    acc <= -threshold && next != null -> select(next)
                                    acc >= threshold && prev != null -> select(prev)
                                }
                                acc = 0f
                            },
                            onDragCancel = { acc = 0f }
                        ) { change, amount ->
                            change.consume()
                            acc += amount
                        }
                    }
                }
        ) {
            Column(Modifier.fillMaxSize()) {
                // F-SPLIT: when active, the video docks at the top of the content column,
                // pushing the NavHost down — not overlaying it.
                if (com.example.onespace.core.designsystem.SplitVideoState.active && showFloat) {
                    Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
                        VideoSurface(
                            player = media.videoPlayer,
                            modifier = Modifier.fillMaxSize(),
                            onDoubleTap = coordinator::toggleVideo
                        )
                        // Tap shows controls that fade after 2s.
                        var splitControls by remember { mutableStateOf(false) }
                        val splitPlaying = videoState.playRequested
                        LaunchedEffect(splitControls, splitPlaying) {
                            if (splitControls && splitPlaying) { kotlinx.coroutines.delay(2000); splitControls = false }
                        }
                        Box(
                            Modifier.fillMaxSize().clickable(
                                indication = null, interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                            ) { splitControls = !splitControls }
                        )
                        if (splitControls) {
                            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.3f)))
                            Row(
                                Modifier.align(androidx.compose.ui.Alignment.Center),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                IconButton(onClick = { coordinator.seekVideoBy(-10_000) }) {
                                    Icon(Icons.Filled.Replay10, contentDescription = tr("Lùi 10 giây", "Back 10 seconds"),
                                        tint = MaterialTheme.oneSpaceColors.onMedia, modifier = Modifier.size(30.dp))
                                }
                                IconButton(onClick = coordinator::toggleVideo, modifier = Modifier.size(46.dp)) {
                                    Icon(
                                        if (videoState.playRequested) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (videoState.playRequested) tr("Tạm dừng", "Pause") else tr("Phát", "Play"),
                                        tint = MaterialTheme.oneSpaceColors.onMedia, modifier = Modifier.size(30.dp)
                                    )
                                }
                                IconButton(onClick = { coordinator.seekVideoBy(10_000) }) {
                                    Icon(Icons.Filled.Forward10, contentDescription = tr("Tới 10 giây", "Forward 10 seconds"),
                                        tint = MaterialTheme.oneSpaceColors.onMedia, modifier = Modifier.size(30.dp))
                                }
                            }
                            Row(
                                Modifier.align(androidx.compose.ui.Alignment.TopEnd).padding(8.dp)
                            ) {
                                IconButton(onClick = { com.example.onespace.core.designsystem.SplitVideoState.set(false) }) {
                                    Icon(Icons.Filled.PictureInPictureAlt, contentDescription = tr("Thu nhỏ video", "Minimize video"),
                                        tint = MaterialTheme.oneSpaceColors.onMedia)
                                }
                                IconButton(onClick = {
                                    coordinator.closeVideo()
                                    scope.launch { snackbarHostState.showSnackbar(msgSavedWatchLater) }
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = tr("Đóng", "Close"),
                                        tint = MaterialTheme.oneSpaceColors.onMedia)
                                }
                            }
                        }
                        // Progress bar at bottom edge.
                        ScrubBar(
                            videoState.positionMs, videoState.totalMs,
                            onSeek = coordinator::seekVideoTo,
                            modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter),
                            trackHeight = 3.dp, knobRadius = 5.dp,
                            trackColor = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.3f),
                            progressColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Box(Modifier.fillMaxWidth().weight(1f)) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.example.onespace.core.designsystem.LocalSearchState provides (activeSearch ?: com.example.onespace.core.designsystem.SearchState())
                    ) {
                        AppNavigation(appState, coordinator, ::select,
                            Modifier.fillMaxSize(),
                            downloadedContent = downloadedContent,
                            videoPlayer = media.videoPlayer)
                    }
                }
            }
            if (showFloat && !com.example.onespace.core.designsystem.SplitVideoState.active) FloatingVideo(
                player = media.videoPlayer,
                title = videoState.title,
                playback = videoState,
                position = floatingPosition,
                onHeightChanged = { floatingPlayerHeight = it },
                onExpand = { videoId?.let { appState.openVideo(it) } },
                onToggle = coordinator::toggleVideo,
                onSeekBy = coordinator::seekVideoBy,
                speed = videoSpeed,
                onCycleSpeed = coordinator::cycleVideoSpeed,
                onToggleMute = {
                    if (videoVolume > 0f) { lastVideoVolume = videoVolume; coordinator.setVideoVolume(0f) }
                    else coordinator.setVideoVolume(lastVideoVolume.coerceAtLeast(0.2f))
                },
                onSeekTo = coordinator::seekVideoTo,
                onClose = {
                    coordinator.closeVideo()
                    scope.launch { snackbarHostState.showSnackbar(msgSavedWatchLater) }
                },
            )
        }
    }

    recordSource?.takeIf { inMain }?.let { source ->
        RecordFlow(
            source = source,
            player = media.videoPlayer,
            isPlaying = { if (source.isVideo) videoState.playRequested else musicState.playRequested },
            onPlayPauseSource = { if (source.isVideo) coordinator.toggleVideo() else coordinator.toggleMusic() },
            onDismiss = { recordSource = null }
        )
    }

    // Case C1: while music collides with a playing video, one of three
    // choices must be picked; "Không hỏi lại" is a tick combined with the
    // choice so the remembered rule is never ambiguous.
    conflict?.takeIf { inMain }?.let { request ->
        var dontAskAgain by remember(request) { mutableStateOf(false) }
        fun choose(rule: MediaConflictRule) =
            coordinator.resolveMusicConflict(rule, remember = dontAskAgain)
        AlertDialog(
            onDismissRequest = { /* one of the three must be chosen */ },
            title = { Text(tr("Phát \"", "Play \"") + request.track.title + "\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Video đang phát", "Video is playing"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { dontAskAgain = !dontAskAgain }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = dontAskAgain, onCheckedChange = { dontAskAgain = it })
                        Text(tr("Không hỏi lại", "Don't ask again"), style = MaterialTheme.typography.bodyMedium)
                    }
                    ConflictRow(tr("Tắt tiếng video", "Mute the video"), MediaConflictRule.MUTE_VIDEO, ::choose)
                    ConflictRow(tr("Dừng video", "Pause the video"), MediaConflictRule.PAUSE_VIDEO, ::choose)
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
    // C2: opening a video while music plays asks, mirroring C1.
    videoConflict?.takeIf { inMain }?.let { request ->
        var c2DontAsk by remember(request) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { /* must choose */ },
            title = { Text(tr("Mở \"", "Open \"") + request.title + "\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Nhạc đang phát", "Music is playing"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { c2DontAsk = !c2DontAsk }
                            .padding(vertical = 4.dp)
                    ) {
                        Checkbox(checked = c2DontAsk, onCheckedChange = { c2DontAsk = it })
                        Text(tr("Không hỏi lại", "Don't ask again"), style = MaterialTheme.typography.bodyMedium)
                    }
                    VideoConflictRow(tr("Tắt tiếng video", "Mute the video")) {
                        coordinator.resolveVideoConflict(pauseMusic = false, remember = c2DontAsk)
                    }
                    VideoConflictRow(tr("Dừng nhạc", "Stop the music")) {
                        coordinator.resolveVideoConflict(pauseMusic = true, remember = c2DontAsk)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}

@Composable
private fun VideoConflictRow(label: String, choose: () -> Unit) {
    FilledTonalButton(
        onClick = choose,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

/** One full-width text line: the choice reads as a sentence, not a picture. */
@Composable
private fun ConflictRow(
    label: String, rule: MediaConflictRule,
    choose: (MediaConflictRule) -> Unit
) {
    FilledTonalButton(
        onClick = { choose(rule) },
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label)
    }
}

/** Simple wrapper so the shell can access the repository without a feature module. */
@dagger.hilt.android.lifecycle.HiltViewModel
class DownloadsViewModel @javax.inject.Inject constructor(
    val repository: com.example.onespace.domain.session.repository.DownloadedContentRepository
) : androidx.lifecycle.ViewModel()

/** Shell access to the shared TTS engine: the reading card shows its progress and pauses/resumes it. */
@dagger.hilt.android.lifecycle.HiltViewModel
class TtsControlsViewModel @javax.inject.Inject constructor(
    val engine: com.example.onespace.domain.news.repository.TtsEngine
) : androidx.lifecycle.ViewModel()
