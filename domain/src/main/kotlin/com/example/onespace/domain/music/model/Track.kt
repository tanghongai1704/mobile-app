package com.example.onespace.domain.music.model

data class Track(val id: String, val title: String, val artist: String, val duration: String, val downloaded: Boolean = false)
