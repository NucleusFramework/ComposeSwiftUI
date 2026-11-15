#!/usr/bin/env python3
"""
Classifies the SwiftUI APIs that other Apple frameworks add (MapKit, Charts, StoreKit, PhotosUI…) for the coverage
report. They aren't generated: the bridged ones are hand-written in runtime/src/integrations (Kotlin) and
swift/ComposeSwiftUIIntegrations (one opt-in Swift file per framework), listed in generator/integrations.json.

usage: overlays.py <symbolgraph-root> <project-root>   (reads <symbolgraph-root>/overlays/<platform>/*.symbols.json,
       writes generator/coverage_overlays.json)
"""
import glob, json, os, sys

sg_root, project = sys.argv[1], sys.argv[2]
table = json.load(open(os.path.join(project, "generator", "integrations.json")))

syms = {}
for plat in ("ios", "macos", "tvos", "watchos"):
    for f in sorted(glob.glob(os.path.join(sg_root, "overlays", plat, "*.symbols.json"))):
        d = json.load(open(f))
        module = d["module"]["name"]
        for raw in d["symbols"]:
            usr = raw["identifier"]["precise"]
            s = syms.setdefault(usr, {"usr": usr, "path": ".".join(raw["pathComponents"]), "kind": raw["kind"]["identifier"],
                                      "module": module, "platforms": set()})
            available = not any(a.get("isUnconditionallyUnavailable") or a.get("obsoleted")
                                for a in raw.get("availability") or [] if a.get("domain") in (None, "*", {"ios": "iOS", "macos": "macOS", "tvos": "tvOS", "watchos": "watchOS"}[plat]))
            if available:
                s["platforms"].add(plat)


def match(dotted, key):
    return dotted == key or (not key.endswith(")") and dotted.startswith(key))


def framework(module):
    return module.strip("_").replace("_SwiftUI", "")


def classify(s):
    path, kind = s["path"], s["kind"]
    for key, api in table["covered"].items():
        if match(path, key):
            return "covered", "hand-written (%s integration): %s" % (framework(s["module"]), api)
    for key, why in table["not_applicable"].items():
        if match(path, key):
            return "n/a", why
    name = path.split(".")[-1]
    if name.startswith(("hash(into:)", "==(", "encode(to:)", "init(from:)", "index(", "contains(", "relative(")) or name in ("hashValue", "description"):
        return "n/a", "protocol conformance plumbing (Equatable, Hashable, Codable, Collection…)"
    if name.startswith("init(primitivePlottable:"):
        return "n/a", "Plottable conformance plumbing"
    if name == "makeBody(configuration:)":
        return "n/a", "requirement of a Swift style protocol (Kotlin picks the style with an enum)"
    if kind in ("swift.typealias", "swift.associatedtype"):
        return "n/a", "type alias / associated type (no runtime surface)"
    if kind == "swift.func.op":
        return "n/a", "operator"
    if kind in ("swift.property", "swift.subscript", "swift.type.subscript"):
        return "n/a", "instance property / subscript (read side, not a view-building API)"
    if kind == "swift.macro":
        return "n/a", "Swift macro (compile-time)"
    if kind == "swift.protocol":
        return "n/a", "Swift protocol adopted by app-defined types"
    if kind in ("swift.struct", "swift.enum", "swift.class"):
        return "n/a", "type not built directly from Kotlin: the %s integration exposes what views need" % framework(s["module"])
    if not s["platforms"]:
        return "n/a", "not available on iOS, macOS, tvOS or watchOS"
    return "skipped", "%s integration: not bridged" % framework(s["module"])


out = []
for s in syms.values():
    status, detail = classify(s)
    out.append({"usr": s["usr"], "path": s["path"], "kind": s["kind"], "status": status, "detail": detail,
                "platforms": sorted(s["platforms"])})
out.sort(key=lambda x: x["path"])
with open(os.path.join(project, "generator", "coverage_overlays.json"), "w") as f:
    json.dump(out, f, indent=1)
counts = {}
for e in out:
    counts[e["status"]] = counts.get(e["status"], 0) + 1
print("overlays:", counts)
for e in out:
    if e["status"] == "skipped":
        print("  skipped", e["path"])
