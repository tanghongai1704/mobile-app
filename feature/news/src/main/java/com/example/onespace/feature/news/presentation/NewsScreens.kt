package com.example.onespace.feature.news.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
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
internal fun NewsScreen(articles: List<Article>, saved: Set<String>, onOpen: (String) -> Unit) {
    var onlySaved by rememberSaveable { mutableStateOf(false) }
    val visible = articles.filter { !onlySaved || it.id in saved }
    FeaturePage {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !onlySaved,
                    onClick = { onlySaved = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Mới nhất") }
                )
                SegmentedButton(
                    selected = onlySaved,
                    onClick = { onlySaved = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Đã lưu (" + saved.size + ")") }
                )
            }
        }
        if (visible.isEmpty()) {
            item {
                EmptyState(EmptySpec(
                    icon = Icons.Filled.Newspaper,
                    title = if (onlySaved) "Chưa lưu bài nào" else "Chưa có tin tức",
                    cta = if (onlySaved) "Xem tin mới nhất" else "Làm mới",
                    onCta = { if (onlySaved) onlySaved = false }
                ))
            }
        }
        if (!onlySaved) visible.firstOrNull()?.let { featured ->
            item(key = "featured-" + featured.id) { FeaturedStory(featured, onOpen) }
        }
        items(visible.drop(if (!onlySaved) 1 else 0), key = { it.id }) { article ->
            ContentCard(article.title, article.category + " · " + article.readTime, Icons.Filled.Newspaper, { onOpen(article.id) })
        }
    }
}

/** Reading view: everything is the article. Identity is in the top bar ("← News"). */
@Composable
internal fun ArticleScreen(article: Article?, saved: Boolean, onBookmark: () -> Unit) {
    if (article == null) { Text("Chưa có bài viết này"); return }
    FeaturePage {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(article.category.uppercase(), color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium)
                Text(article.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(article.category + " · " + article.readTime,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            OutlinedButton(onClick = onBookmark) {
                Icon(if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Đã lưu ✓" else "Lưu bài viết")
            }
        }
        item { Text(article.body, style = MaterialTheme.typography.bodyLarge) }
    }
}
