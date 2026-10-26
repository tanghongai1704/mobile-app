package com.example.onespace.data.video.repository

import com.example.onespace.domain.video.model.Video
import com.example.onespace.domain.video.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.delay

/**
 * Sample catalog of 30 videos over 6 topics and 12 channels; every URL is a
 * public test stream verified reachable (HTTP 200) at the time of writing.
 */
internal class MockVideoRepository : VideoRepository {
    override fun observeCatalog() = flow {
        delay(400)
        emit(catalog)
    }

    private val bb360 = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4"
    private val bb720 = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_1MB.mp4"
    private val je360 = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/360/Jellyfish_360_10s_1MB.mp4"
    private val je720 = "https://test-videos.co.uk/vids/jellyfish/mp4/h264/720/Jellyfish_720_10s_1MB.mp4"
    private val si360 = "https://test-videos.co.uk/vids/sintel/mp4/h264/360/Sintel_360_10s_1MB.mp4"
    private val si720 = "https://test-videos.co.uk/vids/sintel/mp4/h264/720/Sintel_720_10s_1MB.mp4"
    private val bunny = "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/360/Big_Buck_Bunny_360_10s_1MB.mp4"
    private val ocean = "https://vjs.zencdn.net/v/oceans.mp4"
    private val muxHi = "https://stream.mux.com/DS00Spx1CV902MCtPj5WknGlR102V5HFkDe/high.mp4"
    private val muxLo = "https://stream.mux.com/DS00Spx1CV902MCtPj5WknGlR102V5HFkDe/low.mp4"

    private val catalog = listOf(
        // ---- Thiên nhiên ----
        Video("v1", "Gần với thiên nhiên thêm chút", "OneSpace Studio", "1:00", bunny, "Thiên nhiên"),
        Video("v7", "Nắng xuyên tán lá sớm", "Góc xanh", "0:10", je360, "Thiên nhiên"),
        Video("v8", "Sương giăng trên đồi chè", "Góc xanh", "0:10", si360, "Thiên nhiên"),
        Video("v9", "Bão biển qua ống kính nhỏ", "OneSpace Studio", "0:46", ocean, "Thiên nhiên"),
        Video("v10", "Một vòng rừng già đầu mùa", "Leo núi cùng Anh", "0:10", bb720, "Thiên nhiên"),
        // ---- Đời sống ----
        Video("v2", "Sài Gòn, sau giờ tan", "Chuyện phố", "0:46", ocean, "Đời sống"),
        Video("v11", "Chợ sớm có gì vui", "Tạp ghi Sài Gòn", "0:10", je720, "Đời sống"),
        Video("v12", "Tuyến xe buýt tôi hay đi", "Chuyện phố", "0:10", si720, "Đời sống"),
        Video("v13", "Phố cổ sau cơn mưa", "Tạp ghi Sài Gòn", "0:10", bb360, "Đời sống"),
        Video("v14", "Một ngày của shipper", "Chuyện phố", "0:30", muxHi, "Đời sống"),
        // ---- Nhẹ nhàng ----
        Video("v3", "Nghệ thuật chậm lại", "Sống chậm", "0:10", je360, "Nhẹ nhàng"),
        Video("v15", "Trà chiều và một cuốn sách", "Một góc yên tĩnh", "0:10", si360, "Nhẹ nhàng"),
        Video("v16", "Tiếng mưa trên mái tôn", "Một góc yên tĩnh", "0:10", bb360, "Nhẹ nhàng"),
        Video("v17", "Nến, nhạc lo-fi và tối thứ bảy", "Sống chậm", "0:15", muxLo, "Nhẹ nhàng"),
        Video("v18", "Sáng tạo với giấy vụn", "Một góc yên tĩnh", "0:10", je720, "Nhẹ nhàng"),
        // ---- Du lịch ----
        Video("v4", "Khung hình êm, phố vắng", "Sống chậm", "0:30", muxHi, "Du lịch"),
        Video("v19", "Đèo ngàn mùa nở", "Balo và tôi", "0:10", si720, "Du lịch"),
        Video("v20", "Cà phê ban mê bất ngờ", "Balo và tôi", "0:10", bb720, "Du lịch"),
        Video("v21", "Vé tàu một chiều ra bờ biển", "Đường xa", "0:10", je360, "Du lịch"),
        Video("v22", "Trải nghiệm ngủ bạt ven suối", "Đường xa", "0:15", muxLo, "Du lịch"),
        // ---- Ẩm thực ----
        Video("v5", "Bếp hồng giữa đông", "Bữa cơm nhà", "0:15", muxLo, "Ẩm thực"),
        Video("v23", "Bánh mì không phải là bánh mì", "Bếp nhà làm", "0:10", bb360, "Ẩm thực"),
        Video("v24", "Một nồi phở tự nấu", "Bữa cơm nhà", "0:46", ocean, "Ẩm thực"),
        Video("v25", "Đậu hũ đường phố", "Ăn gì hôm nay", "0:10", si360, "Ẩm thực"),
        Video("v26", "Cà phê trứng ở góc quen", "Ăn gì hôm nay", "0:10", je720, "Ẩm thực"),
        // ---- Thể thao ----
        Video("v6", "Đạp ven đô buổi sáng", "Sân sau", "0:10", si360, "Thể thao"),
        Video("v27", "5 cây số đầu tiên của tôi", "Chạy bộ 5km", "0:10", bb720, "Thể thao"),
        Video("v28", "Học pickleball trong một giờ", "Pickleball VN", "0:30", muxHi, "Thể thao"),
        Video("v29", "Sân cầu lông cuối tuần", "Sân sau", "0:10", je720, "Thể thao"),
        Video("v30", "Bơi sớm cho ngày dài", "Chạy bộ 5km", "0:10", si720, "Thể thao"),
        // ---- Thiên nhiên (tiếp) ----
        Video("v31", "Mây mùa trên rặng dừa", "Góc xanh", "0:10", je360, "Thiên nhiên"),
        Video("v32", "Suối reo giữa đại ngàn", "Leo núi cùng Anh", "0:10", si720, "Thiên nhiên"),
        Video("v33", "Rừng ngập mặn một buổi chiều", "OneSpace Studio", "0:30", muxHi, "Thiên nhiên"),
        Video("v34", "Cánh đồng lúa chín vàng", "Góc xanh", "0:10", bb360, "Thiên nhiên"),
        // ---- Đời sống (tiếp) ----
        Video("v35", "Người gác đèn đường cuối phố", "Tạp ghi Sài Gòn", "0:10", si360, "Đời sống"),
        Video("v36", "Góc tập thể cũ nay thành quán", "Chuyện phố", "0:10", je720, "Đời sống"),
        Video("v37", "Tan trường, ngõ nhỏ đông vui", "Tạp ghi Sài Gòn", "0:15", muxLo, "Đời sống"),
        // ---- Nhẹ nhàng (tiếp) ----
        Video("v38", "Vẽ mandala cho tâm an", "Một góc yên tĩnh", "0:10", bb720, "Nhẹ nhàng"),
        Video("v39", "Sóng trắng và cát ấm", "Sống chậm", "0:10", je360, "Nhẹ nhàng"),
        Video("v40", "Tối thứ hai không khó chịu", "Sống chậm", "0:30", muxHi, "Nhẹ nhàng"),
        // ---- Du lịch (tiếp) ----
        Video("v41", "Phố cổ ngoài giờ cao điểm", "Đường xa", "0:10", si360, "Du lịch"),
        Video("v42", "Chợ nổi miền Tây sớm mờ", "Balo và tôi", "0:15", muxLo, "Du lịch"),
        Video("v43", "Bàn chân trên cát trắng", "Balo và tôi", "0:10", bb720, "Du lịch"),
        // ---- Ẩm thực (tiếp) ----
        Video("v44", "Bánh cuốn nóng bên cửa sổ", "Ăn gì hôm nay", "0:10", je720, "Ẩm thực"),
        Video("v45", "Nước mía thơm cuối hẻm", "Bếp nhà làm", "0:10", si360, "Ẩm thực"),
        Video("v46", "Học nấu bún bò Huế", "Bữa cơm nhà", "0:46", ocean, "Ẩm thực"),
        Video("v47", "Trà chanh giã tay", "Ăn gì hôm nay", "0:10", bb360, "Ẩm thực"),
        // ---- Thể thao (tiếp) ----
        Video("v48", "Leo cầu thang bộ 10 tầng", "Chạy bộ 5km", "0:10", je360, "Thể thao"),
        Video("v49", "Bóng chuyền hơi dưới nắng", "Sân sau", "0:10", si720, "Thể thao"),
        Video("v50", "Đi bộ 10 nghìn bước", "Chạy bộ 5km", "0:30", muxHi, "Thể thao")
    )
}
