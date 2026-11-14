#!/usr/bin/env python3
"""Builds report.html + summary.md from the iOS and macOS results.json of a bench run."""
import json, os, statistics, sys

out = sys.argv[1]
plats = {}
for p in ("ios", "macos"):
    f = os.path.join(out, p, "results.json")
    if os.path.exists(f):
        plats[p] = {r["index"]: r for r in json.load(open(f))}

def pct(x):
    return "%.4f%%" % (x * 100)

md = ["# Benchmark: SwiftUI vs Compose SwiftUI", ""]
html = ["<!doctype html><meta charset=utf-8><title>Compose SwiftUI bench</title>",
        "<style>body{font:14px -apple-system,system-ui;margin:24px;color:#222}table{border-collapse:collapse;margin:12px 0}"
        "td,th{border:1px solid #ddd;padding:4px 8px;text-align:right}th{background:#f4f4f6}td.l{text-align:left}"
        ".ok{color:#1a7f37;font-weight:600}.ko{color:#cf222e;font-weight:600}img{width:180px;border:1px solid #ccc;margin:2px}"
        "details{margin:6px 0}</style><h1>SwiftUI vs Compose SwiftUI</h1>"]
for p, res in plats.items():
    rows = [res[k] for k in sorted(res)]
    identical = sum(1 for r in rows if r["initialDiff"] <= 0.0001 and r["afterUpdateDiff"] <= 0.0001)
    ms = [r["medianMountSwiftUI"] for r in rows]
    mc = [r["medianMountCompose"] for r in rows]
    us = [r["swiftUI"]["updateMs"] for r in rows]
    uc = [r["compose"]["updateMs"] for r in rows]
    summary = [
        "## %s — %d screens" % ("iOS simulator" if p == "ios" else "macOS", len(rows)), "",
        "- Pixel-identical (≤0.01%% before and after 30 updates): **%d / %d**" % (identical, len(rows)),
        "- Mount (median per screen, then mean): SwiftUI **%.2f ms**, Compose SwiftUI **%.2f ms** (×%.2f)" % (
            statistics.mean(ms), statistics.mean(mc), statistics.mean(mc) / statistics.mean(ms)),
        "- Update (state change → laid out): SwiftUI **%.3f ms**, Compose SwiftUI **%.3f ms** (×%.2f)" % (
            statistics.mean(us), statistics.mean(uc), statistics.mean(uc) / statistics.mean(us)), "",
        "| # | Screen | diff | after updates | mount SwiftUI | mount Compose | update SwiftUI | update Compose |",
        "|---|---|---|---|---|---|---|---|"]
    md += summary
    html.append("<h2>%s</h2><ul>%s</ul>" % (summary[0][3:], "".join("<li>%s</li>" % s[2:].replace("**", "") for s in summary[2:5])))
    html.append("<table><tr><th>#</th><th>Screen</th><th>diff</th><th>after updates</th><th>mount SwiftUI (ms)</th>"
                "<th>mount Compose (ms)</th><th>update SwiftUI (ms)</th><th>update Compose (ms)</th><th>shots</th></tr>")
    for r in rows:
        n = r["index"]
        ok = r["initialDiff"] <= 0.0001 and r["afterUpdateDiff"] <= 0.0001
        md.append("| %d | %s | %s | %s | %.2f | %.2f | %.3f | %.3f |" % (
            n, r["title"], pct(r["initialDiff"]), pct(r["afterUpdateDiff"]), r["medianMountSwiftUI"],
            r["medianMountCompose"], r["swiftUI"]["updateMs"], r["compose"]["updateMs"]))
        shots = "".join('<img src="%s/screen%02d-%s.png">' % (p, n, k) for k in ("swiftui", "compose", "diff"))
        html.append("<tr><td>%d</td><td class=l>%s</td><td class=%s>%s</td><td class=%s>%s</td><td>%.2f</td><td>%.2f</td>"
                    "<td>%.3f</td><td>%.3f</td><td><details><summary>view</summary>%s</details></td></tr>" % (
                        n, r["title"], "ok" if ok else "ko", pct(r["initialDiff"]), "ok" if ok else "ko",
                        pct(r["afterUpdateDiff"]), r["medianMountSwiftUI"], r["medianMountCompose"],
                        r["swiftUI"]["updateMs"], r["compose"]["updateMs"], shots))
    html.append("</table>")
    md.append("")
open(os.path.join(out, "report.html"), "w").write("\n".join(html))
open(os.path.join(out, "summary.md"), "w").write("\n".join(md))
print("\n".join(l for l in md if l.startswith("- ") or l.startswith("## ")))
