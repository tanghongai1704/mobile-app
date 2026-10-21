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
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.*
import com.example.onespace.domain.news.model.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewsScreen(articles: List<Article>, saved: Set<String>, onOpen: (String) -> Unit) {
    var onlySaved by rememberSaveable { mutableStateOf(false) }
    FeaturePage("Read / stay curious", "A fresh perspective.", "Good stories. A little more understanding.") {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !onlySaved,
                    onClick = { onlySaved = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    label = { Text("Latest") }
                )
                SegmentedButton(
                    selected = onlySaved,
                    onClick = { onlySaved = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    label = { Text("Saved (" + saved.size + ")") }
                )
            }
        }
        val visible = articles.filter { !onlySaved || it.id in saved }
        if (visible.isEmpty()) item {
            Text("Save a story to find it here.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(visible, key = { it.id }) { article ->
            ContentCard(article.title, article.category + " · " + article.readTime, Icons.Filled.Newspaper, { onOpen(article.id) })
        }
    }
}

@Composable
internal fun ArticleScreen(article: Article?, saved: Boolean, onBookmark: () -> Unit) {
    if (article == null) { Text("Article unavailable"); return }
    FeaturePage(article.category, article.title, "OneSpace Journal · " + article.readTime) {
        item { Artwork("THE JOURNAL", Modifier.fillMaxWidth().height(180.dp)) }
        item {
            OutlinedButton(onClick = onBookmark) {
                Icon(if (saved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                    contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (saved) "Saved ✓" else "Save story")
            }
        }
        item { Text(article.body, style = MaterialTheme.typography.bodyLarge) }
        item { PrototypeNote("Sample editorial content · Bookmarks are retained for this prototype session.") }
    }
}
