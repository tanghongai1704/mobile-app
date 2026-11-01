#!/usr/bin/env bash
# ============================================================================
# run_demo.sh - OneSpace automated demo + screenshot run (Maestro)
#
# Single entry point. Does ALL the live work:
#   1. Export SDK paths (adb + emulator)
#   2. Ensure Maestro is installed (brew, fallback to get.maestro.mobile.dev)
#   3. Ensure a device is running (boot Pixel_10_API37 if none)
#   4. Ensure the app is installed (gradlew :app:installDebug if missing)
#   5. Fresh output dir demo-run/<stamp>/ (previous demo-run/ wiped first)
#   6. adb shell pm clear com.example.onespace (fresh first-run state)
#   7. Run every .maestro/[0-9][0-9]-*.yaml in numeric order
#   8. Collect screenshots into demo-run/<stamp>/screenshots/
#   9. Generate demo-run/<stamp>/manifest.md (scripts/gen_manifest.py)
#  10. Print a summary (screenshot count + FR coverage)
#
# Usage:   scripts/run_demo.sh
# Requires: Android SDK at $HOME/Library/Android/sdk, python3, network access.
# ============================================================================
set -uo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ID="com.example.onespace"
AVD_NAME="Pixel_10_API37"
SDK_ROOT="$HOME/Library/Android/sdk"
export PATH="$SDK_ROOT/platform-tools:$SDK_ROOT/emulator:$PATH"

log() { printf '\n\033[1;34m[run_demo]\033[0m %s\n' "$*"; }
die() { printf '\n\033[1;31m[run_demo] ERROR:\033[0m %s\n' "$*" >&2; exit 1; }

command -v adb >/dev/null 2>&1 || die "adb not found under $SDK_ROOT/platform-tools"

# ---------------------------------------------------------------------------
# 1-2. Maestro: install on first run
# IMPORTANT: the official installer is the curl script. Do NOT use
# `brew install maestro`: on this machine that resolves to an unrelated
# Electron cask, not the mobile.dev CLI. The brew formula is only a fallback
# and is validated afterwards (a plain semver from `maestro --version`).
# ---------------------------------------------------------------------------
maestro_ok() { command -v maestro >/dev/null 2>&1; }

if ! maestro_ok; then
  log "Maestro not found - installing (this happens only once)..."
  if curl -fsSL "https://get.maestro.mobile.dev" | bash; then
    export PATH="$HOME/.maestro/bin:$PATH"
  elif command -v brew >/dev/null 2>&1; then
    brew install maestro || true
  fi
fi
if ! maestro_ok && [ -x "$HOME/.maestro/bin/maestro" ]; then
  export PATH="$HOME/.maestro/bin:$PATH"
fi
maestro_ok || die "Maestro CLI not found on PATH. Install it with: curl -fsSL \"https://get.maestro.mobile.dev\" | bash"
# Sanity check: a real mobile.dev CLI prints a plain semver (e.g. 2.12.0).
MAESTRO_VERSION="$(maestro --version 2>/dev/null | grep -E '^[0-9]+\.[0-9]+' | tail -1 || true)"
[ -n "$MAESTRO_VERSION" ] || die "'maestro' on PATH is not the mobile.dev CLI (wrong binary installed?)."
log "Maestro: $MAESTRO_VERSION"

# ---------------------------------------------------------------------------
# 3. Device: use an existing emulator/device, or boot the AVD
# ---------------------------------------------------------------------------
device_serial() { adb devices | awk 'NR>1 && $2=="device" {print $1; exit}'; }

if [ -z "$(device_serial)" ]; then
  log "No device online - booting emulator '$AVD_NAME' (cold boot, no snapshot)..."
  "$SDK_ROOT/emulator/emulator" -avd "$AVD_NAME" -no-snapshot-load >/dev/null 2>&1 &
  EMU_PID=$!
  adb wait-for-device
  log "Waiting for sys.boot_completed (up to 5 min)..."
  boot_ok=""
  for _ in $(seq 1 150); do
    if [ "$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; then
      boot_ok=1; break
    fi
    sleep 2
  done
  [ -n "$boot_ok" ] || { kill "$EMU_PID" 2>/dev/null; die "Emulator did not finish booting."; }
  sleep 5   # let the launcher settle
fi
log "Device: $(device_serial)"

# ---------------------------------------------------------------------------
# 4. App: install the debug build if the package is missing
# ---------------------------------------------------------------------------
if ! adb shell pm list packages 2>/dev/null | tr -d '\r' | grep -q "package:$APP_ID$"; then
  log "App not installed - building + installing :app:installDebug..."
  ( cd "$REPO_ROOT" && ./gradlew :app:installDebug ) \
    || die "Failed to install $APP_ID. Build it manually (./gradlew :app:installDebug) and re-run."
fi
log "App installed: $APP_ID"

# ---------------------------------------------------------------------------
# 5. Fresh output dir; wipe the previous demo-run first (re-shoot every run)
# ---------------------------------------------------------------------------
STAMP="$(date +%d-%m-%Y_%H%M)"
RUN_DIR="$REPO_ROOT/demo-run/$STAMP"
rm -rf "$REPO_ROOT/demo-run"
mkdir -p "$RUN_DIR/maestro-out" "$RUN_DIR/screenshots"
log "Output dir: $RUN_DIR"

# Marker used later to find screenshots Maestro saved into its default location
MARKER="$RUN_DIR/.marker"
touch "$MARKER"

# ---------------------------------------------------------------------------
# 6. Fresh first-run state (clears app data + permissions)
# ---------------------------------------------------------------------------
log "Clearing app data (pm clear $APP_ID)..."
adb shell pm clear "$APP_ID" >/dev/null || die "pm clear failed"

# ---------------------------------------------------------------------------
# 7. Run all flows in numeric order; keep going when one flow fails
#    (Maestro 2.x ignores --output and writes to ~/.maestro/tests/<stamp>/)
# ---------------------------------------------------------------------------
FAILED_FLOWS=()
for flow in "$REPO_ROOT"/.maestro/[0-9][0-9]-*.yaml; do
  [ -e "$flow" ] || die "No .maestro/[0-9][0-9]-*.yaml flows found."
  name="$(basename "$flow" .yaml)"
  log "Running flow: $name"
  if maestro test "$flow"; then
    echo "  OK: $name"
  else
    echo "  FAILED: $name (continuing with the remaining flows)"
    FAILED_FLOWS+=("$name")
  fi
done

# ---------------------------------------------------------------------------
# 8. Collect screenshots
#    Maestro 2.x saves them under ~/.maestro/tests/<stamp>/<flow>/takeScreenshot/
#    regardless of --output; grab every PNG newer than the marker, and also
#    honour --output dirs if a future Maestro version writes them.
# ---------------------------------------------------------------------------
SHOTS_DIR="$RUN_DIR/screenshots"
find "$RUN_DIR/maestro-out" -type f -name '*.png' -exec cp -f {} "$SHOTS_DIR/" \; 2>/dev/null
find "$HOME/.maestro/tests" -type f -name '*.png' -newer "$MARKER" -exec cp -f {} "$SHOTS_DIR/" \; 2>/dev/null

SHOT_COUNT="$(ls -1 "$SHOTS_DIR" 2>/dev/null | wc -l | tr -d ' ')"
log "Collected $SHOT_COUNT screenshot(s) into $SHOTS_DIR"

# ---------------------------------------------------------------------------
# 9. Manifest with FR coverage
# ---------------------------------------------------------------------------
MANIFEST="$RUN_DIR/manifest.md"
if command -v python3 >/dev/null 2>&1; then
  python3 "$REPO_ROOT/scripts/gen_manifest.py" "$SHOTS_DIR" "$MANIFEST" || true
else
  echo "python3 not found - manifest not generated" > "$MANIFEST"
fi

# ---------------------------------------------------------------------------
# 10. Summary
# ---------------------------------------------------------------------------
log "================ DEMO RUN SUMMARY ================"
echo "Run dir:        $RUN_DIR"
echo "Screenshots:    $SHOT_COUNT"
if [ -f "$MANIFEST" ]; then
  echo "Manifest:       $MANIFEST"
  grep -E '^Tổng phủ|^Total' "$MANIFEST" || true
  echo "--- coverage per group ---"
  grep -E '^\| (Video|Music|News|Games|Dùng chung|Total)' "$MANIFEST" || true
fi
if [ "${#FAILED_FLOWS[@]}" -gt 0 ]; then
  echo "Failed flows:   ${FAILED_FLOWS[*]}"
  echo "Re-run a single failed flow later with:"
  echo "  maestro test $REPO_ROOT/.maestro/<flow>.yaml"
else
  echo "Failed flows:   (none)"
fi
echo "Open $RUN_DIR/manifest.md to review the 69-FR coverage table."
