package com.example.onespace.feature.music.navigation

import kotlinx.serialization.Serializable

/** Public navigation contract; arguments carry IDs only. */
sealed interface MusicRoute {
    @Serializable data object Graph : MusicRoute
    @Serializable data object Feed : MusicRoute
    @Serializable data class NowPlaying(val id: String) : MusicRoute
}
