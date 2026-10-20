package com.example.onespace.feature.music.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.music.model.Track

@Composable
internal fun MusicScreen(tracks: List<Track>, onOpen: (String) -> Unit) {
    var downloaded by rememberSaveable { mutableStateOf(false) }
    FeaturePage("Listen / unwind", "Find your rhythm.", "A soundtrack for whatever comes next.") {
        item { Artwork("YOUR DAILY MIX", Modifier.fillMaxWidth().height(170.dp)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(!downloaded, { downloaded = false }, { Text("For you") })
                FilterChip(downloaded, { downloaded = true }, { Text("Downloaded") })
            }
        }
        items(tracks.filter { !downloaded || it.downloaded }, key = { it.id }) { track ->
            ContentCard(track.title, track.artist + " · " + track.duration +
                if (track.downloaded) " · Offline sample" else "", "♫", { onOpen(track.id) })
        }
        item { PrototypeNote("Downloaded is a sample filter. Audio files will be added later.") }
    }
}

@Composable
internal fun NowPlayingScreen(track: Track?, playing: Boolean, onToggle: () -> Unit, onNext: () -> Unit) {
    if (track == null) { Text("Track unavailable"); return }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artworkSize = minOf(maxWidth - 40.dp, (maxHeight * .4f).coerceIn(140.dp, 240.dp))
        FeaturePage("Your daily mix", track.title, track.artist) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Artwork("ON REPEAT", Modifier.size(artworkSize))
                }
            }
            item {
                LinearProgressIndicator(progress = { .32f }, modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("1:12 · Demo"); Text(track.duration)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = onToggle) { Text(if (playing) "Pause" else "Play") }
                    TextButton(onClick = onNext) { Text("Next →") }
                }
            }
            item { PrototypeNote("Playback controls simulate UI state. Background audio and notifications are not connected yet.") }
        }
    }
}
