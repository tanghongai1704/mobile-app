package com.example.onespace.feature.video.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.formatMsWhole
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.ContinueEntry

@Composable
internal fun VideoScreen(
    videos: List<Video>,
    continueWatching: List<ContinueEntry>,
    onOpen: (String) -> Unit
) {
    FeaturePage {
        if (continueWatching.isNotEmpty()) {
            item { SectionTitle("Tiếp tục xem") }
            items(continueWatching, key = { "continue-" + it.videoId }) { entry ->
                val video = videos.firstOrNull { it.id == entry.videoId }
                ContentCard(
                    title = video?.title ?: entry.title,
                    subtitle = "đã xem đến " + formatMsWhole(entry.positionMs),
                    icon = Icons.Filled.Schedule,
                    onClick = { onOpen(entry.videoId) }
                )
            }
        }
        items(videos, key = { it.id }) { video ->
            ContentCard(video.title, video.creator + " · " + video.duration, Icons.Filled.PlayCircle, { onOpen(video.id) })
        }
    }
}

/**
 * Content detail: the player is the dominant element; identity (title,
 * creator) and actions sit directly under it, then related videos. The shell
 * top bar carries "← Video".
 */
@Composable
internal fun VideoDetailScreen(
    video: Video?,
    related: List<Video>,
    playing: Boolean,
    muted: Boolean,
    positionMs: Long,
    totalMs: Long,
    videoSurface: @Composable (Modifier, () -> Unit) -> Unit,
    onToggle: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onMinimize: () -> Unit,
    onOpenRelated: (String) -> Unit
) {
    if (video == null) { Text("Chưa có video này"); return }
    FeaturePage {
        item {
            Box(Modifier.fillMaxWidth()) {
                videoSurface(Modifier.fillMaxWidth().height(230.dp)) { onSeekBy(10_000) }
                if (muted) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    ) {
                        Text(
                            "Tắt tiếng",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onToggle) { Text(if (playing) "Tạm dừng" else "Phát") }
                OutlinedButton(onClick = onMinimize) { Text("Thu nhỏ & khám phá") }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(video.title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    video.creator + " · " + video.duration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            if (totalMs > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LinearProgressIndicator(
                        progress = { if (totalMs == 0L) 0f else (positionMs.toFloat() / totalMs).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        formatMsWhole(positionMs) + " / " + formatMsWhole(totalMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        if (related.any { it.id != video.id }) {
            item { SectionTitle("Liên quan") }
            items(related.filter { it.id != video.id }, key = { "related-" + it.id }) { other ->
                ContentCard(other.title, other.creator + " · " + other.duration, Icons.Filled.PlayCircle, { onOpenRelated(other.id) })
            }
        }
        item { PrototypeNote("Chạm hai lần vào video để tua nhanh 10 giây.") }
    }
}
