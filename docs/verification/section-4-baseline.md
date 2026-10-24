# Kiểm chứng baseline Section 4 — 24/10/2026

Phạm vi: working tree Android và nguồn báo cáo trên nhánh duy_1.2_1.5_parts. Hồ sơ này ghi kết quả của đợt kiểm chứng, không khẳng định các revision sau tự động còn pass.

## Kết quả

| Kiểm tra | Kết quả |
| --- | --- |
| verifyArchitecture | Pass: module dependencies, imports, feature navigation callbacks |
| assembleDebug + assembleDebugAndroidTest | Pass |
| Unit: Domain | 15 pass |
| Unit: core:session | 14 pass |
| Unit: data:auth | 5 pass |
| Unit: app (test mặc định) | 1 pass |
| Emulator API 35, x86_64, APK debug | 12 pass: 9 navigation/UI, 2 Media3 contracts, 1 test context mặc định |
| LaTeX: pdfLaTeX + biber/latexmk | Pass; tham chiếu/citation được resolve |
| Render Section 4 | Đã xem bảng, sơ đồ, cây thư mục và phần lifecycle; không còn overflow/đường nối đè nội dung trong Section 4 |

## Hành vi đã đối chiếu

- Chọn Music catalog không tự sửa trạng thái playback; floating video và controls vẫn hiện.
- Engine snapshot là nguồn của identity/playing/position. SessionState không nhân bản cờ playing.
- C1 do Domain tạo plan, adapter thực thi; video mute không tranh nguồn tiếng với music.
- Next/Previous chọn từ runtime queue, kể cả khi danh sách đó khác catalog. Native auto-advance dùng cùng playlist.
- Next/Previous không thêm màn NowPlaying vào back stack; controls chỉ bật khi queue sẵn sàng.
- playing thực tế tách playRequested, BUFFERING/READY/ENDED/ERROR và suppression. Có thể pause lúc buffering và retry khi lỗi.
- Đóng media hoặc background hủy lệnh play cũ còn chờ dữ liệu.
- Auth/onboarding/guest/sign-out, recreate, tab/detail, bookmark trong phiên và settings có regression test.

Test nguồn: [SessionPolicyTest](../../domain/src/test/kotlin/com/example/onespace/domain/session/SessionPolicyTest.kt), [SessionCoordinatorTest](../../core/session/src/test/java/com/example/onespace/SessionCoordinatorTest.kt), [MediaPlaybackContractTest](../../app/src/androidTest/java/com/example/onespace/MediaPlaybackContractTest.kt), [NavigationSmokeTest](../../app/src/androidTest/java/com/example/onespace/NavigationSmokeTest.kt).

## Cách chạy lại

Dùng full JDK 25 có jlink, Android SDK và emulator phù hợp:

~~~powershell
./gradlew.bat verifyArchitecture :domain:test :core:session:testDebugUnitTest :data:auth:testDebugUnitTest :app:testDebugUnitTest --max-workers=1
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest --max-workers=1
./gradlew.bat :app:connectedDebugAndroidTest --max-workers=1
~~~

Media3 contract tests dùng WAV cục bộ và đường dẫn không tồn tại để kiểm tra auto-advance/error độc lập mạng. Test phát sample stream vẫn cần mạng. Trên API 35 với targetSdk 35+, fixture playback mở Activity foreground để đáp ứng audio focus, không tắt cơ chế audio focus của production.

Log cục bộ được giữ trong build/section4-completion-final.log, build/section4-emulator-fixes-build.log, build/section4-emulator-final.txt và build/section4-latex-final.log; build outputs không commit.

## Giới hạn

Chưa chứng minh service/lock-screen/background music, mọi tình huống call/noisy, OS process death restore đầy đủ, offline News durable, gameplay/game snapshot, adaptive bitrate hoặc ngân sách RAM/pin/startup. Activity recreate không thay thế process death. Các mục này vẫn là phần triển khai/đo kiểm tiếp theo trong Section 4.
