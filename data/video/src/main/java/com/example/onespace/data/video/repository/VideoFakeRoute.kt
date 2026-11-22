package com.example.onespace.data.video.repository

import com.example.onespace.core.network.api.ApiErrorCode
import com.example.onespace.core.network.api.PageDto
import com.example.onespace.core.network.api.VideoDto
import com.example.onespace.core.network.fake.FakeJson
import com.example.onespace.core.network.fake.FakeRequest
import com.example.onespace.core.network.fake.FakeResponse
import com.example.onespace.core.network.fake.FakeRoute

/** `GET /catalog/videos` for the in-process fake backend, serving the bundled sample catalogue. */
internal class VideoFakeRoute(
    private val videos: () -> List<VideoDto> = {
        MockVideoRepository().catalog.map {
            VideoDto(it.id, it.title, it.creator, parseDuration(it.duration), it.url, topic = it.topic, durationLabel = it.duration)
        }
    }
) : FakeRoute {
    override fun handle(request: FakeRequest): FakeResponse? {
        if (request.method != "GET" || request.path != "catalog/videos") return null
        val page = request.query["page"]?.toIntOrNull() ?: 1
        val limit = request.query["limit"]?.toIntOrNull() ?: 20
        if (page < 1 || limit !in 1..50) return FakeJson.error(400, ApiErrorCode.INVALID_PARAMS, "Tham số phân trang không hợp lệ")
        val all = videos()
        val from = (page - 1) * limit
        val items = all.drop(from).take(limit)
        return FakeJson.ok(PageDto.serializer(VideoDto.serializer()), PageDto(items, page, hasMore = from + items.size < all.size))
    }
}
