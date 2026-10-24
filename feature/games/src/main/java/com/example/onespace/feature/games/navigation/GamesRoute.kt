package com.example.onespace.feature.games.navigation

import kotlinx.serialization.Serializable

/** Public navigation contract; arguments carry IDs only. */
sealed interface GamesRoute {
    @Serializable data object Graph : GamesRoute
    @Serializable data object Feed : GamesRoute
    @Serializable data class Game(val id: String) : GamesRoute
}
