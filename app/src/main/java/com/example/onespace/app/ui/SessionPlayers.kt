package com.example.onespace.app.ui

import com.example.onespace.core.designsystem.oneSpaceColors
import com.example.onespace.core.designsystem.tr
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.draw.alpha
import com.example.onespace.core.designsystem.ScrubBar
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.abs
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
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Splitscreen
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
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

/**
 * Mini player dock: one compact row (48-56dp) — note tile, title above a progress line
 * that runs to the buttons, then speed / pause / close. Tap the tile or title to expand.
 * With [showProgress] off the music is not seekable: title only, no scrub bar.
 */
@Composable
fun MusicMiniPlayer(
    title: String, playback: MusicPlayback, onExpand: () -> Unit, onToggle: () -> Unit, onClose: () -> Unit,
    speed: Float = 1f, onCycleSpeed: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    showProgress: Boolean = true,
    /** False for a Beat Tiles backing track: progress + speed visible, pause/close hidden, no seek. */
    interactive: Boolean = true
) {
    val playing = playback.playRequested && playback.error == null
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.large.copy(
            bottomStart = androidx.compose.foundation.shape.ZeroCornerSize,
            bottomEnd = androidx.compose.foundation.shape.ZeroCornerSize
        ),
        shadowElevation = 3.dp
    ) {
        Row(
            Modifier.fillMaxWidth()
                .height(60.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Compact note tile; tapping it opens the player (no-op in games).
            Box(
                Modifier.size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small)
                    .clickable(onClick = onExpand),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.MusicNote, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            // Title + progress bar fill the middle. In games the progress is
            // read-only (no onSeek, no knob).
            Column(Modifier.weight(1f).clickable(onClick = onExpand)) {
                Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium)
                if (showProgress) {
                    ScrubBar(
                        playback.positionMs, playback.totalMs,
                        onSeek = if (interactive) onSeekTo else { _ -> },
                        trackHeight = 3.dp,
                        knobRadius = if (interactive) 5.dp else 0.dp
                    )
                }
            }
            // Speed always works (Beat Tiles plays along with it).
            Text(
                "x" + (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onCycleSpeed).padding(horizontal = 4.dp)
            )
            // Pause + close only outside games.
            if (interactive) {
                IconButton(onClick = onToggle, modifier = Modifier.size(36.dp)) {
                    Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"),
                        modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = tr("Đóng", "Close"),
                        modifier = Modifier.size(18.dp))
                }
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
    onHeightChanged: (Int) -> Unit = {},
    onSeekBy: (Long) -> Unit = {},
    speed: Float = 1f,
    onCycleSpeed: () -> Unit = {},
    onToggleMute: () -> Unit = {},
    onSeekTo: (Long) -> Unit = {},
    /** F-SPLIT: dock the video to the top of the screen (split mode) instead of the window. */
) {
    val playing = playback.playRequested && playback.error == null
    // F-SPLIT: the split button tells the shell (OneSpaceApp) to dock the video
    // at the top of the content column, pushing content down. The shell hides
    // this floating window while split is active.
    if (com.example.onespace.core.designsystem.SplitVideoState.active) return
    DraggableFloating(position, onHeightChanged) {
        // F-VID-09: sound-only mode renders a music-style mini player, no surface.
        if (com.example.onespace.core.designsystem.AudioOnlyState.active) {
            Column(Modifier.width(264.dp)) {
                Row(
                    Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                        PlaybackFeedback(playback.status == PlaybackStatus.BUFFERING, playback.suppressed, playback.error)
                    }
                    // F-SPLIT: the split button sits with expand/close.
                    FloatingAction(Icons.Filled.Splitscreen, tr("Chia đôi màn hình", "Split screen"), { com.example.onespace.core.designsystem.SplitVideoState.set(true) })
                    FloatingAction(Icons.Filled.OpenInFull, tr("Mở rộng", "Expand"), onExpand)
                    FloatingAction(
                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"), onToggle
                    )
                    FloatingAction(Icons.Filled.Close, tr("Đóng", "Close"), onClose)
                }
                // Progress of the video while only its sound is playing; drag to seek.
                ScrubBar(playback.positionMs, playback.totalMs, onSeek = onSeekTo, trackHeight = 3.dp)
            }
        } else {
            // Tap shows back-10 / play-pause / forward-10 and close / expand; they fade after 2s.
            var controls by remember { mutableStateOf(false) }
            var touches by remember { mutableIntStateOf(0) }
            LaunchedEffect(controls, touches, playing) {
                if (controls && playing) { kotlinx.coroutines.delay(2000); controls = false }
            }
            val alpha by androidx.compose.animation.core.animateFloatAsState(
                if (controls) 1f else 0f, androidx.compose.animation.core.tween(300), label = "floating-controls"
            )
            Column(Modifier.width(224.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
                    VideoSurface(player, Modifier.fillMaxSize().background(MaterialTheme.oneSpaceColors.mediaBackdrop))
                    // The video view would swallow touches; this layer shields it so a drag that
                    // starts on the picture still moves the whole window, and a tap shows controls.
                    Box(Modifier.matchParentSize().pointerInput(Unit) {
                        detectTapGestures(onTap = { controls = !controls; touches++ })
                    })
                    if (alpha > 0.01f) {
                        val onMedia = MaterialTheme.oneSpaceColors.onMedia
                        Box(Modifier.matchParentSize().alpha(alpha)) {
                            Box(Modifier.matchParentSize().background(MaterialTheme.oneSpaceColors.mediaScrim.copy(alpha = 0.35f)))
                            Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { touches++; onSeekBy(-10_000) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Filled.Replay10, contentDescription = tr("Lùi 10 giây", "Back 10 seconds"), tint = onMedia)
                                }
                                IconButton(onClick = { touches++; onToggle() }, modifier = Modifier.size(44.dp)) {
                                    Icon(
                                        if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"),
                                        tint = onMedia, modifier = Modifier.size(30.dp)
                                    )
                                }
                                IconButton(onClick = { touches++; onSeekBy(10_000) }, modifier = Modifier.size(40.dp)) {
                                    Icon(Icons.Filled.Forward10, contentDescription = tr("Tới 10 giây", "Forward 10 seconds"), tint = onMedia)
                                }
                            }
                            // Bottom row: speaker toggle on the left, expand / close on the right.
                            Row(
                                Modifier.align(Alignment.BottomStart).padding(bottom = 3.dp).fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FloatingAction(
                                    if (playback.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                    if (playback.muted) tr("Bật tiếng", "Unmute") else tr("Tắt tiếng", "Mute"),
                                    onToggleMute, onMedia
                                )
                                Row {
                                    // F-SPLIT: dock the video to the top of the screen (split mode).
                                    FloatingAction(Icons.Filled.Splitscreen, tr("Chia đôi màn hình", "Split screen"), { com.example.onespace.core.designsystem.SplitVideoState.set(true) }, onMedia)
                                    FloatingAction(Icons.Filled.OpenInFull, tr("Mở rộng", "Expand"), onExpand, onMedia)
                                    FloatingAction(Icons.Filled.Close, tr("Đóng", "Close"), onClose, onMedia)
                                }
                            }
                        }
                    }
                    // Grab handle: the whole window drags, this just says so.
                    Box(
                        Modifier.align(Alignment.TopCenter).padding(top = 5.dp).size(width = 28.dp, height = 4.dp)
                            .background(MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.7f), CircleShape)
                    )
                }
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                        // The channel name replaces the old "Video · floating window" caption.
                        Text(
                            playback.creator.ifBlank { title },
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Speed: x1 -> x1.5 -> x2 -> x0.5 -> x1 ..., one tap each.
                    TextButton(onClick = onCycleSpeed, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)) {
                        Text("x" + (if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()),
                            style = MaterialTheme.typography.labelLarge)
                    }
                }
                PlaybackFeedback(
                    playback.status == PlaybackStatus.BUFFERING, playback.suppressed, playback.error,
                    Modifier.padding(horizontal = 10.dp).padding(bottom = 6.dp)
                )
                // Thin draggable progress along the bottom edge of the panel: drag to seek.
                ScrubBar(playback.positionMs, playback.totalMs, onSeek = onSeekTo, trackHeight = 3.dp)
            }
        }
    }
}

/**
 * F-SPLIT: split mode — the video docks to the top of the screen as a fixed-height,
 * full-width surface (like the Games split, but on whatever tab the user is on) while
 * the tab's own content keeps running underneath. Its "thu nhỏ" button returns the video
 * to the draggable floating window. Sound-only mode mirrors the floating window's bar.
 */
@Composable
private fun BoxScope.SplitDock(
    player: Player?,
    title: String,
    playback: VideoPlayback,
    onExpand: () -> Unit,
    onToggle: () -> Unit,
    onClose: () -> Unit,
    onToggleMute: () -> Unit,
    onSeekBy: (Long) -> Unit,
    onSeekTo: (Long) -> Unit,
    onMinimize: () -> Unit
) {
    val playing = playback.playRequested && playback.error == null
    if (com.example.onespace.core.designsystem.AudioOnlyState.active) {
        // F-VID-09: sound-only split — a compact control bar docked to the top.
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            Row(
                Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Headphones, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                    PlaybackFeedback(playback.status == PlaybackStatus.BUFFERING, playback.suppressed, playback.error)
                }
                FloatingAction(
                    if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"), onToggle
                )
                // F-SPLIT: back to the draggable floating window.
                FloatingAction(Icons.Filled.PictureInPictureAlt, tr("Thu nhỏ video", "Minimize video"), onMinimize)
                FloatingAction(Icons.Filled.Close, tr("Đóng", "Close"), onClose)
            }
            ScrubBar(playback.positionMs, playback.totalMs, onSeek = onSeekTo, trackHeight = 3.dp)
        }
    } else {
        // Tap shows the transport controls; they fade after 2s, like the floating window.
        var controls by remember { mutableStateOf(true) }
        var touches by remember { mutableIntStateOf(0) }
        LaunchedEffect(controls, touches, playing) {
            if (controls && playing) { kotlinx.coroutines.delay(2000); controls = false }
        }
        val alpha by androidx.compose.animation.core.animateFloatAsState(
            if (controls) 1f else 0f, androidx.compose.animation.core.tween(300), label = "split-controls"
        )
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth().aspectRatio(16f / 9f), contentAlignment = Alignment.Center) {
            VideoSurface(player, Modifier.fillMaxSize().background(MaterialTheme.oneSpaceColors.mediaBackdrop))
            // The video view would swallow touches; this layer makes a tap toggle the controls.
            Box(Modifier.matchParentSize().pointerInput(Unit) {
                detectTapGestures(onTap = { controls = !controls; touches++ })
            })
            if (alpha > 0.01f) {
                val onMedia = MaterialTheme.oneSpaceColors.onMedia
                Box(Modifier.matchParentSize().alpha(alpha)) {
                    Box(Modifier.matchParentSize().background(MaterialTheme.oneSpaceColors.mediaScrim.copy(alpha = 0.35f)))
                    Row(Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { touches++; onSeekBy(-10_000) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Replay10, contentDescription = tr("Lùi 10 giây", "Back 10 seconds"), tint = onMedia)
                        }
                        IconButton(onClick = { touches++; onToggle() }, modifier = Modifier.size(44.dp)) {
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (playing) tr("Tạm dừng", "Pause") else tr("Phát", "Play"),
                                tint = onMedia, modifier = Modifier.size(30.dp)
                            )
                        }
                        IconButton(onClick = { touches++; onSeekBy(10_000) }, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Filled.Forward10, contentDescription = tr("Tới 10 giây", "Forward 10 seconds"), tint = onMedia)
                        }
                    }
                    // Top row: speaker toggle on the left, minimize / expand / close on the right.
                    Row(
                        Modifier.align(Alignment.TopStart).padding(top = 4.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FloatingAction(
                            if (playback.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            if (playback.muted) tr("Bật tiếng", "Unmute") else tr("Tắt tiếng", "Mute"),
                            onToggleMute, onMedia
                        )
                        Row {
                            // F-SPLIT: back to the draggable floating window.
                            FloatingAction(Icons.Filled.PictureInPictureAlt, tr("Thu nhỏ video", "Minimize video"), onMinimize, onMedia)
                            FloatingAction(Icons.Filled.OpenInFull, tr("Mở rộng", "Expand"), onExpand, onMedia)
                            FloatingAction(Icons.Filled.Close, tr("Đóng", "Close"), onClose, onMedia)
                        }
                    }
                }
            }
            // Thin draggable progress along the bottom edge of the docked video.
            ScrubBar(
                playback.positionMs, playback.totalMs,
                modifier = Modifier.align(Alignment.BottomCenter),
                onSeek = { touches++; onSeekTo(it) },
                interactive = alpha > 0.5f,
                trackHeight = if (alpha > 0.5f) 3.dp else 2.dp,
                knobRadius = 6.dp,
                trackColor = MaterialTheme.oneSpaceColors.onMedia.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun FloatingAction(
    icon: ImageVector, description: String, onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = androidx.compose.ui.graphics.Color.Unspecified
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = description, tint = if (tint == androidx.compose.ui.graphics.Color.Unspecified) LocalContentColor.current else tint, modifier = Modifier.size(19.dp))
    }
}

/**
 * Card the user can drag anywhere inside the content area. It keeps a 12dp margin from the edges,
 * and when released it settles against the nearest side edge (vertical position stays where it was).
 * F-ZOOM: pinch-to-zoom grows/shrinks the window (0.5x..2x); pan and pinch share one gesture
 * handler so they can never steal events from each other.
 */
@Composable
private fun BoxScope.DraggableFloating(
    position: FloatingPosition,
    onHeightChanged: (Int) -> Unit,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val marginPx = with(LocalDensity.current) { 12.dp.toPx() }
    // F-ZOOM: pinch scale of the window, kept across it leaving/re-entering composition.
    var scale by rememberSaveable { mutableStateOf(1f) }
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
                // F-ZOOM: the window grows/shrinks around its centre; the layout (and so the
                // drag clamp) keeps the unscaled size.
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .onSizeChanged { size = it; onHeightChanged(it.height) }
                // After the offset, so the tagged bounds follow the window wherever it is dragged.
                .testTag("floating-video")
                // F-ZOOM: one handler for both pan (one finger) and pinch zoom (two fingers) —
                // the transform detector's job; on release it still settles against the nearest
                // side edge, exactly like the drag-only handler it replaces.
                .pointerInput(minDx, minDy) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var pastSlop = false
                        var zoomAccum = 1f
                        val touchSlop = viewConfiguration.touchSlop
                        var canceled = false
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.any { it.isConsumed }) { canceled = true; break }
                            if (!event.changes.any { it.pressed }) break
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (!pastSlop) {
                                zoomAccum *= zoomChange
                                // Zoom slop in pixels: how far the fingers have spread apart.
                                val pressed = event.changes.filter { it.pressed }
                                val spread = if (pressed.size >= 2) {
                                    (pressed[0].position - pressed[1].position).getDistance()
                                } else 0f
                                if (abs(1f - zoomAccum) * spread > touchSlop || panChange.getDistance() > touchSlop) {
                                    pastSlop = true
                                }
                            }
                            if (pastSlop) {
                                scale = (scale * zoomChange).coerceIn(0.5f, 2f)
                                position.dx = (position.dx + panChange.x).coerceIn(minDx, 0f)
                                position.dy = (position.dy + panChange.y).coerceIn(minDy, 0f)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                        if (pastSlop && !canceled) {
                            val target = if (position.dx < minDx / 2f) minDx else 0f
                            scope.launch { animate(position.dx, target) { value, _ -> position.dx = value } }
                        }
                    }
                },
            // The floating window keeps rounded corners for drag affordance.
            shape = MaterialTheme.shapes.medium
        ) { content() }
    }
}
