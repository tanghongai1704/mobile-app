package com.example.onespace.core.media

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.onespace.domain.news.repository.TtsEngine
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Short feedback sounds shared by every minigame (F-GAME-13). */
enum class GameSound { TAP, MERGE, CORRECT, WRONG, WIN, GAMEOVER }

/**
 * Game sound effects ride along the shared media state instead of fighting it
 * (case C9): each effect is a short sonification blip on its own SoundPool and
 * automatically ducks to [DUCKED_VOLUME] while music, video or TTS is audible,
 * so the game never overrides the primary voice.
 */
@Singleton
class GameSfx @Inject constructor(
    @ApplicationContext context: Context,
    private val media: MediaPlaybackCoordinator,
    private val tts: TtsEngine,
    preferences: SessionPreferencesRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids: Map<GameSound, Int> = mapOf(
        GameSound.TAP to pool.load(context, R.raw.game_tap, 1),
        GameSound.MERGE to pool.load(context, R.raw.game_merge, 1),
        GameSound.CORRECT to pool.load(context, R.raw.game_correct, 1),
        GameSound.WRONG to pool.load(context, R.raw.game_wrong, 1),
        GameSound.WIN to pool.load(context, R.raw.game_win, 1),
        GameSound.GAMEOVER to pool.load(context, R.raw.game_over, 1)
    )

    /** User's game volume from Settings, 0 (off) to 1 (full). */
    private var volume = 1f

    init {
        scope.launch {
            preferences.preferences.collectLatest { volume = it.gameSoundVolume }
        }
    }

    /** Plays [sound] once; no-ops when the user muted game sounds in Settings. */
    fun play(sound: GameSound) {
        if (volume <= 0f) return
        val id = ids[sound] ?: return
        val duck = media.music.value.playing || media.video.value.audible || tts.state.value.isPlaying
        val level = volume * if (duck) DUCKED_VOLUME else 1f
        pool.play(id, level, level, 1, 0, 1f)
    }

    private companion object {
        /** Music keeps the floor: effects duck to a quiet layer, they never silence it. */
        const val DUCKED_VOLUME = 0.25f
    }
}
