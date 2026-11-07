package com.example.onespace.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import com.example.onespace.feature.auth.navigation.AuthRoute
import com.example.onespace.feature.video.navigation.VideoRoute
import com.example.onespace.feature.music.navigation.MusicRoute
import com.example.onespace.feature.news.navigation.NewsRoute
import com.example.onespace.feature.games.navigation.GamesRoute
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.account.navigation.AccountRoute
import com.example.onespace.feature.home.navigation.HomeRoute

/** Shell navigation policy only. Feature state and business rules stay outside the app. */
class OneSpaceAppState(val navController: NavHostController) {
    /** Supplies a graph owner without exposing the controller to a feature. */
    fun graphEntry(route: Any): NavBackStackEntry = navController.getBackStackEntry(route)

    fun navigateBack() { navController.popBackStack() }

    fun openProfile() {
        navController.navigate(AccountRoute.Profile) { launchSingleTop = true }
    }

    fun openSettings() {
        navController.navigate(AccountRoute.Settings) { launchSingleTop = true }
    }

    fun openWrapped() {
        navController.navigate(AccountRoute.Wrapped) { launchSingleTop = true }
    }

    fun openNotifications() {
        navController.navigate(AccountRoute.Notifications) { launchSingleTop = true }
    }

    fun openPlaylists() {
        navController.navigate(AccountRoute.Playlists) { launchSingleTop = true }
    }

    fun openBadges() {
        navController.navigate(AccountRoute.Badges) { launchSingleTop = true }
    }

    fun openDownloads() {
        navController.navigate(AccountRoute.Downloads) { launchSingleTop = true }
    }

    /**
     * Content always opens on top of ITS OWN tab's feed, never on the
     * caller's stack: back from a detail lands on that section's feed,
     * then Home, then exit — one predictable rule everywhere.
     */
    private fun openInTab(tab: TopLevelDestination, route: Any) {
        navController.navigate(tab.route) {
            popUpTo<HomeRoute.Feed> { saveState = true }
            launchSingleTop = true
        }
        navController.navigate(route) { launchSingleTop = true }
    }

    /**
     * In-tab exploration pushes linearly (feed → detail → artist…, back
     * walks it one by one); cross-context opens land on the section's own
     * feed first. Same user-facing rule: back descends one level.
     */
    private fun openSmart(tab: TopLevelDestination, graph: Any, route: Any) {
        val inTab = navController.currentDestination?.hierarchy
            ?.any { it.hasRoute(graph::class) } == true
        if (inTab) {
            navController.navigate(route) { launchSingleTop = true }
        } else {
            openInTab(tab, route)
        }
    }

    fun openVideo(id: String) {
        openSmart(TopLevelDestination.VIDEO, VideoRoute.Graph, VideoRoute.Detail(id))
    }


    fun openVideoInPlaylist(id: String, playlist: String) {
        openSmart(TopLevelDestination.VIDEO, VideoRoute.Graph, VideoRoute.Detail(id, playlist))
    }

    fun openVideoArtist(creator: String) {
        openSmart(TopLevelDestination.VIDEO, VideoRoute.Graph, VideoRoute.Artist(creator))
    }

    /**
     * A playlist opens on top of the playlist hub (Profile → Playlists), so
     * back from the playlist returns to the playlist LIST, never to the
     * Video or Music tab feed.
     */
    fun openVideoPlaylist(name: String) {
        navController.navigate(VideoRoute.Graph) { launchSingleTop = true }
        navController.navigate(VideoRoute.Playlist(name)) { launchSingleTop = true }
    }

    fun openMusicPlaylist(name: String) {
        navController.navigate(MusicRoute.Graph) { launchSingleTop = true }
        navController.navigate(MusicRoute.Playlist(name)) { launchSingleTop = true }
    }

    /**
     * A mixed playlist session moving to a video replaces the current player
     * screen (music → video), so back still lands on the playlist list.
     */
    fun openPlaylistVideo(id: String, playlist: String) {
        replaceTop(VideoRoute.Detail(id, playlist))
    }

    /** A mixed playlist session moving to a track replaces the current player screen (video → music). */
    fun openPlaylistTrack(id: String) {
        replaceTop(MusicRoute.NowPlaying(id))
    }

    /**
     * A track tapped in a video playlist pushes the music player on top of
     * the current stack; back returns to the playlist list.
     */
    fun openPlaylistTrackPlayer(id: String) {
        navController.navigate(MusicRoute.NowPlaying(id)) { launchSingleTop = true }
    }

    /** Swaps the top screen for [route]; used by playlist session transitions. */
    private fun replaceTop(route: Any) {
        val current = navController.currentDestination ?: return
        navController.navigate(route) {
            popUpTo(current.id) { inclusive = true }
            launchSingleTop = true
        }
    }

    /** Back from a playlist goes to the playlist list; false when the hub is not below. */
    fun backToPlaylists(): Boolean =
        navController.popBackStack(AccountRoute.Playlists, /* inclusive = */ false)

    fun openMusicPlayer(id: String) {
        openSmart(TopLevelDestination.MUSIC, MusicRoute.Graph, MusicRoute.NowPlaying(id))
    }

    fun openMusicArtist(artist: String) {
        openInTab(TopLevelDestination.MUSIC, MusicRoute.Artist(artist))
    }

    fun openArticle(id: String) {
        openSmart(TopLevelDestination.NEWS, NewsRoute.Graph, NewsRoute.Article(id))
    }

    fun openGame(id: String, track: String? = null) {
        openSmart(TopLevelDestination.GAMES, GamesRoute.Graph, GamesRoute.Game(id, track))
    }


    fun verifyEmail(email: String) {
        navController.navigate(AuthRoute.VerifyCode(email)) { launchSingleTop = true }
    }

    fun navigateToSection(section: Section) {
        val destination = TopLevelDestination.entries.first { it.section == section }
        navController.navigate(destination.route) {
            popUpTo<HomeRoute.Feed> { saveState = true }
            launchSingleTop = true
            // Home always lands on its feed root: account detours pushed on
            // top of it must never come back through a saved tab state.
            restoreState = section != Section.HOME
        }
    }
}

@Composable
fun rememberOneSpaceAppState(navController: NavHostController): OneSpaceAppState =
    remember(navController) { OneSpaceAppState(navController) }
