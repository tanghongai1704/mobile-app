package com.example.onespace.feature.games.presentation

import com.example.onespace.core.designsystem.tr
import com.example.onespace.core.media.GameSfx
import com.example.onespace.core.media.GameSound
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
 * F-GAME-05: Snake — swipe to steer, eat to grow, game over on collision.
 */
@Composable
internal fun SnakeScreen(sfx: GameSfx? = null, onGameOver: (Int) -> Unit = {}) {
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
                sfx?.play(GameSound.GAMEOVER)
                if (!reported) { reported = true; onGameOver(score) }
            } else {
                snake = listOf(next) + if (next == food) {
                    score += 1
                    sfx?.play(GameSound.TAP)
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

/**
 * Knowledge quiz in the "Who wants to be a millionaire" style: ten general-knowledge questions
 * from easy to hard, a growing reward, a safe checkpoint after question five and the option to
 * walk away with what has been earned.
 */
@Composable
internal fun KnowledgeQuizScreen(
    onAnswerSound: (correct: Boolean) -> Unit = {},
    onFinish: (earned: Int, correct: Int) -> Unit = { _, _ -> }
) {
    var run by remember { mutableStateOf(com.example.onespace.domain.games.engine.KnowledgeQuizRun.start()) }
    var version by remember { mutableIntStateOf(0) }
    var picked by remember { mutableStateOf<Int?>(null) }
    var correct by remember { mutableIntStateOf(0) }
    var reported by remember { mutableStateOf(false) }
    @Suppress("UNUSED_EXPRESSION") version
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(tr("Quiz kiến thức", "Knowledge quiz"), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        val q = run.current
        if (q == null || (picked != null && run.over)) {
            // Result card (also shown after the last answer is revealed).
        }
        val showing = run.current
        if (showing != null) {
            // Ladder: question number and what it is worth.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("Câu ${run.index + 1}/${run.questions.size}", "Question ${run.index + 1}/${run.questions.size}"),
                    style = MaterialTheme.typography.titleMedium)
                Text(tr("Giá trị ${run.worth} điểm", "Worth ${run.worth} points"),
                    style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(progress = { run.index / run.questions.size.toFloat() }, modifier = Modifier.fillMaxWidth())
            Text(tr("Đã tích lũy: ${run.earned} điểm", "Earned so far: ${run.earned} points"),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.fillMaxWidth()) {
                Text(showing.question, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(16.dp))
            }
            val letters = listOf("A", "B", "C", "D")
            showing.options.forEachIndexed { oi, option ->
                val reveal = picked != null
                val isRight = oi == showing.correctIndex
                OutlinedButton(
                    onClick = {
                        if (picked == null) {
                            picked = oi
                            val right = oi == showing.correctIndex
                            if (right) correct += 1
                            onAnswerSound(right)
                        }
                    },
                    colors = when {
                        reveal && isRight -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.oneSpaceColors.feedback.container)
                        reveal && picked == oi -> ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                        else -> ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(letters[oi] + ".  " + option, modifier = Modifier.fillMaxWidth())
                }
            }
            if (picked != null) {
                val wasRight = picked == showing.correctIndex
                Text(
                    if (wasRight) tr("Chính xác! +${run.worth} điểm", "Correct! +${run.worth} points")
                    else tr("Rất tiếc, đáp án đúng là ${letters[showing.correctIndex]}.", "Sorry, the answer was ${letters[showing.correctIndex]}."),
                    style = MaterialTheme.typography.titleSmall,
                    color = if (wasRight) MaterialTheme.oneSpaceColors.feedback.success else MaterialTheme.colorScheme.error
                )
                Button(onClick = {
                    run.answer(picked!!)
                    picked = null
                    version++
                    if (run.over && !reported) { reported = true; onFinish(run.earned, correct) }
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Tiếp tục", "Continue"))
                }
            } else if (run.index > 0) {
                // Walking away is only offered between questions, after at least one right answer.
                TextButton(onClick = {
                    run.walkAway(); version++
                    if (!reported) { reported = true; onFinish(run.earned, correct) }
                }) { Text(tr("Dừng cuộc chơi, giữ ${run.earned} điểm", "Stop here and keep ${run.earned} points")) }
            }
        } else {
            val won = run.won
            Text(
                if (won) tr("Chúc mừng, bạn đã vượt qua tất cả!", "Congratulations, you cleared them all!")
                else tr("Kết thúc ván chơi", "Round over"),
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary
            )
            Text(tr("Trả lời đúng $correct/${run.questions.size} câu", "$correct/${run.questions.size} correct"),
                style = MaterialTheme.typography.titleMedium)
            Text(tr("Nhận ${run.earned} điểm", "You earn ${run.earned} points"),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Button(onClick = {
                run = com.example.onespace.domain.games.engine.KnowledgeQuizRun.start()
                picked = null; correct = 0; reported = false; version++
            }) { Text(tr("Chơi lại", "Play again")) }
        }
    }
}

/** Beat Tiles entry: a random round, or pick one of your songs that has beat data. */
@Composable
internal fun BeatTilesMenu(
    songs: List<com.example.onespace.domain.music.model.Track>,
    onRandom: () -> Unit,
    onSong: (com.example.onespace.domain.music.model.Track) -> Unit
) {
    var choosing by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Beat Tiles", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            tr("Chạm đúng ô khi nốt nhạc rơi tới vạch sáng.", "Tap the lane when a note reaches the glowing line."),
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Button(onClick = onRandom, modifier = Modifier.fillMaxWidth()) { Text(tr("Chơi ngẫu nhiên", "Play random")) }
        OutlinedButton(onClick = { choosing = !choosing }, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Chọn bài hát của bạn", "Choose one of your songs"))
        }
        if (choosing) {
            if (songs.isEmpty()) {
                Text(tr("Chưa có bài hát nào hỗ trợ Beat Tiles.", "No songs support Beat Tiles yet."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else songs.forEach { t ->
                com.example.onespace.core.designsystem.ContentCard(
                    t.title, t.artist + " · " + t.bpm + " BPM",
                    Icons.Filled.MusicNote, { onSong(t) }
                )
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
 * F-GAME-06: Beat Tiles — four lanes of falling notes tapped on the beat.
 * Music from the dock keeps playing alongside (case C9); the notes give
 * the rhythm visually so the game is playable with or without a song.
 */
@Composable
internal fun BeatTilesScreen(
    bpm: Int? = null,
    songTitle: String? = null,
    sfx: GameSfx? = null,
    onGameOver: (Int) -> Unit = {}
) {
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
            // With a song the tiles follow its tempo (one tile per beat); otherwise a gentle
            // speed-up. 33 ms per tick.
            val interval = if (bpm != null) maxOf(6, (60_000f / bpm / 33f).toInt()) else maxOf(9, 18 - tick / 400)
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
            if (misses >= 3) {
                sfx?.play(GameSound.GAMEOVER)
                running = false
            }
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
            sfx?.play(GameSound.TAP)
        } else {
            combo = 0
            sfx?.play(GameSound.WRONG)
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Score header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(tr("Điểm: $score", "Score: $score"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (songTitle != null) Text(songTitle, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
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
                    tr("Nốt nhạc rơi theo nhịp — chạm đúng ô khi nốt chạm vùng sáng ở đáy.", "Notes fall to the beat: tap the lane when a note reaches the glowing zone at the bottom."))
            },
            confirmButton = {
                Button(onClick = { start() }) { Text(if (score > 0) tr("Chơi lại", "Play again") else tr("Bắt đầu", "Start")) }
            }
        )
    }
}
