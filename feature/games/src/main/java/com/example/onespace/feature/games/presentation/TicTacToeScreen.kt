package com.example.onespace.feature.games.presentation

import com.example.onespace.core.designsystem.t
import com.example.onespace.core.designsystem.tr
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.domain.games.engine.Difficulty
import com.example.onespace.domain.games.engine.Mark

/** Tic-Tac-Toe against the computer (3 levels) or a friend on the same phone. */
@Composable
internal fun TicTacToeScreen(
    ui: TicTacToeUi,
    bestStreak: Int,
    onOpponent: (Opponent) -> Unit,
    onPlay: (Int) -> Unit,
    onNewRound: () -> Unit
) {
    val vsComputer = ui.opponent is Opponent.Computer
    val haptic = LocalHapticFeedback.current
    val placed = ui.board.cells.count { it != null }
    LaunchedEffect(placed) { if (placed > 0) haptic.performHapticFeedback(HapticFeedbackType.SegmentTick) }
    LaunchedEffect(ui.board.winner, ui.board.isFull) {
        if (ui.finished) haptic.performHapticFeedback(
            if (ui.board.winner == Mark.O && vsComputer) HapticFeedbackType.Reject else HapticFeedbackType.Confirm
        )
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Difficulty.entries.forEach { level ->
                FilterChip(
                    selected = (ui.opponent as? Opponent.Computer)?.level == level,
                    onClick = { onOpponent(Opponent.Computer(level)) },
                    label = { Text(tr("Máy · ${level.label}", "CPU · ${level.label.t()}")) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !vsComputer,
                onClick = { onOpponent(Opponent.Friend) },
                label = { Text(tr("2 người", "2 players")) }
            )
        }
        Text(
            statusText(ui, vsComputer),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Board(ui, onPlay)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Stat(tr("Thắng", "Wins"), ui.wins)
            Stat(tr("Hòa", "Draws"), ui.draws)
            Stat(tr("Thua", "Losses"), ui.losses)
            if (vsComputer) Stat(tr("Chuỗi thắng", "Win streak"), ui.winStreak)
        }
        if (vsComputer && bestStreak > 0) {
            Text(
                tr("Chuỗi thắng dài nhất: $bestStreak", "Longest win streak: $bestStreak"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        OutlinedButton(onClick = onNewRound) { Text(if (ui.finished) tr("Ván tiếp theo", "Next round") else tr("Ván mới", "New game")) }
    }
}

@Composable
private fun statusText(ui: TicTacToeUi, vsComputer: Boolean): String {
    val winner = ui.board.winner
    return when {
        winner == Mark.X && vsComputer ->
            if (ui.lastReward > 0) tr("Bạn thắng! +${ui.lastReward} điểm", "You win! +${ui.lastReward} points") else tr("Bạn thắng!", "You win!")
        winner == Mark.O && vsComputer -> tr("Máy thắng. Thử lại nhé!", "The computer wins. Try again!")
        winner != null -> tr("Người chơi ${winner.name} thắng!", "Player ${winner.name} wins!")
        ui.board.isFull -> tr("Hòa!", "It's a draw!")
        ui.thinking -> tr("Máy đang nghĩ…", "The computer is thinking…")
        vsComputer -> tr("Lượt của bạn (X)", "Your turn (X)")
        else -> tr("Lượt của người chơi ${ui.turn.name}", "Player ${ui.turn.name}'s turn")
    }
}

@Composable
private fun Stat(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Board(ui: TicTacToeUi, onPlay: (Int) -> Unit) {
    val line = ui.board.winningLine.orEmpty()
    Column(
        Modifier.widthIn(max = 320.dp).fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ui.board.cells.chunked(3).forEachIndexed { row, cells ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.forEachIndexed { col, mark ->
                    val index = row * 3 + col
                    val onLine = index in line
                    Box(
                        Modifier.weight(1f).aspectRatio(1f)
                            .background(
                                when {
                                    onLine -> MaterialTheme.colorScheme.primaryContainer
                                    mark == null -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    else -> MaterialTheme.colorScheme.secondaryContainer
                                },
                                MaterialTheme.shapes.small
                            )
                            .testTag("ttt-cell-$index")
                            .clickable(enabled = mark == null && !ui.finished && !ui.thinking) { onPlay(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (mark != null) {
                            Text(
                                mark.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (mark == Mark.X) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
        }
    }
}
