package com.example.onespace.feature.games.navigation

import kotlinx.serialization.Serializable

sealed interface GamesRoute {
    @Serializable data object Graph : GamesRoute
    @Serializable data object Feed : GamesRoute
    /** [track] is a song id: Beat Tiles then plays with that song. */
    @Serializable data class Game(val id: String, val track: String? = null) : GamesRoute
    @Serializable data object GuessSong : GamesRoute
}
