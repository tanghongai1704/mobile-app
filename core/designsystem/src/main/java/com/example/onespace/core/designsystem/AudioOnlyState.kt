package com.example.onespace.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * F-VID-09: sound-only flag shared with the shell so the floating window
 * can render as a music-style mini player instead of the video surface.
 */
object AudioOnlyState {
    var active by mutableStateOf(false)
        private set

    fun set(value: Boolean) { active = value }
}

/**
 * F-SPLIT: shared flag so the floating window's split button can tell the shell
 * to dock the video at the top of the content column, pushing content down.
 */
object SplitVideoState {
    var active by mutableStateOf(false)
        private set

    fun set(value: Boolean) { active = value }
}

/**
 * True only while a Beat Tiles round runs on a song picked for the game
 * (from the game's picker or sent over from Now Playing). That song is the
 * round's backing track, so the dock shows it read-only — no pause, no close.
 * Music the user was already listening to before entering a game keeps the
 * full dock.
 */
object GameMusicState {
    var active by mutableStateOf(false)
        private set

    fun set(value: Boolean) { active = value }
}
