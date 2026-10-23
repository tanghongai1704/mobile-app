package com.example.onespace.data.video.repository

import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.VideoRepository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * Bundled demo catalog (per the MVP decision "tự dựng catalog trong phạm vi môn học"):
 * public CC sample streams from stable open CDNs, so the player runs for real with
 * no content license. Replace this binding, not the UI, when integrating real sources.
 */
internal class MockVideoRepository : VideoRepository {
    override fun observeCatalog() = flow {
        delay(400)
        emit(listOf(
        Video(
            "v1", "Gần với thiên nhiên thêm chút", "OneSpace Studio", "1:00",
            "https://cdn.jsdelivr.net/gh/mediaelement/mediaelement-files@master/big_buck_bunny.mp4"
        ),
        Video(
            "v2", "Sài Gòn, sau giờ tan", "Chuyện phố", "0:46",
            "https://vjs.zencdn.net/v/oceans.mp4"
        ),
        Video(
            "v3", "Nghệ thuật chậm lại", "Sống chậm", "0:06",
            "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"
        ),
        Video(
            "v4", "Khung hình êm, phố vắng", "Sống chậm", "0:30",
            "https://stream.mux.com/DS00Spx1CV902MCtPj5WknGlR102V5HFkDe/high.mp4"
        )
        ))
    }
}
