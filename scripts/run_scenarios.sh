#!/bin/bash
# Scenario benchmark (long lists, single-row change, animations): release builds, macOS and an iOS simulator.
# usage: scripts/run_scenarios.sh [extra app args, e.g. -sizes 1000,10000 -lazy]
# SIM selects the simulator (UDID or "booted"). Writes ../bench-results/scenarios/{ios,macos}/
set -euo pipefail
cd "$(dirname "$0")/.."
# PLATFORMS="ios macos" by default; SIM selects the simulator (UDID or "booted")
PLATFORMS=${PLATFORMS:-ios macos}
SIM=${SIM:-booted}
OUT="$(cd .. && pwd)/bench-results/scenarios"
mkdir -p "$OUT/ios" "$OUT/macos"
./gradlew :bench:shared:linkReleaseFrameworkMacosArm64 :bench:shared:linkReleaseFrameworkIosSimulatorArm64 --console=plain -q
SWIFT=(bench/app/*.swift bench/app/Screens/*.swift)
SWIFT_OPT=-O scripts/build_app.sh macos BenchRelease dev.nucleusframework.composeswiftui.bench.release bench/shared/build/bin/macosArm64/releaseFramework build/release-macos "${SWIFT[@]}" | tail -1
SWIFT_OPT=-O scripts/build_app.sh ios BenchRelease dev.nucleusframework.composeswiftui.bench.release bench/shared/build/bin/iosSimulatorArm64/releaseFramework build/release-ios "${SWIFT[@]}" | tail -1
if [[ " $PLATFORMS " == *" macos "* ]]; then
  echo "== macOS"
  build/release-macos/BenchRelease.app/Contents/MacOS/BenchRelease -runScenarios -out "$OUT/macos" "$@" | grep '\[scenario\]'
fi
echo "== iOS ($SIM)"
xcrun simctl install "$SIM" build/release-ios/BenchRelease.app
LOG=/tmp/compose-swiftui-scenarios-ios.log
xcrun simctl launch --console-pty --terminate-running-process "$SIM" dev.nucleusframework.composeswiftui.bench.release -runScenarios "$@" > "$LOG" 2>&1 &
PID=$!
for _ in $(seq 1 900); do grep -q 'DONE' "$LOG" && break; sleep 1; done
kill $PID 2>/dev/null || true
grep '\[scenario\]' "$LOG"
DATA=$(xcrun simctl get_app_container "$SIM" dev.nucleusframework.composeswiftui.bench.release data)
cp "$DATA"/Documents/scenarios/* "$OUT/ios/" 2>/dev/null || true
