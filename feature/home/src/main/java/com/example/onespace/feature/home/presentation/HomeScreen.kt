package com.example.onespace.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.formatMsWhole
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.ContinueEntry

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val articles: List<Article> = emptyList(),
    val continueWatching: List<ContinueEntry> = emptyList()
)

data class GamificationUiState(
    val totalPoints: Int = 0,
    val streak: Int = 0,
    val missions: List<MissionUi> = emptyList()
)

data class MissionUi(
    val id: String,
    val title: String,
    val progress: Int = 0,
    val target: Int = 1,
    val completed: Boolean = false
) {
    val progressText: String get() = if (completed) "Hoàn thành" else "$progress/$target"
}

/**
 * Discovery surface: resume where the user left off and a small set of
 * cross-category content picks. Content deeplinks only — never tab shortcuts,
 * which would duplicate the bottom navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    gamification: GamificationUiState = GamificationUiState(),
    offline: Boolean = false,
    watchLater: Set<String> = emptySet(),
    onMissionClick: (String) -> Unit = {},
    onOpenVideo: (id: String, title: String) -> Unit,
    onPlayTrack: (id: String, title: String) -> Unit,
    onOpenArticle: (id: String) -> Unit
) {
    // F-COM-11: one search field across all four sections.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showWatchlist by remember { mutableStateOf(false) }
    // System back clears an active search before leaving the app.
    androidx.activity.compose.BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
    }
    val q = searchQuery.trim()
    val videos = if (q.isEmpty()) state.videos else state.videos.filter { it.title.contains(q, true) || it.creator.contains(q, true) }
    val tracks = if (q.isEmpty()) state.tracks else state.tracks.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    val articles = if (q.isEmpty()) state.articles else state.articles.filter { it.title.contains(q, true) || it.category.contains(q, true) }
    FeaturePage {
        item {
            androidx.compose.material3.OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { androidx.compose.material3.Text("Tìm trong OneSpace") },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Filled.Close,
                                contentDescription = "Xóa tìm kiếm"
                            )
                        }
                    }
                },
                singleLine = true
            )
        }
        if (q.isEmpty()) {
            item {
                // F-VID-04: the watchlist lives on Home.
                androidx.compose.material3.FilledTonalButton(onClick = { showWatchlist = true }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Text(
                        if (watchLater.isEmpty()) "Xem sau" else "Xem sau (" + watchLater.size + ")"
                    )
                }
            }
        }
        if (q.isNotEmpty()) {
            if (videos.isEmpty() && tracks.isEmpty() && articles.isEmpty()) {
                item {
                    androidx.compose.material3.Text(
                        "Không có kết quả cho \"" + q + "\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (videos.isNotEmpty()) {
                item { SectionTitle("Video") }
                items(videos.take(5), key = { "gs-v-" + it.id }) { video ->
                    ContentCard(video.title, video.creator + " · " + video.duration,
                        Icons.Filled.PlayCircle, { onOpenVideo(video.id, video.title) })
                }
            }
            if (tracks.isNotEmpty()) {
                item { SectionTitle("Nhạc") }
                items(tracks.take(5), key = { "gs-m-" + it.id }) { track ->
                    ContentCard(track.title, track.artist + " · " + track.duration,
                        Icons.Filled.MusicNote, { onPlayTrack(track.id, track.title) })
                }
            }
            if (articles.isNotEmpty()) {
                item { SectionTitle("Tin tức") }
                items(articles.take(5), key = { "gs-n-" + it.id }) { article ->
                    ContentCard(article.title, article.category + " · " + article.readTime,
                        Icons.Filled.Newspaper, { onOpenArticle(article.id) })
                }
            }
        }
        // Gamification banner: XP + streak + missions
        if (gamification.missions.isNotEmpty() && q.isEmpty()) {
            item(key = "gamification") {
                GamificationBanner(gamification, onMissionClick)
            }
        }

        if (offline) {
            item {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Text(
                        "Mất kết nối mạng — đang hiển thị nội dung đã lưu.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        if (state.continueWatching.isNotEmpty() && q.isEmpty()) {
            item { SectionTitle("Tiếp tục") }
            items(state.continueWatching, key = { "continue-" + it.videoId }) { entry ->
                ContentCard(
                    title = state.videos.firstOrNull { it.id == entry.videoId }?.title ?: entry.title,
                    subtitle = "đã xem đến " + formatMsWhole(entry.positionMs),
                    icon = Icons.Filled.Schedule,
                    onClick = { onOpenVideo(entry.videoId, entry.title) }
                )
            }
        }
        if (q.isEmpty()) item { SectionTitle("Đề xuất hôm nay") }
        if (q.isEmpty()) state.videos.firstOrNull()?.let { video ->
            item(key = "pick-video") {
                ContentCard(
                    title = video.title,
                    subtitle = video.creator + " · " + video.duration,
                    icon = Icons.Filled.PlayCircle,
                    onClick = { onOpenVideo(video.id, video.title) }
                )
            }
        }
        if (q.isEmpty()) state.tracks.firstOrNull()?.let { track ->
            item(key = "pick-track") {
                ContentCard(
                    title = track.title,
                    subtitle = track.artist + " · " + track.duration,
                    icon = Icons.Filled.MusicNote,
                    onClick = { onPlayTrack(track.id, track.title) }
                )
            }
        }
        if (q.isEmpty()) state.articles.firstOrNull()?.let { article ->
            item(key = "pick-article") {
                ContentCard(
                    title = article.title,
                    subtitle = article.category + " · " + article.readTime,
                    icon = Icons.Filled.Newspaper,
                    onClick = { onOpenArticle(article.id) }
                )
            }
        }
    }

    // F-VID-04: the watchlist opens on demand from Home.
    if (showWatchlist) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showWatchlist = false }) {
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).fillMaxHeight(0.75f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SectionTitle("Xem sau") }
                if (watchLater.isEmpty()) {
                    item {
                        androidx.compose.material3.Text(
                            "Chưa lưu video nào. Mở một video, bấm Lưu và chọn Xem sau.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(watchLater.mapNotNull { id -> state.videos.find { it.id == id } }, key = { it.id }) { video ->
                        ContentCard(
                            title = video.title,
                            subtitle = video.creator + " · " + video.duration,
                            icon = androidx.compose.material.icons.Icons.Filled.PlayCircle,
                            onClick = {
                                showWatchlist = false
                                onOpenVideo(video.id, video.title)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun GamificationBanner(state: GamificationUiState, onMissionClick: (String) -> Unit = {}) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: points + streak
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Filled.Star, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${state.totalPoints} điểm",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (state.streak > 0) {
                    Text(
                        "Streak ${state.streak} ngày",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Missions
            state.missions.forEach { mission ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = if (mission.completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        mission.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        mission.progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (mission.completed) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
