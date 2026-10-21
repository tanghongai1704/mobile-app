package com.example.onespace.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/** M3 mini player: whole row expands; transport actions are 48dp icon buttons. */
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
                    contentDescription = if (playing) "Pause" else "Play")
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "Close")
            }
        }
    }
}

/** Floating (picture-in-picture style) video card with M3 icon actions. */
@Composable
fun BoxScope.FloatingVideo(title: String, playing: Boolean, onExpand: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit) {
    Card(Modifier.align(Alignment.BottomEnd).padding(12.dp).widthIn(max = 320.dp)) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(
                Brush.linearGradient(listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.surfaceContainerHighest)),
                RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)).clickable(onClick = onExpand),
                contentAlignment = Alignment.Center) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp))
            }
            Column(Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)) {
                Text(title, maxLines = 1, style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp))
                Text(if (playing) "Floating · Demo playing" else "Floating · Paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row {
                    IconButton(onClick = onExpand) {
                        Icon(Icons.Filled.OpenInFull, contentDescription = "Expand")
                    }
                    IconButton(onClick = onToggle) {
                        Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
            }
        }
    }
}
