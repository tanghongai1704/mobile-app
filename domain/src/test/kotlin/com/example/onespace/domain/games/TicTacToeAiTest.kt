package com.example.onespace.domain.games

import com.example.onespace.domain.games.engine.Difficulty
import com.example.onespace.domain.games.engine.Mark
import com.example.onespace.domain.games.engine.TicTacToeAi
import com.example.onespace.domain.games.engine.TicTacToeBoard
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TicTacToeAiTest {
    private fun boardOf(s: String) = TicTacToeBoard(s.map { when (it) { 'X' -> Mark.X; 'O' -> Mark.O; else -> null } })

    @Test fun detectsWinnerAndDraw() {
        assertEquals(Mark.X, boardOf("XXX......").winner)
        assertEquals(listOf(0, 4, 8), boardOf("X...X...X").winningLine)
        assertTrue(boardOf("XOXXOOOXX").isDraw)
        assertNull(boardOf("XO.......").winner)
    }

    @Test fun mediumTakesAWinAndBlocks() {
        assertEquals(2, TicTacToeAi.chooseMove(boardOf("OO.XX...."), Mark.O, Difficulty.MEDIUM, Random(1)))
        assertEquals(5, TicTacToeAi.chooseMove(boardOf("XX.OO...."), Mark.O, Difficulty.MEDIUM, Random(1)))
        assertEquals(2, TicTacToeAi.chooseMove(boardOf("XX.O....."), Mark.O, Difficulty.MEDIUM, Random(1)))
    }

    @Test fun hardNeverLosesAgainstAnyRandomOpponent() {
        repeat(300) { seed ->
            val rnd = Random(seed)
            var board = TicTacToeBoard()
            var turn = Mark.X
            while (!board.isFinished) {
                val move = if (turn == Mark.O) TicTacToeAi.chooseMove(board, Mark.O, Difficulty.HARD, rnd)
                else board.emptyCells.random(rnd)
                board = board.place(move, turn)
                turn = turn.other
            }
            assertNotEquals("seed $seed", Mark.X, board.winner)
        }
    }

    @Test fun hardPlayingFirstAlsoNeverLoses() {
        repeat(200) { seed ->
            val rnd = Random(seed)
            var board = TicTacToeBoard()
            var turn = Mark.O
            while (!board.isFinished) {
                val move = if (turn == Mark.O) TicTacToeAi.chooseMove(board, Mark.O, Difficulty.HARD, rnd)
                else board.emptyCells.random(rnd)
                board = board.place(move, turn)
                turn = turn.other
            }
            assertNotEquals("seed $seed", Mark.X, board.winner)
        }
    }
}
