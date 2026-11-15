#!/bin/bash
# Release benchmark: Kotlin release frameworks + Swift -O, all screens, iOS simulator and macOS.
# Writes ../bench-results/full/{ios,macos}/ and ../bench-results/full/report.html
set -euo pipefail
cd "$(dirname "$0")/.."
# PLATFORMS="ios macos" by default; SIM selects the simulator (UDID or "booted")
PLATFORMS=${PLATFORMS:-ios macos}
SIM=${SIM:-booted}
OUT="$(cd .. && pwd)/bench-results/full"
mkdir -p "$OUT/ios" "$OUT/macos"
python3 -I scripts/gen_registry.py .
./gradlew :bench:shared:linkReleaseFrameworkMacosArm64 :bench:shared:linkReleaseFrameworkIosSimulatorArm64 --console=plain -q
SWIFT=(bench/app/*.swift bench/app/Screens/*.swift)
SWIFT_OPT=-O scripts/build_app.sh macos BenchRelease dev.nucleusframework.composeswiftui.bench.release bench/shared/build/bin/macosArm64/releaseFramework build/release-macos "${SWIFT[@]}" | tail -1
SWIFT_OPT=-O scripts/build_app.sh ios BenchRelease dev.nucleusframework.composeswiftui.bench.release bench/shared/build/bin/iosSimulatorArm64/releaseFramework build/release-ios "${SWIFT[@]}" | tail -1
if [[ " $PLATFORMS " == *" macos "* ]]; then
  build/release-macos/BenchRelease.app/Contents/MacOS/BenchRelease -runBench -out "$OUT/macos" | grep '\[bench\]'
fi
xcrun simctl install "$SIM" build/release-ios/BenchRelease.app
LOG=/tmp/compose-swiftui-full-ios.log
xcrun simctl launch --console-pty --terminate-running-process "$SIM" dev.nucleusframework.composeswiftui.bench.release -runBench > "$LOG" 2>&1 &
PID=$!
for _ in $(seq 1 900); do grep -q 'DONE' "$LOG" && break; sleep 1; done
kill $PID 2>/dev/null || true
grep '\[bench\]' "$LOG"
DATA=$(xcrun simctl get_app_container "$SIM" dev.nucleusframework.composeswiftui.bench.release data)
cp "$DATA"/Documents/bench/* "$OUT/ios/"
python3 -I scripts/report.py "$OUT"
