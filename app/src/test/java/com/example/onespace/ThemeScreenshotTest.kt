package com.example.onespace

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Assert.assertTrue
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.util.concurrent.TimeUnit
import org.robolectric.shadows.ShadowLooper

/**
 * Renders the real app on the JVM (Robolectric native graphics) and saves PNGs of the main
 * screens. Run: ./gradlew :app:testDebugUnitTest --tests '*ThemeScreenshotTest*'
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-xxhdpi")
class ThemeScreenshotTest : ScreenshotBase("light") { @Test fun walk() = walkThroughApp() }

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-night-xxhdpi")
class ThemeScreenshotDarkTest : ScreenshotBase("dark") { @Test fun walk() = walkThroughApp() }

abstract class ScreenshotBase(private val mode: String) {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    protected fun shot(name: String) {
        val dir = File(System.getProperty("screenshot.dir") ?: "build/screenshots", mode).apply { mkdirs() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(dir, "$name.png").path)
    }

    protected fun waitText(text: String, timeoutMs: Long = 30_000) {
        compose.waitUntil(timeoutMs) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    protected fun tab(label: String) {
        compose.onAllNodes(hasText(label).and(hasClickAction())).onFirst().performClick()
        compose.waitForIdle()
    }

    /** Lets async repositories emit and layout settle before a capture. */
    protected fun settle() {
        compose.waitForIdle()
        // Robolectric's main looper clock is paused: advance it so delayed loads (mock repos) fire.
        repeat(3) {
            ShadowLooper.idleMainLooper(2, TimeUnit.SECONDS)
            Thread.sleep(300)
            compose.waitForIdle()
        }
    }

    /** A screen that cannot be reached must not lose the screenshots already taken. */
    protected fun optional(label: String, block: () -> Unit) {
        try { block() } catch (e: Throwable) { println("SKIPPED $label: ${e.message}"); println(compose.onRoot().printToString(8)) }
    }

    /** Gets a fresh install to the signed-in Home screen without taking screenshots. */
    protected fun firstRunToHome() {
        waitText("Tất cả trong một app", timeoutMs = 180_000)
        compose.onNodeWithText("Tiếp tục").performClick()
        waitText("Đa nhiệm liền mạch")
        compose.onNodeWithText("Tiếp tục").performClick()
        waitText("Thiên nhiên")
        compose.onNodeWithText("Thiên nhiên").performClick()
        compose.onNodeWithText("Công nghệ").performClick()
        compose.onNodeWithText("Du lịch").performClick()
        compose.onNodeWithText("Bắt đầu").performClick()
        waitText("Dùng thử không cần tài khoản")
        compose.onNodeWithText("Dùng thử không cần tài khoản").performScrollTo().performClick()
        compose.onNodeWithText("Tiếp tục dùng thử").performClick()
        waitText("Playlist của tôi")
        settle()
    }

    protected fun back() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        settle()
    }

    protected fun walkThroughApp() {
        waitText("Tất cả trong một app", timeoutMs = 180_000)
        shot("00-onboarding")
        compose.onNodeWithText("Tiếp tục").performClick()
        waitText("Đa nhiệm liền mạch")
        compose.onNodeWithText("Tiếp tục").performClick()
        waitText("Thiên nhiên")
        compose.onNodeWithText("Thiên nhiên").performClick()
        compose.onNodeWithText("Công nghệ").performClick()
        compose.onNodeWithText("Du lịch").performClick()
        shot("01-onboarding-topics")
        compose.onNodeWithText("Bắt đầu").performClick()
        waitText("Dùng thử không cần tài khoản")
        shot("02-login")
        compose.onNodeWithText("Dùng thử không cần tài khoản").performScrollTo().performClick()
        compose.onNodeWithText("Tiếp tục dùng thử").performClick()
        waitText("Playlist của tôi")
        settle()
        shot("03-home")
        optional("home-feed") {
            compose.onNode(hasScrollAction()).performScrollToIndex(4)
            settle(); shot("03b-home-feed")
        }
        optional("account") {
            compose.onNode(hasContentDescription("Tài khoản").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick)
            settle(); shot("08-account")
            compose.onNode(hasScrollAction()).performScrollToIndex(3)
            settle(); shot("08b-account-pet")
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            settle()
        }
        tab("Video"); settle(); shot("04-video")
        tab("Nhạc"); settle(); shot("05-music")
        optional("now-playing") {
            compose.onAllNodesWithText("Nắng sớm").onFirst().performSemanticsAction(SemanticsActions.OnClick)
            settle(); shot("05b-now-playing")
            compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
            settle()
        }
        tab("Tin tức"); settle(); shot("06-news")
        tab("Trò chơi"); settle(); shot("07-games")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-xxhdpi")
class GamesScreenshotTest : GamesShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-night-xxhdpi")
class GamesScreenshotDarkTest : GamesShots("dark")

/** Screens of the games feature: hub, 2048 mid-game, Tic-Tac-Toe against the computer. */
abstract class GamesShots(mode: String) : ScreenshotBase(mode) {
    @Test
    fun games() {
        firstRunToHome()
        tab("Trò chơi"); settle(); shot("g00-hub")

        compose.onNodeWithText("2048").performClick(); settle(); shot("g01-2048-start")
        val board = compose.onNodeWithTag("board2048")
        listOf("l", "u", "r", "d", "l", "u", "l", "d", "r", "u").forEach { d ->
            board.performTouchInput {
                when (d) { "l" -> swipeLeft(); "r" -> swipeRight(); "u" -> swipeUp(); else -> swipeDown() }
            }
            settle()
        }
        shot("g02-2048-playing")
        back()

        compose.onNodeWithText("Tic-Tac-Toe").performClick(); settle(); shot("g03-ttt-start")
        compose.onNodeWithText("Máy · Khó").performClick(); settle()
        listOf(4, 0, 8, 2, 1, 3, 5, 6, 7).forEach { cell ->
            optional("ttt-cell-$cell") {
                compose.onNodeWithTag("ttt-cell-$cell").performClick()
                settle()
            }
        }
        shot("g04-ttt-hard")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-xxhdpi")
class LanguageScreenshotTest : LanguageShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-night-xxhdpi")
class LanguageScreenshotDarkTest : LanguageShots("dark")

/** Switches the language in Settings and captures the main screens in English. */
abstract class LanguageShots(mode: String) : ScreenshotBase(mode) {
    @Test
    fun switchToEnglish() {
        firstRunToHome()
        compose.onNode(hasContentDescription("Tài khoản").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick)
        settle(); shot("l00-account-vi")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Tùy chỉnh")); settle()
        compose.onNodeWithText("Tùy chỉnh").performSemanticsAction(SemanticsActions.OnClick); settle()
        shot("l01-settings-vi")
        compose.onNodeWithText("English").performSemanticsAction(SemanticsActions.OnClick); settle()
        shot("l02-settings-en")
        back(); back()
        shot("l03-home-en")
        tab("Video"); settle(); shot("l04-video-en")
        tab("Music"); settle(); shot("l05-music-en")
        tab("News"); settle(); shot("l06-news-en")
        tab("Games"); settle(); shot("l07-games-en")
        compose.onNodeWithText("2048").performClick(); settle(); shot("l08-2048-en")
        back()
        compose.onNodeWithText("Tic-Tac-Toe").performClick(); settle(); shot("l09-ttt-en")
        back()
        tab("Home"); settle()
        compose.onNode(hasContentDescription("Account").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick)
        settle(); shot("l10-account-en")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-xxhdpi")
class DetailScreenshotTest : DetailShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-night-xxhdpi")
class DetailScreenshotDarkTest : DetailShots("dark")

/** Secondary screens: video detail, article reader, settings, downloads, playlists, notifications. */
abstract class DetailShots(mode: String) : ScreenshotBase(mode) {
    private fun open(text: String) {
        compose.onAllNodesWithText(text).onFirst().performSemanticsAction(SemanticsActions.OnClick); settle()
    }

    @Test
    fun details() {
        firstRunToHome()
        tab("Video"); settle()
        optional("video-detail") {
            open("Gần với thiên nhiên thêm chút"); shot("d01-video-detail")
            compose.onNode(hasScrollAction()).performScrollToIndex(2); settle(); shot("d02-video-detail-more")
            back()
        }
        tab("Tin tức"); settle()
        optional("article") {
            open("Thành phố xanh bắt đầu từ ý nhỏ"); shot("d03-article")
            compose.onNode(hasScrollAction()).performScrollToIndex(3); settle(); shot("d04-article-more")
            back()
        }
        tab("Trang chủ"); settle()
        optional("settings") {
            compose.onNode(hasContentDescription("Tài khoản").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick); settle()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Tùy chỉnh")); settle()
            compose.onNodeWithText("Tùy chỉnh").performSemanticsAction(SemanticsActions.OnClick); settle(); shot("d05-settings")
            compose.onNode(hasScrollAction()).performScrollToIndex(6); settle(); shot("d06-settings-more")
            back()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Nhắc nhở")); settle()
            compose.onNodeWithText("Nhắc nhở").performSemanticsAction(SemanticsActions.OnClick); settle(); shot("d07-notifications")
            back()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Đã tải")); settle()
            compose.onNodeWithText("Đã tải").performSemanticsAction(SemanticsActions.OnClick); settle(); shot("d08-downloads")
            back()
            compose.onNode(hasScrollAction()).performScrollToNode(hasText("Tổng kết tháng")); settle()
            compose.onNodeWithText("Tổng kết tháng").performSemanticsAction(SemanticsActions.OnClick); settle(); shot("d09-wrapped")
        }
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-xxhdpi")
class FloatingVideoTest : FloatingShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-night-xxhdpi")
class FloatingVideoDarkTest : FloatingShots("dark")

/** The floating video window is small, can be dragged anywhere and remembers where it was left. */
abstract class FloatingShots(mode: String) : ScreenshotBase(mode) {
    private fun bounds() = compose.onNodeWithTag("floating-video").fetchSemanticsNode().boundsInRoot

    @Test
    fun dragAndRemember() {
        firstRunToHome()
        tab("Video"); settle()
        compose.onAllNodesWithText("Gần với thiên nhiên thêm chút").onFirst().performSemanticsAction(SemanticsActions.OnClick); settle()
        back()
        tab("Tin tức"); settle()
        shot("f01-floating-default")
        val start = bounds()
        compose.onNodeWithTag("floating-video").performClick(); settle()
        shot("f01b-floating-controls")
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        // 224dp plus margins is well under the old 320dp window (which took 88% of the screen width).
        assertTrue("window should be compact, was ${start.width}px of ${root.width}px", start.width < root.width * 0.7f)

        compose.onNodeWithTag("floating-video").performTouchInput {
            down(center)
            moveBy(androidx.compose.ui.geometry.Offset(-width * 1.2f, -height * 2.5f))
            up()
        }
        settle()
        shot("f02-floating-dragged")
        val moved = bounds()
        assertTrue("window should move up", moved.top < start.top - 100f)
        assertTrue("window should settle on the left edge", moved.left < start.left - 100f)
        assertTrue("window stays on screen", moved.left >= 0f && moved.top >= 0f && moved.right <= root.width && moved.bottom <= root.height)

        tab("Nhạc"); settle()
        val kept = bounds()
        assertTrue("position kept across tabs", kotlin.math.abs(kept.left - moved.left) < 2f && kotlin.math.abs(kept.top - moved.top) < 2f)
        shot("f03-floating-other-tab")
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-xxhdpi")
class FeedbackScreenshotTest : FeedbackShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h800dp-night-xxhdpi")
class FeedbackScreenshotDarkTest : FeedbackShots("dark")

/** Karaoke popup, song actions, search drop-down, quiz, playlists for Guess the Song, Beat Tiles menu, split. */
abstract class FeedbackShots(mode: String) : ScreenshotBase(mode) {
    private fun open(text: String) {
        compose.onAllNodesWithText(text).onFirst().performSemanticsAction(SemanticsActions.OnClick); settle()
    }

    private fun openScrolling(text: String) {
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(text)); settle()
        compose.onAllNodesWithText(text).onFirst().performSemanticsAction(SemanticsActions.OnClick); settle()
    }

    @Test
    fun feedback() {
        firstRunToHome()
        optional("search") {
            compose.onNode(hasContentDescription("Tìm kiếm").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick)
            settle(); shot("k01-home-search-open")
            compose.onNode(hasContentDescription("Tìm kiếm").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick)
            settle()
        }
        tab("Nhạc"); settle()
        optional("music-menu") {
            compose.onAllNodes(hasContentDescription("Thêm tùy chọn").and(hasClickAction())).onFirst().performSemanticsAction(SemanticsActions.OnClick)
            settle(); shot("k02-music-row-menu")
            back()
        }
        optional("music-detail") {
            open("Nắng sớm"); shot("k03-music-detail")
            compose.onNode(hasContentDescription("Ghi âm / Karaoke").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick)
            settle(); shot("k04-record-mode")
            compose.onNodeWithText("Hủy").performClick(); settle()
            compose.onNodeWithText("Xem hàng đợi").performClick(); settle(); shot("k05-queue")
            back()
        }
        tab("Video"); settle()
        optional("video-detail") {
            open("Gần với thiên nhiên thêm chút"); shot("k06-video-detail")
            compose.onNode(hasContentDescription("Ghi âm / Karaoke").and(hasClickAction())).performSemanticsAction(SemanticsActions.OnClick)
            settle(); shot("k07-record-mode-video")
            compose.onNodeWithText("Ghi cùng video").performClick(); settle(); shot("k08-record-mic-intro")
            compose.onNodeWithText("Để sau").performClick(); settle()
        }
        tab("Trò chơi"); settle()
        optional("split") {
            open("2048"); shot("k09-split-2048")
            back()
        }
        optional("split-snake") {
            openScrolling("Rắn săn mồi"); shot("k10-split-snake-notice")
            compose.onNodeWithText("Chơi toàn màn hình").performClick(); settle(); shot("k11-snake-full")
            back()
        }
        optional("quiz") {
            openScrolling("Quiz kiến thức"); shot("k12-knowledge-quiz")
            back()
        }
        optional("guess") {
            open("Đoán bài hát")
            compose.onNodeWithText("Chơi từ Playlist của bạn").performClick(); settle(); shot("k13-guess-playlist")
            back()
        }
        optional("beat") {
            openScrolling("Beat Tiles")
            shot("k14-beat-menu")
            compose.onNodeWithText("Chọn bài hát của bạn").performClick(); settle(); shot("k15-beat-songs")
            back()
        }
    }
}
