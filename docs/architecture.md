# Bản đồ kiến trúc OneSpace

**Nguồn chốt thiết kế:** [LaTeX Section 4 - Kiến trúc kỹ thuật](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/main.tex). File đó mô tả ranh giới, quyết định, lifecycle, audio policy và ma trận phần đã có/phần còn thiếu. File này hướng dẫn đọc implementation; khi code chưa đáp ứng một yêu cầu đích, ghi gap thay vì coi yêu cầu đã xong.

## Quyết định tổ chức

Ba layer Presentation–Domain–Data dùng MVVM/UDF; có 23 Gradle module leaf nằm ngoài app. Không có nguồn feature/core/domain/data đặt dưới app/src.

- **app:** Activity/Application, shell, native splash startup, root flow, route execution và composition root Hilt.
- **feature:home/video/music/news/games/account/onboarding/auth:** screens, UI state, ViewModel, typed routes và graph declarations.
- **domain:** Kotlin/JVM thuần; package theo auth/video/music/news/games/session. Models, contracts, policies và use cases cần thiết.
- **data:video/music/news/games/auth:** repository implementations, mapping và source orchestration. Chỉ app chọn factory trả domain interface.
- **core:common/designsystem/session:** shared presentation types, design tokens/components và coordinator cho phiên nội dung.
- **core:media/database/datastore/network/sync:** Android/media/storage adapters; network/sync vẫn là extension point. VideoSurface là phần hiển thị platform trong media adapter.

Core là nhóm module theo vai trò, không phải layer thứ tư. Feature không import feature khác hoặc data implementation; Domain không phụ thuộc Android/framework/implementation. Xem matrix dependency đầy đủ ở Section 4.3.2.

## Navigation hiện tại

Một NavController và một root NavHost được tạo tại app shell.

| File/thành phần | Trách nhiệm |
| --- | --- |
| app/navigation/AppRoute.kt | Typed Root/Splash/Main |
| app/navigation/AppNavigation.kt | Khai báo root, ghép graphs, quan sát auth/onboarding đã persist |
| app/navigation/OneSpaceAppState.kt | Thực thi navigate/pop, detail, tab stack, settings và OTP |
| app/navigation/Destinations.kt | Tab metadata và context title theo typed route |
| feature/*/navigation/*Route.kt | Contract graph/feed/detail, argument nhỏ có kiểu dữ liệu |
| feature/*/navigation/*NavGraph.kt | Destination bindings, state collection, VM scope và UI callbacks |

Feature graph không nhận NavController/NavHostController. App cung cấp callback cụ thể như onOpenVideo(id), onOpenArticle(id), onVerifyEmail(email), onBack(). Vì app là nơi ghép flow nên app biết các typed route public cần thiết; feature không biết route của feature khác.

Catalog Video/Music/News/Games và Auth chia sẻ ViewModel theo parent graph. App cấp getGraphEntry callback, feature lấy owner trong remember(entry) rồi gọi hiltViewModel(owner). Provider này chỉ phục vụ scope, không cấp quyền navigate. Settings dùng destination-scoped ViewModel; onboarding giữ state theo destination của flow.

Luồng:
- Startup đọc preferences/session thật → onboarding, auth hoặc main.
- Hoàn tất onboarding chỉ lưu sở thích/onboarding, không tự cấp guest.
- Guest/OTP thành công ghi session → root quan sát rồi vào Main.
- Sign-out ghi session → root dừng phiên phát và về Auth.
- Root anchor giữ ổn định; child back stack bị bỏ khi đổi auth flow.
- Năm tab dùng saveState/restoreState/launchSingleTop.
- Music Next/Previous giữ nguyên destination, đọc track từ runtime playback và không tích lũy màn NowPlaying.
- ViewModel không giữ callback NavController qua coroutine, nên recreate ngay lúc sign-out không điều hướng vào Activity cũ.

## Luồng nghiệp vụ và state

Screens → UI callback/ViewModel → Domain use case/policy → Domain contract → Data/Android adapter → Flow/StateFlow → UI.

SessionCoordinator là Activity-scoped ViewModel adapter trong core:session, khác authentication session. Nó không điều hướng và không gọi feature ViewModel khác. SessionPolicy là luật Kotlin thuần. MediaSessionPort là contract. MediaPlaybackCoordinator thực thi engine và phát playback truth. Floating/dock trong app nhận state; không tự tạo player.

AuthRepository có demo implementation ở data:auth; request/verify/guest/sign-out đi qua use cases. DataStore ghi email/guest mode atomically. Email null khác với signed-out nhờ guestMode. Auth thật/provider chưa chọn; mã 123456 không gửi email thật và không xác minh danh tính server.

## Runtime đã có và giới hạn

- Metadata mock; video/MP3 sample streams phát thật bằng Media3.
- Hai engine dùng chung: video và audio, không tạo lại theo screen.
- Room lưu video progress/watchlist; checkpoint video định kỳ hiện là 3 giây.
- DataStore lưu settings, interests, last section/track ID và phiên demo.
- C1 có ba lựa chọn, nhớ global trên thiết bị, reset ở Settings.
- Native splash, themed shell, auth/onboarding và graph navigation có baseline.
- News bookmark còn trong RAM; chưa phải durable/offline-first.
- Game mới board preview; chưa có gameplay/snapshot.
- Chưa có MediaSessionService, notification/lock-screen controls, đầy đủ audio focus/noisy cho video, real network sync, TTS/conversion/mixed queue.
- Chọn tab không đổi playback; floating còn trên Music, dock còn trên Games. Nội dung chừa khoảng trống theo chiều cao floating thực tế.
- Playback snapshot tách playing thực tế, playRequested, status, suppressed và error. SessionState không giữ cờ playing trùng lặp.
- Domain tạo audio plans; adapter thực thi. Video mute không tạo C1 conflict.
- Runtime queue do Media3 adapter sở hữu; UI Next/Previous dùng queue đó, auto-advance dùng cùng playlist repeat-all.
- Đóng media/new intent vô hiệu request cũ còn chờ catalog/preferences.
- Recreate UI test không chứng minh OS process death, lock-screen service, RAM 3 GB hoặc battery profile.

## Kiểm chứng và quy trình thay đổi

[Hồ sơ baseline Section 4 ngày 24/10/2026](verification/section-4-baseline.md): build/architecture check, 35 unit tests, 12 emulator tests và kiểm tra render LaTeX.

Chạy từ root:

~~~powershell
./gradlew.bat verifyArchitecture
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=1
./gradlew.bat :domain:test :core:session:testDebugUnitTest :data:auth:testDebugUnitTest
./gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
~~~

app:check phụ thuộc verifyArchitecture. Guardrail kiểm tra import/module dependencies và feature controller exposure; compiler và runtime tests kiểm tra phần còn lại.

Khi thêm feature:
1. Thêm module presentation và model/contract/use case/data adapter cần thực tế.
2. Feature khai báo Route/NavGraph, không import feature khác.
3. App đăng ký DI/graph và bind navigation callbacks.
4. Nếu thay audio/lifecycle/persistence policy, cập nhật ADR/acceptance criteria tương ứng trước.
5. Chạy architecture check và test chịu ảnh hưởng. Chưa cần thêm screen/service/manager rỗng để giữ chỗ.

Build dùng Gradle daemon JDK 25 và bytecode JVM 11. Cần full JDK có jlink; JRE do editor đi kèm không đủ cho Android JdkImageTransform. Đường dẫn SDK/JDK của từng máy không được commit. Nếu auto-detect chọn nhầm JRE, đặt JAVA_HOME/Gradle JDK về full JDK rồi restart daemon; có thể giới hạn installations paths trong cấu hình local/lệnh chạy.

## Tài liệu liên quan

- [Section 4 và các đối chiếu với mục 2.2/3.1](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/main.tex)
- [Tổng quan project](../README.md)
- Android: [architecture](https://developer.android.com/topic/architecture/recommendations), [modularization](https://developer.android.com/topic/modularization/patterns), [nested graphs](https://developer.android.com/guide/navigation/design/nested-graphs), [background playback](https://developer.android.com/media/media3/session/background-playback).
