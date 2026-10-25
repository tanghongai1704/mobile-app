package com.example.onespace.feature.games.navigation

import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.feature.games.presentation.*
fun NavGraphBuilder.gamesGraph(
    session: SessionCoordinator,
    getGraphEntry: () -> NavBackStackEntry,
    onOpenGame: (String) -> Unit
) {
    navigation<GamesRoute.Graph>(startDestination = GamesRoute.Feed) {
        composable<GamesRoute.Feed> { entry ->
            val owner = remember(entry) { getGraphEntry() }
            val vm: GamesViewModel = hiltViewModel(owner)
            val games by vm.uiState.collectAsStateWithLifecycle()
            var showLeaderboard by remember { mutableStateOf(false) }
            var gameFilter by remember { mutableStateOf("all") }
            var leaderboardPeriod by remember { mutableStateOf("week") }
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
            com.example.onespace.core.designsystem.FeaturePage {
                if (!showLeaderboard) {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FilterChip(selected = gameFilter == "all", onClick = { gameFilter = "all" }, label = { Text("Tất cả") }) }
                            item { FilterChip(selected = gameFilter == "suy_luan", onClick = { gameFilter = "suy_luan" }, label = { Text("Suy luận") }) }
                            item { FilterChip(selected = gameFilter == "am_nhac", onClick = { gameFilter = "am_nhac" }, label = { Text("Âm nhạc") }) }
                            item { FilterChip(selected = gameFilter == "hanh_dong", onClick = { gameFilter = "hanh_dong" }, label = { Text("Hành động") }) }
                        }
                    }
                }
                if (showLeaderboard) {
                    item {
                        val total by vm.totalPoints.collectAsStateWithLifecycle()
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = leaderboardPeriod == "week",
                                onClick = { leaderboardPeriod = "week" },
                                label = { Text("Tuần này") }
                            )
                            FilterChip(
                                selected = leaderboardPeriod == "month",
                                onClick = { leaderboardPeriod = "month" },
                                label = { Text("Tháng này") }
                            )
                        }
                    }
                    item {
                        val total by vm.totalPoints.collectAsStateWithLifecycle()
                        if (leaderboardPeriod == "week") {
                            LeaderboardList(vm.weeklyBoard, total)
                        } else {
                            LeaderboardList(vm.monthlyBoard, total)
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                } else {
                    val filtered = when (gameFilter) {
                        "suy_luan" -> games.items.filter { it.id in listOf("g1", "g2", "g4") }
                        "am_nhac" -> games.items.filter { it.id in listOf("g3") }
                        "hanh_dong" -> games.items.filter { it.id in listOf("g5") }
                        else -> games.items
                    }
                    items(filtered, key = { it.id }) { game ->
                        com.example.onespace.core.designsystem.ContentCard(
                            title = game.title,
                            subtitle = game.description,
                            icon = gameIcon(game.id),
                            onClick = { onOpenGame(game.id) }
                        )
                    }
                }
            }
            // F-GAME-07: FAB bottom-right opens the leaderboard as a
            // dedicated view — chips and game cards disappear entirely.
            if (!showLeaderboard) {
                FloatingActionButton(
                    onClick = { showLeaderboard = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Filled.EmojiEvents,
                        contentDescription = "Xếp hạng",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            } else {
                FloatingActionButton(
                    onClick = { showLeaderboard = false },
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.Default.Close,
                        contentDescription = "Đóng xếp hạng",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            } // Box
        }
        composable<GamesRoute.Game> { entry ->
            val id = entry.toRoute<GamesRoute.Game>().id
            // F-GAME-08: while a video is playing it stays on top and the
            // game runs in the bottom half of the screen.
            val playback by session.video.collectAsStateWithLifecycle()
            val videoActive = playback.videoId != null
            val awardVm: PlayAwardViewModel = hiltViewModel()
            Column(Modifier.fillMaxSize()) {
                if (videoActive) {
                    com.example.onespace.core.media.VideoSurface(
                        awardVm.player,
                        Modifier.fillMaxWidth().height(170.dp),
                        { session.seekVideoBy(10_000) }
                    )
                }
                Box(Modifier.weight(1f)) {
                    when (id) {
                        "g1" -> Game2048DemoScreen()
                        "g2" -> {
                            TicTacToeScreen(onWin = { awardVm.awardGame("thang_tic_tac_toe", 20) })
                        }
                        "g3" -> {
                            val vm: GuessSongViewModel = hiltViewModel()
                            val state by vm.state.collectAsStateWithLifecycle()
                            GuessSongScreen(
                                state = state,
                                onStart = vm::startNewRound,
                                onAnswer = vm::answer,
                                onNext = vm::nextRound
                            )
                        }
                        "g4" -> {
                            val vm: NewsQuizViewModel = hiltViewModel()
                            LaunchedEffect(Unit) { vm.load() }
                            val questions by vm.questions.collectAsStateWithLifecycle()
                            NewsQuizScreen(
                                questions = questions,
                                onAnswer = { if (it) vm.award() }
                            )
                        }
                        "g5" -> {
                            SnakeScreen(onGameOver = { score ->
                                if (score > 0) awardVm.awardGame("ran_san_moi", score * 2)
                            })
                        }
                        else -> Text("Không tìm thấy game")
                    }
                }
            }
        }
    }
}

/** Unique icon per game so the hub reads at a glance. */
private fun gameIcon(id: String): androidx.compose.ui.graphics.vector.ImageVector = when (id) {
    "g1" -> androidx.compose.material.icons.Icons.Filled.GridView
    "g2" -> androidx.compose.material.icons.Icons.Filled.Apps
    "g3" -> androidx.compose.material.icons.Icons.Filled.MusicNote
    "g4" -> androidx.compose.material.icons.Icons.Filled.Quiz
    "g5" -> androidx.compose.material.icons.Icons.Filled.Pets
    else -> androidx.compose.material.icons.Icons.Filled.SportsEsports
}

@androidx.compose.runtime.Composable
private fun Game2048DemoScreen() {
    com.example.onespace.feature.games.presentation.GameScreen(
        game = com.example.onespace.domain.games.model.Game("g1", "2048", "Tập trung chút, số lớn hơn.", 2048),
        paused = false,
        onPause = {}
    )
}

/** F-GAME-07: one leaderboard section with medals and a standing-out "Bạn". */
@Composable
private fun LeaderboardList(board: List<Pair<String, Int>>, myPoints: Int) {
    val rows = (board + ("Bạn" to myPoints)).sortedByDescending { it.second }
    Column(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { idx, (name, pts) ->
            val isMe = name == "Bạn"
            val medal = when (idx) {
                0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> null
            }
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                border = if (isMe) androidx.compose.foundation.BorderStroke(
                    2.dp, MaterialTheme.colorScheme.primary
                ) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Rank: medal for top 3, number for the rest.
                    if (medal != null) {
                        Text(medal, style = MaterialTheme.typography.titleLarge)
                    } else {
                        Text(
                            "${idx + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(28.dp)
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isMe) "Bạn" else name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (isMe) {
                            Text(
                                "Điểm của bạn",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Text(
                        "$pts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isMe) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "điểm",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
