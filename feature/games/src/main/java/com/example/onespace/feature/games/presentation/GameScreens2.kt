package com.example.onespace.feature.games.presentation

import com.example.onespace.core.designsystem.tr
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.SectionTitle
import com.example.onespace.core.designsystem.oneSpaceColors
import kotlinx.coroutines.delay

private data class Pt(val x: Int, val y: Int)

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

    val foodColor = MaterialTheme.colorScheme.error
    val snakeHead = MaterialTheme.colorScheme.primary
    val snakeBody = MaterialTheme.colorScheme.secondary
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(tr("Rắn săn mồi", "Snake"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(tr("Điểm: $score", "Score: $score"), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
                color = foodColor,
                topLeft = Offset(food.x * cell + 2, food.y * cell + 2),
                size = androidx.compose.ui.geometry.Size(cell - 4, cell - 4),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
            )
            snake.forEachIndexed { i, p ->
                drawRoundRect(
                    color = if (i == 0) snakeHead else snakeBody,
                    topLeft = Offset(p.x * cell + 2, p.y * cell + 2),
                    size = androidx.compose.ui.geometry.Size(cell - 4, cell - 4),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                )
            }
        }
        when {
            !started -> Button(onClick = { started = true }) { Text(tr("Bắt đầu", "Start")) }
            !alive -> Button(onClick = {
                snake = listOf(Pt(5, 8), Pt(4, 8), Pt(3, 8)); dir = Pt(1, 0)
                score = 0; alive = true; reported = false
            }) { Text(tr("Chơi lại", "Play again")) }
            else -> Text(tr("Vuốt trên bàn chơi để đổi hướng", "Swipe on the board to turn"),
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
        Text(tr("Quiz tin tức", "News quiz"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        if (questions.isEmpty()) {
            Text(tr("Chưa có câu hỏi nào.", "No questions yet."), color = MaterialTheme.colorScheme.onSurfaceVariant)
            return@Column
        }
        if (index >= questions.size) {
            Text(tr("Hoàn thành!", "Done!"), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(tr("Đúng $score/${questions.size} câu", "$score/${questions.size} correct"),
                style = MaterialTheme.typography.titleMedium)
            Button(onClick = { index = 0; score = 0; picked = null; onRestart() }) {
                Text(tr("Chơi lại", "Play again"))
            }
            return@Column
        }
        Text(tr("Câu ${index + 1}/${questions.size} · Điểm $score", "Question ${index + 1}/${questions.size} · Score $score"),
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
                Text(tr("Câu tiếp theo", "Next question"))
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
        Text(tr("Bảng xếp hạng", "Leaderboard"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = mode == "week", onClick = { mode = "week" }, label = { Text(tr("Tuần này", "This week")) })
            FilterChip(selected = mode == "month", onClick = { mode = "month" }, label = { Text(tr("Tháng này", "This month")) })
        }
        val me = tr("Bạn", "You")
        val rows = (board + (me to totalPoints)).sortedByDescending { it.second }
        rows.forEachIndexed { rank, (name, points) ->
            val isMe = name == me
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
                    Text(tr("$points điểm", "$points points"), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

private data class TileNote(val lane: Int, val y: Float, val id: Int)

/**
 * F-GAME-13: Beat Tiles — four lanes of falling notes tapped on the beat.
 * Music from the dock keeps playing alongside (case C9); the notes give
 * the rhythm visually so the game is playable with or without a song.
 */
@Composable
internal fun BeatTilesScreen(onGameOver: (Int) -> Unit = {}) {
    var notes by remember { mutableStateOf(listOf<TileNote>()) }
    var running by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    var misses by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    var nextId by remember { mutableIntStateOf(0) }
    var reported by remember { mutableStateOf(false) }

    fun start() {
        notes = emptyList(); score = 0; combo = 0; bestCombo = 0
        misses = 0; tick = 0; reported = false; running = true
    }

    // 30 fps loop: spawn on a beat, let the tiles fall, count the pass-overs.
    LaunchedEffect(running) {
        while (running) {
            delay(33)
            tick += 1
            val interval = maxOf(9, 18 - tick / 400)
            if (tick % interval == 0) {
                val lane = (tick * 7 + nextId * 3) % 4
                notes = notes + TileNote(lane, 0f, nextId)
                nextId += 1
            }
            notes = notes.map { it.copy(y = it.y + 0.020f) }
            val passed = notes.count { it.y >= 1f }
            if (passed > 0) {
                misses += passed
                combo = 0
                notes = notes.filter { it.y < 1f }
            }
            if (misses >= 3) running = false
            if (tick >= 30 * 90) running = false
        }
    }

    LaunchedEffect(running, score) {
        if (!running && !reported && score > 0) {
            reported = true
            onGameOver(score)
        }
    }

    fun tapLane(lane: Int) {
        if (!running) return
        val hit = notes.filter { it.lane == lane && it.y in 0.72f..1f }
            .minByOrNull { it.y }
        if (hit != null) {
            combo += 1
            bestCombo = maxOf(bestCombo, combo)
            score += 10 + minOf(combo, 20)
            notes = notes.filter { it.id != hit.id }
        } else {
            combo = 0
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Score header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(tr("Điểm: $score", "Score: $score"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (combo > 0) "Combo x$combo" else tr("Trượt $misses/3", "Miss $misses/3"),
                style = MaterialTheme.typography.titleSmall,
                color = if (combo > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
        }
        // Falling field
        val laneColors = MaterialTheme.oneSpaceColors.lanes
        val laneLine = MaterialTheme.colorScheme.outlineVariant
        val hitZone = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            val laneW = size.width / 4f
            val hitTop = size.height * 0.78f
            // lanes
            val line = laneLine
            for (i in 1..3) drawLine(line, Offset(laneW * i, 0f), Offset(laneW * i, size.height), 2f)
            // hit zone
            drawRect(hitZone, topLeft = Offset(0f, hitTop), size = androidx.compose.ui.geometry.Size(size.width, size.height - hitTop))
            // notes: one brand color per lane so the eye locks onto its pad
                        notes.forEach { n ->
                val top = n.y * size.height
                drawRoundRect(
                    color = laneColors[n.lane],
                    topLeft = Offset(n.lane * laneW + 6f, top),
                    size = androidx.compose.ui.geometry.Size(laneW - 12f, laneW * 0.62f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
                )
            }
        }
        // Tap pads
        Row(Modifier.fillMaxWidth()) {
            repeat(4) { lane ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .height(92.dp)
                        .padding(4.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                        .clickable { tapLane(lane) },
                    contentAlignment = Alignment.Center
                ) {}
            }
        }
    }

    if (!running) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = {},
            title = { Text(if (score > 0) tr("Hết nhạc!", "Out of music!") else "Beat Tiles") },
            text = {
                Text(if (score > 0)
                    tr("Điểm: $score — combo cao nhất x$bestCombo.", "Score: $score, best combo x$bestCombo.")
                else
                    tr("Nốt nhạc rơi theo nhịp — chạm đúng ô khi nốt chạm vùng sáng ở đáy. Chơi kèm bài đang phát trên dock cho trọn (case C9).", "Notes fall to the beat: tap the lane when a note reaches the glowing zone at the bottom. Best played along with the song in the dock (case C9)."))
            },
            confirmButton = {
                Button(onClick = { start() }) { Text(if (score > 0) tr("Chơi lại", "Play again") else tr("Bắt đầu", "Start")) }
            }
        )
    }
}
