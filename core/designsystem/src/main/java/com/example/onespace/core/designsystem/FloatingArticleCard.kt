package com.example.onespace.core.designsystem

import com.example.onespace.core.designsystem.tr
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * F-NEWS-12: the reading shrinks into this card while the reader is on
 * another tab; tapping anywhere on the card returns to the article.
 * While TTS is reading the article aloud ([ttsActive]), the card also carries
 * a progress bar and a play/pause toggle next to the close button; without
 * audio it stays a plain "Đang đọc" label.
 */
@Composable
fun FloatingArticleCard(
    title: String,
    onExpand: () -> Unit,
    onClose: () -> Unit,
    ttsActive: Boolean = false,
    ttsPlaying: Boolean = false,
    ttsProgress: Float = 0f,
    onToggleTts: () -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = androidx.compose.foundation.shape.ZeroCornerSize,
            bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize
        ),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onExpand)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Filled.Newspaper,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        tr("Đang đọc", "Reading"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
                // TTS is reading aloud: pause / resume on the left of the close button.
                if (ttsActive) {
                    IconButton(onClick = onToggleTts) {
                        Icon(
                            if (ttsPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (ttsPlaying) tr("Tạm dừng đọc", "Pause reading")
                            else tr("Đọc tiếp", "Resume reading"),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = tr("Đóng", "Close"))
                }
            }
            // How far the spoken reading has got; only while audio is involved.
            if (ttsActive) {
                LinearProgressIndicator(
                    progress = { ttsProgress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
