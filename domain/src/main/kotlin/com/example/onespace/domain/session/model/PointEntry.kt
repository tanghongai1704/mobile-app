package com.example.onespace.domain.session.model

/** One earned-point record. [source] describes the action ("video_watch", "game_win", ...). */
data class PointEntry(
    val id: Long = 0,
    val source: String,
    val amount: Int,
    val timestamp: Long
)
