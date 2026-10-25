package com.example.onespace.feature.video.presentation

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.formatMsWhole
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.ContinueEntry
import kotlinx.coroutines.delay

/**
 * F-VID-02: YouTube-style filter chips. "Tất cả" is the default and lists
 * the whole catalog; "Dành cho bạn" keeps the onboarding topics first.
 * F-VID-04 / F-VID-05: watchlist and history live behind compact buttons,
 * not as top-level feed sections.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideoScreen(
    videos: List<Video>,
    topics: Set<String>,
    history: List<ContinueEntry>,
    onOpen: (String) -> Unit,
    onOpenChannel: (String) -> Unit = {}
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }

    val q = searchQuery.trim()
    val searched = if (q.isEmpty()) videos
        else videos.filter { it.title.contains(q, true) || it.creator.contains(q, true) }
    // F-VID-03: matching channels first, then the videos.
    val channels = if (q.isEmpty()) emptyList()
        else videos.map { it.creator }.distinct().filter { it.contains(q, true) }
    // Related = shares a word with the query but does not match it.
    val related = if (q.isEmpty() || searched.isNotEmpty()) emptyList()
        else videos.filter { v ->
            q.lowercase().split(' ').any { w -> w.length > 1 &&
                (v.title.lowercase().contains(w) || v.topic.lowercase().contains(w)) }
        }
    val filtered = when {
        filter == "all" -> searched
        filter == "foryou" -> searched.filter { it.topic in topics }
        filter == "watched" -> emptyList()
        else -> searched.filter { it.topic == filter }
    }
    val topicChips = videos.map { it.topic }.filter { it.isNotBlank() }.distinct()
    // F-VID-05: videoId -> last watch position, for the card progress bars.
    val progressMap = history.associate { it.videoId to it.positionMs }

    FeaturePage {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm video hoặc kênh") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    // One tap clears the whole query.
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = "Xóa tìm kiếm")
                        }
                    }
                },
                singleLine = true
            )
        }
        if (q.isEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("Tất cả") })
                    }
                    if (topics.isNotEmpty()) {
                        item {
                            FilterChip(
                                selected = filter == "foryou",
                                onClick = { filter = "foryou" },
                                label = { Text("Dành cho bạn") }
                            )
                        }
                    }
                    // F-VID-05: history lives with the filters, not its own button.
                    item {
                        FilterChip(
                            selected = filter == "watched",
                            onClick = { filter = "watched" },
                            label = { Text("Đã xem") }
                        )
                    }
                    items(topicChips, key = { it }) { topic ->
                        FilterChip(
                            selected = filter == topic,
                            onClick = { filter = topic },
                            label = { Text(topic) }
                        )
                    }
                }
            }
            if (filter == "watched") {
                // F-VID-05: grouped by Hôm nay / Hôm qua / weekday / dd.MM.
                val groups = history
                    .sortedByDescending { it.updatedAt }
                    .groupBy { timeGroupLabel(it.updatedAt) }
                if (groups.isEmpty()) {
                    item {
                        Text(
                            "Chưa xem video nào.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                }
                groups.forEach { (label, entries) ->
                    item { SectionTitle(label) }
                    items(entries, key = { label + it.videoId }) { entry ->
                        videos.find { it.id == entry.videoId }?.let { video ->
                            FeedCard(
                                video, onOpen,
                                watchedAt = entry.positionMs,
                                showWatchedText = true
                            )
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { video ->
                    FeedCard(video, onOpen, watchedAt = progressMap[video.id])
                }
            }
        } else {
            // F-VID-03: channels first, then the matching videos — no
            // "Tất cả video" header while searching.
            if (channels.isNotEmpty()) {
                // Channel cards lead the results; the avatar tile makes them self-evident, no section header needed.
                items(channels, key = { "ch-" + it }) { creator ->
                    Card(onClick = { onOpenChannel(creator) }, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Box(
                                Modifier.size(40.dp).background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    creator.take(1).uppercase(),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(creator, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    videos.count { it.creator == creator }.toString() + " video",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            if (searched.isNotEmpty()) {
                if (channels.isEmpty()) item { SectionTitle("Video") }
                items(searched, key = { it.id }) { video ->
                    FeedCard(video, onOpen, watchedAt = progressMap[video.id])
                }
            } else {
                item {
                    Text(
                        "Không có video phù hợp",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                if (related.isNotEmpty()) {
                    item { SectionTitle("Liên quan") }
                    items(related, key = { "rel-" + it.id }) { video ->
                        FeedCard(video, onOpen, watchedAt = progressMap[video.id])
                    }
                }
            }
        }
    }

}

/** One titled list inside a bottom sheet. */
@Composable
private fun SheetList(
    title: String,
    empty: String,
    count: Int,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    androidx.compose.foundation.lazy.LazyColumn(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).fillMaxHeight(0.75f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item { SectionTitle(title) }
        if (count == 0) {
            item {
                Text(
                    empty,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        } else {
            content()
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/**
 * F-VID-01: the illustration is the card's center — a themed tile per topic
 * so videos stay distinguishable; title, channel and duration are support
 * info below, styled differently from each other.
 */
@Composable
private fun FeedCard(
    video: Video,
    onOpen: (String) -> Unit,
    watchedAt: Long? = null,
    showWatchedText: Boolean = false
) {
    Card(onClick = { onOpen(video.id) }, modifier = Modifier.fillMaxWidth()) {
        Column {
            VideoThumb(video, watchedAt)
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(
                    video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Channel and duration are visually distinct, not one
                    // same-style line split by a dot.
                    Text(
                        video.creator,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Box(
                        Modifier
                            .size(3.dp)
                            .background(
                                MaterialTheme.colorScheme.outline,
                                androidx.compose.foundation.shape.CircleShape
                            )
                    )
                    Text(
                        video.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (showWatchedText && watchedAt != null && watchedAt > 0) {
                    Text(
                        "Đã xem đến " + formatMsWhole(watchedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/** Topic-tinted tile: one look per topic, duration badge like YouTube. */
@Composable
private fun VideoThumb(video: Video, watchedAt: Long? = null) {
    val (icon, colors) = topicLook(video.topic)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Brush.linearGradient(colors))
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
        )
        Surface(
            color = Color.Black.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text(
                video.duration,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        // F-VID-05: the last watch position as a YouTube-style bar
        // pinned to the bottom edge of the tile.
        if (watchedAt != null && watchedAt > 0) {
            val total = durationMsOf(video.duration)
            if (total > 0) {
                val fraction = (watchedAt.toFloat() / total).coerceIn(0.04f, 1f)
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(fraction)
                        .height(3.dp)
                        .background(MaterialTheme.colorScheme.error)
                )
            }
        }
    }
}

/** "1:00" / "0:46" in the catalog -> milliseconds for the progress bar. */
private fun durationMsOf(duration: String): Long {
    val parts = duration.split(":")
    return when (parts.size) {
        2 -> ((parts[0].toLongOrNull() ?: 0) * 60 + (parts[1].toLongOrNull() ?: 0)) * 1000
        3 -> ((parts[0].toLongOrNull() ?: 0) * 3600 + (parts[1].toLongOrNull() ?: 0) * 60 + (parts[2].toLongOrNull() ?: 0)) * 1000
        else -> 0
    }
}

/** F-VID-05: Hôm nay / Hôm qua / weekday within this week / dd.MM for older. */
private fun timeGroupLabel(updatedAt: Long): String {
    if (updatedAt <= 0) return "Trước đây"
    val date = java.time.Instant.ofEpochMilli(updatedAt)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    return when {
        date == today -> "Hôm nay"
        date == today.minusDays(1) -> "Hôm qua"
        date >= today.with(java.time.DayOfWeek.MONDAY) ->
            date.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale("vi")
            ).replaceFirstChar { it.uppercase() }
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))
    }
}

private fun topicLook(topic: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, List<androidx.compose.ui.graphics.Color>> = when (topic) {
    "Thiên nhiên" -> Icons.Filled.Park to listOf(Color(0xFF2E7D32), Color(0xFF81C784))
    "Đời sống" -> Icons.Filled.LocationCity to listOf(Color(0xFF37474F), Color(0xFF78909C))
    "Nhẹ nhàng" -> Icons.Filled.Spa to listOf(Color(0xFF6A1B9A), Color(0xFFBA68C8))
    "Du lịch" -> Icons.Filled.Flight to listOf(Color(0xFFE65100), Color(0xFFFFB74D))
    "Ẩm thực" -> Icons.Filled.Restaurant to listOf(Color(0xFF8D6E63), Color(0xFFFFCC80))
    "Thể thao" -> Icons.Filled.DirectionsRun to listOf(Color(0xFF00695C), Color(0xFF4DB6AC))
    else -> Icons.Filled.PlayCircle to listOf(Color(0xFF283593), Color(0xFF7986CB))
}

/**
 * Video detail. Everything manipulates the player box itself: tap toggles,
 * double-tap seeks, long-press extracts MP3 (F-VID-10); speed (F-VID-17)
 * and quality (F-VID-06) live in the gear menu on the video; the minimize
 * button sits in the video corner. Only content-level actions (share,
 * download, watch later, playlist) form a separate action row.
 */
@Composable
internal fun VideoDetailScreen(
    video: Video?,
    related: List<Video>,
    playing: Boolean,
    muted: Boolean,
    buffering: Boolean,
    suppressed: Boolean,
    error: String?,
    positionMs: Long,
    totalMs: Long,
    videoSurface: @Composable (Modifier, () -> Unit) -> Unit,
    audioOnly: Boolean = false,
    onAudioOnly: () -> Unit = {},
    onSoundOnly: () -> Unit = {},
    speed: Float = 1f,
    onSpeed: (Float) -> Unit = {},
    quality: String = "Auto",
    onQuality: (String) -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    onShare: () -> Unit = {},
    onWatchLater: () -> Unit = {},
    watchLaterActive: Boolean = false,
    downloaded: Boolean = false,
    onToggleDownload: () -> Unit = {},
    onExtractMp3: () -> Unit = {},
    onOpenArtist: (String) -> Unit = {},
    onQuizCorrect: () -> Unit = {},
    relatedTitle: String = "Liên quan",
    inPlaylist: Boolean = false,
    playlistOrder: String = "sequential",
    playlistRepeat: Boolean = false,
    onTogglePlaylistOrder: () -> Unit = {},
    onTogglePlaylistRepeat: () -> Unit = {},
    onOpenRelated: (String) -> Unit,
    onToggle: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onPrevVideo: () -> Unit = {},
    onNextVideo: () -> Unit = {},
    onMinimize: () -> Unit
) {
    if (video == null) { Text("Chưa có video này"); return }
    var showGear by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    val inAnyPlaylist = AppPlaylistStore.playlists.values.any { video.id in it.videoIds }
    val ended = totalMs > 0 && positionMs >= totalMs
    // F-VID-18: the end-of-video game must ask first, never auto-show.
    // Inside a playlist the video rolls into the next one instead.
    var quizOfferDone by rememberSaveable(video.id) { mutableStateOf(false) }
    var showQuiz by rememberSaveable(video.id) { mutableStateOf(false) }

    FeaturePage {
        item {
            Box(Modifier.fillMaxWidth()) {
                if (!audioOnly) {
                    videoSurface(Modifier.fillMaxWidth().height(230.dp)) { onSeekBy(10_000) }
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) "Đang phát" else "Đã tạm dừng",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                if (playing) "Đang phát âm thanh" else "Đã tạm dừng",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Gestures stay on the video box (F-VID-10: giữ để trích MP3).
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(video.id) {
                            detectTapGestures(
                                onTap = { onToggle() },
                                onDoubleTap = { onSeekBy(10_000) },
                                onLongPress = { onExtractMp3() }
                            )
                        }
                )
                // F-VID-06 / F-VID-17: one gear — speed as a slider,
                // quality as a list, two visually distinct sections.
                Box(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { showGear = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Cài đặt phát",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    DropdownMenu(expanded = showGear, onDismissRequest = { showGear = false }) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(0.72f)
                            ) {
                                Text("Tốc độ phát", style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (speed == 1f) "1x" else speed.toString() + "x",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                                )
                            }
                            // Four steps only: a compact slider beats a list.
                            val steps = listOf(0.5f, 1f, 1.5f, 2f)
                            Slider(
                                value = steps.indexOf(speed).coerceAtLeast(0).toFloat(),
                                onValueChange = { onSpeed(steps[it.toInt()]) },
                                valueRange = 0f..3f,
                                steps = 2,
                                modifier = Modifier.fillMaxWidth(0.72f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth(0.72f)
                            ) {
                                steps.forEach {
                                    Text(it.toString() + "x", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            HorizontalDivider(Modifier.padding(vertical = 8.dp))
                            Text("Chất lượng", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        listOf("Auto", "360p", "480p", "720p", "1080p").forEach { q ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (q == "Auto") "Auto (theo băng thông)" else q,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                onClick = { onQuality(q); showGear = false },
                                trailingIcon = {
                                    if (quality == q) Icon(
                                        Icons.Filled.CheckCircle, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                // F-VID-17: speed badge on the video while not default.
                if (speed != 1f) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Text(
                            speed.toString() + "x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                // Bottom-right corner: sound-only then minimize (PIP).
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // F-VID-09: sound only — the floating window becomes a
                    // music-style mini player.
                    IconButton(onClick = { onSoundOnly() }) {
                        Icon(Icons.Filled.Headphones, contentDescription = "Chỉ nghe âm thanh",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    IconButton(onClick = onMinimize) {
                        Icon(Icons.Filled.PictureInPictureAlt, contentDescription = "Thu nhỏ",
                            tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
                if (muted) {
                    Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.BottomStart).padding(8.dp)
                    ) {
                        Text(
                            "Tắt tiếng",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
        item { PlaybackFeedback(buffering, suppressed, error) }
        // YouTube-style: drag the bar to seek, double-tap skips ±10s.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Slider(
                    value = positionMs.toFloat().coerceIn(0f, totalMs.toFloat().coerceAtLeast(1f)),
                    onValueChange = { onSeekTo(it.toLong()) },
                    enabled = totalMs > 0,
                    valueRange = 0f..totalMs.toFloat().coerceAtLeast(1f),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatMsWhole(positionMs), style = MaterialTheme.typography.labelMedium)
                    Text(if (totalMs > 0) formatMsWhole(totalMs) else video.duration,
                        style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        // Skip prev / play / skip next — like Music, centered.
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onPrevVideo) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Video trước",
                        modifier = Modifier.size(32.dp))
                }
                IconButton(onClick = { onSeekBy(-10_000) }) {
                    Icon(Icons.Filled.Replay10, contentDescription = "Lùi 10 giây",
                        modifier = Modifier.size(28.dp))
                }
                FilledIconButton(onClick = onToggle, modifier = Modifier.size(56.dp)) {
                    Icon(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "Tạm dừng" else "Phát",
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { onSeekBy(10_000) }) {
                    Icon(Icons.Filled.Forward10, contentDescription = "Tới 10 giây",
                        modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onNextVideo) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Video tiếp",
                        modifier = Modifier.size(32.dp))
                }
            }
        }
        // While listening inside a playlist the order and repeat toggles
        // stay visible, not a one-time pick.
        if (inPlaylist) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = playlistOrder == "shuffle",
                        onClick = onTogglePlaylistOrder,
                        label = {
                            Text(if (playlistOrder == "shuffle") "Ngẫu nhiên" else "Tuần tự")
                        },
                        leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = playlistRepeat,
                        onClick = onTogglePlaylistRepeat,
                        label = { Text("Lặp lại") },
                        leadingIcon = { Icon(Icons.Filled.Repeat, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        }
        item {
            // Title + channel + 3 icon-only actions inline on the right.
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(video.title, style = MaterialTheme.typography.headlineSmall)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onOpenArtist(video.creator) }
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            video.creator + " · " + video.duration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Icon-only actions, larger and balanced with the title.
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(onClick = onShare) {
                            Icon(Icons.Filled.Share, contentDescription = "Chia sẻ",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp))
                        }
                        IconButton(onClick = onToggleDownload) {
                            Icon(
                                if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                                contentDescription = if (downloaded) "Bỏ tải" else "Tải xuống",
                                tint = if (downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(onClick = { showSave = true }) {
                            Icon(
                                if (watchLaterActive || inAnyPlaylist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = "Lưu",
                                tint = if (watchLaterActive || inAnyPlaylist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
        if (showQuiz) {
            item { EndOfVideoQuiz(video = video, others = related, onCorrect = onQuizCorrect) }
        }
        if (related.any { it.id != video.id }) {
            item { SectionTitle(relatedTitle) }
            items(related.filter { it.id != video.id }, key = { "related-" + it.id }) { other ->
                // Same card design as the feed: themed thumb as the focal
                // point, title + channel + duration below.
                FeedCard(other, onOpenRelated)
            }
        }
        item {
            PrototypeNote(
                "Chạm video để phát/dừng, chạm hai lần để tua 10 giây, giữ để trích MP3."
            )
        }
    }
    // F-VID-18: offer the game once, on the viewer's terms.
    if (ended && !inPlaylist && !quizOfferDone && !showQuiz) {
        AlertDialog(
            onDismissRequest = { quizOfferDone = true },
            title = { Text("Xem xong rồi!") },
            text = { Text("Chơi thử câu đố nhỏ về video này để nhận thêm 20 điểm?") },
            confirmButton = {
                TextButton(onClick = { showQuiz = true; quizOfferDone = true }) { Text("Chơi ngay") }
            },
            dismissButton = {
                TextButton(onClick = { quizOfferDone = true }) { Text("Để sau") }
            }
        )
    }
    if (showSave) {
        SaveSheet(
            video = video,
            watchLaterActive = watchLaterActive,
            onToggleWatchLater = onWatchLater,
            onDismiss = { showSave = false }
        )
    }
}

/** One action in the video action row: icon on top, small label below. */
@Composable
private fun ActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    highlight: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick).padding(horizontal = 4.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(26.dp)
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * F-VID-04 / F-VID-15: one "Lưu" entry point — watch later and playlists
 * live behind the same sheet, existing playlists first, new ones inline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveSheet(
    video: Video,
    watchLaterActive: Boolean,
    onToggleWatchLater: () -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionTitle("Lưu video")
            // F-VID-04: one row, no explanations.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleWatchLater() }
                    .padding(vertical = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                Text("Xem sau", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                if (watchLaterActive) {
                    Icon(
                        Icons.Filled.CheckCircle, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (AppPlaylistStore.playlists.isNotEmpty()) {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                AppPlaylistStore.playlists.forEach { (name, pl) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AppPlaylistStore.addVideo(name, video.id)
                            }
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(
                            Icons.Filled.LibraryMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(
                            pl.videoIds.size.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (video.id in pl.videoIds) {
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.CheckCircle, contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)
                            )
                        }
                    }
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
                    AppPlaylistStore.addVideo(newName.trim(), video.id)
                    newName = ""
                },
                enabled = newName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Tạo") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * F-VID-18: one small question after the video ends; a correct answer
 * earns 20 XP through the shared point repository.
 */
@Composable
private fun EndOfVideoQuiz(video: Video, others: List<Video>, onCorrect: () -> Unit = {}) {
    var picked by remember(video.id) { mutableStateOf<String?>(null) }
    val correct = video.topic.ifBlank { "Giải trí" }
    val options = (listOf(correct) + others.map { it.topic }.filter { it.isNotBlank() && it != correct })
        .distinct()
        .take(4)
        .let { if (correct in it) it else it + correct }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Bạn đã xem xong!", style = MaterialTheme.typography.titleMedium)
            Text(
                "Video này thuộc chủ đề nào?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            options.forEach { option ->
                val isRight = option == correct
                OutlinedButton(
                    onClick = {
                        if (picked == null) {
                            picked = option
                            if (isRight) onCorrect()
                        }
                    },
                    colors = if (picked != null && isRight) {
                        ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    } else ButtonDefaults.outlinedButtonColors()
                ) { Text(option) }
            }
            if (picked != null) {
                Text(
                    if (picked == correct) "Chính xác! +20 điểm" else "Tiếc quá, đúng là: " + correct,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (picked == correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** F-VID-14: artist profile — every video of one creator on one page. */
@Composable
internal fun ArtistProfileScreen(
    creator: String,
    videos: List<Video>,
    onOpen: (String) -> Unit,
    watchProgress: Map<String, Long> = emptyMap()
) {
    val own = videos.filter { it.creator == creator }
    FeaturePage {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            creator.take(1).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(creator, style = MaterialTheme.typography.titleLarge)
                    Text(
                        own.size.toString() + " video",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        item { SectionTitle("Video của kênh") }
        items(own, key = { it.id }) { video ->
            FeedCard(video, onOpen, watchedAt = watchProgress[video.id])
        }
    }
}

/** F-VID-15: the videos saved in one app-wide playlist. */
@Composable
internal fun VideoPlaylistScreen(
    name: String,
    videos: List<Video>,
    onOpen: (String) -> Unit,
    onPlay: (List<Video>) -> Unit = { _ -> },
    watchProgress: Map<String, Long> = emptyMap()
) {
    val items = videos.filter { it.id in (AppPlaylistStore.playlists[name]?.videoIds ?: emptyList()) }
    // Order and repeat stay visible and toggleable, before and during playback.
    val settings = AppPlaylistStore.settingsOf(name)
    val orderLabel = if (settings.order == "shuffle") "Ngẫu nhiên" else "Tuần tự"
    FeaturePage {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (items.isEmpty()) "0 video"
                    else items.size.toString() + " video · " + orderLabel +
                        if (settings.repeat) " · Lặp lại" else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (items.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = { onPlay(items) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Phát")
                        }
                        IconButton(onClick = { AppPlaylistStore.toggleOrder(name) }) {
                            Icon(Icons.Filled.Shuffle, contentDescription = "Phát ngẫu nhiên",
                                tint = if (settings.order == "shuffle") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { AppPlaylistStore.toggleRepeat(name) }) {
                            Icon(Icons.Filled.Repeat, contentDescription = "Lặp lại",
                                tint = if (settings.repeat) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        items(items, key = { it.id }) { video ->
            FeedCard(video, onOpen, watchedAt = watchProgress[video.id])
        }
    }
}
