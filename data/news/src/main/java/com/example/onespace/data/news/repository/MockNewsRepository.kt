package com.example.onespace.data.news.repository

import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.news.repository.NewsRepository

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Bundled demo data. Replace this binding, not the UI, when integrating real sources. */
internal class MockNewsRepository : NewsRepository {
    override fun observeCatalog() = flow {
        delay(400)
        emit(listOf(
        Article("n1", "Thành phố xanh bắt đầu từ ý nhỏ", "Đời sống", "4 phút đọc", "Khắp thành phố, những khu vườn cộng đồng nhỏ đang gắn kết mọi người lại với nhau.\n\nĐây là bài mẫu minh hoạ bố cục trình đọc, tính năng lưu bài và điều hướng. Nội dung biên tập thật và lưu ngoại tuyến sẽ được nối ở giai đoạn sau.\n\nMột không gian chung có thể bắt đầu từ vài cây xanh, một chút kiên nhẫn và một lần trò chuyện với hàng xóm."),
        Article("n2", "Công nghệ nhường chỗ cho cuộc sống", "Công nghệ", "6 phút đọc", "Công nghệ chu đáo nên làm các việc hằng ngày đơn giản hơn.\n\nĐây là nội dung mẫu cho trình đọc tin của OneSpace. Vị trí cuộn được giữ nguyên bởi đích điều hướng khi bạn chuyển tab."),
        Article("n3", "Chạy trốn cuối tuần kế tiếp", "Du lịch", "3 phút đọc", "Một chuyến đi ngắn cũng có thể mang đến góc nhìn mới.\n\nKhám phá những cung đường yên tĩnh, quán cà phê địa phương và những nơi mới gần nhà. Bài này là nội dung mẫu kèm ứng dụng.")
        ))
    }
    private val savedIds = MutableStateFlow<Set<String>>(emptySet())
    override fun observeSavedIds() = savedIds.asStateFlow()
    override suspend fun toggleBookmark(id: String) {
        savedIds.update { if (id in it) it - id else it + id }
    }
}
