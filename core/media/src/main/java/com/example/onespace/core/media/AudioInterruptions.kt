package com.example.onespace.core.media

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * External interruptions for the voices the app arbitrates itself (video and TTS). Music already
 * yields through Media3 audio focus and pauses on "becoming noisy"; video ignores system focus on
 * purpose (case C1 mutes it instead), so it and the TTS reader are paused here when headphones are
 * unplugged or a call starts. Resuming is left to the user (Play), as the product rules state.
 */
@Singleton
class AudioInterruptions @Inject constructor(
    @ApplicationContext private val context: Context,
    private val media: MediaPlaybackCoordinator,
    private val tts: AndroidTtsEngine
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var started = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pauseArbitratedVoices()
        }
    }

    /** Called once from the Application. */
    fun start() {
        if (started) return
        started = true
        ContextCompat.registerReceiver(
            context, noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val audio = context.getSystemService(AudioManager::class.java)
            audio.addOnModeChangedListener(ContextCompat.getMainExecutor(context)) { mode ->
                if (mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL ||
                    mode == AudioManager.MODE_IN_COMMUNICATION
                ) pauseArbitratedVoices()
            }
        }
    }

    private fun pauseArbitratedVoices() {
        if (media.video.value.playRequested) media.pauseVideo()
        scope.launch { if (tts.state.value.isPlaying) tts.pause() }
    }
}
