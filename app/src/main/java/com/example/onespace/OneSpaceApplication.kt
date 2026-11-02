package com.example.onespace

import android.app.Application
import com.example.onespace.core.datastore.LocalJsonStore
import com.example.onespace.core.designsystem.AppPlaylistStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltAndroidApp
class OneSpaceApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Playlists live in an in-memory store the screens read directly; load the saved copy
        // at start and write every change back.
        val store = LocalJsonStore(this)
        appScope.launch {
            store.read(PLAYLISTS_KEY)?.let { saved -> withContext(Dispatchers.Main) { AppPlaylistStore.restore(saved) } }
            AppPlaylistStore.onChanged = { json -> appScope.launch { store.write(PLAYLISTS_KEY, json) } }
        }
    }

    private companion object { const val PLAYLISTS_KEY = "playlists" }
}
