package com.example.onespace.domain.video

import com.example.onespace.domain.video.model.VideoQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** F-VID-06: quality picks must resolve to a real variant of the same stream. */
class VideoQualityTest {

    private val bb360 = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4"
    private val bb720 = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"
    private val muxHi = "https://stream.mux.com/DS00Spx1CV902MCtPj5WknGlR102V5HFkDe/high.mp4"
    private val muxLo = "https://stream.mux.com/DS00Spx1CV902MCtPj5WknGlR102V5HFkDe/low.mp4"
    private val ocean = "https://vjs.zencdn.net/v/oceans.mp4"

    @Test fun sdPickSwapsTestVideosToThe360Variant() {
        assertEquals(bb360, VideoQuality.variantUrl(bb720, VideoQuality.SD))
    }

    @Test fun hdPickSwapsTestVideosToThe720Variant() {
        assertEquals(bb720, VideoQuality.variantUrl(bb360, VideoQuality.HD))
    }

    @Test fun fhdPickResolvesToTheHighestRealVariantOfTheSource() {
        // test-videos tops out at 720; mux high is mux's best rendition.
        assertEquals(bb720, VideoQuality.variantUrl(bb360, VideoQuality.FHD))
        assertEquals(muxHi, VideoQuality.variantUrl(muxLo, VideoQuality.FHD))
    }

    @Test fun muxStreamSwapsBetweenItsHighAndLowRenditions() {
        assertEquals(muxLo, VideoQuality.variantUrl(muxHi, VideoQuality.SD))
        assertEquals(muxHi, VideoQuality.variantUrl(muxLo, VideoQuality.HD))
    }

    @Test fun singleRenditionSourcesKeepTheirOnlyStream() {
        assertNull(VideoQuality.variantUrl(ocean, VideoQuality.SD))
        assertNull(VideoQuality.variantUrl(ocean, VideoQuality.HD))
        assertNull(VideoQuality.variantUrl(ocean, VideoQuality.FHD))
    }

    @Test fun autoAndUnknownPicksNeverChangeTheStream() {
        assertNull(VideoQuality.variantUrl(bb360, VideoQuality.AUTO))
        assertNull(VideoQuality.variantUrl(bb360, "Tiết kiệm"))
    }

    @Test fun alreadyAtTheWantedQualityReturnsNull() {
        assertNull(VideoQuality.variantUrl(bb360, VideoQuality.SD))
        assertNull(VideoQuality.variantUrl(bb720, VideoQuality.HD))
        assertNull(VideoQuality.variantUrl(bb720, VideoQuality.FHD))
        assertNull(VideoQuality.variantUrl(muxHi, VideoQuality.HD))
        assertNull(VideoQuality.variantUrl(muxLo, VideoQuality.SD))
    }
}
