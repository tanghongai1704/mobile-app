package com.example.onespace.core.media

import android.view.LayoutInflater
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
 * Inflated from player_surface_texture.xml so output goes through TextureView:
 * several emulator images cannot composite SurfaceView video.
 */
@Composable
fun VideoSurface(player: Player?, modifier: Modifier = Modifier, onDoubleTap: () -> Unit = {}) {
    AndroidView(
        modifier = modifier.pointerInput(player, onDoubleTap) {
            detectTapGestures(onDoubleTap = { onDoubleTap() })
        },
        factory = { viewContext ->
            val view = LayoutInflater.from(viewContext)
                .inflate(R.layout.player_surface_texture, null) as PlayerView
            view.setShutterBackgroundColor(android.graphics.Color.TRANSPARENT)
            view
        },
        onRelease = { it.player = null },
        update = { view -> if (view.player !== player) view.player = player }
    )
}
