package com.example.onespace.core.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp

/**
 * Search of one top-level section. The shell owns it: a magnifier in the top bar opens a field
 * under the bar ([AppSearchField]); the section screen only reads [query] to filter its list.
 */
@Stable
class SearchState {
    var open by mutableStateOf(false)
    var query by mutableStateOf("")

    fun toggle() {
        open = !open
        if (!open) query = ""
    }

    fun close() {
        open = false
        query = ""
    }
}

/** The search of the section being shown; a detached default keeps previews working. */
val LocalSearchState = compositionLocalOf { SearchState() }

/** Pill field that drops down under the top bar while a section's search is open. */
@Composable
fun AppSearchField(state: SearchState, placeholder: String, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    OutlinedTextField(
        value = state.query,
        onValueChange = { state.query = it },
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).focusRequester(focus),
        shape = CircleShape,
        colors = AppFieldDefaults.colors(),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (state.query.isNotEmpty()) {
                IconButton(onClick = { state.query = "" }) {
                    Icon(Icons.Filled.Close, contentDescription = tr("Xóa tìm kiếm", "Clear search"))
                }
            }
        },
        singleLine = true
    )
}
