#!/usr/bin/env python3
"""Writes docs/API.md: one line per generated Kotlin composable, modifier and value member."""
import glob, os, re, sys

root = sys.argv[1]
gen = os.path.join(root, "runtime/src/generated/kotlin/dev/composeswiftui")
out = ["# Compose SwiftUI — generated Kotlin API index", "",
       "Generated from the SwiftUI symbol graphs. Every entry is compiler-verified on iOS and macOS.", ""]

def collapse(text):
    return re.sub(r"\s+", " ", text).replace("( ", "(").replace(", )", ")")

views = open(os.path.join(gen, "GenViews.kt")).read()
out += ["## Views (@Composable)", "", "```kotlin"]
for m in re.finditer(r"public fun (\w+)\((.*?)\n\) \{", views, re.S):
    out.append(collapse("%s(%s)" % (m.group(1), m.group(2))))
for m in re.finditer(r"public fun (\w+View)\(([^)]*)\)", views):
    out.append(collapse("%s(%s)" % (m.group(1), m.group(2))))
hand = open(os.path.join(root, "runtime/src/extras/kotlin/dev/composeswiftui/Handwritten.kt")).read()
for m in re.finditer(r"public fun (ShapeView)\((.*?)\n\) \{", hand, re.S):
    out.append(collapse("%s(%s)" % (m.group(1), m.group(2))))
out += ["```", "", "## Modifiers", "", "```kotlin"]
mods = []
for f in sorted(glob.glob(os.path.join(gen, "GenModifiers*.kt"))):
    for m in re.finditer(r"public fun Modifier\.(`?\w+`?)\((.*?)\): Modifier =", open(f).read()):
        mods.append("Modifier.%s(%s)" % (m.group(1), re.sub(r"SValueCallback.*", "", m.group(2))))
mods.append("Modifier.toolbarItem(placement: ToolbarItemPlacement = ToolbarItemPlacement.automatic, content: @Composable () -> Unit)")
out += sorted(mods, key=str.lower) + ["```", "", "## Value types (static members, factories, constructors)", ""]
for f in sorted(glob.glob(os.path.join(gen, "GenTypes*.kt"))):
    stack = []
    for line in open(f):
        m = re.match(r"^(\s*)(?:@HiddenFromObjC )?public (class|interface|object) (\w+)", line)
        if m:
            depth = len(m.group(1)) // 4
            stack = stack[:depth] + [m.group(3)]
            out.append("- `%s` (%s)" % (".".join(stack), m.group(2)))
            continue
        m = re.match(r"^(\s*)public (val|fun) (`?\w+`?)(.*?)(?: get\(\)| =)", line)
        if m and stack:
            depth = len(m.group(1)) // 4
            out.append("  - `%s.%s%s`" % (".".join(stack[:max(1, depth - 1)] if False else stack), m.group(3), m.group(4) if m.group(2) == "fun" else ""))
        m = re.match(r"^\s*public constructor\((.*?)\) :", line)
        if m and stack:
            out.append("  - `%s(%s)`" % (".".join(stack), m.group(1)))
os.makedirs(os.path.join(root, "docs"), exist_ok=True)
open(os.path.join(root, "docs/API.md"), "w").write("\n".join(out) + "\n")
print(len(out), "lines")
