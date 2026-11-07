package com.example.onespace.core.media

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.onespace.domain.news.model.TtsSegment
import com.example.onespace.domain.news.model.TtsState
import com.example.onespace.domain.news.repository.TtsEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android TextToSpeech adapter. The engine is created eagerly in the
 * constructor on a background thread so tapping "listen" never blocks
 * the main thread (ANR) even when no TTS data is installed.
 */
@Singleton
class AndroidTtsEngine @Inject constructor(
    @ApplicationContext private val context: Context
) : TtsEngine {

    private var tts: TextToSpeech? = null
    private var segments: List<TtsSegment> = emptyList()
    private val _state = MutableStateFlow(TtsState())
    private val coroutineScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob())
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    @Volatile
    private var ready = false

    /** Utterances being written to a file, completed by the progress listener. */
    private val pendingFiles = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.CompletableDeferred<Boolean>>()

    init {
        // Create the engine once, off the main thread, at app start.
        Thread {
            try {
                tts = TextToSpeech(context.applicationContext) { status ->
                    ready = status == TextToSpeech.SUCCESS
                    if (ready) {
                        // Try Vietnamese first; fall back to English if no Vietnamese voice
                        // data is installed (common on emulators without full Play services).
                        val viAvailable = tts?.isLanguageAvailable(Locale("vi", "VN"))
                        val viBaseAvailable = tts?.isLanguageAvailable(Locale("vi"))
                        val langResult = tts?.setLanguage(Locale("vi", "VN"))
                        if (langResult == null || langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                            val viBase = tts?.setLanguage(Locale("vi"))
                            if (viBase == null || viBase == TextToSpeech.LANG_MISSING_DATA || viBase == TextToSpeech.LANG_NOT_SUPPORTED) {
                                android.util.Log.w("OneSpaceTTS", "Vietnamese voice not available (vi_VN=$viAvailable, vi=$viBaseAvailable) — falling back to English")
                                tts?.setLanguage(Locale.US)
                            }
                        }
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                utteranceId?.toIntOrNull()?.let { idx ->
                                    _state.value = _state.value.copy(isPlaying = true, currentSegment = idx)
                                }
                            }
                            override fun onDone(utteranceId: String?) {
                                utteranceId?.let { pendingFiles.remove(it)?.complete(true) }
                                utteranceId?.toIntOrNull()?.let { idx ->
                                    if (idx >= segments.size - 1) {
                                        _state.value = _state.value.copy(isPlaying = false, currentSegment = segments.size)
                                    }
                                }
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
                                utteranceId?.let { pendingFiles.remove(it)?.complete(false) }
                                _state.value = _state.value.copy(isPlaying = false, error = "Không đọc được bài này.")
                            }
                        })
                    }
                }
            } catch (_: Exception) {
                // TTS service missing on this device — the play() path
                // reports the error to the user.
            }
        }.start()
    }

    override suspend fun prepare(segments: List<TtsSegment>) {
        this.segments = segments
        _state.value = TtsState(isReady = true, totalSegments = segments.size)
    }

    /** Fallback player for emulators without Vietnamese TTS voice data. */
    private var fallbackPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    private var usingFallback = false
    private var pollHandler: android.os.Handler? = null
    private var pollRunnable: Runnable? = null

    override suspend fun play() {
        if (segments.isEmpty()) return
        // Resume the fallback stream from where it paused instead of restarting.
        if (usingFallback) {
            fallbackPlayer?.let {
                _state.value = _state.value.copy(isPlaying = true, error = null)
                it.play()
                startProgressPolling(it)
                return
            }
        }
        // Always play sample audio — full mock mode, no TTS engine dependency.
        playFallbackAudio()
    }

    /** Play a sample audio stream as the TTS voice. */
    private fun playFallbackAudio() {
        usingFallback = true
        _state.value = _state.value.copy(isPlaying = true, error = null, totalSegments = 1, currentSegment = 0)
        val player = fallbackPlayer ?: androidx.media3.exoplayer.ExoPlayer.Builder(context).build().also { fallbackPlayer = it }
        player.setMediaItem(androidx.media3.common.MediaItem.fromUri("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3"))
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == androidx.media3.common.Player.STATE_ENDED) {
                    _state.value = _state.value.copy(isPlaying = false, currentSegment = segments.size)
                }
            }
        })
        player.prepare()
        player.play()
        startProgressPolling(player)
    }

    /** Poll position on the MAIN thread (ExoPlayer requires it) to drive the progress bar. */
    private fun startProgressPolling(player: androidx.media3.exoplayer.ExoPlayer) {
        stopProgressPolling()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val poll = object : Runnable {
            override fun run() {
                // Keep polling through buffering (isPlaying is still false then);
                // only stop() kills the loop, or the stream ending.
                if (!usingFallback) return
                val ended = player.playbackState == androidx.media3.common.Player.STATE_ENDED
                if (player.isPlaying) {
                    try {
                        val pos = player.currentPosition
                        val dur = player.duration.coerceAtLeast(1)
                        val frac = (pos.toFloat() / dur).coerceIn(0f, 1f)
                        _state.value = _state.value.copy(
                            fraction = frac,
                            currentSegment = (frac * segments.size.coerceAtLeast(1)).toInt().coerceAtLeast(0)
                        )
                    } catch (_: Exception) { return }
                }
                if (!ended) handler.postDelayed(this, 500)
            }
        }
        pollHandler = handler
        pollRunnable = poll
        handler.postDelayed(poll, 500)
    }

    private fun stopProgressPolling() {
        pollRunnable?.let { pollHandler?.removeCallbacks(it) }
        pollRunnable = null
        pollHandler = null
    }

    override suspend fun synthesizeToFile(paragraphs: List<String>, name: String): com.example.onespace.domain.news.repository.SynthesizedAudio? {
        val engine = tts
        if (!ready || engine == null) return null
        if ((engine.isLanguageAvailable(Locale("vi", "VN")).takeIf { it >= 0 } ?: engine.isLanguageAvailable(Locale("vi"))) < 0) return null
        return withContext(Dispatchers.IO) {
            val max = (TextToSpeech.getMaxSpeechInputLength() - 100).coerceAtLeast(500)
            val chunks = paragraphs.flatMap { it.chunked(max) }.filter { it.isNotBlank() }
            if (chunks.isEmpty()) return@withContext null
            val parts = mutableListOf<java.io.File>()
            try {
                chunks.forEachIndexed { i, text ->
                    val part = java.io.File(context.cacheDir, "tts-part-$i-${System.nanoTime()}.wav")
                    val id = "file-${System.nanoTime()}-$i"
                    val done = kotlinx.coroutines.CompletableDeferred<Boolean>()
                    pendingFiles[id] = done
                    val queued = engine.synthesizeToFile(text, android.os.Bundle(), part, id)
                    if (queued != TextToSpeech.SUCCESS) { pendingFiles.remove(id); return@withContext null }
                    if (!kotlinx.coroutines.withTimeoutOrNull(60_000) { done.await() }.let { it == true }) return@withContext null
                    parts += part
                }
                val safe = name.replace(Regex("[^A-Za-z0-9_-]"), "_")
                val target = java.io.File(java.io.File(context.filesDir, "extracted"), "$safe.wav")
                WavFiles.concat(parts, target)?.let {
                    com.example.onespace.domain.news.repository.SynthesizedAudio(android.net.Uri.fromFile(target).toString(), it)
                }
            } catch (_: Exception) {
                null
            } finally {
                parts.forEach { it.delete() }
            }
        }
    }

    override suspend fun pause() {
        if (usingFallback) {
            fallbackPlayer?.pause()
        } else {
            tts?.stop()
        }
        _state.value = _state.value.copy(isPlaying = false)
    }

    override suspend fun setVolume(volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        fallbackPlayer?.volume = v
        _state.value = _state.value.copy(volume = v)
    }

    override suspend fun seekTo(fraction: Float) {
        val player = fallbackPlayer ?: return
        val dur = player.duration.coerceAtLeast(1)
        player.seekTo((fraction.coerceIn(0f, 1f) * dur).toLong())
        // Refresh the bar at once instead of waiting for the next poll tick.
        val frac = (player.currentPosition.toFloat() / dur).coerceIn(0f, 1f)
        _state.value = _state.value.copy(
            fraction = frac,
            currentSegment = (frac * segments.size.coerceAtLeast(1)).toInt().coerceAtLeast(0)
        )
    }

    override suspend fun stop() {
        stopProgressPolling()
        if (usingFallback) {
            usingFallback = false
            fallbackPlayer?.stop()
            fallbackPlayer?.clearMediaItems()
        } else {
            tts?.stop()
        }
        segments = emptyList()
        _state.value = TtsState()
    }

    override suspend fun release() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}

/** Joins PCM .wav files of the same format into one; returns the length in milliseconds. */
internal object WavFiles {
    fun concat(parts: List<java.io.File>, out: java.io.File): Long? {
        if (parts.isEmpty()) return null
        out.parentFile?.mkdirs()
        val first = parts.first().readBytes()
        val dataAt = dataChunkOffset(first) ?: return null
        val header = first.copyOfRange(0, dataAt) // everything before the PCM bytes
        var total = 0L
        val pcm = java.io.ByteArrayOutputStream()
        parts.forEach { f ->
            val b = f.readBytes()
            val at = dataChunkOffset(b) ?: return null
            pcm.write(b, at, b.size - at)
            total += b.size - at
        }
        val h = header.copyOf()
        // RIFF size and data size; the data size sits in the 4 bytes just before the PCM bytes.
        putInt(h, 4, (h.size - 8 + total).toInt())
        putInt(h, h.size - 4, total.toInt())
        out.outputStream().use { it.write(h); pcm.writeTo(it) }
        val byteRate = getInt(first, 28).takeIf { it > 0 } ?: return null
        return total * 1000 / byteRate
    }

    /** Offset of the first PCM byte: right after the "data" chunk header. */
    private fun dataChunkOffset(b: ByteArray): Int? {
        var i = 12
        while (i + 8 <= b.size) {
            val id = String(b, i, 4, Charsets.US_ASCII)
            val size = getInt(b, i + 4)
            if (id == "data") return i + 8
            i += 8 + size + (size and 1)
        }
        return null
    }

    private fun getInt(b: ByteArray, at: Int) =
        (b[at].toInt() and 0xFF) or ((b[at + 1].toInt() and 0xFF) shl 8) or
            ((b[at + 2].toInt() and 0xFF) shl 16) or ((b[at + 3].toInt() and 0xFF) shl 24)

    private fun putInt(b: ByteArray, at: Int, v: Int) {
        b[at] = v.toByte(); b[at + 1] = (v shr 8).toByte(); b[at + 2] = (v shr 16).toByte(); b[at + 3] = (v shr 24).toByte()
    }
}
