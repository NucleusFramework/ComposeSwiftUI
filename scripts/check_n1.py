#!/usr/bin/env python3
"""
Checks that the Swift renderer still builds with the previous Xcode (N-1): every use of an API of the newest SDKs
(an `#available` / `@available` naming version NEWEST or later) sits inside an `#if compiler(>=COMPILER)` or
`#if canImport(SwiftUI, _version: ...)` region, so the Swift compiler of Xcode N-1 never sees it.

usage: scripts/check_n1.py [files...]   (default: the renderer and the integrations)
"""
import glob, os, re, sys

NEWEST = 27          # major version of the SDKs of the newest Xcode
COMPILER = "6.4"     # its Swift compiler (keep in sync with NEWEST_COMPILER in generator/generate.py)
SWIFTUI_MIN_VER = (8, 0)  # SwiftUI module version in SDK 27.0 (keep in sync with NEWEST_SWIFTUI_VERSION in generator/generate.py)
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AVAILABLE = re.compile(r"[#@]available\(([^)]*)\)")
VERSION = re.compile(r"\b(?:iOS|macOS|tvOS|watchOS|visionOS)\s+(\d+)")
CAN_IMPORT_VER = re.compile(r"canImport\(\s*SwiftUI\s*,\s*_version\s*:\s*([0-9]+(?:\.[0-9]+)*)\s*\)")


def newest(line):
    return any(int(v) >= NEWEST for m in AVAILABLE.finditer(line) for v in VERSION.findall(m.group(1)))


def has_newest_guard(line):
    compact = line.replace(" ", "")
    if ("compiler(>=%s)" % COMPILER) in compact:
        return True
    for m in CAN_IMPORT_VER.finditer(line):
        ver = tuple(int(p) for p in m.group(1).split("."))
        if ver >= SWIFTUI_MIN_VER:
            return True
    return False


def check(path):
    errors, stack = [], []  # stack: one bool per open #if: does it (or an enclosing one) require the new SDK/compiler?
    for n, line in enumerate(open(path, encoding="utf-8"), 1):
        s = line.strip()
        if s.startswith("#if "):
            stack.append(has_newest_guard(s) or (bool(stack) and stack[-1]))
        elif s.startswith("#elseif"):
            parent = bool(len(stack) > 1) and stack[-2]
            stack[-1] = has_newest_guard(s) or parent
        elif s.startswith("#else"):
            # an #else branch of a compiler/SDK check is what N-1 compiles
            stack[-1] = bool(len(stack) > 1) and stack[-2]
        elif s.startswith("#endif"):
            stack.pop()
        elif newest(line) and not (stack and stack[-1]):
            errors.append(
                "%s:%d: SDK %d API outside #if canImport(SwiftUI, _version: 8.0) / #if compiler(>=%s): %s"
                % (os.path.relpath(path, ROOT), n, NEWEST, COMPILER, s)
            )
    return errors


files = sys.argv[1:] or sorted(glob.glob(os.path.join(ROOT, "swift", "ComposeSwiftUI", "**", "*.swift"), recursive=True)
                                + glob.glob(os.path.join(ROOT, "swift", "ComposeSwiftUIIntegrations", "*.swift")))
errors = [e for f in files for e in check(f)]
print(
    "\n".join(errors)
    or "ok: %d files build with Xcode N-1 (no SDK %d API outside #if canImport(SwiftUI, _version: 8.0) / #if compiler(>=%s))"
    % (len(files), NEWEST, COMPILER)
)
sys.exit(1 if errors else 0)
