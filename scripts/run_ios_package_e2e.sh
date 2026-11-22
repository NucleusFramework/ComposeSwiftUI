#!/bin/bash
# End-to-end test of the Swift package integration on iOS: assembles the package of e2e/shared (XCFramework +
# renderer), generates an Xcode app that depends on it (e2e/iosApp, script sandboxing on, no Gradle build phase), builds
# it for the simulator, runs it and reads its self-check.
# usage: scripts/run_ios_package_e2e.sh [--cocoapods] [simulator-udid]   (default: a booted iPhone)
#   --cocoapods: the same app through the generated podspec (e2e/iosPodApp) instead of the Swift package
set -euo pipefail
PODS=0
if [ "${1:-}" = "--cocoapods" ]; then PODS=1; shift; fi
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT=${E2E_OUT:-/tmp/csui-e2e}/ios
SIM=${1:-$(xcrun simctl list devices booted | grep -m1 -oE "iPhone[^(]*\(([0-9A-F-]{36})\)" | grep -oE "[0-9A-F-]{36}")}
[ -n "$SIM" ] || { echo "No booted iPhone simulator"; exit 1; }
mkdir -p "$OUT"

(cd "$ROOT/e2e" && ./gradlew :shared:assembleComposeSwiftUIPackage -q)
if [ $PODS = 1 ]; then
  NAME=E2EiOSPods
  (cd "$ROOT/e2e/iosPodApp" && xcodegen generate --quiet && pod install --silent)
  xcodebuild -workspace "$ROOT/e2e/iosPodApp/$NAME.xcworkspace" -scheme $NAME -configuration Debug -sdk iphonesimulator \
    -destination "id=$SIM" -derivedDataPath "$OUT/DerivedData" build > "$OUT/build.log" 2>&1 \
    || { grep -E "error:" "$OUT/build.log" | head -20; echo "FAIL xcodebuild"; exit 1; }
  echo "PASS xcodebuild through CocoaPods"
else
  NAME=E2EiOS
  (cd "$ROOT/e2e/iosApp" && xcodegen generate --quiet)
  grep -q "ENABLE_USER_SCRIPT_SANDBOXING = YES" "$ROOT/e2e/iosApp/$NAME.xcodeproj/project.pbxproj"
  if grep -q "shellScript" "$ROOT/e2e/iosApp/$NAME.xcodeproj/project.pbxproj"; then echo "FAIL the app has a script phase"; exit 1; fi
  xcodebuild -project "$ROOT/e2e/iosApp/$NAME.xcodeproj" -scheme $NAME -configuration Debug -sdk iphonesimulator \
    -destination "id=$SIM" -derivedDataPath "$OUT/DerivedData" build > "$OUT/build.log" 2>&1 \
    || { grep -E "error:" "$OUT/build.log" | head -20; echo "FAIL xcodebuild"; exit 1; }
  echo "PASS xcodebuild with ENABLE_USER_SCRIPT_SANDBOXING = YES, no build phase"
fi

APP="$OUT/DerivedData/Build/Products/Debug-iphonesimulator/$NAME.app"
xcrun simctl install "$SIM" "$APP"
xcrun simctl terminate "$SIM" dev.nucleusframework.composeswiftui.$NAME >/dev/null 2>&1 || true
xcrun simctl launch --console-pty "$SIM" dev.nucleusframework.composeswiftui.$NAME > "$OUT/run.log" 2>&1 &
for _ in $(seq 60); do grep -q "IOS-E2E DONE" "$OUT/run.log" && break; sleep 1; done
grep -E "IOS-E2E" "$OUT/run.log" || true
# What the user sees, for a look (the self-check above is what passes or fails)
xcrun simctl io "$SIM" screenshot "$OUT/screenshot.png" >/dev/null 2>&1 || true
xcrun simctl terminate "$SIM" dev.nucleusframework.composeswiftui.$NAME >/dev/null 2>&1 || true
grep -q "IOS-E2E DONE" "$OUT/run.log" && ! grep -q "IOS-E2E FAIL" "$OUT/run.log" && echo "iOS PACKAGE E2E OK" || { echo "iOS PACKAGE E2E FAILED"; exit 1; }
