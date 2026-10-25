package com.example.onespace.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * F-VID-15: ONE playlist hub for the whole app — videos and tracks live in
 * the same playlists. The feeds never render playlist lists; the profile
 * screen owns the single entry point.
 */
data class AppPlaylist(
    val name: String,
    val videoIds: List<String> = emptyList(),
    val trackIds: List<String> = emptyList()
)

object AppPlaylistStore {
    var playlists by mutableStateOf<Map<String, AppPlaylist>>(emptyMap())
        private set

    fun create(name: String, videoIds: List<String> = emptyList(), trackIds: List<String> = emptyList()) {
        if (name.isBlank()) return
        playlists = playlists + (name to AppPlaylist(name, videoIds, trackIds))
    }

    fun addVideo(name: String, videoId: String) {
        val current = playlists[name] ?: AppPlaylist(name)
        playlists = playlists + (name to current.copy(videoIds = (current.videoIds + videoId).distinct()))
    }

    fun addTrack(name: String, trackId: String) {
        val current = playlists[name] ?: AppPlaylist(name)
        playlists = playlists + (name to current.copy(trackIds = (current.trackIds + trackId).distinct()))
    }

    fun remove(name: String) { playlists = playlists - name }

    /** How a playlist plays: order is sequential|shuffle, repeat is on/off. */
    data class PlaySettings(
        val order: String = "sequential",
        val repeat: Boolean = false
    )

    var playSettings by mutableStateOf<Map<String, PlaySettings>>(emptyMap())
        private set

    fun settingsOf(name: String): PlaySettings = playSettings[name] ?: PlaySettings()

    fun setOrder(name: String, order: String) {
        val current = settingsOf(name)
        playSettings = playSettings + (name to current.copy(order = order))
    }

    fun toggleOrder(name: String) {
        val current = settingsOf(name)
        setOrder(name, if (current.order == "shuffle") "sequential" else "shuffle")
    }

    fun toggleRepeat(name: String) {
        val current = settingsOf(name)
        playSettings = playSettings + (name to current.copy(repeat = !current.repeat))
    }
}
