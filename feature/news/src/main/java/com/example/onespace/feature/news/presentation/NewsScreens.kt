package com.example.onespace.feature.news.presentation

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

/** First story gets an editorial featured treatment; the rest stay compact cards. */
@Composable
private fun FeaturedStory(article: Article, onOpen: (String) -> Unit) {
    Card(onClick = { onOpen(article.id) }) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(article.category.uppercase(), color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium)
            Text(article.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(article.category + " · " + article.readTime,
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
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf("all") }
    val q = searchQuery.trim()
    val base = if (q.isEmpty()) articles
        else articles.filter { it.title.contains(q, true) || it.category.contains(q, true) }
    val topicChips = articles.map { it.category }.filter { it.isNotBlank() }.distinct()
    val visible = when {
        filter == "foryou" -> base.filter { it.category in topics }
        filter == "saved" -> base.filter { it.id in saved }
        filter != "all" -> base.filter { it.category == filter }
        else -> base
    }
    FeaturePage {
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm bài báo") },
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
        if (q.isEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item { FilterChip(selected = filter == "all", onClick = { filter = "all" }, label = { Text("Tất cả") }) }
                    if (topics.isNotEmpty()) {
                        item { FilterChip(selected = filter == "foryou", onClick = { filter = "foryou" }, label = { Text("Dành cho bạn") }) }
                    }
                    items(topicChips, key = { it }) { cat ->
                        FilterChip(selected = filter == cat, onClick = { filter = cat }, label = { Text(cat) })
                    }
                }
            }
        }
        if (visible.isEmpty()) {
            item {
                Text(
                    if (q.isNotEmpty()) "Không có bài phù hợp"
                    else if (filter == "saved") "Chưa lưu bài nào"
                    else "Chưa có tin tức",
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
    Card(
        onClick = { onOpen(article.id) },
        modifier = Modifier.fillMaxWidth()
    ) {
            // Themed header per category, like video thumbs.
            val (icon, colors) = newsLook(article.category)
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(androidx.compose.ui.graphics.Brush.linearGradient(colors))
            ) {
                Icon(
                    icon, contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.align(Alignment.Center).size(48.dp)
                )
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(article.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(
                    article.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

/** Stable icon + gradient per news category. */
private fun newsLook(category: String): Pair<androidx.compose.ui.graphics.vector.ImageVector, List<androidx.compose.ui.graphics.Color>> = when (category) {
    "Thể thao" -> Icons.Filled.SportsSoccer to listOf(androidx.compose.ui.graphics.Color(0xFF00695C), androidx.compose.ui.graphics.Color(0xFF4DB6AC))
    "Công nghệ" -> Icons.Filled.Devices to listOf(androidx.compose.ui.graphics.Color(0xFF283593), androidx.compose.ui.graphics.Color(0xFF7986CB))
    "Du lịch" -> Icons.Filled.Flight to listOf(androidx.compose.ui.graphics.Color(0xFFE65100), androidx.compose.ui.graphics.Color(0xFFFFB74D))
    "Thiên nhiên" -> Icons.Filled.Park to listOf(androidx.compose.ui.graphics.Color(0xFF2E7D32), androidx.compose.ui.graphics.Color(0xFF81C784))
    "Ẩm thực" -> Icons.Filled.Restaurant to listOf(androidx.compose.ui.graphics.Color(0xFF8D6E63), androidx.compose.ui.graphics.Color(0xFFFFCC80))
    else -> Icons.Filled.Newspaper to listOf(androidx.compose.ui.graphics.Color(0xFF37474F), androidx.compose.ui.graphics.Color(0xFF78909C))
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
    quizAnswers: Map<Int, Int> = emptyMap(),
    onQuizAnswer: (Int, Int) -> Unit = { _, _ -> },
    onOpenRelated: (String) -> Unit = {}
) {
    if (article == null) { Text("Chưa có bài viết này"); return }
    var fontScale by rememberSaveable(article.id) { mutableStateOf(1f) }
    var quizOfferDone by rememberSaveable(article.id) { mutableStateOf(false) }
    var showQuiz by rememberSaveable(article.id) { mutableStateOf(false) }
    val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
        fontSize = MaterialTheme.typography.bodyLarge.fontSize * fontScale,
        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * fontScale
    )
    FeaturePage {
        // Compact header: category + title + icon-only actions on the right.
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(article.category.uppercase(), color = MaterialTheme.colorScheme.primary,
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
                        contentDescription = if (saved) "Bỏ xem sau" else "Xem sau",
                        tint = if (saved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onToggleDownload) {
                    Icon(
                        if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        contentDescription = if (downloaded) "Bỏ tải" else "Tải xuống",
                        tint = if (downloaded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // F-NEWS-05: TTS — tap to listen.
                IconButton(onClick = { if (ttsState.isPlaying) onTtsStop() else onListen() }) {
                    Icon(
                        if (ttsState.isPlaying) Icons.Filled.Pause else Icons.Filled.Headphones,
                        contentDescription = if (ttsState.isPlaying) "Dừng đọc" else "Nghe bài này",
                        tint = if (ttsState.isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                    val (icon, colors) = newsLook(article.category)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 8f)
                            .background(androidx.compose.ui.graphics.Brush.linearGradient(colors)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            icon, contentDescription = null,
                            tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
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
                    Text("Chơi nhanh — kiểm tra hiểu bài")
                }
            }
        }
        // F-NEWS-11: same-category stories.
        val relatedItems = related.filter { it.id != article.id && it.category == article.category }
        if (relatedItems.isNotEmpty()) {
            item(key = "related-section") { SectionTitle("Tin liên quan") }
            items(relatedItems, key = { "rel-" + it.id }) { other ->
                NewsCard(other, saved = false, onOpenRelated)
            }
        }
        // F-NEWS-10: quiz must ask before showing.
        if (showQuiz && article.quiz.isNotEmpty()) {
            item(key = "quiz-section") { SectionTitle("Kiểm tra nhanh") }
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
                    Text("Kết quả: $correct/${article.quiz.size} đúng",
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
                "Câu ${questionIndex + 1}: ${question.question}",
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
