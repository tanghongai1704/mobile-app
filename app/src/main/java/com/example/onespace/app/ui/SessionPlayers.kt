package com.example.onespace.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.onespace.core.media.VideoSurface

/** M3 mini player dock: whole row expands; transport actions are 48dp icon buttons. */
@Composable
fun MusicMiniPlayer(title: String, playing: Boolean, onExpand: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(horizontal = 16.dp)
            .height(56.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MusicNote, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(title, maxLines = 1, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f))
            IconButton(onClick = onToggle) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) "Tạm dừng" else "Phát")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Đóng")
            }
        }
    }
}

/**
 * In-app floating window for the playing video (ADR-08): the SAME shared player
 * renders here through [VideoSurface]; closing keeps the video in the Watchlist
 * at its last position (case C3).
 */
@Composable
fun BoxScope.FloatingVideo(
    player: Player?,
    title: String,
    playing: Boolean,
    muted: Boolean,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onClose: () -> Unit
) {
    Card(Modifier.align(Alignment.BottomEnd).padding(12.dp).widthIn(max = 320.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                VideoSurface(player, Modifier.fillMaxSize())
                if (muted) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
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
            Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Text(title, maxLines = 1, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp))
                Text(if (playing) "Đang phát · cửa sổ nổi" else "Tạm dừng · cửa sổ nổi",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    IconButton(onClick = onExpand) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = "Mở rộng")
                    }
                    IconButton(onClick = onToggle) {
                        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Tạm dừng" else "Phát")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Đóng")
                    }
                }
            }
        }
    }
}
