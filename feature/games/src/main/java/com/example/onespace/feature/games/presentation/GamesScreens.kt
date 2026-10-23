package com.example.onespace.feature.games.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text(game.title, style = MaterialTheme.typography.headlineSmall)
            Text("Điểm cao · " + game.bestScore, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Column(Modifier.fillMaxWidth().background(
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
            item { Button(onClick = onPause) { Text(if (paused) "Resume preview" else "Pause preview") } }
            item { PrototypeNote(if (paused) "Paused · Return when you are ready." else "Board preview · Gameplay will be implemented in the next phase.") }
        }
        Button(onClick = onPause) { Text(if (paused) "Chơi bản demo" else "Tạm dừng bản demo") }
        if (paused) PrototypeNote("Đang tạm dừng · Tiếp tục khi bạn sẵn sàng.")
    }
}
