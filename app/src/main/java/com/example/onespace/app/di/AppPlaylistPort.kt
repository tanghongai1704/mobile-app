package com.example.onespace.app.di

import com.example.onespace.core.designsystem.AppPlaylist
import com.example.onespace.core.designsystem.AppPlaylistStore
import com.example.onespace.core.sync.PlaylistPort
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gives the sync worker the playlists the screens use. Waits until the saved copy has been
 * loaded at app start, so an early sync never mistakes "not loaded yet" for "deleted".
 */
@Singleton
class AppPlaylistPort @Inject constructor() : PlaylistPort {
    val restored = CompletableDeferred<Unit>()

    override suspend fun local(): Map<String, List<String>> {
        restored.await()
        return withContext(Dispatchers.Main) { AppPlaylistStore.playlists.mapValues { it.value.orderedIds() } }
    }

    override suspend fun put(name: String, ids: List<String>) {
        restored.await()
        // Content ids carry their section: v… video, m… music, n… news.
        val playlist = AppPlaylist(
            name = name,
            videoIds = ids.filter { it.startsWith("v") },
            trackIds = ids.filter { it.startsWith("m") },
            order = ids,
            articleIds = ids.filter { it.startsWith("n") }
        )
        withContext(Dispatchers.Main) { AppPlaylistStore.replace(playlist) }
    }
}
