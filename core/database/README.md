# Room infrastructure

Đã có ContentDatabase, video progress và watchlist, cùng DI cho data:video. Data repository map entity sang domain model; screens không truy cập DAO.

News/bookmark cache, game snapshot và music queue/position là kiến trúc đích ở [Section 4](../../docs/spec/section-4-technical-architecture.md), chưa phải schema đã triển khai. Chỉ lưu giá trị/ID/version/checkpoint, không lưu Player hoặc ViewModel. Khi đổi schema phải có migration và kiểm thử dữ liệu cũ.
