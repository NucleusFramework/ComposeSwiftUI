#!/usr/bin/env python3
"""
Compiles the Kotlin snippets of the documentation (docs/guide/*.md) against the runtime, so every example stays valid.

usage: scripts/check_docs.py [docs/guide/xx.md ...]   (all guide pages by default)

Each ```kotlin block becomes Kotlin code in package docsamples.<page>: a block with top-level declarations (fun, class,
object…) is copied as is, any other block is wrapped in a @Composable function. Blocks fenced as ```kotlin skip, Gradle
scripts (```kts) and Swift are not compiled. Errors are reported against the Markdown lines.
"""
import fcntl, glob, os, re, shutil, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "build", "docsamples", str(os.getpid()))  # one folder per run: runs may overlap
DEFAULT_IMPORTS = ["androidx.compose.runtime.*", "dev.nucleusframework.composeswiftui.*"]
# A block declaring functions or types is top-level code; one with only statements (val/var included) is a body.
TOP_LEVEL = re.compile(r"^(@\w+|fun |class |data |enum |object |sealed |interface |private |internal |public |typealias |abstract |open |suspend |const )")

files = sys.argv[1:] or sorted(glob.glob(os.path.join(ROOT, "docs", "guide", "*.md")))
shutil.rmtree(OUT, ignore_errors=True)
os.makedirs(OUT)
origin = {}  # (generated file, line) -> (markdown file, line)
count = 0
for md in files:
    page = "p_" + re.sub(r"\W", "_", os.path.splitext(os.path.basename(md))[0])
    lines = open(md).read().split("\n")
    imports, body = set(DEFAULT_IMPORTS), []  # body: (text, md line or None)
    i = 0
    while i < len(lines):
        fence = re.match(r"^\s*```(\w*)\s*(\w*)", lines[i])
        if not fence or not lines[i].strip().startswith("```"):
            i += 1
            continue
        lang, flag = fence.group(1), fence.group(2)
        start, block = i + 1, []
        i += 1
        while i < len(lines) and not lines[i].strip().startswith("```"):
            block.append((lines[i], i + 1)); i += 1
        i += 1
        if lang not in ("kotlin", "kt") or flag == "skip":
            continue
        count += 1
        code = [(t, n) for t, n in block if not t.startswith("import ") or imports.add(t[7:].strip())]
        if any(TOP_LEVEL.match(t) for t, _ in code):
            body += code + [("", None)]
        else:
            body += [("@Composable", None), ("private fun sample%d() {" % count, None)] + [("    " + t, n) for t, n in code] + [("}", None), ("", None)]
    if not body:
        continue
    header = ["@file:Suppress(\"UNUSED_VARIABLE\", \"UNUSED_PARAMETER\", \"unused\", \"UNUSED_EXPRESSION\")",
              "@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)", "", "package docsamples.%s" % page, ""] + \
             ["import %s" % x for x in sorted(imports)] + [""]
    gen = os.path.join(OUT, page + ".kt")
    with open(gen, "w") as f:
        f.write("\n".join(header + [t for t, _ in body]) + "\n")
    for k, (_, n) in enumerate(body):
        if n:
            origin[(gen, len(header) + k + 1)] = (os.path.relpath(md, ROOT), n)

print("%d snippets from %d pages" % (count, len(files)))
os.makedirs(os.path.join(ROOT, "build"), exist_ok=True)
lock = open(os.path.join(ROOT, "build", "docsamples.lock"), "w")
fcntl.flock(lock, fcntl.LOCK_EX)  # one Gradle compile at a time
r = subprocess.run([os.path.join(ROOT, "gradlew"), "-q", ":bench:shared:compileKotlinMacosArm64", "-PdocSamples=" + OUT],
                   cwd=ROOT, capture_output=True, text=True)
errors = 0
for l in (r.stdout + r.stderr).split("\n"):
    m = re.match(r"^e: file://(.+?):(\d+):(\d+) (.*)", l)
    if m:
        errors += 1
        where = origin.get((m.group(1), int(m.group(2))))
        print("%s: %s" % ("%s:%d" % where if where else "%s:%s" % (os.path.relpath(m.group(1), ROOT), m.group(2)), m.group(4)))
    elif l.startswith("e: "):
        errors += 1
        print(l)
if r.returncode != 0 and not errors:
    print((r.stdout + r.stderr)[-3000:])
shutil.rmtree(OUT, ignore_errors=True)
print("ok" if r.returncode == 0 else "%d errors" % errors)
sys.exit(r.returncode)
