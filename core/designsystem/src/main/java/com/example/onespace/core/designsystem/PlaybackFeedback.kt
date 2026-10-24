package com.example.onespace.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun PlaybackFeedback(buffering: Boolean, suppressed: Boolean, error: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        when {
            error != null -> Text(error, color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall)
            buffering -> {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Đang tải nội dung…", style = MaterialTheme.typography.bodySmall)
            }
            suppressed -> Text("Phát đang bị gián đoạn bởi hệ thống", style = MaterialTheme.typography.bodySmall)
        }
    }
}
