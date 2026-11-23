# OneSpace

Ứng dụng giải trí Android (Jetpack Compose) gộp Video, Music, News và Games. App shell, feature, domain, data và core là Gradle module riêng (23 module). Video, nhạc, tin tức và tài khoản đi qua API (Retrofit/OkHttp) theo hợp đồng `/v1`. Mặc định app dùng backend giả chạy ngay trong app (không cần server); khi build với `-Ponespace.apiBaseUrl=...` app nói chuyện với server thật trong [`backend/`](backend) (Ktor + PostgreSQL) và đồng bộ điểm, nhiệm vụ, tiến độ xem, playlist.

## Cấu trúc

```text
backend/                Server Ktor + Exposed + Flyway: schema 9 bảng, seed, REST API /v1, /media
app/                    Activity, Application, shell, root NavHost, Hilt bindings
domain/                 Pure Kotlin models, contracts, use cases, policies
data/
  auth/                 AuthApi client (OTP/mật khẩu), token mã hoá bằng Keystore, route backend giả /auth/*
  video/ music/         Offline-first: Room cache (catalog_items) + CatalogApi; viewing progress, thư viện trích âm thanh; route backend giả /catalog/*
  news/                 Offline-first: Room cache + NewsApi; route backend giả /news/*
  games/                Catalog game, điểm/nhiệm vụ (Room, ghi kèm event vào outbox), ván 2048 dở (Room), pet/shop/streak (DataStore)
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
  database/             Room schema v7 (11 bảng) + migration 5→6→7
  network/              Retrofit APIs, error envelope, refresh token, FakeApiInterceptor (backend giả)
  sync/                 ReminderWorker (nhắc nhiệm vụ) và SyncWorker/SyncEngine (đồng bộ với server)
```

Không có dependency feature → feature/data. Screens nhận state/callback, graph sở hữu route/ViewModel; app chọn repository qua Hilt. [Bản đồ implementation](docs/architecture.md). **Nguồn chốt kiến trúc cho bài:** [LaTeX Section 4](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/main.tex).

## Navigation và auth

System splash → đọc phiên → onboarding / auth / main. Main có năm nested tab graphs giữ back stack và Account graph riêng. Mọi route dùng Kotlin Serialization và Navigation Compose typed API. Feature khai báo graph và phát callbacks; app/OneSpaceAppState thực thi điều hướng. Catalog/auth dùng graph-scoped ViewModel qua graph-entry provider, feature không nhận NavController.

Auth gọi `AuthApi` (`/auth/otp`, `/auth/verify`, `/auth/login`, `/auth/logout`) qua Retrofit/OkHttp. Chưa có server nên `FakeApiInterceptor` đóng vai backend: OTP luôn là **123456**, hết hạn sau 5 phút, sai 5 lần phải gửi lại mã; đăng nhập mật khẩu cần ít nhất 6 ký tự. Không có SMS/email thật. Access/refresh token được mã hoá AES-GCM bằng khoá Android Keystore trước khi lưu. Guest mode lưu trên thiết bị; đăng xuất trở lại Auth và giữ interests.

## Network và backend

`core/network` khai báo `CatalogApi`, `NewsApi`, `AuthApi`, `MeApi` theo hợp đồng API trong report. Lỗi luôn có dạng `{ "error": { code, message, details } }` và được đổi thành `ApiException`; mất mạng thành `NetworkUnavailableException`. Request `/me/...` tự gắn `Authorization: Bearer`; gặp 401 thì OkHttp `Authenticator` đổi refresh token lấy cặp token mới một lần rồi gửi lại.

Chọn backend lúc build (cờ `BuildConfig.USE_FAKE_BACKEND`):

- **Backend giả (mặc định)**: `FakeApiInterceptor` là interceptor cuối chuỗi OkHttp, mỗi module data đăng ký route của mình (`@IntoSet FakeRoute` trong `app/di`). Nó trả JSON đúng hợp đồng và vẫn ném lỗi mạng khi thiết bị mất kết nối hoặc khi bật **Cài đặt → Mô phỏng mất mạng**. Không có dữ liệu `/me` nên không đồng bộ.
- **Server thật**: `./gradlew :app:installDebug -Ponespace.apiBaseUrl=http://10.0.2.2:8080/v1/` (10.0.2.2 là máy chạy emulator). Repository, ViewModel và UI giữ nguyên; chỉ OkHttp bỏ interceptor giả. HTTP không mã hoá chỉ được phép tới 10.0.2.2/localhost (`network_security_config.xml`).

**Offline-first**: video, nhạc và tin đều đọc từ Room (`catalog_items`, `news_articles`); mỗi lần mở feed app tải lại qua API rồi ghi vào Room. Mất mạng vẫn xem danh sách đã tải; cache rỗng mới hiện lỗi.

**Đồng bộ** (`core/sync`, chỉ khi dùng server thật và đã đăng nhập): `SyncWorker` (WorkManager, cần mạng, retry backoff) chạy mỗi giờ, khi mở app, sau khi đăng nhập và sau khi sửa playlist. Mỗi lượt `SyncEngine`:

1. gửi outbox `pending_events` lên `POST /me/events` (mỗi event có `eventId` tạo trên máy nên gửi lại không cộng điểm hai lần);
2. lấy `GET /me/missions/today`, tiến độ nhiệm vụ chỉ tăng;
3. đẩy/kéo vị trí xem video qua `/me/progress`;
4. đồng bộ playlist qua `/me/playlists` có `version`; gặp `409 VERSION_CONFLICT` thì gộp hai danh sách (giữ thứ tự trên máy trước) và ghi lại trên version mới của server.

### Chạy server

```bash
cd backend
docker compose up -d                       # PostgreSQL 16 (tuỳ chọn; mặc định server dùng file H2)
tools/make-sample-media.sh media 5         # tuỳ chọn: media mẫu bằng ffmpeg, phục vụ ở /media
DB_URL=jdbc:postgresql://localhost:5432/onespace DB_USER=onespace DB_PASSWORD=onespace \
MEDIA_DIR=media PUBLIC_BASE_URL=http://10.0.2.2:8080 ../gradlew -p . run
../gradlew -p . test                        # 8 test API (H2 in-memory)
```

Không đặt `DB_URL` thì server dùng H2 file `./data/onespace` (chế độ PostgreSQL), không cần cài gì. Lần chạy đầu Flyway tạo 9 bảng (`V1__init.sql`: users, sessions, content_items, quiz_questions, playlists, playlist_items, point_entries, daily_missions, progress) và nạp 50 video, 50 bài hát, 50 bài báo kèm quiz từ `resources/seed`. Mục nào có file `MEDIA_DIR/video/<id>.mp4` hoặc `MEDIA_DIR/audio/<id>.mp3` thì URL trỏ về `/media` của server (thay cho CDN); còn lại giữ URL mẫu công khai.

| Nhóm | Endpoint |
|---|---|
| Nội dung | `GET /v1/catalog/videos`, `/v1/catalog/tracks`, `/v1/news/articles` (`?page=&limit=`, tối đa 50), `/v1/news/articles/{id}` |
| Xác thực | `POST /v1/auth/otp` (204; mã in ra log server, demo là 123456, hết hạn 5 phút, sai 5 lần → 429, tối đa 5 mã/giờ), `/verify`, `/login` (tự đăng ký nếu mới, bcrypt), `/refresh` (xoay vòng token), `/logout` |
| Cá nhân (Bearer) | `GET/POST /v1/me/playlists`, `PUT/DELETE /v1/me/playlists/{id}` (409 nếu version cũ), `GET /v1/me/progress?since=`, `PUT /v1/me/progress/{itemId}`, `GET /v1/me/missions/today`, `POST /v1/me/events`, `GET /v1/me/points` |

Token chỉ lưu dạng SHA-256 trong bảng `sessions`; access token sống 1 giờ, refresh token 30 ngày.

## Chức năng

Material 3 light/dark theme "Blue Calm" (xanh dịu, seed `#3B82F6`, bảng màu sinh theo Material Theme Builder), Lexend headings + Be Vietnam Pro body đóng gói offline. Mọi màu nằm trong `core/designsystem` (`Color.kt`, `ExtendedColors.kt`); feature chỉ dùng `MaterialTheme.colorScheme` và `MaterialTheme.oneSpaceColors`. Có onboarding, login/verify, Home, catalog/detail và settings.

Video/nhạc dùng ExoPlayer phát sample stream thật, có controls, music dock và conflict policy. Nhạc chạy nền qua `MusicPlaybackService` (Media3 `MediaSessionService`): điều khiển trên thông báo, màn hình khoá và tai nghe Bluetooth, tự dừng khi rút tai nghe.

**Tin tức offline-first**: màn Tin đọc từ bảng Room `news_articles`; mỗi lần mở feed app tải lại qua `NewsApi` rồi ghi đè cache. Mất mạng vẫn đọc được tin đã tải; cache rỗng thì hiện lỗi kèm nút "Thử lại". Video và nhạc theo cùng cách với bảng `catalog_items`.

**Siêu playlist**: một playlist chứa cả video, bài hát và bài báo (thêm bài báo bằng nút "Thêm vào danh sách phát" trong trình đọc). Khi phát, các mục chạy lần lượt; đến bài báo thì TTS đọc to rồi tự chuyển sang mục kế; mỗi lúc chỉ một nguồn tiếng. Floating video là cửa sổ nhỏ (224dp) kéo đặt ở bất kỳ đâu, tự dính mép trái/phải khi thả và nhớ vị trí qua các tab. Room lưu progress/watchlist/điểm/nhiệm vụ; DataStore lưu settings/phiên, tin đã lưu, playlist, avatar và khung avatar.

**Game** (module `feature/games`, luật chơi thuần Kotlin trong `domain/games`): 2048 vuốt gộp ô, Tic-Tac-Toe đấu máy 3 mức (mức Khó dùng minimax, không thể thua) hoặc 2 người, Đoán bài hát, Quiz kiến thức, Rắn, Beat Tiles. Ván 2048 dở được lưu vào Room sau mỗi nước đi và tự khôi phục khi mở lại app. Kỷ lục từng game lưu trên máy; thử thách hằng ngày xoay vòng theo ngày, thưởng điểm theo chuỗi ngày và một thức ăn thưởng cho thú cưng; 9 huy hiệu game có tiến độ; quiz cuối video có 4 kiểu câu hỏi và combo. Điểm, nhiệm vụ, streak, thú cưng, cửa hàng và huy hiệu nằm ở Tài khoản.

**Ngôn ngữ**: Tùy chỉnh → Ngôn ngữ đổi Tiếng Việt/English ngay, không cần khởi động lại (mặc định tiếng Việt). Chữ giao diện dùng `tr(vi, en)`; chữ đến từ domain/ViewModel/media core nằm trong `Translations` (hiển thị bằng `String.t()`). Nội dung mẫu (tên video, bài hát, bài báo) là dữ liệu nên giữ tiếng Việt. Nhãn tab theo ngôn ngữ: Trang chủ/Nhạc/Tin tức/Trò chơi hoặc Home/Music/News/Games.

**Tải về offline**: nút "Tải về" ở video và bài hát lưu tệp thật vào bộ nhớ riêng của app (`files/offline/`, không cần quyền bộ nhớ); trình phát ưu tiên tệp trên máy nên xem/nghe được khi mất mạng; xoá khỏi mục Tải về thì xoá luôn tệp.

**Nhắc nhiệm vụ** (Tài khoản → Nhắc nhở): WorkManager kiểm tra mỗi giờ; từ 9:00 nhắc nhiệm vụ còn thiếu, từ 20:00 nhắc giữ chuỗi nếu hôm nay chưa làm gì; mỗi loại tối đa 1 lần/ngày, tôn trọng giờ yên tĩnh 22:00–7:00. Quyền thông báo chỉ được xin khi bật nhắc lần đầu. Bấm thông báo mở Trang chủ (nơi có nhiệm vụ hôm nay).

**Nghe tin (TTS)**: máy có giọng tiếng Việt (Google TTS trên điện thoại thật) thì app đọc chính nội dung bài theo từng đoạn; máy không có giọng Việt (đa số emulator) thì phát một bản âm thanh mẫu để minh hoạ luồng. Bản đọc không phát được thì báo lỗi, và trong siêu playlist mục đó được bỏ qua.

**Gián đoạn**: rút tai nghe hoặc có cuộc gọi thì video và giọng đọc TTS tạm dừng (bấm Phát để tiếp tục); nhạc tự nhường và tự phát lại theo audio focus.

**Chưa có**: gửi OTP qua SMS/email thật (server chỉ ghi mã ra log), CDN và chuyển mã video thật (server phục vụ file tĩnh), đồng bộ điểm tiêu/cửa hàng/thú cưng (chỉ lưu trên máy), đăng nhập Google, khôi phục vị trí đọc tin sau khi tiến trình bị huỷ.

## Build

Mở root bằng Android Studio, Sync Gradle, chọn run configuration **OneSpace**. Emulator nằm trong **View → Tool Windows → Running Devices**. minSdk 26, compile/targetSdk 37; daemon full JDK 25 (có jlink), bytecode JVM 11. Nếu editor chọn nhầm JRE, đặt JAVA_HOME/Gradle JDK về full JDK và restart daemon; không commit đường dẫn cục bộ.

`verifyArchitecture` còn chặn màu và font viết cứng trong `feature/` và `app/`: mọi màu đến từ `MaterialTheme.colorScheme` hoặc `MaterialTheme.oneSpaceColors`, mọi font từ `MaterialTheme.typography`.

```powershell
.\gradlew.bat verifyArchitecture
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=1
.\gradlew.bat :domain:test :core:session:testDebugUnitTest :data:auth:testDebugUnitTest --max-workers=1
# Network, tin offline-first, auth qua API giả, đồng bộ và migration Room 5→7
.\gradlew.bat :core:network:testDebugUnitTest :data:news:testDebugUnitTest :core:sync:testDebugUnitTest :app:testDebugUnitTest --tests "*DatabaseMigrationTest" --max-workers=1
# Server
.\gradlew.bat -p backend test
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
# Chụp màn hình sáng/tối và EN/VI ngay trên JVM (Robolectric), không cần emulator; ảnh ra app/build/screenshots
.\gradlew.bat :app:testDebugUnitTest --tests "*ScreenshotTest" --tests "*FloatingVideoTest" --max-workers=1
```

NavigationSmokeTest kiểm tra các tab, media, player overlay, settings/offline, bookmark, email/OTP và recreate trong luồng auth. Mạng cần cho sample streams; backend giả và fonts nằm trong app nên auth/tin tức không cần server.

**Bản release để demo**: `./gradlew :app:assembleRelease` (thêm `-Ponespace.apiBaseUrl=...` cho bản nối server). APK khoảng 6 MB (R8), ký bằng khoá debug để cài thẳng. Kịch bản, chuẩn bị và phương án dự phòng: [docs/demo.md](docs/demo.md).

## Kiểm tra nhanh trên máy (trước khi demo)

1. **Tin offline**: mở tab Tin → bật chế độ máy bay (hoặc "Mô phỏng mất mạng") → tắt hẳn app → mở lại: tin vẫn hiện.
2. **Nhạc nền**: phát một bài → về Home → khoá màn hình: nhạc vẫn phát, điều khiển được trên thông báo.
3. **Đăng nhập**: OTP 123456 hoặc mật khẩu ≥ 6 ký tự; thử mã sai, đăng xuất.
4. **2048**: chơi vài nước → tắt hẳn app → mở lại: ván cũ còn nguyên.
5. **Siêu playlist**: thêm một bài báo vào playlist có video và bài hát → Phát: bài báo được đọc rồi tự chuyển mục.
6. **Tải về**: tải một bài hát → bật chế độ máy bay → phát bài đó: vẫn nghe được.
7. **Nhắc nhở**: bật nhắc trong Tài khoản → Nhắc nhở (cho phép thông báo); để thử ngay, đổi giờ máy sang sau 20:00 khi chưa làm nhiệm vụ nào rồi chờ WorkManager chạy (hoặc dùng App Inspection → Background Task Inspector để chạy worker).
8. **Server thật**: chạy server (mục trên), cài app với `-Ponespace.apiBaseUrl=http://10.0.2.2:8080/v1/`, đăng nhập, chơi một game và tạo playlist → kiểm tra `GET /v1/me/points`, `/v1/me/playlists` (hoặc bảng `point_entries`, `playlists` trong PostgreSQL).

Font theo SIL OFL; giấy phép trong [core/designsystem/licenses](core/designsystem/licenses). Tham khảo: [Android architecture](https://developer.android.com/topic/architecture/recommendations), [typed navigation](https://developer.android.com/guide/navigation/design/type-safety), [Material 3](https://m3.material.io/).
