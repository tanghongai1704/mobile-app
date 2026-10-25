package com.example.onespace.domain.news.usecase

import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.news.model.TtsSegment
import com.example.onespace.domain.news.repository.TtsEngine

class ListenArticle(private val engine: TtsEngine) {
    suspend operator fun invoke(article: Article) {
        val segments = article.body
            .split("\n\n")
            .filter { it.isNotBlank() }
            .mapIndexed { index, text -> TtsSegment(index, text.trim()) }
        engine.prepare(segments)
        engine.play()
    }
}

class PauseTts(private val engine: TtsEngine) {
    suspend operator fun invoke() = engine.pause()
}

class StopTts(private val engine: TtsEngine) {
    suspend operator fun invoke() = engine.stop()
}
