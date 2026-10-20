package com.example.onespace.app.navigation

import com.example.onespace.domain.session.Section
import com.example.onespace.feature.home.navigation.HomeNavigation
import com.example.onespace.feature.video.navigation.VideoNavigation
import com.example.onespace.feature.music.navigation.MusicNavigation
import com.example.onespace.feature.news.navigation.NewsNavigation
import com.example.onespace.feature.games.navigation.GamesNavigation

enum class TopLevelDestination(
    val section: Section, val route: String, val feedRoute: String, val label: String, val glyph: String
) {
    HOME(Section.HOME, HomeNavigation.GRAPH, HomeNavigation.FEED, "Home", "⌂"),
    VIDEO(Section.VIDEO, VideoNavigation.GRAPH, VideoNavigation.FEED, "Video", "▶"),
    MUSIC(Section.MUSIC, MusicNavigation.GRAPH, MusicNavigation.FEED, "Music", "♫"),
    NEWS(Section.NEWS, NewsNavigation.GRAPH, NewsNavigation.FEED, "News", "≡"),
    GAMES(Section.GAMES, GamesNavigation.GRAPH, GamesNavigation.FEED, "Games", "⊞")
}
