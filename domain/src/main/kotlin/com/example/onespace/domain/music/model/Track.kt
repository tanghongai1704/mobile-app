package com.example.onespace.domain.music.model

data class Track(val id: String, val title: String, val artist: String, val duration: String, val url: String, val downloaded: Boolean = false,
    /** Plain lyric lines; empty when the song has no lyrics yet. */
    val lyrics: List<String> = emptyList(),
    /** Tempo used to time the Beat Tiles game; null when the song has no beat data (game disabled). */
    val bpm: Int? = null)
