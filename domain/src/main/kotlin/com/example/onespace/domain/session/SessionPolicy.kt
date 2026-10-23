package com.example.onespace.domain.session

/** Product rules only. No lifecycle, player, storage or navigation dependencies. */

/** Rule remembered for the video+music pair when two audible sources collide (case C1). */
enum class MediaConflictRule { MUTE_VIDEO, PAUSE_VIDEO, KEEP_VIDEO }

/** How the already-playing video must change when music starts. */
enum class VideoAudioEffect { NONE, MUTE, PAUSE }

sealed interface MusicStartPlan {
    data class Start(val videoEffect: VideoAudioEffect) : MusicStartPlan
    /** Music must not start; the video keeps its audio. */
    data object DoNotStart : MusicStartPlan
    /** No remembered rule: the shell must ask the user once (C1 popup). */
    data object AskUser : MusicStartPlan
}

object SessionPolicy {
    fun enter(state: SessionState, destination: Section) = state.copy(
        videoPlaying = state.videoPlaying && destination != Section.MUSIC,
        gamePaused = state.gamePaused || destination != Section.GAMES
    )
    fun playVideo(state: SessionState, id: String, title: String) =
        state.copy(videoId = id, videoTitle = title, videoPlaying = true, musicPlaying = false)
    fun playMusic(state: SessionState, id: String, title: String) =
        state.copy(trackId = id, trackTitle = title, musicPlaying = true, videoPlaying = false)
    fun toggleVideo(state: SessionState) = if (state.videoId == null) state else
        state.copy(videoPlaying = !state.videoPlaying, musicPlaying = false)
    fun toggleMusic(state: SessionState) = if (state.trackId == null) state else
        state.copy(musicPlaying = !state.musicPlaying, videoPlaying = false)
    fun closeVideo(state: SessionState) = state.copy(videoId = null, videoTitle = null, videoPlaying = false)
    fun closeMusic(state: SessionState) = state.copy(trackId = null, trackTitle = null, musicPlaying = false)
    fun background(state: SessionState) = state.copy(videoPlaying = false, gamePaused = true)

    /**
     * Case C1: the user starts music while the video is playing. The first time the
     * pair collides there is no remembered rule and the shell must ask; afterwards
     * the stored rule is applied silently.
     */
    fun resolveMusicStart(videoActive: Boolean, rememberedRule: MediaConflictRule?): MusicStartPlan = when {
        !videoActive -> MusicStartPlan.Start(VideoAudioEffect.NONE)
        rememberedRule == MediaConflictRule.MUTE_VIDEO  -> MusicStartPlan.Start(VideoAudioEffect.MUTE)
        rememberedRule == MediaConflictRule.PAUSE_VIDEO -> MusicStartPlan.Start(VideoAudioEffect.PAUSE)
        rememberedRule == MediaConflictRule.KEEP_VIDEO  -> MusicStartPlan.DoNotStart
        else -> MusicStartPlan.AskUser
    }
}
