#!/bin/bash
# Typechecks the Swift renderer for iOS, macOS, tvOS and watchOS against runtime-only frameworks.
# usage: scripts/typecheck_swift.sh <core-framework-root>   (root/<platform>/BenchShared.framework, see CONTRIBUTING)
set -uo pipefail
cd "$(dirname "$0")/.."
FW=$1
FILES=(swift/ComposeSwiftUI/*.swift swift/ComposeSwiftUI/Generated/*.swift)
check() {
  xcrun swiftc -typecheck -swift-version 5 -target "$1" -sdk "$(xcrun --show-sdk-path --sdk "$2")" -F "$FW/$3" \
    -module-name ComposeSwiftUI "${FILES[@]}" > "/tmp/typecheck_$3.log" 2>&1
  echo "$3 rc=$?"
}
check arm64-apple-ios17.0-simulator iphonesimulator ios &
check arm64-apple-macos14.0 macosx macos &
check arm64-apple-tvos17.0-simulator appletvsimulator tvos &
check arm64-apple-watchos10.0-simulator watchsimulator watchos &
wait
grep -h "error:" /tmp/typecheck_*.log | sort -u | head -20
