#!/usr/bin/env python3
"""Lists the symbols of the newest SDK that the previous Xcode's SDK lacks although they are available on older OS
versions (back-deployed APIs, like `GraphicsContext.Filter.alphaMultiply` declared for iOS 18 but new in SDK 27).
The generator puts them behind `#if compiler(>=…)` like the APIs introduced in the newest SDK, so the renderer still
builds with Xcode N-1.

usage: generator/n1_missing.py <newest-symbol-graphs> <n1-symbol-graphs>   (both from generator/extract.sh; CI keeps
       the N-1 ones as the `symbolgraphs-n1` artifact) — writes generator/n1_missing.json
"""
import glob
import json
import os
import re
import sys

from generate import NEWEST_SDK

BUILDER = re.compile(r"@\w+Builder(<[^>]*>)? ")
DOMAIN = {"ios": "iOS", "macos": "macOS", "tvos": "tvOS", "watchos": "watchOS"}


def version(s):
    return (s.get("major", 0), s.get("minor", 0))


def symbols(root, plat):
    """usr -> (path and parameter types, version introduced on `plat` or None when the graph doesn't say)."""
    out = {}
    for f in glob.glob(os.path.join(root, plat, "*.symbols.json")):
        for raw in json.load(open(f))["symbols"]:
            intro = next((version(a["introduced"]) for a in raw.get("availability") or []
                          if a.get("domain") == DOMAIN[plat] and a.get("introduced")), None)
            # Parameter types without their result builder: `@ViewBuilder` became `@ContentBuilder` in SDK 27
            params = [BUILDER.sub("", "".join(fr["spelling"] for fr in prm.get("declarationFragments", [])))
                      for prm in raw.get("functionSignature", {}).get("parameters", [])]
            out[raw["identifier"]["precise"]] = (".".join(raw["pathComponents"]) + "(" + ", ".join(params) + ")", intro)
    return out


def main(newest, n1):
    missing = set()
    for plat in DOMAIN:
        if not glob.glob(os.path.join(n1, plat, "*.symbols.json")):
            continue
        old = symbols(n1, plat)
        old_paths = {path for path, _ in old.values()}
        for usr, (path, intro) in symbols(newest, plat).items():
            # Introduced in the newest SDK: already guarded by its version. Same path and parameter types in the
            # previous SDK: an existing API whose mangled name changed, compiled by both.
            if usr not in old and path not in old_paths and (intro is None or intro < NEWEST_SDK[plat]):
                missing.add(usr)
    path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "n1_missing.json")
    with open(path, "w") as f:
        json.dump(sorted(missing), f, indent=0)
        f.write("\n")
    print("%d symbols missing from the previous SDK: %s" % (len(missing), path))


if __name__ == "__main__":
    main(*sys.argv[1:3])
