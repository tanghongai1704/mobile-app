package com.example.onespace.domain.video.model

/**
 * F-VID-06: quality picks resolve to a real stream variant when the catalog
 * source has one — test-videos ships 360/720 files of the same clip and the
 * mux stream has high/low renditions. 1080p and 720p both land on the
 * highest rendition the source actually serves; sources with a single
 * rendition (oceans) simply keep playing their only file.
 */
object VideoQuality {
    const val AUTO = "Auto"
    const val FHD = "1080p"
    const val HD = "720p"
    const val SD = "360p"

    val options = listOf(AUTO, FHD, HD, SD)

    /** Variant URL for [quality], or null when the source has no such rendition. */
    fun variantUrl(url: String, quality: String): String? {
        if (quality !in options || quality == AUTO) return null
        return when {
            // Mux only exposes high/low; high is the best it can serve.
            url.contains("stream.mux.com") -> when (quality) {
                SD -> muxVariant(url, "low.mp4")
                else -> muxVariant(url, "high.mp4")
            }
            // test-videos clips ship 360/720; 720 is the top rendition.
            url.contains("test-videos.co.uk") -> when (quality) {
                SD -> testVideosVariant(url, "360")
                else -> testVideosVariant(url, "720")
            }
            else -> null
        }
    }

    private fun muxVariant(url: String, file: String): String? {
        val candidate = url.substringBeforeLast('/') + "/" + file
        return candidate.takeIf { it != url }
    }

    private fun testVideosVariant(url: String, wanted: String): String? {
        if (url.contains("/$wanted/")) return null
        return url
            .replace(Regex("""/(\d{3,4})/"""), "/$wanted/")
            .replace(Regex("""_(\d{3,4})_"""), "_${wanted}_")
    }
}
