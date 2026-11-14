#!/bin/bash
# Builds a SwiftUI app bundle linking the Kotlin framework, without an Xcode project.
# usage: build_app.sh <ios|macos> <AppName> <bundle-id> <kotlin-framework-dir> <out-dir> <swift files...>
set -euo pipefail
PLATFORM=$1; NAME=$2; BUNDLE_ID=$3; FW=$4; OUT=$5; shift 5
OPT=${SWIFT_OPT:--Onone}
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
RENDERER=("$ROOT"/swift/ComposeSwiftUI/*.swift "$ROOT"/swift/ComposeSwiftUI/Generated/*.swift)
if [ "$PLATFORM" = ios ]; then
  TARGET=arm64-apple-ios17.0-simulator; SDK=$(xcrun --show-sdk-path --sdk iphonesimulator)
  APP="$OUT/$NAME.app"; BIN="$APP/$NAME"; PLIST="$APP/Info.plist"; mkdir -p "$APP"
  EXTRA_PLIST='<key>UILaunchScreen</key><dict/><key>UIApplicationSceneManifest</key><dict><key>UIApplicationSupportsMultipleScenes</key><true/></dict><key>MinimumOSVersion</key><string>17.0</string><key>UIDeviceFamily</key><array><integer>1</integer><integer>2</integer></array>'
else
  TARGET=arm64-apple-macos14.0; SDK=$(xcrun --show-sdk-path --sdk macosx)
  APP="$OUT/$NAME.app"; mkdir -p "$APP/Contents/MacOS"; BIN="$APP/Contents/MacOS/$NAME"; PLIST="$APP/Contents/Info.plist"
  EXTRA_PLIST='<key>LSMinimumSystemVersion</key><string>14.0</string><key>NSPrincipalClass</key><string>NSApplication</string>'
fi
cat > "$PLIST" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>$BUNDLE_ID</string>
<key>CFBundleExecutable</key><string>$NAME</string>
<key>CFBundleName</key><string>$NAME</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleShortVersionString</key><string>1.0</string>
<key>CFBundleVersion</key><string>1</string>
$EXTRA_PLIST
</dict></plist>
PL
rm -f "$BIN"
set +e
xcrun swiftc -parse-as-library -swift-version 5 $OPT -target $TARGET -sdk "$SDK" -module-name "$NAME" \
  -F "$FW" -framework BenchShared -lc++ -Xlinker -framework -Xlinker Foundation \
  "${RENDERER[@]}" "$@" -o "$BIN" > /tmp/build_app_$$.log 2>&1
RC=$?
set -e
grep -E ' error' /tmp/build_app_$$.log || true
if [ $RC -ne 0 ] || [ ! -f "$BIN" ]; then echo "BUILD FAILED"; exit 1; fi
codesign --force --sign - "$APP" >/dev/null 2>&1
echo "$APP"
