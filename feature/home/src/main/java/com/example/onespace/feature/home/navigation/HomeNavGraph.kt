package com.example.onespace.feature.home.navigation

import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.example.onespace.feature.home.presentation.HomeScreen
import com.example.onespace.feature.home.presentation.HomeViewModel

fun NavGraphBuilder.homeGraph(
    onOpenVideo: (id: String, title: String) -> Unit,
    onPlayTrack: (id: String, title: String) -> Unit,
    onOpenArticle: (id: String) -> Unit,
    onMissionClick: (String) -> Unit = {},
    onOpenPlaylists: () -> Unit = {},
    onPlayVideoAtHome: (id: String, title: String) -> Unit = { _, _ -> },
    onPlayMusicAtHome: (id: String, title: String) -> Unit = { _, _ -> },
    videoPlayer: androidx.media3.common.Player? = null,
    onToggleVideo: () -> Unit = {},
    onToggleMusic: () -> Unit = {},
    onSeekVideo: (positionMs: Long) -> Unit = {},
    onSeekMusic: (positionMs: Long) -> Unit = {},
    // Stable reference only: NavGraph content captures parameters at build
    // time, so changing state must be collected inside the composable.
    session: com.example.onespace.core.session.SessionCoordinator? = null
) {
    navigation<HomeRoute.Graph>(startDestination = HomeRoute.Feed) {
        composable<HomeRoute.Feed> {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            val videoPlayback = session?.video?.collectAsStateWithLifecycle()?.value
            val musicPlayback = session?.music?.collectAsStateWithLifecycle()?.value
            val gamification by vm.gamification.collectAsStateWithLifecycle()
            val offline by vm.offline.collectAsStateWithLifecycle()
            val watchLater by vm.watchLater.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                gamification = gamification,
                offline = offline,
                watchLater = watchLater,
                onRemoveFromWatchLater = { videoId -> vm.removeFromWatchLater(videoId) },
                onWatchedFromLater = { videoId -> vm.watchedFromLater(videoId) },
                onOpenVideo = onOpenVideo,
                onPlayTrack = onPlayTrack,
                onOpenArticle = onOpenArticle,
                onMissionClick = onMissionClick,
                onOpenPlaylists = onOpenPlaylists,
                onPlayVideoAtHome = onPlayVideoAtHome,
                onPlayMusicAtHome = onPlayMusicAtHome,
                videoPlayer = videoPlayer,
                videoPlayback = videoPlayback,
                musicPlayback = musicPlayback,
                onToggleVideo = onToggleVideo,
                onToggleMusic = onToggleMusic,
                onSeekVideo = onSeekVideo,
                onSeekMusic = onSeekMusic
            )
        }
    }
}
