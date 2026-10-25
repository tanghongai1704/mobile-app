package com.example.onespace.feature.games.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.games.model.Game

@Composable
internal fun GamesScreen(games: List<Game>, onOpen: (String) -> Unit) {
    FeaturePage {
        items(games, key = { it.id }) { game ->
            ContentCard(game.title, game.description,
                if (game.id == "g1") Icons.Filled.GridView else Icons.Filled.SelectAll,
                { onOpen(game.id) })
        }
    }
}

/** Game detail: score status, board, pause control. Identity is in the top bar ("← Games"). */
@Composable
internal fun GameScreen(game: Game?, paused: Boolean, onPause: () -> Unit) {
    if (game == null) { Text("Chưa có game này"); return }
    val is2048 = game.id == "g1"
    val cells = if (is2048) listOf("2", "", "4", "", "", "8", "", "2", "16", "", "4", "", "", "2", "", "")
                else listOf("×", "", "○", "", "×", "", "○", "", "")
    val columns = if (is2048) 4 else 3
    val cellColor = if (is2048) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.secondaryContainer
    val cellTextColor = if (is2048) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSecondaryContainer
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(game.title, style = MaterialTheme.typography.headlineSmall)
            Text("Điểm cao · " + game.bestScore, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.widthIn(max = 320.dp).fillMaxWidth().background(
                MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            cells.chunked(columns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { value ->
                        Box(Modifier.weight(1f).aspectRatio(1f)
                            .background(if (value.isEmpty())
                                MaterialTheme.colorScheme.surfaceContainerHighest
                                else cellColor,
                                MaterialTheme.shapes.small),
                            contentAlignment = Alignment.Center) {
                            Text(if (paused) "·" else value, style = MaterialTheme.typography.headlineSmall,
                                color = if (value.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant else cellTextColor)
                        }
                    }
                }
            }
        }
        Button(onClick = onPause) { Text(if (paused) "Chơi bản demo" else "Tạm dừng bản demo") }
        if (paused) PrototypeNote("Đang tạm dừng · Tiếp tục khi bạn sẵn sàng.")
    }
}

/** Guess the Song: play 5 seconds, pick the correct title from 4 options. */
@Composable
internal fun GuessSongScreen(
    state: com.example.onespace.domain.games.model.GuessSongState,
    onStart: () -> Unit,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Đoán bài hát", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text("Nghe 5 giây và chọn tên bài hát đúng",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Score
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Điểm: ${state.score}", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text("Ván: ${state.round}", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when {
            state.question == null -> {
                // Start screen
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("Bắt đầu chơi")
                }
            }
            !state.hasAnswered -> {
                // Playing state
                Text(
                    if (state.isPlaying) "Đang phát..." else "Chọn đáp án",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                // Options
                val currentQuestion = state.question ?: return@Column
                currentQuestion.options.forEach { option ->
                    OutlinedButton(
                        onClick = { onAnswer(option) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(option)
                    }
                }
            }
            else -> {
                // Answered state
                val question = state.question ?: return@Column
                val isCorrect = state.isCorrect == true
                Text(
                    if (isCorrect) "Chính xác! +10 điểm" else "Sai rồi!",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isCorrect) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Đáp án: ${question.trackTitle} — ${question.artist}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text("Ván tiếp theo")
                }
            }
        }
    }
}
