package com.example.onespace.domain.session

enum class Section { HOME, VIDEO, MUSIC, NEWS, GAMES }

data class SessionState(
    val videoId: String? = null,
    val videoTitle: String? = null,
    val trackId: String? = null,
    val trackTitle: String? = null,
    val gamePaused: Boolean = true
)
