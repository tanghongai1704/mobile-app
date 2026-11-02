package com.example.onespace.app.ui

import com.example.onespace.core.designsystem.oneSpaceColors
import com.example.onespace.core.designsystem.tr
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import com.example.onespace.core.designsystem.AppCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.onespace.core.media.VideoSurface
import com.example.onespace.core.designsystem.PlaybackFeedback
import com.example.onespace.domain.session.port.MusicPlayback
import com.example.onespace.domain.session.port.VideoPlayback
import com.example.onespace.domain.session.port.PlaybackStatus

/** Mini player dock: rounded top edge, tinted note tile, whole row expands; actions are 40dp buttons. */
@Composable
fun MusicMiniPlayer(title: String, playback: MusicPlayback, onExpand: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit) {
    val playing = playback.playRequested && playback.error == null
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = androidx.compose.foundation.shape.ZeroCornerSize,
            bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize
        ),
        shadowElevation = 3.dp
    ) {
        Row(Modifier.fillMaxWidth()
            .clickable(onClick = onExpand)
            .padding(start = 14.dp, end = 4.dp)
            .heightIn(min = 60.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f), MaterialTheme.shapes.medium
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                Text(title, maxLines = 1, style = MaterialTheme.typography.titleSmall)
                PlaybackFeedback(playback.status == PlaybackStatus.BUFFERING, playback.suppressed, playback.error)
            }
            IconButton(onClick = onToggle) {
                Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"))
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = tr("Đóng", "Close"))
            }
        }
    }
}

/**
 * Where the floating video window sits, as an offset from its default bottom-end corner
 * (both values are zero or negative). Hoisted to the shell so the window stays where the user put
 * it while it leaves and re-enters composition (tab changes, expanding the player).
 */
class FloatingPosition(dx: Float = 0f, dy: Float = 0f) {
    var dx by mutableFloatStateOf(dx)
    var dy by mutableFloatStateOf(dy)

    companion object {
        val Saver = listSaver<FloatingPosition, Float>(
            save = { listOf(it.dx, it.dy) },
            restore = { FloatingPosition(it[0], it[1]) }
        )
    }
}

@Composable
fun rememberFloatingPosition(): FloatingPosition = rememberSaveable(saver = FloatingPosition.Saver) { FloatingPosition() }

/**
 * In-app floating window for the playing video (ADR-08): the SAME shared player
 * renders here through [VideoSurface]; closing keeps the video in the Watchlist
 * at its last position (case C3). It is a small window the user can drag anywhere
 * on screen; on release it settles against the nearest side edge.
 */
@Composable
fun BoxScope.FloatingVideo(
    player: Player?,
    title: String,
    playback: VideoPlayback,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    position: FloatingPosition,
    onHeightChanged: (Int) -> Unit = {}
) {
    val playing = playback.playRequested && playback.error == null
    val actions: @Composable RowScope.() -> Unit = {
        FloatingAction(Icons.Filled.OpenInFull, tr("Mở rộng", "Expand"), onExpand)
        FloatingAction(
            if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"), onToggle
        )
        FloatingAction(Icons.Filled.Close, tr("Đóng", "Close"), onClose)
    }
    DraggableFloating(position, onHeightChanged) {
        // F-VID-09: sound-only mode renders a music-style mini player, no surface.
        if (com.example.onespace.core.designsystem.AudioOnlyState.active) {
            Row(
                Modifier.width(264.dp).padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                    PlaybackFeedback(playback.status == PlaybackStatus.BUFFERING, playback.suppressed, playback.error)
                }
                actions()
            }
        } else {
            Column(Modifier.width(224.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                    VideoSurface(player, Modifier.fillMaxSize().background(MaterialTheme.oneSpaceColors.mediaBackdrop))
                    // The video view would swallow touches; this layer shields it (without consuming
                    // anything) so a drag that starts on the picture still moves the whole window.
                    Box(Modifier.matchParentSize().pointerInput(Unit) {
                        awaitPointerEventScope { while (true) awaitPointerEvent() }
                    })
                    // Grab handle: the whole window drags, this just says so.
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 5.dp).size(width = 28.dp, height = 4.dp)
                            .background(MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.7f), CircleShape)
                    )
                    if (playback.muted) {
                        Surface(
                            color = MaterialTheme.oneSpaceColors.mediaScrim,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp)
                        ) {
                            Text(
                                tr("Tắt tiếng", "Muted"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.oneSpaceColors.onMedia,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Row(Modifier.padding(start = 10.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                        Text(
                            if (playback.playing) tr("Đang phát · cửa sổ nổi", "Playing · floating window") else tr("Video · cửa sổ nổi", "Video · floating window"),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    actions()
                }
                PlaybackFeedback(
                    playback.status == PlaybackStatus.BUFFERING, playback.suppressed, playback.error,
                    Modifier.padding(horizontal = 10.dp).padding(bottom = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun FloatingAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = description, modifier = Modifier.size(19.dp))
    }
}

/**
 * Card the user can drag anywhere inside the content area. It keeps a 12dp margin from the edges,
 * and when released it settles against the nearest side edge (vertical position stays where it was).
 */
@Composable
private fun BoxScope.DraggableFloating(
    position: FloatingPosition,
    onHeightChanged: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val marginPx = with(LocalDensity.current) { 12.dp.toPx() }
    BoxWithConstraints(Modifier.matchParentSize()) {
        var size by remember { mutableStateOf(IntSize.Zero) }
        val maxW = constraints.maxWidth.toFloat()
        val maxH = constraints.maxHeight.toFloat()
        val minDx = -(maxW - size.width - 2 * marginPx).coerceAtLeast(0f)
        val minDy = -(maxH - size.height - 2 * marginPx).coerceAtLeast(0f)
        AppCard(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .offset { IntOffset(position.dx.coerceIn(minDx, 0f).roundToInt(), position.dy.coerceIn(minDy, 0f).roundToInt()) }
                .onSizeChanged { size = it; onHeightChanged(it.height) }
                // After the offset, so the tagged bounds follow the window wherever it is dragged.
                .testTag("floating-video")
                .pointerInput(minDx, minDy) {
                    detectDragGestures(
                        onDragEnd = {
                            val target = if (position.dx < minDx / 2f) minDx else 0f
                            scope.launch { animate(position.dx, target) { value, _ -> position.dx = value } }
                        }
                    ) { change, drag ->
                        change.consume()
                        position.dx = (position.dx + drag.x).coerceIn(minDx, 0f)
                        position.dy = (position.dy + drag.y).coerceIn(minDy, 0f)
                    }
                }
        ) { content() }
    }
}
