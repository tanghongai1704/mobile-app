package com.example.onespace.core.media

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.usecase.SelectNextTrack
import com.example.onespace.domain.session.MediaConflictRule
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
 * PlaybackCoordinator from the report: one shared engine for every audible
 * content, so PIP, the dock and detail screens never recreate a player. The
 * ExoPlayer instances handle Android audio focus natively — one audible source
 * at a time (ADR-07), the dock music yields to user-started video or TTS.
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

    val videoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        // The app arbitrates the video voice itself (case C1): muting must keep the
        // frames rolling, so the video player must not react to system audio focus.
        .setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ false)
        .build()

    val musicPlayer: ExoPlayer = ExoPlayer.Builder(context)
        // Dock music yields to any other audible source (calls, other apps, video).
        .setAudioAttributes(mediaAudioAttributes, /* handleAudioFocus = */ true)
        .build()

    private val _music = MutableStateFlow(MusicPlayback())
    override val music: StateFlow<MusicPlayback> = _music.asStateFlow()

    private val _video = MutableStateFlow(VideoPlayback())
    override val video: StateFlow<VideoPlayback> = _video.asStateFlow()

    private var queue: List<Track> = emptyList()

    init {
        musicPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = syncMusicState()
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) skipToNext()
                syncMusicState()
            }
        })
        videoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) = syncVideoState()
            override fun onPlaybackStateChanged(state: Int) = syncVideoState()
        })
        // Persist the video resume position on a slow cadence (F-VID-05, TC10).
        scope.launch {
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                val current = _video.value
                val videoId = current.videoId
                if (videoId != null && current.playing) {
                    viewingSession.saveProgress(videoId, videoPlayer.currentPosition)
                }
            }
        }
        // Data saver (F-COM-13): cap playback quality to roughly 720p.
        scope.launch {
            preferences.preferences.collect { prefs ->
                videoPlayer.trackSelectionParameters = videoPlayer.trackSelectionParameters
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
                if (videoPlayer.playbackState != Player.STATE_IDLE) syncVideoState()
            }
        }
    }

    // ---- Music ----

    override fun requestMusic(track: Track, queue: List<Track>, effect: VideoAudioEffect) {
        when (effect) {
            // The video keeps playing visually without audio (ADR-07 single audible source).
            VideoAudioEffect.MUTE -> { videoPlayer.volume = 0f; syncVideoState() }
            VideoAudioEffect.PAUSE -> videoPlayer.pause()
            VideoAudioEffect.NONE -> Unit
        }
        playTrack(track, queue)
    }

    override fun resolveMusicConflict(track: Track, queue: List<Track>, rule: MediaConflictRule) {
        if (rule == MediaConflictRule.KEEP_VIDEO) return
        requestMusic(track, queue, when (rule) {
            MediaConflictRule.MUTE_VIDEO -> VideoAudioEffect.MUTE
            MediaConflictRule.PAUSE_VIDEO -> VideoAudioEffect.PAUSE
            MediaConflictRule.KEEP_VIDEO -> VideoAudioEffect.NONE
        })
    }

    override fun prepareRestoredMusic(track: Track, queue: List<Track>) {
        this.queue = queue
        musicPlayer.pause()
        musicPlayer.setMediaItem(mediaItem(track))
        musicPlayer.prepare()
        _music.value = MusicPlayback(
            trackId = track.id, title = track.title, artist = track.artist,
            playing = false, positionMs = 0, totalMs = 0
        )
    }

    override fun toggleMusic() {
        if (musicPlayer.playWhenReady) musicPlayer.pause() else {
            if (videoPlayer.volume > 0f) videoPlayer.pause()
            musicPlayer.play()
        }
        syncMusicState()
    }

    override fun stopMusic() {
        musicPlayer.pause()
        musicPlayer.stop()
        musicPlayer.clearMediaItems()
        _music.value = MusicPlayback()
    }

    override fun seekMusicTo(positionMs: Long) {
        musicPlayer.seekTo(positionMs.coerceAtLeast(0))
        syncMusicState()
    }

    override fun skipToNext() {
        SelectNextTrack(queue, _music.value.trackId)?.let { playTrack(it, queue) }
    }

    override fun skipToPrevious() {
        if (queue.isEmpty()) return
        val index = queue.indexOfFirst { it.id == _music.value.trackId }
        val previous = queue[(index - 1 + queue.size) % queue.size]
        playTrack(previous, queue)
    }

    private fun playTrack(track: Track, queue: List<Track>) {
        this.queue = queue
        if (videoPlayer.volume > 0f) videoPlayer.pause()
        if (musicPlayer.currentMediaItem?.mediaId != track.id || musicPlayer.playbackState == Player.STATE_IDLE) {
            musicPlayer.setMediaItem(mediaItem(track))
            musicPlayer.prepare()
        } else if (musicPlayer.playbackState == Player.STATE_ENDED) {
            musicPlayer.seekTo(0)
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
            // playWhenReady = user intent; true while buffering too, so transport UI is stable.
            playing = musicPlayer.playWhenReady && musicPlayer.playbackState != Player.STATE_IDLE,
            positionMs = musicPlayer.currentPosition.coerceAtLeast(0),
            totalMs = durationOf(musicPlayer)
        )
    }

    // ---- Video ----

    override fun openVideo(video: Video, startPositionMs: Long) {
        // ADR-07, one audible source: opening a user-started video parks the dock music.
        musicPlayer.pause()
        videoPlayer.setMediaItem(
            MediaItem.Builder()
                .setUri(video.url)
                .setMediaId(video.id)
                .setMediaMetadata(
                    MediaMetadata.Builder().setTitle(video.title).setArtist(video.creator).build()
                )
                .build()
        )
        videoPlayer.prepare()
        if (startPositionMs > 0) videoPlayer.seekTo(startPositionMs)
        videoPlayer.volume = 1f
        videoPlayer.play()
        syncVideoState()
    }

    override fun toggleVideo() {
        if (videoPlayer.playWhenReady && videoPlayer.playbackState != Player.STATE_ENDED) videoPlayer.pause() else {
            if (videoPlayer.volume > 0f) musicPlayer.pause()
            if (videoPlayer.playbackState == Player.STATE_ENDED) videoPlayer.seekTo(0)
            if (videoPlayer.playbackState == Player.STATE_IDLE) videoPlayer.prepare()
            videoPlayer.play()
        }
        syncVideoState()
    }

    override fun pauseVideo() {
        videoPlayer.pause()
        syncVideoState()
    }

    override fun seekVideoBy(deltaMs: Long) {
        videoPlayer.seekTo((videoPlayer.currentPosition + deltaMs).coerceIn(0, durationOf(videoPlayer)))
    }

    override fun dismissVideo(toWatchlist: Boolean) {
        val current = _video.value
        val videoId = current.videoId
        val position = videoPlayer.currentPosition
        val hasContent = videoPlayer.currentMediaItem != null
        if (videoId != null && hasContent) {
            scope.launch {
                viewingSession.saveProgress(videoId, position)
                if (toWatchlist) viewingSession.addToWatchlist(videoId, current.title)
            }
        }
        videoPlayer.stop()
        videoPlayer.clearMediaItems()
        _video.value = VideoPlayback()
    }

    private fun syncVideoState() {
        val item = videoPlayer.currentMediaItem ?: if (videoPlayer.playbackState == Player.STATE_IDLE) {
            _video.value = VideoPlayback()
            return
        } else null
        if (item == null) return
        _video.value = VideoPlayback(
            videoId = item.mediaId,
            title = item.mediaMetadata.title?.toString().orEmpty(),
            // playWhenReady = user intent; true while buffering too, so status text is stable.
            playing = videoPlayer.playWhenReady && videoPlayer.playbackState != Player.STATE_IDLE &&
                videoPlayer.playbackState != Player.STATE_ENDED,
            positionMs = videoPlayer.currentPosition.coerceAtLeast(0),
            totalMs = durationOf(videoPlayer),
            muted = videoPlayer.volume == 0f
        )
    }

    private fun durationOf(player: Player): Long =
        player.duration.takeIf { it != C.TIME_UNSET }?.coerceAtLeast(0) ?: 0

    fun release() {
        scope.cancel()
        musicPlayer.release()
        videoPlayer.release()
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
}
