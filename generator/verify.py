#!/usr/bin/env python3
"""
Compiler-verifies every generated API. Loop:
  generate -> swiftc -typecheck (iOS simulator + macOS) -> map errors to items -> reject -> repeat
and, with --kotlin, the same for the Kotlin bindings (compileKotlinMacosArm64).

Usage: verify.py <symbolgraph-root> <project-root> <core-framework-root> [--kotlin]
  <core-framework-root>/<platform>/BenchShared.framework for each platform to typecheck
  (built with `./gradlew :runtime:linkDebugFramework<Target> -Pcsui.coreOnly=true`).
"""

import glob
import json
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def sdk(name):
    return subprocess.check_output(["xcrun", "--show-sdk-path", "--sdk", name], text=True).strip()


def generate(sg, project):
    subprocess.check_call([sys.executable, "-I", os.path.join(HERE, "generate.py"), sg, project])


def load_linemap(project):
    lm = json.load(open(os.path.join(project, "generator", "linemap.json")))
    return {os.path.realpath(path): ranges for path, ranges in lm}


def lookup(linemap, path, line):
    for start, end, key in linemap.get(os.path.realpath(path), []):
        if start <= line <= end:
            return key
    return None


def swift_errors(project, target, sdk_name, fw):
    files = sorted(glob.glob(os.path.join(project, "swift", "ComposeSwiftUI", "*.swift")) +
                   glob.glob(os.path.join(project, "swift", "ComposeSwiftUI", "Generated", "*.swift")) +
                   glob.glob(os.path.join(project, "swift", "ComposeSwiftUIIntegrations", "*.swift")))
    cmd = ["xcrun", "swiftc", "-typecheck", "-swift-version", "5", "-target", target, "-sdk", sdk(sdk_name),
           "-F", fw, "-module-name", "ComposeSwiftUI"] + files
    p = subprocess.run(cmd, capture_output=True, text=True)
    errs = []
    for line in (p.stdout + p.stderr).splitlines():
        m = re.match(r"^(/.+?\.swift):(\d+):(\d+): error: (.*)$", line)
        if m is None:
            m = re.match(r"^([^ ].+?\.swift):(\d+):(\d+): error: (.*)$", line)
        if m:
            errs.append((m.group(1), int(m.group(2)), m.group(4)))
    return errs, p.returncode


def kotlin_errors(project):
    p = subprocess.run(["./gradlew", ":runtime:compileKotlinMacosArm64", ":bench:shared:linkDebugFrameworkMacosArm64", "--console=plain", "-q"],
                       cwd=project, capture_output=True, text=True)
    errs = []
    for line in (p.stdout + p.stderr).splitlines():
        m = re.match(r"^e: file://(/.+?\.kt):(\d+):(\d+) (.*)$", line)
        if m:
            from urllib.parse import unquote
            errs.append((unquote(m.group(1)), int(m.group(2)), m.group(4)))
    return errs, p.returncode, p.stdout + p.stderr


def reject(project, errs, label):
    rej_path = os.path.join(project, "generator", "rejected.json")
    rejected = json.load(open(rej_path)) if os.path.exists(rej_path) else {}
    linemap = load_linemap(project)
    unmapped = []
    added = 0
    for path, line, msg in errs:
        key = lookup(linemap, path, line)
        if key is None:
            unmapped.append((path, line, msg))
            continue
        if key not in rejected:
            rejected[key] = "%s: %s" % (label, msg)
            added += 1
    with open(rej_path, "w") as f:
        json.dump(rejected, f, indent=1, sort_keys=True)
    return added, unmapped


def main():
    sg, project, fw_root = [os.path.abspath(x) for x in sys.argv[1:4]]
    with_kotlin = "--kotlin" in sys.argv
    known = [("macos", "arm64-apple-macos14.0", "macosx"),
             ("ios", "arm64-apple-ios17.0-simulator", "iphonesimulator"),
             ("tvos", "arm64-apple-tvos17.0-simulator", "appletvsimulator"),
             ("watchos", "arm64-apple-watchos10.0-simulator", "watchsimulator")]
    targets = [(t, sdk_name, os.path.join(fw_root, plat), "swift-" + plat) for plat, t, sdk_name in known
               if os.path.isdir(os.path.join(fw_root, plat, "BenchShared.framework")) and os.path.isdir(os.path.join(sg, plat))]
    print("typechecking for:", ", ".join(t[3] for t in targets))
    for iteration in range(80):
        generate(sg, project)
        total = 0
        for target, sdk_name, fw, label in targets:
            errs, rc = swift_errors(project, target, sdk_name, fw)
            added, unmapped = reject(project, errs, label)
            print("[%d] %s: %d errors, %d newly rejected, %d unmapped" % (iteration, label, len(errs), added, len(unmapped)))
            for u in unmapped[:15]:
                print("    UNMAPPED", u)
            if unmapped and not added:
                sys.exit(1)
            total += added
            if added:
                break  # regenerate before checking the other platform
        if total:
            continue
        if with_kotlin:
            errs, rc, out = kotlin_errors(project)
            added, unmapped = reject(project, errs, "kotlin")
            print("[%d] kotlin: %d errors, %d newly rejected, %d unmapped (rc=%d)" % (iteration, len(errs), added, len(unmapped), rc))
            for u in unmapped[:15]:
                print("    UNMAPPED", u)
            if rc != 0 and not added:
                print(out[-4000:])
                sys.exit(1)
            if added:
                continue
        print("verified: generated code compiles for all targets")
        return
    sys.exit("did not converge")


if __name__ == "__main__":
    main()
