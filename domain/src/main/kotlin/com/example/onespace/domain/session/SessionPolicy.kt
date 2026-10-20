package com.example.onespace.domain.session

/** Product rules only. No lifecycle, player, storage or navigation dependencies. */
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
}
