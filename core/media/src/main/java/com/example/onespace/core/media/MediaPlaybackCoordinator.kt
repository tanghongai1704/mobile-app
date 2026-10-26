package com.example.onespace.core.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.session.VideoStartPlan
import com.example.onespace.domain.session.port.PlaybackStatus
import com.example.onespace.domain.session.VideoAudioEffect
import com.example.onespace.domain.session.port.MediaSessionPort
import com.example.onespace.domain.session.port.MusicPlayback
import com.example.onespace.domain.session.port.VideoPlayback
import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.ViewingSessionRepository
import com.example.onespace.domain.session.repository.SessionPreferencesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared video/audio engines: in-app floating, dock and detail screens reuse them.
 * Music currently delegates audio focus to Media3; complete video focus/noisy handling
 * and service-owned background playback are follow-up work in Section 4.
 * Internal conflict policy allows one audible primary source at a time.
 */
@Singleton
class MediaPlaybackCoordinator @Inject constructor(
    @ApplicationContext context: Context,
    private val viewingSession: ViewingSessionRepository,
    preferences: SessionPreferencesRepository
) : MediaSessionPort {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val mediaAudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
        .build()

    // Emulator images (esp. ARM64) ship a fragile software avc decoder; falling
    // back to the next decoder keeps real streams playing instead of erroring out.
    private val renderersFactory = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(true)

    private val videoEngine: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        // The app arbitrates the video voice itself (case C1): muting must keep the
        // frames rolling, so the video player must not react to system audio focus.
        .setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ false)
        .build()

    private val musicPlayer: ExoPlayer = ExoPlayer.Builder(context, renderersFactory)
        // Dock music yields to any other audible source (calls, other apps, video).
        .setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ true)
        .build()

    /** Platform surface handle; audio engine and playlist are private implementation details. */
    val videoPlayer: Player get() = videoEngine

    private val _music = MutableStateFlow(MusicPlayback())
    override val music: StateFlow<MusicPlayback> = _music.asStateFlow()

    private val _video = MutableStateFlow(VideoPlayback())
    override val video: StateFlow<VideoPlayback> = _video.asStateFlow()

    private var queue: List<Track> = emptyList()

    // User's video volume for this session; separate from the C1 conflict
    // mute, so a volume pick never lifts a mute the conflict rule forced.
    private var videoVolume = 1f
    private var videoMutedByConflict = false

    init {
        musicPlayer.repeatMode = Player.REPEAT_MODE_ALL
        musicPlayer.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = syncMusicState()
        })
        videoEngine.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) = syncVideoState()
        })
        // Persist the video resume position on a slow cadence (F-VID-05, TC10).
        scope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                val current = _video.value
                val videoId = current.videoId
                if (videoId != null && current.playing) {
                    viewingSession.saveProgress(videoId, videoEngine.currentPosition)
                }
            }
        }
        // Data saver (F-COM-13): cap playback quality to roughly 720p.
        scope.launch {
            preferences.preferences.collect { prefs ->
                videoEngine.trackSelectionParameters = videoEngine.trackSelectionParameters
                    .buildUpon()
                    .setMaxVideoSize(if (prefs.dataSaver) Int.MAX_VALUE else Int.MAX_VALUE,
                        if (prefs.dataSaver) 720 else Int.MAX_VALUE)
                    .build()
            }
        }
        // Position ticker for the dock, progress bars and sliders.
        scope.launch {
            while (isActive) {
                delay(POSITION_TICK_MS)
                if (musicPlayer.playbackState != Player.STATE_IDLE) syncMusicState()
                if (videoEngine.playbackState != Player.STATE_IDLE) syncVideoState()
            }
        }
    }

    // ---- Music ----

    override fun requestMusic(track: Track, queue: List<Track>, effect: VideoAudioEffect) {
        when (effect) {
            // The video keeps playing visually without audio (ADR-07 single audible source).
            VideoAudioEffect.MUTE -> { videoMutedByConflict = true; videoEngine.volume = 0f; syncVideoState() }
            VideoAudioEffect.PAUSE -> videoEngine.pause()
            VideoAudioEffect.NONE -> Unit
        }
        playTrack(track, queue)
    }

    override fun prepareRestoredMusic(track: Track, queue: List<Track>) {
        val index = queue.indexOfFirst { it.id == track.id }
        if (index < 0) return
        this.queue = queue.toList()
        musicPlayer.pause()
        musicPlayer.setMediaItems(this.queue.map(::mediaItem), index, 0)
        musicPlayer.prepare()
        syncMusicState()
    }

    override fun pauseMusic() {
        musicPlayer.pause()
        syncMusicState()
    }

    override fun stopMusic() {
        musicPlayer.pause()
        musicPlayer.stop()
        musicPlayer.clearMediaItems()
        queue = emptyList()
        _music.value = MusicPlayback()
    }

    override fun seekMusicTo(positionMs: Long) {
        musicPlayer.seekTo(positionMs.coerceAtLeast(0))
        syncMusicState()
    }

    override fun removeTrackFromQueue(trackId: String) {
        val old = _music.value.queue
        val newQueue = old.filterNot { it.id == trackId }
        if (newQueue.size == old.size) return
        queue = newQueue
        val currentId = _music.value.trackId
        val currentPosition = musicPlayer.currentPosition
        if (newQueue.isEmpty()) {
            musicPlayer.stop()
            musicPlayer.clearMediaItems()
            _music.value = MusicPlayback()
        } else {
            val currentIndex = newQueue.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            musicPlayer.setMediaItems(newQueue.map(::mediaItem), currentIndex, currentPosition)
            musicPlayer.prepare()
            // Full rebuild to guarantee the state change propagates.
            _music.value = MusicPlayback(
                trackId = currentId,
                title = _music.value.title,
                artist = _music.value.artist,
                playing = musicPlayer.isPlaying,
                playRequested = musicPlayer.playWhenReady,
                status = statusOf(musicPlayer),
                queue = newQueue,
                positionMs = currentPosition.coerceAtLeast(0),
                totalMs = _music.value.totalMs
            )
        }
    }

    override fun moveTrackInQueue(fromIndex: Int, toIndex: Int) {
        val old = _music.value.queue
        if (fromIndex == toIndex || fromIndex !in old.indices || toIndex !in old.indices) return
        val newQueue = old.toMutableList().apply { add(toIndex, removeAt(fromIndex)) }
        queue = newQueue
        // Moving inside the player keeps the current item playing — no rebuild.
        musicPlayer.moveMediaItem(fromIndex, toIndex)
        syncMusicState()
    }

    private fun playTrack(track: Track, queue: List<Track>) {
        val index = queue.indexOfFirst { it.id == track.id }
        if (index < 0) return
        val changed = this.queue != queue || musicPlayer.mediaItemCount != queue.size
        this.queue = queue.toList()
        if (changed) {
            musicPlayer.setMediaItems(this.queue.map(::mediaItem), index, 0)
            musicPlayer.prepare()
        } else {
            if (musicPlayer.currentMediaItemIndex != index || musicPlayer.playbackState == Player.STATE_ENDED) {
                musicPlayer.seekToDefaultPosition(index)
            }
            if (musicPlayer.playbackState == Player.STATE_IDLE || musicPlayer.playerError != null) {
                musicPlayer.prepare()
            }
        }
        musicPlayer.play()
        syncMusicState()
    }

    private fun mediaItem(track: Track): MediaItem = MediaItem.Builder()
        .setUri(track.url)
        .setMediaId(track.id)
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle(track.title).setArtist(track.artist).build()
        )
        .build()

    private fun syncMusicState() {
        val item = musicPlayer.currentMediaItem ?: return
        val track = queue.firstOrNull { it.id == item.mediaId }
        _music.value = MusicPlayback(
            trackId = item.mediaId,
            title = track?.title ?: item.mediaMetadata.title?.toString().orEmpty(),
            artist = track?.artist ?: item.mediaMetadata.artist?.toString().orEmpty(),
            playing = musicPlayer.isPlaying,
            playRequested = musicPlayer.playWhenReady && musicPlayer.playbackState != Player.STATE_ENDED,
            status = statusOf(musicPlayer),
            suppressed = musicPlayer.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE,
            error = musicPlayer.playerError?.let { "Không phát được nhạc. Kiểm tra kết nối rồi nhấn Phát để thử lại." },
            queue = queue,
            positionMs = musicPlayer.currentPosition.coerceAtLeast(0),
            totalMs = durationOf(musicPlayer)
        )
    }

    // ---- Video ----

    override fun openVideo(video: Video, startPositionMs: Long, plan: com.example.onespace.domain.session.VideoStartPlan.Play) {
        if (plan.pauseMusic) musicPlayer.pause()
        videoEngine.setMediaItem(
            MediaItem.Builder()
                .setUri(video.url)
                .setMediaId(video.id)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(video.title).setArtist(video.creator).build()
                )
                .build()
        )
        videoEngine.prepare()
        if (startPositionMs > 0) videoEngine.seekTo(startPositionMs)
        // A new video restarts the audio voice at the session volume.
        // C2: when music keeps playing (pauseMusic=false), the video opens
        // muted. Disable the music player's audio focus reaction BEFORE the
        // video starts so it does not pause itself when the new player opens.
        // C2: when music keeps playing (pauseMusic=false), the video opens
        // muted. The music engine's audio focus handling resumes it on
        // devices with a standard audio pipeline; the emulator may pause it.
        if (!plan.pauseMusic) videoMutedByConflict = true
        videoEngine.volume = if (plan.pauseMusic) videoVolume else 0f
        videoEngine.play()
        syncVideoState()
    }

    override fun resumeVideo(plan: com.example.onespace.domain.session.VideoStartPlan.Play) {
        if (videoEngine.currentMediaItem == null) return
        if (plan.pauseMusic) musicPlayer.pause()
        if (videoEngine.playbackState == Player.STATE_ENDED) videoEngine.seekTo(0)
        if (videoEngine.playbackState == Player.STATE_IDLE || videoEngine.playerError != null) videoEngine.prepare()
        videoEngine.play()
        syncVideoState()
    }

    override fun pauseVideo() {
        videoEngine.pause()
        syncVideoState()
    }

    override fun seekVideoBy(deltaMs: Long) {
        videoEngine.seekTo((videoEngine.currentPosition + deltaMs).coerceIn(0, durationOf(videoEngine)))
    }

    override fun seekVideoTo(positionMs: Long) {
        videoEngine.seekTo(positionMs.coerceIn(0, durationOf(videoEngine)))
    }

    override fun setVideoVolume(volume: Float) {
        videoVolume = volume.coerceIn(0f, 1f)
        // The C1 conflict mute stays until a new video starts.
        if (!videoMutedByConflict) videoEngine.volume = videoVolume
        if (videoEngine.currentMediaItem != null) syncVideoState()
    }

    override fun dismissVideo(toWatchlist: Boolean) {
        val current = _video.value
        val videoId = current.videoId
        val position = videoEngine.currentPosition
        val hasContent = videoEngine.currentMediaItem != null
        if (videoId != null && hasContent) {
            scope.launch {
                viewingSession.saveProgress(videoId, position)
                if (toWatchlist) viewingSession.addToWatchlist(videoId, current.title)
            }
        }
        videoEngine.stop()
        videoEngine.clearMediaItems()
        _video.value = VideoPlayback()
    }

    private fun syncVideoState() {
        val item = videoEngine.currentMediaItem ?: if (videoEngine.playbackState == Player.STATE_IDLE) {
            _video.value = VideoPlayback()
            return
        } else null
        if (item == null) return
        _video.value = VideoPlayback(
            videoId = item.mediaId,
            title = item.mediaMetadata.title?.toString().orEmpty(),
            playing = videoEngine.isPlaying,
            playRequested = videoEngine.playWhenReady && videoEngine.playbackState != Player.STATE_ENDED,
            status = statusOf(videoEngine),
            suppressed = videoEngine.playbackSuppressionReason != Player.PLAYBACK_SUPPRESSION_REASON_NONE,
            error = videoEngine.playerError?.let { "Không phát được video. Kiểm tra kết nối rồi nhấn Phát để thử lại." },
            positionMs = videoEngine.currentPosition.coerceAtLeast(0),
            totalMs = durationOf(videoEngine),
            muted = videoEngine.volume == 0f
        )
    }

    private fun statusOf(player: Player): PlaybackStatus = when {
        player.playerError != null -> PlaybackStatus.ERROR
        player.playbackState == Player.STATE_BUFFERING -> PlaybackStatus.BUFFERING
        player.playbackState == Player.STATE_READY -> PlaybackStatus.READY
        player.playbackState == Player.STATE_ENDED -> PlaybackStatus.ENDED
        else -> PlaybackStatus.IDLE
    }

    private fun durationOf(player: Player): Long =
        player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0

    fun release() {
        scope.cancel()
        musicPlayer.release()
        videoEngine.release()
    }

    companion object {
        private const val POSITION_TICK_MS = 500L
        private const val PROGRESS_SAVE_INTERVAL_MS = 3_000L
    }
}

@Module
@InstallIn(SingletonComponent::class)
object MediaModule {
    @Provides @Singleton
    fun provideMediaSessionPort(coordinator: MediaPlaybackCoordinator): MediaSessionPort = coordinator
    @Provides @Singleton
    fun provideTtsEngine(engine: AndroidTtsEngine): com.example.onespace.domain.news.repository.TtsEngine = engine
}
