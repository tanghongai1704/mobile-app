package com.example.onespace.app.ui

import com.example.onespace.core.designsystem.tr
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
            Text(tr("Bốn cách tận hưởng. Một không gian.", "Four ways to enjoy. One space."), style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            if (state is StartupUiState.Error) {
                Text(tr("Chưa thể khôi phục phiên trên thiết bị.", "Couldn't restore the session on this device."))
                TextButton(onClick = onRetry) { Text(tr("Thử lại", "Try again")) }
            } else CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        }
    }
}
