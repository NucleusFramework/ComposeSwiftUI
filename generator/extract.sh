#!/bin/bash
# Extracts the SwiftUI / SwiftUICore symbol graphs of the installed Xcode SDKs.
# usage: generator/extract.sh [out-dir]   (default: generator/symbolgraph)
set -euo pipefail
OUT=${1:-"$(dirname "$0")/symbolgraph"}
extract() { # platform sdk target
  mkdir -p "$OUT/$1"
  local SDK; SDK=$(xcrun --show-sdk-path --sdk "$2")
  for m in SwiftUI SwiftUICore; do
    xcrun swift-symbolgraph-extract -module-name $m -target "$3" -sdk "$SDK" -output-dir "$OUT/$1" \
      -minimum-access-level public -skip-synthesized-members
  done
}
extract macos macosx arm64-apple-macos27.0
extract ios iphonesimulator arm64-apple-ios27.0-simulator
extract tvos appletvsimulator arm64-apple-tvos27.0-simulator
extract watchos watchsimulator arm64-apple-watchos27.0-simulator
echo "$OUT"
