package com.example.onespace.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.account.navigation.AccountRoute
import com.example.onespace.feature.home.navigation.HomeRoute
import com.example.onespace.feature.video.navigation.VideoRoute
import com.example.onespace.feature.music.navigation.MusicRoute
import com.example.onespace.feature.news.navigation.NewsRoute
import com.example.onespace.feature.games.navigation.GamesRoute

enum class TopLevelDestination(
    val section: Section, val route: Any, val label: String, val icon: ImageVector
) {
    HOME(Section.HOME, HomeRoute.Graph, "Home", Icons.Filled.Home),
    VIDEO(Section.VIDEO, VideoRoute.Graph, "Video", Icons.Filled.SmartDisplay),
    MUSIC(Section.MUSIC, MusicRoute.Graph, "Music", Icons.Filled.MusicNote),
    NEWS(Section.NEWS, NewsRoute.Graph, "News", Icons.Filled.Newspaper),
    GAMES(Section.GAMES, GamesRoute.Graph, "Games", Icons.Filled.SportsEsports);

    fun contains(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any { it.hasRoute(route::class) } == true
}

internal fun NavDestination?.contextTitle(): String? = when {
    this == null -> null
    hasRoute<VideoRoute.Detail>() -> "Video"
    hasRoute<VideoRoute.Artist>() -> "Video"
    hasRoute<VideoRoute.Playlist>() -> "Video"
    hasRoute<MusicRoute.NowPlaying>() -> "Music"
    hasRoute<MusicRoute.Playlist>() -> "Music"
    hasRoute<NewsRoute.Article>() -> "News"
    hasRoute<GamesRoute.Game>() -> "Games"
    hasRoute<AccountRoute.Profile>() -> "Tài khoản"
    hasRoute<AccountRoute.Settings>() -> "Tùy chỉnh"
    hasRoute<AccountRoute.Wrapped>() -> "Hồ sơ"
    hasRoute<AccountRoute.Notifications>() -> "Hồ sơ"
    hasRoute<AccountRoute.Playlists>() -> "Hồ sơ"
    else -> null
}
