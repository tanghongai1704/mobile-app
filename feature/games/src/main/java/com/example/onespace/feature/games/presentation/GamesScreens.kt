package com.example.onespace.feature.games.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.games.model.Game

@Composable
internal fun GamesScreen(games: List<Game>, onOpen: (String) -> Unit) {
    FeaturePage("Play / recharge", "A small escape.", "Offline favourites. No pressure, just play.") {
        item { Artwork("MAKE SPACE FOR PLAY", Modifier.fillMaxWidth().height(160.dp)) }
        items(games, key = { it.id }) { game ->
            ContentCard(game.title, game.description, if (game.id == "g1") "2048" else "×○", { onOpen(game.id) })
        }
        item { PrototypeNote("Game boards are previews. Rules, scoring and persistent saves come next.") }
    }
}

@Composable
internal fun GameScreen(game: Game?, paused: Boolean, onPause: () -> Unit) {
    if (game == null) { Text("Game unavailable"); return }
    val is2048 = game.id == "g1"
    val cells = if (is2048) listOf("2", "", "4", "", "", "8", "", "2", "16", "", "4", "", "", "2", "", "")
                else listOf("×", "", "○", "", "×", "", "○", "", "")
    val columns = if (is2048) 4 else 3
    FeaturePage("Muted game / preview", game.title, "Best · " + game.bestScore + "  /  Sound off") {
        item {
            Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(20.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                cells.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { value ->
                            Box(Modifier.weight(1f).aspectRatio(1f)
                                .background(Mint.copy(alpha = if (value.isEmpty()) .04f else .18f), RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center) {
                                Text(if (paused) "·" else value, style = MaterialTheme.typography.headlineSmall, color = Mint)
                            }
                        }
                    }
                }
            }
        }
        item { Button(onClick = onPause) { Text(if (paused) "Resume preview" else "Pause preview") } }
        item { PrototypeNote(if (paused) "Paused · Return when you are ready." else "Board preview · Gameplay will be implemented in the next phase.") }
    }
}
