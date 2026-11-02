package com.example.onespace.domain.games

import com.example.onespace.domain.games.badge.GameBadges
import com.example.onespace.domain.games.badge.RecordKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameBadgesTest {
    private fun status(records: Map<String, Int>, id: String) = GameBadges.evaluate(records).first { it.badge.id == id }

    @Test fun nothingEarnedWithoutRecords() {
        assertTrue(GameBadges.evaluate(emptyMap()).none { it.earned })
    }

    @Test fun idsAreUnique() {
        assertEquals(GameBadges.all.size, GameBadges.all.map { it.id }.toSet().size)
    }

    @Test fun thresholdsAreInclusiveAndProgressIsKept() {
        val s = status(mapOf(RecordKeys.SNAKE to 14), "snake_15")
        assertFalse(s.earned)
        assertEquals(14, s.progress)
        assertTrue(status(mapOf(RecordKeys.SNAKE to 15), "snake_15").earned)
        assertEquals(1f, status(mapOf(RecordKeys.SNAKE to 99), "snake_15").fraction, 0f)
    }

    @Test fun reaching2048EarnsBothTileBadges() {
        val records = mapOf(RecordKeys.GAME_2048_TILE to 2048)
        assertTrue(status(records, "tile_512").earned)
        assertTrue(status(records, "tile_2048").earned)
        assertFalse(status(mapOf(RecordKeys.GAME_2048_TILE to 1024), "tile_2048").earned)
    }

    @Test fun allRounderCountsDistinctGamesPlayed() {
        val some = mapOf(RecordKeys.GAME_2048_TILE to 64, RecordKeys.SNAKE to 3, RecordKeys.BEAT_TILES to 0)
        assertEquals(2, status(some, "all_rounder").progress)
        val all = mapOf(
            RecordKeys.GAME_2048_TILE to 8, RecordKeys.TIC_TAC_TOE_STREAK to 1, RecordKeys.GUESS_SONG to 10,
            RecordKeys.SNAKE to 1, RecordKeys.BEAT_TILES to 1
        )
        assertTrue(status(all, "all_rounder").earned)
    }

    @Test fun newlyEarnedOnlyListsFreshUnlocks() {
        val before = mapOf(RecordKeys.SNAKE to 10)
        val after = mapOf(RecordKeys.SNAKE to 16, RecordKeys.BEAT_TILES to 5)
        assertEquals(listOf("snake_15"), GameBadges.newlyEarned(before, after).map { it.id })
        assertTrue(GameBadges.newlyEarned(after, after).isEmpty())
    }
}
