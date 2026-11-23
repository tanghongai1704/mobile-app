# Kịch bản demo OneSpace

Thời lượng khoảng 6 phút. Có hai bản APK (release, R8, khoảng 6 MB, ký bằng khoá debug để cài thẳng):

| Bản | Build | Khi nào dùng |
|---|---|---|
| Offline (backend giả) | `./gradlew :app:assembleRelease` | Mặc định. Cài là chạy, không cần server; mọi tính năng trừ đồng bộ tài khoản. |
| Server | `./gradlew :app:assembleRelease -Ponespace.apiBaseUrl=http://<IP-máy-chạy-server>:8080/v1/` | Khi có laptop chạy server cùng mạng với điện thoại/emulator. |

APK ra ở `app/build/outputs/apk/release/app-release.apk`; build bản nào thì đổi tên ngay (`onespace-offline.apk`, `onespace-server.apk`) vì hai lần build ghi cùng một file.

Emulator dùng IP `10.0.2.2`. Điện thoại thật dùng IP LAN của laptop (ví dụ `192.168.1.20`); khi đó thêm IP này vào `app/src/main/res/xml/network_security_config.xml` (HTTP chỉ được phép tới các địa chỉ liệt kê ở đó) rồi build lại.

## Chuẩn bị (trước giờ demo)

1. Server (bỏ qua nếu dùng bản offline):
   ```bash
   cd backend
   docker compose up -d
   tools/make-sample-media.sh media 5
   DB_URL=jdbc:postgresql://localhost:5432/onespace DB_USER=onespace DB_PASSWORD=onespace \
   MEDIA_DIR=media PUBLIC_BASE_URL=http://<IP>:8080 ../gradlew -p . run
   ```
   Kiểm tra: `curl http://<IP>:8080/health` trả `ok`; mở `http://<IP>:8080/v1/catalog/videos?limit=1` từ trình duyệt điện thoại.
2. Cài APK, mở app một lần, đi qua onboarding, đăng nhập `demo@onespace.vn` / `secret1` (lần đầu server tự tạo tài khoản).
3. Tải sẵn 1 bài hát (nút tải trong trình phát) để demo offline.
4. Mở sẵn một tab terminal với `psql` (hoặc trình duyệt tới `/v1/me/points` kèm token) để cho xem dữ liệu phía server.

## Kịch bản

| Phút | Thao tác | Nói gì (ứng với phần nào trong report) |
|---|---|---|
| 0:00 | Mở app → Trang chủ: điểm, chuỗi ngày, 3 nhiệm vụ, đề xuất | Bốn phân hệ trong một app; điều hướng 5 tab (sơ đồ điều hướng) |
| 0:40 | Tab Nhạc → phát "Nắng sớm" → về Trang chủ → khoá màn hình | Phát nền bằng MediaSessionService, điều khiển trên thông báo; nhiệm vụ "Nghe 1 bài nhạc" tự hoàn thành, +20 điểm |
| 1:30 | Tab Video → mở một video → kéo cửa sổ nổi sang tab khác | Một nguồn tiếng tại một thời điểm (SessionCoordinator); floating video |
| 2:15 | Tab Tin → mở bài → Nghe (TTS) → thêm vào playlist | Siêu playlist trộn video, nhạc, bài báo |
| 3:00 | Tài khoản → Danh sách phát → Phát playlist trộn | Hàng đợi chạy lần lượt; đến bài báo thì đọc rồi tự chuyển |
| 3:40 | Trò chơi → 2048 vài nước → tắt hẳn app → mở lại | Ván dở lưu trong Room, khôi phục sau khi tiến trình bị huỷ |
| 4:20 | Bật chế độ máy bay (hoặc Cài đặt → Mô phỏng mất mạng) → tab Tin, Nhạc, phát bài đã tải | Offline-first: UI đọc Room, mạng chỉ làm mới cache; tệp tải về phát không cần mạng |
| 5:00 | *(Bản server)* Tắt chế độ máy bay → terminal: `select source, amount from point_entries;` và `select name, version from playlists;` | Đồng bộ: outbox event idempotent theo eventId; playlist có version, xung đột 409 được gộp |
| 5:40 | Mở `docs/` hoặc slide kiến trúc | Module/tầng, verifyArchitecture chặn phụ thuộc sai |

Nếu dùng bản offline, bỏ dòng 5:00 và nói: "bản này dùng backend giả cùng hợp đồng API; server thật nằm trong thư mục `backend/`" (có thể mở `backend/src/main/resources/db/migration/V1__init.sql` cho xem schema).

## Phương án dự phòng

- **Mất Wi-Fi ở phòng demo**: dùng bản offline. Nội dung mẫu và backend giả nằm trong app; video/nhạc mẫu cần mạng, trừ bài đã tải về.
- **Server không kết nối được**: app vẫn chạy từ cache Room; event nằm trong outbox và tự gửi khi có lại mạng. Chuyển sang bản offline nếu danh sách trống.
- **Điện thoại không có giọng tiếng Việt**: TTS phát bản âm thanh mẫu (đã ghi trong README); nói rõ đây là giới hạn của máy, không phải lỗi luồng.
- **Muốn cho xem xung đột 409**: sửa playlist bằng `curl -X PUT .../v1/me/playlists/<id>` với `version` hiện tại, rồi thêm một bài vào cùng playlist trên điện thoại; log server hiện `409 Conflict` rồi `200 OK`, playlist trên server chứa cả hai thay đổi.
