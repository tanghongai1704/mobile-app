package com.example.onespace.data.music.repository

import com.example.onespace.core.network.api.ApiErrorCode
import com.example.onespace.core.network.api.PageDto
import com.example.onespace.core.network.api.TrackDto
import com.example.onespace.core.network.fake.FakeJson
import com.example.onespace.core.network.fake.FakeRequest
import com.example.onespace.core.network.fake.FakeResponse
import com.example.onespace.core.network.fake.FakeRoute

/** `GET /catalog/tracks` for the in-process fake backend, serving the bundled sample catalogue. */
internal class MusicFakeRoute(
    private val tracks: () -> List<TrackDto> = {
        MockMusicRepository().catalog.map {
            val seconds = it.duration.split(':').fold(0) { acc, p -> acc * 60 + (p.toIntOrNull() ?: 0) }
            TrackDto(it.id, it.title, it.artist, duration = seconds, url = it.url, durationLabel = it.duration, lyrics = it.lyrics, bpm = it.bpm)
        }
    }
) : FakeRoute {
    override fun handle(request: FakeRequest): FakeResponse? {
        if (request.method != "GET" || request.path != "catalog/tracks") return null
        val page = request.query["page"]?.toIntOrNull() ?: 1
        val limit = request.query["limit"]?.toIntOrNull() ?: 20
        if (page < 1 || limit !in 1..50) return FakeJson.error(400, ApiErrorCode.INVALID_PARAMS, "Tham số phân trang không hợp lệ")
        val all = tracks()
        val from = (page - 1) * limit
        val items = all.drop(from).take(limit)
        return FakeJson.ok(PageDto.serializer(TrackDto.serializer()), PageDto(items, page, hasMore = from + items.size < all.size))
    }
}
