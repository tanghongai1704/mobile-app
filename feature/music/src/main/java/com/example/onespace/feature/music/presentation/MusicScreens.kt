package com.example.onespace.feature.music.presentation

import androidx.compose.animation.core.animateFloat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.example.onespace.core.common.formatMsWhole
import com.example.onespace.core.designsystem.*
import com.example.onespace.core.designsystem.AppPlaylistStore
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.port.PlaybackStatus
import com.example.onespace.domain.session.port.MusicPlayback

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicScreen(
    tracks: List<Track>,
    favorites: Set<String>,
    downloaded: Set<String>,
    extracted: List<com.example.onespace.domain.music.repository.ExtractedAudio> = emptyList(),
    onOpen: (String) -> Unit,
    onOpenExtracted: (com.example.onespace.domain.music.repository.ExtractedAudio) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onToggleDownload: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenArtist: (String) -> Unit = {}
) {
    // F-MUS-06: search over title and artist.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val q = searchQuery.trim()
    val base = if (q.isEmpty()) tracks
        else tracks.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    // F-MUS-06: matching artists lead the results, like Video.
    val artists = if (q.isEmpty()) emptyList()
        else tracks.map { it.artist }.distinct().filter { it.contains(q, true) }
    val relatedTracks = if (q.isEmpty() || base.isNotEmpty()) emptyList()
        else tracks.filter { t ->
            q.lowercase().split(' ').any { w -> w.length > 1 &&
                (t.title.lowercase().contains(w) || t.artist.lowercase().contains(w)) }
        }
    // F-MUS-08: "Dành cho bạn" = suggestions from favorites, merged into the chip.
    val loved = base.filter { it.id in favorites }
    val suggestedIds = suggestedOf(tracks, favorites).map { it.id }.toSet()
    val genreChips = listOf("Pop", "US-UK", "Việt", "Lo-fi", "Acoustic", "Chill")
    val visible = when {
        filter == "foryou" -> base.filter { it.id in suggestedIds || it.id in favorites }
        filter == "loved" -> loved
        filter == "extracted" -> emptyList() // only the extracted section shows
        filter != "all" -> base.filter { it.artist.contains(filter, true) }
        else -> base
    }
    FeaturePage {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm bài hát hoặc nghệ sĩ") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                singleLine = true
            )
        }
        if (searchQuery.isBlank()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("Tất cả") }) }
                    item { FilterChip(selected = filter == "foryou", onClick = { filter = "foryou" }, label = { Text("Dành cho bạn") }) }
                    item { FilterChip(selected = filter == "loved", onClick = { filter = "loved" }, label = { Text("Yêu thích") }) }
                    item { FilterChip(selected = filter == "extracted", onClick = { filter = "extracted" }, label = { Text("Từ video") }) }
                    items(genreChips, key = { it }) { g ->
                        FilterChip(selected = filter == g, onClick = { filter = g }, label = { Text(g) })
                    }
                }
            }
            items(visible, key = { it.id }) { track ->
                TrackCard(
                    track,
                    favorite = track.id in favorites,
                    downloaded = track.id in downloaded,
                    onOpen, onToggleFavorite, onToggleDownload,
                    onOpenArtist
                )
            }
            // "Từ video" is a filter chip, not a bottom section.
            if (filter == "extracted" && extracted.isNotEmpty()) {
                item { SectionTitle("Từ video của bạn") }
                items(extracted, key = { "ext-" + it.videoId }) { entry ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenExtracted(entry) }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.LibraryMusic, contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                listOf(entry.creator, entry.duration).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            if (artists.isNotEmpty()) {
                items(artists, key = { "ar-" + it }) { artist ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenArtist(artist) }
                            .padding(vertical = 10.dp)
                    ) {
                        val (grad, letter) = artistLook(artist)
                        Box(
                            Modifier.size(40.dp).background(
                                androidx.compose.ui.graphics.Brush.linearGradient(grad),
                                androidx.compose.foundation.shape.CircleShape
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(letter, color = androidx.compose.ui.graphics.Color.White)
                        }
                        Column {
                            Text(artist, style = MaterialTheme.typography.titleSmall)
                            Text(
                                tracks.count { it.artist == artist }.toString() + " bài hát",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            if (base.isNotEmpty()) {
                items(base, key = { it.id }) { track ->
                    TrackCard(track, track.id in favorites, track.id in downloaded,
                        onOpen, onToggleFavorite, onToggleDownload, onOpenArtist)
                }
            } else {
                item {
                    Text("Không có bài hát phù hợp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                if (relatedTracks.isNotEmpty()) {
                    item { SectionTitle("Liên quan") }
                    items(relatedTracks, key = { "rel-" + it.id }) { track ->
                        TrackCard(track, track.id in favorites, track.id in downloaded,
                            onOpen, onToggleFavorite, onToggleDownload, onOpenArtist)
                    }
                }
            }
        }
    }
}

/** F-MUS-08: no favorites yet means no suggestions either. */
private fun suggestedOf(tracks: List<Track>, favorites: Set<String>): List<Track> {
    if (favorites.isEmpty()) return emptyList()
    val artists = tracks.filter { it.id in favorites }.map { it.artist }.toSet()
    return tracks.filter { it.artist in artists && it.id !in favorites }.take(5)
}

@Composable
private fun TrackCard(
    track: Track,
    favorite: Boolean,
    downloaded: Boolean,
    onOpen: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onToggleDownload: (String, String, String) -> Unit = { _, _, _ -> },
    onOpenArtist: (String) -> Unit = {}
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(track.id) }
            .padding(vertical = 8.dp)
    ) {
        // Artist avatar: gradient tile with the artist's initial so
        // different artists are visually distinguishable.
        val (grad, letter) = artistLook(track.artist)
        Box(
            Modifier.size(48.dp).background(
                androidx.compose.ui.graphics.Brush.linearGradient(grad),
                androidx.compose.foundation.shape.CircleShape
            ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter,
                style = MaterialTheme.typography.titleLarge,
                color = androidx.compose.ui.graphics.Color.White
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.bodyLarge)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onOpenArtist(track.artist) }
                )
                Box(Modifier.size(3.dp).background(
                    MaterialTheme.colorScheme.outline,
                    androidx.compose.foundation.shape.CircleShape))
                Text(
                    track.duration,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // F-MUS-07: the badges are direct toggles.
        if (favorite) {
            IconButton(onClick = { onToggleFavorite(track.id) }) {
                Icon(Icons.Filled.Favorite, contentDescription = "Bỏ yêu thích",
                    tint = MaterialTheme.colorScheme.error)
            }
        }
        if (downloaded) {
            Text(
                "Đã tải",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable {
                    onToggleDownload(track.id, track.title, track.artist + " · " + track.duration)
                }
            )
        }
    }
}

/** Stable gradient + initial per artist so all songs by one artist look the same. */
private fun artistLook(artist: String): Pair<List<androidx.compose.ui.graphics.Color>, String> {
    val colors = listOf(
        listOf(androidx.compose.ui.graphics.Color(0xFF1565C0), androidx.compose.ui.graphics.Color(0xFF64B5F6)),
        listOf(androidx.compose.ui.graphics.Color(0xFF6A1B9A), androidx.compose.ui.graphics.Color(0xFFCE93D8)),
        listOf(androidx.compose.ui.graphics.Color(0xFF2E7D32), androidx.compose.ui.graphics.Color(0xFFA5D6A7)),
        listOf(androidx.compose.ui.graphics.Color(0xFFE65100), androidx.compose.ui.graphics.Color(0xFFFFCC80)),
        listOf(androidx.compose.ui.graphics.Color(0xFF00695C), androidx.compose.ui.graphics.Color(0xFF80CBC4)),
        listOf(androidx.compose.ui.graphics.Color(0xFF880E4F), androidx.compose.ui.graphics.Color(0xFFF48FB1)),
        listOf(androidx.compose.ui.graphics.Color(0xFF283593), androidx.compose.ui.graphics.Color(0xFF9FA8DA)),
        listOf(androidx.compose.ui.graphics.Color(0xFF4527A0), androidx.compose.ui.graphics.Color(0xFFB39DDB))
    )
    val hash = artist.hashCode()
    return colors[kotlin.math.abs(hash) % colors.size] to artist.take(1).uppercase()
}

/** Full player: artwork, meta, seek bar, transport. Identity is in the top bar ("← Music"). */
@Composable
internal fun NowPlayingScreen(
    track: Track?,
    playback: MusicPlayback,
    shuffle: Boolean = false,
    repeat: Boolean = false,
    favorite: Boolean = false,
    downloaded: Boolean = false,
    onToggle: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onToggleDownload: () -> Unit = {},
    onPlayQueueItem: (String) -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onRemoveFromQueue: (String) -> Unit = {},
    onMoveQueueItem: (Int, Int) -> Unit = { _, _ -> },
    onAddToPlaylist: (String) -> Unit = {}
) {
    if (track == null) { Text("Chưa có bài này"); return }
    var showQueue by remember { mutableStateOf(false) }
    var showSaveSheet by remember { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val artworkSize = minOf(maxWidth - 48.dp, (maxHeight * .3f).coerceIn(100.dp, 220.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            // Compact header: small artwork left, title/artist right.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Artwork(Modifier.size(64.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(track.title, style = MaterialTheme.typography.titleLarge)
                    Text(track.artist, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // Content actions on the right — thumb-friendly.
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (favorite) "Bỏ yêu thích" else "Yêu thích",
                            tint = if (favorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleDownload) {
                        Icon(Icons.Filled.Download, contentDescription = if (downloaded) "Bỏ tải xuống" else "Tải xuống",
                            tint = if (downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { showSaveSheet = true }) {
                        Icon(Icons.Filled.BookmarkBorder, contentDescription = "Lưu")
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            PlaybackFeedback(playback.status == PlaybackStatus.BUFFERING, playback.suppressed, playback.error)
            Slider(
                value = playback.positionMs.toFloat().coerceIn(0f, playback.totalMs.toFloat().coerceAtLeast(1f)),
                onValueChange = { onSeekTo(it.toLong()) },
                enabled = playback.totalMs > 0,
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
            // YouTube-style: skip back / play-pause / skip forward.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onPrevious, enabled = playback.queue.size > 1) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Bài trước",
                        modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onSeekTo((playback.positionMs - 10_000).coerceAtLeast(0)) }) {
                    Icon(Icons.Filled.Replay10, contentDescription = "Lùi 10 giây",
                        modifier = Modifier.size(28.dp))
                }
                FilledIconButton(onClick = onToggle, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (playback.playRequested && playback.error == null) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playback.playRequested && playback.error == null) "Tạm dừng" else "Phát",
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { onSeekTo((playback.positionMs + 10_000).coerceAtMost(playback.totalMs)) }) {
                    Icon(Icons.Filled.Forward10, contentDescription = "Tới 10 giây",
                        modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onNext, enabled = playback.queue.size > 1) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Tiếp theo",
                        modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            // F-MUS-05: the queue behind the player.
            TextButton(onClick = { showQueue = !showQueue }) {
                Text(if (showQueue) "Ẩn hàng đợi" else "Xem hàng đợi (" + playback.queue.size + " bài)")
            }
            if (showQueue) {
                // F-MUS-05: one-time queue, not a playlist — flows top to
                // bottom, delete on the right, artist clickable. Long-press
                // grabs a row; dragging past a neighbour swaps the two.
                val currentQueue by rememberUpdatedState(playback.queue)
                val moveItem by rememberUpdatedState(onMoveQueueItem)
                var draggingId by remember { mutableStateOf<String?>(null) }
                var dragOffset by remember { mutableFloatStateOf(0f) }
                var rowHeightPx by remember { mutableFloatStateOf(56f) }
                playback.queue.forEachIndexed { index, q ->
                    val current = q.id == playback.trackId
                    val dragging = q.id == draggingId
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (dragging) 1f else 0f)
                            .graphicsLayer {
                                shape = RoundedCornerShape(12.dp)
                                translationY = if (dragging) dragOffset else 0f
                                shadowElevation = if (dragging) 12f else 0f
                            }
                            .background(
                                if (dragging) MaterialTheme.colorScheme.surface else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .onSizeChanged { rowHeightPx = it.height.toFloat() }
                            .pointerInput(q.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingId = q.id
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        var from = currentQueue.indexOfFirst { it.id == q.id }
                                        while (from >= 0) {
                                            if (dragOffset >= rowHeightPx && from < currentQueue.lastIndex) {
                                                moveItem(from, from + 1)
                                                from += 1
                                                dragOffset -= rowHeightPx
                                            } else if (dragOffset <= -rowHeightPx && from > 0) {
                                                moveItem(from, from - 1)
                                                from -= 1
                                                dragOffset += rowHeightPx
                                            } else break
                                        }
                                    },
                                    onDragEnd = { draggingId = null; dragOffset = 0f },
                                    onDragCancel = { draggingId = null; dragOffset = 0f }
                                )
                            }
                            .clickable { onPlayQueueItem(q.id) }
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            (index + 1).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(24.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                q.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (current) FontWeight.Bold else FontWeight.Normal,
                                color = if (current) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                q.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (current) MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.clickable { onOpenArtist(q.artist) }
                            )
                        }
                        // "Đang phát" badge: equalizer icon + label, vertically centered,
                        // to the right — distinct from the title.
                        if (current) {
                            val isPlaying = playback.playRequested && playback.error == null
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Animated equalizer bars only while playing.
                                if (isPlaying) EqualizerIcon()
                                Text(
                                    if (isPlaying) "Đang phát" else "Tạm dừng",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            "✕",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { onRemoveFromQueue(q.id) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
    if (showSaveSheet) {
        MusicSaveSheet(track = track, onDismiss = { showSaveSheet = false }, onAddToPlaylist = onAddToPlaylist)
    }
}

/** F-MUS-04: the tracks saved in one music playlist. */
@Composable
internal fun MusicPlaylistScreen(
    name: String,
    tracks: List<Track>,
    onOpen: (String) -> Unit,
    onDelete: (String) -> Unit = {},
    shuffle: Boolean = false,
    repeat: Boolean = false,
    onToggleShuffle: () -> Unit = {},
    onToggleRepeat: () -> Unit = {},
    onPlay: (List<Track>) -> Unit = { _ -> }
) {
    val items = tracks.filter { it.id in (AppPlaylistStore.playlists[name]?.trackIds ?: emptyList()) }
    FeaturePage {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (items.isEmpty()) "0 bài" else items.size.toString() + " bài",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { onDelete(name) }) { Text("Xóa playlist") }
                }
                if (items.isNotEmpty()) {
                    // Order and repeat are player-level toggles here; they stay
                    // visible on the Now Playing screen while listening.
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = { onPlay(items) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Phát")
                        }
                        IconButton(onClick = onToggleShuffle) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "Phát ngẫu nhiên",
                                tint = if (shuffle) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onToggleRepeat) {
                            Icon(Icons.Filled.Repeat, contentDescription = "Lặp lại",
                                tint = if (repeat) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        items(items, key = { it.id }) { track ->
            ContentCard(track.title, track.artist + " · " + track.duration, Icons.Filled.MusicNote, { onOpen(track.id) })
        }
    }
}




/** Trang nghệ sĩ: liệt kê mọi bài hát của một nghệ sĩ. */
@Composable
internal fun MusicArtistScreen(
    artist: String,
    tracks: List<Track>,
    onOpen: (String) -> Unit
) {
    val own = tracks.filter { it.artist == artist }
    FeaturePage {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val (grad, letter) = artistLook(artist)
                Box(
                    Modifier.size(72.dp).background(
                        androidx.compose.ui.graphics.Brush.linearGradient(grad),
                        androidx.compose.foundation.shape.CircleShape
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        letter,
                        style = MaterialTheme.typography.headlineMedium,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(artist, style = MaterialTheme.typography.titleLarge)
                    Text(
                        own.size.toString() + " bài hát",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { SectionTitle("Bài hát") }
        items(own, key = { it.id }) { track ->
            TrackCard(track, favorite = false, downloaded = false, onOpen = onOpen, onToggleFavorite = {}, onToggleDownload = { _, _, _ -> })
        }
    }
}


/** Sheet Lưu cho bài nhạc: thêm vào playlist có sẵn hoặc tạo mới. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MusicSaveSheet(
    track: Track,
    onDismiss: () -> Unit,
    onAddToPlaylist: (String) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle("Lưu bài hát")
            com.example.onespace.core.designsystem.AppPlaylistStore.playlists.forEach { (name, pl) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onAddToPlaylist(track.id)
                            onDismiss()
                        }
                        .padding(vertical = 8.dp)
                ) {
                    Icon(Icons.Filled.LibraryMusic, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(12.dp))
                    Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text(pl.trackIds.size.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Playlist mới") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    com.example.onespace.core.designsystem.AppPlaylistStore.addTrack(newName.trim(), track.id)
                    onDismiss()
                },
                enabled = newName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tạo") }
        }
    }
}


/** Animated equalizer bars for the "Đang phát" badge. */
@Composable
private fun EqualizerIcon() {
    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "eq")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(300),
            androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "bar1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.4f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(400),
            androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "bar2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0.9f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(350),
            androidx.compose.animation.core.RepeatMode.Reverse
        ), label = "bar3"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        listOf(bar1, bar2, bar3).forEach { h ->
            Box(
                Modifier
                    .size(width = 3.dp, height = (14 * h).dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}
