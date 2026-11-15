#!/usr/bin/env python3
"""
Integrates staged screens, builds, and runs the pixel-identity + perf bench on macOS and iOS.

usage: scripts/check_screens.py 7 8 9        (screen numbers)
Staged files: bench/staging/ScreenNN.kt and bench/staging/ScreenNNView.swift
A global lock serializes concurrent callers. If the build fails, the staged screens are
removed from the tree again (the tree always stays buildable) and the errors are printed.
Screenshots: ../bench-results/check/{macos,ios}/screenNN-{swiftui,compose,diff}.png
"""
import fcntl, glob, os, re, shutil, subprocess, sys, time

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
KT = os.path.join(ROOT, "bench/shared/src/commonMain/kotlin/bench/screens")
SW = os.path.join(ROOT, "bench/app/Screens")
STAGE = os.path.join(ROOT, "bench/staging")
OUT = os.path.abspath(os.path.join(ROOT, "..", "bench-results", "check"))


def run(cmd, **kw):
    return subprocess.run(cmd, cwd=ROOT, capture_output=True, text=True, **kw)


def main():
    nums = [int(x) for x in sys.argv[1:]]
    lock = open("/tmp/compose-swiftui-check.lock", "w")
    print("waiting for lock...", flush=True)
    fcntl.flock(lock, fcntl.LOCK_EX)
    print("lock acquired", flush=True)
    backups = {}
    for n in nums:
        for src, dst in ((os.path.join(STAGE, "Screen%02d.kt" % n), os.path.join(KT, "Screen%02d.kt" % n)),
                         (os.path.join(STAGE, "Screen%02dView.swift" % n), os.path.join(SW, "Screen%02dView.swift" % n))):
            if not os.path.exists(src):
                sys.exit("missing staged file %s" % src)
            backups[dst] = open(dst).read() if os.path.exists(dst) else None
            shutil.copy(src, dst)

    def revert(reason, output):
        for dst, content in backups.items():
            if content is None:
                os.remove(dst)
            else:
                open(dst, "w").write(content)
        run([sys.executable, "scripts/gen_registry.py", "."])
        print("FAILED (%s) — staged screens removed from the tree again.\n" % reason)
        print(output[-12000:])
        sys.exit(1)

    print(run([sys.executable, "scripts/gen_registry.py", "."]).stdout.strip())
    t = time.time()
    g = run(["./gradlew", ":bench:shared:linkDebugFrameworkMacosArm64", ":bench:shared:linkDebugFrameworkIosSimulatorArm64",
             "--console=plain", "-q"])
    if g.returncode != 0:
        errs = "\n".join(l for l in (g.stdout + g.stderr).splitlines() if l.startswith("e:")) or g.stdout + g.stderr
        revert("kotlin", errs)
    print("kotlin ok (%.0fs)" % (time.time() - t), flush=True)
    swift_files = sorted(glob.glob(os.path.join(ROOT, "bench/app/*.swift")) + glob.glob(os.path.join(SW, "*.swift")))
    for plat, fw in (("macos", "macosArm64"), ("ios", "iosSimulatorArm64")):
        b = run(["scripts/build_app.sh", plat, "Bench", "dev.nucleusframework.composeswiftui.bench",
                 "bench/shared/build/bin/%s/debugFramework" % fw, "build/" + plat] + swift_files)
        if b.returncode != 0 or not b.stdout.strip().endswith("Bench.app"):
            errs = "\n".join(l for l in b.stdout.splitlines() if " error" in l) or b.stdout
            revert("swift " + plat, errs)
    print("swift ok", flush=True)
    numbers = re.search(r"numbers: \[Int\] = \[(.*?)\]", open(os.path.join(ROOT, "bench/app/SwiftRegistry.swift")).read()).group(1)
    order = [int(x) for x in numbers.split(",") if x.strip()]
    only = ",".join(str(order.index(n) + 1) for n in nums)
    os.makedirs(os.path.join(OUT, "macos"), exist_ok=True)
    m = run(["build/macos/Bench.app/Contents/MacOS/Bench", "-runBench", "-only", only, "-out", os.path.join(OUT, "macos")])
    print("\n".join("macOS " + l for l in m.stdout.splitlines() if "[bench]" in l and "DONE" not in l))
    run(["xcrun", "simctl", "install", "booted", "build/ios/Bench.app"])
    log = "/tmp/compose-swiftui-ios-bench.log"
    with open(log, "w") as f:
        p = subprocess.Popen(["xcrun", "simctl", "launch", "--console-pty", "--terminate-running-process", "booted",
                              "dev.nucleusframework.composeswiftui.bench", "-runBench", "-only", only], stdout=f, stderr=subprocess.STDOUT, cwd=ROOT)
    deadline = time.time() + 60 + 30 * len(nums)
    while time.time() < deadline and "DONE" not in open(log).read():
        time.sleep(1)
    p.kill()
    print("\n".join("iOS   " + l for l in open(log).read().splitlines() if "[bench]" in l and "DONE" not in l))
    data = run(["xcrun", "simctl", "get_app_container", "booted", "dev.nucleusframework.composeswiftui.bench", "data"]).stdout.strip()
    os.makedirs(os.path.join(OUT, "ios"), exist_ok=True)
    for f in glob.glob(os.path.join(data, "Documents/bench/*.png")):
        shutil.copy(f, os.path.join(OUT, "ios"))
    print("screenshots in", OUT)


if __name__ == "__main__":
    main()
