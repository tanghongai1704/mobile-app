package com.example.onespace.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.Mint
import com.example.onespace.core.designsystem.Panel

@Composable
fun MusicMiniPlayer(title: String, playing: Boolean, onExpand: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit) {
    Surface(color = Panel) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onExpand, modifier = Modifier.weight(1f)) { Text("♫  " + title, maxLines = 1) }
            TextButton(onClick = onToggle) { Text(if (playing) "Pause" else "Play") }
            TextButton(onClick = onClose) { Text("Close") }
        }
    }
}

@Composable
fun BoxScope.FloatingVideo(title: String, playing: Boolean, onExpand: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit) {
    Card(Modifier.align(Alignment.BottomEnd).padding(12.dp).widthIn(max = 300.dp),
        colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.padding(12.dp)) {
            Text("▶  " + title, maxLines = 1, style = MaterialTheme.typography.titleSmall)
            Text(if (playing) "Floating · Demo playing" else "Floating · Paused",
                style = MaterialTheme.typography.bodySmall, color = Mint)
            Row {
                TextButton(onClick = onExpand) { Text("Expand") }
                TextButton(onClick = onToggle) { Text(if (playing) "Pause" else "Play") }
                TextButton(onClick = onClose) { Text("Close") }
            }
        }
    }
}
