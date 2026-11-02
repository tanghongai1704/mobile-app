package com.example.onespace.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Thin progress line with a round knob sitting on it. Video uses it on the bottom edge of the
 * frame (thick line, big knob); music uses a slimmer line and a small knob. With
 * [interactive] off it is a plain read-only line (history, floating windows).
 */
@Composable
fun ScrubBar(
    positionMs: Long,
    totalMs: Long,
    modifier: Modifier = Modifier,
    onSeek: (Long) -> Unit = {},
    interactive: Boolean = true,
    trackHeight: Dp = 3.dp,
    knobRadius: Dp = 7.dp,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
    progressColor: Color = MaterialTheme.colorScheme.primary
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val fraction = dragFraction
        ?: if (totalMs > 0) (positionMs.toFloat() / totalMs).coerceIn(0f, 1f) else 0f
    val touchHeight = if (interactive) maxOf(knobRadius * 2 + 8.dp, 28.dp) else trackHeight
    val base = modifier.fillMaxWidth().height(touchHeight)
    val gestures = if (interactive && totalMs > 0) base
        .pointerInput(totalMs) {
            detectTapGestures { o -> onSeek((o.x / size.width).coerceIn(0f, 1f).times(totalMs).toLong()) }
        }
        .pointerInput(totalMs) {
            detectHorizontalDragGestures(
                onDragStart = { o -> dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                onDragEnd = {
                    dragFraction?.let { onSeek((it * totalMs).toLong()) }
                    dragFraction = null
                },
                onDragCancel = { dragFraction = null }
            ) { change, _ ->
                change.consume()
                dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
            }
        } else base
    Canvas(gestures) {
        val y = size.height / 2
        val h = trackHeight.toPx()
        val knob = if (interactive) knobRadius.toPx() else 0f
        // Keep the knob fully inside the width at both ends.
        val startX = knob
        val endX = size.width - knob
        drawLine(trackColor, Offset(startX, y), Offset(endX, y), h, StrokeCap.Round)
        val x = startX + (endX - startX) * fraction
        if (fraction > 0f) drawLine(progressColor, Offset(startX, y), Offset(x, y), h, StrokeCap.Round)
        if (interactive) drawCircle(progressColor, knob, Offset(x, y))
    }
}
