package com.example.onespace.feature.music.navigation

import kotlinx.serialization.Serializable

sealed interface MusicRoute {
    @Serializable data object Graph : MusicRoute
    @Serializable data object Feed : MusicRoute
    @Serializable data class NowPlaying(val id: String) : MusicRoute
    @Serializable data class Playlist(val name: String) : MusicRoute
    @Serializable data class Artist(val artist: String) : MusicRoute
}
