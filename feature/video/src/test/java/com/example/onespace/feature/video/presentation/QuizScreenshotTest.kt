package com.example.onespace.feature.video.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.onespace.core.designsystem.OneSpaceTheme
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.quiz.QuizCombo
import com.example.onespace.domain.video.quiz.VideoQuizGenerator
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import kotlin.random.Random
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val POOL = listOf(
    Video("v1", "Gần với thiên nhiên thêm chút", "OneSpace Studio", "1:00", "u", "Thiên nhiên"),
    Video("v7", "Nắng xuyên tán lá sớm", "Góc xanh", "0:10", "u", "Thiên nhiên"),
    Video("v9", "Bão biển qua ống kính nhỏ", "OneSpace Studio", "0:46", "u", "Thiên nhiên"),
    Video("v2", "Sài Gòn, sau giờ tan", "Chuyện phố", "0:46", "u", "Đời sống"),
    Video("v11", "Chợ sớm có gì vui", "Tạp ghi Sài Gòn", "0:10", "u", "Đời sống"),
    Video("v15", "Trà chiều và một cuốn sách", "Một góc yên tĩnh", "0:10", "u", "Nhẹ nhàng"),
    Video("v17", "Nến, nhạc lo-fi và tối thứ bảy", "Sống chậm", "0:15", "u", "Nhẹ nhàng"),
    Video("v4", "Khung hình êm, phố vắng", "Sống chậm", "0:30", "u", "Du lịch")
)

/** Renders the end-of-video quiz in each state (video playback is not available on the JVM). */
abstract class QuizShots(private val mode: String) {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String) {
        val dir = File(System.getProperty("screenshot.dir") ?: "build/screenshots", mode).apply { mkdirs() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(dir, "$name.png").path)
    }

    private fun show(round: Int, combo: Int, seed: Int) {
        compose.setContent {
            OneSpaceTheme {
                Surface {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        EndOfVideoQuiz(
                            video = POOL[0], pool = POOL, startRound = round, combo = combo,
                            onAnswered = { QuizCombo.reward(combo + 1).takeIf { _ -> it } ?: 0 },
                            random = Random(seed)
                        )
                    }
                }
            }
        }
    }

    @Test
    fun quizStates() {
        // Round 0: which channel posted it; second run in this class shows other kinds.
        val kinds = (0..3).map { VideoQuizGenerator.generate(POOL[0], POOL, it, Random(11)) }
        show(round = 0, combo = 3, seed = 11)
        shot("q1-question-combo")
        val q = kinds[0]
        // A wrong pick first (explanation + combo broken), using the real generator's answer key.
        val wrong = q.options.first { it != q.options[q.correctIndex] }
        compose.onNodeWithText(wrong).performClick()
        shot("q2-wrong")
    }

    @Test
    fun correctAnswerAndOtherKinds() {
        val right = VideoQuizGenerator.generate(POOL[0], POOL, 1, Random(21)).let { it.options[it.correctIndex] }
        show(round = 1, combo = 3, seed = 21)
        shot("q3-other-kind")
        compose.onNodeWithText(right).performClick()
        shot("q4-correct-combo")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-xxhdpi")
class QuizScreenshotTest : QuizShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-night-xxhdpi")
class QuizScreenshotDarkTest : QuizShots("dark")
