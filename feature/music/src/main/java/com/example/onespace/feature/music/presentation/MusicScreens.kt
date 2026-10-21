package com.example.onespace.feature.music.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.music.model.Track

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicScreen(tracks: List<Track>, onOpen: (String) -> Unit) {
    var downloaded by rememberSaveable { mutableStateOf(false) }
    FeaturePage("Listen / unwind", "Find your rhythm.", "A soundtrack for whatever comes next.") {
        item { Artwork("YOUR DAILY MIX", Modifier.fillMaxWidth().height(170.dp)) }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !downloaded,
                    onClick = { downloaded = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("For you") }
                )
                SegmentedButton(
                    selected = downloaded,
                    onClick = { downloaded = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Downloaded") }
                )
            }
        }
        items(tracks.filter { !downloaded || it.downloaded }, key = { it.id }) { track ->
            ContentCard(track.title, track.artist + " · " + track.duration +
                if (track.downloaded) " · Offline sample" else "", Icons.Filled.MusicNote, { onOpen(track.id) })
        }
        item { PrototypeNote("Downloaded is a sample filter. Audio files will be added later.") }
    }
}

@Composable
internal fun NowPlayingScreen(track: Track?, playing: Boolean, onToggle: () -> Unit, onNext: () -> Unit) {
    if (track == null) { Text("Track unavailable"); return }
    FeaturePage("Your daily mix", track.title, track.artist) {
        item { Artwork("ON REPEAT", Modifier.fillMaxWidth().aspectRatio(1f)) }
        item {
            LinearProgressIndicator(progress = { .32f }, modifier = Modifier.fillMaxWidth())
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("1:12 · Demo", style = MaterialTheme.typography.labelMedium); Text(track.duration, style = MaterialTheme.typography.labelMedium)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                FilledIconButton(onClick = onToggle) {
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play")
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                }
            }
        }
        item { PrototypeNote("Playback controls simulate UI state. Background audio and notifications are not connected yet.") }
    }
}
