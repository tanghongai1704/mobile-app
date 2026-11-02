package com.example.onespace.feature.games.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.onespace.domain.games.badge.RecordKeys
import com.example.onespace.domain.games.engine.Direction
import com.example.onespace.domain.games.engine.Game2048
import com.example.onespace.domain.games.engine.Game2048State
import com.example.onespace.domain.games.usecase.ObserveBestScore
import com.example.onespace.domain.games.usecase.SubmitGameScore
import com.example.onespace.domain.session.model.MissionType
import com.example.onespace.domain.session.repository.PointRepository
import com.example.onespace.domain.session.usecase.TrackMissionAction
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** What the player earned when a 2048 round ended or hit the target. */
internal data class Game2048Outcome(
    val reachedTarget: Boolean,
    val isOver: Boolean,
    val score: Int,
    val pointsEarned: Int,
    val newRecord: Boolean
)

/** Game id of 2048 in the catalog and in the records store. */
internal const val GAME_2048_ID = "g1"

@OptIn(FlowPreview::class)
@HiltViewModel
internal class Game2048ViewModel @Inject constructor(
    private val points: PointRepository,
    private val trackMissionAction: TrackMissionAction,
    private val submitScore: SubmitGameScore,
    private val challenge: ChallengeReporter,
    observeBest: ObserveBestScore
) : ViewModel() {
    private val _state = MutableStateFlow(Game2048.newGame())
    val state = _state.asStateFlow()

    /** Bumped on every accepted move so the UI can replay tile animations. */
    private val _moves = MutableStateFlow(0)
    val moves = _moves.asStateFlow()

    private val _outcome = MutableStateFlow<Game2048Outcome?>(null)
    val outcome = _outcome.asStateFlow()

    val best = observeBest(GAME_2048_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // Report the round's best score shortly after the player stops moving, so the daily
        // challenge fills up while playing instead of only when the round ends.
        viewModelScope.launch {
            _state.map { it.score to it.bestTile }.distinctUntilChanged().debounce(SCORE_REPORT_DELAY_MS)
                .collect { (score, tile) ->
                    if (score > 0) challenge.report(GAME_2048_ID, score)
                    submitScore(RecordKeys.GAME_2048_TILE, tile)
                }
        }
    }

    private var targetPaid = false
    private var overPaid = false

    fun move(direction: Direction) {
        if (_state.value.isOver || _outcome.value?.isOver == true) return
        val next = Game2048.move(_state.value, direction)
        if (next === _state.value) return
        _state.value = next
        _moves.value += 1
        when {
            next.isOver -> finish(next, over = true)
            next.reachedTarget && !targetPaid -> finish(next, over = false)
        }
    }

    fun restart() {
        _state.value = Game2048.newGame()
        _outcome.value = null
        targetPaid = false
        overPaid = false
    }

    /** Dismisses the "reached 2048" banner and keeps playing. */
    fun continuePlaying() {
        if (_outcome.value?.isOver == false) _outcome.value = null
    }

    private fun finish(state: Game2048State, over: Boolean) {
        if (over && overPaid) return
        if (over) overPaid = true else targetPaid = true
        viewModelScope.launch {
            val reward = if (over) state.score / 20 else TARGET_BONUS
            val record = if (over) submitScore(GAME_2048_ID, state.score) else false
            if (reward > 0) points.add("game_2048", reward)
            trackMissionAction(MissionType.PLAY)
            _outcome.value = Game2048Outcome(
                reachedTarget = state.reachedTarget,
                isOver = over,
                score = state.score,
                pointsEarned = reward,
                newRecord = record
            )
        }
    }

    private companion object {
        const val TARGET_BONUS = 100
        const val SCORE_REPORT_DELAY_MS = 800L
    }
}
