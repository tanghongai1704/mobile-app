# OneSpace

Ứng dụng giải trí Android (Jetpack Compose) gộp Video, Music, News và Games. App shell, feature, domain, data và core là Gradle module riêng (23 module). Tin tức và tài khoản đi qua API thật (Retrofit/OkHttp) tới một backend giả lập chạy ngay trong app; danh mục video và nhạc vẫn là dữ liệu mẫu.

## Cấu trúc

```text
app/                    Activity, Application, shell, root NavHost, Hilt bindings
domain/                 Pure Kotlin models, contracts, use cases, policies
data/
  auth/                 AuthApi client (OTP/mật khẩu), token mã hoá bằng Keystore, route backend giả /auth/*
  video/ music/         Catalog mẫu; Room viewing progress, thư viện trích âm thanh
  news/                 Offline-first: Room cache + NewsApi; route backend giả /news/*
  games/                Catalog game, điểm/nhiệm vụ (Room), ván 2048 dở (Room), pet/shop/streak (DataStore)
feature/
  auth/                 Email → OTP → guest/authenticated session
  onboarding/           Welcome → multitasking → interests
  home/ video/ music/ news/ games/ account/
    navigation/         Typed Route + NavGraph
    presentation/       State, ViewModel, screens
core/
  designsystem/         Material 3 colors, bundled fonts, shapes, components
  common/               Shared UI types and formatting
  session/              Cross-feature playback coordinator, mixed playlist session (video/nhạc/bài báo)
  media/                Media3 engines, video surface, MusicPlaybackService (phát nền), TTS, tệp offline, xử lý tai nghe/cuộc gọi
  datastore/            Settings and session preferences
  database/             Room schema v6 (8 bảng) + migration 5→6
  network/              Retrofit APIs, error envelope, FakeApiInterceptor (backend giả)
  sync/                 ReminderWorker (WorkManager) cho nhắc nhiệm vụ/chuỗi ngày
```

Không có dependency feature → feature/data. Screens nhận state/callback, graph sở hữu route/ViewModel; app chọn repository qua Hilt. [Bản đồ implementation](docs/architecture.md). **Nguồn chốt kiến trúc cho bài:** [LaTeX Section 4](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/main.tex).

## Navigation và auth

System splash → đọc phiên → onboarding / auth / main. Main có năm nested tab graphs giữ back stack và Account graph riêng. Mọi route dùng Kotlin Serialization và Navigation Compose typed API. Feature khai báo graph và phát callbacks; app/OneSpaceAppState thực thi điều hướng. Catalog/auth dùng graph-scoped ViewModel qua graph-entry provider, feature không nhận NavController.

Auth gọi `AuthApi` (`/auth/otp`, `/auth/verify`, `/auth/login`, `/auth/logout`) qua Retrofit/OkHttp. Chưa có server nên `FakeApiInterceptor` đóng vai backend: OTP luôn là **123456**, hết hạn sau 5 phút, sai 5 lần phải gửi lại mã; đăng nhập mật khẩu cần ít nhất 6 ký tự. Không có SMS/email thật. Access/refresh token được mã hoá AES-GCM bằng khoá Android Keystore trước khi lưu. Guest mode lưu trên thiết bị; đăng xuất trở lại Auth và giữ interests.

## Network và backend giả

`core/network` khai báo `CatalogApi`, `NewsApi`, `AuthApi`, `MeApi` theo hợp đồng API trong report (base URL `https://api.onespace.app/v1/`). Lỗi luôn có dạng `{ "error": { code, message, details } }` và được đổi thành `ApiException`; mất mạng thành `NetworkUnavailableException`. `FakeApiInterceptor` là interceptor cuối chuỗi OkHttp: mỗi module data đăng ký route của mình (`@IntoSet FakeRoute` trong `app/di`), nó trả JSON đúng hợp đồng và vẫn ném lỗi mạng khi thiết bị mất kết nối thật hoặc khi bật **Cài đặt → Mô phỏng mất mạng**. Có server thật thì bỏ interceptor và đổi base URL; repository, ViewModel và UI giữ nguyên.

## Chức năng

Material 3 light/dark theme "Blue Calm" (xanh dịu, seed `#3B82F6`, bảng màu sinh theo Material Theme Builder), Lexend headings + Be Vietnam Pro body đóng gói offline. Mọi màu nằm trong `core/designsystem` (`Color.kt`, `ExtendedColors.kt`); feature chỉ dùng `MaterialTheme.colorScheme` và `MaterialTheme.oneSpaceColors`. Có onboarding, login/verify, Home, catalog/detail và settings.

Video/nhạc dùng ExoPlayer phát sample stream thật, có controls, music dock và conflict policy. Nhạc chạy nền qua `MusicPlaybackService` (Media3 `MediaSessionService`): điều khiển trên thông báo, màn hình khoá và tai nghe Bluetooth, tự dừng khi rút tai nghe.

**Tin tức offline-first**: màn Tin đọc từ bảng Room `news_articles`; mỗi lần mở feed app tải lại qua `NewsApi` rồi ghi đè cache. Mất mạng vẫn đọc được tin đã tải; cache rỗng thì hiện lỗi kèm nút "Thử lại".

**Siêu playlist**: một playlist chứa cả video, bài hát và bài báo (thêm bài báo bằng nút "Thêm vào danh sách phát" trong trình đọc). Khi phát, các mục chạy lần lượt; đến bài báo thì TTS đọc to rồi tự chuyển sang mục kế; mỗi lúc chỉ một nguồn tiếng. Floating video là cửa sổ nhỏ (224dp) kéo đặt ở bất kỳ đâu, tự dính mép trái/phải khi thả và nhớ vị trí qua các tab. Room lưu progress/watchlist/điểm/nhiệm vụ; DataStore lưu settings/phiên, tin đã lưu, playlist, avatar và khung avatar.

**Game** (module `feature/games`, luật chơi thuần Kotlin trong `domain/games`): 2048 vuốt gộp ô, Tic-Tac-Toe đấu máy 3 mức (mức Khó dùng minimax, không thể thua) hoặc 2 người, Đoán bài hát, Quiz kiến thức, Rắn, Beat Tiles. Ván 2048 dở được lưu vào Room sau mỗi nước đi và tự khôi phục khi mở lại app. Kỷ lục từng game lưu trên máy; thử thách hằng ngày xoay vòng theo ngày, thưởng điểm theo chuỗi ngày và một thức ăn thưởng cho thú cưng; 9 huy hiệu game có tiến độ; quiz cuối video có 4 kiểu câu hỏi và combo. Điểm, nhiệm vụ, streak, thú cưng, cửa hàng và huy hiệu nằm ở Tài khoản.

**Ngôn ngữ**: Tùy chỉnh → Ngôn ngữ đổi Tiếng Việt/English ngay, không cần khởi động lại (mặc định tiếng Việt). Chữ giao diện dùng `tr(vi, en)`; chữ đến từ domain/ViewModel/media core nằm trong `Translations` (hiển thị bằng `String.t()`). Nội dung mẫu (tên video, bài hát, bài báo) là dữ liệu nên giữ tiếng Việt. Nhãn tab theo ngôn ngữ: Trang chủ/Nhạc/Tin tức/Trò chơi hoặc Home/Music/News/Games.

**Tải về offline**: nút "Tải về" ở video và bài hát lưu tệp thật vào bộ nhớ riêng của app (`files/offline/`, không cần quyền bộ nhớ); trình phát ưu tiên tệp trên máy nên xem/nghe được khi mất mạng; xoá khỏi mục Tải về thì xoá luôn tệp.

**Nhắc nhiệm vụ** (Tài khoản → Nhắc nhở): WorkManager kiểm tra mỗi giờ; từ 9:00 nhắc nhiệm vụ còn thiếu, từ 20:00 nhắc giữ chuỗi nếu hôm nay chưa làm gì; mỗi loại tối đa 1 lần/ngày, tôn trọng giờ yên tĩnh 22:00–7:00. Quyền thông báo chỉ được xin khi bật nhắc lần đầu. Bấm thông báo mở Trang chủ (nơi có nhiệm vụ hôm nay).

**Gián đoạn**: rút tai nghe hoặc có cuộc gọi thì video và giọng đọc TTS tạm dừng (bấm Phát để tiếp tục); nhạc tự nhường và tự phát lại theo audio focus.

**Chưa có**: server thật (backend đang giả lập trong app), đồng bộ dữ liệu lên server, danh mục video/nhạc từ API, đăng nhập Google, khôi phục vị trí đọc tin sau khi tiến trình bị huỷ.

## Build

Mở root bằng Android Studio, Sync Gradle, chọn run configuration **OneSpace**. Emulator nằm trong **View → Tool Windows → Running Devices**. minSdk 26, compile/targetSdk 37; daemon full JDK 25 (có jlink), bytecode JVM 11. Nếu editor chọn nhầm JRE, đặt JAVA_HOME/Gradle JDK về full JDK và restart daemon; không commit đường dẫn cục bộ.

`verifyArchitecture` còn chặn màu và font viết cứng trong `feature/` và `app/`: mọi màu đến từ `MaterialTheme.colorScheme` hoặc `MaterialTheme.oneSpaceColors`, mọi font từ `MaterialTheme.typography`.

```powershell
.\gradlew.bat verifyArchitecture
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=1
.\gradlew.bat :domain:test :core:session:testDebugUnitTest :data:auth:testDebugUnitTest --max-workers=1
# Network, tin offline-first, auth qua API giả và migration Room 5→6
.\gradlew.bat :core:network:testDebugUnitTest :data:news:testDebugUnitTest :app:testDebugUnitTest --tests "*DatabaseMigrationTest" --max-workers=1
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
# Chụp màn hình sáng/tối và EN/VI ngay trên JVM (Robolectric), không cần emulator; ảnh ra app/build/screenshots
.\gradlew.bat :app:testDebugUnitTest --tests "*ScreenshotTest" --tests "*FloatingVideoTest" --max-workers=1
```

NavigationSmokeTest kiểm tra các tab, media, player overlay, settings/offline, bookmark, email/OTP và recreate trong luồng auth. Mạng cần cho sample streams; backend giả và fonts nằm trong app nên auth/tin tức không cần server.

## Kiểm tra nhanh trên máy (trước khi demo)

1. **Tin offline**: mở tab Tin → bật chế độ máy bay (hoặc "Mô phỏng mất mạng") → tắt hẳn app → mở lại: tin vẫn hiện.
2. **Nhạc nền**: phát một bài → về Home → khoá màn hình: nhạc vẫn phát, điều khiển được trên thông báo.
3. **Đăng nhập**: OTP 123456 hoặc mật khẩu ≥ 6 ký tự; thử mã sai, đăng xuất.
4. **2048**: chơi vài nước → tắt hẳn app → mở lại: ván cũ còn nguyên.
5. **Siêu playlist**: thêm một bài báo vào playlist có video và bài hát → Phát: bài báo được đọc rồi tự chuyển mục.
6. **Tải về**: tải một bài hát → bật chế độ máy bay → phát bài đó: vẫn nghe được.
7. **Nhắc nhở**: bật nhắc trong Tài khoản → Nhắc nhở (cho phép thông báo); để thử ngay, đổi giờ máy sang sau 20:00 khi chưa làm nhiệm vụ nào rồi chờ WorkManager chạy (hoặc dùng App Inspection → Background Task Inspector để chạy worker).

Font theo SIL OFL; giấy phép trong [core/designsystem/licenses](core/designsystem/licenses). Tham khảo: [Android architecture](https://developer.android.com/topic/architecture/recommendations), [typed navigation](https://developer.android.com/guide/navigation/design/type-safety), [Material 3](https://m3.material.io/).
