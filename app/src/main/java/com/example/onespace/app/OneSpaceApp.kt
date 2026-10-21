package com.example.onespace.app

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.example.onespace.app.navigation.*
import com.example.onespace.app.ui.*
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.account.navigation.AccountNavigation
import com.example.onespace.feature.music.navigation.MusicNavigation
import com.example.onespace.feature.video.navigation.VideoNavigation
import com.example.onespace.feature.games.navigation.GamesNavigation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OneSpaceApp() {
    // One coordinator for the entire shell; catalog ViewModels live inside feature graphs.
    val coordinator: SessionCoordinator = hiltViewModel()
    val session by coordinator.uiState.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val selected = TopLevelDestination.entries.find { tab ->
        entry?.destination?.hierarchy?.any { it.route == tab.route } == true
    }
    fun select(section: Section) {
        val destination = TopLevelDestination.entries.first { it.section == section }
        coordinator.enter(section)
        nav.navigate(destination.route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    LaunchedEffect(entry) { coordinator.enter(selected?.section ?: Section.HOME) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, coordinator) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) coordinator.background()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val detail = route != null && route !in TopLevelDestination.entries.map { it.feedRoute }
    val videoId = session.videoId
    val trackId = session.trackId
    val showFloat = videoId != null && route != VideoNavigation.DETAIL && selected?.section != Section.MUSIC
    val showMini = trackId != null && route != MusicNavigation.NOW_PLAYING &&
        route != GamesNavigation.GAME

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("oneSpace", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    if (detail) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { nav.navigate(AccountNavigation.ACCOUNT) { launchSingleTop = true } }) {
                        Icon(Icons.Filled.AccountCircle, contentDescription = "Account")
                    }
                }
            )
        },
        bottomBar = {
            Column {
                if (showMini) MusicMiniPlayer(
                    title = session.trackTitle.orEmpty(), playing = session.musicPlaying,
                    onExpand = { nav.navigate(MusicNavigation.nowPlaying(trackId)) { launchSingleTop = true } },
                    onToggle = coordinator::toggleMusic, onClose = coordinator::closeMusic
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
                title = session.videoTitle.orEmpty(), playing = session.videoPlaying,
                onExpand = { nav.navigate(VideoNavigation.detail(videoId)) { launchSingleTop = true } },
                onToggle = coordinator::toggleVideo, onClose = coordinator::closeVideo
            )
        }
    }
}
