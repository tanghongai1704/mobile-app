package com.example.onespace.domain.news.model

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int
) {
    init {
        require(options.size >= 2) { "Quiz needs at least 2 options" }
        require(correctIndex in options.indices) { "correctIndex out of bounds" }
    }
}

data class QuizResult(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val pointsEarned: Int
) {
    val scorePercent: Int get() = if (totalQuestions == 0) 0 else correctAnswers * 100 / totalQuestions
}
