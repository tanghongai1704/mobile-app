package com.example.onespace.app.record

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Records the screen and the microphone into an .mp4 in the app's recordings folder. The system
 * only allows screen capture from a foreground service of type mediaProjection, started after the
 * user accepted the system capture prompt; this is that service. The Karaoke flow starts it and
 * talks to it through [ScreenRecorder].
 */
class ScreenRecordService : Service() {
    private var projection: MediaProjection? = null
    private var display: VirtualDisplay? = null
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val code = intent?.getIntExtra(EXTRA_CODE, 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>(EXTRA_DATA)
        val title = intent?.getStringExtra(EXTRA_TITLE).orEmpty()
        if (data == null) { stopSelf(); return START_NOT_STICKY }
        // Android 14: the foreground service must be running before the projection is used.
        startAsForeground()
        ScreenRecorder.attach(this)
        ScreenRecorder.ready = runCatching { begin(code, data, title) }.isSuccess
        if (!ScreenRecorder.ready) { cleanup(); ScreenRecorder.detach(); stopSelf() }
        return START_NOT_STICKY
    }

    private fun startAsForeground() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Ghi màn hình", NotificationManager.IMPORTANCE_LOW)
            )
        }
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("OneSpace đang ghi")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun begin(code: Int, data: Intent, title: String) {
        val metrics = resources.displayMetrics
        // Even sizes, capped near 720p so the file stays small and every encoder accepts it.
        val scale = minOf(1f, 1280f / maxOf(metrics.widthPixels, metrics.heightPixels))
        val width = (metrics.widthPixels * scale).toInt() / 2 * 2
        val height = (metrics.heightPixels * scale).toInt() / 2 * 2
        val dir = File(filesDir, "recordings").apply { mkdirs() }
        val safe = title.replace(Regex("[^\\p{L}\\p{N}]+"), "-").trim('-').take(40).ifBlank { "man-hinh" }
        val file = File(dir, "$safe-${System.currentTimeMillis()}.mp4")
        output = file

        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(this) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setVideoSize(width, height)
        r.setVideoFrameRate(30)
        r.setVideoEncodingBitRate(4_000_000)
        r.setAudioEncodingBitRate(128_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(file.absolutePath)
        r.prepare()
        recorder = r

        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val p = mpm.getMediaProjection(code, data) ?: error("No projection")
        // Android 14 requires a callback before the first virtual display.
        p.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() { ScreenRecorder.stop() }
        }, Handler(Looper.getMainLooper()))
        projection = p
        display = p.createVirtualDisplay(
            "onespace-record", width, height, metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, r.surface, null, null
        )
        r.start()
    }

    fun pauseTake() { runCatching { recorder?.pause() } }
    fun resumeTake() { runCatching { recorder?.resume() } }

    /** Finishes the file; true when a usable video was written. */
    fun finish(): Boolean {
        val r = recorder
        recorder = null
        val ok = r != null && runCatching { r.stop() }.isSuccess
        r?.release()
        if (!ok) output?.delete()
        cleanup()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        return ok
    }

    private fun cleanup() {
        runCatching { display?.release() }
        runCatching { projection?.stop() }
        runCatching { recorder?.release() }
        display = null; projection = null; recorder = null
    }

    override fun onDestroy() {
        cleanup()
        ScreenRecorder.detach()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_CODE = "code"
        const val EXTRA_DATA = "data"
        const val EXTRA_TITLE = "title"
        private const val CHANNEL = "screen-record"
        private const val NOTIFICATION_ID = 4711
    }
}

/** The Karaoke flow's handle on the running [ScreenRecordService]. */
object ScreenRecorder {
    @Volatile private var service: ScreenRecordService? = null
    @Volatile var ready: Boolean = false

    internal fun attach(s: ScreenRecordService) { service = s; ready = false }
    internal fun detach() { service = null; ready = false }

    /** Starts the service with the result of the system capture prompt. */
    fun start(context: Context, resultCode: Int, data: Intent, title: String) {
        ready = false
        val intent = Intent(context, ScreenRecordService::class.java)
            .putExtra(ScreenRecordService.EXTRA_CODE, resultCode)
            .putExtra(ScreenRecordService.EXTRA_DATA, data)
            .putExtra(ScreenRecordService.EXTRA_TITLE, title)
        ContextCompat.startForegroundService(context, intent)
    }

    /** True once the service is recording; waits up to [timeoutMs] for it to come up. */
    suspend fun awaitReady(timeoutMs: Long = 4000): Boolean {
        var waited = 0L
        while (!ready && waited < timeoutMs) { kotlinx.coroutines.delay(100); waited += 100 }
        return ready
    }

    fun pause() { service?.pauseTake() }
    fun resume() { service?.resumeTake() }

    /** Stops the take; true when a video file was saved. */
    fun stop(): Boolean = service?.finish() ?: false
}
