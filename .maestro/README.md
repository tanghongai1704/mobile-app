# OneSpace — Maestro demo & screenshot flows

Bộ flow Maestro chạy demo tự động toàn bộ app **OneSpace** (`com.example.onespace`)
và chụp ảnh màn hình cho từng yêu cầu chức năng (FR) để sinh `manifest.md`.

Tất cả 15 flow đã được chạy thử trực tiếp trên emulator (Maestro 2.12.0) và
đạt ở phiên bản hiện tại.

## Chạy bằng một lệnh duy nhất

```bash
scripts/run_demo.sh
```

Script tự làm mọi việc: cài Maestro CLI nếu thiếu (curl installer chính thức —
`brew install maestro` là SAI: cask đó là một app Electron khác), mở emulator
nếu chưa có thiết bị, cài app nếu thiếu, **xoá sạch `demo-run/` rồi chụp lại từ
đầu** (mỗi lần chạy), `pm clear` app để có trạng thái lần đầu tiên, chạy lần
lượt 15 flow theo thứ tự số, gom ảnh từ `~/.maestro/tests/` về
`demo-run/<dd-mm-yyyy_HHMM>/screenshots/` và sinh `manifest.md` (bảng 69 FR
✅/❌ kèm tên ảnh). Kết quả tham chiếu: 58/69 FR ✅ (11 FR còn lại là tính
năng chưa có trong app — xem manifest).

Lưu ý chạy lại: nếu emulator mới boot, chờ ~2 phút cho hệ thống ổn định
trước khi chạy (dialog "System UI isn't responding" nếu có thì bấm **Wait**).

## Danh sách flow

| Flow | Nội dung | FR chính |
|---|---|---|
| `00-onboarding.yaml` | 3 trang onboarding + chọn 3 chủ đề | F-COM-02 |
| `01-login.yaml` | Điện thoại/Email + mật khẩu, OTP demo (123456), Google, sheet Dùng thử | F-COM-01, F-COM-18 |
| `02-home.yaml` | Feed, banner điểm/nhiệm vụ/streak, Xem sau, tìm xuyên nội dung, phát tại chỗ | F-VID-02/04, F-COM-04/05/06/11 |
| `03-video.yaml` | Feed + filter, tìm kiếm + kênh, player, Lưu/playlist, tốc độ/chất lượng, chia sẻ, tải, tua, lịch sử, quiz cuối video | F-VID-01..06/11..15 |
| `04-video-floating.yaml` | Thu nhỏ → cửa sổ nổi, phát/tạm dừng, audio-only, đóng + snackbar | F-VID-08/09 |
| `05-music.yaml` | Now Playing, hàng đợi, sheet Lưu, yêu thích, dock mini, tìm, nghệ sĩ; đóng nhạc cuối flow | F-MUS-01..08 |
| `06-news.yaml` | Feed + chuyên mục, trình đọc + cỡ chữ, Lưu bài, TTS, quiz, tin liên quan, tìm | F-NEWS-01..05/08/09 |
| `07-games.yaml` | Hub, 2048, Đoán bài hát, Quiz tin, Rắn, xếp hạng, game + video nửa màn (Tic-Tac-Toe) | F-GAME-01..03/05..08/11/12 |
| `08-audio-conflict.yaml` | C1 (mở nhạc khi video 46s đang phát) + tick "Không hỏi lại", C2 (mở video khi nhạc phát) | F-COM-14, F-MUS-10 |
| `09-gamification.yaml` | Hồ sơ, điểm/streak/thú cưng, huy hiệu, khung avatar, Wrapped, nhắc nhiệm vụ, Đã tải | F-COM-03/04/06..10/12 |
| `10-shop.yaml` | Cửa hàng đổi quà + thử mua | F-COM-15 |
| `11-settings.yaml` | Sáng/tối, ngôn ngữ, tiết kiệm dữ liệu, mô phỏng mất mạng, làm lại hướng dẫn, quy tắc xung đột | F-COM-13/14/16 |
| `12-extract-mp3.yaml` | Giữ video → Trích xuất MP3 → "Từ video của bạn" trong Nhạc → phát | F-VID-10, F-MUS-07 |
| `13-states.yaml` | Không kết quả tìm kiếm, Xem sau có mục, offline banner + error state | F-COM-11/16 |
| `14-cross-navigation.yaml` | Đọc tin + dock nhạc, thẻ "Đang đọc" nổi, tìm xuyên nội dung, dock trên Video/Games, Beat Tiles (cuối cùng) | F-NEWS-07/10, F-COM-11, F-MUS-02, F-GAME-13 |

Mỗi flow **tự chứa setup** và bắt đầu bằng launchApp (không clearState) +
khoá preamble chuẩn hoá về Home.

## Ảnh & manifest

- Tên ảnh mang mã FR: `takeScreenshot: F-VID-08-floating` (Maestro tự thêm
  timestamp). Maestro 2.x lưu vào `~/.maestro/tests/<stamp>/<flow>/takeScreenshot/`
  — run_demo.sh gom tất cả về `demo-run/<stamp>/screenshots/`.
- `python3 scripts/gen_manifest.py <thư-mục-ảnh> [manifest.md]` quét mã
  `F-<nhóm>-<số>` trong tên file và sinh bảng 69 FR: ✅ + tên ảnh, hoặc ❌ +
  lý do (v2 chưa có, không có trong mã nguồn…).

## Kinh nghiệm đã rút ra khi chạy thật (quan trọng khi sửa flow)

- Nhãn tab dưới cùng là **tiếng Anh** (`Home/Video/Music/News/Games`); phần
  còn lại tiếng Việt. Chỉ tap tab **khác** với tiêu đề màn hình hiện tại.
- App **khôi phục section cuối** khi cold start → mọi flow có preamble:
  chờ "Games" (thanh tab) → tap "Home" → chờ "Playlist của tôi".
- Text của Maestro khớp **regex toàn chuỗi** (không có `exact:`): nhãn động
  dùng `'Xem sau \(.*'`; chuỗi có `(` phải escape; tiền tố phải thêm `.*`.
- Bàn phím: `pressKey: enter` chỉ đóng bàn phím trên ô search; với ô nhập
  SĐT dùng `pressKey: back`. KHÔNG dùng `hideKeyboard` trên Home (BACK bị
  BackHandler chặn và xoá nội dung tìm kiếm).
- **File video mock dài tối đa ~10 giây** bất kể nhãn thời lượng (riêng
  "Bão biển qua ống kính nhỏ" chạy thật 46s) — mọi kịch bản phụ thuộc "video
  đang phát" phải dùng video 46s đó.
- Tab video vẫn phát tiếp trong cửa sổ nổi khi chuyển tab (đã kiểm chứng);
  video "không phát" trong các lần thử trước là do file 10s đã kết thúc.
- Chuyển tab không tắt video. Cửa sổ nổi nay nhỏ (224dp) và kéo được; flow nào cần
  chạm vùng nó đang che thì kéo nó đi chỗ khác (hoặc đóng). Dock nhạc vẫn nằm ngay trên thanh tab.
- Nhãn tab theo ngôn ngữ của app; mặc định là tiếng Việt (Trang chủ/Video/Nhạc/Tin tức/Trò chơi).
- Chạm tiêu đề bài hát trên feed Nhạc dễ dính vào artist text (clickable)
  → chạm vào text thời lượng ("9:19") để mở player.
- Sheet "Lưu video": bấm "Tạo" xong phải **swipe xuống** để đóng sheet (BACK
  chỉ đóng bàn phím).
- Mọi cuộn tìm phần tử dùng `scrollUntilVisible` hay bị lỗi check 100%:
  dùng 2 swipe + `extendedWaitUntil` cho các mục dưới gầm màn hình.
- Từ khoá tìm kiếm cố tình **không dấu** (`xanh`, `onespace`, `lo-fi`, `Caf`,
  `Ch`) vì inputText của adb không gõ ổn định dấu tiếng Việt.
- Các bước `# VERIFY:` còn lại trong YAML là chỗ chọn bằng toạ độ (đáp án
  game ngẫu nhiên). **Flaky đã biết**: flow 03 (sheet), 08 (thời điểm C1),
  14 — nếu chạy lại đơn lẻ mà lỗi, chạy lại cả pipeline.
