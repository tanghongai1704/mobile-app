package com.example.onespace.feature.music.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.formatMsWhole
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.port.MusicPlayback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicScreen(tracks: List<Track>, onOpen: (String) -> Unit) {
    var downloaded by rememberSaveable { mutableStateOf(false) }
    val visible = tracks.filter { !downloaded || it.downloaded }
    FeaturePage {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !downloaded,
                    onClick = { downloaded = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Dành cho bạn") }
                )
                SegmentedButton(
                    selected = downloaded,
                    onClick = { downloaded = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Đã tải") }
                )
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(EmptySpec(
                    icon = Icons.Filled.MusicNote,
                    title = if (downloaded) "Không có bài tải xuống trong mục này" else "Kho nhạc trống",
                    cta = if (downloaded) "Bật nhạc ngẫu nhiên" else "Làm mới",
                    onCta = {
                        // Always offer a next step: random play takes the listener straight in.
                        if (downloaded) tracks.randomOrNull()?.let { onOpen(it.id) }
                    }
                ))
            }
        }
        items(visible, key = { it.id }) { track ->
            ContentCard(track.title, track.artist + " · " + track.duration +
                if (track.downloaded) " · Mẫu ngoại tuyến" else "", Icons.Filled.MusicNote, { onOpen(track.id) })
        }
    }
}

/** Full player: artwork, meta, seek bar, transport. Identity is in the top bar ("← Music"). */
@Composable
internal fun NowPlayingScreen(
    track: Track?,
    playback: MusicPlayback,
    onToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    if (track == null) { Text("Chưa có bài này"); return }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artworkSize = minOf(maxWidth - 48.dp, (maxHeight * .3f).coerceIn(100.dp, 220.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Artwork(Modifier.size(artworkSize))
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(track.title, style = MaterialTheme.typography.headlineSmall)
                Text(track.artist, style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            Slider(
                value = playback.positionMs.toFloat().coerceIn(0f, playback.totalMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { onSeekTo(it.toLong()) },
                valueRange = 0f..playback.totalMs.toFloat().coerceAtLeast(1f),
                modifier = Modifier.fillMaxWidth()
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatMsWhole(playback.positionMs), style = MaterialTheme.typography.labelMedium)
                Text(
                    if (playback.totalMs > 0) formatMsWhole(playback.totalMs) else track.duration,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Bài trước")
                }
                FilledIconButton(onClick = onToggle) {
                    Icon(
                        if (playback.playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.playing) "Tạm dừng" else "Phát"
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Tiếp theo")
                }
            }
        }
    }
}
