package com.example.onespace.app.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaRecorder
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import com.example.onespace.core.designsystem.oneSpaceColors
import com.example.onespace.core.designsystem.tr
import com.example.onespace.core.media.VideoSurface
import kotlinx.coroutines.delay
import java.io.File

/** What is being recorded over: a video (picture + sound) or a song (sound only). */
class RecordSource(val title: String, val isVideo: Boolean)

private enum class RecordStage { Mode, MicIntro, MicDenied, Countdown, Recording }

/** Which kind of take the user picked. */
private enum class RecordKind { WithVideo, WithAudio }

/**
 * Karaoke / Record. The flow is: pick a mode, ask for the microphone the first time (with a
 * short explanation), count 3-2-1, then record. While recording the screen is taken over (video
 * full screen, back does nothing); only the stop button leaves it.
 *
 * The voice goes to an .m4a file in the app's recordings folder. Capturing the screen itself needs
 * MediaProjection and a foreground service and is not wired up yet: the video mode records the
 * voice while showing the video full screen.
 */
@Composable
fun RecordFlow(
    source: RecordSource,
    player: Player?,
    isPlaying: () -> Boolean,
    onPlayPauseSource: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(RecordStage.Mode) }
    var kind by remember { mutableStateOf(RecordKind.WithAudio) }
    var count by remember { mutableIntStateOf(3) }
    var paused by remember { mutableStateOf(false) }
    var micOn by remember { mutableStateOf(true) }
    var elapsed by remember { mutableIntStateOf(0) }
    val recorder = remember { VoiceRecorder(context) }
    val micAllowed = {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }
    val savedMsg = tr("Đã lưu bản ghi vào thư mục Bản ghi của ứng dụng", "Recording saved to the app's Recordings folder")
    val failedMsg = tr("Không ghi được. Thử lại sau nhé", "Could not record. Please try again")

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) { count = 3; stage = RecordStage.Countdown } else stage = RecordStage.MicDenied
    }

    fun choose(k: RecordKind) {
        kind = k
        if (micAllowed()) { count = 3; stage = RecordStage.Countdown } else stage = RecordStage.MicIntro
    }

    DisposableEffect(Unit) { onDispose { recorder.release() } }

    LaunchedEffect(stage) {
        if (stage == RecordStage.Countdown) {
            while (count > 0) { delay(1000); count -= 1 }
            if (recorder.start(source.title)) {
                elapsed = 0; paused = false; micOn = true
                if (!isPlaying()) onPlayPauseSource()
                stage = RecordStage.Recording
            } else {
                android.widget.Toast.makeText(context, failedMsg, android.widget.Toast.LENGTH_SHORT).show()
                onDismiss()
            }
        }
    }
    LaunchedEffect(stage, paused) {
        while (stage == RecordStage.Recording && !paused) { delay(1000); elapsed += 1 }
    }

    fun stop() {
        val saved = recorder.stop()
        runCatching { (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMicrophoneMute = false }
        if (isPlaying()) onPlayPauseSource()
        if (saved) android.widget.Toast.makeText(context, savedMsg, android.widget.Toast.LENGTH_LONG).show()
        onDismiss()
    }

    when (stage) {
        RecordStage.Mode -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(tr("Bạn muốn ghi như thế nào?", "How do you want to record?")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(source.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    if (source.isVideo) ModeCard(
                        Icons.Filled.Videocam,
                        tr("Ghi cùng video", "Record with the video"),
                        tr("Hình và tiếng của video cùng giọng bạn", "The video's picture and sound, with your voice")
                    ) { choose(RecordKind.WithVideo) }
                    ModeCard(
                        Icons.Filled.GraphicEq,
                        if (source.isVideo) tr("Ghi cùng âm thanh video", "Record with the video's sound")
                        else tr("Hát cùng bài hát", "Sing along with the song"),
                        tr("Chỉ ghi âm thanh và giọng của bạn, như karaoke", "Sound only: karaoke with your voice")
                    ) { choose(RecordKind.WithAudio) }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Hủy", "Cancel")) } }
        )
        RecordStage.MicIntro -> MicIntro(
            onAllow = { permission.launch(Manifest.permission.RECORD_AUDIO) },
            onDismiss = onDismiss
        )
        RecordStage.MicDenied -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Filled.MicOff, contentDescription = null) },
            title = { Text(tr("Chưa có quyền micro", "No microphone access")) },
            text = {
                Text(tr("Bạn có thể bật lại quyền micro trong Cài đặt của điện thoại để dùng tính năng này.",
                    "You can turn the microphone back on in the phone's Settings to use this feature."))
            },
            confirmButton = {
                TextButton(onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    onDismiss()
                }) { Text(tr("Mở cài đặt", "Open settings")) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Để sau", "Not now")) } }
        )
        RecordStage.Countdown, RecordStage.Recording -> {
            // The take owns the whole screen (its own window above the bars): back and outside
            // taps do nothing until the stop button is pressed.
            androidx.compose.ui.window.Dialog(
                onDismissRequest = {},
                properties = androidx.compose.ui.window.DialogProperties(
                    dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false,
                    decorFitsSystemWindows = false
                )
            ) {
            BackHandler(enabled = true) {}
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.oneSpaceColors.mediaBackdrop) {
                Box(Modifier.fillMaxSize()) {
                    if (kind == RecordKind.WithVideo) {
                        VideoSurface(player, Modifier.fillMaxSize().background(MaterialTheme.oneSpaceColors.mediaBackdrop))
                    } else {
                        Column(
                            Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null,
                                tint = MaterialTheme.oneSpaceColors.onMedia, modifier = Modifier.size(72.dp))
                            Text(source.title, style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.oneSpaceColors.onMedia, textAlign = TextAlign.Center)
                        }
                    }
                    if (stage == RecordStage.Countdown) {
                        Box(Modifier.fillMaxSize().background(MaterialTheme.oneSpaceColors.mediaScrim), contentAlignment = Alignment.Center) {
                            Text(
                                if (count > 0) count.toString() else tr("Ghi!", "Go!"),
                                style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.oneSpaceColors.onMedia
                            )
                        }
                    } else {
                        RecordingBar(
                            elapsed = elapsed,
                            paused = paused,
                            micOn = micOn,
                            onPauseResume = {
                                paused = !paused
                                if (paused) recorder.pause() else recorder.resume()
                                onPlayPauseSource()
                            },
                            onMic = {
                                micOn = !micOn
                                runCatching {
                                    (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).isMicrophoneMute = !micOn
                                }
                            },
                            onStop = ::stop,
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
private fun ModeCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** First-use explanation shown before the system microphone prompt. */
@Composable
private fun MicIntro(onAllow: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Mic, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(tr("Cho phép dùng micro", "Allow the microphone")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("OneSpace cần micro để ghi giọng của bạn khi hát cùng video hoặc bài hát.",
                    "OneSpace needs the microphone to record your voice while you sing along."))
                Text(tr("Bản ghi chỉ được lưu trên điện thoại của bạn. Bạn có thể tắt micro bất cứ lúc nào khi đang ghi.",
                    "Recordings stay on your phone. You can mute the microphone at any time while recording."),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { Button(onClick = onAllow) { Text(tr("Cho phép", "Allow")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Để sau", "Not now")) } }
    )
}

/** Pause / resume, stop and microphone on-off, with the elapsed time. */
@Composable
private fun RecordingBar(
    elapsed: Int, paused: Boolean, micOn: Boolean,
    onPauseResume: () -> Unit, onMic: () -> Unit, onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth().background(MaterialTheme.oneSpaceColors.mediaScrim).padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(10.dp).background(if (paused) MaterialTheme.oneSpaceColors.onMedia else MaterialTheme.colorScheme.error, CircleShape))
            Text(
                "%02d:%02d".format(elapsed / 60, elapsed % 60) + if (paused) "  " + tr("Tạm dừng", "Paused") else "",
                style = MaterialTheme.typography.titleMedium, color = MaterialTheme.oneSpaceColors.onMedia
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick = onPauseResume, modifier = Modifier.size(56.dp)) {
                Icon(
                    if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                    contentDescription = if (paused) tr("Tiếp tục ghi", "Resume recording") else tr("Tạm dừng ghi", "Pause recording")
                )
            }
            FilledIconButton(
                onClick = onStop, modifier = Modifier.size(72.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError
                )
            ) { Icon(Icons.Filled.Stop, contentDescription = tr("Dừng ghi", "Stop recording"), modifier = Modifier.size(34.dp)) }
            FilledTonalIconButton(onClick = onMic, modifier = Modifier.size(56.dp)) {
                Icon(
                    if (micOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                    contentDescription = if (micOn) tr("Tắt micro", "Mute microphone") else tr("Bật micro", "Unmute microphone")
                )
            }
        }
    }
}

/** MediaRecorder wrapper: one take at a time, saved as .m4a in the app's recordings folder. */
private class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null

    fun start(title: String): Boolean = runCatching {
        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        val safe = title.replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').take(40).ifBlank { "ban-ghi" }
        val out = File(dir, "$safe-${System.currentTimeMillis()}.m4a")
        val r = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(out.absolutePath)
        r.prepare()
        r.start()
        recorder = r; file = out
    }.isSuccess

    fun pause() { runCatching { recorder?.pause() } }
    fun resume() { runCatching { recorder?.resume() } }

    /** Finishes the take; true when a file was written. */
    fun stop(): Boolean {
        val r = recorder ?: return false
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        if (!ok) file?.delete()
        return ok
    }

    fun release() { runCatching { recorder?.release() }; recorder = null }
}
