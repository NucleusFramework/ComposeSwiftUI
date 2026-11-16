#!/bin/bash
# Checks that the rich TextEditor codec keeps every native attribute across a Kotlin restyle (macOS 26+).
# usage: scripts/check_rich_text.sh   (builds the runtime macOS framework first)
set -euo pipefail
cd "$(dirname "$0")/.."
./gradlew -q :runtime:linkDebugFrameworkMacosArm64 -Pcsui.coreOnly=true
OUT=$(mktemp -d)
scripts/build_app.sh macos RichTextCheck dev.local.richtextcheck runtime/build/bin/macosArm64/debugFramework "$OUT" \
  scripts/rich_text_check/main.swift > /dev/null
DYLD_FRAMEWORK_PATH=runtime/build/bin/macosArm64/debugFramework "$OUT/RichTextCheck.app/Contents/MacOS/RichTextCheck"
