package com.example.onespace.domain.session.port

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.MediaConflictRule
import com.example.onespace.domain.session.VideoAudioEffect
import com.example.onespace.domain.video.model.Video
import kotlinx.coroutines.flow.StateFlow

data class MusicPlayback(
    val trackId: String? = null,
    val title: String = "",
    val artist: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val totalMs: Long = 0
)

data class VideoPlayback(
    val videoId: String? = null,
    val title: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val totalMs: Long = 0,
    val muted: Boolean = false
)

/** First-time collision of video + music; the shell turns it into the case C1 popup. */
data class MusicConflictRequest(val track: Track, val queue: List<Track>)

/**
 * Domain-facing contract of the shared media engine (PlaybackCoordinator in the
 * report's coordinator table). Implemented in core:media with a single shared
 * player pair for music and video, so no screen ever creates its own player.
 */
interface MediaSessionPort {
    val music: StateFlow<MusicPlayback>
    val video: StateFlow<VideoPlayback>

    /** Start music, telling the engine how to treat a playing video per [effect]. */
    fun requestMusic(track: Track, queue: List<Track>, effect: VideoAudioEffect)
    fun resolveMusicConflict(track: Track, queue: List<Track>, rule: MediaConflictRule)
    /** Rehydrate the queue and last track after a process death, without autoplay. */
    fun prepareRestoredMusic(track: Track, queue: List<Track>)
    fun toggleMusic()
    fun stopMusic()
    fun seekMusicTo(positionMs: Long)
    fun skipToNext()
    fun skipToPrevious()

    fun openVideo(video: Video, startPositionMs: Long)
    fun toggleVideo()
    fun seekVideoBy(deltaMs: Long)
    fun pauseVideo()
    /** Stops the video; progress is persisted, [toWatchlist] adds it to Watchlist (case C3). */
    fun dismissVideo(toWatchlist: Boolean)
}
