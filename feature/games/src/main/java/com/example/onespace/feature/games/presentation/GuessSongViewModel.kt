package com.example.onespace.feature.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.games.model.GuessSongQuestion
import com.example.onespace.domain.games.model.GuessSongState
import com.example.onespace.domain.games.usecase.StartGuessSong
import com.example.onespace.domain.music.repository.MusicRepository
import com.example.onespace.domain.session.repository.PointRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
internal class GuessSongViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val startGuessSong: StartGuessSong
) : ViewModel() {

    private val _state = MutableStateFlow(GuessSongState())
    val state = _state.asStateFlow()

    fun startNewRound() {
        viewModelScope.launch {
            val tracks = musicRepository.observeCatalog().first()
            val question = startGuessSong.generate(tracks)
            _state.value = GuessSongState(
                question = question,
                isPlaying = true,
                round = _state.value.round + 1,
                score = _state.value.score
            )
        }
    }

    fun answer(selected: String) {
        val question = _state.value.question ?: return
        viewModelScope.launch {
            val isCorrect = startGuessSong.answer(selected, question)
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
