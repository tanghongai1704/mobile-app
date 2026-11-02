package com.example.onespace.data.games.repository

import com.example.onespace.domain.games.model.Game
import com.example.onespace.domain.games.repository.GamesRepository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/** Bundled demo data. Replace this binding, not the UI, when integrating real sources. */
internal class MockGamesRepository : GamesRepository {
    override fun observeCatalog() = flow {
        delay(400)
        emit(listOf(
        Game("g1", "2048", "Vuốt để gộp ô, chạm mốc 2048.", 0),
        Game("g2", "Tic-Tac-Toe", "Đấu với máy ở 3 mức độ hoặc chơi cùng bạn.", 0),
        Game("g3", "Đoán bài hát", "Nghe 5 giây, chọn tên bài đúng.", 0),
        Game("g4", "Quiz tin tức", "Trả lời câu hỏi từ các bài báo đã đọc.", 0),
        Game("g5", "Rắn săn mồi", "Vuốt để điều khiển, ăn thật nhiều điểm.", 0),
        Game("g6", "Beat Tiles", "Nốt nhạc rơi theo bài đang phát, chạm đúng nhịp.", 0)
        ))
    }
}
