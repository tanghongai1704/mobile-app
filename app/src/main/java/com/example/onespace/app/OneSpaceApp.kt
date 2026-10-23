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
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.onespace.app.navigation.*
import com.example.onespace.app.ui.*
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.Section
import com.example.onespace.domain.session.repository.SessionPreferences
import com.example.onespace.feature.account.navigation.AccountNavigation
import com.example.onespace.feature.games.navigation.GamesNavigation
import com.example.onespace.feature.home.navigation.HomeNavigation
import com.example.onespace.feature.music.navigation.MusicNavigation
import com.example.onespace.feature.onboarding.navigation.OnboardingNavigation
import com.example.onespace.feature.video.navigation.VideoNavigation
import kotlinx.coroutines.launch

/**
 * IA: the top bar always answers "Where am I?" — the tab name on root screens,
 * or "<- Parent" on a detail screen. Branding and profile live only on Home.
 */
private fun NavDestination?.contextTitle(): String? = when (this?.route) {
    VideoNavigation.DETAIL -> "Video"
    MusicNavigation.NOW_PLAYING -> "Music"
    com.example.onespace.feature.news.navigation.NewsNavigation.ARTICLE -> "News"
    GamesNavigation.GAME -> "Games"
    AccountNavigation.ACCOUNT -> "Tài khoản"
    AccountNavigation.SETTINGS -> "Tùy chỉnh"
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneSpaceApp() {
    // One coordinator for the entire shell; catalog ViewModels live inside feature graphs.
    val coordinator: SessionCoordinator = hiltViewModel()
    val media: MediaViewModel = hiltViewModel()
    val session by coordinator.uiState.collectAsStateWithLifecycle()
    val musicState by coordinator.music.collectAsStateWithLifecycle()
    val videoState by coordinator.video.collectAsStateWithLifecycle()
    val conflict by coordinator.conflict.collectAsStateWithLifecycle()
    val prefs: SessionPreferences? by media.preferences.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    var floatingPlayerHeight by remember { mutableIntStateOf(0) }

    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val selected = TopLevelDestination.entries.find { tab ->
        entry?.destination?.hierarchy?.any { it.route == tab.route } == true
    }
    val contextTitle = entry?.destination.contextTitle()
    fun select(section: Section) {
        val destination = TopLevelDestination.entries.first { it.section == section }
        // Account/settings are auxiliary screens, not part of Home's saved tab stack.
        if (nav.currentDestination?.route == AccountNavigation.ACCOUNT ||
            nav.currentDestination?.route == AccountNavigation.SETTINGS) {
            nav.popBackStack(HomeNavigation.FEED, inclusive = false)
        }
        coordinator.enter(section)
        nav.navigate(destination.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    // First-run gate: act only on a REAL preference emission, and never when the
    // flow already sits inside onboarding (e.g. right after a logout → Login).
    LaunchedEffect(prefs?.onboarded) {
        if (prefs?.onboarded == false) {
            val alreadyInside = nav.currentBackStackEntry?.destination?.hierarchy
                ?.any { it.route == OnboardingNavigation.GRAPH } == true
            if (!alreadyInside) nav.navigate(OnboardingNavigation.GRAPH) { launchSingleTop = true }
        }
    }

    // TC10 restore chain: jump back to the last active tab after process death.
    LaunchedEffect(coordinator, prefs?.onboarded) {
        if (prefs?.onboarded == true) {
            coordinator.restoredSection.collect { restored ->
                if (restored != null) {
                    select(restored)
                    coordinator.consumeRestoredSection()
                }
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
    val inOnboarding = entry?.destination?.hierarchy
        ?.any { it.route == OnboardingNavigation.GRAPH } == true
    val videoId = session.videoId
    val trackId = musicState.trackId ?: session.trackId
    val showFloat = !inOnboarding && videoId != null && contextTitle != "Video" && selected?.section != Section.MUSIC
    val showMini = !inOnboarding && trackId != null && contextTitle != "Music" && contextTitle != "Games"

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState, Modifier.padding(
                bottom = if (showFloat) with(density) { floatingPlayerHeight.toDp() } else 0.dp
            ))
        },
        topBar = {
            if (!inOnboarding) TopAppBar(
                title = { Text(contextTitle ?: selected?.label ?: "") },
                navigationIcon = {
                    if (contextTitle != null) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    // Profile is secondary: reachable from Home only, not every screen.
                    if (onHome) IconButton(onClick = {
                        coordinator.cancelPendingRestore()
                        nav.navigate(AccountNavigation.ACCOUNT) { launchSingleTop = true }
                    }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Tài khoản")
                    }
                }
            )
        },
        bottomBar = {
            if (!inOnboarding) Column {
                // Hierarchy: content > mini player > bottom navigation.
                if (showMini) MusicMiniPlayer(
                    title = musicState.title.ifEmpty { session.trackTitle.orEmpty() },
                    playing = musicState.playing,
                    onExpand = { trackId?.let { nav.navigate(MusicNavigation.nowPlaying(it)) { launchSingleTop = true } } },
                    onToggle = coordinator::toggleMusic,
                    onClose = {
                        // Undo pattern: the user can bring the music back within 4 seconds.
                        val lastId = trackId
                        val lastTitle = musicState.title
                        coordinator.closeMusic()
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                "Đã dừng phát nhạc", actionLabel = "Hoàn tác")
                            if (result == SnackbarResult.ActionPerformed && lastId != null) {
                                coordinator.playMusic(lastId, lastTitle.ifBlank { lastId })
                            }
                        }
                    }
                )
                NavigationBar {
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
            AppNavigation(nav, coordinator, ::select,
                Modifier.fillMaxSize().padding(bottom = if (showFloat) 140.dp else 0.dp))
            if (showFloat) FloatingVideo(
                player = media.videoPlayer,
                title = videoState.title.ifEmpty { session.videoTitle.orEmpty() },
                playing = videoState.playing,
                muted = videoState.muted,
                onHeightChanged = { floatingPlayerHeight = it },
                onExpand = { videoId?.let { nav.navigate(VideoNavigation.detail(it)) { launchSingleTop = true } } },
                onToggle = coordinator::toggleVideo,
                onClose = {
                    coordinator.closeVideo()
                    scope.launch { snackbarHostState.showSnackbar("Đã lưu vào Xem sau") }
                }
            )
        }
    }

    // Case C1: the first time music collides with a playing video, ask once and remember.
    conflict?.let { request ->
        AlertDialog(
            onDismissRequest = coordinator::dismissMusicConflict,
            title = { Text(videoState.title.ifEmpty { "Video đang phát" }) },
            text = { Text("Bật \"" + request.track.title + "\" trong khi video vẫn chạy?") },
            confirmButton = {
                Column {
                    ConflictOption("Tắt tiếng video, phát nhạc", MediaConflictRule.MUTE_VIDEO) {
                        coordinator.resolveMusicConflict(it)
                    }
                    ConflictOption("Dừng video, phát nhạc", MediaConflictRule.PAUSE_VIDEO) {
                        coordinator.resolveMusicConflict(it)
                    }
                    ConflictOption("Chỉ giữ video", MediaConflictRule.KEEP_VIDEO) {
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
