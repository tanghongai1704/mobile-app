package com.example.onespace.core.media

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView

/**
 * The single render surface for the shared video player (ADR-08: the same
 * player instance moves between the detail screen and the in-app floating
 * window; only this view re-attaches, the player is never recreated).
 */
@Composable
fun VideoSurface(player: Player?, modifier: Modifier = Modifier, onDoubleTap: () -> Unit = {}) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.pointerInput(player, onDoubleTap) {
            detectTapGestures(onDoubleTap = { onDoubleTap() })
        },
        factory = {
            PlayerView(context).apply {
                useController = false
                setShutterBackgroundColor(0)
            }
        },
        onRelease = { it.player = null },
        update = { view -> if (view.player !== player) view.player = player }
    )
}
