package com.example.onespace.app

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
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

    val nav = rememberNavController()
    val appState = rememberOneSpaceAppState(nav)
    val entry by nav.currentBackStackEntryAsState()
    val selected = TopLevelDestination.entries.find { it.contains(entry?.destination) }
    val inMain = entry?.destination?.hierarchy?.any { it.hasRoute<AppRoute.Main>() } == true
    val contextTitle = entry?.destination.contextTitle()
    fun select(section: Section) {
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

    val onHome = selected?.section == Section.HOME && contextTitle == null
    val videoId = videoState.videoId
    val trackId = musicState.trackId
    // F-GAME-08: inside a game the video renders as a split top half, not the floating window.
    val showFloat = inMain && videoId != null && contextTitle != "Video" && contextTitle != "Games"
    val showMini = inMain && trackId != null && contextTitle != "Music"
    val showArticleFloat = inMain && ReadingState.articleId != null && contextTitle != "News"

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },
        topBar = {
            if (inMain) TopAppBar(
                title = { Text(contextTitle ?: selected?.label ?: "") },
                navigationIcon = {
                    if (contextTitle != null) IconButton(onClick = appState::navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    // Profile is secondary: reachable from Home only, not every screen.
                    if (onHome) IconButton(onClick = {
                        coordinator.cancelPendingRestore()
                        appState.openProfile()
                    }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Tài khoản")
                    }
                }
            )
        },
        bottomBar = {
            if (inMain) Column {
                // Hierarchy: content > mini player > bottom navigation.
                if (showMini) MusicMiniPlayer(
                    title = musicState.title,
                    playback = musicState,
                    onExpand = { trackId?.let { appState.openMusicPlayer(it) } },
                    onToggle = coordinator::toggleMusic,
                    onClose = {
                        // Undo pattern: the user can bring the music back within 4 seconds.
                        val lastId = trackId
                        val lastTitle = musicState.title
                        coordinator.closeMusic()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                "Đã dừng phát nhạc", actionLabel = "Hoàn tác",
                                // With an action the default is Indefinite — it
                                // would never leave; give it ~10s instead.
                                duration = androidx.compose.material3.SnackbarDuration.Long)
                            if (result == SnackbarResult.ActionPerformed && lastId != null) {
                                coordinator.playMusic(lastId, lastTitle.ifBlank { lastId })
                            }
                        }
                    }
                )
                if (showArticleFloat) FloatingArticleCard(
                    title = ReadingState.title,
                    onExpand = { ReadingState.articleId?.let { appState.openArticle(it) } },
                    onClose = ReadingState::close
                )
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selected == tab,
                            onClick = { select(tab.section) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) }
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
                // Swipe left/right flips between the five sections —
                // continuous browsing, even from a player screen where
                // content keeps running. Games opt out: they own swipes.
                .pointerInput(inMain, contextTitle, selected) {
                    // Games own their swipes (Snake, tiles); everywhere else
                    // the horizontal swipe keeps the five sections flowing.
                    if (inMain && contextTitle != "Games" && selected != null) {
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
            AppNavigation(appState, coordinator, ::select,
                Modifier.fillMaxSize(),
                downloadedContent = downloadedContent)
            if (showFloat) FloatingVideo(
                player = media.videoPlayer,
                title = videoState.title,
                playback = videoState,
                onHeightChanged = { floatingPlayerHeight = it },
                onExpand = { videoId?.let { appState.openVideo(it) } },
                onToggle = coordinator::toggleVideo,
                onClose = {
                    coordinator.closeVideo()
                    scope.launch { snackbarHostState.showSnackbar("Đã lưu vào Xem sau") }
                }
            )
        }
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
            title = { Text("Phát \"" + request.track.title + "\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Video đang phát",
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
                        Text("Không hỏi lại", style = MaterialTheme.typography.bodyMedium)
                    }
                    ConflictRow("Tắt tiếng video", MediaConflictRule.MUTE_VIDEO, ::choose)
                    ConflictRow("Dừng video", MediaConflictRule.PAUSE_VIDEO, ::choose)
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
            title = { Text("Mở \"" + request.title + "\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nhạc đang phát",
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
                        Text("Không hỏi lại", style = MaterialTheme.typography.bodyMedium)
                    }
                    VideoConflictRow("Tắt tiếng video, nhạc vẫn phát") {
                        coordinator.resolveVideoConflict(pauseMusic = false, remember = c2DontAsk)
                    }
                    VideoConflictRow("Tắt tiếng nhạc, video có tiếng") {
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
