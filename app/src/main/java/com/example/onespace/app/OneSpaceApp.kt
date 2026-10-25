package com.example.onespace.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
            SnackbarHost(snackbarHostState, Modifier.padding(
                bottom = if (showFloat) with(density) { floatingPlayerHeight.toDp() } else 0.dp
            ))
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
        Box(Modifier.fillMaxSize().padding(padding)) {
            AppNavigation(appState, coordinator, ::select,
                Modifier.fillMaxSize().padding(bottom = if (showFloat) with(density) { floatingPlayerHeight.toDp() } else 0.dp),
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

    // Case C1: the first time music collides with a playing video, ask once and remember.
    conflict?.takeIf { inMain }?.let { request ->
        AlertDialog(
            onDismissRequest = coordinator::dismissMusicConflict,
            title = { Text("Phát \"" + request.track.title + "\"?") },
            text = { Text("Video đang phát.") },
            confirmButton = {
                Column {
                    ConflictOption("🔇 Tắt tiếng video", MediaConflictRule.MUTE_VIDEO) {
                        coordinator.resolveMusicConflict(it)
                    }
                    ConflictOption("⏸ Dừng video", MediaConflictRule.PAUSE_VIDEO) {
                        coordinator.resolveMusicConflict(it)
                    }
                    ConflictOption("▶ Chỉ giữ video", MediaConflictRule.KEEP_VIDEO) {
                        coordinator.resolveMusicConflict(it)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = coordinator::dismissMusicConflict) { Text("Bỏ qua") }
            }
        )
    }
}

@Composable
private fun ConflictOption(label: String, rule: MediaConflictRule, choose: (MediaConflictRule) -> Unit) {
    TextButton(onClick = { choose(rule) }) { Text(label) }
}

/** Simple wrapper so the shell can access the repository without a feature module. */
@dagger.hilt.android.lifecycle.HiltViewModel
class DownloadsViewModel @javax.inject.Inject constructor(
    val repository: com.example.onespace.domain.session.repository.DownloadedContentRepository
) : androidx.lifecycle.ViewModel()
