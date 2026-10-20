package com.example.onespace.core.designsystem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.example.onespace.core.common.ContentUiState

@Composable
fun <T> CatalogContent(state: ContentUiState<T>, content: @Composable (List<T>) -> Unit) {
    when {
        state.isLoading -> Text("Loading…")
        state.error != null -> Text(state.error.orEmpty())
        else -> content(state.items)
    }
}
