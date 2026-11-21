package com.example.onespace.domain.session.port

/**
 * F-VID-07 / F-MUS-12: media files kept on the device for offline playback. Only the app's own
 * catalogue is downloaded; the player prefers [localUri] over the stream URL when it exists.
 */
interface OfflineMediaStore {
    /** Downloads [url] for [contentId]; returns false when the file could not be saved. */
    suspend fun save(contentId: String, url: String): Boolean
    suspend fun delete(contentId: String)
    /** A playable `file://` URI when the item is on the device, else null. */
    fun localUri(contentId: String): String?
}
