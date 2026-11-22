#!/bin/bash
# End-to-end tests of the runtime + Swift renderer + Gradle plugin, in a real SwiftUI window on this Mac.
# Builds e2e/shared (with the Gradle plugin, which also writes the renderer), builds the app twice (debug and
# release Swift flags), runs every scenario, then the process-death phase, and checks the unified log.
# usage: scripts/run_e2e.sh [scenario,...]     (E2E_ONLY filter, e.g. scripts/run_e2e.sh callbacks,lifecycle)
set -uo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT=${E2E_OUT:-/tmp/csui-e2e}
export E2E_ONLY=${1:-${E2E_ONLY:-}}
[ -z "$E2E_ONLY" ] && unset E2E_ONLY
mkdir -p "$OUT"

(cd "$ROOT/e2e" && ./gradlew :shared:linkDebugFrameworkMacosArm64 :shared:syncComposeSwiftUIRenderer :shared:assembleMacosArm64MainResources :shared:assembleComposeSwiftUIPackage -q) || { echo "GRADLE BUILD FAILED"; exit 1; }
FW="$ROOT/e2e/shared/build/bin/macosArm64/debugFramework"
RENDERER="$ROOT/e2e/shared/build/renderer/ComposeSwiftUI"
RESOURCES="$ROOT/e2e/shared/build/generated/compose/resourceGenerator/assembledResources/macosArm64Main"

build() {  # build <name> <extra swiftc flags...>
  local name=$1; shift
  local app="$OUT/$name.app"
  rm -rf "$app"; mkdir -p "$app/Contents/MacOS" "$app/Contents/Resources"
  cat > "$app/Contents/Info.plist" <<PL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>dev.nucleusframework.composeswiftui.$name</string>
<key>CFBundleExecutable</key><string>$name</string>
<key>CFBundleName</key><string>$name</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>LSMinimumSystemVersion</key><string>14.0</string>
<key>NSPrincipalClass</key><string>NSApplication</string>
</dict></plist>
PL
  cp -R "$RESOURCES" "$app/Contents/Resources/compose-resources"
  xcrun swiftc -parse-as-library -swift-version 5 -target arm64-apple-macos14.0 -sdk "$(xcrun --show-sdk-path --sdk macosx)" \
    -module-name "$name" -F "$FW" -framework E2EShared -lc++ -Xlinker -framework -Xlinker Foundation "$@" \
    $(find "$RENDERER" "$RENDERER/../ComposeSwiftUIIntegrations" -name "*.swift") "$ROOT"/e2e/app/*.swift -o "$app/Contents/MacOS/$name" > "$OUT/$name.build.log" 2>&1 \
    || { grep -E "error" "$OUT/$name.build.log" | head -20; echo "SWIFT BUILD FAILED ($name)"; exit 1; }
  codesign --force --sign - "$app" >/dev/null 2>&1
}

run() {  # run <name> [env...]
  local name=$1; shift
  env "$@" "$OUT/$name.app/Contents/MacOS/$name" -ApplePersistenceIgnoreState YES 2>&1 | grep -E "^(PASS|FAIL|==|E2E:)"
  return ${PIPESTATUS[0]}
}

build E2EDebug -Onone -D DEBUG
build E2ERelease -O
STATUS=0
START=$(date "+%Y-%m-%d %H:%M:%S")
echo "### debug build"; run E2EDebug || STATUS=1
if [ -z "${E2E_ONLY:-}" ] || [[ ",$E2E_ONLY," == *",saveable,"* ]]; then
  echo "### process death (new process restores the saved state)"
  run E2EDebug E2E_PHASE=2 || STATUS=1
fi
echo "### release build"; run E2ERelease || STATUS=1

if [ -z "${E2E_ONLY:-}" ] || [[ ",$E2E_ONLY," == *",callbacks,"* ]]; then
  echo "### unified log (os_log)"
  sleep 1
  if /usr/bin/log show --last 5m --predicate 'subsystem == "dev.nucleusframework.composeswiftui"' --style compact 2>/dev/null | grep -c "action boom" | grep -qv "^0$"; then
    echo "PASS errors reach os_log"
  else
    echo "FAIL errors reach os_log"; STATUS=1
  fi
fi
if [ -z "${E2E_ONLY:-}" ]; then
  echo "### XCTest: renderer, snapshots, memory (through the generated Swift package)"
  if swift test --package-path "$ROOT/e2e/RendererTests" > "$OUT/xctest.log" 2>&1; then
    grep -E "Executed [0-9]+ tests" "$OUT/xctest.log" | tail -1; echo "PASS renderer XCTests"
  else
    grep -E "error:|failed" "$OUT/xctest.log" | head -20; echo "FAIL renderer XCTests"; STATUS=1
  fi
fi
[ $STATUS -eq 0 ] && echo "E2E OK" || echo "E2E FAILED"
exit $STATUS
