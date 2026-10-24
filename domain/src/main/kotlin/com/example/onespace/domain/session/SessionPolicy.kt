package com.example.onespace.domain.session

/** Device-wide C1 choice; resettable in Settings. */
enum class MediaConflictRule { MUTE_VIDEO, PAUSE_VIDEO, KEEP_VIDEO }
enum class VideoAudioEffect { NONE, MUTE, PAUSE }
data class VideoStartPlan(val pauseMusic: Boolean)

sealed interface MusicStartPlan {
    data class Start(val videoEffect: VideoAudioEffect) : MusicStartPlan
    data object DoNotStart : MusicStartPlan
    data object AskUser : MusicStartPlan
}

/** Product decisions only. Playback state always comes from MediaSessionPort. */
object SessionPolicy {
    fun enter(state: SessionState, destination: Section) = state.copy(
        gamePaused = state.gamePaused || destination != Section.GAMES
    )
    fun background(state: SessionState) = state.copy(gamePaused = true)

    fun resolveVideoStart(muted: Boolean) = VideoStartPlan(pauseMusic = !muted)

    /** An explicit cancellation must remain a cancellation even if the video stopped meanwhile. */
    fun resolveMusicChoice(videoAudible: Boolean, choice: MediaConflictRule): MusicStartPlan =
        if (choice == MediaConflictRule.KEEP_VIDEO) MusicStartPlan.DoNotStart
        else resolveMusicStart(videoAudible, choice)

    fun resolveMusicStart(videoAudible: Boolean, rememberedRule: MediaConflictRule?): MusicStartPlan = when {
        !videoAudible -> MusicStartPlan.Start(VideoAudioEffect.NONE)
        rememberedRule == MediaConflictRule.MUTE_VIDEO -> MusicStartPlan.Start(VideoAudioEffect.MUTE)
        rememberedRule == MediaConflictRule.PAUSE_VIDEO -> MusicStartPlan.Start(VideoAudioEffect.PAUSE)
        rememberedRule == MediaConflictRule.KEEP_VIDEO -> MusicStartPlan.DoNotStart
        else -> MusicStartPlan.AskUser
    }
}
