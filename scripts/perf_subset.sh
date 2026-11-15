#!/bin/bash
# Release perf check on a few screens: scripts/perf_subset.sh 1,6,16   (indices = screen numbers)
set -euo pipefail
cd "$(dirname "$0")/.."
ONLY=$1
bash -c './gradlew :bench:shared:linkReleaseFrameworkMacosArm64 :bench:shared:linkReleaseFrameworkIosSimulatorArm64 --console=plain -q' 2>&1 | grep -E '^e:' || true
SWIFT=(bench/app/*.swift bench/app/Screens/*.swift)
SWIFT_OPT=-O scripts/build_app.sh macos BenchRelease dev.nucleusframework.composeswiftui.bench.release bench/shared/build/bin/macosArm64/releaseFramework build/release-macos "${SWIFT[@]}" > /dev/null
SWIFT_OPT=-O scripts/build_app.sh ios BenchRelease dev.nucleusframework.composeswiftui.bench.release bench/shared/build/bin/iosSimulatorArm64/releaseFramework build/release-ios "${SWIFT[@]}" > /dev/null
build/release-macos/BenchRelease.app/Contents/MacOS/BenchRelease -runBench -only "$ONLY" -out /tmp/perf-mac | grep '\[bench\] [0-9]' | sed 's/^/macOS /'
xcrun simctl install booted build/release-ios/BenchRelease.app
LOG=/tmp/perf-ios.log
xcrun simctl launch --console-pty --terminate-running-process booted dev.nucleusframework.composeswiftui.bench.release -runBench -only "$ONLY" > "$LOG" 2>&1 &
PID=$!
for _ in $(seq 1 300); do grep -q DONE "$LOG" && break; sleep 1; done
kill $PID 2>/dev/null || true
grep '\[bench\] [0-9]' "$LOG" | sed 's/^/iOS   /'
