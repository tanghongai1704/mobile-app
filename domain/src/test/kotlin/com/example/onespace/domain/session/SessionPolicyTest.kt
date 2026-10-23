package com.example.onespace.domain.session

import org.junit.Assert.*
import org.junit.Test

class SessionPolicyTest {
    @Test fun videoContinuesOverNewsAndMutedGamesButPausesForMusic() {
        val playing = SessionPolicy.playVideo(SessionState(), "v1", "Video")
        assertTrue(SessionPolicy.enter(playing, Section.NEWS).videoPlaying)
        assertTrue(SessionPolicy.enter(playing, Section.GAMES).videoPlaying)
        assertFalse(SessionPolicy.enter(playing, Section.MUSIC).videoPlaying)
    }

    @Test fun onlyOneAudibleMediaSourcePlaysAtATime() {
        val video = SessionPolicy.playVideo(SessionState(), "v1", "Video")
        val music = SessionPolicy.playMusic(video, "m1", "Track")
        assertTrue(music.musicPlaying)
        assertFalse(music.videoPlaying)
        assertTrue(SessionPolicy.enter(music, Section.GAMES).musicPlaying)
        val nextVideo = SessionPolicy.playVideo(music, "v2", "Next")
        assertFalse(nextVideo.musicPlaying)
        assertTrue(nextVideo.videoPlaying)
    }

    @Test fun backgroundPausesVideoAndGameWhileKeepingMusic() {
        val music = SessionPolicy.playMusic(SessionState(gamePaused = false), "m1", "Track")
        val background = SessionPolicy.background(music)
        assertTrue(background.musicPlaying)
        assertTrue(background.gamePaused)
        val video = SessionPolicy.playVideo(music, "v1", "Video")
        assertFalse(SessionPolicy.background(video).videoPlaying)
    }

    @Test fun emptySessionControlsNeverStartAnUnknownMediaItem() {
        assertFalse(SessionPolicy.toggleVideo(SessionState()).videoPlaying)
        assertFalse(SessionPolicy.toggleMusic(SessionState()).musicPlaying)
    }

    @Test fun leavingGamePausesItAndEnteringCatalogDoesNotStartGameLoop() {
        val leaving = SessionPolicy.enter(SessionState(gamePaused = false), Section.NEWS)
        assertTrue(leaving.gamePaused)
        assertTrue(SessionPolicy.enter(leaving, Section.GAMES).gamePaused)
    }

    @Test fun musicStartsFreelyWhenNoVideoIsPlaying() {
        val plan = SessionPolicy.resolveMusicStart(videoActive = false, rememberedRule = null)
        assertEquals(MusicStartPlan.Start(VideoAudioEffect.NONE), plan)
    }

    @Test fun firstCollisionOfVideoAndMusicAsksTheUserForARule() {
        assertEquals(MusicStartPlan.AskUser, SessionPolicy.resolveMusicStart(true, null))
    }

    @Test fun rememberedRulesAreAppliedSilently() {
        assertEquals(
            MusicStartPlan.Start(VideoAudioEffect.MUTE),
            SessionPolicy.resolveMusicStart(true, MediaConflictRule.MUTE_VIDEO))
        assertEquals(
            MusicStartPlan.Start(VideoAudioEffect.PAUSE),
            SessionPolicy.resolveMusicStart(true, MediaConflictRule.PAUSE_VIDEO))
        assertEquals(
            MusicStartPlan.DoNotStart,
            SessionPolicy.resolveMusicStart(true, MediaConflictRule.KEEP_VIDEO))
    }

    @Test fun rememberedRuleIsIgnoredWhenNoVideoIsActive() {
        assertEquals(
            MusicStartPlan.Start(VideoAudioEffect.NONE),
            SessionPolicy.resolveMusicStart(false, MediaConflictRule.KEEP_VIDEO))
    }
}
