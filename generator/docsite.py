#!/usr/bin/env python3
"""Builds the documentation website (GitHub Pages) into an output directory.

usage: docsite.py <repo-root> <out-dir>

- Reference: one page per SwiftUI symbol supported by Compose SwiftUI, built from the generated Kotlin sources: the
  Swift declaration, the Kotlin equivalent, availability, a usage sketch and a link to Apple's page for the full
  explanation. Apple's documentation text is not copied.
- Guides: the README sections, CHANGELOG and benchmark results, rendered from Markdown.
"""
import glob, html, json, os, re, shutil, sys

ROOT, OUT = sys.argv[1], sys.argv[2]
GEN = os.path.join(ROOT, "runtime/src/generated/kotlin/dev/nucleusframework/composeswiftui")
APPLE = "https://developer.apple.com/documentation/swiftui/"
REPO = "https://github.com/NucleusFramework/ComposeSwiftUI"

def esc(s):
    return html.escape(s, quote=True)

def collapse(s):
    return re.sub(r"\s+", " ", s).replace("( ", "(").replace(",)", ")").replace(", )", ")").strip()

def slug(s):
    return re.sub(r"[^A-Za-z0-9_.-]+", "-", s).strip("-") or "index"

# ---- parse generated Kotlin -----------------------------------------------------------------------------------

def parse_file(path, kind_hint):
    """Yields entries from generated KDoc blocks followed by a public declaration."""
    lines = open(path).read().split("\n")
    i = 0
    while i < len(lines):
        if lines[i].strip() == "/**":
            doc = []
            i += 1
            while i < len(lines) and lines[i].strip() != "*/":
                doc.append(re.sub(r"^\s*\* ?", "", lines[i]))
                i += 1
            i += 1
            while i < len(lines) and lines[i].strip().startswith("@"):
                i += 1
            decl = [lines[i]] if i < len(lines) else []
            # A composable's parameters span several lines, up to ") {"; other declarations fit on one line.
            if i < len(lines) and re.match(r"\s*public fun \w+\($", lines[i].rstrip()):
                while i + 1 < len(lines) and not lines[i].rstrip().endswith(") {"):
                    i += 1
                    decl.append(lines[i])
            yield make_entry(doc, "\n".join(decl), kind_hint)
        i += 1

def make_entry(doc, decl, kind_hint):
    text = "\n".join(doc)
    m = re.match(r"SwiftUI `([^`]+)`", text.strip())
    swift_path = m.group(1) if m else None
    sm = re.search(r"```swift\n(.*?)\n```", text, re.S)
    swift_decl = sm.group(1).strip() if sm else ""
    am = re.search(r"Availability: (.*)", text)
    avail = am.group(1).strip() if am else ""
    notes = [l for l in doc if l.strip() and not l.startswith("SwiftUI `") and not l.startswith("Availability:")
             and "```" not in l and l.strip() not in swift_decl]
    d = collapse(decl)
    d = re.sub(r"\s*\{.*$", "", d)
    d = re.sub(r"\s*=\s*element\(.*$", "", d)
    d = re.sub(r"\s*: Modifier$", ": Modifier", d)
    d = re.sub(r" internal constructor\(.*$", "", d)
    d = re.sub(r"\s*get\(\) = .*$", "", d)          # value: drop the encoding
    d = re.sub(r"\)\s*: this\(.*$", ")", d)          # constructor: drop the delegation
    d = re.sub(r"\)(: [\w.<>?, ]+?) = \w[\w.]*\(.*$", r")\1", d)  # factory function: keep the return type
    d = re.sub(r"\s*:\s*SExpr\(.*$", "", d)
    d = d.replace("public ", "").replace("@HiddenFromObjC ", "")
    kind = kind_hint
    if d.startswith("class ") or d.startswith("interface ") or d.startswith("object "):
        kind = "type"
    return {"swift": swift_path, "swiftDecl": swift_decl, "avail": avail, "kotlin": d, "kind": kind,
            "notes": " ".join(n.strip() for n in notes)}

entries = []
for f in sorted(glob.glob(os.path.join(GEN, "GenViews.kt"))):
    entries += list(parse_file(f, "view"))
for f in sorted(glob.glob(os.path.join(GEN, "GenModifiers*.kt"))):
    entries += list(parse_file(f, "modifier"))
for f in sorted(glob.glob(os.path.join(GEN, "GenTypes*.kt"))):
    entries += list(parse_file(f, "value"))
entries = [e for e in entries if e["swift"] and e["kotlin"]]

# ---- usage sketches -------------------------------------------------------------------------------------------

def split_params(s):
    out, depth, cur = [], 0, ""
    for ch in s:
        if ch in "(<[{":
            depth += 1
        elif ch in ")>]}":
            depth -= 1
        if ch == "," and depth == 0:
            out.append(cur.strip()); cur = ""
        else:
            cur += ch
    if cur.strip():
        out.append(cur.strip())
    return out

def placeholder(t):
    t = t.strip()
    if t.endswith("?"):
        t = t[:-1]
    if t.startswith("@Composable"):
        return "{ … }"
    if "->" in t:
        args = t.split("->")[0].strip().strip("()")
        return "{ }" if not args else "{ value -> }"
    return {"String": '"…"', "Double": "0.0", "Int": "0", "Boolean": "true", "Float": "0f", "Long": "0L",
            "LocalizedKey": 'LocalizedKey("key")', "Any": "value"}.get(t, t.split("<")[0] + ".…")

def sketch(e):
    k = e["kotlin"]
    m = re.match(r"fun (Modifier\.)?(`?\w+`?)\((.*)\)", k)
    if not m:
        return None
    params = [p for p in split_params(m.group(3)) if p]
    req, trailing = [], None
    for p in params:
        if "=" in p.split(":", 1)[-1] and not p.split(":", 1)[-1].strip().startswith("@Composable"):
            if "= " in p:
                continue
        name, _, typ = p.partition(":")
        name = name.strip()
        if name == "modifier":
            continue
        if "=" in typ:
            continue
        if typ.strip().startswith("@Composable") and p is params[-1]:
            trailing = name
            continue
        req.append("%s = %s" % (name, placeholder(typ)))
    call = "%s(%s)" % (m.group(2), ", ".join(req))
    if m.group(1):
        return "Modifier." + call
    if trailing:
        call = (call[:-2] if call.endswith("()") else call) + " { … }"
    return call

# ---- group into pages ---------------------------------------------------------------------------------------

pages = {}  # key -> {"title", "category", "entries"}
for e in entries:
    path = e["swift"]
    head = path.split(".")[0].split("(")[0]
    if head == "View" and e["kind"] == "modifier":
        name = path.split(".", 1)[1].split("(")[0]
        key, title, cat = "modifier-" + name, name, "Modifiers"
    else:
        key, title, cat = "type-" + head, head, "Types"
    p = pages.setdefault(key, {"title": title, "category": cat, "entries": []})
    p["entries"].append(e)
for p in pages.values():
    if any(e["kind"] == "view" for e in p["entries"]):
        p["category"] = "Views"

def apple_url(e, page):
    if page["category"] == "Modifiers":
        return APPLE + "view/" + e["swift"].split(".", 1)[1].lower()
    return APPLE + page["title"].lower()

# ---- Markdown subset for the guides -------------------------------------------------------------------------

def inline(s):
    s = esc(s)
    s = re.sub(r"`([^`]+)`", r"<code>\1</code>", s)
    s = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", s)
    s = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", lambda m: '<a href="%s">%s</a>' % (link(m.group(2)), m.group(1)), s)
    return s

def link(target):
    t = html.unescape(target)
    if t.startswith("http"):
        return t
    mapping = {"docs/API.md": "reference.html", "docs/COVERAGE.md": REPO + "/blob/main/docs/COVERAGE.md",
               "docs/bench/RESULTS.md": "benchmarks.html", "docs/CONTRIBUTING.md": "contributing.html",
               "../bench/BENCH_GUIDE.md": REPO + "/blob/main/bench/BENCH_GUIDE.md"}
    return mapping.get(t, REPO + "/blob/main/" + t)

def markdown(md):
    out, lines, i = [], md.split("\n"), 0
    while i < len(lines):
        l = lines[i]
        if l.startswith("```"):
            lang = l[3:].strip()
            code = []
            i += 1
            while i < len(lines) and not lines[i].startswith("```"):
                code.append(lines[i]); i += 1
            out.append('<pre><code class="%s">%s</code></pre>' % (esc(lang), esc("\n".join(code))))
        elif re.match(r"^#{1,4} ", l):
            n = len(l.split(" ")[0])
            text = l[n + 1:]
            out.append('<h%d id="%s">%s</h%d>' % (n, slug(text.lower()), inline(text), n))
        elif l.startswith("|"):
            rows = []
            while i < len(lines) and lines[i].startswith("|"):
                rows.append(lines[i]); i += 1
            i -= 1
            cells = lambda r: [c.strip() for c in r.strip().strip("|").split("|")]
            t = ["<table><thead><tr>" + "".join("<th>%s</th>" % inline(c) for c in cells(rows[0])) + "</tr></thead><tbody>"]
            for r in rows[2:]:
                t.append("<tr>" + "".join("<td>%s</td>" % inline(c) for c in cells(r)) + "</tr>")
            out.append("".join(t) + "</tbody></table>")
        elif re.match(r"^\s*(- |\d+\. )", l):
            ordered = bool(re.match(r"^\s*\d+\. ", l))
            items = []
            while i < len(lines) and (re.match(r"^\s*(- |\d+\. )", lines[i]) or (lines[i].startswith("  ") and items)):
                if re.match(r"^\s*(- |\d+\. )", lines[i]):
                    items.append(re.sub(r"^\s*(- |\d+\. )", "", lines[i]))
                elif lines[i].strip().startswith("```"):
                    break
                else:
                    items[-1] += " " + lines[i].strip()
                i += 1
            i -= 1
            tag = "ol" if ordered else "ul"
            out.append("<%s>%s</%s>" % (tag, "".join("<li>%s</li>" % inline(x) for x in items), tag))
        elif l.strip():
            para = [l]
            while i + 1 < len(lines) and lines[i + 1].strip() and not re.match(r"^(#|```|\||\s*- |\s*\d+\. )", lines[i + 1]):
                i += 1; para.append(lines[i])
            out.append("<p>%s</p>" % inline(" ".join(x.strip() for x in para)))
        i += 1
    return "\n".join(out)

def sections(md):
    """Splits a Markdown document on its level-2 headings."""
    parts, cur, title = [], [], None
    for l in md.split("\n"):
        if l.startswith("## "):
            if title is not None:
                parts.append((title, "\n".join(cur)))
            title, cur = l[3:].strip(), []
        elif title is not None:
            cur.append(l)
    if title is not None:
        parts.append((title, "\n".join(cur)))
    return parts

# ---- page layout ----------------------------------------------------------------------------------------------

CSS = """
:root{--bg:#fff;--fg:#1d1d1f;--muted:#6e6e73;--line:#e5e5ea;--code:#f5f5f7;--accent:#7f52ff}
@media (prefers-color-scheme:dark){:root{--bg:#1c1c1e;--fg:#f2f2f7;--muted:#a1a1a6;--line:#3a3a3c;--code:#2c2c2e;--accent:#b49cff}}
*{box-sizing:border-box}body{margin:0;font:16px/1.55 -apple-system,BlinkMacSystemFont,"Segoe UI",sans-serif;background:var(--bg);color:var(--fg)}
a{color:var(--accent);text-decoration:none}a:hover{text-decoration:underline}
header{border-bottom:1px solid var(--line);padding:12px 24px;display:flex;gap:20px;align-items:center;position:sticky;top:0;background:var(--bg);z-index:1}
header .brand{font-weight:700;color:var(--fg)}header nav a{margin-right:14px}
main{max-width:980px;margin:0 auto;padding:24px}
code{font:0.88em ui-monospace,SFMono-Regular,Menlo,monospace;background:var(--code);padding:1px 5px;border-radius:5px}
pre{background:var(--code);padding:12px 14px;border-radius:10px;overflow:auto}pre code{background:none;padding:0}
table{border-collapse:collapse;width:100%;margin:12px 0;font-size:.95em}th,td{border:1px solid var(--line);padding:6px 8px;text-align:left;vertical-align:top}
.entry{border:1px solid var(--line);border-radius:12px;padding:14px 16px;margin:16px 0}
.entry h3{margin:0 0 6px;font-size:1em}.label{color:var(--muted);font-size:.8em;text-transform:uppercase;letter-spacing:.04em;margin:10px 0 2px}
.avail{color:var(--muted);font-size:.9em}.cols{columns:3 220px}.cols a{display:block}
#q{width:100%;padding:10px 12px;font-size:1em;border:1px solid var(--line);border-radius:10px;background:var(--bg);color:var(--fg)}
#results a{display:block;padding:3px 0}.kind{color:var(--muted);font-size:.85em;margin-left:6px}
footer{color:var(--muted);font-size:.85em;max-width:980px;margin:40px auto;padding:0 24px}
"""

def layout(title, body, depth=0):
    up = "../" * depth
    return """<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>%s · Compose SwiftUI</title><link rel="stylesheet" href="%sstyle.css"></head><body>
<header><a class="brand" href="%sindex.html">Compose SwiftUI</a><nav><a href="%sguide.html">Guide</a><a href="%sreference.html">Reference</a>
<a href="%sbenchmarks.html">Benchmarks</a><a href="%schangelog.html">Changelog</a><a href="%s">GitHub</a></nav></header>
<main>%s</main><footer>Compose SwiftUI documentation. SwiftUI is a trademark of Apple Inc. Explanations of each SwiftUI API are on
<a href="https://developer.apple.com/documentation/swiftui">Apple's documentation</a>, linked from every reference page.</footer></body></html>
""" % (esc(title), up, up, up, up, up, up, REPO, body)

def write(rel, content):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write(content)

# ---- build ----------------------------------------------------------------------------------------------------

if os.path.isdir(OUT):
    shutil.rmtree(OUT)
os.makedirs(OUT)
write("style.css", CSS)
write(".nojekyll", "")

readme = open(os.path.join(ROOT, "README.md")).read()
intro = readme.split("\n## ")[0]
intro_html = markdown("\n".join(intro.split("\n")[1:]))
guide_parts = [(t, b) for t, b in sections(readme) if t not in ("Contributing", "Reference", "Performance")]

index_body = """<h1>Compose SwiftUI</h1>%s
<p><a href="guide.html">Read the guide</a> · <a href="reference.html">Browse the %d supported SwiftUI APIs</a> · <a href="%s">GitHub</a></p>
<h2>Search the reference</h2><input id="q" placeholder="Text, padding, NavigationStack…" autofocus><div id="results"></div>
<script>
let idx=[];fetch('search.json').then(r=>r.json()).then(d=>idx=d);
const q=document.getElementById('q'),res=document.getElementById('results');
q.addEventListener('input',()=>{const s=q.value.trim().toLowerCase();if(!s){res.innerHTML='';return}
const hits=idx.filter(e=>e.n.toLowerCase().includes(s)).sort((a,b)=>(a.n.toLowerCase().indexOf(s))-(b.n.toLowerCase().indexOf(s))||a.n.length-b.n.length).slice(0,60);
res.innerHTML=hits.map(e=>`<a href="${e.u}">${e.n}<span class="kind">${e.k}</span></a>`).join('')||'<p>No match.</p>'});
</script>""" % (intro_html, len(entries), REPO)
write("index.html", layout("Home", index_body))

toc = "".join('<li><a href="#%s">%s</a></li>' % (slug(t.lower()), esc(t)) for t, _ in guide_parts)
guide_body = "<h1>Guide</h1><ul>%s</ul>" % toc + "".join(
    '<h2 id="%s">%s</h2>%s' % (slug(t.lower()), esc(t), markdown(b)) for t, b in guide_parts)
write("guide.html", layout("Guide", guide_body))

perf = dict(sections(readme)).get("Performance", "")
results = open(os.path.join(ROOT, "docs/bench/RESULTS.md")).read()
write("benchmarks.html", layout("Benchmarks", "<h1>Performance</h1>" + markdown(perf) + markdown(results)))
write("changelog.html", layout("Changelog", markdown(open(os.path.join(ROOT, "CHANGELOG.md")).read())))
write("contributing.html", layout("Contributing", markdown(open(os.path.join(ROOT, "docs/CONTRIBUTING.md")).read())))

search = []
cats = {"Views": [], "Modifiers": [], "Types": []}
for key, p in sorted(pages.items(), key=lambda kv: kv[1]["title"].lower()):
    rel = "reference/%s.html" % slug(key)
    cats[p["category"]].append((p["title"], rel))
    search.append({"n": p["title"] if p["category"] != "Modifiers" else "." + p["title"], "u": rel, "k": p["category"][:-1].lower()})
    body = ['<p><a href="../reference.html">Reference</a> › %s</p><h1>%s</h1>' % (p["category"], esc(p["title"]))]
    body.append('<p>Compose SwiftUI equivalents of SwiftUI <code>%s</code>. For what it does, see '
                '<a href="%s">Apple\'s documentation</a>.</p>' % (esc(p["title"]), apple_url(p["entries"][0], p)))
    for e in p["entries"]:
        ex = sketch(e)
        body.append('<div class="entry"><h3><code>%s</code> <a class="kind" href="%s">Apple ↗</a></h3>' % (esc(e["swift"]), apple_url(e, p)))
        if e["swiftDecl"]:
            body.append('<div class="label">Swift</div><pre><code>%s</code></pre>' % esc(e["swiftDecl"]))
        body.append('<div class="label">Compose SwiftUI (Kotlin)</div><pre><code>%s</code></pre>' % esc(e["kotlin"]))
        if ex and e["kind"] in ("view", "modifier"):
            body.append('<div class="label">Usage</div><pre><code>%s</code></pre>' % esc(ex))
        if e["notes"]:
            body.append("<p>%s</p>" % inline(e["notes"]))
        if e["avail"]:
            body.append('<div class="avail">%s</div>' % esc(e["avail"]))
        body.append("</div>")
    write(rel, layout(p["title"], "".join(body), depth=1))

handwritten = dict(sections(readme)).get("Writing screens", "")
ref_body = ["<h1>Reference</h1><p>Every SwiftUI API that Compose SwiftUI supports, generated from Apple's SDK and compiled for "
            "iOS, macOS, tvOS and watchOS. Each page shows the Swift declaration next to its Kotlin equivalent. "
            "APIs that need Kotlin-specific shapes (navigation paths, item presentations, gestures, lazy lists…) are "
            "listed in the <a href=\"guide.html#writing-screens\">guide</a>. "
            "<a href=\"%s/blob/main/docs/COVERAGE.md\">COVERAGE.md</a> gives the status of every Apple documentation entry.</p>" % REPO]
for cat in ("Views", "Modifiers", "Types"):
    ref_body.append("<h2>%s (%d)</h2><div class=\"cols\">%s</div>" % (
        cat, len(cats[cat]), "".join('<a href="%s">%s</a>' % (rel, esc(("." if cat == "Modifiers" else "") + t)) for t, rel in cats[cat])))
write("reference.html", layout("Reference", "".join(ref_body)))
write("search.json", json.dumps(search, separators=(",", ":")))
print("entries=%d pages=%d views=%d modifiers=%d types=%d" % (len(entries), len(pages), len(cats["Views"]), len(cats["Modifiers"]), len(cats["Types"])))
