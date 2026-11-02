package com.example.onespace.feature.video.presentation

import com.example.onespace.core.designsystem.fadingEdgeEnd
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryMusic
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
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.formatMsWhole
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.video.quiz.VideoQuizGenerator
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.model.VideoQuality
import com.example.onespace.domain.video.repository.ContinueEntry
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * F-VID-02: YouTube-style filter chips. "Tất cả" is the default and lists
 * the whole catalog; "Đề xuất" keeps the onboarding topics first.
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
    onOpenChannel: (String) -> Unit = {},
    cardActions: VideoCardActions? = null,
    volume: Float = 1f,
    onToggleMute: () -> Unit = {},
    systemVolume: Float? = null,
    savedVideo: Video? = null,
    watchLaterIds: Set<String> = emptySet(),
    onToggleWatchLater: (Video) -> Unit = {},
    onDismissSave: () -> Unit = {}
) {
    val searchState = com.example.onespace.core.designsystem.LocalSearchState.current
    val searchQuery = searchState.query
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
        // "Đề xuất" always exists; without onboarding topics it recommends the whole catalog.
        filter == "foryou" -> if (topics.isEmpty()) searched else searched.filter { it.topic in topics }
        // The catalog has no upload date: later entries are the newer uploads.
        filter == "latest" -> searched.asReversed()
        filter == "watched" -> emptyList()
        else -> searched.filter { it.topic == filter }
    }
    val topicChips = videos.map { it.topic }.filter { it.isNotBlank() }.distinct()
    // F-VID-05: videoId -> last watch position, for the card progress bars.
    val progressMap = history.associate { it.videoId to it.positionMs }

    val english = LocalEnglish.current
    FeaturePage {
        // Top-left speaker: tap to mute at once; the bar shows the phone volume while it changes.
        item { VolumeControl(volume, onToggleMute, systemVolume) }
        if (q.isEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.fadingEdgeEnd(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text(tr("Tất cả", "All")) })
                    }
                    item {
                        FilterChip(
                            selected = filter == "foryou",
                            onClick = { filter = "foryou" },
                            label = { Text(tr("Đề xuất", "Recommended")) }
                        )
                    }
                    // F-VID-05: history lives with the filters, not its own button.
                    item {
                        FilterChip(
                            selected = filter == "watched",
                            onClick = { filter = "watched" },
                            label = { Text(tr("Đã xem", "Watched")) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = filter == "latest",
                            onClick = { filter = "latest" },
                            label = { Text(tr("Mới nhất", "Latest")) }
                        )
                    }
                    items(topicChips, key = { it }) { topic ->
                        FilterChip(
                            selected = filter == topic,
                            onClick = { filter = topic },
                            label = { Text(topic.t()) }
                        )
                    }
                }
            }
            if (filter == "watched") {
                // F-VID-05: grouped by Hôm nay / Hôm qua / weekday / dd.MM.
                val groups = history
                    .sortedByDescending { it.updatedAt }
                    .groupBy { timeGroupLabel(it.updatedAt, english) }
                if (groups.isEmpty()) {
                    item {
                        Text(
                            tr("Chưa xem video nào.", "No videos watched yet."),
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
                    FeedCard(video, onOpen, watchedAt = progressMap[video.id], actions = cardActions)
                }
            }
        } else {
            // F-VID-03: channels first, then the matching videos — no
            // "Tất cả video" header while searching.
            if (channels.isNotEmpty()) {
                // Channel cards lead the results; the avatar tile makes them self-evident, no section header needed.
                items(channels, key = { "ch-" + it }) { creator ->
                    AppCard(onClick = { onOpenChannel(creator) }, modifier = Modifier.fillMaxWidth()) {
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
                    FeedCard(video, onOpen, watchedAt = progressMap[video.id], actions = cardActions)
                }
            } else {
                item {
                    Text(
                        tr("Không có video phù hợp", "No matching videos"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
                if (related.isNotEmpty()) {
                    item { SectionTitle(tr("Liên quan", "Related")) }
                    items(related, key = { "rel-" + it.id }) { video ->
                        FeedCard(video, onOpen, watchedAt = progressMap[video.id], actions = cardActions)
                    }
                }
            }
        }
    }
    savedVideo?.let { v ->
        SaveSheet(
            video = v,
            watchLaterActive = v.id in watchLaterIds,
            onToggleWatchLater = { onToggleWatchLater(v) },
            onDismiss = onDismissSave
        )
    }
}

/** Speaker button (mute toggle) with a bar that appears while the phone volume is being changed. */
@Composable
private fun VolumeControl(volume: Float, onToggleMute: () -> Unit, systemVolume: Float?) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(systemVolume) {
        if (systemVolume != null) {
            shown = true
            kotlinx.coroutines.delay(2000)
            shown = false
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onToggleMute) {
            Icon(
                if (volume <= 0f) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (volume <= 0f) tr("Bật tiếng", "Unmute") else tr("Tắt tiếng", "Mute"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        androidx.compose.animation.AnimatedVisibility(visible = shown && systemVolume != null) {
            LinearProgressIndicator(
                progress = { systemVolume ?: 0f },
                modifier = Modifier.width(140.dp).height(4.dp).clip(androidx.compose.foundation.shape.CircleShape)
            )
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
    showWatchedText: Boolean = false,
    actions: VideoCardActions? = null
) {
    AppCard(onClick = { onOpen(video.id) }, modifier = Modifier.fillMaxWidth()) {
        Column {
            VideoThumb(video, watchedAt)
            Column(Modifier.padding(start = 12.dp, end = if (actions != null) 4.dp else 12.dp, top = 10.dp, bottom = 10.dp)) {
              Row(verticalAlignment = Alignment.Top) {
                Text(
                    video.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    modifier = Modifier.weight(1f).padding(top = if (actions != null) 8.dp else 0.dp)
                )
                if (actions != null) VideoCardMenu(video, actions)
              }
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
                        tr("Đã xem đến ", "Watched up to ") + formatMsWhole(watchedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

/** What the "⋮" menu of a video card can do; the screen decides how each one is carried out. */
internal class VideoCardActions(
    val downloaded: (Video) -> Boolean,
    val onShare: (Video) -> Unit,
    val onToggleDownload: (Video) -> Unit,
    val onAddToPlaylist: (Video) -> Unit
)

/** Vertical "⋮" with Share, Download and Add to playlist, like the detail screen. */
@Composable
private fun VideoCardMenu(video: Video, actions: VideoCardActions) {
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = tr("Thêm tùy chọn", "More options"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(tr("Chia sẻ", "Share")) },
                leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null) },
                onClick = { open = false; actions.onShare(video) }
            )
            val done = actions.downloaded(video)
            DropdownMenuItem(
                text = { Text(if (done) tr("Bỏ tải", "Remove download") else tr("Tải xuống", "Download")) },
                leadingIcon = { Icon(if (done) Icons.Filled.DownloadDone else Icons.Filled.Download, contentDescription = null) },
                onClick = { open = false; actions.onToggleDownload(video) }
            )
            DropdownMenuItem(
                text = { Text(tr("Thêm vào Playlist", "Add to playlist")) },
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                onClick = { open = false; actions.onAddToPlaylist(video) }
            )
        }
    }
}

/** Topic-tinted tile: one look per topic, duration badge like YouTube. */
@Composable
private fun VideoThumb(video: Video, watchedAt: Long? = null) {
    val (icon, colors) = topicLook(video.topic, MaterialTheme.oneSpaceColors.topics)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(colors.brush())
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.9f),
            modifier = Modifier
                .align(Alignment.Center)
                .size(56.dp)
        )
        Surface(
            color = MaterialTheme.oneSpaceColors.mediaScrim,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text(
                video.duration,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.oneSpaceColors.onMedia,
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
                        .background(MaterialTheme.colorScheme.primary)
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
private fun timeGroupLabel(updatedAt: Long, english: Boolean): String {
    if (updatedAt <= 0) return if (english) "Earlier" else "Trước đây"
    val date = java.time.Instant.ofEpochMilli(updatedAt)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    return when {
        date == today -> if (english) "Today" else "Hôm nay"
        date == today.minusDays(1) -> if (english) "Yesterday" else "Hôm qua"
        date >= today.with(java.time.DayOfWeek.MONDAY) ->
            date.dayOfWeek.getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale(if (english) "en" else "vi")
            ).replaceFirstChar { it.uppercase() }
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("dd.MM"))
    }
}

private fun topicLook(topic: String, topics: TopicGradients): Pair<androidx.compose.ui.graphics.vector.ImageVector, GradientPair> = when (topic) {
    "Thiên nhiên" -> Icons.Filled.Park to topics.nature
    "Đời sống" -> Icons.Filled.LocationCity to topics.life
    "Nhẹ nhàng" -> Icons.Filled.Spa to topics.calm
    "Du lịch" -> Icons.Filled.Flight to topics.travel
    "Ẩm thực" -> Icons.Filled.Restaurant to topics.food
    "Thể thao" -> Icons.Filled.DirectionsRun to topics.sport
    else -> Icons.Filled.PlayCircle to topics.tech
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
    volume: Float = 1f,
    onVolume: (Float) -> Unit = {},
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
    quizCombo: Int = 0,
    quizRound: Int = 0,
    onQuizAnswered: (Boolean) -> Int = { 0 },
    relatedTitle: String = tr("Liên quan", "Related"),
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
    if (video == null) { Text(tr("Chưa có video này", "This video is not available")); return }
    var showGear by remember { mutableStateOf(false) }
    var showSave by remember { mutableStateOf(false) }
    val inAnyPlaylist = AppPlaylistStore.playlists.values.any { video.id in it.videoIds }
    val ended = totalMs > 0 && positionMs >= totalMs
    // F-VID-18: the end-of-video game must ask first, never auto-show.
    // Inside a playlist the video rolls into the next one instead.
    var showQuiz by rememberSaveable(video.id) { mutableStateOf(false) }
    // Controls appear on tap and fade out after 2s without touch; they stay up when paused.
    var controlsVisible by remember(video.id) { mutableStateOf(true) }
    var lastTouch by remember(video.id) { mutableStateOf(0) }
    fun touch() { lastTouch++ }
    LaunchedEffect(controlsVisible, lastTouch, playing, audioOnly) {
        if (controlsVisible && playing && !audioOnly) {
            kotlinx.coroutines.delay(2000)
            controlsVisible = false
        }
    }
    val controlsAlpha by androidx.compose.animation.core.animateFloatAsState(
        if (controlsVisible || audioOnly) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(350), label = "controls"
    )
    val showControls = controlsVisible || audioOnly || controlsAlpha > 0.01f

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
                                contentDescription = if (playing) tr("Đang phát", "Now playing") else tr("Đã tạm dừng", "Paused"),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                if (playing) tr("Đang phát âm thanh", "Playing audio") else tr("Đã tạm dừng", "Paused"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                // Tap shows the controls, double-tap skips 10s, hold extracts the MP3 (F-VID-10).
                Box(
                    Modifier
                        .matchParentSize()
                        .pointerInput(video.id) {
                            detectTapGestures(
                                onTap = { controlsVisible = !controlsVisible; touch() },
                                onDoubleTap = { onSeekBy(10_000) },
                                onLongPress = { onExtractMp3() }
                            )
                        }
                )
                // F-VID-17: speed and mute badges stay readable even when the controls are hidden.
                Row(
                    Modifier.align(Alignment.TopStart).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (speed != 1f) Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            speed.toString() + "x",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.oneSpaceColors.onMedia,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    if (muted) Surface(
                        color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.55f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            tr("Tắt tiếng", "Muted"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.oneSpaceColors.onMedia,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (showControls) {
                    val onMedia = MaterialTheme.oneSpaceColors.onMedia
                    Box(Modifier.matchParentSize().alpha(controlsAlpha)) {
                        Box(Modifier.matchParentSize().background(MaterialTheme.oneSpaceColors.mediaScrim.copy(alpha = 0.35f)))
                        // Playback settings: speed and quality only (volume lives on the system and the Video list).
                        IconButton(onClick = { touch(); showGear = true }, modifier = Modifier.align(Alignment.TopEnd)) {
                            Icon(Icons.Filled.Settings, contentDescription = tr("Cài đặt phát", "Playback settings"), tint = onMedia)
                        }
                        // Previous - back 10s - play/pause - forward 10s - next, inside the frame.
                        Row(
                            Modifier.align(Alignment.Center),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { touch(); onPrevVideo() }) {
                                Icon(Icons.Filled.SkipPrevious, contentDescription = tr("Video trước", "Previous video"),
                                    tint = onMedia, modifier = Modifier.size(32.dp))
                            }
                            IconButton(onClick = { touch(); onSeekBy(-10_000) }) {
                                Icon(Icons.Filled.Replay10, contentDescription = tr("Lùi 10 giây", "Back 10 seconds"),
                                    tint = onMedia, modifier = Modifier.size(30.dp))
                            }
                            FilledIconButton(
                                onClick = { touch(); onToggle() },
                                modifier = Modifier.size(56.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(
                                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"),
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            IconButton(onClick = { touch(); onSeekBy(10_000) }) {
                                Icon(Icons.Filled.Forward10, contentDescription = tr("Tới 10 giây", "Forward 10 seconds"),
                                    tint = onMedia, modifier = Modifier.size(30.dp))
                            }
                            IconButton(onClick = { touch(); onNextVideo() }) {
                                Icon(Icons.Filled.SkipNext, contentDescription = tr("Video tiếp", "Next video"),
                                    tint = onMedia, modifier = Modifier.size(32.dp))
                            }
                        }
                        // Elapsed time with sound-only and minimise, above the bar.
                        Row(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(start = 12.dp, bottom = 22.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                formatMsWhole(positionMs) + " / " + (if (totalMs > 0) formatMsWhole(totalMs) else video.duration),
                                style = MaterialTheme.typography.labelMedium, color = onMedia,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { touch(); onSoundOnly() }) {
                                Icon(Icons.Filled.Headphones, contentDescription = tr("Chỉ nghe âm thanh", "Audio only"), tint = onMedia)
                            }
                            IconButton(onClick = { touch(); onMinimize() }) {
                                Icon(Icons.Filled.PictureInPictureAlt, contentDescription = tr("Thu nhỏ", "Minimise"), tint = onMedia)
                            }
                        }
                    }
                }
                // Progress line on the bottom edge of the frame: a knob you drag while the
                // controls are up, a quiet thin line the rest of the time (like YouTube).
                if (showControls) ScrubBar(
                    positionMs, totalMs,
                    modifier = Modifier.align(Alignment.BottomCenter).alpha(controlsAlpha),
                    onSeek = { touch(); onSeekTo(it) },
                    trackColor = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.35f)
                ) else ScrubBar(
                    positionMs, totalMs,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    interactive = false,
                    trackHeight = 2.dp,
                    trackColor = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.25f)
                )
            }
        }
        item { PlaybackFeedback(buffering, suppressed, error) }
        // While listening inside a playlist the order and repeat toggles
        // stay visible, not a one-time pick.
        if (inPlaylist) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = playlistOrder == "shuffle",
                        onClick = onTogglePlaylistOrder,
                        label = {
                            Text(if (playlistOrder == "shuffle") tr("Ngẫu nhiên", "Shuffle") else tr("Tuần tự", "In order"))
                        },
                        leadingIcon = { Icon(Icons.Filled.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = playlistRepeat,
                        onClick = onTogglePlaylistRepeat,
                        label = { Text(tr("Lặp lại", "Repeat")) },
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
                }
                // Channel · length, then the three actions with their labels.
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TextButton(onClick = onShare) {
                        Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(tr("Chia sẻ", "Share"))
                    }
                    TextButton(onClick = onToggleDownload) {
                        Icon(
                            if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                            contentDescription = null, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (downloaded) tr("Đã tải", "Downloaded") else tr("Tải xuống", "Download"))
                    }
                    TextButton(onClick = { showSave = true }) {
                        Icon(
                            if (watchLaterActive || inAnyPlaylist) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                            contentDescription = null, modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (watchLaterActive || inAnyPlaylist) tr("Đã lưu", "Saved") else tr("Lưu", "Save"))
                    }
                }
            }
        }
        // F-VID-15: the end-of-video game offer sits below the player,
        // on the viewer's terms — never on top of them.
        if (ended && !inPlaylist && !showQuiz) {
            item(key = "quiz-offer") {
                OutlinedButton(
                    onClick = { showQuiz = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) {
                    Icon(Icons.Filled.SportsEsports, contentDescription = null,
                        modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Chơi nhanh: câu đố về video này, từ 20 điểm, đúng liên tiếp được thêm", "Quick play: a quiz about this video, from 20 points, more for streaks"))
                }
            }
        }
        if (showQuiz) {
            item { EndOfVideoQuiz(video = video, pool = related, startRound = quizRound, combo = quizCombo, onAnswered = onQuizAnswered) }
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
                tr("Chạm video để phát/dừng, chạm hai lần để tua 10 giây, giữ để trích MP3.", "Tap the video to play or pause, double-tap to skip 10 seconds, hold to extract the MP3.")
            )
        }
    }
    if (showSave) {
        SaveSheet(
            video = video,
            watchLaterActive = watchLaterActive,
            onToggleWatchLater = onWatchLater,
            onDismiss = { showSave = false }
        )
    }
    if (showGear) {
        PlaybackSettingsSheet(
            speed = speed,
            onSpeed = onSpeed,
            quality = quality,
            onQuality = { onQuality(it); showGear = false },
            onDismiss = { showGear = false }
        )
    }
}

/**
 * F-VID-17: the playback settings sheet — speed and
 * quality as two evenly spaced label + control sections split by thin
 * dividers. Section labels share one typography and never shrink below the
 * option text they introduce.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybackSettingsSheet(
    speed: Float,
    onSpeed: (Float) -> Unit,
    quality: String,
    onQuality: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SheetSectionLabel(tr("Tốc độ phát", "Playback speed"))
            SpeedPicker(speed, onSpeed)
            HorizontalDivider()
            SheetSectionLabel(tr("Chất lượng", "Quality"))
            QualityPicker(quality, onQuality)
        }
    }
}

/** One playback sheet section heading — one style for every section. */
@Composable
private fun SheetSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall)
}

/** F-VID-17: four discrete speeds on one stepped slider, marks under the stops. */
@Composable
private fun SpeedPicker(speed: Float, onSpeed: (Float) -> Unit) {
    // Four steps only: a compact slider beats a list.
    val steps = listOf(0.5f, 1f, 1.5f, 2f)
    Column {
        Slider(
            value = steps.indexOf(speed).coerceAtLeast(0).toFloat(),
            onValueChange = { onSpeed(steps[it.toInt()]) },
            valueRange = 0f..3f,
            steps = 2
        )
        // Same width as the slider above, so each mark sits under its stop.
        StepMarks(Modifier.fillMaxWidth(), steps.map { it.toString() + "x" })
    }
}

// Mirrors the Material3 Slider geometry: the 4dp handle rests 2dp inside
// each end and the middle stops inset by the 8dp track corner.
private val SliderHandleWidth = 4.dp
private val SliderTrackCorner = 8.dp

/** Marks centered exactly under each thumb stop of an evenly stepped slider. */
@Composable
private fun StepMarks(modifier: Modifier, marks: List<String>) {
    Layout(content = {
        marks.forEach {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }, modifier = modifier) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val width = constraints.maxWidth
        val inset = SliderHandleWidth.roundToPx() / 2
        val corner = SliderTrackCorner.roundToPx()
        val track = width - 2 * inset
        val last = placeables.lastIndex
        layout(width, placeables.maxOf { it.height }) {
            placeables.forEachIndexed { index, placeable ->
                val center = when (index) {
                    0 -> inset
                    last -> width - inset
                    else -> inset + corner + (track - 2 * corner) * index / last
                }
                placeable.placeRelative(center - placeable.width / 2, 0)
            }
        }
    }
}

/** F-VID-06: four equal cells — two rows of two, Auto first. */
@Composable
private fun QualityPicker(quality: String, onQuality: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        VideoQuality.options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { q ->
                    FilterChip(
                        selected = quality == q,
                        onClick = { onQuality(q) },
                        label = {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(q, style = MaterialTheme.typography.bodyMedium)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
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
            SectionTitle(tr("Lưu video", "Save video"))
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
                Text(tr("Xem sau", "Watch later"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
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
                label = { Text(tr("Playlist mới", "New playlist")) },
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
            ) { Text(tr("Tạo", "Create")) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * F-VID-18: a short question after the video. The type rotates (who posted it, how long,
 * same topic, true/false) and correct answers in a row raise the reward (combo).
 */
@Composable
internal fun EndOfVideoQuiz(
    video: Video,
    pool: List<Video>,
    startRound: Int,
    combo: Int,
    onAnswered: (Boolean) -> Int,
    random: kotlin.random.Random = kotlin.random.Random.Default
) {
    val firstRound = remember(video.id) { startRound }
    var asked by remember(video.id) { mutableIntStateOf(0) }
    val english = LocalEnglish.current
    val quiz = remember(video.id, asked, english) {
        VideoQuizGenerator.generate(
            video, pool, firstRound + asked, random,
            english = english, localize = { Translations.english(it) }
        )
    }
    var picked by remember(quiz) { mutableStateOf<Int?>(null) }
    var reward by remember(quiz) { mutableIntStateOf(0) }
    val comboBefore = remember(quiz) { combo }
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val answered = picked
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    quiz.kind.label.t(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (combo > 0) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocalFireDepartment, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "Combo x$combo",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            Text(quiz.prompt, style = MaterialTheme.typography.titleMedium)
            quiz.options.forEachIndexed { index, option ->
                val isRight = index == quiz.correctIndex
                val feedback = MaterialTheme.oneSpaceColors.feedback
                val showRight = answered != null && isRight
                val showWrong = answered != null && !isRight && index == answered
                val container = when {
                    showRight -> feedback.container
                    showWrong -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                }
                val content = when {
                    showRight -> feedback.onContainer
                    showWrong -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
                OutlinedButton(
                    onClick = {
                        if (picked == null) {
                            picked = index
                            haptic.performHapticFeedback(
                                if (isRight) androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm
                                else androidx.compose.ui.hapticfeedback.HapticFeedbackType.Reject
                            )
                            reward = onAnswered(isRight)
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = container, contentColor = content
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (showRight || showWrong) {
                        Icon(
                            if (showRight) Icons.Filled.CheckCircle else Icons.Filled.Close,
                            contentDescription = if (showRight) tr("Đáp án đúng", "Correct answer") else tr("Bạn chọn sai", "You picked the wrong one"),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(option)
                }
            }
            if (answered != null) {
                val correct = answered == quiz.correctIndex
                Text(
                    if (correct) {
                        tr("Chính xác! +$reward điểm", "Correct! +$reward points") + if (comboBefore + 1 >= 2) " (combo x${comboBefore + 1})" else ""
                    } else {
                        quiz.explanation + if (comboBefore > 0) tr(" Combo x$comboBefore bị ngắt.", " Combo x$comboBefore ended.") else ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (correct) MaterialTheme.oneSpaceColors.feedback.success else MaterialTheme.colorScheme.error
                )
                Button(onClick = { asked += 1 }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (correct) tr("Câu tiếp theo (thêm điểm combo)", "Next question (more combo points)") else tr("Thử câu khác", "Try another"))
                }
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
        item { SectionTitle(tr("Video của kênh", "Video from the channel")) }
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
    val orderLabel = if (settings.order == "shuffle") tr("Ngẫu nhiên", "Shuffle") else tr("Tuần tự", "In order")
    FeaturePage {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (items.isEmpty()) "0 video"
                    else items.size.toString() + " video · " + orderLabel +
                        if (settings.repeat) tr(" · Lặp lại", " · Repeat") else "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (items.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(onClick = { onPlay(items) }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("Phát", "Play"))
                        }
                        IconButton(onClick = { AppPlaylistStore.toggleOrder(name) }) {
                            Icon(Icons.Filled.Shuffle, contentDescription = tr("Phát ngẫu nhiên", "Shuffle play"),
                                tint = if (settings.order == "shuffle") MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { AppPlaylistStore.toggleRepeat(name) }) {
                            Icon(Icons.Filled.Repeat, contentDescription = tr("Lặp lại", "Repeat"),
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
