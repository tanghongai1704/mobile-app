package com.example.onespace.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.onespace.core.common.ContentUiState

@Composable
fun <T> CatalogContent(state: ContentUiState<T>, content: @Composable (List<T>) -> Unit) {
    when {
        state.isLoading -> Box(Modifier.fillMaxWidth().height(192.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        state.error != null -> Box(Modifier.fillMaxWidth().height(192.dp), contentAlignment = Alignment.Center) {
            Text(state.error.orEmpty(), color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 16.dp))
        }
        else -> content(state.items)
    }
}
