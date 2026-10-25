package com.example.onespace.feature.games.navigation

import kotlinx.serialization.Serializable

sealed interface GamesRoute {
    @Serializable data object Graph : GamesRoute
    @Serializable data object Feed : GamesRoute
    @Serializable data class Game(val id: String) : GamesRoute
    @Serializable data object GuessSong : GamesRoute
}
