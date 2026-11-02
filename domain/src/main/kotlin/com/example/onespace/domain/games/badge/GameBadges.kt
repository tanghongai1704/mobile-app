package com.example.onespace.domain.games.badge

/** Record keys the games write into the records store; badges read them back. */
object RecordKeys {
    const val GAME_2048 = "g1"
    const val GAME_2048_TILE = "g1_tile"
    const val TIC_TAC_TOE_STREAK = "g2"
    const val GUESS_SONG = "g3"
    const val SNAKE = "g5"
    const val BEAT_TILES = "g6"
    const val VIDEO_QUIZ_COMBO = "video_quiz_combo"
    const val CHALLENGES_DONE = "challenges_done"
}

/** A goal with a progress number derived from the stored records. */
class GameBadge(
    val id: String,
    val name: String,
    val description: String,
    val goal: Int,
    private val progressOf: (Map<String, Int>) -> Int
) {
    fun progress(records: Map<String, Int>): Int = progressOf(records)
}

data class BadgeStatus(val badge: GameBadge, val progress: Int) {
    val earned: Boolean get() = progress >= badge.goal
    val fraction: Float get() = if (badge.goal <= 0) 1f else (progress.toFloat() / badge.goal).coerceIn(0f, 1f)
}

object GameBadges {
    private fun best(key: String): (Map<String, Int>) -> Int = { it[key] ?: 0 }

    val all = listOf(
        GameBadge("snake_15", "Rắn tham ăn", "Ăn 15 mồi trong một ván Rắn", 15, best(RecordKeys.SNAKE)),
        GameBadge("tile_512", "Gộp tới 512", "Tạo được ô 512 trong 2048", 512, best(RecordKeys.GAME_2048_TILE)),
        GameBadge("tile_2048", "Chạm 2048", "Tạo được ô 2048", 2048, best(RecordKeys.GAME_2048_TILE)),
        GameBadge("quiz_combo_10", "Bậc thầy câu đố", "Đúng 10 câu liên tiếp ở quiz cuối video", 10, best(RecordKeys.VIDEO_QUIZ_COMBO)),
        GameBadge("ttt_streak_3", "Khắc tinh của máy", "Thắng máy 3 ván liên tiếp Tic-Tac-Toe", 3, best(RecordKeys.TIC_TAC_TOE_STREAK)),
        GameBadge("beat_30", "Nhịp chuẩn", "Đạt 30 điểm trong một ván Beat Tiles", 30, best(RecordKeys.BEAT_TILES)),
        GameBadge("guess_50", "Tai thính", "Đạt 50 điểm Đoán bài hát", 50, best(RecordKeys.GUESS_SONG)),
        GameBadge("challenge_3", "Chăm chỉ", "Hoàn thành 3 thử thách hằng ngày", 3, best(RecordKeys.CHALLENGES_DONE)),
        GameBadge(
            "all_rounder", "Thử đủ 5 game", "Chơi 2048, Tic-Tac-Toe, Đoán bài hát, Rắn và Beat Tiles", 5
        ) { records ->
            listOf(
                RecordKeys.GAME_2048_TILE, RecordKeys.TIC_TAC_TOE_STREAK, RecordKeys.GUESS_SONG,
                RecordKeys.SNAKE, RecordKeys.BEAT_TILES
            ).count { (records[it] ?: 0) > 0 }
        }
    )

    fun evaluate(records: Map<String, Int>): List<BadgeStatus> =
        all.map { BadgeStatus(it, it.progress(records)) }

    /** Badges that are earned in [after] but were not in [before]. */
    fun newlyEarned(before: Map<String, Int>, after: Map<String, Int>): List<GameBadge> {
        val was = evaluate(before).filter { it.earned }.map { it.badge.id }.toSet()
        return evaluate(after).filter { it.earned && it.badge.id !in was }.map { it.badge }
    }
}
