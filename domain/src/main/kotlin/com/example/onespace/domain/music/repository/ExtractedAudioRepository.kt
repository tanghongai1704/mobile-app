package com.example.onespace.domain.music.repository

import kotlinx.coroutines.flow.Flow

/** F-VID-10: one entry in the personal music library, taken from a video. */
data class ExtractedAudio(
    val videoId: String,
    val title: String,
    val creator: String,
    val url: String,
    val duration: String
)

/** Extracted entries live in the music library; playing one opens the
 * video in audio-only mode (no MP3 file is produced on disk). */
interface ExtractedAudioRepository {
    fun observeAll(): Flow<List<ExtractedAudio>>
    suspend fun add(videoId: String, title: String, creator: String, url: String, duration: String)
    suspend fun remove(videoId: String)
}
