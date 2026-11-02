package com.example.onespace.domain.games.engine

import kotlin.random.Random

enum class Mark { X, O;
    val other: Mark get() = if (this == X) O else X
}

enum class Difficulty(val label: String, val reward: Int) {
    EASY("Dễ", 10),
    MEDIUM("Vừa", 20),
    HARD("Khó", 40)
}

/** 3x3 board, row-major; null means empty. */
data class TicTacToeBoard(val cells: List<Mark?> = List(9) { null }) {
    val winner: Mark? get() = winningLine?.let { cells[it.first()] }
    val winningLine: List<Int>? get() = LINES.firstOrNull { l ->
        cells[l[0]] != null && cells[l[0]] == cells[l[1]] && cells[l[0]] == cells[l[2]]
    }
    val isFull: Boolean get() = cells.all { it != null }
    val isDraw: Boolean get() = winner == null && isFull
    val isFinished: Boolean get() = winner != null || isFull
    val emptyCells: List<Int> get() = cells.indices.filter { cells[it] == null }

    fun place(index: Int, mark: Mark): TicTacToeBoard {
        require(cells[index] == null) { "Cell $index is taken" }
        return TicTacToeBoard(cells.toMutableList().also { it[index] = mark })
    }

    companion object {
        val LINES = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
    }
}

/** Computer opponent. HARD is minimax and cannot lose; EASY plays randomly. */
object TicTacToeAi {
    fun chooseMove(
        board: TicTacToeBoard,
        me: Mark,
        difficulty: Difficulty,
        random: Random = Random.Default
    ): Int {
        val free = board.emptyCells
        require(free.isNotEmpty()) { "No moves left" }
        return when (difficulty) {
            Difficulty.EASY -> free[random.nextInt(free.size)]
            Difficulty.MEDIUM -> winningMove(board, me)
                ?: winningMove(board, me.other)
                ?: free[random.nextInt(free.size)]
            Difficulty.HARD -> bestMove(board, me, random)
        }
    }

    private fun winningMove(board: TicTacToeBoard, mark: Mark): Int? =
        board.emptyCells.firstOrNull { board.place(it, mark).winner == mark }

    private fun bestMove(board: TicTacToeBoard, me: Mark, random: Random): Int {
        val scored = board.emptyCells.map { it to minimax(board.place(it, me), me, me.other, 1) }
        val top = scored.maxOf { it.second }
        val best = scored.filter { it.second == top }.map { it.first }
        return best[random.nextInt(best.size)]
    }

    /** Positive when [me] wins; shorter wins and longer losses score better. */
    private fun minimax(board: TicTacToeBoard, me: Mark, turn: Mark, depth: Int): Int {
        board.winner?.let { return if (it == me) 10 - depth else depth - 10 }
        if (board.isFull) return 0
        val results = board.emptyCells.map { minimax(board.place(it, turn), me, turn.other, depth + 1) }
        return if (turn == me) results.max() else results.min()
    }
}
