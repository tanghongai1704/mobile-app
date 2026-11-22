package com.example.onespace.core.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.onespace.core.database.ContentDatabase
import com.example.onespace.core.network.ApiException
import com.example.onespace.core.network.NetworkUnavailableException
import com.example.onespace.core.network.SessionTokens
import com.example.onespace.core.network.api.MeApi
import com.example.onespace.core.network.di.BackendConfig
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncEntryPoint {
    fun meApi(): MeApi
    fun database(): ContentDatabase
    fun tokens(): SessionTokens
    fun preferences(): SessionPreferencesRepository
    fun playlists(): PlaylistPort
}

/**
 * Deferred sync with WorkManager: runs only with a network connection, only for a signed-in
 * user and only against the real server (the fake backend has no `/me` data). Failures retry
 * with exponential backoff; the app keeps working from Room meanwhile.
 */
class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (BackendConfig.useFakeBackend) return Result.success()
        val deps = EntryPointAccessors.fromApplication(applicationContext, SyncEntryPoint::class.java)
        if (deps.tokens().accessToken() == null) return Result.success()
        val user = deps.preferences().preferences.first().authEmail ?: return Result.success()
        val db = deps.database()
        val engine = SyncEngine(
            api = deps.meApi(),
            events = db.pendingEventDao(),
            missions = db.dailyMissionDao(),
            progress = db.videoProgressDao(),
            catalog = db.catalogItemDao(),
            playlistState = db.playlistSyncDao(),
            playlists = deps.playlists(),
            cursor = PrefsSyncCursor(applicationContext)
        )
        return try {
            val report = engine.run(user)
            Log.i(TAG, "sync ok: $report")
            Result.success()
        } catch (e: NetworkUnavailableException) {
            Result.retry()
        } catch (e: ApiException) {
            Log.w(TAG, "sync stopped: ${e.httpStatus} ${e.code}")
            if (e.httpStatus == 401) Result.success() else Result.retry()
        }
    }

    companion object {
        private const val TAG = "OneSpaceSync"
        private const val PERIODIC = "sync-periodic"
        private const val NOW = "sync-now"
        private val online = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        /** Idempotent; call on app start. */
        fun schedule(context: Context) {
            if (BackendConfig.useFakeBackend) return
            runCatching {
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
                    PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS).setConstraints(online).build()
                )
            }
        }

        /** Requests a pass soon (after sign-in, or when the app returns to the foreground). */
        fun syncNow(context: Context) {
            if (BackendConfig.useFakeBackend) return
            runCatching {
                WorkManager.getInstance(context).enqueueUniqueWork(
                    NOW, ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<SyncWorker>()
                        .setConstraints(online)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                        .build()
                )
            }
        }
    }
}

internal class PrefsSyncCursor(context: Context) : SyncCursor {
    private val prefs = context.getSharedPreferences("onespace_sync", Context.MODE_PRIVATE)
    override var owner: String?
        get() = prefs.getString("owner", null)
        set(value) = prefs.edit().putString("owner", value).apply()
    override var progressPushedUntil: Long
        get() = prefs.getLong("progress_pushed", 0)
        set(value) = prefs.edit().putLong("progress_pushed", value).apply()
    override var progressPulledUntil: Long
        get() = prefs.getLong("progress_pulled", 0)
        set(value) = prefs.edit().putLong("progress_pulled", value).apply()
}
