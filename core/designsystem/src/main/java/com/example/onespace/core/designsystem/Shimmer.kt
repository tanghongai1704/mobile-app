package com.example.onespace.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Skeleton shimmer per the UX rule: gradient animation, no images. */
fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerProgress"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceContainerLowest
    val width = 480f
    background(base).drawWithContent {
        drawContent()
        val start = Offset(x = (size.width + width) * progress - width, y = 0f)
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, highlight.copy(alpha = 0.55f), Color.Transparent),
                start = start,
                end = Offset(start.x + width, size.height)
            )
        )
    }
}

/** Feed placeholder while the catalog loads: content-shaped shimmer blocks. */
@Composable
fun SkeletonFeed(rows: Int = 4) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).shimmer())
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.72f).height(18.dp).shimmer())
                    Box(Modifier.fillMaxWidth(0.45f).height(14.dp).shimmer())
                }
                Spacer(Modifier.size(8.dp))
                Box(Modifier.size(20.dp).shimmer())
            }
        }
    }
}

/** EmptySpec for feeds: icon + honest message + a next step (CTA is mandatory). */
data class EmptySpec(
    val icon: ImageVector,
    val title: String,
    val cta: String,
    val onCta: () -> Unit
)

@Composable
fun EmptyState(spec: EmptySpec) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(spec.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp))
        Text(spec.title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = spec.onCta) { Text(spec.cta) }
    }
}

/** Error/offline state: says what happened, offers retry plus a useful secondary link. */
@Composable
fun ErrorState(
    title: String,
    message: String,
    onRetry: () -> Unit,
    retryLabel: String = "Thử lại",
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    Column(
        Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(message, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onRetry) { Text(retryLabel) }
        if (secondaryLabel != null && onSecondary != null) {
            OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
        }
    }
}
