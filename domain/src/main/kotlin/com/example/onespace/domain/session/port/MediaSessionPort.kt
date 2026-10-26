package com.example.onespace.domain.session.port

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.VideoAudioEffect
import com.example.onespace.domain.session.VideoStartPlan
import com.example.onespace.domain.video.model.Video
import kotlinx.coroutines.flow.StateFlow

/** Framework-independent state; requested playback is distinct from actual output. */
enum class PlaybackStatus { IDLE, BUFFERING, READY, ENDED, ERROR }

data class MusicPlayback(
    val trackId: String? = null,
    val title: String = "",
    val artist: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val totalMs: Long = 0,
    val playRequested: Boolean = playing,
    val status: PlaybackStatus = if (playing) PlaybackStatus.READY else PlaybackStatus.IDLE,
    val suppressed: Boolean = false,
    val error: String? = null,
    val queue: List<Track> = emptyList()
)

data class VideoPlayback(
    val videoId: String? = null,
    val title: String = "",
    val playing: Boolean = false,
    val positionMs: Long = 0,
    val totalMs: Long = 0,
    val muted: Boolean = false,
    val playRequested: Boolean = playing,
    val status: PlaybackStatus = if (playing) PlaybackStatus.READY else PlaybackStatus.IDLE,
    val suppressed: Boolean = false,
    val error: String? = null
) {
    // Buffering video still owns its audio intent; muted/ended/error video does not.
    val audible: Boolean get() = videoId != null && playRequested && !muted &&
        status != PlaybackStatus.ENDED && status != PlaybackStatus.ERROR
}

/** A user request awaiting the C1 decision; it is not a second active queue. */
data class MusicConflictRequest(val track: Track, val queue: List<Track>)

/** A user request awaiting the C2 decision: open video while music is playing. */
data class VideoConflictRequest(val videoId: String, val title: String, val resumeFrom: Long)

/** Adapters execute explicit effects; policy and navigation stay outside the engine. */
interface MediaSessionPort {
    val music: StateFlow<MusicPlayback>
    val video: StateFlow<VideoPlayback>

    fun requestMusic(track: Track, queue: List<Track>, effect: VideoAudioEffect)
    fun prepareRestoredMusic(track: Track, queue: List<Track>)
    fun pauseMusic()
    fun stopMusic()
    fun seekMusicTo(positionMs: Long)
    fun removeTrackFromQueue(trackId: String)
    fun moveTrackInQueue(fromIndex: Int, toIndex: Int)

    fun openVideo(video: Video, startPositionMs: Long, plan: com.example.onespace.domain.session.VideoStartPlan.Play)
    fun resumeVideo(plan: com.example.onespace.domain.session.VideoStartPlan.Play)
    fun seekVideoBy(deltaMs: Long)
    fun seekVideoTo(positionMs: Long)
    fun pauseVideo()
    fun dismissVideo(toWatchlist: Boolean)

    /** User's video volume, 0 (mute) to 1 (full); session state, never persisted. */
    fun setVideoVolume(volume: Float)
}
