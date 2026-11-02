package com.example.onespace.domain.video

import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.quiz.QuizCombo
import com.example.onespace.domain.video.quiz.QuizKind
import com.example.onespace.domain.video.quiz.VideoQuizGenerator
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoQuizTest {
    private fun v(id: String, title: String, creator: String, duration: String, topic: String) =
        Video(id, title, creator, duration, "url", topic)

    private val pool = listOf(
        v("1", "Gần với thiên nhiên", "OneSpace Studio", "1:00", "Thiên nhiên"),
        v("2", "Nắng xuyên tán lá", "Góc xanh", "0:10", "Thiên nhiên"),
        v("3", "Sài Gòn sau giờ tan", "Chuyện phố", "0:46", "Đời sống"),
        v("4", "Chợ sớm có gì vui", "Tạp ghi Sài Gòn", "0:10", "Đời sống"),
        v("5", "Tiếng mưa trên mái tôn", "Một góc yên tĩnh", "0:10", "Nhẹ nhàng"),
        v("6", "Nến và nhạc lo-fi", "Sống chậm", "0:15", "Nhẹ nhàng"),
        v("7", "Phố cổ sau cơn mưa", "Tạp ghi Sài Gòn", "0:30", "Du lịch")
    )

    @Test fun everyKindIsReachableAndWellFormed() {
        val seen = mutableSetOf<QuizKind>()
        pool.forEach { video ->
            repeat(12) { round ->
                val quiz = VideoQuizGenerator.generate(video, pool, round, Random(round * 31 + video.id.hashCode()))
                seen += quiz.kind
                assertTrue(quiz.explanation.isNotBlank())
                assertEquals(quiz.options.size, quiz.options.distinct().size)
                if (quiz.kind != QuizKind.TRUE_FALSE) {
                    assertEquals(4, quiz.options.size)
                    assertFalse(quiz.prompt.isBlank())
                }
            }
        }
        assertEquals(QuizKind.entries.toSet(), seen)
    }

    @Test fun correctAnswerMatchesTheVideo() {
        pool.forEach { video ->
            repeat(16) { round ->
                val quiz = VideoQuizGenerator.generate(video, pool, round, Random(round))
                val right = quiz.options[quiz.correctIndex]
                when (quiz.kind) {
                    QuizKind.CREATOR -> assertEquals(video.creator, right)
                    QuizKind.DURATION -> assertEquals(video.duration, right)
                    QuizKind.SAME_TOPIC -> assertEquals(
                        video.topic, pool.first { it.title == right }.topic
                    )
                    QuizKind.TRUE_FALSE -> {
                        val claimsCreator = quiz.prompt.contains("kênh")
                        val claim = quiz.prompt.substringAfter(if (claimsCreator) "kênh " else "chủ đề ").trimEnd('.')
                        val truth = if (claimsCreator) video.creator else video.topic
                        assertEquals(claim == truth, right == "Đúng")
                    }
                }
            }
        }
    }

    @Test fun rotatesQuestionTypeBetweenRounds() {
        val kinds = (0 until 4).map { VideoQuizGenerator.generate(pool[0], pool, it, Random(1)).kind }.toSet()
        assertTrue(kinds.size >= 3)
    }

    @Test fun smallPoolStillProducesAQuiz() {
        val tiny = listOf(pool[0], pool[1])
        repeat(8) { round ->
            val quiz = VideoQuizGenerator.generate(pool[0], tiny, round, Random(round))
            assertTrue(quiz.options.size >= 2)
        }
    }

    @Test fun comboRaisesRewardAndCapsIt() {
        assertEquals(0, QuizCombo.reward(0))
        assertEquals(20, QuizCombo.reward(1))
        assertEquals(25, QuizCombo.reward(2))
        assertEquals(50, QuizCombo.reward(7))
        assertEquals(50, QuizCombo.reward(40))
        assertEquals(3, QuizCombo.next(2, true))
        assertEquals(0, QuizCombo.next(5, false))
    }
}
