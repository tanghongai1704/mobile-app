package com.example.onespace.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * F-NEWS-12: the article being read, kept at shell level so it can shrink
 * into a floating card when the reader leaves the News tab.
 */
object ReadingState {
    var articleId by mutableStateOf<String?>(null)
        private set
    var title by mutableStateOf("")
        private set

    fun open(id: String, articleTitle: String) {
        articleId = id
        title = articleTitle
    }

    fun close() {
        articleId = null
        title = ""
    }
}
