package com.example.onespace.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.onespace.core.designsystem.tr
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
    val section: Section, val route: Any, private val labelVi: String, private val labelEn: String, val icon: ImageVector
) {
    HOME(Section.HOME, HomeRoute.Graph, "Trang chủ", "Home", Icons.Filled.Home),
    VIDEO(Section.VIDEO, VideoRoute.Graph, "Video", "Video", Icons.Filled.SmartDisplay),
    MUSIC(Section.MUSIC, MusicRoute.Graph, "Nhạc", "Music", Icons.Filled.MusicNote),
    NEWS(Section.NEWS, NewsRoute.Graph, "Tin tức", "News", Icons.Filled.Newspaper),
    GAMES(Section.GAMES, GamesRoute.Graph, "Trò chơi", "Games", Icons.Filled.SportsEsports);

    @Composable
    @ReadOnlyComposable
    fun label(): String = tr(labelVi, labelEn)

    fun contains(destination: NavDestination?): Boolean =
        destination?.hierarchy?.any { it.hasRoute(route::class) } == true
}

/** Which kind of secondary screen is on top; logic keys off this, never off the (translated) title. */
enum class ContextScreen(private val titleVi: String, private val titleEn: String) {
    VIDEO("Video", "Video"),
    MUSIC("Nhạc", "Music"),
    NEWS("Tin tức", "News"),
    GAMES("Trò chơi", "Games"),
    ACCOUNT("Tài khoản", "Account"),
    SETTINGS("Tùy chỉnh", "Settings"),
    PLAYLISTS("Danh sách phát", "Playlists"),
    PROFILE("Hồ sơ", "Profile");

    @Composable
    @ReadOnlyComposable
    fun title(): String = tr(titleVi, titleEn)
}

internal fun NavDestination?.contextScreen(): ContextScreen? = when {
    this == null -> null
    hasRoute<VideoRoute.Detail>() -> ContextScreen.VIDEO
    hasRoute<VideoRoute.Artist>() -> ContextScreen.VIDEO
    hasRoute<VideoRoute.Playlist>() -> ContextScreen.VIDEO
    hasRoute<MusicRoute.NowPlaying>() -> ContextScreen.MUSIC
    hasRoute<MusicRoute.Playlist>() -> ContextScreen.MUSIC
    hasRoute<MusicRoute.Artist>() -> ContextScreen.MUSIC
    hasRoute<NewsRoute.Article>() -> ContextScreen.NEWS
    hasRoute<GamesRoute.Game>() -> ContextScreen.GAMES
    hasRoute<AccountRoute.Profile>() -> ContextScreen.ACCOUNT
    hasRoute<AccountRoute.Settings>() -> ContextScreen.SETTINGS
    hasRoute<AccountRoute.Wrapped>() -> ContextScreen.PROFILE
    hasRoute<AccountRoute.Notifications>() -> ContextScreen.PROFILE
    hasRoute<AccountRoute.Playlists>() -> ContextScreen.PLAYLISTS
    hasRoute<AccountRoute.Downloads>() -> ContextScreen.PROFILE
    hasRoute<AccountRoute.Badges>() -> ContextScreen.PROFILE
    else -> null
}

/**
 * The playlist's name when the current screen is a playlist detail, so the
 * top bar can say "Danh sách phát: Mobile" instead of the section label.
 */
internal fun androidx.navigation.NavBackStackEntry?.playlistName(): String? {
    val destination = this?.destination ?: return null
    val isPlaylist = destination.hasRoute<VideoRoute.Playlist>() ||
        destination.hasRoute<MusicRoute.Playlist>()
    if (!isPlaylist) return null
    return arguments?.getString("name")
}
