package com.example.onespace

import android.graphics.Bitmap
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.printToString
import com.example.onespace.app.ui.MediaViewModel
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.onespace.core.session.SessionCoordinator
import java.io.File
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Run on an API 26+ device with connectedDebugAndroidTest. */
class NavigationSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    /**
     * The shell top bar now carries the destination name ("Video", "News"...),
     * so a bare text lookup hits both the title and the tab. Tabs are the only
     * nodes with a click action.
     */
    private fun SemanticsNodeInteractionsProvider.tapTab(label: String) {
        // Waits past onboarding→Home navigation races before touching the tab.
        compose.waitUntil(10_000) {
            onAllNodes(hasText(label).and(hasClickAction())).fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodes(hasText(label).and(hasClickAction())).onFirst().performClick()
    }

    /**
     * First-run gate: a fresh install (or a logout) shows Splash → steps →
     * Login; walk straight through as guest so the smoke flows can run.
     */
    /**
     * First-run gate tolerance: a fresh install, an orchestrator data-clear or a
     * post-logout state may leave the app in Splash → steps → Login. Poll (no
     * throw) and walk through as guest when those screens really are on top;
     * otherwise return quietly and let the main-flow assertions do the waiting.
     */
    private fun bypassOnboardingIfNeeded() {
        lateinit var media: MediaViewModel
        compose.runOnIdle { media = ViewModelProvider(compose.activity)[MediaViewModel::class.java] }
        compose.waitUntil(20_000) { media.preferences.value != null }
        if (media.preferences.value?.onboarded == true && awaitAny("Games")) return
        compose.waitUntil(20_000) {
            compose.onAllNodesWithText("Tất cả trong một app").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Dùng thử không cần tài khoản").fetchSemanticsNodes().isNotEmpty()
        }
        // Home also has a "Tiếp tục" section. Only the unique intro heading identifies onboarding.
        if (compose.onAllNodesWithText("Tất cả trong một app").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Tiếp tục").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Đa nhiệm liền mạch").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Tiếp tục").performClick()
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText("Thiên nhiên").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithText("Thiên nhiên").performClick()
            compose.onNodeWithText("Công nghệ").performClick()
            compose.onNodeWithText("Du lịch").performClick()
            compose.onNodeWithText("Bắt đầu").performClick()
        }
        // Both fresh-run and post-logout states end on the Login screen.
        if (awaitAny("Dùng thử không cần tài khoản", timeoutMs = 10_000)) {
            compose.onNodeWithText("Dùng thử không cần tài khoản").performClick()
            compose.onNodeWithText("Tiếp tục dùng thử").performClick()
        }
    }

    private fun awaitText(text: String) {
        try {
            compose.waitUntil(10_000) {
                compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (error: Throwable) {
            saveScreenshot("failure-" + text.hashCode())
            println(compose.onRoot().printToString())
            throw error
        }
    }

    private fun awaitAny(text: String, timeoutMs: Long = 8_000): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) return true
            Thread.sleep(200)
        }
        return false
    }

    @Before fun resetMedia() {
        bypassOnboardingIfNeeded()
        // Dismiss a pending C1 conflict dialog, if any survived.
        if (compose.onAllNodesWithText("Bỏ qua").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Bỏ qua").performClick()
        }
        // Close the floating window and the dock player, whichever is alive.
        while (compose.onAllNodesWithContentDescription("Đóng").fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodesWithContentDescription("Đóng").onFirst().performClick()
        }
        compose.tapTab("Home")
        openSettings()
        listOf("Mô phỏng mất mạng (demo)", "Tiết kiệm dữ liệu").forEach { label ->
            scrollSetting(label)
            val enabled = compose.onAllNodes(hasText(label).and(isToggleable()).and(isOn()))
            if (enabled.fetchSemanticsNodes().isNotEmpty()) enabled.onFirst().performScrollTo().performClick()
        }
        compose.tapTab("Home")
    }

    @Test fun videoCanFloatOverNewsAndExpandAgain() {
        compose.tapTab("Video")
        // The resumed-progress row ("Tiếp tục xem") shows the same title; both open v1.
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Gần với thiên nhiên thêm chút").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodesWithText("Gần với thiên nhiên thêm chút").onFirst().performClick()
        compose.onNodeWithText("Thu nhỏ & khám phá").performClick()
        awaitText("Mới nhất"); compose.onNodeWithText("Mới nhất").assertIsDisplayed()
        awaitText("Đang phát · cửa sổ nổi")
        compose.onNodeWithText("Đang phát · cửa sổ nổi").assertIsDisplayed()
        saveScreenshot("news-floating-video")
        compose.onNodeWithContentDescription("Mở rộng").performClick()
        awaitText("Thu nhỏ & khám phá")
        compose.onNodeWithText("Thu nhỏ & khám phá").assertIsDisplayed()
    }

    @Test fun articleBookmarkSurvivesSwitchingTabs() {
        compose.tapTab("News")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Thành phố xanh bắt đầu từ ý nhỏ").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Thành phố xanh bắt đầu từ ý nhỏ").performClick()
        awaitText("Lưu bài viết")
        compose.onNodeWithText("Lưu bài viết").performClick()
        compose.tapTab("Music")
        compose.tapTab("News")
        awaitText("Đã lưu ✓"); compose.onNodeWithText("Đã lưu ✓").assertIsDisplayed()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("Đã lưu (1)").performClick()
        compose.onNodeWithText("Thành phố xanh bắt đầu từ ý nhỏ").assertIsDisplayed()
    }

    @Test fun musicMiniPlayerReopensNowPlaying() {
        compose.tapTab("Music")
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Nắng sớm").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Nắng sớm").performClick()
        compose.tapTab("News")
        awaitText("Nắng sớm")
        compose.onNodeWithText("Nắng sớm").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Nhóm Lo-fi").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Nhóm Lo-fi").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tạm dừng").assertIsDisplayed()
        saveScreenshot("music")
        compose.onNodeWithContentDescription("Tiếp theo").performClick()
        awaitText("Nơi nào yên tĩnh")
        compose.onNodeWithContentDescription("Bài trước").performClick()
        awaitText("Nắng sớm")
        compose.onNodeWithContentDescription("Tạm dừng").performClick()
        compose.onNodeWithContentDescription("Phát").assertIsDisplayed()
    }

    @Test fun allTabsAndGamePreviewRemainUsable() {
        saveScreenshot("home")
        compose.tapTab("Video")
        compose.tapTab("Music")
        compose.tapTab("News")
        compose.tapTab("Games")
        awaitText("2048")
        compose.onNodeWithText("2048").performClick()
        compose.onNodeWithText("Tạm dừng bản demo").performScrollTo().performClick()
        compose.onNodeWithText("Chơi bản demo").assertIsDisplayed().performClick()
        saveScreenshot("game-board")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithText("2048").assertIsDisplayed()
    }

    private fun openSettings() {
        compose.tapTab("Home")
        compose.onNodeWithContentDescription("Tài khoản").performClick()
        compose.onNodeWithText("Tùy chỉnh").performClick()
        awaitText("Tiết kiệm dữ liệu")
    }

    private fun scrollSetting(label: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(label))
    }

    @Test fun settingsSurviveRecreationAndOfflineCanBeRecovered() {
        openSettings()
        val dataSaver = hasText("Tiết kiệm dữ liệu").and(isToggleable())
        compose.onNode(dataSaver).performClick().assertIsOn()
        compose.activityRule.scenario.recreate()
        compose.onNode(dataSaver).assertIsOn().performClick()
        compose.onNodeWithText("Tối").performClick()
        compose.onNodeWithText("Hệ thống").performClick()
        val offline = hasText("Mô phỏng mất mạng (demo)").and(isToggleable())
        scrollSetting("Mô phỏng mất mạng (demo)")
        compose.onNode(offline).performScrollTo().performClick()
        compose.tapTab("News")
        compose.onNodeWithText("Không có kết nối").assertIsDisplayed()
        openSettings()
        scrollSetting("Mô phỏng mất mạng (demo)")
        compose.onNode(offline).performScrollTo().performClick()
        saveScreenshot("settings")
        compose.tapTab("News")
        awaitText("Mới nhất")
        compose.onNodeWithText("Mới nhất").assertIsDisplayed()
    }

    @Test fun audioConflictCanMuteVideoAndKeepBothPlayersControllable() {
        openSettings()
        scrollSetting("Hỏi lại")
        val resetRule = compose.onAllNodes(hasText("Hỏi lại").and(isEnabled()))
        if (resetRule.fetchSemanticsNodes().isNotEmpty()) resetRule.onFirst().performScrollTo().performClick()
        compose.tapTab("Video")
        awaitText("Gần với thiên nhiên thêm chút")
        compose.onAllNodesWithText("Gần với thiên nhiên thêm chút").onFirst().performClick()
        compose.tapTab("Music")
        awaitText("Nắng sớm")
        compose.onNodeWithText("Nắng sớm").performClick()
        awaitText("Tắt tiếng video, phát nhạc")
        compose.onNodeWithText("Tắt tiếng video, phát nhạc").performClick()
        compose.tapTab("News")
        compose.onNodeWithText("Tắt tiếng").assertIsDisplayed()
        compose.onNodeWithText("Nắng sớm").assertIsDisplayed()
        saveScreenshot("video-and-music")
    }

    @Test fun realMediaStreamsAdvanceBeyondTheInitialPosition() {
        lateinit var session: SessionCoordinator
        compose.runOnIdle { session = ViewModelProvider(compose.activity)[SessionCoordinator::class.java] }
        compose.tapTab("Video")
        awaitText("Gần với thiên nhiên thêm chút")
        compose.onAllNodesWithText("Gần với thiên nhiên thêm chút").onFirst().performClick()
        compose.waitUntil(60_000) { session.video.value.totalMs > 0 }
        // Room may contain a completed video from an earlier run. Start the stream assertion at zero.
        compose.runOnIdle { session.seekVideoBy(-session.video.value.totalMs) }
        compose.waitUntil(15_000) { session.video.value.positionMs < 2_000 }
        val videoPosition = session.video.value.positionMs
        compose.waitUntil(15_000) { session.video.value.positionMs > videoPosition + 500 }
        compose.runOnIdle { session.seekVideoBy(session.video.value.totalMs) }
        compose.waitUntil(10_000) { !session.video.value.playing }
        compose.onNodeWithText("Phát").performClick()
        compose.waitUntil(15_000) { session.video.value.positionMs in 501..5_000 }
        compose.tapTab("News")
        compose.onNodeWithContentDescription("Đóng").performClick()
        compose.tapTab("Music")
        awaitText("Nắng sớm")
        compose.onNodeWithText("Nắng sớm").performClick()
        compose.waitUntil(60_000) { session.music.value.totalMs > 0 }
        val musicPosition = session.music.value.positionMs
        compose.waitUntil(15_000) { session.music.value.positionMs > musicPosition + 500 }
        compose.onNodeWithContentDescription("Tạm dừng").assertIsDisplayed()
    }

    @Test fun demoLoginValidatesEmailAndPreservesInterests() {
        lateinit var preferences: MediaViewModel
        compose.runOnIdle { preferences = ViewModelProvider(compose.activity)[MediaViewModel::class.java] }
        val interests = checkNotNull(preferences.preferences.value).selectedTopics
        compose.onNodeWithContentDescription("Tài khoản").performClick()
        compose.onNodeWithText("Đăng nhập để giữ tiến độ").performClick()
        awaitText("Tiếp tục với email")
        compose.onNodeWithText("Email").performTextInput("invalid")
        compose.onNodeWithText("Tiếp tục với email").performScrollTo().performClick()
        compose.onNodeWithText("Email chưa hợp lệ — kiểm tra lại giúp mình nhé.").assertIsDisplayed()
        compose.onNodeWithText("Email").performTextReplacement("test@onespace.example")
        compose.onNodeWithText("Tiếp tục với email").performScrollTo().performClick()
        compose.tapTab("Home")
        compose.onNodeWithContentDescription("Tài khoản").performClick()
        awaitText("test@onespace.example")
        compose.onNodeWithText("Đăng xuất").performClick()
        compose.onAllNodesWithText("Đăng xuất").onLast().performClick()
        awaitText("Dùng thử không cần tài khoản")
        compose.onNodeWithText("Dùng thử không cần tài khoản").performClick()
        compose.onNodeWithText("Tiếp tục dùng thử").performClick()
        compose.tapTab("Home")
        assertEquals(interests, checkNotNull(preferences.preferences.value).selectedTopics)
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "screenshots")
        check(directory.isDirectory || directory.mkdirs())
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try {
            File(directory, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
