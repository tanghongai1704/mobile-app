# Architecture review — OneSpace

Ngày rà soát: 20/10/2026. Phạm vi: skeleton và navigation prototype; chưa đánh giá media runtime hoặc hiệu năng.

## Kết luận

Cấu trúc phù hợp cho MVP gồm bốn phân hệ dùng chung session: feature modules độc lập, domain thuần Kotlin, repository implementation tách khỏi UI, app làm composition root. Ranh giới Gradle kiểm soát chiều dependency; đây không còn là chia package trong app.

Đây là quyết định thiết kế dựa trên hướng dẫn Android, không phải cấu trúc duy nhất Android bắt buộc. Domain/use case là tùy chọn theo độ phức tạp; dự án giữ domain vì yêu cầu ba lớp và quy tắc liên feature.

## Những điểm đã sửa

| Vấn đề trước | Điều chỉnh hiện tại |
| --- | --- |
| Core/domain/features đều ở app | Chuyển ra Gradle module riêng ở root |
| App tạo catalog ViewModel và giữ bookmark | Feature graph sở hữu VM; News repository sở hữu bookmark |
| App chọn bài nhạc kế tiếp | SelectNextTrack ở domain; Music điều phối màn Now Playing |
| DI thủ công | Hilt Application/Activity, HiltViewModel và KSP; bindings ở app |
| Repository trả List đồng bộ | Contract phát Flow, VM chuyển thành lifecycle-aware StateFlow |
| Quy tắc session trong Android ViewModel | SessionPolicy thuần Kotlin; coordinator là state/saved-state adapter |
| Public screens/VMs và mock classes | Feature công khai navigation API; presentation và implementation internal |

## Đối chiếu hướng dẫn Android

| Hướng dẫn | Áp dụng trong OneSpace | Đánh giá |
| --- | --- | --- |
| UI/data tách biệt, truy cập qua repository | UI → VM → use case/contract; data implementation ở module riêng | Đã áp dụng |
| UDF, immutable UI state | Callbacks vào VM; read-only StateFlow ra UI | Đã áp dụng |
| Thu thập state theo lifecycle | Feature routes và shell dùng collectAsStateWithLifecycle | Đã áp dụng |
| ViewModel theo screen/graph | Catalog/detail dùng VM scope theo feature graph | Đã áp dụng |
| Module ít phụ thuộc, rõ trách nhiệm | Không có feature → feature hoặc feature → data | Đã áp dụng |
| App làm entry point/root navigation | Application, Activity, Hilt wiring, shell, NavHost ở app | Đã áp dụng |
| Navigation truyền ID thay model | Detail routes dùng ID; screen composable nhận callbacks | Đã áp dụng |
| Domain cho rule phức tạp/tái sử dụng | SessionPolicy và queue rule thuần Kotlin | Quyết định dự án |
| Phục hồi state đúng tầng | SavedStateHandle/UI state hiện tại; persisted repository dự kiến | Chưa có persistence thật |
| Playback owner ngoài UI lifecycle | Có boundary core:media; chưa có controller/service | Chờ triển khai |

Nguồn tham chiếu:

- [Recommendations for Android architecture](https://developer.android.com/topic/architecture/recommendations): UDF, repository, StateFlow, lifecycle collection và scope ViewModel.
- [Common modularization patterns](https://developer.android.com/topic/modularization/patterns?hl=en): module types, dependency inversion, giao tiếp qua navigation.
- [Domain layer](https://developer.android.com/topic/architecture/domain-layer): domain tùy chọn, use case tái sử dụng.
- [ViewModel scoping APIs](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-apis): owner và scope theo graph.
- [UI layer](https://developer.android.com/topic/architecture/ui-layer): state holder, UI logic và business logic.
- [Hilt Gradle setup](https://dagger.dev/hilt/gradle-setup) và [Dagger KSP](https://dagger.dev/dev-guide/ksp.html): cấu hình DI/code generation.

## Ownership theo nghiệp vụ

| Phân hệ | Presentation | Domain | Data |
| --- | --- | --- | --- |
| Video | Feed/detail, args, controls gửi shared session | Video, repository contract, GetVideo | Catalog mock; metadata/progress/cache sau |
| Music | Catalog/Now Playing, controls và route bài mới | Track, contract, GetMusic, SelectNextTrack | Catalog mock; queue/download metadata sau |
| News | Feed/reader, saved filter, bookmark intent | Article, contract, GetNews, ObserveSavedArticles, ToggleBookmark | Catalog, in-memory bookmark source of truth |
| Games | Catalog/board preview, pause/resume intent | Game, contract, GetGames | Catalog mock; gameplay/save game sau |
| Shared session | Global players, lifecycle bridge, coordinator adapter | Audio exclusivity, tab/background policy | SavedStateHandle cho UI; durable repository chưa có |

Không để ViewModel gọi ViewModel khác. App nối graph và callbacks liên feature. Shell tạo một coordinator bằng Hilt, feature nhận cùng instance; không tạo coordinator mới theo graph.

Settings chỉ là UI preference preview nên giữ saveable state trong destination. Khi có DataStore, thêm contract/use case; không đọc DataStore trực tiếp trong composable.

## Chiều dependency

```text
app → feature:*, data:*, domain, core:designsystem, core:session
feature:video/music/news/games → domain, core:common, core:designsystem, core:session
feature:home/account → domain, core:designsystem
data:* → domain
core:session → domain
core:designsystem → core:common
domain → Kotlin/coroutines only
```

Data trả public factory có kiểu repository interface; mock class internal. App chọn binding/scope, không chứa cách tải/lưu dữ liệu. Core media/network/database/datastore/sync là extension points; chỉ nối từ data/runtime khi thực sự sử dụng.

## Trade-offs

- Một domain module, chia package theo nghiệp vụ. Chưa tách domain thành nhiều api/impl modules vì MVP còn nhỏ.
- UI và graph chung feature module; data tách riêng để compiler ngăn UI gọi implementation.
- Có 20 leaf modules; vài core hạ tầng là skeleton. Có chi phí Gradle/configuration để đổi lấy cấu trúc quản lý riêng. Chỉ thêm module khi có trách nhiệm rõ ràng.
- Giữ Navigation Compose 2 theo baseline. Tài liệu Android hiện đề xuất Navigation 3 cho app mới; việc chuyển Nav3 là ADR riêng, không nằm trong lần tách module này.
- Repository mock scope Singleton giữ bookmark qua đổi tab/recreation, không qua process death.
- Coordinator là ViewModel của shell, chỉ mô phỏng UI. Background audio thật phải do MediaSessionService/controller sở hữu; UI quan sát/gửi lệnh tới runtime, không dùng VM thay service.

## Điều chưa thể kết luận từ skeleton

Chưa có Media3/Audio Focus, background music/notification, decoder dùng chung, persisted session, offline cache/download, game loop/save hoặc retry mạng thật. UI có play/pause/floating không chứng minh các cơ chế đó đã hoạt động.

Tests kiểm tra policy Kotlin, queue và saved-state adapter; navigation cần thiết bị. Kiểm thử process death, service lifecycle, pin/RAM/decoder và mất mạng thực hiện sau khi có runtime.

## Quy tắc triển khai tiếp

1. Rule thay đổi hành vi sản phẩm ở domain; truy cập dữ liệu ở repository; navigation/layout ở feature/shell.
2. Không thêm Context/Activity/Player vào domain hoặc feature ViewModel; platform adapter ở data/core.
3. Feature mới đăng ký graph và Hilt bindings; không import feature cũ để phối hợp.
4. Persisted state có một source of truth trong repository; không giữ thêm bản bookmark/game/queue tại app.
5. Test nghiệp vụ JVM trước, adapter/lifecycle bằng Android tests; không suy diễn từ mock UI.
