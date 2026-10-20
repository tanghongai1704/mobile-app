package com.example.onespace.feature.video.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.video.model.Video

@Composable
internal fun VideoScreen(videos: List<Video>, onOpen: (String) -> Unit) {
    FeaturePage("Watch / discover", "Worth your time.", "Stories, ideas and a change of scenery.") {
        item { Artwork("THE EVERYDAY COLLECTION", Modifier.fillMaxWidth().height(170.dp)) }
        item { SectionTitle("Made for your next break") }
        items(videos, key = { it.id }) { video ->
            ContentCard(video.title, video.creator + " · " + video.duration, "▶", { onOpen(video.id) })
        }
    }
}

@Composable
internal fun VideoDetailScreen(video: Video?, playing: Boolean, onToggle: () -> Unit, onMinimize: () -> Unit) {
    if (video == null) { Text("Video unavailable"); return }
    FeaturePage("OneSpace video", video.title, video.creator + " · " + video.duration) {
        item { Artwork(if (playing) "PLAYING · DEMO" else "PAUSED · DEMO", Modifier.fillMaxWidth().height(210.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onToggle) { Text(if (playing) "Pause" else "Play") }
                OutlinedButton(onClick = onMinimize) { Text("Float & explore") }
            }
        }
        item { SectionTitle("A moment to explore") }
        item { Text("Keep this story with you while you read the news or take a break with a game.") }
        item { PrototypeNote("Player preview · Sample content, no video stream connected yet.") }
    }
}
