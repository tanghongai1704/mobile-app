package com.example.onespace.domain.games

import com.example.onespace.domain.games.engine.Direction
import com.example.onespace.domain.games.engine.Game2048
import com.example.onespace.domain.games.engine.Game2048State
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class Game2048Test {
    private fun board(vararg rows: List<Int>) = Game2048State(rows.flatMap { it })

    @Test fun newGameStartsWithTwoTiles() {
        val s = Game2048.newGame(Random(1))
        assertEquals(2, s.tiles.count { it != 0 })
        assertTrue(s.tiles.all { it == 0 || it == 2 || it == 4 })
    }

    @Test fun slideMergesEachPairOnce() {
        assertEquals(listOf(4, 4, 0, 0) to 8, Game2048.slide(listOf(2, 2, 2, 2)))
        assertEquals(listOf(4, 2, 0, 0) to 4, Game2048.slide(listOf(2, 2, 2, 0)))
        assertEquals(listOf(4, 4, 0, 0) to 4, Game2048.slide(listOf(0, 2, 2, 4)))
    }

    @Test fun moveLeftAddsScoreAndSpawnsOneTile() {
        val start = board(listOf(2, 2, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        val next = Game2048.move(start, Direction.LEFT, Random(3))
        assertEquals(4, next.score)
        assertEquals(4, next.tiles[0])
        assertEquals(2, next.tiles.count { it != 0 }) // merged tile + one spawn
        assertTrue(0 in next.mergedAt)
    }

    @Test fun moveThatChangesNothingReturnsSameState() {
        val start = board(listOf(2, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        assertSame(start, Game2048.move(start, Direction.LEFT, Random(1)))
    }

    @Test fun allFourDirectionsSlideToTheirEdge() {
        val start = board(listOf(0, 0, 0, 0), listOf(0, 2, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        // The single tile sits at index 5; it must land on the matching edge of row/column 1.
        assertEquals(2, Game2048.move(start, Direction.LEFT, Random(5)).tiles[4])
        assertEquals(2, Game2048.move(start, Direction.RIGHT, Random(5)).tiles[7])
        assertEquals(2, Game2048.move(start, Direction.UP, Random(5)).tiles[1])
        assertEquals(2, Game2048.move(start, Direction.DOWN, Random(5)).tiles[13])
    }

    @Test fun detectsGameOver() {
        val full = board(listOf(2, 4, 2, 4), listOf(4, 2, 4, 2), listOf(2, 4, 2, 4), listOf(4, 2, 4, 2))
        assertTrue(full.isOver)
        val movable = board(listOf(2, 2, 2, 4), listOf(4, 2, 4, 2), listOf(2, 4, 2, 4), listOf(4, 2, 4, 2))
        assertFalse(movable.isOver)
    }

    @Test fun reachingTargetIsRemembered() {
        val start = board(listOf(1024, 1024, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0), listOf(0, 0, 0, 0))
        val next = Game2048.move(start, Direction.LEFT, Random(2))
        assertTrue(next.reachedTarget)
        assertEquals(2048, next.bestTile)
    }
}
