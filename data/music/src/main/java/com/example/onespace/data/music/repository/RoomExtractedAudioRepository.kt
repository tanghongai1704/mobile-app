package com.example.onespace.data.music.repository

import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.database.ExtractedAudioEntity
import com.example.onespace.domain.music.repository.ExtractedAudio
import com.example.onespace.domain.music.repository.ExtractedAudioRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** F-VID-10: extracted-audio entries on top of the shared Room database (ADR-09). */
internal class RoomExtractedAudioRepository(
    private val database: ContentDatabase
) : ExtractedAudioRepository {

    override fun observeAll(): Flow<List<ExtractedAudio>> =
        database.extractedAudioDao().observeAll().map { list ->
            list.map { ExtractedAudio(it.videoId, it.title, it.creator, it.url, it.duration) }
        }

    override suspend fun add(videoId: String, title: String, creator: String, url: String, duration: String) =
        database.extractedAudioDao().upsert(
            ExtractedAudioEntity(videoId, title, creator, url, duration, System.currentTimeMillis())
        )

    override suspend fun remove(videoId: String) =
        database.extractedAudioDao().delete(videoId)
}

fun createExtractedAudioRepository(database: ContentDatabase): ExtractedAudioRepository =
    RoomExtractedAudioRepository(database)
