#!/usr/bin/env python3
"""
Cross-checks every entry of Apple's SwiftUI documentation index against the generator coverage
and writes docs/COVERAGE.md.
usage: coverage_report.py <apple-doc-index.json> <project-root>
"""
import collections, json, os, re, sys

index_path, root = sys.argv[1], sys.argv[2]
cov = json.load(open(os.path.join(root, "generator/coverage.json")))
# SwiftUI APIs of other frameworks (MapKit, Charts, StoreKit…), classified by generator/overlays.py
overlays_path = os.path.join(root, "generator/coverage_overlays.json")
if os.path.exists(overlays_path):
    cov += json.load(open(overlays_path))
by_key = collections.defaultdict(list)
for e in cov:
    key = e["path"].lower().replace(".", "/")
    by_key[key].append(e)
    if "<" in key or ">" in key:  # Apple's URLs spell operators with '_' for '<' and '>'
        by_key[key.replace("<", "_").replace(">", "_")].append(e)
# Documented symbols that no supported SDK declares (newer than the installed SDKs, or Mac Catalyst only).
doc_only = json.load(open(os.path.join(root, "generator/integrations.json"))).get("doc_only", {})

RANK = {"generated": 0, "covered": 1, "rejected": 2, "skipped": 3, "n/a": 4}
ICON = {"generated": "✅", "covered": "☑️", "rejected": "⛔", "skipped": "➖", "n/a": "·", "unmatched": "?"}

def norm(path):
    segs = path.split("/")[3:]  # drop '', 'documentation', 'swiftui'
    out = []
    for s in segs:
        s = re.sub(r"-(swift\.[a-z.]+|[0-9a-z]{4,6})$", "", s)
        out.append(s)
    return "/".join(out)

doc = json.load(open(index_path))
entries = []
def walk(node, section, group):
    t = node.get("type")
    if t == "groupMarker":
        return
    if node.get("external") or not node.get("path"):
        return
    entries.append((section, node.get("title", ""), t, node["path"]))
    for c in node.get("children", []):
        walk(c, section, group)

top = doc["interfaceLanguages"]["swift"][0]
section = ""
for c in top.get("children", []):
    if c.get("type") == "groupMarker":
        section = c["title"]
        continue
    walk(c, section, None)

symbol_kinds = {"method", "property", "init", "struct", "case", "protocol", "associatedtype", "enum", "typealias",
                "subscript", "macro", "class", "op", "func"}
rows = []
stats = collections.Counter()
per_kind = collections.defaultdict(collections.Counter)
for section, title, kind, path in entries:
    if kind not in symbol_kinds:
        continue
    key = norm(path)
    matches = by_key.get(key, [])
    if matches:
        best = min(matches, key=lambda e: RANK.get(e["status"], 9))
        n_gen = sum(1 for e in matches if e["status"] in ("generated", "covered"))
        status = best["status"]
        detail = best["detail"] if status != "generated" else "%s (%d/%d overloads bridged)" % (best["detail"], n_gen, len(matches))
        plats = ",".join(best["platforms"])
    elif any(key == k or key.startswith(k) for k in doc_only):
        status, detail, plats = "n/a", next(v for k, v in doc_only.items() if key == k or key.startswith(k)), ""
    else:
        status, detail, plats = "unmatched", "not in the iOS, macOS, tvOS or watchOS SDKs (visionOS-only, or a framework without a SwiftUI overlay on these platforms)", ""
    stats[status] += 1
    per_kind[kind][status] += 1
    rows.append((section, kind, title, status, detail, path))

lines = ["# SwiftUI API coverage", "",
         "Every symbol entry of Apple's SwiftUI documentation index (developer.apple.com/documentation/swiftui), "
         "matched against the iOS, macOS, tvOS and watchOS 27 SDK symbol graphs of SwiftUI and of the SwiftUI overlays of other frameworks (MapKit, Charts, StoreKit…), and the generator.", "",
         "Legend: ✅ generated and compiler-verified on every platform where Apple ships it (iOS, macOS, tvOS, watchOS) · ☑️ covered by an equivalent overload · "
         "⛔ generated but rejected by the compiler (reason given) · ➖ skipped (reason given) · · not applicable to a "
         "declarative bridge (type aliases, operators, read-only instance properties) · ? not in the iOS/macOS SDK.", "",
         "## Summary", "", "| Status | Entries |", "|---|---|"]
total = sum(stats.values())
for st in ("generated", "covered", "rejected", "skipped", "n/a", "unmatched"):
    lines.append("| %s %s | %d (%.1f%%) |" % (ICON[st], st, stats[st], 100.0 * stats[st] / total))
lines += ["| **total** | **%d** |" % total, "", "## By kind", "", "| Kind | ✅ | ☑️ | ⛔ | ➖ | · | ? |", "|---|---|---|---|---|---|---|"]
for kind, c in sorted(per_kind.items(), key=lambda x: -sum(x[1].values())):
    lines.append("| %s | %s |" % (kind, " | ".join(str(c[s]) for s in ("generated", "covered", "rejected", "skipped", "n/a", "unmatched"))))
lines += ["", "## Top skip reasons", "", "| Reason | Entries |", "|---|---|"]
reasons = collections.Counter(re.sub(r"^param \w+: ", "", d)[:90] for _, _, _, s, d, _ in rows if s == "skipped")
for r, n in reasons.most_common(25):
    lines.append("| %s | %d |" % (r.replace("|", "\\|"), n))
lines += ["", "## All entries", ""]
cur = None
for section, kind, title, status, detail, path in rows:
    if section != cur:
        lines += ["", "### " + section, "", "| | Kind | API | Detail |", "|---|---|---|---|"]
        cur = section
    lines.append("| %s | %s | [%s](https://developer.apple.com%s) | %s |" % (
        ICON[status], kind, title.replace("|", "\\|"), path, detail.replace("|", "\\|")))
os.makedirs(os.path.join(root, "docs"), exist_ok=True)
open(os.path.join(root, "docs/COVERAGE.md"), "w").write("\n".join(lines) + "\n")
for st in ("generated", "covered", "rejected", "skipped", "n/a", "unmatched"):
    print("%-10s %5d" % (st, stats[st]))
print("total", total)
for kind, c in sorted(per_kind.items(), key=lambda x: -sum(x[1].values()))[:8]:
    print(kind, dict(c))
