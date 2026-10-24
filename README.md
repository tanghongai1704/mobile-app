# OneSpace

Android Compose baseline cho Video, Music, News và Games. App shell, feature, domain, data và core là Gradle module riêng. Metadata hiện dùng mock.

## Cấu trúc

```text
app/                    Activity, Application, shell, root NavHost, Hilt bindings
domain/                 Pure Kotlin models, contracts, use cases, policies
data/
  auth/                 Demo OTP authentication repository
  video/ music/         Catalogs; Room viewing progress
  news/ games/          Mock catalogs/bookmarks/board metadata
feature/
  auth/                 Email → OTP → guest/authenticated session
  onboarding/           Welcome → multitasking → interests
  home/ video/ music/ news/ games/ account/
    navigation/         Typed Route + NavGraph
    presentation/       State, ViewModel, screens
core/
  designsystem/         Material 3 colors, bundled fonts, shapes, components
  common/               Shared UI types and formatting
  session/              Cross-feature playback coordinator
  media/                Media3 engine and video surface
  datastore/            Settings and demo session
  database/             Room progress/watchlist
  network/ sync/        Reserved extension points
```

Không có dependency feature → feature/data. Screens nhận state/callback, graph sở hữu route/ViewModel; app chọn repository qua Hilt. [Bản đồ implementation](docs/architecture.md). **Nguồn chốt kiến trúc cho bài:** [LaTeX Section 4](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/main.tex).

## Navigation và auth

System splash → đọc phiên → onboarding / auth / main. Main có năm nested tab graphs giữ back stack và Account graph riêng. Mọi route dùng Kotlin Serialization và Navigation Compose typed API. Feature khai báo graph và phát callbacks; app/OneSpaceAppState thực thi điều hướng. Catalog/auth dùng graph-scoped ViewModel qua graph-entry provider, feature không nhận NavController.

Auth là **demo local**: nhập email hợp lệ → OTP **123456**. Mã hết hạn sau 5 phút, năm lần sai cần gửi lại. Không gửi email, chưa có backend/Firebase. Guest mode được lưu trên thiết bị. Đăng xuất trở lại Auth và giữ interests.

## Chức năng

Material 3 light/dark theme, teal/lavender, Manrope headings + Roboto body đóng gói offline. Có onboarding, login/verify, Home, catalog/detail và settings.

Video/nhạc dùng ExoPlayer phát sample stream thật, có controls, floating video, music dock và conflict policy. Room lưu progress/watchlist; DataStore lưu settings/phiên. Game là board preview, news bookmarks còn trong RAM. Chưa có remote sync, TTS/conversion, gamification hay background playback service.

## Build

Mở root bằng Android Studio, Sync Gradle, chọn run configuration **OneSpace**. Emulator nằm trong **View → Tool Windows → Running Devices**. minSdk 26, compile/targetSdk 37; daemon full JDK 25 (có jlink), bytecode JVM 11. Nếu editor chọn nhầm JRE, đặt JAVA_HOME/Gradle JDK về full JDK và restart daemon; không commit đường dẫn cục bộ.

```powershell
.\gradlew.bat verifyArchitecture
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=1
.\gradlew.bat :domain:test :core:session:testDebugUnitTest :data:auth:testDebugUnitTest --max-workers=1
.\gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
```

NavigationSmokeTest kiểm tra các tab, media, player overlay, settings/offline, bookmark, email/OTP và recreate trong luồng auth. Mạng cần cho sample streams; demo auth và fonts chạy offline.

Font theo SIL OFL; giấy phép trong [core/designsystem/licenses](core/designsystem/licenses). Tham khảo: [Android architecture](https://developer.android.com/topic/architecture/recommendations), [typed navigation](https://developer.android.com/guide/navigation/design/type-safety), [Material 3](https://m3.material.io/).
