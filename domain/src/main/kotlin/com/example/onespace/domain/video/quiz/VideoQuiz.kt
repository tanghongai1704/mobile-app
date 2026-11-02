package com.example.onespace.domain.video.quiz

import com.example.onespace.domain.video.model.Video
import kotlin.math.abs
import kotlin.random.Random

enum class QuizKind(val label: String) {
    CREATOR("Ai đăng video này?"),
    DURATION("Video dài bao lâu?"),
    SAME_TOPIC("Cùng chủ đề"),
    TRUE_FALSE("Đúng hay sai?")
}

data class VideoQuiz(
    val kind: QuizKind,
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int,
    /** Shown after answering, so a wrong answer still teaches something. */
    val explanation: String
) {
    init {
        require(options.size >= 2) { "A quiz needs at least 2 options" }
        require(correctIndex in options.indices) { "correctIndex out of bounds" }
        require(options.distinct().size == options.size) { "Options must be unique" }
    }
}

/** Builds one varied question about a video that the viewer has just watched. */
object VideoQuizGenerator {
    /**
     * [round] is how many quizzes the viewer already answered this session; it rotates the
     * question type so two quizzes in a row never feel the same.
     */
    fun generate(
        video: Video,
        pool: List<Video>,
        round: Int = 0,
        random: Random = Random.Default,
        english: Boolean = false,
        /** Maps a catalog topic to the display language (topics are Vietnamese keys in the data). */
        localize: (String) -> String = { it }
    ): VideoQuiz {
        val others = pool.filter { it.id != video.id }
        val feasible = QuizKind.entries.filter { canAsk(it, video, others) }
        val kind = feasible[Math.floorMod(round + abs(video.id.hashCode()), feasible.size)]
        return when (kind) {
            QuizKind.CREATOR -> creatorQuiz(video, others, random, english)
            QuizKind.DURATION -> durationQuiz(video, others, random, english)
            QuizKind.SAME_TOPIC -> sameTopicQuiz(video, others, random, english, localize)
            QuizKind.TRUE_FALSE -> trueFalseQuiz(video, others, random, english, localize)
        }
    }

    private fun canAsk(kind: QuizKind, video: Video, others: List<Video>): Boolean = when (kind) {
        QuizKind.CREATOR -> others.map { it.creator }.filter { it != video.creator }.distinct().size >= 3
        QuizKind.DURATION -> true
        QuizKind.SAME_TOPIC -> video.topic.isNotBlank() &&
            others.any { it.topic == video.topic } &&
            others.filter { it.topic != video.topic }.map { it.title }.distinct().size >= 3
        QuizKind.TRUE_FALSE -> true
    }

    private fun creatorQuiz(video: Video, others: List<Video>, random: Random, en: Boolean): VideoQuiz {
        val wrong = others.map { it.creator }.filter { it != video.creator }.distinct().shuffled(random).take(3)
        return build(
            QuizKind.CREATOR,
            if (en) "Which channel posted “${video.title}”?" else "Video “${video.title}” do kênh nào đăng?",
            video.creator, wrong, random,
            if (en) "This video is from the channel ${video.creator}." else "Đây là video của kênh ${video.creator}."
        )
    }

    private fun durationQuiz(video: Video, others: List<Video>, random: Random, en: Boolean): VideoQuiz {
        val real = others.map { it.duration }.filter { it != video.duration }.distinct().shuffled(random)
        val seconds = toSeconds(video.duration)
        val synthetic = listOf(seconds + 10, seconds + 25, seconds + 40, (seconds - 5).coerceAtLeast(5), seconds * 2)
            .map { format(it) }.filter { it != video.duration }
        val wrong = (real + synthetic).distinct().take(3)
        return build(
            QuizKind.DURATION,
            if (en) "How long is “${video.title}”?" else "Video “${video.title}” dài bao lâu?",
            video.duration, wrong, random,
            if (en) "This video is ${video.duration} long." else "Video này dài ${video.duration}."
        )
    }

    private fun sameTopicQuiz(
        video: Video, others: List<Video>, random: Random, en: Boolean, localize: (String) -> String
    ): VideoQuiz {
        val right = others.filter { it.topic == video.topic }.map { it.title }.distinct().shuffled(random).first()
        val wrong = others.filter { it.topic != video.topic }.map { it.title }.distinct().shuffled(random).take(3)
        return build(
            QuizKind.SAME_TOPIC,
            if (en) "Which video shares the topic “${localize(video.topic)}” with the one you just watched?"
            else "Video nào cùng chủ đề “${video.topic}” với video bạn vừa xem?",
            right, wrong, random,
            if (en) "“$right” is also in the topic ${localize(video.topic)}." else "“$right” cũng thuộc chủ đề ${video.topic}."
        )
    }

    private fun trueFalseQuiz(
        video: Video, others: List<Video>, random: Random, en: Boolean, localize: (String) -> String
    ): VideoQuiz {
        val otherCreators = others.map { it.creator }.filter { it != video.creator }.distinct()
        val otherTopics = others.map { it.topic }.filter { it.isNotBlank() && it != video.topic }.distinct()
        val statementTrue = random.nextBoolean()
        val aboutCreator = otherCreators.isNotEmpty() && (video.topic.isBlank() || otherTopics.isEmpty() || random.nextBoolean())
        val actual = if (aboutCreator) video.creator else video.topic.ifBlank { "Giải trí" }
        val shown = if (statementTrue) actual
        else (if (aboutCreator) otherCreators else otherTopics.ifEmpty { listOf("Giải trí") }).random(random)
        val shownText = if (aboutCreator) shown else localize(shown).takeIf { en } ?: shown
        val actualText = if (aboutCreator) actual else localize(actual).takeIf { en } ?: actual
        val subjectVi = if (aboutCreator) "đăng bởi kênh" else "thuộc chủ đề"
        val subjectEn = if (aboutCreator) "posted by the channel" else "in the topic"
        return VideoQuiz(
            kind = QuizKind.TRUE_FALSE,
            prompt = if (en) "The video “${video.title}” is $subjectEn $shownText." else "Video “${video.title}” $subjectVi $shown.",
            options = if (en) listOf("True", "False") else listOf("Đúng", "Sai"),
            correctIndex = if (statementTrue) 0 else 1,
            explanation = if (en) "In fact, this video is $subjectEn $actualText." else "Thực tế, video này $subjectVi $actual."
        )
    }

    private fun build(
        kind: QuizKind, prompt: String, right: String, wrong: List<String>, random: Random, explanation: String
    ): VideoQuiz {
        val options = (wrong.take(3) + right).shuffled(random)
        return VideoQuiz(kind, prompt, options, options.indexOf(right), explanation)
    }

    private fun toSeconds(duration: String): Int {
        val parts = duration.split(":").mapNotNull { it.toIntOrNull() }
        return if (parts.size == 2) parts[0] * 60 + parts[1] else parts.firstOrNull() ?: 0
    }

    private fun format(seconds: Int) = "%d:%02d".format(seconds / 60, seconds % 60)
}

/** Streak of correct quiz answers: each extra correct answer in a row pays more. */
object QuizCombo {
    const val BASE_POINTS = 20
    private const val STEP = 5
    private const val MAX_STEPS = 6

    /** Points for the [combo]-th correct answer in a row (1 = first). 0 for no combo. */
    fun reward(combo: Int): Int =
        if (combo <= 0) 0 else BASE_POINTS + STEP * (combo - 1).coerceAtMost(MAX_STEPS)

    fun next(combo: Int, correct: Boolean): Int = if (correct) combo + 1 else 0
}
