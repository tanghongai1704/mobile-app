#!/usr/bin/env bash
# Generates small local media so the server streams it from /media (the CDN stand-in).
# Run from backend/. Usage: tools/make-sample-media.sh [MEDIA_DIR] [COUNT]   (needs ffmpeg, python3)
set -euo pipefail
OUT="${1:-media}"
COUNT="${2:-5}"
mkdir -p "$OUT/video" "$OUT/audio"
IDS=$(python3 -c '
import json, sys
n = int(sys.argv[1])
for kind, f in (("video", "videos.json"), ("audio", "tracks.json")):
    for item in json.load(open("src/main/resources/seed/" + f))[:n]:
        print(kind, item["id"])
' "$COUNT")
i=0
while read -r kind id; do
  i=$((i + 1))
  if [ "$kind" = video ]; then
    ffmpeg -nostdin -loglevel error -y -f lavfi -i "testsrc2=size=640x360:rate=25:duration=12" \
      -f lavfi -i "sine=frequency=$((300 + i * 40)):duration=12" -shortest \
      -c:v libx264 -pix_fmt yuv420p -preset veryfast -c:a aac -b:a 96k -movflags +faststart "$OUT/video/$id.mp4"
  else
    ffmpeg -nostdin -loglevel error -y -f lavfi -i "sine=frequency=$((220 + i * 30)):duration=30" \
      -af "volume=0.4" -c:a libmp3lame -b:a 96k "$OUT/audio/$id.mp3"
  fi
  echo "$OUT/$kind/$id"
done <<< "$IDS"
