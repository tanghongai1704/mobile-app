package com.example.onespace.core.media

/** Integration boundary for the future Media3 adapter. No player is created by a screen. */
interface PlaybackController {
    fun play(contentId: String)
    fun pause()
    fun stop()
}
