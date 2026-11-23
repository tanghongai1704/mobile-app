package com.example.onespace

import android.app.Application
import com.example.onespace.app.di.AppPlaylistPort
import com.example.onespace.core.datastore.LocalJsonStore
import com.example.onespace.core.designsystem.AppPlaylistStore
import com.example.onespace.core.sync.SyncWorker
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltAndroidApp
class OneSpaceApplication : Application() {
    @Inject lateinit var audioInterruptions: com.example.onespace.core.media.AudioInterruptions
    @Inject lateinit var playlistPort: AppPlaylistPort
    @Inject lateinit var preferences: SessionPreferencesRepository
    @Inject lateinit var database: com.example.onespace.core.database.ContentDatabase

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @OptIn(FlowPreview::class)
    override fun onCreate() {
        super.onCreate()
        // Headphones unplugged or a call starting pause video and the TTS reader.
        audioInterruptions.start()
        // F-COM-12: hourly check for the daily mission / streak reminders.
        com.example.onespace.core.sync.ReminderWorker.schedule(this)
        // Server sync (real backend only): hourly, plus a pass at start, after sign-in, after playlist edits
        // and when the event outbox fills.
        SyncWorker.schedule(this)
        // Playlists live in an in-memory store the screens read directly; load the saved copy
        // at start and write every change back.
        val store = LocalJsonStore(this)
        appScope.launch {
            store.read(PLAYLISTS_KEY)?.let { saved -> withContext(Dispatchers.Main) { AppPlaylistStore.restore(saved) } }
            AppPlaylistStore.onChanged = { json ->
                appScope.launch { store.write(PLAYLISTS_KEY, json) }
                SyncWorker.syncNow(this@OneSpaceApplication)
            }
            playlistPort.restored.complete(Unit)
        }
        appScope.launch {
            preferences.preferences.map { it.authEmail }.distinctUntilChanged().filterNotNull()
                .collect { SyncWorker.syncNow(this@OneSpaceApplication) }
        }
        // New points or mission actions in the outbox: send them a few seconds later, batched.
        appScope.launch {
            database.pendingEventDao().observeCount().filter { it > 0 }.debounce(5_000)
                .collect { SyncWorker.syncNow(this@OneSpaceApplication) }
        }
    }

    private companion object { const val PLAYLISTS_KEY = "playlists" }
}
