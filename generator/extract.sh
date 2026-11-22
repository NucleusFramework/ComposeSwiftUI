#!/bin/bash
# Extracts the SwiftUI / SwiftUICore symbol graphs of the installed Xcode SDKs.
# usage: generator/extract.sh [out-dir]   (default: generator/symbolgraph)
set -euo pipefail
OUT=${1:-"$(dirname "$0")/symbolgraph"}
extract() { # platform sdk target-prefix target-suffix (the SDK's own version goes between them)
  mkdir -p "$OUT/$1"
  local SDK; SDK=$(xcrun --show-sdk-path --sdk "$2")
  set -- "$1" "$2" "$3$(xcrun --show-sdk-version --sdk "$2")$4"
  for m in SwiftUI SwiftUICore; do
    xcrun swift-symbolgraph-extract -module-name $m -target "$3" -sdk "$SDK" -output-dir "$OUT/$1" \
      -minimum-access-level public -skip-synthesized-members
  done
  # SwiftUI overlays of other frameworks (MapKit, StoreKit…) and Charts: classified for the coverage report
  mkdir -p "$OUT/overlays/$1"
  local OVERLAYS; OVERLAYS=$(ls "$SDK/System/Library/Frameworks" | sed -n 's/^\(_.*_SwiftUI\)\.framework$/\1/p')
  for m in $OVERLAYS Charts; do
    [ -d "$SDK/System/Library/Frameworks/$m.framework" ] || continue
    xcrun swift-symbolgraph-extract -module-name "$m" -target "$3" -sdk "$SDK" -output-dir "$OUT/overlays/$1" \
      -minimum-access-level public -skip-synthesized-members 2>/dev/null || true
  done
}
extract macos macosx arm64-apple-macos ""
extract ios iphonesimulator arm64-apple-ios -simulator
extract tvos appletvsimulator arm64-apple-tvos -simulator
extract watchos watchsimulator arm64-apple-watchos -simulator
echo "$OUT"
