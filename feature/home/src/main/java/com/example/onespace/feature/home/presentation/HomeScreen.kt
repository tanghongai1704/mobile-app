package com.example.onespace.feature.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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

/**
 * Discovery surface: resume where the user left off and a small set of
 * cross-category content picks. Content deeplinks only — never tab shortcuts,
 * which would duplicate the bottom navigation.
 */
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    onOpenVideo: (id: String, title: String) -> Unit,
    onPlayTrack: (id: String, title: String) -> Unit,
    onOpenArticle: (id: String) -> Unit
) {
    FeaturePage {
        if (state.continueWatching.isNotEmpty()) {
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
        item { SectionTitle("Đề xuất hôm nay") }
        state.videos.firstOrNull()?.let { video ->
            item(key = "pick-video") {
                ContentCard(
                    title = video.title,
                    subtitle = video.creator + " · " + video.duration,
                    icon = Icons.Filled.PlayCircle,
                    onClick = { onOpenVideo(video.id, video.title) }
                )
            }
        }
        state.tracks.firstOrNull()?.let { track ->
            item(key = "pick-track") {
                ContentCard(
                    title = track.title,
                    subtitle = track.artist + " · " + track.duration,
                    icon = Icons.Filled.MusicNote,
                    onClick = { onPlayTrack(track.id, track.title) }
                )
            }
        }
        state.articles.firstOrNull()?.let { article ->
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
}
