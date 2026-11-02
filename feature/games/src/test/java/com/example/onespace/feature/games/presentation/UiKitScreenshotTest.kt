package com.example.onespace.feature.games.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.onespace.core.designsystem.AppFieldDefaults
import com.example.onespace.core.designsystem.ContentCard
import com.example.onespace.core.designsystem.EmptySpec
import com.example.onespace.core.designsystem.EmptyState
import com.example.onespace.core.designsystem.ErrorState
import com.example.onespace.core.designsystem.OneSpaceTheme
import com.example.onespace.core.designsystem.SectionTitle
import com.example.onespace.core.designsystem.SkeletonFeed
import com.example.onespace.core.designsystem.fadingEdgeEnd
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A gallery of the shared building blocks, for reviewing the look in one picture. */
abstract class UiKitShots(private val mode: String) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun gallery() {
        compose.setContent {
            OneSpaceTheme {
                Surface {
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = "", onValueChange = {}, modifier = Modifier.padding(0.dp),
                                shape = androidx.compose.foundation.shape.CircleShape, colors = AppFieldDefaults.colors(),
                                placeholder = { Text("Tìm video hoặc kênh") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
                            )
                            LazyRow(modifier = Modifier.fadingEdgeEnd(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Tất cả", "Dành cho bạn", "Đã xem", "Thiên nhiên", "Đời sống", "Du lịch").forEachIndexed { i, label ->
                                    item { FilterChip(selected = i == 0, onClick = {}, label = { Text(label) }) }
                                }
                            }
                            ContentCard("Tic-Tac-Toe", "Đấu với máy ở 3 mức độ hoặc chơi cùng bạn.", Icons.Filled.SportsEsports) {}
                            SectionTitle("Đang tải")
                        }
                        SkeletonFeed(rows = 2)
                        EmptyState(EmptySpec(Icons.Filled.Bookmark, "Chưa lưu video nào", "Khám phá video") {})
                        ErrorState("Không có kết nối", "Hiển thị nội dung đã tải khi có mạng lại.", onRetry = {})
                    }
                }
            }
        }
        val dir = File(System.getProperty("screenshot.dir") ?: "build/screenshots", mode).apply { mkdirs() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage(File(dir, "k1-ui-kit.png").path)
    }
}

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h1300dp-xxhdpi")
class UiKitScreenshotTest : UiKitShots("light")

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w392dp-h1300dp-night-xxhdpi")
class UiKitScreenshotDarkTest : UiKitShots("dark")
