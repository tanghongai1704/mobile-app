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

    fun openVideo(id: String) {
        navController.navigate(VideoRoute.Detail(id)) { launchSingleTop = true }
    }

    fun openMusicPlayer(id: String) {
        navController.navigate(MusicRoute.NowPlaying(id)) {
            launchSingleTop = true
        }
    }

    fun openArticle(id: String) {
        navController.navigate(NewsRoute.Article(id)) { launchSingleTop = true }
    }

    fun openGame(id: String) {
        navController.navigate(GamesRoute.Game(id)) { launchSingleTop = true }
    }

    fun verifyEmail(email: String) {
        navController.navigate(AuthRoute.VerifyCode(email)) { launchSingleTop = true }
    }

    fun navigateToSection(section: Section) {
        val destination = TopLevelDestination.entries.first { it.section == section }
        val current = navController.currentDestination
        if (current?.hasRoute<AccountRoute.Profile>() == true ||
            current?.hasRoute<AccountRoute.Settings>() == true) {
            navController.popBackStack<HomeRoute.Feed>(inclusive = false)
        }
        navController.navigate(destination.route) {
            popUpTo<HomeRoute.Feed> { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun rememberOneSpaceAppState(navController: NavHostController): OneSpaceAppState =
    remember(navController) { OneSpaceAppState(navController) }
