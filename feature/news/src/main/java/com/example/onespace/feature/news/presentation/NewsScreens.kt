package com.example.onespace.feature.news.presentation

import com.example.onespace.core.designsystem.fadingEdgeEnd
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.news.model.Article
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** First story gets an editorial featured treatment; the rest stay compact cards. */
@Composable
private fun FeaturedStory(article: Article, onOpen: (String) -> Unit) {
    AppCard(onClick = { onOpen(article.id) }) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(article.category.t().uppercase(), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium)
            Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(article.category.t() + " · " + article.readTime,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewsScreen(
    articles: List<Article>,
    saved: Set<String>,
    topics: Set<String> = emptySet(),
    onOpen: (String) -> Unit
) {
    val searchState = com.example.onespace.core.designsystem.LocalSearchState.current
    val searchQuery = searchState.query
    var filter by rememberSaveable { mutableStateOf("all") }
    val q = searchQuery.trim()
    val base = if (q.isEmpty()) articles
        else articles.filter { it.title.contains(q, true) || it.category.contains(q, true) }
    val topicChips = articles.map { it.category }.filter { it.isNotBlank() }.distinct()
    val visible = when {
        filter == "foryou" -> if (topics.isEmpty()) base else base.filter { it.category in topics }
        // No publish date in the catalog: later entries are newer; featured = longest reads.
        filter == "latest" -> base.asReversed()
        filter == "featured" -> base.sortedByDescending { it.readMinutes() }.take(4)
        filter == "saved" -> base.filter { it.id in saved }
        filter != "all" -> base.filter { it.category == filter }
        else -> base
    }
    FeaturePage {
        if (q.isEmpty()) {
            item {
                LazyRow(
                    modifier = Modifier.fadingEdgeEnd(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text(tr("Tất cả", "All")) }) }
                    item { FilterChip(selected = filter == "foryou", onClick = { filter = "foryou" }, label = { Text(tr("Đề xuất", "Recommended")) }) }
                    item { FilterChip(selected = filter == "latest", onClick = { filter = "latest" }, label = { Text(tr("Mới nhất", "Latest")) }) }
                    item { FilterChip(selected = filter == "featured", onClick = { filter = "featured" }, label = { Text(tr("Nổi bật", "Featured")) }) }
                    items(topicChips, key = { it }) { cat ->
                        FilterChip(selected = filter == cat, onClick = { filter = cat }, label = { Text(cat.t()) })
                    }
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Text(
                    if (q.isNotEmpty()) tr("Không có bài phù hợp", "No matching articles")
                    else if (filter == "saved") tr("Chưa lưu bài nào", "No articles saved")
                    else tr("Chưa có tin tức", "No news yet"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            }
        }
        items(visible, key = { it.id }) { article ->
            NewsCard(article, saved.contains(article.id), onOpen)
        }
    }
}

/** Article card: themed image as focal point, title + source below. */
@Composable
private fun NewsCard(article: Article, saved: Boolean, onOpen: (String) -> Unit, onExtractAudio: () -> Unit = {}) {
    AppCard(
        onClick = { onOpen(article.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
            // Themed header per category, like video thumbs.
            val (icon, colors) = newsLook(article.category, MaterialTheme.oneSpaceColors.topics)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(colors.brush())
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center).size(48.dp)
                )
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        article.category.t(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    // Reading time on the same line, bottom right, when the article can be read.
                    if (article.readMinutes() > 0) Text(
                        article.readMinutes().let { tr("$it phút đọc", "$it min read") },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

/** Stable icon + gradient per news category. */
private fun newsLook(category: String, topics: TopicGradients): Pair<androidx.compose.ui.graphics.vector.ImageVector, GradientPair> = when (category) {
    "Thể thao" -> Icons.Filled.SportsSoccer to topics.sport
    "Công nghệ" -> Icons.Filled.Devices to topics.tech
    "Du lịch" -> Icons.Filled.Flight to topics.travel
    "Thiên nhiên" -> Icons.Filled.Park to topics.nature
    "Ẩm thực" -> Icons.Filled.Restaurant to topics.food
    else -> Icons.Filled.Newspaper to topics.neutral
}

/** Reading view: everything is the article. Identity is in the top bar ("← News"). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArticleScreen(
    article: Article?,
    related: List<Article> = emptyList(),
    saved: Boolean,
    ttsState: com.example.onespace.domain.news.model.TtsState = com.example.onespace.domain.news.model.TtsState(),
    onListen: () -> Unit = {},
    onTtsStop: () -> Unit = {},
    onSeekTts: (Float) -> Unit = {},
    onSetTtsVolume: (Float) -> Unit = {},
    onBookmark: () -> Unit,
    downloaded: Boolean = false,
    onToggleDownload: () -> Unit = {},
    onExtractAudio: ((Boolean) -> Unit) -> Unit = { it(false) },
    onReadComplete: () -> Unit = {},
    quizAnswers: Map<Int, Int> = emptyMap(),
    onQuizAnswer: (Int, Int) -> Unit = { _, _ -> },
    onOpenRelated: (String) -> Unit = {},
    musicPlaying: Boolean = false,
    onPlayMusic: () -> Unit = {}
) {
    if (article == null) { Text(tr("Chưa có bài viết này", "This article is not available")); return }
    var fontScale by rememberSaveable(article.id) { mutableStateOf(1f) }
    var quizOfferDone by rememberSaveable(article.id) { mutableStateOf(false) }
    var showQuiz by rememberSaveable(article.id) { mutableStateOf(false) }
    // Skip the quiz offer entirely (X button on the card), like the video quiz card.
    var quizDismissed by rememberSaveable(article.id) { mutableStateOf(false) }
    var showExtract by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val msgExtracted = tr("Đã đọc bài báo thành MP3 và lưu vào thư viện Nhạc (Từ video của bạn)", "Article read aloud and saved to your Music library (From your videos)")
    val msgExtractedDemo = tr("Máy chưa có giọng đọc tiếng Việt nên đã lưu âm thanh mẫu vào thư viện Nhạc", "No Vietnamese voice on this device, so a sample audio was saved to your Music library")
    var readCounted by rememberSaveable(article.id) { mutableStateOf(false) }
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * fontScale
    )
    val quizListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Save confirmation rides a snackbar at the bottom of the reader,
    // like the floating video's "Đã lưu vào Xem sau".
    val snackbarHostState = remember { SnackbarHostState() }
    val msgSavedReadLater = tr("Đã thêm vào Xem sau", "Added to Read later")
    // No auto-scroll: opening the quiz or answering a question must never move
    // the page — only the user's own scrolling does.
    // F-COM-05: the READ mission counts once the reader reaches the end of
    // the article or stays on it for 20 seconds — whichever comes first.
    androidx.compose.runtime.LaunchedEffect(article.id, readCounted) {
        if (readCounted) return@LaunchedEffect
        kotlinx.coroutines.withTimeoutOrNull(20_000) {
            androidx.compose.runtime.snapshotFlow {
                val info = quizListState.layoutInfo
                info.totalItemsCount > 0 &&
                    (info.visibleItemsInfo.lastOrNull()?.index ?: -1) >= info.totalItemsCount - 1
            }.first { it }
        }
        readCounted = true
        onReadComplete()
    }
    Box(Modifier.fillMaxSize()) {
        FeaturePage(quizListState) {
            // Compact header: category + title + icon-only actions on the right.
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(article.category.t().uppercase(), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(article.title, style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))

                    }
                }
            }
            // Action row: 4 content buttons + font A−/A+ on the right.
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // F-NEWS-02: font controls on the LEFT.
                    IconButton(onClick = { if (fontScale > 0.8f) fontScale -= 0.15f }, modifier = Modifier.size(32.dp)) {
                        Text("A−", style = MaterialTheme.typography.labelMedium)
                    }
                    IconButton(onClick = { if (fontScale < 1.6f) fontScale += 0.15f }, modifier = Modifier.size(32.dp)) {
                        Text("A+", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.weight(1f))
                    // Actions on the RIGHT — thumb-friendly.
                    // Saving confirms with a snackbar (removing stays quiet).
                    IconButton(onClick = {
                        if (!saved) scope.launch { snackbarHostState.showSnackbar(msgSavedReadLater) }
                        onBookmark()
                    }) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = if (saved) tr("Bỏ xem sau", "Remove from Read later") else tr("Xem sau", "Read later"),
                            tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onToggleDownload) {
                        Icon(
                            if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                            contentDescription = if (downloaded) tr("Bỏ tải", "Remove download") else tr("Tải xuống", "Download"),
                            tint = if (downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // F-NEWS-05: TTS — tap to listen.
                    IconButton(onClick = { if (ttsState.isPlaying) onTtsStop() else onListen() }) {
                        Icon(
                            if (ttsState.isPlaying) Icons.Filled.Pause else Icons.Filled.Headphones,
                            contentDescription = if (ttsState.isPlaying) tr("Dừng đọc", "Stop reading") else tr("Nghe bài này", "Listen to this article"),
                            tint = if (ttsState.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // F-NEWS-05 / F-VID-10: visible button to extract as MP3 (asks first).
                    IconButton(onClick = { showExtract = true }) {
                        Icon(
                            Icons.Filled.LibraryMusic,
                            contentDescription = tr("Trích MP3 vào nhạc", "Extract MP3 to music"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // TTS progress: a knob the reader can drag, plus the speaker and
            // volume bar on the right — the same pair the music player uses.
            if (ttsState.totalSegments > 0 && ttsState.isPlaying) {
                item {
                    var lastVolume by remember { mutableFloatStateOf(1f) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        com.example.onespace.core.designsystem.ScrubBar(
                            positionMs = (ttsState.progress * 1000f).toLong(),
                            totalMs = 1000L,
                            onSeek = { onSeekTts(it / 1000f) },
                            trackHeight = 3.dp,
                            knobRadius = 6.dp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (ttsState.volume > 0f) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                            contentDescription = if (ttsState.volume > 0f) tr("Tắt tiếng đọc", "Mute reading") else tr("Bật tiếng đọc", "Unmute reading"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    if (ttsState.volume > 0f) lastVolume = ttsState.volume
                                    onSetTtsVolume(if (ttsState.volume > 0f) 0f else lastVolume)
                                }
                        )
                        com.example.onespace.core.designsystem.ScrubBar(
                            positionMs = (ttsState.volume * 1000f).toLong(),
                            totalMs = 1000L,
                            onSeek = { onSetTtsVolume(it / 1000f) },
                            trackHeight = 3.dp,
                            knobRadius = 5.dp,
                            modifier = Modifier.width(92.dp)
                        )
                    }
                }
            }
            // Body: split into paragraphs, interleave themed images
            // between every other paragraph (F-NEWS-01 illustration).
            // F-NEWS-05: long-press on the body to extract MP3.
            val paragraphs = article.body.split("\\n\\n")
            paragraphs.forEachIndexed { pIdx, para ->
                item(key = "p-$pIdx") {
                    Text(
                        para,
                        style = bodyStyle,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(article.id) {
                                detectTapGestures(
                                    onLongPress = { showExtract = true }
                                )
                            }
                    )
                }
                // Insert a themed image between every other paragraph.
                if (pIdx < paragraphs.size - 1 && pIdx % 2 == 1) {
                    item(key = "img-$pIdx") {
                        val (icon, colors) = newsLook(article.category, MaterialTheme.oneSpaceColors.topics)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 8f)
                                .background(colors.brush()),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                icon, contentDescription = null,
                                tint = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.7f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                }
                if (pIdx < paragraphs.size - 1) {
                    item(key = "sp-$pIdx") { Spacer(Modifier.height(12.dp)) }
                }
            }
            // F-NEWS-10: quiz offer AFTER the body, styled like the end-of-video quiz card.
            if (article.quiz.isNotEmpty() && !showQuiz && !quizDismissed) {
                item(key = "quiz-offer") {
                    Surface(
                        onClick = { showQuiz = true },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                Modifier.size(44.dp).background(
                                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.2f),
                                    MaterialTheme.shapes.medium
                                ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.SportsEsports, contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(24.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    tr("Chơi nhanh: Kiểm tra hiểu bài", "Quick play: check your understanding"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = androidx.compose.ui.graphics.Color.White
                                )
                                Text(
                                    tr("Trả lời đúng nhận điểm thưởng", "Correct answers earn points"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.8f)
                                )
                            }
                            IconButton(onClick = { quizDismissed = true }) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = tr("Đóng", "Close"),
                                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
            // F-NEWS-10: quiz must ask before showing.
            if (showQuiz && article.quiz.isNotEmpty()) {
                item(key = "quiz-section") { SectionTitle(tr("Kiểm tra nhanh", "Quick check")) }
                items(article.quiz.size, key = { "quiz-$it" }) { qIndex ->
                    QuizQuestionCard(
                        question = article.quiz[qIndex],
                        questionIndex = qIndex,
                        onAnswer = { selected -> onQuizAnswer(qIndex, selected) },
                        answered = quizAnswers[qIndex]
                    )
                }
                item(key = "quiz-result") {
                    val correct = quizAnswers.count { (idx, ans) ->
                        article.quiz.getOrNull(idx)?.correctIndex == ans
                    }
                    if (quizAnswers.size == article.quiz.size) {
                        Text(tr("Kết quả: $correct/${article.quiz.size} đúng · +${correct * 15} điểm", "Result: $correct/${article.quiz.size} correct · +${correct * 15} points"),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            // F-NEWS-11: same-category stories.
            val relatedItems = related.filter { it.id != article.id && it.category == article.category }
            if (relatedItems.isNotEmpty()) {
                item(key = "related-section") { SectionTitle(tr("Tin liên quan", "Related news")) }
                items(relatedItems, key = { "rel-" + it.id }) { other ->
                    NewsCard(other, saved = false, onOpenRelated)
                }
            }
        }
        // The save confirmation floats above the reader's bottom edge.
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
    if (showExtract) {
        AlertDialog(
            onDismissRequest = { showExtract = false },
            icon = { Icon(Icons.Filled.LibraryMusic, contentDescription = null) },
            title = { Text(tr("Trích xuất MP3", "Extract MP3")) },
            text = {
                Text(tr(
                    "Lưu âm thanh của bài báo này vào thư viện nhạc để nghe lại bất cứ lúc nào.",
                    "Save the audio of this article to your music library to listen anytime."
                ))
            },
            confirmButton = {
                TextButton(onClick = {
                    showExtract = false
                    onExtractAudio { real ->
                        android.widget.Toast.makeText(context, if (real) msgExtracted else msgExtractedDemo, android.widget.Toast.LENGTH_LONG).show()
                    }
                }) { Text(tr("Trích xuất", "Extract")) }
            },
            dismissButton = { TextButton(onClick = { showExtract = false }) { Text(tr("Hủy", "Cancel")) } }
        )
    }
    // (quiz dialog removed — replaced by a play button below the article)
}

@Composable
private fun QuizQuestionCard(
    question: com.example.onespace.domain.news.model.QuizQuestion,
    questionIndex: Int,
    onAnswer: (Int) -> Unit,
    answered: Int? = null
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                tr("Câu ${questionIndex + 1}: ${question.question}", "Question ${questionIndex + 1}: ${question.question}"),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            question.options.forEachIndexed { optIndex, option ->
                val isSelected = answered == optIndex
                val isCorrect = answered != null && optIndex == question.correctIndex
                val isWrong = isSelected && !isCorrect
                OutlinedButton(
                    onClick = { if (answered == null) onAnswer(optIndex) },
                    enabled = answered == null,
                    colors = when {
                        isCorrect -> ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        isWrong -> ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                        else -> ButtonDefaults.outlinedButtonColors()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(option)
                }
            }
        }
    }
}

/** Minutes in strings like "5 phút đọc"; 0 when the article has no reading time. */
private fun com.example.onespace.domain.news.model.Article.readMinutes(): Int =
    Regex("\\d+").find(readTime)?.value?.toIntOrNull() ?: 0
