package com.example.onespace.data.music.repository

import com.example.onespace.domain.music.model.Track
import com.example.onespace.domain.music.repository.MusicRepository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow

/**
 * Bundled demo catalog: 50 tracks over SoundHelix's freely distributable
 * sample songs (16 verified URLs, durations follow the real files), so the
 * audio pipeline runs for real. Replace this binding, not the UI, later.
 */
internal class MockMusicRepository : MusicRepository {
    override fun observeCatalog() = flow {
        delay(400)
        emit(catalog)
    }

    private fun sh(n: Int) = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-$n.mp3"

    private val catalog = listOf(
        Track("m1", "Nắng sớm", "Nhóm Lo-fi", "9:19", sh(1), true),
        Track("m2", "Nơi nào yên tĩnh", "Đài Chủ nhật", "10:38", sh(2)),
        Track("m3", "Suy nghĩ đêm muộn", "Trạm Chill", "8:36", sh(3), true),
        Track("m4", "Chuyện kể chậm rãi", "Đài Chủ nhật", "7:33", sh(4)),
        Track("m5", "Gió qua ban công", "Mưa Tháng Sáu", "8:50", sh(5)),
        Track("m6", "Tà áo trắng", "Mưa Tháng Sáu", "6:59", sh(6)),
        Track("m7", "Đêm thứ hai", "Trạm Chill", "10:30", sh(7)),
        Track("m8", "Cà phê đen không đường", "Nhóm Lo-fi", "8:07", sh(8), true),
        Track("m9", "Chuyến tàu cuối", "Đài Chủ nhật", "9:44", sh(9)),
        Track("m10", "Mùa gạo mới", "Mùa Gạo Mới", "13:11", sh(10)),
        Track("m11", "Bão nhỏ trong tim", "Sóng Cỏ", "13:46", sh(11)),
        Track("m12", "Lối về không tên", "Sóng Cỏ", "12:50", sh(12)),
        Track("m13", "Trời hôm nay xanh", "Gác Tum", "11:45", sh(13)),
        Track("m14", "Nhắn chưa trả lời", "Dạo Này", "12:54", sh(14)),
        Track("m15", "Yêu từ cái nhìn đầu", "Vệ Tinh", "10:57", sh(15)),
        Track("m16", "Con đường mỗi ngày", "Vệ Tinh", "12:05", sh(16)),
        Track("m17", "Bình minh sau mưa", "Mưa Tháng Sáu", "9:19", sh(1)),
        Track("m18", "Góc quen của hai đứa", "Dạo Này", "10:38", sh(2)),
        Track("m19", "Bàn chân trần trên cát", "Sóng Cỏ", "8:36", sh(3)),
        Track("m20", "Tiếng ve cuối hạ", "Mùa Gạo Mới", "7:33", sh(4), true),
        Track("m21", "Một tối không ngủ", "Gác Tum", "8:50", sh(5)),
        Track("m22", "Hẹn ở góc phố", "Nhóm Lo-fi", "6:59", sh(6)),
        Track("m23", "Bữa cơm nhà mẹ nấu", "Mùa Gạo Mới", "10:30", sh(7)),
        Track("m24", "Tháng sáu mưa dầm", "Mưa Tháng Sáu", "8:07", sh(8)),
        Track("m25", "Bài ca không lời", "Trạm Chill", "9:44", sh(9), true),
        Track("m26", "Đi qua tuổi mười tám", "Dạo Này", "13:11", sh(10)),
        Track("m27", "Lời hứa mùa thu", "Vệ Tinh", "13:46", sh(11)),
        Track("m28", "Trộm nhìn nụ cười", "Kẹo Dừa", "12:50", sh(12)),
        Track("m29", "Chỉ còn mình em", "Kẹo Dừa", "11:45", sh(13)),
        Track("m30", "Sài Gòn ơi có nhớ", "Đài Chủ nhật", "12:54", sh(14)),
        Track("m31", "Hà Nội mùa này", "Gác Tum", "10:57", sh(15), true),
        Track("m32", "Quê hương hai tiếng", "Mùa Gạo Mới", "12:05", sh(16)),
        Track("m33", "Yêu em mưa bụi", "Kẹo Dừa", "9:19", sh(1)),
        Track("m34", "Bến nước quen quen", "Sóng Cỏ", "10:38", sh(2)),
        Track("m35", "Nụ cười của em", "Vệ Tinh", "8:36", sh(3)),
        Track("m36", "Chiều nay nao lòng", "Dạo Này", "7:33", sh(4)),
        Track("m37", "Đêm trắng chờ mong", "Trạm Chill", "8:50", sh(5), true),
        Track("m38", "Ngày đầu gặp mẹ", "Mưa Tháng Sáu", "6:59", sh(6)),
        Track("m39", "Đôi mắt niềm vui", "Nhóm Lo-fi", "10:30", sh(7)),
        Track("m40", "Vũ điệu mây bay", "Mùa Gạo Mới", "8:07", sh(8)),
        Track("m41", "Chuyện cũ đừng nhắc", "Kẹo Dừa", "9:44", sh(9)),
        Track("m42", "Điều nhỏ dại khờ", "Dạo Này", "13:11", sh(10)),
        Track("m43", "Ngày nắng vẽ hình", "Gác Tum", "13:46", sh(11), true),
        Track("m44", "Khoảng lặng giữa hồ", "Sóng Cỏ", "12:50", sh(12)),
        Track("m45", "Con thuyền giấy nhỏ", "Trạm Chill", "11:45", sh(13)),
        Track("m46", "Lối nhỏ về nhà", "Đài Chủ nhật", "12:54", sh(14)),
        Track("m47", "Nghe mưa gõ cửa sổ", "Mưa Tháng Sáu", "10:57", sh(15)),
        Track("m48", "Trả lời cho thiếu niên", "Vệ Tinh", "12:05", sh(16)),
        Track("m49", "Bài ca cho người ấy", "Kẹo Dừa", "9:19", sh(1), true),
        Track("m50", "Mình hát cho nhau", "Mùa Gạo Mới", "10:38", sh(2))
    )
}
