package com.example.onespace.core.media

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * F-MUS-03 / F-MUS-10: owns the MediaSession of the music engine. Media3 promotes the service to a
 * `mediaPlayback` foreground service while music plays, shows the media notification and exposes
 * the same controls to the lock screen, Bluetooth headsets and Android Auto. The player itself is
 * the shared engine in [MediaPlaybackCoordinator], so the in-app dock and the notification always
 * agree. Video stays in-app (Section 4: system PiP and background video are out of scope).
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var coordinator: MediaPlaybackCoordinator

    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val launch = packageManager.getLaunchIntentForPackage(packageName)
        session = MediaSession.Builder(this, coordinator.musicSessionPlayer)
            .setId("onespace-music")
            .apply {
                if (launch != null) {
                    setSessionActivity(
                        PendingIntent.getActivity(
                            this@MusicPlaybackService, 0, launch,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                    )
                }
            }
            .build()
            // Register the session right away: Media3 only posts the media notification (and
            // promotes the service to the foreground) for sessions the service knows about. Without
            // this the notification would only appear once an external controller connected.
            .also { addSession(it) }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiping the app away keeps music only while it is actually playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.playbackState == Player.STATE_ENDED) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        // The engine is shared with the app process; only the session is released here.
        session?.release()
        session = null
        super.onDestroy()
    }

    companion object {
        /**
         * Called when music starts from the UI. The app is in the foreground then, so a plain
         * start is allowed; Media3 itself moves the service to the foreground once playback runs.
         */
        fun ensureStarted(context: Context) {
            runCatching { context.startService(Intent(context, MusicPlaybackService::class.java)) }
        }
    }
}
