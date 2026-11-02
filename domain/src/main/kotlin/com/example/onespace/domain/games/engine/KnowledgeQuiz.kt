package com.example.onespace.domain.games.engine

import kotlin.random.Random

/** One general-knowledge question; [correctIndex] points into [options]. */
data class KnowledgeQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    /** 1 = easy, 2 = medium, 3 = hard; a run climbs from easy to hard. */
    val level: Int
) {
    init {
        require(options.size == 4) { "Four options expected" }
        require(correctIndex in options.indices) { "Correct answer must be one of the options" }
    }
}

/**
 * "Who wants to be a millionaire"-style run: [LENGTH] random questions climbing from easy to
 * hard, each worth more than the last. One wrong answer ends the run and keeps the points of the
 * last checkpoint; walking away keeps everything earned so far.
 */
class KnowledgeQuizRun(
    val questions: List<KnowledgeQuestion>,
    private val rewards: List<Int> = REWARDS
) {
    var index: Int = 0
        private set
    var earned: Int = 0
        private set
    var over: Boolean = false
        private set
    val won: Boolean get() = over && index >= questions.size

    val current: KnowledgeQuestion? get() = if (over) null else questions.getOrNull(index)

    /** Points the current question is worth. */
    val worth: Int get() = rewards.getOrElse(index) { rewards.last() }

    /** Answers the current question; true when right. A wrong answer ends the run. */
    fun answer(option: Int): Boolean {
        val q = current ?: return false
        return if (option == q.correctIndex) {
            earned += worth
            index++
            if (index >= questions.size) over = true
            true
        } else {
            // Safe checkpoints: a miss keeps what was banked at the last milestone.
            earned = rewards.take(index / CHECKPOINT_EVERY * CHECKPOINT_EVERY).sum().coerceAtMost(earned)
            over = true
            false
        }
    }

    /** Stops while ahead and keeps everything earned. */
    fun walkAway() { over = true }

    companion object {
        const val LENGTH = 10
        const val CHECKPOINT_EVERY = 5
        val REWARDS = listOf(5, 5, 10, 10, 20, 20, 30, 30, 40, 60)

        /** Picks [LENGTH] questions: three easy, four medium, three hard (as far as the bank allows). */
        fun start(bank: List<KnowledgeQuestion> = KnowledgeBank.questions, random: Random = Random.Default): KnowledgeQuizRun {
            fun pick(level: Int, n: Int) = bank.filter { it.level == level }.shuffled(random).take(n)
            val chosen = pick(1, 3) + pick(2, 4) + pick(3, 3)
            val filler = bank.filterNot { it in chosen }.shuffled(random)
            val all = (chosen + filler).take(LENGTH).sortedBy { it.level }
            return KnowledgeQuizRun(all.map { shuffleOptions(it, random) })
        }

        private fun shuffleOptions(q: KnowledgeQuestion, random: Random): KnowledgeQuestion {
            val correct = q.options[q.correctIndex]
            val shuffled = q.options.shuffled(random)
            return q.copy(options = shuffled, correctIndex = shuffled.indexOf(correct))
        }
    }
}

/** Offline question bank (Vietnamese, general knowledge). */
object KnowledgeBank {
    private fun q(level: Int, text: String, correct: String, vararg wrong: String) =
        KnowledgeQuestion(text, listOf(correct) + wrong, 0, level)

    val questions: List<KnowledgeQuestion> = listOf(
        q(1, "Thủ đô của Việt Nam là thành phố nào?", "Hà Nội", "Huế", "Đà Nẵng", "TP. Hồ Chí Minh"),
        q(1, "Một tuần có bao nhiêu ngày?", "7", "5", "6", "8"),
        q(1, "Hành tinh nào gần Mặt Trời nhất?", "Sao Thủy", "Sao Kim", "Trái Đất", "Sao Hỏa"),
        q(1, "Con vật nào được gọi là chúa sơn lâm?", "Hổ", "Sư tử biển", "Gấu", "Báo"),
        q(1, "Nước sôi ở bao nhiêu độ C (áp suất thường)?", "100", "90", "80", "120"),
        q(1, "Cầu vồng thường có bao nhiêu màu?", "7", "5", "6", "9"),
        q(1, "Bộ phận nào của cây hút nước từ đất?", "Rễ", "Lá", "Hoa", "Thân"),
        q(1, "Trái Đất quay quanh vật thể nào?", "Mặt Trời", "Mặt Trăng", "Sao Hỏa", "Sao Mộc"),
        q(1, "Sông Hồng chảy qua thủ đô nào?", "Hà Nội", "Huế", "Cần Thơ", "Đà Lạt"),
        q(1, "Loài vật nào cho ta sữa phổ biến nhất?", "Bò", "Gà", "Cá", "Vịt"),
        q(2, "Ai là tác giả của truyện Kiều?", "Nguyễn Du", "Nguyễn Trãi", "Hồ Xuân Hương", "Nguyễn Đình Chiểu"),
        q(2, "Vịnh Hạ Long thuộc tỉnh nào?", "Quảng Ninh", "Hải Phòng", "Thanh Hóa", "Nghệ An"),
        q(2, "Kim loại nào dẫn điện tốt nhất trong các kim loại thông dụng?", "Bạc", "Sắt", "Nhôm", "Chì"),
        q(2, "Đại dương nào lớn nhất thế giới?", "Thái Bình Dương", "Đại Tây Dương", "Ấn Độ Dương", "Bắc Băng Dương"),
        q(2, "Chữ quốc ngữ dùng bảng chữ cái nào?", "La-tinh", "Hán", "Cyrillic", "Ả Rập"),
        q(2, "Núi cao nhất Việt Nam là núi nào?", "Fansipan", "Bà Đen", "Langbiang", "Yên Tử"),
        q(2, "Khí nào chiếm nhiều nhất trong không khí?", "Nitơ", "Oxy", "Cacbonic", "Hiđrô"),
        q(2, "Ai được gọi là Cha đẻ của thuyết tương đối?", "Albert Einstein", "Isaac Newton", "Galileo Galilei", "Nikola Tesla"),
        q(2, "Quốc gia nào có hình chiếc ủng trên bản đồ?", "Ý", "Pháp", "Tây Ban Nha", "Hy Lạp"),
        q(2, "Đơn vị đo cường độ dòng điện là gì?", "Ampe", "Vôn", "Oát", "Ôm"),
        q(2, "Nhạc cụ nào có 88 phím?", "Đàn piano", "Đàn guitar", "Đàn tranh", "Sáo"),
        q(2, "Ngày Quốc khánh của Việt Nam là ngày nào?", "2/9", "30/4", "19/5", "1/5"),
        q(3, "Nguyên tố hóa học có ký hiệu Au là gì?", "Vàng", "Bạc", "Nhôm", "Đồng"),
        q(3, "Hiệp định Giơ-ne-vơ về Đông Dương được ký năm nào?", "1954", "1945", "1973", "1975"),
        q(3, "Họa sĩ nào vẽ bức Mona Lisa?", "Leonardo da Vinci", "Michelangelo", "Raphael", "Van Gogh"),
        q(3, "Số Pi xấp xỉ bằng bao nhiêu?", "3,14", "2,71", "1,62", "4,13"),
        q(3, "Con sông dài nhất thế giới thường được nhắc đến là sông nào?", "Sông Nile", "Sông Amazon", "Sông Mê Kông", "Sông Hoàng Hà"),
        q(3, "Thành phố nào là thủ đô của Nhật Bản?", "Tokyo", "Kyoto", "Osaka", "Nagoya"),
        q(3, "Nhà văn nào là tác giả của Dế Mèn phiêu lưu ký?", "Tô Hoài", "Nam Cao", "Ngô Tất Tố", "Nguyên Hồng"),
        q(3, "Đơn vị đo tần số là gì?", "Hz", "Pa", "N", "J"),
        q(3, "Kỳ quan nào nằm ở Ai Cập?", "Kim tự tháp Giza", "Vạn Lý Trường Thành", "Taj Mahal", "Đấu trường La Mã"),
        q(3, "Bao nhiêu độ là tổng ba góc trong một tam giác?", "180", "90", "270", "360")
    )
}
