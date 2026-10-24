package com.example.onespace.domain.session

import com.example.onespace.domain.session.port.PlaybackStatus
import com.example.onespace.domain.session.port.VideoPlayback
import org.junit.Assert.*
import org.junit.Test

class SessionPolicyTest {
    @Test fun choosingAnyCatalogPreservesMediaIdentity() {
        val state = SessionState(videoId = "v1", trackId = "m1")
        Section.entries.forEach { assertEquals(state, SessionPolicy.enter(state, it)) }
    }
    @Test fun leavingGamePausesItWithoutStartingAnotherGame() {
        val state = SessionState(gamePaused = false)
        assertTrue(SessionPolicy.enter(state, Section.NEWS).gamePaused)
        assertTrue(SessionPolicy.enter(SessionState(), Section.GAMES).gamePaused)
        assertTrue(SessionPolicy.background(state).gamePaused)
    }
    @Test fun audibleVideoMustPauseMusicButMutedVideoCanCoexist() {
        assertTrue(SessionPolicy.resolveVideoStart(muted = false).pauseMusic)
        assertFalse(SessionPolicy.resolveVideoStart(muted = true).pauseMusic)
    }
    @Test fun firstAudibleCollisionRequiresAChoice() {
        assertEquals(MusicStartPlan.AskUser, SessionPolicy.resolveMusicStart(true, null))
    }
    @Test fun rememberedRulesProduceExplicitEffects() {
        assertEquals(MusicStartPlan.Start(VideoAudioEffect.MUTE), SessionPolicy.resolveMusicStart(true, MediaConflictRule.MUTE_VIDEO))
        assertEquals(MusicStartPlan.Start(VideoAudioEffect.PAUSE), SessionPolicy.resolveMusicStart(true, MediaConflictRule.PAUSE_VIDEO))
        assertEquals(MusicStartPlan.DoNotStart, SessionPolicy.resolveMusicStart(true, MediaConflictRule.KEEP_VIDEO))
    }
    @Test fun stoppedOrMutedVideoDoesNotBlockMusicWithARememberedKeepVideoRule() {
        assertEquals(MusicStartPlan.Start(VideoAudioEffect.NONE), SessionPolicy.resolveMusicStart(false, MediaConflictRule.KEEP_VIDEO))
        assertFalse(VideoPlayback(videoId = "v1", playing = true, muted = true).audible)
    }
    @Test fun explicitKeepVideoChoiceCancelsEvenWhenTheVideoHasEnded() {
        assertEquals(MusicStartPlan.DoNotStart, SessionPolicy.resolveMusicChoice(false, MediaConflictRule.KEEP_VIDEO))
        assertEquals(MusicStartPlan.Start(VideoAudioEffect.NONE), SessionPolicy.resolveMusicChoice(false, MediaConflictRule.MUTE_VIDEO))
    }
    @Test fun bufferingRetainsAudioIntentButEndedAndFailedMediaDoNot() {
        val buffering = VideoPlayback(videoId = "v1", playRequested = true, status = PlaybackStatus.BUFFERING)
        assertFalse(buffering.playing)
        assertTrue(buffering.audible)
        assertFalse(buffering.copy(status = PlaybackStatus.ENDED).audible)
        assertFalse(buffering.copy(status = PlaybackStatus.ERROR).audible)
    }
}
