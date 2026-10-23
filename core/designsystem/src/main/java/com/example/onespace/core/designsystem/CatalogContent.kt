package com.example.onespace.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.ContentUiState

/**
 * The one place every feed renders its loading / error / offline / empty /
 * loaded states, per the UX course rules: skeleton for rich screens, a real
 * message plus a retry or a CTA instead of dead ends.
 */
@Composable
fun <T> CatalogContent(
    state: ContentUiState<T>,
    offline: Boolean = false,
    onRetry: () -> Unit = {},
    offlineSecondary: Pair<String, () -> Unit>? = null,
    empty: EmptySpec? = null,
    content: @Composable (List<T>) -> Unit
) {
    when {
        offline -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
            ErrorState(
                title = "Không có kết nối",
                message = "Hiển thị nội dung đã tải khi có mạng lại.",
                onRetry = onRetry,
                secondaryLabel = offlineSecondary?.first,
                onSecondary = offlineSecondary?.second
            )
        }
        state.isLoading -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
            SkeletonFeed()
        }
        state.error != null -> Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
            ErrorState(
                title = "Lỗi hệ thống",
                message = state.error.orEmpty(),
                onRetry = onRetry
            )
        }
        state.items.isEmpty() && empty != null -> Box(Modifier.fillMaxWidth().padding(vertical = 24.dp)) {
            EmptyState(empty)
        }
        else -> content(state.items)
    }
}
