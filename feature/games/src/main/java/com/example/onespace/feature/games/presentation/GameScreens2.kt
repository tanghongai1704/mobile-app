package com.example.onespace.feature.games.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.SectionTitle
import kotlinx.coroutines.delay

private data class Pt(val x: Int, val y: Int)

/**
 * F-GAME-05/F-GAME-09: Tic-Tac-Toe two players on one device; a win earns
 * 20 XP through the shared point repository.
 */
@Composable
internal fun TicTacToeScreen(onWin: () -> Unit = {}) {
    var board by remember { mutableStateOf(List(9) { "" }) }
    var turn by remember { mutableStateOf("X") }
    var winner by remember { mutableStateOf<String?>(null) }
    var awarded by remember { mutableStateOf(false) }
    val lines = listOf(
        Triple(0, 1, 2), Triple(3, 4, 5), Triple(6, 7, 8),
        Triple(0, 3, 6), Triple(1, 4, 7), Triple(2, 5, 8),
        Triple(0, 4, 8), Triple(2, 4, 6)
    )
    fun checkWin(b: List<String>): String? {
        lines.forEach { (a, c, d) ->
            if (b[a].isNotEmpty() && b[a] == b[c] && b[a] == b[d]) return b[a]
        }
        return null
    }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tic-Tac-Toe", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            when {
                winner != null -> "Người chơi $winner thắng!"
                board.all { it.isNotEmpty() } -> "Hòa!"
                else -> "Lượt của người chơi $turn"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Column(
            Modifier.widthIn(max = 320.dp).background(
                MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large
            ).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            board.chunked(3).forEachIndexed { row, line ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    line.forEachIndexed { col, value ->
                        val idx = row * 3 + col
                        Box(
                            Modifier.weight(1f).aspectRatio(1f)
                                .background(
                                    if (value.isEmpty()) MaterialTheme.colorScheme.surfaceContainerHighest
                                    else MaterialTheme.colorScheme.secondaryContainer,
                                    MaterialTheme.shapes.small
                                )
                                .clickable {
                                    if (winner == null && board[idx].isEmpty()) {
                                        board = board.toMutableList().also { it[idx] = turn }
                                        val w = checkWin(board)
                                        if (w != null) {
                                            winner = w
                                            if (!awarded) { awarded = true; onWin() }
                                        } else if (board.all { it.isNotEmpty() }) {
                                            // draw
                                        } else turn = if (turn == "X") "O" else "X"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(value, style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = {
            board = List(9) { "" }; turn = "X"; winner = null; awarded = false
        }) { Text("Chơi lại") }
    }
}

/**
 * F-GAME-13: Snake — swipe to steer, eat to grow, game over on collision.
 */
@Composable
internal fun SnakeScreen(onGameOver: (Int) -> Unit = {}) {
    var snake by remember { mutableStateOf(listOf(Pt(5, 8), Pt(4, 8), Pt(3, 8))) }
    var food by remember { mutableStateOf(Pt(8, 4)) }
    var dir by remember { mutableStateOf(Pt(1, 0)) }
    var score by remember { mutableIntStateOf(0) }
    var alive by remember { mutableStateOf(true) }
    var started by remember { mutableStateOf(false) }
    var reported by remember { mutableStateOf(false) }

    LaunchedEffect(alive, started) {
        while (alive && started) {
            delay(280)
            val head = snake.first()
            val next = Pt((head.x + dir.x + 12) % 12, (head.y + dir.y + 12) % 12)
            if (snake.dropLast(1).contains(next)) {
                alive = false
                if (!reported) { reported = true; onGameOver(score) }
            } else {
                snake = listOf(next) + if (next == food) {
                    score += 1
                    food = Pt((0..11).random(), (0..11).random())
                    snake
                } else snake.dropLast(1)
            }
        }
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Rắn săn mồi", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Điểm: $score", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(MaterialTheme.colorScheme.surfaceContainer, MaterialTheme.shapes.large)
                .pointerInput(alive, started) {
                    detectDragGestures { _, dragAmount ->
                        if (abs(dragAmount.x) > abs(dragAmount.y)) {
                            val nd = Pt(if (dragAmount.x > 0) 1 else -1, 0)
                            if (nd.x != -dir.x) dir = nd
                        } else {
                            val nd = Pt(0, if (dragAmount.y > 0) 1 else -1)
                            if (nd.y != -dir.y) dir = nd
                        }
                    }
                }
        ) {
            val cell = size.width / 12
            drawRoundRect(
                color = Color(0xFFE53935),
                topLeft = Offset(food.x * cell + 2, food.y * cell + 2),
                size = androidx.compose.ui.geometry.Size(cell - 4, cell - 4),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            snake.forEachIndexed { i, p ->
                drawRoundRect(
                    color = if (i == 0) Color(0xFF00695C) else Color(0xFF26A69A),
                    topLeft = Offset(p.x * cell + 2, p.y * cell + 2),
                    size = androidx.compose.ui.geometry.Size(cell - 4, cell - 4),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
        }
        when {
            !started -> Button(onClick = { started = true }) { Text("Bắt đầu") }
            !alive -> Button(onClick = {
                snake = listOf(Pt(5, 8), Pt(4, 8), Pt(3, 8)); dir = Pt(1, 0)
                score = 0; alive = true; reported = false
            }) { Text("Chơi lại") }
            else -> Text("Vuốt trên bàn chơi để đổi hướng",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun abs(v: Float) = if (v < 0) -v else v

/** F-GAME-03: the news quiz as a game — questions come from the article catalog. */
@Composable
internal fun NewsQuizScreen(
    questions: List<com.example.onespace.domain.news.model.QuizQuestion>,
    onAnswer: (Boolean) -> Unit = {},
    onRestart: () -> Unit = {}
) {
    var index by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Quiz tin tức", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (questions.isEmpty()) {
            Text("Chưa có câu hỏi nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        if (index >= questions.size) {
            Text("Hoàn thành!", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text("Đúng $score/${questions.size} câu",
                style = MaterialTheme.typography.titleMedium)
            Button(onClick = { index = 0; score = 0; picked = null; onRestart() }) {
                Text("Chơi lại")
            }
            return@Column
        }
        Text("Câu ${index + 1}/${questions.size} · Điểm $score",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        val q = questions[index]
        Text(q.question, style = MaterialTheme.typography.titleMedium)
        q.options.forEachIndexed { oi, option ->
            val chosen = picked == oi
            val isCorrect = picked != null && oi == q.correctIndex
            OutlinedButton(
                onClick = {
                    if (picked == null) {
                        picked = oi
                        val ok = oi == q.correctIndex
                        if (ok) { score += 1; onAnswer(true) }
                    }
                },
                colors = if (picked != null && isCorrect) ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) else ButtonDefaults.outlinedButtonColors(),
                modifier = Modifier.fillMaxWidth()
            ) { Text(option) }
        }
        if (picked != null) {
            Button(onClick = { index += 1; picked = null }, modifier = Modifier.fillMaxWidth()) {
                Text("Câu tiếp theo")
            }
        }
    }
}

/** F-GAME-07: weekly and monthly boards, "Bạn" carries the real point total. */
@Composable
internal fun LeaderboardScreen(
    totalPoints: Int,
    weekly: List<Pair<String, Int>>,
    monthly: List<Pair<String, Int>>
) {
    var mode by remember { mutableStateOf("week") }
    val board = if (mode == "week") weekly else monthly
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Bảng xếp hạng", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "week", onClick = { mode = "week" }, label = { Text("Tuần này") })
            FilterChip(selected = mode == "month", onClick = { mode = "month" }, label = { Text("Tháng này") })
        }
        val rows = (board + ("Bạn" to totalPoints)).sortedByDescending { it.second }
        rows.forEachIndexed { rank, (name, points) ->
            val isMe = name == "Bạn"
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${rank + 1}", style = MaterialTheme.typography.titleMedium,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(32.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f))
                    Text("$points điểm", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
