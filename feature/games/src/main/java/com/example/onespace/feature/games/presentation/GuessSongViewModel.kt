package com.example.onespace.feature.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.games.model.GuessSongQuestion
import com.example.onespace.domain.games.model.GuessSongState
import com.example.onespace.domain.games.usecase.StartGuessSong
import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.core.media.GameSfx
import com.example.onespace.core.media.GameSound
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
internal class GuessSongViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val startGuessSong: StartGuessSong,
    private val challenge: ChallengeReporter,
    private val submitScore: com.example.onespace.domain.games.usecase.SubmitGameScore,
    private val sfx: GameSfx
) : ViewModel() {

    private val _state = MutableStateFlow(GuessSongState())
    val state = _state.asStateFlow()

    /** Names of the playlists whose songs feed the questions; null = the whole library. */
    private var playlistTrackIds: Set<String>? = null

    fun startNewRound() {
        viewModelScope.launch {
            val tracks = musicRepository.observeCatalog().first()
            val pool = playlistTrackIds?.let { ids -> tracks.filter { it.id in ids } }
            val question = startGuessSong.generate(tracks, pool)
            _state.value = GuessSongState(
                question = question,
                isPlaying = true,
                round = _state.value.round + 1,
                score = _state.value.score
            )
        }
    }

    /** Starts a game whose questions come only from the songs of one of the user's playlists. */
    fun startFromPlaylist(trackIds: List<String>) {
        playlistTrackIds = trackIds.toSet()
        _state.value = GuessSongState()
        startNewRound()
    }

    /** Back to questions from the whole library. */
    fun useWholeLibrary() { playlistTrackIds = null }

    fun answer(selected: String) {
        val question = _state.value.question ?: return
        viewModelScope.launch {
            val isCorrect = startGuessSong.answer(selected, question)
            sfx.play(if (isCorrect) GameSound.CORRECT else GameSound.WRONG)
            if (isCorrect) {
                challenge.report("g3", 1)
                submitScore(com.example.onespace.domain.games.badge.RecordKeys.GUESS_SONG, _state.value.score + 10)
            }
            _state.value = _state.value.copy(
                hasAnswered = true,
                selectedOption = selected,
                isCorrect = isCorrect,
                isPlaying = false,
                score = if (isCorrect) _state.value.score + 10 else _state.value.score
            )
        }
    }

    fun nextRound() {
        startNewRound()
    }
}
