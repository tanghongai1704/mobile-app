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
    override val state: StateFlow<TtsState> = _state.asStateFlow()

    @Volatile
    private var ready = false

    init {
        // Create the engine once, off the main thread, at app start.
        Thread {
            try {
                tts = TextToSpeech(context.applicationContext) { status ->
                    ready = status == TextToSpeech.SUCCESS
                    if (ready) {
                        tts?.language = Locale("vi", "VN")
                        val vi = tts?.isLanguageAvailable(Locale("vi", "VN"))
                        if (vi == null || vi < 0) {
                            tts?.language = Locale("vi")
                        }
                        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                            override fun onStart(utteranceId: String?) {
                                utteranceId?.toIntOrNull()?.let { idx ->
                                    _state.value = _state.value.copy(isPlaying = true, currentSegment = idx)
                                }
                            }
                            override fun onDone(utteranceId: String?) {
                                utteranceId?.toIntOrNull()?.let { idx ->
                                    if (idx >= segments.size - 1) {
                                        _state.value = _state.value.copy(isPlaying = false, currentSegment = segments.size)
                                    }
                                }
                            }
                            @Deprecated("Deprecated in Java")
                            override fun onError(utteranceId: String?) {
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

    override suspend fun play() {
        if (!ready || tts == null) {
            _state.value = _state.value.copy(error = "Thiết bị không có bộ đọc tiếng Việt.")
            return
        }
        if (segments.isEmpty()) return
        // Check that the engine actually has a language available.
        val hasEngine = try {
            tts?.engines?.isNotEmpty() == true || (tts?.isLanguageAvailable(Locale("vi")) ?: -1) >= 0
        } catch (_: Exception) { false }
        if (!hasEngine) {
            _state.value = _state.value.copy(error = "Thiết bị không có bộ đọc tiếng Việt.")
            return
        }
        _state.value = _state.value.copy(isPlaying = true, error = null)
        // speak() with QUEUE_ADD is non-blocking per Android docs.
        segments.forEach { segment ->
            try {
                tts?.speak(segment.text, TextToSpeech.QUEUE_ADD, null, segment.index.toString())
            } catch (_: Exception) { }
        }
    }

    override suspend fun pause() {
        tts?.stop()
        _state.value = _state.value.copy(isPlaying = false)
    }

    override suspend fun stop() {
        tts?.stop()
        segments = emptyList()
        _state.value = TtsState()
    }

    override suspend fun release() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}
