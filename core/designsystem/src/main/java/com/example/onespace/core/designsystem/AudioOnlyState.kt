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
