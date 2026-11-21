package com.example.onespace

import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isOff
import androidx.compose.ui.test.isOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.lifecycle.ViewModelProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.example.onespace.app.ui.MediaViewModel
import com.example.onespace.core.session.SessionCoordinator
import com.example.onespace.domain.session.port.PlaybackStatus
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * End-to-end flows on a real device or emulator (connectedDebugAndroidTest). Timeouts are generous
 * so slow emulators pass; on a phone every step answers in well under a second. Flows that need
 * the sample media streams are skipped when the device has no internet.
 */
class NavigationSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private val wait = 60_000L

    // ---------- helpers ----------

    private fun exists(matcher: SemanticsMatcher) = compose.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    private fun await(matcher: SemanticsMatcher, timeoutMs: Long = wait) {
        try {
            compose.waitUntil(timeoutMs) { exists(matcher) }
        } catch (e: Throwable) {
            saveScreenshot("failure-${System.currentTimeMillis()}")
            println(compose.onRoot().printToString())
            throw e
        }
    }

    private fun awaitText(text: String, substring: Boolean = false) = await(hasText(text, substring = substring))
    private fun awaitDesc(desc: String) = await(hasContentDescription(desc))

    /** Brings [matcher] into view inside the first scrollable container when it is not composed yet. */
    private fun scrollTo(matcher: SemanticsMatcher) {
        if (exists(matcher)) return
        await(hasScrollAction())
        compose.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(matcher)
    }

    private fun click(matcher: SemanticsMatcher) {
        scrollTo(matcher)
        await(matcher)
        compose.onAllNodes(matcher).onFirst().performClick()
        compose.waitForIdle()
    }

    private fun clickText(text: String, substring: Boolean = false) = click(hasText(text, substring = substring))
    private fun clickDesc(desc: String) = click(hasContentDescription(desc).and(hasClickAction()))

    /** Bottom tabs are the only clickable nodes with these labels (the top bar repeats them as titles). */
    private fun tab(label: String) = click(hasText(label).and(hasClickAction()))

    private fun session(): SessionCoordinator {
        lateinit var session: SessionCoordinator
        compose.runOnIdle { session = ViewModelProvider(compose.activity)[SessionCoordinator::class.java] }
        return session
    }

    private fun preferences(): MediaViewModel {
        lateinit var media: MediaViewModel
        compose.runOnIdle { media = ViewModelProvider(compose.activity)[MediaViewModel::class.java] }
        return media
    }

    private fun deviceOnline(): Boolean {
        val cm = compose.activity.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** Fresh installs, cleared data and signed-out runs all walk through onboarding/login as guest. */
    private fun reachHome() {
        val media = preferences()
        compose.waitUntil(wait) { media.preferences.value != null }
        compose.waitUntil(wait) {
            exists(hasText("Trò chơi").and(hasClickAction())) || exists(hasText("Tất cả trong một app")) ||
                exists(hasText("Dùng thử không cần tài khoản"))
        }
        if (exists(hasText("Tất cả trong một app"))) {
            clickText("Tiếp tục"); awaitText("Đa nhiệm liền mạch")
            clickText("Tiếp tục"); awaitText("Thiên nhiên")
            listOf("Thiên nhiên", "Công nghệ", "Du lịch").forEach { clickText(it) }
            clickText("Bắt đầu")
        }
        compose.waitUntil(wait) {
            exists(hasText("Trò chơi").and(hasClickAction())) || exists(hasText("Dùng thử không cần tài khoản"))
        }
        if (exists(hasText("Dùng thử không cần tài khoản"))) {
            compose.onNode(hasText("Dùng thử không cần tài khoản")).performScrollTo().performClick()
            clickText("Tiếp tục dùng thử")
        }
        await(hasText("Trò chơi").and(hasClickAction()))
    }

    private fun openSettings() {
        tab("Trang chủ")
        clickDesc("Tài khoản")
        clickText("Tùy chỉnh")
        awaitText("Tiết kiệm dữ liệu")
    }

    private fun setSwitch(label: String, on: Boolean) {
        val toggle = hasText(label).and(isToggleable())
        scrollTo(toggle)
        if (!exists(toggle.and(if (on) isOn() else isOff()))) compose.onNode(toggle).performScrollTo().performClick()
        await(toggle.and(if (on) isOn() else isOff()))
    }

    @Before fun startFromHome() {
        reachHome()
        // Close a floating video or the dock left over from an earlier test.
        while (exists(hasContentDescription("Đóng").and(hasClickAction()))) {
            compose.onAllNodes(hasContentDescription("Đóng").and(hasClickAction())).onFirst().performClick()
            compose.waitForIdle()
        }
        openSettings()
        setSwitch("Mô phỏng mất mạng (demo)", on = false)
        setSwitch("Tiết kiệm dữ liệu", on = false)
        tab("Trang chủ")
    }

    // ---------- flows ----------

    @Test fun allTabsAndA2048RoundRemainUsable() {
        saveScreenshot("home")
        listOf("Video", "Nhạc", "Tin tức", "Trò chơi").forEach { tab(it) }
        clickText("Vuốt để gộp ô", substring = true)
        awaitText("Ván mới")
        await(hasContentDescription("Bảng 2048", substring = true))
        saveScreenshot("game-2048")
        clickDesc("Quay lại")
        awaitText("Tic-Tac-Toe")
    }

    @Test fun readLaterOnAnArticleSurvivesSwitchingTabs() {
        tab("Tin tức")
        clickText("Thành phố xanh bắt đầu từ ý nhỏ")
        val saved = hasContentDescription("Bỏ xem sau")
        if (!exists(saved)) clickDesc("Xem sau")
        await(saved)
        tab("Nhạc")
        tab("Tin tức")
        await(saved)
    }

    @Test fun anArticleJoinsAMixedPlaylist() {
        tab("Tin tức")
        clickText("Công nghệ nhường chỗ cho cuộc sống")
        clickDesc("Thêm vào danh sách phát")
        awaitText("Danh sách phát mới")
        val name = "Smoke ${System.currentTimeMillis() % 10_000}"
        compose.onAllNodes(hasSetTextAction()).onLast().performTextInput(name)
        clickText("Tạo")
        awaitText(name)
        awaitText("1 bài báo")
        clickText("Xong")
        awaitDesc("Thêm vào danh sách phát")
    }

    @Test fun settingsSurviveRecreationAndOfflineModeShowsTheBanner() {
        openSettings()
        setSwitch("Tiết kiệm dữ liệu", on = true)
        compose.activityRule.scenario.recreate()
        await(hasText("Tiết kiệm dữ liệu").and(isToggleable()).and(isOn()))
        setSwitch("Tiết kiệm dữ liệu", on = false)
        setSwitch("Mô phỏng mất mạng (demo)", on = true)
        tab("Trang chủ")
        awaitText("Mất kết nối mạng", substring = true)
        saveScreenshot("offline-home")
        openSettings()
        setSwitch("Mô phỏng mất mạng (demo)", on = false)
        tab("Tin tức")
        awaitText("Thành phố xanh bắt đầu từ ý nhỏ")
    }

    @Test fun musicDockReopensNowPlayingAndSkipsTracks() {
        tab("Nhạc")
        clickText("Nắng sớm")
        awaitText("Nhóm Lo-fi", substring = true)
        clickDesc("Tiếp theo")
        awaitText("Nơi nào yên tĩnh")
        clickDesc("Bài trước")
        awaitText("Nắng sớm")
        clickDesc("Quay lại")
        tab("Tin tức")
        // The dock follows the user to other tabs and reopens the full player.
        clickText("Nắng sớm")
        awaitDesc("Tiếp theo")
        saveScreenshot("now-playing")
    }

    @Test fun otpLoginValidatesTheCodeAndKeepsInterests() {
        val media = preferences()
        val interests = checkNotNull(media.preferences.value).selectedTopics
        clickDesc("Tài khoản")
        clickText("Đăng nhập để giữ tiến độ")
        awaitText("Đăng nhập / Đăng ký")
        clickText("Dùng mã OTP")
        compose.onAllNodesWithText("Email").onFirst().performClick()
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("test@onespace.example")
        clickText("Gửi mã OTP")
        awaitText("Kiểm tra mã xác thực")
        saveScreenshot("auth-verify")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextInput("000000")
        clickText("Xác nhận & tiếp tục")
        awaitText("Mã xác thực chưa đúng. Nhập 6 chữ số, mã demo là 123456.")
        compose.onAllNodes(hasSetTextAction()).onFirst().performTextReplacement("123456")
        clickText("Xác nhận & tiếp tục")
        await(hasText("Trò chơi").and(hasClickAction()))
        compose.activityRule.scenario.recreate()
        tab("Trang chủ")
        clickDesc("Tài khoản")
        awaitText("test@onespace.example", substring = true)
        clickText("Đăng xuất")
        compose.onAllNodesWithText("Đăng xuất").onLast().performClick()
        awaitText("Dùng thử không cần tài khoản")
        compose.onNode(hasText("Dùng thử không cần tài khoản")).performScrollTo().performClick()
        clickText("Tiếp tục dùng thử")
        await(hasText("Trò chơi").and(hasClickAction()))
        assertEquals(interests, checkNotNull(media.preferences.value).selectedTopics)
    }

    @Test fun videoFloatsOverNewsAndExpandsAgain() {
        assumeTrue("needs the sample video stream", deviceOnline())
        tab("Video")
        clickText("Gần với thiên nhiên thêm chút", substring = true)
        tab("Tin tức")
        await(hasContentDescription("Mở rộng"))
        saveScreenshot("news-floating-video")
        clickDesc("Mở rộng")
        awaitDesc("Thu nhỏ video")
    }

    @Test fun realMediaStreamsAdvance() {
        assumeTrue("needs the sample media streams", deviceOnline())
        val session = session()
        tab("Video")
        clickText("Gần với thiên nhiên thêm chút", substring = true)
        compose.waitUntil(wait) { session.video.value.totalMs > 0 }
        val videoPosition = session.video.value.positionMs
        compose.waitUntil(wait) { session.video.value.positionMs > videoPosition + 500 }
        tab("Nhạc")
        clickText("Nắng sớm")
        // Opening music while a video plays may ask (C1); muting the video keeps both going.
        if (exists(hasText("Tắt tiếng video"))) clickText("Tắt tiếng video")
        compose.waitUntil(wait) { session.music.value.totalMs > 0 }
        val musicPosition = session.music.value.positionMs
        compose.waitUntil(wait) { session.music.value.positionMs > musicPosition + 500 }
        assertEquals(false, session.music.value.status == PlaybackStatus.ENDED)
    }

    private fun saveScreenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(checkNotNull(instrumentation.targetContext.getExternalFilesDir(null)), "screenshots")
        check(directory.isDirectory || directory.mkdirs())
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        try {
            File(directory, "$name.png").outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        } finally {
            bitmap.recycle()
        }
    }
}
