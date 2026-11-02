package com.example.onespace.feature.games.presentation

import com.example.onespace.core.designsystem.tr
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.onespace.core.designsystem.AppCard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.domain.games.engine.Direction
import com.example.onespace.domain.games.engine.Game2048State
import kotlin.math.abs

private const val SWIPE_THRESHOLD_PX = 24f

/** 2048: swipe anywhere on the board to slide and merge tiles. */
@Composable
internal fun Game2048Screen(
    state: Game2048State,
    moves: Int,
    best: Int,
    outcome: Game2048Outcome?,
    onMove: (Direction) -> Unit,
    onRestart: () -> Unit,
    onContinue: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(moves) { if (moves > 0) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    LaunchedEffect(outcome) {
        outcome?.let {
            haptic.performHapticFeedback(if (it.isOver && it.pointsEarned == 0) HapticFeedbackType.Reject else HapticFeedbackType.Confirm)
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ScoreChip(tr("Điểm", "Points"), state.score, Modifier.weight(1f))
            ScoreChip(tr("Kỷ lục", "Record"), maxOf(best, state.score), Modifier.weight(1f))
            ScoreChip(tr("Ô cao nhất", "Best tile"), state.bestTile, Modifier.weight(1f))
        }
        Text(
            tr("Vuốt để trượt các ô. Hai ô giống nhau gộp lại, chạm mốc 2048.", "Swipe to slide the tiles. Matching tiles merge; reach 2048."),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Board(state, moves, onMove)
        when {
            outcome?.isOver == true -> OutcomeCard(
                title = tr("Hết nước đi", "No moves left"),
                body = buildString {
                    append(tr("Điểm của bạn: ${outcome.score}", "Your score: ${outcome.score}"))
                    if (outcome.newRecord) append(tr(" · Kỷ lục mới!", " · New record!"))
                    if (outcome.pointsEarned > 0) append(tr("\n+${outcome.pointsEarned} điểm thưởng", "\n+${outcome.pointsEarned} bonus points"))
                },
                primary = tr("Chơi lại", "Play again") to onRestart
            )
            outcome?.reachedTarget == true -> OutcomeCard(
                title = tr("Bạn chạm 2048!", "You reached 2048!"),
                body = tr("+${outcome.pointsEarned} điểm thưởng. Muốn thử đi xa hơn không?", "+${outcome.pointsEarned} bonus points. Want to go further?"),
                primary = tr("Chơi tiếp", "Keep playing") to onContinue,
                secondary = tr("Ván mới", "New game") to onRestart
            )
            else -> OutlinedButton(onClick = onRestart) { Text(tr("Ván mới", "New game")) }
        }
    }
}

@Composable
private fun ScoreChip(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier.background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.medium)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Board(state: Game2048State, moves: Int, onMove: (Direction) -> Unit) {
    val boardDescription = tr("Bảng 2048. Điểm ${state.score}. Ô cao nhất ${state.bestTile}.", "2048 board. Score ${state.score}. Best tile ${state.bestTile}.")
    val slideLeft = tr("Trượt sang trái", "Slide left")
    val slideRight = tr("Trượt sang phải", "Slide right")
    val slideUp = tr("Trượt lên", "Slide up")
    val slideDown = tr("Trượt xuống", "Slide down")
    Column(
        Modifier.widthIn(max = 360.dp).fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
            .padding(10.dp)
            .testTag("board2048")
            .semantics {
                contentDescription = boardDescription
                // Touch-exploration users cannot swipe the board, so each direction is an action.
                customActions = listOf(
                    CustomAccessibilityAction(slideLeft) { onMove(Direction.LEFT); true },
                    CustomAccessibilityAction(slideRight) { onMove(Direction.RIGHT); true },
                    CustomAccessibilityAction(slideUp) { onMove(Direction.UP); true },
                    CustomAccessibilityAction(slideDown) { onMove(Direction.DOWN); true }
                )
            }
            .pointerInput(Unit) {
                var dx = 0f
                var dy = 0f
                detectDragGestures(
                    onDragStart = { dx = 0f; dy = 0f },
                    onDrag = { change, amount -> change.consume(); dx += amount.x; dy += amount.y },
                    onDragEnd = {
                        if (maxOf(abs(dx), abs(dy)) >= SWIPE_THRESHOLD_PX) {
                            onMove(
                                if (abs(dx) > abs(dy)) { if (dx > 0) Direction.RIGHT else Direction.LEFT }
                                else { if (dy > 0) Direction.DOWN else Direction.UP }
                            )
                        }
                    }
                )
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        state.tiles.chunked(state.size).forEachIndexed { row, line ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                line.forEachIndexed { col, value ->
                    val index = row * state.size + col
                    Tile(
                        value = value,
                        stamp = moves,
                        fresh = index == state.spawnedAt,
                        merged = index in state.mergedAt,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun Tile(value: Int, stamp: Int, fresh: Boolean, merged: Boolean, modifier: Modifier) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(stamp, value) {
        if (fresh || merged) {
            scale.snapTo(if (fresh) 0.4f else 1.18f)
            scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
        } else scale.snapTo(1f)
    }
    val (fill, ink) = tilePalette(value)
    Box(
        modifier.aspectRatio(1f).scale(scale.value).background(fill, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        if (value != 0) {
            Text(
                value.toString(),
                style = if (value >= 1024) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = ink
            )
        }
    }
}

/** Tile colours are interpolated along theme roles so they follow light/dark and the brand colour. */
@Composable
private fun tilePalette(value: Int): Pair<Color, Color> {
    val cs = MaterialTheme.colorScheme
    if (value == 0) return cs.surfaceContainerHighest to cs.onSurface
    val level = Integer.numberOfTrailingZeros(value).coerceIn(1, 11) // 2 -> 1 ... 2048 -> 11
    val ramp = listOf(cs.secondaryContainer, cs.primaryContainer, cs.primary, cs.tertiary)
    val position = (level - 1) / 10f * (ramp.size - 1)
    val from = position.toInt().coerceAtMost(ramp.size - 2)
    val fill = lerp(ramp[from], ramp[from + 1], position - from)
    val ink = if (level <= 5) cs.onSecondaryContainer else cs.onPrimary
    return fill to ink
}

@Composable
private fun OutcomeCard(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null
) {
    AppCard(
        Modifier.fillMaxWidth().widthIn(max = 360.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = primary.second) { Text(primary.first) }
                secondary?.let { OutlinedButton(onClick = it.second) { Text(it.first) } }
            }
        }
    }
}
