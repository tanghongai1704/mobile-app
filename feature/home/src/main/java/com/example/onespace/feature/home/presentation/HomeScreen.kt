package com.example.onespace.feature.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.formatMsWhole
import com.example.onespace.core.designsystem.*
import com.example.onespace.core.media.VideoSurface
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.port.MusicPlayback
import com.example.onespace.domain.session.port.VideoPlayback
import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.video.model.Video

data class HomeUiState(
    val videos: List<Video> = emptyList(),
    val tracks: List<Track> = emptyList(),
    val articles: List<Article> = emptyList()
)

data class GamificationUiState(
    val totalPoints: Int = 0,
    val streak: Int = 0,
    val missions: List<MissionUi> = emptyList()
)

data class MissionUi(
    val id: String,
    val title: String,
    val progress: Int = 0,
    val target: Int = 1,
    val completed: Boolean = false
) {
    val progressText: String
        @androidx.compose.runtime.Composable get() = if (completed) tr("Hoàn thành", "Done") else "$progress/$target"
}

/**
 * Discovery surface: resume where the user left off and a small set of
 * cross-category content picks. Content deeplinks only — never tab shortcuts,
 * which would duplicate the bottom navigation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    state: HomeUiState,
    gamification: GamificationUiState = GamificationUiState(),
    offline: Boolean = false,
    watchLater: Set<String> = emptySet(),
    onMissionClick: (String) -> Unit = {},
    onOpenVideo: (id: String, title: String) -> Unit,
    onPlayTrack: (id: String, title: String) -> Unit,
    onOpenArticle: (id: String) -> Unit,
    // Feed plays in place: the card IS the player (ADR-08 shared instance);
    // leaving Home is what turns playback into the floating window / dock.
    onPlayVideoAtHome: (id: String, title: String) -> Unit = { _, _ -> },
    onPlayMusicAtHome: (id: String, title: String) -> Unit = { _, _ -> },
    videoPlayer: androidx.media3.common.Player? = null,
    videoPlayback: VideoPlayback? = null,
    musicPlayback: MusicPlayback? = null,
    onToggleVideo: () -> Unit = {},
    onToggleMusic: () -> Unit = {},
    onSeekVideo: (positionMs: Long) -> Unit = {},
    onSeekMusic: (positionMs: Long) -> Unit = {}
) {
    // F-COM-11: one search field across all four sections.
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showWatchlist by remember { mutableStateOf(false) }
    // System back clears an active search before leaving the app.
    androidx.activity.compose.BackHandler(enabled = searchQuery.isNotEmpty()) {
        searchQuery = ""
    }
    val q = searchQuery.trim()
    val videos = if (q.isEmpty()) state.videos else state.videos.filter { it.title.contains(q, true) || it.creator.contains(q, true) }
    val tracks = if (q.isEmpty()) state.tracks else state.tracks.filter { it.title.contains(q, true) || it.artist.contains(q, true) }
    val articles = if (q.isEmpty()) state.articles else state.articles.filter { it.title.contains(q, true) || it.category.contains(q, true) }
    FeaturePage {
        item {
            androidx.compose.material3.OutlinedTextField(
                shape = androidx.compose.foundation.shape.CircleShape, colors = com.example.onespace.core.designsystem.AppFieldDefaults.colors(),
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { androidx.compose.material3.Text(tr("Tìm trong OneSpace", "Search OneSpace")) },
                leadingIcon = { androidx.compose.material3.Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        androidx.compose.material3.IconButton(onClick = { searchQuery = "" }) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Filled.Close,
                                contentDescription = tr("Xóa tìm kiếm", "Clear search")
                            )
                        }
                    }
                },
                singleLine = true
            )
        }
        if (q.isEmpty()) {
            item {
                // F-VID-04: the watchlist lives on Home.
                androidx.compose.material3.FilledTonalButton(onClick = { showWatchlist = true }) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    androidx.compose.material3.Text(
                        if (watchLater.isEmpty()) tr("Xem sau", "Watch later") else tr("Xem sau (", "Watch later (") + watchLater.size + ")"
                    )
                }
            }
        }
        if (q.isNotEmpty()) {
            if (videos.isEmpty() && tracks.isEmpty() && articles.isEmpty()) {
                item {
                    androidx.compose.material3.Text(
                        tr("Không có kết quả cho \"", "No results for \"") + q + "\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (videos.isNotEmpty()) {
                item { SectionTitle("Video") }
                items(videos.take(5), key = { "gs-v-" + it.id }) { video ->
                    ContentCard(video.title, video.creator + " · " + video.duration,
                        Icons.Filled.PlayCircle, { onOpenVideo(video.id, video.title) })
                }
            }
            if (tracks.isNotEmpty()) {
                item { SectionTitle("Nhạc") }
                items(tracks.take(5), key = { "gs-m-" + it.id }) { track ->
                    ContentCard(track.title, track.artist + " · " + track.duration,
                        Icons.Filled.MusicNote, { onPlayTrack(track.id, track.title) })
                }
            }
            if (articles.isNotEmpty()) {
                item { SectionTitle("Tin tức") }
                items(articles.take(5), key = { "gs-n-" + it.id }) { article ->
                    ContentCard(article.title, article.category.t() + " · " + article.readTime,
                        Icons.Filled.Newspaper, { onOpenArticle(article.id) })
                }
            }
        }
        // Gamification banner: XP + streak + missions
        if (gamification.missions.isNotEmpty() && q.isEmpty()) {
            item(key = "gamification") {
                GamificationBanner(gamification, onMissionClick)
            }
        }

        if (offline) {
            item {
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.material3.Text(
                        tr("Mất kết nối mạng — đang hiển thị nội dung đã lưu.", "You are offline. Showing saved content."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
        // Feed: content shows directly, interleaved and continuous —
        // the user scrolls through actual picks, not section links.
        if (q.isEmpty()) {
            item(key = "feed-title") { SectionTitle(tr("Đề xuất cho bạn", "Recommended for you")) }
            items(interleaveFeed(videos, tracks, articles), key = { it.key }) { post ->
                FeedPost(
                    post = post,
                    onPlayVideoAtHome = onPlayVideoAtHome,
                    onPlayMusicAtHome = onPlayMusicAtHome,
                    onOpenArticle = onOpenArticle,
                    onOpenVideo = onOpenVideo,
                    onOpenTrack = onPlayTrack,
                    videoPlayer = videoPlayer,
                    videoPlayback = videoPlayback,
                    musicPlayback = musicPlayback,
                    onToggleVideo = onToggleVideo,
                    onToggleMusic = onToggleMusic,
                    onSeekVideo = onSeekVideo,
                    onSeekMusic = onSeekMusic
                )
            }
        }
    }

    // F-VID-04: the watchlist opens on demand from Home.
    if (showWatchlist) {
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { showWatchlist = false }) {
            androidx.compose.foundation.lazy.LazyColumn(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp).fillMaxHeight(0.75f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { SectionTitle(tr("Xem sau", "Watch later")) }
                if (watchLater.isEmpty()) {
                    item {
                        androidx.compose.material3.Text(
                            tr("Chưa lưu video nào. Mở một video, bấm Lưu và chọn Xem sau.", "No videos saved yet. Open a video, tap Save and choose Watch later."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 24.dp)
                        )
                    }
                } else {
                    items(watchLater.mapNotNull { id -> state.videos.find { it.id == id } }, key = { it.id }) { video ->
                        ContentCard(
                            title = video.title,
                            subtitle = video.creator + " · " + video.duration,
                            icon = androidx.compose.material.icons.Icons.Filled.PlayCircle,
                            onClick = {
                                showWatchlist = false
                                onOpenVideo(video.id, video.title)
                            }
                        )
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun GamificationBanner(state: GamificationUiState, onMissionClick: (String) -> Unit = {}) {    // Neutral card with small tonal accents so the banner blends into the
    // feed instead of shouting over it.
    AppCard(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Header: points + streak chip
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Filled.Star, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        tr("${state.totalPoints} điểm", "${state.totalPoints} points"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (state.streak > 0) {
                    Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocalFireDepartment, contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                tr("${state.streak} ngày", "${state.streak} ${if (state.streak == 1) "day" else "days"}"),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))

            // Missions: each row jumps to the tab where it can be completed.
            state.missions.forEach { mission ->
                val done = mission.completed
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onMissionClick(mission.id) }
                        .background(
                            if (done) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (done) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        mission.title.t(),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        mission.progressText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (done) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Feed: one post per content item, interleaved video → music → article so
// the scroll never runs out of one kind. Content renders directly in the
// card (thumbnail block, title, meta) — a tap opens the real thing.
// ---------------------------------------------------------------------------

/** One interleaved feed slot; exactly one field is non-null. */
private data class FeedPost(
    val key: String,
    val video: Video? = null,
    val track: Track? = null,
    val article: Article? = null
)

private fun interleaveFeed(
    videos: List<Video>,
    tracks: List<Track>,
    articles: List<Article>
): List<FeedPost> = buildList {
    val n = maxOf(videos.size, tracks.size, articles.size)
    for (i in 0 until n) {
        videos.getOrNull(i)?.let { add(FeedPost("feed-v-" + it.id, video = it)) }
        tracks.getOrNull(i)?.let { add(FeedPost("feed-m-" + it.id, track = it)) }
        articles.getOrNull(i)?.let { add(FeedPost("feed-a-" + it.id, article = it)) }
    }
}

/** Stable gradient per id so the same video always gets the same cover. */
private fun idGradient(id: String, topics: TopicGradients): GradientPair =
    topics.all[Math.floorMod(id.hashCode(), topics.all.size)]

@Composable
private fun FeedPost(
    post: FeedPost,
    onPlayVideoAtHome: (id: String, title: String) -> Unit,
    onPlayMusicAtHome: (id: String, title: String) -> Unit,
    onOpenArticle: (id: String) -> Unit,
    onOpenVideo: (id: String, title: String) -> Unit,
    onOpenTrack: (id: String, title: String) -> Unit,
    videoPlayer: androidx.media3.common.Player?,
    videoPlayback: VideoPlayback?,
    musicPlayback: MusicPlayback?,
    onToggleVideo: () -> Unit,
    onToggleMusic: () -> Unit,
    onSeekVideo: (positionMs: Long) -> Unit,
    onSeekMusic: (positionMs: Long) -> Unit
) {
    post.video?.let {
        val current = videoPlayback?.takeIf { p -> p.videoId == it.id }
        VideoPostCard(
            video = it,
            player = videoPlayer,
            isCurrent = current != null,
            isPlaying = current?.playRequested == true,
            positionMs = current?.positionMs ?: 0L,
            totalMs = current?.totalMs ?: 0L,
            onPlayAtHome = { onPlayVideoAtHome(it.id, it.title) },
            onToggle = onToggleVideo,
            onSeekTo = onSeekVideo,
            onOpenDetail = { onOpenVideo(it.id, it.title) }
        )
    }
    post.track?.let {
        val current = musicPlayback?.takeIf { p -> p.trackId == it.id }
        MusicPostCard(
            track = it,
            isCurrent = current != null,
            isPlaying = current?.playing == true,
            positionMs = current?.positionMs ?: 0L,
            totalMs = current?.totalMs ?: 0L,
            onPlayAtHome = { onPlayMusicAtHome(it.id, it.title) },
            onToggle = onToggleMusic,
            onSeekTo = onSeekMusic,
            onOpenDetail = { onOpenTrack(it.id, it.title) }
        )
    }
    post.article?.let { ArticlePostCard(it) { onOpenArticle(it.id) } }
}

@Composable
private fun VideoPostCard(
    video: Video,
    player: androidx.media3.common.Player?,
    isCurrent: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    totalMs: Long,
    onPlayAtHome: () -> Unit,
    onToggle: () -> Unit,
    onSeekTo: (positionMs: Long) -> Unit,
    onOpenDetail: () -> Unit
) {
    AppCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail)
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .background(idGradient(video.id, MaterialTheme.oneSpaceColors.topics).brush())
        ) {
            if (isCurrent && player != null) {
                // Inline playback on the card — the same shared player instance
                // (ADR-08); the floating window takes over when leaving Home.
                VideoSurface(
                    player = player,
                    modifier = Modifier.fillMaxSize(),
                    onDoubleTap = onToggle
                )
            }
            // Round play button: start inline playback / pause-resume.
            Surface(
                onClick = { if (isCurrent) onToggle() else onPlayAtHome() },
                shape = CircleShape,
                color = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.92f),
                shadowElevation = 4.dp,
                modifier = Modifier.align(Alignment.Center).size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (isCurrent && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isCurrent) tr("Tạm dừng", "Pause") else tr("Phát ", "Play ") + video.title,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            if (!isCurrent) {
                Surface(
                    color = MaterialTheme.oneSpaceColors.mediaScrim,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp)
                ) {
                    Text(
                        video.duration,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.oneSpaceColors.onMedia,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                video.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                video.creator,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isCurrent) {
                FeedSeekbar(positionMs, totalMs, onSeekTo)
            }
        }
    }
}

@Composable
private fun MusicPostCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    positionMs: Long,
    totalMs: Long,
    onPlayAtHome: () -> Unit,
    onToggle: () -> Unit,
    onSeekTo: (positionMs: Long) -> Unit,
    onOpenDetail: () -> Unit
) {
    AppCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenDetail)
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(84.dp).clip(MaterialTheme.shapes.small)
                    .background(idGradient(track.id, MaterialTheme.oneSpaceColors.topics).brush()),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null,
                    tint = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.95f), modifier = Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Text(
                    track.artist + " · " + track.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isCurrent) {
                    FeedSeekbar(positionMs, totalMs, onSeekTo)
                }
            }
            if (isCurrent) {
                FilledIconButton(onClick = onToggle) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) tr("Tạm dừng", "Pause") else tr("Phát tiếp", "Resume")
                    )
                }
            } else {
                FilledIconButton(onClick = onPlayAtHome) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = tr("Phát ", "Play ") + track.title)
                }
            }
        }
    }
}

@Composable
private fun ArticlePostCard(article: Article, onOpen: () -> Unit) {
    AppCard(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    article.category,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Text(
                article.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
            Text(
                article.body.split("\\n\\n").first(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3, overflow = TextOverflow.Ellipsis
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpen) { Text(tr("Xem thêm", "Read more")) }
            }
            Text(
                article.readTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Compact draggable seekbar for the inline feed players: shows position,
 * drags to seek. Seeks once on release so the drag does not fight the
 * periodic position emissions.
 */
@Composable
private fun FeedSeekbar(
    positionMs: Long,
    totalMs: Long,
    onSeekTo: (positionMs: Long) -> Unit
) {
    if (totalMs <= 0) return  // nothing to seek yet
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Slider(
            value = if (dragging) dragValue
            else positionMs.toFloat().coerceIn(0f, totalMs.toFloat()),
            onValueChange = {
                dragging = true
                dragValue = it
            },
            onValueChangeFinished = {
                onSeekTo(dragValue.toLong())
                dragging = false
            },
            valueRange = 0f..totalMs.toFloat(),
            modifier = Modifier.fillMaxWidth()
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                formatMsWhole(if (dragging) dragValue.toLong() else positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                formatMsWhole(totalMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
