package com.example.onespace.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable

/**
 * English wording for Vietnamese text that comes from outside the screens: domain models, data
 * seeds, ViewModel messages and the media core. Those layers stay language-neutral Kotlin and keep
 * Vietnamese as their source text; the UI shows [t] so the language switch applies to them too.
 * Catalog content (video, song and article titles) is data and is intentionally not translated.
 */
object Translations {
    private val exact = mapOf(
        // Topics and categories (matching logic keeps the Vietnamese keys)
        "Thiên nhiên" to "Nature", "Đời sống" to "Lifestyle", "Nhẹ nhàng" to "Chill", "Du lịch" to "Travel",
        "Ẩm thực" to "Food", "Thể thao" to "Sports", "Việt" to "Vietnamese", "Công nghệ" to "Tech", "Giải trí" to "Entertainment",
        // Games and difficulty
        "Dễ" to "Easy", "Vừa" to "Medium", "Khó" to "Hard",
        "Vuốt để gộp ô, chạm mốc 2048." to "Swipe to merge tiles and reach 2048.",
        "Đấu với máy ở 3 mức độ hoặc chơi cùng bạn." to "Play the computer on 3 levels or a friend.",
        "Đoán bài hát" to "Guess the Song", "Nghe 5 giây, chọn tên bài đúng." to "Listen for 5 seconds, pick the right title.",
        "Quiz kiến thức" to "Knowledge Quiz", "Câu hỏi kiến thức tổng quát từ dễ đến khó, kiểu Ai là triệu phú." to "General-knowledge questions from easy to hard, Millionaire style.",
        "Rắn săn mồi" to "Snake", "Vuốt để điều khiển, ăn thật nhiều điểm." to "Swipe to steer and score big.",
        "Nốt nhạc rơi theo bài đang phát, chạm đúng nhịp." to "Notes fall with the playing song; tap on the beat.",
        "Không tải được danh sách game." to "Couldn't load the games.",
        // Daily challenges
        "Đạt 300 điểm trong một ván 2048" to "Score 300 in one 2048 game", "Thắng máy 2 ván Tic-Tac-Toe" to "Beat the computer twice at Tic-Tac-Toe",
        "Đoán đúng 3 bài hát" to "Guess 3 songs correctly", "Trả lời đúng 3 câu hỏi kiến thức" to "Answer 3 knowledge questions correctly",
        "Ăn 6 mồi trong một ván Rắn" to "Eat 6 treats in one Snake game", "Đạt 10 điểm trong một ván Beat Tiles" to "Score 10 in one Beat Tiles game",
        // Game badges
        "Rắn tham ăn" to "Hungry snake", "Ăn 15 mồi trong một ván Rắn" to "Eat 15 treats in one Snake game",
        "Gộp tới 512" to "Merge to 512", "Tạo được ô 512 trong 2048" to "Make a 512 tile in 2048",
        "Chạm 2048" to "Reach 2048", "Tạo được ô 2048" to "Make a 2048 tile",
        "Bậc thầy câu đố" to "Quiz master", "Đúng 10 câu liên tiếp ở quiz cuối video" to "10 correct in a row in the end-of-video quiz",
        "Khắc tinh của máy" to "Computer's nemesis", "Thắng máy 3 ván liên tiếp Tic-Tac-Toe" to "Beat the computer 3 times in a row at Tic-Tac-Toe",
        "Nhịp chuẩn" to "On the beat", "Đạt 30 điểm trong một ván Beat Tiles" to "Score 30 in one Beat Tiles game",
        "Tai thính" to "Good ear", "Đạt 50 điểm Đoán bài hát" to "Score 50 in Guess the Song",
        "Chăm chỉ" to "Dedicated", "Hoàn thành 3 thử thách hằng ngày" to "Complete 3 daily challenges",
        "Thử đủ 5 game" to "Try all 5 games", "Chơi 2048, Tic-Tac-Toe, Đoán bài hát, Rắn và Beat Tiles" to "Play 2048, Tic-Tac-Toe, Guess the Song, Snake and Beat Tiles",
        // Pet
        "Trứng" to "Egg", "Bé cưng" to "Baby", "Thiếu niên" to "Teen", "Trưởng thành" to "Grown-up",
        // Shop
        "Mở khoá game rắn săn mồi" to "Unlock the Snake game", "Mở khoá game nốt nhạc rơi" to "Unlock the falling-notes game",
        "Mũ thú cưng" to "Pet hat", "Phụ kiện trang trí thú cưng" to "Pet decoration", "Khăn thú cưng" to "Pet scarf", "Phụ kiện giữ ấm" to "A warm accessory",
        "Khung Đồng" to "Bronze frame", "Khung avatar mốc 7 ngày streak" to "Avatar frame for a 7-day streak",
        "Khung Bạc" to "Silver frame", "Khung avatar mốc 30 ngày streak" to "Avatar frame for a 30-day streak",
        "Khung Vàng" to "Gold frame", "Khung avatar mốc 1000 điểm" to "Avatar frame for 1000 points",
        "Cơ bản" to "Basic", "Đồng" to "Bronze", "Bạc" to "Silver", "Vàng" to "Gold",
        // Missions
        "Nghe 1 bài nhạc" to "Listen to 1 song", "Đọc 1 bài tin" to "Read 1 article", "Thắng 1 ván game" to "Win 1 game",
        // Quiz kinds
        "Ai đăng video này?" to "Who posted this video?", "Video dài bao lâu?" to "How long is this video?",
        "Cùng chủ đề" to "Same topic", "Đúng hay sai?" to "True or false?", "Đúng" to "True", "Sai" to "False",
        // Feedback and errors from ViewModels / core
        "Đã cho thú ăn (−30 điểm)" to "Fed your pet (−30 points)", "Đã cho thú ăn bằng thức ăn thưởng (miễn phí)" to "Fed your pet with a challenge treat (free)",
        "Không đủ 30 điểm để cho ăn" to "You need 30 points to feed your pet", "Hôm nay đã cho ăn rồi" to "Already fed today",
        "Số điện thoại không hợp lệ" to "Invalid phone number", "Mật khẩu cần ít nhất 6 ký tự" to "Password needs at least 6 characters",
        "Email chưa hợp lệ" to "Invalid email", "Email chưa hợp lệ — kiểm tra lại giúp mình nhé." to "That email doesn't look right. Please check it.",
        "Mã xác thực chưa đúng. Nhập 6 chữ số, mã demo là 123456." to "Wrong code. Enter 6 digits; the demo code is 123456.",
        "Mã đã hết hạn. Vui lòng gửi lại mã." to "The code expired. Please request a new one.",
        "Bạn đã thử quá nhiều lần. Vui lòng gửi lại mã." to "Too many attempts. Please request a new code.",
        "Mật khẩu cần ít nhất 6 ký tự." to "Password needs at least 6 characters.", "Chưa thể đăng nhập. Vui lòng thử lại." to "Couldn't sign in. Please try again.",
        "Không tải được kho nhạc." to "Couldn't load the music library.", "Không tải được tin." to "Couldn't load the news.",
        "Không tải được danh sách video." to "Couldn't load the videos.", "Thư viện" to "Library",
        "Không đọc được bài này." to "Couldn't read this article aloud.", "Thiết bị không có bộ đọc tiếng Việt." to "This device has no Vietnamese text-to-speech voice.",
        "Không phát được nhạc. Kiểm tra kết nối rồi nhấn Phát để thử lại." to "Couldn't play the music. Check your connection, then tap Play to retry.",
        "Không phát được video. Kiểm tra kết nối rồi nhấn Phát để thử lại." to "Couldn't play the video. Check your connection, then tap Play to retry.",
        "Phát tất cả" to "Play all", "Ngẫu nhiên" to "Shuffle", "Từ video của bạn" to "From your videos", "Dành cho bạn" to "For you", "Nhạc của tôi" to "My music"
    )

    /** Messages built as "<prefix><name>": the prefix is translated and the (translated) name appended. */
    private val prefixes = listOf(
        "Không đủ điểm để đổi " to "Not enough points to redeem ",
        "Bạn đã có " to "You already own ",
        "Đã đổi " to "Redeemed "
    )

    fun english(vi: String): String {
        exact[vi]?.let { return it }
        for ((p, e) in prefixes) if (vi.startsWith(p)) return e + english(vi.removePrefix(p))
        return vi
    }
}

/** Shows [this] in the current language when an English wording is known; otherwise unchanged. */
@Composable
@ReadOnlyComposable
fun String.t(): String = if (LocalEnglish.current) Translations.english(this) else this
