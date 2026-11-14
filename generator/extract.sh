#!/bin/bash
# Extracts the SwiftUI / SwiftUICore symbol graphs of the installed Xcode SDKs (iOS simulator + macOS).
# usage: generator/extract.sh [out-dir]   (default: generator/symbolgraph)
set -euo pipefail
OUT=${1:-"$(dirname "$0")/symbolgraph"}
mkdir -p "$OUT/ios" "$OUT/macos"
MAC=$(xcrun --show-sdk-path --sdk macosx)
IOS=$(xcrun --show-sdk-path --sdk iphonesimulator)
for m in SwiftUI SwiftUICore; do
  xcrun swift-symbolgraph-extract -module-name $m -target arm64-apple-macos27.0 -sdk "$MAC" -output-dir "$OUT/macos" \
    -minimum-access-level public -skip-synthesized-members
  xcrun swift-symbolgraph-extract -module-name $m -target arm64-apple-ios27.0-simulator -sdk "$IOS" -output-dir "$OUT/ios" \
    -minimum-access-level public -skip-synthesized-members
done
echo "$OUT"
