package com.example.onespace

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
        // Any main screen shows the bottom nav tab "Games"; onboarding screens don't.
        val onMainScreen = awaitAny("Games")
        val stepsVisible = awaitAny("Tiếp tục", timeoutMs = 1_000)
        val loginVisible = awaitAny("Dùng thử không cần tài khoản", timeoutMs = 1_000)
        if (!onMainScreen && !stepsVisible && !loginVisible) return
        if (stepsVisible) {
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
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
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

    companion object {
        // The onboarding walk happens once per suite: the first test pays the
        // cost; later tests inherit a stable, already-onboarded app process.
        private var onboardingHandled = false
    }

    @Before fun resetMedia() {
        if (!onboardingHandled) {
            bypassOnboardingIfNeeded()
            onboardingHandled = true
        }
        // Dismiss a pending C1 conflict dialog, if any survived.
        if (compose.onAllNodesWithText("Bỏ qua").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Bỏ qua").performClick()
        }
        // Close the floating window and the dock player, whichever is alive.
        while (compose.onAllNodesWithContentDescription("Đóng").fetchSemanticsNodes().isNotEmpty()) {
            compose.onAllNodesWithContentDescription("Đóng").onFirst().performClick()
        }
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
        compose.onNodeWithText("Nắng sớm").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Nhóm Lo-fi").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Nhóm Lo-fi").assertIsDisplayed()
        compose.onNodeWithContentDescription("Tạm dừng").assertIsDisplayed()
    }
}
