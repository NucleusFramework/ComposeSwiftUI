#!/bin/bash
# Checks that the rich TextEditor codec keeps every native attribute across a Kotlin restyle, on macOS and in the
# iOS simulator (macOS 26 / iOS 26+).
# usage: scripts/check_rich_text.sh   (SIM=<udid> picks the simulator, PLATFORMS=macos skips iOS)
set -euo pipefail
cd "$(dirname "$0")/.."
PLATFORMS=${PLATFORMS:-"macos ios"}
OUT=$(mktemp -d)
SRC=scripts/rich_text_check/main.swift

for p in $PLATFORMS; do
  if [ "$p" = macos ]; then
    ./gradlew -q :runtime:linkDebugFrameworkMacosArm64 -Pcsui.coreOnly=true
    FW=runtime/build/bin/macosArm64/debugFramework
    scripts/build_app.sh macos RichTextCheck dev.local.richtextcheck "$FW" "$OUT/macos" "$SRC" > /dev/null
    echo -n "macOS: "
    DYLD_FRAMEWORK_PATH=$FW "$OUT/macos/RichTextCheck.app/Contents/MacOS/RichTextCheck"
  else
    ./gradlew -q :runtime:linkDebugFrameworkIosSimulatorArm64 -Pcsui.coreOnly=true
    FW=runtime/build/bin/iosSimulatorArm64/debugFramework
    scripts/build_app.sh ios RichTextCheck dev.local.richtextcheck "$FW" "$OUT/ios" "$SRC" > /dev/null
    APP="$OUT/ios/RichTextCheck.app"
    mkdir -p "$APP/Frameworks" && cp -R "$FW/BenchShared.framework" "$APP/Frameworks/"
    SIM=${SIM:-$(xcrun simctl list devices booted | grep -oE '[0-9A-F-]{36}' | head -1)}
    [ -n "$SIM" ] || { echo "iOS: no booted simulator (boot one or pass SIM=<udid>)"; exit 1; }
    xcrun simctl install "$SIM" "$APP"
    # simctl doesn't return the app's exit code: the result line decides
    RESULT=$(SIMCTL_CHILD_DYLD_FRAMEWORK_PATH="$APP/Frameworks" xcrun simctl launch --console-pty "$SIM" dev.local.richtextcheck 2>&1 \
      | grep -E "^(rich text round trip|FAIL|needs)" || true)
    echo "iOS: $RESULT"
    case "$RESULT" in "rich text round trip: OK"*) ;; *) exit 1 ;; esac
  fi
done
