package com.example.onespace.feature.games.navigation

import com.example.onespace.core.designsystem.t
import com.example.onespace.core.designsystem.tr
import com.example.onespace.core.designsystem.fadingEdgeEnd
import com.example.onespace.core.designsystem.AppCard
import androidx.compose.runtime.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LineStyle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import com.example.onespace.core.designsystem.oneSpaceColors
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
    onOpenGame: (String) -> Unit,
    onMinimizeVideo: () -> Unit
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
                        val challenge by vm.challenge.collectAsStateWithLifecycle()
                        val streakDays by vm.streakDays.collectAsStateWithLifecycle()
                        challenge?.let { today ->
                            DailyChallengeCard(today, streakDays, onPlay = { onOpenGame(today.template.gameId) })
                        }
                    }
                    item {
                        LazyRow(
                    modifier = Modifier.fadingEdgeEnd(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                            item { FilterChip(selected = gameFilter == "all", onClick = { gameFilter = "all" }, label = { Text(tr("Tất cả", "All")) }) }
                            item { FilterChip(selected = gameFilter == "suy_luan", onClick = { gameFilter = "suy_luan" }, label = { Text(tr("Suy luận", "Logic")) }) }
                            item { FilterChip(selected = gameFilter == "am_nhac", onClick = { gameFilter = "am_nhac" }, label = { Text(tr("Âm nhạc", "Music")) }) }
                            item { FilterChip(selected = gameFilter == "hanh_dong", onClick = { gameFilter = "hanh_dong" }, label = { Text(tr("Hành động", "Action")) }) }
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
                                label = { Text(tr("Tuần này", "This week")) }
                            )
                            FilterChip(
                                selected = leaderboardPeriod == "month",
                                onClick = { leaderboardPeriod = "month" },
                                label = { Text(tr("Tháng này", "This month")) }
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
                        "am_nhac" -> games.items.filter { it.id in listOf("g3", "g6") }
                        "hanh_dong" -> games.items.filter { it.id in listOf("g5") }
                        else -> games.items
                    }
                    items(filtered, key = { it.id }) { game ->
                        // Game card with the best score as a trophy badge on the
                        // title row (right side, before the chevron), not a
                        // separate line below the card.
                        com.example.onespace.core.designsystem.AppCard(
                            onClick = { onOpenGame(game.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            androidx.compose.foundation.layout.Row(
                                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(14.dp)
                            ) {
                                androidx.compose.foundation.layout.Box(
                                    Modifier.size(44.dp).background(
                                        gameTint(game.id).copy(alpha = 0.12f), MaterialTheme.shapes.medium
                                    ),
                                    contentAlignment = androidx.compose.ui.Alignment.Center
                                ) {
                                    Icon(gameIcon(game.id), contentDescription = null, tint = gameTint(game.id))
                                }
                                androidx.compose.foundation.layout.Column(
                                    Modifier.weight(1f),
                                    verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(game.title.t(), style = MaterialTheme.typography.titleMedium)
                                    Text(game.description.t(), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (game.bestScore > 0) {
                                    androidx.compose.foundation.layout.Row(
                                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.EmojiEvents,
                                            contentDescription = tr("Kỷ lục", "Best"),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            game.bestScore.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Icon(
                                    Icons.Filled.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
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
                        contentDescription = tr("Xếp hạng", "Leaderboard"),
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
                        contentDescription = tr("Đóng xếp hạng", "Close leaderboard"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            } // Box
        }
        composable<GamesRoute.Game> { entry ->
            val route = entry.toRoute<GamesRoute.Game>()
            val id = route.id
            // F-GAME-08: while a video is playing it stays on top and the
            // game runs in the bottom half of the screen.
            val playback by session.video.collectAsStateWithLifecycle()
            val videoActive = playback.videoId != null
            val awardVm: PlayAwardViewModel = hiltViewModel()
            var completion by remember { mutableStateOf<com.example.onespace.domain.games.usecase.ChallengeCompletion?>(null) }
            LaunchedEffect(Unit) {
                awardVm.challengeCompletions.collect {
                    completion = it
                    kotlinx.coroutines.delay(5_000)
                    completion = null
                }
            }
            var unlocked by remember { mutableStateOf<List<com.example.onespace.domain.games.badge.GameBadge>>(emptyList()) }
            LaunchedEffect(Unit) {
                awardVm.newBadges.collect {
                    unlocked = it
                    kotlinx.coroutines.delay(5_000)
                    unlocked = emptyList()
                }
            }
            Box(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                // Games that need the whole screen cannot share it with a video: say so first.
                val needsFullScreen = id in FULL_SCREEN_GAMES
                var fullScreenAccepted by remember(id) { mutableStateOf(false) }
                val split = videoActive && !(needsFullScreen && fullScreenAccepted)
                if (videoActive && needsFullScreen && !fullScreenAccepted) {
                    FullScreenNotice(
                        onPlayFullScreen = {
                            if (playback.playRequested) session.toggleVideo()
                            fullScreenAccepted = true
                        }
                    )
                } else if (split) {
                    // F-GAME-07: a real split. The video owns the top third with the same tap-to-show
                    // controls as the player; the board below keeps its full size and is never covered.
                    SplitVideo(
                        player = awardVm.player,
                        title = playback.title,
                        onToggle = session::toggleVideo,
                        onSeekBy = session::seekVideoBy,
                        onSeekTo = session::seekVideoTo,
                        onMinimize = onMinimizeVideo,
                        modifier = Modifier.fillMaxWidth().weight(0.34f)
                    )
                }
                if (!(videoActive && needsFullScreen && !fullScreenAccepted)) Box(Modifier.weight(if (split) 0.66f else 1f)) {
                    when (id) {
                        "g1" -> {
                            val vm: Game2048ViewModel = hiltViewModel()
                            val state by vm.state.collectAsStateWithLifecycle()
                            val moves by vm.moves.collectAsStateWithLifecycle()
                            val best by vm.best.collectAsStateWithLifecycle()
                            val outcome by vm.outcome.collectAsStateWithLifecycle()
                            Game2048Screen(
                                state = state,
                                moves = moves,
                                best = best,
                                outcome = outcome,
                                onMove = vm::move,
                                onRestart = vm::restart,
                                onContinue = vm::continuePlaying
                            )
                        }
                        "g2" -> {
                            val vm: TicTacToeViewModel = hiltViewModel()
                            val ui by vm.ui.collectAsStateWithLifecycle()
                            val bestStreak by vm.bestStreak.collectAsStateWithLifecycle()
                            TicTacToeScreen(
                                ui = ui,
                                bestStreak = bestStreak,
                                onOpponent = vm::setOpponent,
                                onPlay = vm::play,
                                onNewRound = vm::newRound
                            )
                        }
                        "g3" -> {
                            val vm: GuessSongViewModel = hiltViewModel()
                            val state by vm.state.collectAsStateWithLifecycle()
                            GuessSongScreen(
                                state = state,
                                onStart = vm::startNewRound,
                                onAnswer = vm::answer,
                                onNext = vm::nextRound,
                                onStartPlaylist = { vm.startFromPlaylist(it.trackIds) }
                            )
                        }
                        "g4" -> {
                            val vm: KnowledgeQuizViewModel = hiltViewModel()
                            KnowledgeQuizScreen(onAnswerSound = vm::answerSound, onFinish = vm::finish)
                        }
                        "g5" -> {
                            SnakeScreen(sfx = awardVm.sfx, onGameOver = { score ->
                                if (score > 0) awardVm.awardGame("ran_san_moi", score * 2)
                                awardVm.recordScore("g5", score)
                            })
                        }
                        "g6" -> {
                            val vm: BeatTilesViewModel = hiltViewModel()
                            val songs by vm.songs.collectAsStateWithLifecycle()
                            // Null = menu; a pick of "random" or of one song starts the game.
                            var pick by remember(route.track) {
                                mutableStateOf<BeatTilesPick?>(null)
                            }
                            // A song picked for this round (or sent over from Now Playing)
                            // is the round's backing track; leaving the game releases the
                            // dock back to the normal floating player.
                            DisposableEffect(Unit) {
                                onDispose { com.example.onespace.core.designsystem.GameMusicState.set(false) }
                            }
                            LaunchedEffect(route.track, songs) {
                                val wanted = route.track?.let { id -> songs.find { it.id == id } }
                                if (wanted != null && pick == null) {
                                    session.playMusic(wanted.id, wanted.title)
                                    com.example.onespace.core.designsystem.GameMusicState.set(true)
                                    pick = BeatTilesPick.Song(wanted)
                                }
                            }
                            val chosen = pick
                            if (chosen == null) {
                                BeatTilesMenu(
                                    songs = songs,
                                    onRandom = { pick = BeatTilesPick.Random },
                                    onSong = {
                                        session.playMusic(it.id, it.title)
                                        com.example.onespace.core.designsystem.GameMusicState.set(true)
                                        pick = BeatTilesPick.Song(it)
                                    }
                                )
                            } else {
                                BeatTilesScreen(
                                    bpm = (chosen as? BeatTilesPick.Song)?.track?.bpm,
                                    songTitle = (chosen as? BeatTilesPick.Song)?.track?.title,
                                    sfx = awardVm.sfx,
                                    onGameOver = { score ->
                                        if (score > 0) awardVm.awardGame("beat_tiles", score / 2)
                                        awardVm.recordScore("g6", score)
                                    }
                                )
                            }
                        }
                        else -> Text(tr("Không tìm thấy game", "Game not found"))
                    }
                }
            }
            Column(Modifier.align(Alignment.TopCenter)) {
                completion?.let { ChallengeCompletedBanner(it) }
                if (unlocked.isNotEmpty()) BadgeUnlockedBanner(unlocked)
            }
            }
        }
    }
}

/** Games that need the whole screen (tall lanes, swipe-driven boards) and so cannot be split. */
private val FULL_SCREEN_GAMES = setOf("g5", "g6")

/** Shown instead of the board when a full-screen game is opened while a video is playing. */
@Composable
private fun FullScreenNotice(onPlayFullScreen: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.SportsEsports, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp))
        Text(tr("Trò chơi này cần toàn màn hình", "This game needs the full screen"),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            tr("Không thể vừa xem video vừa chơi. Video sẽ được tạm dừng khi bạn chơi toàn màn hình.",
                "You cannot watch the video while playing. The video is paused while you play full screen."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        androidx.compose.material3.Button(onClick = onPlayFullScreen, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Chơi toàn màn hình", "Play full screen"))
        }
    }
}

/**
 * Video on top of a game: tap shows back-10 / play-pause / forward-10 and the progress line, which fade
 * after 2 seconds, so the picture stays clean while you play. The position polls the shared player
 * directly; the session flow only updates on player events. Top-right, "Thu nhỏ" leaves the game and
 * hands the video back to the floating window; "Chỉ nghe âm thanh" hides the surface, mirroring the
 * floating window's F-VID-09 sound-only mode.
 */
@Composable
private fun SplitVideo(
    player: androidx.media3.common.Player?,
    title: String,
    onToggle: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onMinimize: () -> Unit,
    modifier: Modifier = Modifier
) {
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(false) }
    var controls by remember { mutableStateOf(true) }
    var touches by remember { mutableIntStateOf(0) }
    LaunchedEffect(player) {
        while (true) {
            if (player != null) {
                pos = player.currentPosition.coerceAtLeast(0)
                dur = player.duration.coerceAtLeast(0)
                playing = player.isPlaying
            }
            kotlinx.coroutines.delay(500)
        }
    }
    LaunchedEffect(controls, touches, playing) {
        if (controls && playing) { kotlinx.coroutines.delay(2000); controls = false }
    }
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        if (controls) 1f else 0f, androidx.compose.animation.core.tween(300), label = "split-controls"
    )
    val onMedia = MaterialTheme.oneSpaceColors.onMedia
    val audioOnly = com.example.onespace.core.designsystem.AudioOnlyState.active
    Box(modifier) {
        if (audioOnly) {
            // Sound-only: the surface is gone, the audio keeps playing over a
            // compact control row; SmartDisplay brings the picture back.
            Row(
                Modifier.fillMaxSize()
                    .background(MaterialTheme.oneSpaceColors.mediaBackdrop)
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        title, style = MaterialTheme.typography.labelLarge, color = onMedia,
                        maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        if (playing) tr("Đang phát âm thanh", "Playing audio") else tr("Đã tạm dừng", "Paused"),
                        style = MaterialTheme.typography.labelSmall,
                        color = onMedia.copy(alpha = 0.7f)
                    )
                }
                androidx.compose.material3.IconButton(onClick = { onToggle() }) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"),
                        tint = onMedia
                    )
                }
                androidx.compose.material3.IconButton(onClick = {
                    com.example.onespace.core.designsystem.AudioOnlyState.set(false)
                }) {
                    Icon(Icons.Filled.SmartDisplay, contentDescription = tr("Quay lại video", "Back to video"), tint = onMedia)
                }
            }
        } else {
            com.example.onespace.core.media.VideoSurface(
                player, Modifier.fillMaxSize().background(MaterialTheme.oneSpaceColors.mediaBackdrop), { onSeekBy(10_000) }
            )
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                detectTapGestures(onTap = { controls = !controls; touches++ })
            })
            if (alpha > 0.01f) Box(Modifier.matchParentSize().alpha(alpha)) {
                Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                    androidx.compose.material3.IconButton(onClick = { touches++; onSeekBy(-10_000) }) {
                        Icon(Icons.Filled.Replay10, contentDescription = tr("Lùi 10 giây", "Back 10 seconds"), tint = onMedia)
                    }
                    androidx.compose.material3.IconButton(onClick = { touches++; onToggle() }, modifier = Modifier.size(52.dp)) {
                        Icon(
                            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"),
                            tint = onMedia, modifier = Modifier.size(34.dp)
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = { touches++; onSeekBy(10_000) }) {
                        Icon(Icons.Filled.Forward10, contentDescription = tr("Tới 10 giây", "Forward 10 seconds"), tint = onMedia)
                    }
                }
                Text(
                    com.example.onespace.core.common.formatMsWhole(pos) + " / " + com.example.onespace.core.common.formatMsWhole(dur),
                    style = MaterialTheme.typography.labelSmall, color = onMedia,
                    modifier = Modifier.align(Alignment.BottomStart).padding(start = 10.dp, bottom = 16.dp)
                )
                // Top-right: shrink the split back to the floating window, or keep only the sound.
                Row(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp)) {
                    androidx.compose.material3.IconButton(onClick = { touches++; onMinimize() }) {
                        Icon(
                            Icons.Filled.PictureInPictureAlt,
                            contentDescription = tr("Thu nhỏ video", "Minimize video"),
                            tint = onMedia
                        )
                    }
                    androidx.compose.material3.IconButton(onClick = {
                        touches++
                        com.example.onespace.core.designsystem.AudioOnlyState.set(true)
                    }) {
                        Icon(
                            Icons.Filled.Headphones,
                            contentDescription = tr("Chỉ nghe âm thanh", "Audio only"),
                            tint = onMedia
                        )
                    }
                }
            }
        }
        // The line lives on the bottom edge of the frame; draggable while the controls are up
        // (always, in sound-only mode where there is nothing else to tap).
        com.example.onespace.core.designsystem.ScrubBar(
            pos, dur,
            modifier = Modifier.align(Alignment.BottomCenter),
            onSeek = { touches++; onSeekTo(it) },
            interactive = audioOnly || alpha > 0.5f,
            trackHeight = if (audioOnly || alpha > 0.5f) 3.dp else 2.dp,
            knobRadius = 6.dp,
            trackColor = onMedia.copy(alpha = 0.3f)
        )
    }
}

/** Unique icon per game so the hub reads at a glance. */
private fun gameIcon(id: String): androidx.compose.ui.graphics.vector.ImageVector = when (id) {
    "g1" -> Icons.Filled.Tag          // 2048: the number tile
    "g2" -> Icons.Filled.Grid3x3      // Tic-Tac-Toe: the 3x3 board
    "g3" -> Icons.Filled.MusicNote   // Đoán bài hát
    "g4" -> Icons.Filled.Quiz         // Quiz kiến thức
    "g5" -> Icons.Filled.LineStyle    // Rắn săn mồi: the snake line
    "g6" -> Icons.Filled.GridOn       // Beat Tiles: the falling tile grid
    else -> Icons.Filled.SportsEsports
}

/** One hue per game so the hub tiles stay distinguishable even before the icon is read. */
private fun gameTint(id: String): androidx.compose.ui.graphics.Color = when (id) {
    "g1" -> androidx.compose.ui.graphics.Color(0xFFFF9800) // orange
    "g2" -> androidx.compose.ui.graphics.Color(0xFF2196F3) // blue
    "g3" -> androidx.compose.ui.graphics.Color(0xFFE91E63) // pink
    "g4" -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // purple
    "g5" -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // green
    "g6" -> androidx.compose.ui.graphics.Color(0xFF00BCD4) // cyan
    else -> androidx.compose.ui.graphics.Color.Unspecified
}

/** F-GAME-07: one leaderboard section with medals and a standing-out "Bạn". */
@Composable
private fun LeaderboardList(board: List<Pair<String, Int>>, myPoints: Int) {
    val me = tr("Bạn", "You")
    val rows = (board + (me to myPoints)).sortedByDescending { it.second }
    Column(Modifier.fillMaxWidth()) {
        rows.forEachIndexed { idx, (name, pts) ->
            val isMe = name == me
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
                            if (isMe) me else name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isMe) FontWeight.Bold else FontWeight.Normal,
                            color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                        if (isMe) {
                            Text(
                                tr("Điểm của bạn", "Your points"),
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
                        tr("điểm", "points"),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
