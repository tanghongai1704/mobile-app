package com.example.onespace.feature.news.presentation

import com.example.onespace.core.designsystem.fadingEdgeEnd
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Close
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
                Text(
                    article.category.t(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
@Composable
internal fun ArticleScreen(
    article: Article?,
    related: List<Article> = emptyList(),
    saved: Boolean,
    ttsState: com.example.onespace.domain.news.model.TtsState = com.example.onespace.domain.news.model.TtsState(),
    onListen: () -> Unit = {},
    onTtsStop: () -> Unit = {},
    onBookmark: () -> Unit,
    downloaded: Boolean = false,
    onToggleDownload: () -> Unit = {},
    onExtractAudio: () -> Unit = {},
    onReadComplete: () -> Unit = {},
    quizAnswers: Map<Int, Int> = emptyMap(),
    onQuizAnswer: (Int, Int) -> Unit = { _, _ -> },
    onOpenRelated: (String) -> Unit = {}
) {
    if (article == null) { Text(tr("Chưa có bài viết này", "This article is not available")); return }
    var fontScale by rememberSaveable(article.id) { mutableStateOf(1f) }
    var quizOfferDone by rememberSaveable(article.id) { mutableStateOf(false) }
    var showQuiz by rememberSaveable(article.id) { mutableStateOf(false) }
    var readCounted by rememberSaveable(article.id) { mutableStateOf(false) }
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * fontScale
    )
    // The quiz section is appended below the article; jump to it when opened.
    val quizListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    androidx.compose.runtime.LaunchedEffect(showQuiz, quizAnswers.size) {
        if (showQuiz && quizListState.layoutInfo.totalItemsCount > 0) {
            quizListState.animateScrollToItem(quizListState.layoutInfo.totalItemsCount - 1)
        }
    }
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
                IconButton(onClick = onBookmark) {
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
                // F-NEWS-05 / F-VID-10: visible button to extract as MP3.
                IconButton(onClick = onExtractAudio) {
                    Icon(
                        Icons.Filled.LibraryMusic,
                        contentDescription = tr("Trích MP3 vào nhạc", "Extract MP3 to music"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // TTS progress
        if (ttsState.totalSegments > 0 && ttsState.isPlaying) {
            item {
                LinearProgressIndicator(
                    progress = { ttsState.progress },
                    modifier = Modifier.fillMaxWidth()
                )
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
                                onLongPress = { onExtractAudio() }
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
        // F-NEWS-10: play button AFTER the body, at the end of the article.
        if (article.quiz.isNotEmpty() && !showQuiz) {
            item {
                OutlinedButton(
                    onClick = { showQuiz = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) {
                    Icon(Icons.Filled.SportsEsports, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(tr("Chơi nhanh — kiểm tra hiểu bài", "Quick play: check your understanding"))
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
                    Text(tr("Kết quả: $correct/${article.quiz.size} đúng", "Result: $correct/${article.quiz.size} correct"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
        }
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
