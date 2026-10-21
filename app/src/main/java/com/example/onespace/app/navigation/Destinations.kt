package com.example.onespace.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.onespace.domain.session.Section
import com.example.onespace.feature.home.navigation.HomeNavigation
import com.example.onespace.feature.video.navigation.VideoNavigation
import com.example.onespace.feature.music.navigation.MusicNavigation
import com.example.onespace.feature.news.navigation.NewsNavigation
import com.example.onespace.feature.games.navigation.GamesNavigation

enum class TopLevelDestination(
    val section: Section, val route: String, val feedRoute: String,
    val label: String, val icon: ImageVector
) {
    HOME(Section.HOME, HomeNavigation.GRAPH, HomeNavigation.FEED, "Home", Icons.Filled.Home),
    VIDEO(Section.VIDEO, VideoNavigation.GRAPH, VideoNavigation.FEED, "Video", Icons.Filled.SmartDisplay),
    MUSIC(Section.MUSIC, MusicNavigation.GRAPH, MusicNavigation.FEED, "Music", Icons.Filled.MusicNote),
    NEWS(Section.NEWS, NewsNavigation.GRAPH, NewsNavigation.FEED, "News", Icons.Filled.Newspaper),
    GAMES(Section.GAMES, GamesNavigation.GRAPH, GamesNavigation.FEED, "Games", Icons.Filled.SportsEsports)
}
