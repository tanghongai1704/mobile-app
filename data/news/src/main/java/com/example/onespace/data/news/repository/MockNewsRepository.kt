package com.example.onespace.data.news.repository

import com.example.onespace.domain.news.model.Article
import com.example.onespace.domain.news.model.QuizQuestion
import com.example.onespace.domain.news.repository.NewsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

internal class MockNewsRepository : NewsRepository {
    private val savedIds = MutableStateFlow<Set<String>>(emptySet())

    override fun observeSavedIds(): Flow<Set<String>> = savedIds

    override suspend fun toggleBookmark(id: String) {
        val current = savedIds.value.toMutableSet()
        if (id in current) current.remove(id) else current.add(id)
        savedIds.value = current
    }

    override fun observeCatalog() = flow {
        delay(400)
        emit(catalog)
    }

    private val rawCatalog = listOf(
            Article(
                "n1", "Thành phố xanh bắt đầu từ ý nhỏ", "Đời sống", "4 phút đọc",
                "Khắp thành phố, những khu vườn cộng đồng nhỏ đang gắn kết mọi người lại với nhau.\\n\\nỞ góc hẻm trên đường Nguyễn Thị Minh Khai, một mảnh đất trống hai năm trước giờ xanh mướt rau sạch và hoa hồng. Chị Lan – người khởi xướng – chỉ mới chuyển đến đây ba năm nhưng đã kéo được hai mươi hàng xóm cùng làm.\\n\\n\"Ban đầu chỉ định trồng mấy chậu rau cho vui, ai ngờ cả xóm xúm vào giúp, giờ thành điểm hẹn mỗi sáng thứ bảy\", chị kể. Trẻ con phụ tưới nước, người già góp hạt giống, người trẻ chụp ảnh khoe lên mạng xã hội.\\n\\nMột không gian chung có thể bắt đầu từ vài cây xanh, một chút kiên nhẫn và một lần trò chuyện với hàng xóm. Mô hình vườn cộng đồng đang được Nhận định là một trong những cách hiệu quả nhất để cải thiện chất lượng sống đô thị mà không cần ngân sách lớn.\\n\\nTheo Sở Xanh Hà Nội, thành phố hiện có hơn 200 khu vườn cộng đồng tương tự, con số này tăng 40% so với hai năm trước.",
                quiz = listOf(
                    QuizQuestion(
                        "Bài viết nói về chủ đề gì?",
                        listOf("Vườn cộng đồng trong thành phố", "Giao thông đô thị", "Xây dựng nhà cao tầng", "Ô nhiễm không khí"),
                        0
                    ),
                    QuizQuestion(
                        "Theo bài, một không gian chung bắt đầu từ điều gì?",
                        listOf("Vài cây xanh và kiên nhẫn", "Ngân sách lớn", "Quy hoạch của chính quyền", "Sự hỗ trợ của doanh nghiệp"),
                        0
                    ),
                    QuizQuestion(
                        "Vườn cộng đồng giúp điều gì theo bài viết?",
                        listOf("Gắn kết mọi người", "Tăng giá đất", "Giảm chi phí vận chuyển", "Quảng bá du lịch"),
                        0
                    )
                )
            ),
            Article(
                "n2", "Công nghệ nhường chỗ cho cuộc sống", "Công nghệ", "6 phút đọc",
                "Công nghệ chu đáo nên làm các việc hằng ngày đơn giản hơn.\\n\\nNhắc đến từ \"smart\", nhiều người nghĩ ngay đến nhà đầy cảm biến, đèn tự bật, rèm tự đóng. Nhưng thực ra, công nghệ hữu ích nhất là loại ta không nhận ra nó tồn tại.\\n\\nMột ứng dụng nhắc uống nước mỗi giờ, một cái đồng hồ đếm bước chân, một trình đọc tin gom hết tin tức và nhạc vào một chỗ – đó là những \"người trợ giú\" thầm lặng của đời sống hiện đại.\\n\\nNghiên cứu của Decision Lab cho thấy 73% người dùng Việt Nam cảm thấy bị \"quá tải thông tin\", và xu hướng đang chuyển sang các sản phẩm tối giản, làm đúng một việc nhưng làm thật tốt.",
                quiz = listOf(
                    QuizQuestion(
                        "Quan điểm chính của bài là gì?",
                        listOf("Công nghệ nên phục vụ cuộc sống", "Công nghệ nên phức tạp hơn", "Nên loại bỏ công nghệ", "Công nghệ chỉ dành cho giới trẻ"),
                        0
                    ),
                    QuizQuestion(
                        "Tính năng nào giữ vị trí cuộn khi chuyển tab?",
                        listOf("Đích điều hướng", "Cache trình duyệt", "Lưu vào bookmark", "Tải lại toàn bộ"),
                        0
                    )
                )
            ),
            Article(
                "n3", "Chạy trốn cuối tuần kế tiếp", "Du lịch", "3 phút đọc",
                "Một chuyến đi ngắn cũng có thể mang đến góc nhìn mới.\\n\\nChỉ cần một chiếc xe máy, hai chai nước và một bản đồ giấy, bạn đã có đủ cho một ngày \"trốn\" khỏi thành phố. Cung đường Tam Đảo – Vĩnh Yên – Sơn Dương dài chưa đến 100km nhưng đủ núi, enough rừng và ruộng.\\n\\nNghỉ chân ở một quán cà phê dân dọc quốc lộ, ly cà phê chỉ 15.000 đồng nhưng view núi mây miễn phí. Chủ quán kể khách đô về cuối tuần nhiều, ai cũng bảo \"thở được\".\\n\\nNếu bạn tìm sự yên tĩnh thật sự, hãy đi trước khi nơi này thay đổi."
            ),
            Article(
                "n4", "Góc làm việc nhỏ trong thành phố lớn", "Đời sống", "5 phút đọc",
                "Một bàn làm việc gọn cũng đủ để tập trung.\\n\\nSắp xếp góc làm việc tại nhà không cần đắt tiền – một tấm ván gỗ, hai chồng gạch, một cái đèn bàn là đủ khởi đầu. Điều quan trọng là thói quen dọn dẹp cuối ngày.\\n\\nChuyên gia tổ chức không gian Marie Kondo từng nói: \"Giữ lại những gì mang lại niềm vui, cảm ơn và chia tay những thứ còn lại\". Nguyên tắc này áp dụng được cả cho bàn làm việc lẫn hộp thư điện tử."
            ),
            Article(
                "n5", "Ứng dụng nhẹ cho ngày ít thời gian", "Công nghệ", "4 phút đọc",
                "Không cần cài nhiều ứng dụng.\\n\\nMột số công cụ nhỏ giúp đọc tin, nghe nhạc và giải trí trong cùng một chỗ, tiết kiệm pin và bộ nhớ máy. Trong khi các siêu ứng dụng cạnh tranh bằng cách thêm nhiều tính năng, nhóm người dùng \"tối giản\" ngày càng lớn – họ chọn ít app hơn nhưng dùng thường xuyên hơn.\\n\\nTheo thống kê của App Annie, người Việt trung bình cài 80 ứng dụng nhưng chỉ dùng 25 trong đó hằng ngày."
            ),
            Article(
                "n6", "Đường ven sông buổi sáng", "Du lịch", "4 phút đọc",
                "Đi bộ dọc sông vào buổi sớm là cách bắt đầu ngày nhẹ nhàng.\\n\\nTừ cầu Sài Gòn đến bến Bach Đằng chỉ 2km nhưng đủ để ngắm bình minh, hít không khí trong lành trước khi thành phố thức giấc. Cung đường này an toàn, có vỉa hè rộng và phù hợp cho cả người mới bắt đầu chạy bộ.\\n\\nMột số mẹo cho người mới: khởi động 5 phút, đi nhanh 10 phút, sau đó từ từ tăng tốc. Mang theo nước, đeo giày có đệm tốt."
            ),
            // ---- Đời sống ----
            Article("n7", "Chợ ấm - nơi thành phố còn giữ được nhịp chậm", "Đời sống", "3 phút đọc",
                "Bốn giờ sáng, chợ đầu mối đã đông người. Tiếng gọi hàng, tiếng cân kẽo kẹt làm nên một bản nhạc quen thuộc.\n\nVới nhiều người, đi chợ sớm không chỉ để mua đồ ăn mà còn để gặp gỡ, hỏi thăm nhau một câu."),
            Article("n8", "Người đưa thư cuối cùng của con hẻm", "Đời sống", "4 phút đọc",
                "Dù thư tay đã hiếm, chú Ba vẫn đi từng con hẻm đều đặn mỗi sáng. Chiếc xe đạp cũ và chiếc túi vải xanh gắn với chú đã hơn hai mươi năm.\n\nCó những bức thư mà thành phố không thể thay thế không thể thay thế: niềm mong chờ của người nhận."),
            Article("n9", "Café bể - quán cà phê không bảng hiệu", "Đời sống", "3 phút đọc",
                "Nằm trong một con hẻm nhỏ, quán chỉ có năm cái bàn thấp và vài tấm mái lá. Chủ quán pha kiểu thủ công, không đường hoá học.\n\nNgười đến đây đa phần là hàng xóm, ngồi nói chuyện đủ thứ chuyện đời thường."),
            Article("n10", "Thư gửi người trẻ sắp rời ký túc xá", "Đời sống", "5 phút đọc",
                "Bốn năm dài vậy mà chỉ còn vài hộp giấy để gói hết. Căn phòng này từng là nơi mình khóc, cười và trưởng thành.\n\nHãy mang theo không chỉ đồ đạc mà cả những bài học về tình bạn và tự lập."),
            // ---- Công nghệ ----
            Article("n11", "5 mẹo tiết kiệm pin bạn nên biết", "Công nghệ", "3 phút đọc",
                "Pin là thứ dễ hao nhất trên điện thoại. Hạ độ sáng màn hình và tắt thông báo không cần thiết là hai việc đơn giản nhất.\n\nBật chế độ tối cũng giúp màn hình AMOLED tiết kiệm đáng kể pin.",
                quiz = listOf(
                    QuizQuestion(
                        "Mẹo nào giúp màn hình AMOLED tiết kiệm pin?",
                        listOf("Bật chế độ tối", "Bật độ sáng tối đa", "Mở nhiều ứng dụng", "Tắt Wi-Fi hoàn toàn"),
                        0
                    )
                )),
            Article("n12", "Rác thải điện tử: câu chuyện ít ai để ý", "Công nghệ", "5 phút đọc",
                "Mỗi năm cả nước thải bỏ hàng triệu điện thoại cũ. Chỉ một phần nhỏ được thu gom và tái chế đúng cách.\n\nChiếc điện thoại cũ của bạn có thể trở thành nguyên liệu quý nếu được đưa về đúng nơi."),
            Article("n13", "Cách chọn tai nghe phù hợp túi tiền", "Công nghệ", "4 phút đọc",
                "Không phải tai nghe đắt nhất là tốt nhất với bạn. Hãy xác định nhu cầu: nghe nhạc, chơi game hay gọi điện.\n\nMột chiếc tai nghe giá vừa túi nhưng đúng nhu cầu sẽ dùng bền hơn nhiều so với đồ đắt sai mục đích."),
            Article("n14", "Tự động hoá việc nhà với công nghệ giá rẻ", "Công nghệ", "4 phút đọc",
                "Bạn không cần nhà thông minh đắt tiền để cuộc sống dễ hơn. Một ổ cắm hẹn giờ cũng đã giúp đèn tự tắt buổi tối.\n\nCác thiết bị nhỏ, rẻ tiền tích hợp lại vẫn tạo nên trải nghiệm tiện lợi đáng ngạc nhiên."),
            // ---- Du lịch ----
            Article("n15", "Đà Lạt mùa hoa dã quỳ", "Du lịch", "4 phút đọc",
                "Tháng mười một, dã quỳ nở vàng khắp các cung đường đồi. Chỉ cần một chiếc xe máy là đủ cho chuyến đi khó quên.\n\nSương sớm lạnh buốt nhưng nắng lên rất nhanh, hãy chuẩn bị áo ấm nhẹ.",
                quiz = listOf(
                    QuizQuestion(
                        "Mùa hoa dã quỳ Đà Lạt thường vào tháng mấy?",
                        listOf("Tháng 11", "Tháng 3", "Tháng 6", "Tháng 8"),
                        0
                    )
                )),
            Article("n16", "Bụi phấn trắng - 36 phố phường Hà Nội", "Du lịch", "5 phút đọc",
                "Những con phố cổ có rất nhiều câu chuyện trong từng bức tường rêu phong. Đi bộ là cách tốt nhất để cảm nhận.\n\nHãy thử bắt đầu từ cửa Nam rồi để chân mình dẫn đi, không cần lịch trình cứng nhắc."),
            Article("n17", "Biển Quỳnh - bình minh không báo trước", "Du lịch", "3 phút đọc",
                "Nơi đây chưa có resort lớn, chỉ có thuyền cá và bãi cát còn nguyên vẹn. Sáng nào cũng có người dân đi chợ biển.\n\nNếu bạn tìm sự yên tĩnh thật sự, hãy đến trước khi nơi này thay đổi."),
            Article("n18", "Đèo Hải Vân - cung đường đẹp nhất Việt Nam", "Du lịch", "4 phút đọc",
                "Một bên là núi, một bên là biển, đèo Hải Vân mang vẻ đẹp hoang sơ hiếm có. Sương mù che phủ nửa đỉnh là cảnh quen thuộc.\n\nCung đường này đẹp nhất vào sáng sớm hoặc chiều muộn, tránh nắng gắt giữa trưa."),
            // ---- Thể thao ----
            Article("n19", "Bóng đá - vì sao chúng ta mê đám đông", "Thể thao", "4 phút đọc",
                "Chín mươi phút trên sân cỏ kéo theo hàng triệu trái tim ngoài kia. Sự cuồng nhiệt ấy không hoàn toàn chỉ vì trái bóng.\n\nNó còn là dịp để gặp gỡ, ăn mừng cùng những người cùng chung niềm vui.",
                quiz = listOf(
                    QuizQuestion(
                        "Theo bài, điều gì khiến người ta mê bóng đá ngoài trận đấu?",
                        listOf("Dịp gặp gỡ cùng niềm vui chung", "Vì được nghỉ làm", "Vì được ăn ngon", "Vì thích tranh cãi"),
                        0
                    )
                )),
            Article("n20", "Chạy bộ 5km - khởi đầu cho người mới", "Thể thao", "3 phút đọc",
                "Không cần chạy nhanh, chỉ cần ra khỏi cửa. Năm cây số đầu tiên luôn khó nhất nhưng cũng đáng nhớ nhất.\n\nHãy đầu tư một đôi giày thật phù hợp trước khi bắt đầu tập luyện."),
            Article("n21", "Leo núi - môn thể thao rẻ nhất nhưng dễ nghiện", "Thể thao", "4 phút đọc",
                "Chỉ cần một đôi giày tốt và một chai nước, bạn đã có thể chinh phục đỉnh núi gần nhà.\n\nCảm giác đứng trên cao nhìn xuống sẽ khiến bạn quên hết mệt nhọc trên đường đi."),
            Article("n22", "Cầu lông - môn thể thao quốc dân ở sân thượng", "Thể thao", "3 phút đọc",
                "Mỗi chiều tan học, sân thượng các chung cư lại vang tiếng cầu chạm vợt. Một trận đấu có thể kéo dài đến tận tối.\n\nĐây là môn thể thao gắn kết nhiều thế hệ trong một gia đình."),
            // ---- Ẩm thực ----
            Article("n23", "Phở - tinh hoa ẩm thực từ nồi nước dùng", "Ẩm thực", "4 phút đọc",
                "Một bát phở ngon bắt đầu từ xương hầm kỹ càng, không chất điều vị. Thời gian ninh là bí quyết không thể thay thế.\n\nBánh phở mềm, thịt thái mỏng và rau thơm tươi là ba yếu tố quyết định.",
                quiz = listOf(
                    QuizQuestion(
                        "Điều gì quyết định bát phở ngon theo bài viết?",
                        listOf("Nồi nước dùng hầm kỹ", "Giá đỗ nhiều", "Ớt tươi đỏ", "Bát to"),
                        0
                    )
                )),
            Article("n24", "Bánh mì Việt - món ăn đường phố hoàn hảo", "Ẩm thực", "3 phút đọc",
                "Vỏ giòn ruột mềm, bên trong đầy ắp pate, thịt, rau thơm. Bánh mì Việt được nhiều tạp chí xếp vào nhóm đường phố ngon nhất.\n\nMỗi thành phố có một biến tấu riêng, đều đáng thử một lần."),
            Article("n25", "Cà phê trứng - sáng tạo làm nên thương hiệu", "Ẩm thực", "3 phút đọc",
                "Sinh ra trong khó khăn, cà phê trứng lại trở thành niềm tự hào của người Hà Nội. Kem trứng đánh bông mềm mịn quyện cùng đắng nhẹ.\n\nNgày nay, nhiều du khách tìm đến chỉ để thử một lần cho biết."),
            Article("n26", "Bún bò Huế - đậm đà bản sắc miền Trung", "Ẩm thực", "4 phút đọc",
                "Nước dùng trong veo nhưng cay nồng, sợi bún nhỏ dai. Mỗi tô bún bò là cả một nghệ thuật cân bằng vị giác.\n\nNếu đến Huế, đừng chỉ ăn một lần rồi đi - hãy thử nhiều quán để so sánh."),
            // ---- Thiên nhiên ----
            Article("n27", "Rừng nguyên sinh - lá phổi xanh còn lại", "Thiên nhiên", "5 phút đọc",
                "Những khu rừng già này đã tồn tại hàng trăm năm, là nhà của vô số loài động thực vật quý hiếm.\n\nMỗi khu rừng bị mất đi là một hệ sinh thái không thể phục hồi.",
                quiz = listOf(
                    QuizQuestion(
                        "Rừng nguyên sinh được ví là gì theo bài viết?",
                        listOf("Lá phổi xanh", "Bức tường thép", "Mái nhà chung", "Chiếc khiên đỏ"),
                        0
                    )
                )),
            Article("n28", "Đồng bằng sông Cửu Long - vựa lúa của quốc gia", "Thiên nhiên", "4 phút đọc",
                "Hệ thống sông ngòi chằng chịt nuôi dựng những cánh đồng lúa bát ngát. Mùa nước nổi mang phù sa cho cả vùng.\n\nCuộc sống nơi đây gắn liền với con nước, con thuyền và những khu chợ nổi."),
            Article("n29", "Hà Giang - địa hình đá vôi độc đáo", "Thiên nhiên", "4 phút đọc",
                "Những dãy núi đá vôi sừng sững tạo nên cảnh quan hùng vĩ hiếm có. Cung đường đèo quanh co, đầy thử thách.\n\nMùa buckwheat hoa nở vào tháng mười khiến cả vùng như khoác áo mới."),
            Article("n30", "Biển đảo Việt Nam - tiềm năng du lịch xanh", "Thiên nhiên", "3 phút đọc",
                "Với hơn ba nghìn cây số bờ biển, Việt Nam có mọi loại hình du lịch biển từ nghỉ dưỡng đến khám phá.\n\nPhát triển đúng cách sẽ vừa giữ được môi trường vừa nâng cao đời sống ngư dân."),
            // ---- Đời sống (tiếp) ----
            Article("n31", "Sách giấy trong thời điện tử", "Đời sống", "3 phút đọc",
                "Giữa thời đọc nhanh, vẫn có những người sưu tầm và đọc sách giấy. Mùi mực in và cảm giác lật trang không gì thay thế được.\n\nNhiều quán cà phê sách đang nở rộ, trở thành điểm hẹn của người yêu chữ nghĩa."),
            Article("n32", "Người nghệ nhân cuối cùng làm đèn lồng", "Đời sống", "4 phút đọc",
                "Cả làng ngày nào có hàng trăm hộ làm đèn lồng, giờ chỉ còn gia đình cô Tư còn giữ nghề. Bốn mươi năm gắn bó với tre và giấy dó.\n\nCô bảo nghề này không giàu nhưng bỏ thì tiếc, vì còn ai làm đèn cho Tết trẻ con nó vui."),
            // ---- Công nghệ (tiếp) ----
            Article("n33", "Chữ ký số - tiện lợi nhưng cần cẩn thận", "Công nghệ", "4 phút đọc",
                "Chữ ký số giúp giao dịch nhanh hơn nhưng cũng kéo theo rủi ro nếu bị lộ khoá. Hãy bảo quản cẩn thận thiết bị chứa khoá.\n\nKhông bao giờ ký vào văn bản bạn chưa đọc kỹ, dù là với người quen."),
            Article("n34", "Mạng xã hội - giữ gìn sự tử tế trực tuyến", "Công nghệ", "3 phút đọc",
                "Mỗi bình luận bạn để lại đều ảnh hưởng đến người đọc. Hãy dừng lại ba giây trước khi bấm gửi.\n\nẨn danh không có nghĩa là mất đi trách nhiệm với lời nói của mình."),
            // ---- Du lịch (tiếp) ----
            Article("n35", "Mũi Cà Mau - điểm cuối bản đồ", "Du lịch", "3 phút đọc",
                "Đứng ở nơi đất nước kết thúc, bạn sẽ thấy cả một vùng trời nước mênh mông. Đảo Hòn Khoai thấp thoáng xa xa.\n\nĐi xa đến vậy chỉ để nghe tiếng sóng và cảm nhận sự bình yên hiếm có."),
            Article("n36", "Tràng An - hòn non nước bên những dãy núi đá", "Du lịch", "4 phút đọc",
                "Thuyền len lỏi qua những hang xuyên thủy, hai bên là vách đá vôi sừng sững. Mỗi hang mang một tên gọi riêng.\n\nNơi đây được UNESCO công nhận là di sản thế giới hỗn hợp đầu tiên của Việt Nam."),
            // ---- Thể thao (tiếp) ----
            Article("n37", "Võ cổ truyền - võ học Việt Nam", "Thể thao", "4 phút đọc",
                "Không chỉ để tự vệ, võ cổ truyền còn là cách rèn luyện bản lĩnh và đạo đức. Nhiều võ đường mở lớp miễn phí cho thiếu nhi.\n\nNgười xưa nói học võ trước học lễ - đến giờ vẫn đúng."),
            Article("n38", "Bơi - kỹ năng sống còn cần dạy từ nhỏ", "Thể thao", "3 phút đọc",
                "Biết bơi không chỉ là môn thể thao mà là kỹ năng sống còn. Nhiều trường đã đưa bơi vào chương trình ngoại khoá.\n\nHãy cho trẻ học bơi sớm, nhưng tuyệt đối không thả tự do khi chưa thành thạo."),
            // ---- Ẩm thực (tiếp) ----
            Article("n39", "Cơm nhà - khẩu vị khó thay", "Ẩm thực", "3 phút đọc",
                "Dù ngoài đường có bao nhiêu món ngon, người ta vẫn nhớ về cơm nhà mẹ nấu. Một khay cá kho, đĩa rau luộc cũng đủ ấm lòng.\n\nĂn cơm nhà không chỉ để no mà còn để về với nhau."),
            Article("n40", "Trà đạo Việt - dư vị thanh tao", "Ẩm thực", "4 phút đọc",
                "Chén trà nhỏ nhưng chứa cả nghệ thuật sống. Trà phải thong thả, nước sôi để tự nhiên, tâm phải lặng.\n\nNgày nay nhiều người trẻ tìm về trà đạo như một cách chậm lại giữa đời sống gấp gáp."),
            // ---- Thiên nhiên (tiếp) ----
            Article("n41", "Vườn quốc gia Cúc Phương - rừng già đầu tiên", "Thiên nhiên", "4 phút đọc",
                "Vườn quốc gia đầu tiên của Việt Nam, Cúc Phương có khu rừng nguyên sinh và hàng nghìn loài động thực vật.\n\nVào mùa hè, hàng triệu con ve đổ về đây tạo nên bản giao hưởng đặc trưng.",
                quiz = listOf(
                    QuizQuestion(
                        "Cúc Phương là vườn quốc gia thứ mấy của Việt Nam?",
                        listOf("Thứ nhất", "Thứ ba", "Thứ năm", "Thứ mười"),
                        0
                    )
                )),
            Article("n42", "Săn mây - trải nghiệm cho người trẻ", "Thiên nhiên", "3 phút đọc",
                "Ba giờ sáng lên đỉnh núi chỉ để chờ mây trôi dưới chân. Rét run người nhưng khi bình minh lên thì mọi mệt nhọc tan biến.\n\nNhững khoảnh khắc ấy khó dùng lời để tả cho người chưa từng trải qua."),
            // ---- Đời sống (tiếp) ----
            Article("n43", "Giao thông giờ tan tầm - chuyện thường ngày ở phố", "Đời sống", "3 phút đọc",
                "Năm giờ chiều, các nút giao bắt đầu đông dần. Ai cũng vội nhưng chẳng ai đi nhanh hơn được.\n\nGiữa dòng người, vẫn có những mẩu chuyện nhỏ khiến ta thấy thành phố không đến nỗi tệ."),
            Article("n44", "Người lao công thầm lặng của thành phố", "Đời sống", "4 phút đọc",
                "Ba giờ sáng, khi thành phố còn say ngủ, họ đã bắt đầu gom rác từng con phố. Công việc vất vả nhưng gần như vô hình.\n\nLần tới gặp họ, một lời chào cũng là sự trân trọng."),
            // ---- Công nghệ (tiếp) ----
            Article("n45", "Trí tuệ nhân tạo - đồng minh hay đối thủ", "Công nghệ", "5 phút đọc",
                "AI đang làm được nhiều việc hơn mỗi ngày. Câu hỏi không còn là nó làm được gì mà là ta muốn nó làm gì cho mình.\n\nNgười biết dùng công cụ sẽ luôn có lợi thế, dù công cụ có thông minh đến đâu."),
            Article("n46", "Đồng hồ thông minh - trợ thủ theo dõi sức khoẻ", "Công nghệ", "3 phút đọc",
                "Đo nhịp tim, đếm bước chân, theo dõi giấc ngủ - đã từ lâu đồng hồ thông minh không chỉ là phụ kiện thời trang.\n\nTuy nhiên, dữ liệu chỉ có giá trị khi bạn thực sự dùng nó để thay đổi thói quen."),
            // ---- Du lịch (tiếp) ----
            Article("n47", "Sa Pa - mây ở ngay trước hiên nhà", "Du lịch", "4 phút đọc",
                "Người ta đến Sa Pa để ngắm ruộng bậc thang và săn mây. Nhưng điều đáng nhớ nhất có khi lại là bữa cơm nhà dân.\n\nMùa lúa chín vào tháng chín là thời điểm đẹp nhất trong năm."),
            Article("n48", "Cù Lao Chàm - hòn đảo xanh", "Du lịch", "3 phút đọc",
                "Chỉ cách bờ biển Cửa Đại một chuyến thuyền, Cù Lao Chàm vẫn giữ được nét hoang sơ hiếm thấy.\n\nNước biển trong vắt nhìn thấy đáy, san hô nhiều màu dưới lòng biển nông."),
            // ---- Thể thao (tiếp) ----
            Article("n49", "Đạp xe - tự do trên hai bánh", "Thể thao", "3 phút đọc",
                "Không cần bằng lái, không tốn xăng, chỉ cần một đôi chân mạnh và trái tim yêu khám phá.\n\nXe đạp cũng là phương tiện hoàn hảo để khám phá những cung đường mới mỗi cuối tuần."),
            Article("n50", "Thiền - môn thể thao cho tâm trí", "Thể thao", "4 phút đọc",
                "Ngồi yên một chỗ nghe có vẻ dễ, nhưng thuần hoá tâm trí khó hơn chạy marathon. Mười phút mỗi ngày là đủ để cảm nhận khác biệt.\n\nĐây là bài cuối trong bộ sưu tập mẫu của OneSpace - cảm ơn bạn đã đọc đến đây.")
    )

    /** Handwritten quizzes win; every other article gets a generated pair. */
    private val catalog: List<Article> = rawCatalog.mapIndexed { index, article ->
        if (article.quiz.isNotEmpty()) article
        else article.copy(quiz = fallbackQuiz(article, index))
    }

    private fun fallbackQuiz(article: Article, index: Int): List<QuizQuestion> {
        val otherCategories = rawCatalog.map { it.category }.distinct().filter { it != article.category }
        val catOptions = listOf(
            otherCategories[index % otherCategories.size],
            otherCategories[(index + 3) % otherCategories.size],
            article.category
        ).distinct().shuffled(kotlin.random.Random(index))
        val others = rawCatalog.filter { it.id != article.id }
        val titleOptions = listOf(
            others[index % others.size].title,
            others[(index + 7) % others.size].title,
            article.title
        ).distinct().shuffled(kotlin.random.Random(index + 1))
        return listOf(
            QuizQuestion(
                "Bài \"" + article.title + "\" thuộc chuyên mục nào?",
                catOptions,
                catOptions.indexOf(article.category)
            ),
            QuizQuestion(
                "Bài vừa đọc có tiêu đề nào?",
                titleOptions,
                titleOptions.indexOf(article.title)
            )
        )
    }
}
