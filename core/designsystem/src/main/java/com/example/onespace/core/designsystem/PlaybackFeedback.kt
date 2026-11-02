package com.example.onespace.core.designsystem

import com.example.onespace.core.designsystem.tr
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlaybackFeedback(buffering: Boolean, suppressed: Boolean, error: String?, modifier: Modifier = Modifier) {
    Column(modifier) {
        when {
            error != null -> Text(error.t(), color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall, maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            buffering -> {
                // No progress BAR here: the seek bar below already shows
                // position, a second bar would read as two sliders.
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text(tr("Đang tải nội dung…", "Loading…"), style = MaterialTheme.typography.bodySmall)
                }
            }
            suppressed -> Text(tr("Phát đang bị gián đoạn bởi hệ thống", "Playback was interrupted by the system"), style = MaterialTheme.typography.bodySmall)
        }
    }
}
