# DataStore infrastructure

SessionPreferencesStore triển khai domain SessionPreferencesRepository và được Hilt bind. Đã persist theme, data saver, interests/onboarding, last section/track ID, conflict rule và demo auth metadata.

Email/guest mode được ghi atomic để phân biệt guest với signed-out. Hoàn tất onboarding không cấp guest session. C1 nhớ global trên thiết bị và reset được từ Settings. Đây là metadata demo, không phải token/provider xác thực thật.

Screens không gọi DataStore trực tiếp. Queue/position đầy đủ, article data và game snapshot thuộc structured persistence theo [Section 4](../../docs/spec/section-4-technical-architecture.md), không nhét model lớn vào preferences.
