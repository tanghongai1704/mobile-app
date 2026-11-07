package com.example.onespace.feature.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.games.engine.Difficulty
import com.example.onespace.domain.games.engine.Mark
import com.example.onespace.domain.games.engine.TicTacToeAi
import com.example.onespace.domain.games.engine.TicTacToeBoard
import com.example.onespace.domain.games.usecase.ObserveBestScore
import com.example.onespace.domain.games.usecase.SubmitGameScore
import com.example.onespace.domain.session.model.MissionType
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.domain.session.usecase.TrackMissionAction
import com.example.onespace.core.media.GameSfx
import com.example.onespace.core.media.GameSound
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal const val GAME_TIC_TAC_TOE_ID = "g2"

/** Who plays O: the computer at a level, or a second person on the same phone. */
internal sealed interface Opponent {
    data class Computer(val level: Difficulty) : Opponent
    data object Friend : Opponent
}

internal data class TicTacToeUi(
    val board: TicTacToeBoard = TicTacToeBoard(),
    val opponent: Opponent = Opponent.Computer(Difficulty.MEDIUM),
    val turn: Mark = Mark.X,
    val thinking: Boolean = false,
    val wins: Int = 0,
    val draws: Int = 0,
    val losses: Int = 0,
    val winStreak: Int = 0,
    val lastReward: Int = 0
) {
    val finished: Boolean get() = board.isFinished
}

@HiltViewModel
internal class TicTacToeViewModel @Inject constructor(
    private val points: PointRepository,
    private val trackMissionAction: TrackMissionAction,
    private val submitScore: SubmitGameScore,
    private val challenge: ChallengeReporter,
    private val sfx: GameSfx,
    observeBest: ObserveBestScore
) : ViewModel() {
    private val _ui = MutableStateFlow(TicTacToeUi())
    val ui = _ui.asStateFlow()

    /** Best win streak against the computer. */
    val bestStreak = observeBest(GAME_TIC_TAC_TOE_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private var aiJob: Job? = null

    fun setOpponent(opponent: Opponent) {
        _ui.update { it.copy(opponent = opponent, winStreak = 0) }
        newRound()
    }

    fun newRound() {
        aiJob?.cancel()
        _ui.update { it.copy(board = TicTacToeBoard(), turn = Mark.X, thinking = false, lastReward = 0) }
    }

    fun play(index: Int) {
        val now = _ui.value
        if (now.finished || now.thinking || now.board.cells[index] != null) return
        val next = now.board.place(index, now.turn)
        _ui.value = now.copy(board = next, turn = now.turn.other)
        sfx.play(GameSound.TAP)
        if (next.isFinished) { settle(next); return }
        val opponent = now.opponent
        if (opponent is Opponent.Computer) {
            _ui.update { it.copy(thinking = true) }
            aiJob = viewModelScope.launch {
                delay(AI_THINK_MS)
                val board = _ui.value.board
                val move = TicTacToeAi.chooseMove(board, Mark.O, opponent.level)
                val after = board.place(move, Mark.O)
                _ui.update { it.copy(board = after, turn = Mark.X, thinking = false) }
                if (after.isFinished) settle(after)
            }
        }
    }

    private fun settle(board: TicTacToeBoard) {
        val opponent = _ui.value.opponent
        viewModelScope.launch {
            when {
                board.winner == null -> Unit // a draw stays quiet
                opponent is Opponent.Computer && board.winner == Mark.O ->
                    sfx.play(GameSound.WRONG) // the computer won the round
                else -> sfx.play(GameSound.WIN)
            }
            when {
                board.winner == null -> _ui.update { it.copy(draws = it.draws + 1) }
                opponent is Opponent.Friend -> _ui.update {
                    if (board.winner == Mark.X) it.copy(wins = it.wins + 1) else it.copy(losses = it.losses + 1)
                }
                board.winner == Mark.X -> {
                    val reward = (opponent as Opponent.Computer).level.reward
                    points.add("thang_tic_tac_toe", reward)
                    trackMissionAction(MissionType.PLAY)
                    val streak = _ui.value.winStreak + 1
                    submitScore(GAME_TIC_TAC_TOE_ID, streak)
                    challenge.report(GAME_TIC_TAC_TOE_ID, 1)
                    _ui.update { it.copy(wins = it.wins + 1, winStreak = streak, lastReward = reward) }
                }
                else -> _ui.update { it.copy(losses = it.losses + 1, winStreak = 0) }
            }
        }
    }

    private companion object { const val AI_THINK_MS = 450L }
}
