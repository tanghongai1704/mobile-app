# Section 4 — Kiến trúc kỹ thuật

Nguồn spec chính là [LaTeX Section 4](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/main.tex). Sửa trực tiếp các file dưới đây; không duy trì một bản nội dung thay thế riêng trong Markdown.

- [4.1 Ràng buộc mobile](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/mobile-constraints.tex)
- [4.2 Nền tảng và stack](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/platform-selection.tex)
- [4.3 Layer, module và navigation](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/layered-architecture.tex)
- [4.4 Lifecycle, ADR và trạng thái triển khai](https://github.com/nhinguyen140809/mobile-report/blob/duy_1.2_1.5_parts/sections/4-technical-architecture/lifecycle-state.tex)

[Bản đồ implementation](../architecture.md) giải thích các class tương ứng trong Android repository.

`mobile-report` là repository Git riêng; thay đổi báo cáo nằm trên nhánh `duy_1.2_1.5_parts`, dựa trên `origin/duy_1.2_1.5_parts`. Build bằng `python scripts/build.py` từ thư mục báo cáo với pdfLaTeX/latexmk và biber.
