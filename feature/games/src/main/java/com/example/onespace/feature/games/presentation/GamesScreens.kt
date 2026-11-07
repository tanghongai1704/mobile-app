package com.example.onespace.feature.games.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.games.model.Game

@Composable
internal fun GamesScreen(games: List<Game>, onOpen: (String) -> Unit) {
    val q = LocalSearchState.current.query.trim()
    val shown = if (q.isEmpty()) games
        else games.filter { it.title.t().contains(q, true) || it.description.t().contains(q, true) }
    FeaturePage {
        if (q.isNotEmpty() && shown.isEmpty()) item {
            EmptyHint(Icons.Filled.Search, tr("Không có trò chơi nào khớp", "No games match"))
        }
        items(shown, key = { it.id }) { game ->
            val record = if (game.bestScore > 0) tr(" · Kỷ lục ", " · Record ") + game.bestScore else ""
            ContentCard(game.title.t(), game.description.t() + record,
                if (game.id == "g1") Icons.Filled.GridView else Icons.Filled.SelectAll,
                { onOpen(game.id) })
        }
    }
}

/** Guess the Song: play 5 seconds, pick the correct title from 4 options. */
@Composable
internal fun GuessSongScreen(
    state: com.example.onespace.domain.games.model.GuessSongState,
    onStart: () -> Unit,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onStartPlaylist: (AppPlaylist) -> Unit = {}
) {
    // Playlists that mix in videos are left out: only pure music playlists can play.
    val usablePlaylists = AppPlaylistStore.playlists.values.filter { it.videoIds.isEmpty() && it.trackIds.isNotEmpty() }
    // Playlist-first: opening the game lands straight on the user's playlists;
    // random is the fallback when there is nothing to pick from.
    var choosing by remember { mutableStateOf(usablePlaylists.isNotEmpty()) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(tr("Đoán bài hát", "Guess the Song"), style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Text(tr("Nghe 5 giây và chọn tên bài hát đúng", "Listen for 5 seconds and pick the right title"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        // Score
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(tr("Điểm: ${state.score}", "Score: ${state.score}"), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)
            Text(tr("Ván: ${state.round}", "Round: ${state.round}"), style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        when {
            state.question == null -> {
                // Start screen: playlist-first. The user picks one of their
                // playlists right away; random stays as a secondary choice and
                // is the only mode when no playlist exists yet.
                if (choosing) {
                    if (usablePlaylists.isEmpty()) {
                        // Fallback: nothing to pick from, so random is the only mode.
                        Text(
                            tr("Chưa có danh sách phát nhạc nào. Lưu bài hát vào một danh sách phát trước nhé.",
                                "No music playlists yet. Save songs to a playlist first."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                            Text(tr("Chơi ngẫu nhiên", "Play random"))
                        }
                    } else {
                        usablePlaylists.forEach { pl ->
                            ContentCard(
                                pl.name,
                                tr("${pl.trackIds.size} bài hát", "${pl.trackIds.size} songs"),
                                Icons.Filled.LibraryMusic,
                                { choosing = false; onStartPlaylist(pl) }
                            )
                        }
                        OutlinedButton(onClick = { choosing = false }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Chơi ngẫu nhiên", "Play random"))
                        }
                    }
                } else {
                    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                        Text(tr("Bắt đầu chơi", "Start playing"))
                    }
                    OutlinedButton(onClick = { choosing = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.LibraryMusic, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(tr("Chơi từ danh sách phát của bạn", "Play from your playlists"))
                    }
                }
            }
            !state.hasAnswered -> {
                // Playing state
                Text(
                    if (state.isPlaying) tr("Đang phát...", "Playing...") else tr("Chọn đáp án", "Pick an answer"),
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
                    if (isCorrect) tr("Chính xác! +10 điểm", "Correct! +10 points") else tr("Sai rồi!", "Wrong!"),
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isCorrect) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    tr("Đáp án: ${question.trackTitle} — ${question.artist}", "Answer: ${question.trackTitle} by ${question.artist}"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                    Text(tr("Ván tiếp theo", "Next round"))
                }
            }
        }
    }
}
