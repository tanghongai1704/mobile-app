package com.example.onespace.domain.video.model

/** Catalog video. [topic] matches the onboarding topic names so the feed can
 * put videos the user asked for first (F-VID-02 "Dành cho bạn"). */
data class Video(
    val id: String,
    val title: String,
    val creator: String,
    val duration: String,
    val url: String,
    val topic: String = ""
)
