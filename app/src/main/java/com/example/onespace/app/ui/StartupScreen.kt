package com.example.onespace.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.onespace.core.designsystem.OneSpaceBrand

/** Fallback loading/error content; the system splash covers normal initialization. */
@Composable
fun StartupScreen(state: StartupUiState, onRetry: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center) {
            OneSpaceBrand()
            Spacer(Modifier.height(24.dp))
            Text("Bốn cách tận hưởng. Một không gian.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            if (state is StartupUiState.Error) {
                Text("Chưa thể khôi phục phiên trên thiết bị.")
                TextButton(onClick = onRetry) { Text("Thử lại") }
            } else CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}
