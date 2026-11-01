#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
gen_manifest.py - build manifest.md for a OneSpace demo screenshot run.

Usage:
    python3 scripts/gen_manifest.py <screenshot-dir> [output-manifest.md]

Parses FR codes out of screenshot filenames (Maestro appends timestamps,
e.g. F-VID-08-floating-1730000000000.png) and emits a coverage table of all
69 functional requirements. Standard library only.
"""

import os
import re
import sys
from collections import defaultdict

# ----------------------------------------------------------------------------
# The 69 functional requirements (code, description)
# ----------------------------------------------------------------------------
FRS = [
    # Video
    ("VID", 1, "Xem video cơ bản"),
    ("VID", 2, "Feed theo sở thích"),
    ("VID", 3, "Tìm kiếm video"),
    ("VID", 4, "Watchlist (Xem sau)"),
    ("VID", 5, "Lịch sử + tiếp tục xem"),
    ("VID", 6, "Chất lượng (tự động/1080p/720p/360p)"),
    ("VID", 7, "Tải offline (v2, chưa có)"),
    ("VID", 8, "PIP cửa sổ nổi"),
    ("VID", 9, "Audio-only"),
    ("VID", 10, "Trích MP3"),
    ("VID", 11, "Chia sẻ link"),
    ("VID", 12, "Trang kênh nghệ sĩ"),
    ("VID", 13, "Playlist video"),
    ("VID", 14, "Tua nhanh + tốc độ"),
    ("VID", 15, "Thẻ quiz cuối video"),
    # Music
    ("MUS", 1, "Player đầy đủ"),
    ("MUS", 2, "Dock mini player"),
    ("MUS", 3, "Nhạc nền notification"),
    ("MUS", 4, "Playlist"),
    ("MUS", 5, "Queue"),
    ("MUS", 6, "Tìm nhạc"),
    ("MUS", 7, "Kho mp3 trích từ video"),
    ("MUS", 8, "Gợi ý"),
    ("MUS", 9, "Siêu playlist (v2)"),
    ("MUS", 10, "Audio focus xung đột"),
    ("MUS", 11, "Equalizer (v2)"),
    ("MUS", 12, "Tải offline (v2)"),
    # News
    ("NEWS", 1, "Feed chuyên mục"),
    ("NEWS", 2, "Trình đọc + cỡ chữ + tối"),
    ("NEWS", 3, "Lưu bài"),
    ("NEWS", 4, "Tìm"),
    ("NEWS", 5, "TTS nghe tin"),
    ("NEWS", 6, "Sắp xếp"),
    ("NEWS", 7, "Nghe nhạc khi đọc"),
    ("NEWS", 8, "Quiz từ bài báo"),
    ("NEWS", 9, "Bài liên quan"),
    ("NEWS", 10, "Thẻ đọc nổi (Đang đọc)"),
    # Games
    ("GAME", 1, "Hub game"),
    ("GAME", 2, "Đoán bài hát"),
    ("GAME", 3, "Quiz tin"),
    ("GAME", 4, "Rhythm (v2)"),
    ("GAME", 5, "Trivia"),
    ("GAME", 6, "Bảng xếp hạng"),
    ("GAME", 7, "Game + video nửa màn"),
    ("GAME", 8, "Điểm thưởng"),
    ("GAME", 9, "Memory match"),
    ("GAME", 10, "Đập chuột"),
    ("GAME", 11, "2048"),
    ("GAME", 12, "Snake"),
    ("GAME", 13, "Beat Tiles"),
    ("GAME", 14, "Flappy"),
    # Dùng chung
    ("COM", 1, "Tài khoản đăng nhập"),
    ("COM", 2, "Onboarding sở thích"),
    ("COM", 3, "Hồ sơ avatar"),
    ("COM", 4, "Hệ điểm XP"),
    ("COM", 5, "Nhiệm vụ hằng ngày"),
    ("COM", 6, "Streak"),
    ("COM", 7, "Thú cưng"),
    ("COM", 8, "Huy hiệu"),
    ("COM", 9, "Khung avatar"),
    ("COM", 10, "Wrapped tổng kết"),
    ("COM", 11, "Tìm xuyên nội dung"),
    ("COM", 12, "Nhắc nhiệm vụ"),
    ("COM", 13, "Cài đặt sáng/tối + ngôn ngữ + tiết kiệm dữ liệu"),
    ("COM", 14, "Popup xung đột âm thanh"),
    ("COM", 15, "Cửa hàng đổi quà"),
    ("COM", 16, "Ngoại tuyến"),
    ("COM", 17, "Báo nội dung"),
    ("COM", 18, "Chế độ khách"),
]

GROUP_NAMES = {
    "VID": "Video",
    "MUS": "Music",
    "NEWS": "News",
    "GAME": "Games",
    "COM": "Dùng chung",
}

# Known gaps: FRs that cannot be covered by screenshots in this build.
MISSING_REASONS = {
    ("VID", 7): "Tính năng v2 — chưa triển khai trong bản demo.",
    ("MUS", 3): "Bản demo chỉ có dock trong app, không tạo media notification (bước VERIFY trong flow 05).",
    ("MUS", 9): "Tính năng v2 — chưa triển khai trong bản demo.",
    ("MUS", 11): "Tính năng v2 — chưa triển khai trong bản demo.",
    ("MUS", 12): "Tính năng v2 — chưa triển khai trong bản demo.",
    ("NEWS", 6): "Không tìm thấy UI sắp xếp trong mã nguồn (chỉ có chip lọc chuyên mục).",
    ("GAME", 4): "Tính năng v2 — chưa triển khai trong bản demo (Beat Tiles thay thế).",
    ("GAME", 9): "Game không có trong danh mục demo (Tic-Tac-Toe là game suy luận/ghép của bản demo).",
    ("GAME", 10): "Game không có trong danh mục demo (chỉ 2048, Tic-Tac-Toe, Đoán bài hát, Quiz tin tức, Rắn săn mồi, Beat Tiles).",
    ("GAME", 14): "Game không có trong danh mục demo.",
    ("COM", 17): "Không tìm thấy UI báo nội dung trong mã nguồn — cần thêm tính năng trước khi chụp.",
}
DEFAULT_MISSING = "Thiếu ảnh cho mã này — kiểm tra flow tương ứng trong .maestro/."

FR_PATTERN = re.compile(r"F-(VID|MUS|NEWS|GAME|COM)-(\d{1,2})")


def collect(screenshot_dir):
    """Map (group, number) -> sorted list of screenshot filenames."""
    found = defaultdict(list)
    if not os.path.isdir(screenshot_dir):
        return found
    for entry in sorted(os.listdir(screenshot_dir)):
        if not entry.lower().endswith(".png"):
            continue
        for match in FR_PATTERN.finditer(entry):
            group, number = match.group(1), int(match.group(2))
            found[(group, number)].append(entry)
    return found


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)

    screenshot_dir = sys.argv[1]
    manifest_path = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        screenshot_dir, "manifest.md")

    found = collect(screenshot_dir)
    total_pngs = sum(len(v) for v in found.values())

    lines = []
    lines.append("# OneSpace — Demo manifest (FR coverage)")
    lines.append("")
    lines.append("- Thư mục ảnh: `{}`".format(screenshot_dir))
    lines.append("- Tổng số ảnh chứa mã FR: **{}**".format(total_pngs))
    lines.append("")

    covered = 0
    group_stats = defaultdict(lambda: [0, 0])  # group -> [covered, total]

    lines.append("| Nhóm | Mã | Nêu | Trạng thái | Ảnh chụp |")
    lines.append("|---|---|---|---|---|")
    for group, number, desc in FRS:
        code = "F-{}-{:02d}".format(group, number)
        shots = found.get((group, number))
        if shots:
            covered += 1
            status = "✅"
            shot_cell = "<br>".join("`{}`".format(s) for s in sorted(shots))
            group_stats[group][0] += 1
        else:
            status = "❌"
            shot_cell = MISSING_REASONS.get((group, number), DEFAULT_MISSING)
        group_stats[group][1] += 1
        lines.append("| {} | {} | {} | {} | {} |".format(
            GROUP_NAMES[group], code, desc, status, shot_cell))

    lines.append("")
    lines.append("## Tổng phủ")
    lines.append("")
    lines.append("| Nhóm | Đã phủ | Tổng |")
    lines.append("|---|---|---|")
    for group in ("VID", "MUS", "NEWS", "GAME", "COM"):
        c, t = group_stats[group]
        lines.append("| {} | {} | {} |".format(GROUP_NAMES[group], c, t))
    lines.append("| **Total** | **{}** | **{}** |".format(covered, len(FRS)))
    lines.append("")

    with open(manifest_path, "w", encoding="utf-8") as fh:
        fh.write("\n".join(lines) + "\n")

    print("Wrote {} ({} / {} FRs covered, {} tagged screenshots)".format(
        manifest_path, covered, len(FRS), total_pngs))


if __name__ == "__main__":
    main()
