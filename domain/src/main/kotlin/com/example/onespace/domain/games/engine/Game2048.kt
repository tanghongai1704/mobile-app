package com.example.onespace.domain.games.engine

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * Immutable 2048 position. [tiles] is row-major (size*size), 0 means empty.
 * [spawnedAt] is the index of the tile added by the last move, for the UI to highlight.
 */
data class Game2048State(
    val tiles: List<Int>,
    val score: Int = 0,
    val size: Int = Game2048.SIZE,
    val spawnedAt: Int? = null,
    val mergedAt: Set<Int> = emptySet(),
    val reachedTarget: Boolean = false
) {
    val bestTile: Int get() = tiles.maxOrNull() ?: 0
    val isOver: Boolean get() = Game2048.availableMoves(this).isEmpty()
}

/** Pure-Kotlin 2048 rules. Randomness is injected so tests stay deterministic. */
object Game2048 {
    const val SIZE = 4
    const val TARGET = 2048

    fun newGame(random: Random = Random.Default, size: Int = SIZE): Game2048State {
        var state = Game2048State(List(size * size) { 0 }, size = size)
        repeat(2) { state = spawn(state, random) }
        return state.copy(spawnedAt = null)
    }

    /** Slides and merges one row/column toward index 0. Returns the new line and score gained. */
    internal fun slide(line: List<Int>): Pair<List<Int>, Int> {
        val packed = line.filter { it != 0 }
        val out = mutableListOf<Int>()
        var gained = 0
        var i = 0
        while (i < packed.size) {
            if (i + 1 < packed.size && packed[i] == packed[i + 1]) {
                val merged = packed[i] * 2
                out += merged
                gained += merged
                i += 2
            } else {
                out += packed[i]
                i += 1
            }
        }
        while (out.size < line.size) out += 0
        return out to gained
    }

    /** Positions (in the slid line) that were produced by a merge. */
    private fun mergedPositions(line: List<Int>): Set<Int> {
        val packed = line.filter { it != 0 }
        val out = mutableSetOf<Int>()
        var i = 0
        var pos = 0
        while (i < packed.size) {
            if (i + 1 < packed.size && packed[i] == packed[i + 1]) { out += pos; i += 2 } else i += 1
            pos += 1
        }
        return out
    }

    /** Applies a move. Returns [state] unchanged when nothing slides, so callers can ignore it. */
    fun move(state: Game2048State, direction: Direction, random: Random = Random.Default): Game2048State {
        val n = state.size
        val tiles = state.tiles.toMutableList()
        var gained = 0
        val merged = mutableSetOf<Int>()
        for (k in 0 until n) {
            val indices = (0 until n).map { j ->
                when (direction) {
                    Direction.LEFT -> k * n + j
                    Direction.RIGHT -> k * n + (n - 1 - j)
                    Direction.UP -> j * n + k
                    Direction.DOWN -> (n - 1 - j) * n + k
                }
            }
            val before = indices.map { tiles[it] }
            val (after, score) = slide(before)
            gained += score
            val mergedPositions = mergedPositions(before)
            indices.forEachIndexed { j, idx ->
                tiles[idx] = after[j]
                if (j in mergedPositions) merged += idx
            }
        }
        if (tiles == state.tiles) return state
        val moved = state.copy(
            tiles = tiles,
            score = state.score + gained,
            mergedAt = merged,
            reachedTarget = state.reachedTarget || tiles.any { it >= TARGET }
        )
        return spawn(moved, random)
    }

    fun availableMoves(state: Game2048State): List<Direction> {
        if (state.tiles.any { it == 0 }) return Direction.entries.toList()
        val n = state.size
        return Direction.entries.filter { dir ->
            (0 until n).any { k ->
                (0 until n - 1).any { j ->
                    val a: Int
                    val b: Int
                    if (dir == Direction.LEFT || dir == Direction.RIGHT) {
                        a = state.tiles[k * n + j]; b = state.tiles[k * n + j + 1]
                    } else {
                        a = state.tiles[j * n + k]; b = state.tiles[(j + 1) * n + k]
                    }
                    a == b
                }
            }
        }
    }

    private fun spawn(state: Game2048State, random: Random): Game2048State {
        val empty = state.tiles.indices.filter { state.tiles[it] == 0 }
        if (empty.isEmpty()) return state
        val at = empty[random.nextInt(empty.size)]
        val value = if (random.nextInt(10) == 0) 4 else 2
        return state.copy(
            tiles = state.tiles.toMutableList().also { it[at] = value },
            spawnedAt = at
        )
    }
}
