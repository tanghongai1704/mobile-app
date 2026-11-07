package com.example.onespace.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * F-VID-15: ONE playlist hub for the whole app — videos and tracks live in
 * the same playlists. The feeds never render playlist lists; the profile
 * screen owns the single entry point.
 *
 * [order] is the unified play order: every video and track id in the
 * sequence the user added them, so a mixed playlist can play video → track
 * → video. Playlists saved before it existed fall back to videos-then-tracks.
 */
data class AppPlaylist(
    val name: String,
    val videoIds: List<String> = emptyList(),
    val trackIds: List<String> = emptyList(),
    val order: List<String> = emptyList()
) {
    /** Every item id in mixed play order; falls back to videos then tracks when the order is missing or stale. */
    fun orderedIds(): List<String> {
        val union = (videoIds + trackIds).distinct()
        return if (order.toSet() == union.toSet()) order else union
    }
}

/** "2 video · 3 bài hát" — both counts together, only the parts that exist. */
@Composable
fun playlistCountsLabel(playlist: AppPlaylist): String = buildList {
    if (playlist.videoIds.isNotEmpty()) add(playlist.videoIds.size.toString() + " video")
    if (playlist.trackIds.isNotEmpty()) add(playlist.trackIds.size.toString() + tr(" bài hát", " songs"))
}.joinToString(" · ")

object AppPlaylistStore {
    var playlists by mutableStateOf<Map<String, AppPlaylist>>(emptyMap())
        private set

    /** Set by the app to save every change; the store itself stays free of storage code. */
    var onChanged: ((String) -> Unit)? = null

    /** Serialised form of playlists and their play settings. */
    fun toJson(): String {
        val root = org.json.JSONObject()
        root.put("playlists", org.json.JSONArray().also { arr ->
            playlists.values.forEach { p ->
                arr.put(org.json.JSONObject().put("name", p.name)
                    .put("videoIds", org.json.JSONArray(p.videoIds))
                    .put("trackIds", org.json.JSONArray(p.trackIds))
                    .put("order", org.json.JSONArray(p.orderedIds())))
            }
        })
        root.put("settings", org.json.JSONObject().also { obj ->
            playSettings.forEach { (name, st) ->
                obj.put(name, org.json.JSONObject().put("order", st.order).put("repeat", st.repeat))
            }
        })
        return root.toString()
    }

    /** Replaces the in-memory state with a previously saved snapshot; bad data is ignored. */
    fun restore(json: String) {
        runCatching {
            val root = org.json.JSONObject(json)
            val list = root.optJSONArray("playlists") ?: org.json.JSONArray()
            fun ids(a: org.json.JSONArray?) = (0 until (a?.length() ?: 0)).map { a!!.getString(it) }
            playlists = (0 until list.length()).map { list.getJSONObject(it) }.associate { o ->
                o.getString("name") to AppPlaylist(o.getString("name"), ids(o.optJSONArray("videoIds")), ids(o.optJSONArray("trackIds")), ids(o.optJSONArray("order")))
            }
            val st = root.optJSONObject("settings")
            playSettings = st?.keys()?.asSequence()?.associateWith { k ->
                val o = st.getJSONObject(k)
                PlaySettings(o.optString("order", "sequential"), o.optBoolean("repeat", false))
            } ?: emptyMap()
        }
    }

    private fun changed() { onChanged?.invoke(toJson()) }

    fun create(name: String, videoIds: List<String> = emptyList(), trackIds: List<String> = emptyList()) {
        if (name.isBlank()) return
        playlists = playlists + (name to AppPlaylist(name, videoIds, trackIds, videoIds + trackIds))
        changed()
    }

    fun addVideo(name: String, videoId: String) {
        val current = playlists[name] ?: AppPlaylist(name)
        playlists = playlists + (name to current.copy(
            videoIds = (current.videoIds + videoId).distinct(),
            order = if (videoId in current.order) current.order else current.order + videoId
        ))
        changed()
    }

    fun addTrack(name: String, trackId: String) {
        val current = playlists[name] ?: AppPlaylist(name)
        playlists = playlists + (name to current.copy(
            trackIds = (current.trackIds + trackId).distinct(),
            order = if (trackId in current.order) current.order else current.order + trackId
        ))
        changed()
    }

    fun remove(name: String) { playlists = playlists - name; changed() }

    /** Removing one item keeps the playlist and its play order intact. */
    fun removeVideo(name: String, videoId: String) {
        val current = playlists[name] ?: return
        playlists = playlists + (name to current.copy(
            videoIds = current.videoIds - videoId,
            order = current.order - videoId
        ))
        changed()
    }

    fun removeTrack(name: String, trackId: String) {
        val current = playlists[name] ?: return
        playlists = playlists + (name to current.copy(
            trackIds = current.trackIds - trackId,
            order = current.order - trackId
        ))
        changed()
    }

    /** How a playlist plays: order is sequential|shuffle, repeat is on/off. */
    data class PlaySettings(
        val order: String = "sequential",
        val repeat: Boolean = false
    )

    var playSettings by mutableStateOf<Map<String, PlaySettings>>(emptyMap())
        private set

    fun settingsOf(name: String): PlaySettings = playSettings[name] ?: PlaySettings()

    fun setOrder(name: String, order: String) {
        val current = settingsOf(name)
        playSettings = playSettings + (name to current.copy(order = order))
        changed()
    }

    fun toggleOrder(name: String) {
        val current = settingsOf(name)
        setOrder(name, if (current.order == "shuffle") "sequential" else "shuffle")
    }

    fun toggleRepeat(name: String) {
        val current = settingsOf(name)
        playSettings = playSettings + (name to current.copy(repeat = !current.repeat))
        changed()
    }
}
