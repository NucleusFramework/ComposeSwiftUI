#!/usr/bin/env python3
"""Builds the documentation website (GitHub Pages) into an output directory.

usage: docsite.py <repo-root> <out-dir>

- Reference: one page per SwiftUI symbol supported by Compose SwiftUI, built from the generated Kotlin sources: the
  Swift declaration, the Kotlin equivalent, availability, a usage sketch and a link to Apple's page for the full
  explanation. Apple's documentation text is not copied.
- Guides: the README sections, CHANGELOG and benchmark results, rendered from Markdown.
"""
import glob, html, json, os, re, shutil, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from doc_content import CONTENT, TOPICS

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
            # Skip annotation-only lines (a declaration may also start with an annotation on its own line)
            while i < len(lines) and lines[i].strip().startswith("@") and " public " not in lines[i]:
                i += 1
            decl = [lines[i]] if i < len(lines) else []
            # A composable's parameters span several lines, up to ") {"; other declarations fit on one line.
            if i < len(lines) and re.match(r"\s*(@\w+ )*public fun \w+\($", lines[i].rstrip()):
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
            out.append('<pre><code class="language-%s">%s</code></pre>' % (esc(lang or "plaintext"), esc("\n".join(code))))
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

# ---- page layout (modelled on the structure of Apple's documentation pages) --------------------------------

CSS = """
:root{--bg:#fff;--fg:#1d1d1f;--muted:#6e6e73;--line:#d2d2d7;--soft:#f5f5f7;--code:#f5f5f7;--accent:#0066cc;--side:#fafafc;--pill:#fff}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]){--bg:#1d1d1f;--fg:#f5f5f7;--muted:#a1a1a6;--line:#424245;--soft:#2c2c2e;--code:#2c2c2e;--accent:#2997ff;--side:#161617;--pill:#4a4a4e;color-scheme:dark}}
:root[data-theme=dark]{--bg:#1d1d1f;--fg:#f5f5f7;--muted:#a1a1a6;--line:#424245;--soft:#2c2c2e;--code:#2c2c2e;--accent:#2997ff;--side:#161617;--pill:#4a4a4e;color-scheme:dark}
.theme{display:inline-flex;gap:2px;padding:2px;border-radius:999px;background:var(--soft);border:1px solid var(--line)}.theme button{display:grid;place-items:center;width:28px;height:24px;border:0;border-radius:999px;background:none;color:var(--muted);cursor:pointer;transition:background .2s,color .2s}.theme button:hover{color:var(--fg)}.theme button[aria-pressed=true]{background:var(--pill);color:var(--fg);box-shadow:0 1px 3px rgba(0,0,0,.15)}.theme button:focus-visible{outline:2px solid var(--accent);outline-offset:1px}
*{box-sizing:border-box}html,body{margin:0}body{font:17px/1.47 -apple-system,BlinkMacSystemFont,"SF Pro Text","Helvetica Neue",sans-serif;background:var(--bg);color:var(--fg)}
a{color:var(--accent);text-decoration:none}a:hover{text-decoration:underline}
.top{height:52px;border-bottom:1px solid var(--line);display:flex;align-items:center;gap:22px;padding:0 22px;position:sticky;top:0;background:color-mix(in srgb,var(--bg) 85%,transparent);backdrop-filter:saturate(180%) blur(20px);z-index:2}
.top .brand{font-weight:600;color:var(--fg);font-size:17px}.top a{font-size:14px;color:var(--fg)}.top .sp{flex:1}
.wrap{display:flex;min-height:calc(100vh - 52px)}
.side{width:280px;flex:none;border-right:1px solid var(--line);background:var(--side);padding:16px 0;position:sticky;top:52px;height:calc(100vh - 52px);overflow:auto;font-size:14px}
.side h4{margin:16px 20px 4px;font-size:12px;text-transform:uppercase;letter-spacing:.04em;color:var(--muted)}
.side a{display:block;padding:3px 20px;color:var(--fg)}.side a.on{background:var(--soft);font-weight:600}
.side input{margin:0 16px 8px;width:calc(100% - 32px);padding:7px 10px;border:1px solid var(--line);border-radius:8px;background:var(--bg);color:var(--fg);font-size:14px}
main{flex:1;min-width:0}
.hero{background:var(--soft);border-bottom:1px solid var(--line);padding:40px 56px 32px}
.eyebrow{color:var(--muted);font-size:15px;margin-bottom:4px}.hero h1{font-size:40px;line-height:1.1;margin:0 0 12px;font-weight:700;letter-spacing:-.01em}
.abstract{font-size:21px;line-height:1.38;margin:0 0 16px;max-width:760px}
.chips span{display:inline-block;border:1px solid var(--line);border-radius:12px;padding:1px 10px;margin:0 6px 6px 0;font-size:13px;color:var(--muted)}
.content{padding:32px 56px 64px;max-width:980px}
h2{font-size:28px;margin:40px 0 12px;padding-top:12px;border-top:1px solid var(--line)}h2:first-child{border-top:0;margin-top:0}
h3{font-size:20px;margin:28px 0 10px}
code{font:.88em "SF Mono",ui-monospace,Menlo,monospace}p code,li code,td code{background:var(--code);padding:1px 5px;border-radius:5px}
pre code.hljs{background:none;padding:0}pre{background:var(--code);padding:14px 16px;border-radius:12px;overflow:auto;font-size:15px;line-height:1.5}
.decl{border:1px solid var(--line);background:var(--bg)}
.topic{display:flex;gap:16px;padding:12px 0;border-bottom:1px solid var(--line)}.topic:last-child{border-bottom:0}
.topic .sig{font:14px "SF Mono",ui-monospace,Menlo,monospace;flex:1;min-width:0;overflow-wrap:anywhere}
.topic .meta{color:var(--muted);font-size:13px;margin-top:3px;font-family:-apple-system,sans-serif}
table{border-collapse:collapse;width:100%;margin:12px 0;font-size:15px}th,td{border-bottom:1px solid var(--line);padding:8px;text-align:left;vertical-align:top}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(240px,1fr));gap:12px;margin:16px 0}
.card{border:1px solid var(--line);border-radius:14px;padding:16px}.card b{display:block;margin-bottom:4px;color:var(--fg)}.card span{color:var(--muted);font-size:14px}
#results a{display:block;padding:4px 20px}
footer{color:var(--muted);font-size:13px;padding:24px 56px;border-top:1px solid var(--line)}
@media(max-width:820px){.side{display:none}.hero,.content,footer{padding-left:20px;padding-right:20px}}
"""

def page_url(key):
    return "reference/%s.html" % slug(key)

def sidebar(up, current):
    out = ['<nav class="side"><input id="q" placeholder="Filter" autocomplete="off"><div id="results"></div><div id="tree">',
           '<h4>Essentials</h4><a href="%sindex.html">Overview</a><a href="%sguide.html">Guide</a><a href="%sreference.html">All APIs</a>' % (up, up, up)]
    for title, keys in TOPICS:
        links = [k for k in keys if k in pages]
        if not links:
            continue
        out.append("<h4>%s</h4>" % esc(title))
        for k in links:
            name = pages[k]["title"]
            name = name if not k.startswith("modifier-") else name + "(_:)"
            out.append('<a href="%s%s"%s>%s</a>' % (up, page_url(k), ' class="on"' if k == current else "", esc(name)))
    out.append('<h4>More</h4><a href="%sbenchmarks.html">Benchmarks</a><a href="%schangelog.html">Changelog</a><a href="%scontributing.html">Contributing</a>' % (up, up, up))
    out.append("""</div></nav><script>
const UP='%s';let idx=null;const q=document.getElementById('q'),res=document.getElementById('results'),tree=document.getElementById('tree');
q.addEventListener('input',async()=>{const s=q.value.trim().toLowerCase();if(!s){res.innerHTML='';tree.style.display='';return}
if(!idx)idx=await (await fetch(UP+'search.json')).json();tree.style.display='none';
const hits=idx.filter(e=>e.n.toLowerCase().includes(s)).sort((a,b)=>a.n.toLowerCase().indexOf(s)-b.n.toLowerCase().indexOf(s)||a.n.length-b.n.length).slice(0,50);
res.innerHTML=hits.map(e=>`<a href="${UP+e.u}">${e.n}</a>`).join('')||'<a>No results</a>'});
</script>""" % up)
    return "".join(out)

def layout(title, body, depth=0, current=None):
    up = "../" * depth
    return """<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>%s | Compose SwiftUI</title><link rel="stylesheet" href="%sstyle.css">
<link id="hl-light" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github.min.css" media="(prefers-color-scheme:light)">
<link id="hl-dark" rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/github-dark.min.css" media="(prefers-color-scheme:dark)">
<script>
// Theme: "light", "dark" or "system" (follows the OS). Applied before first paint to avoid a flash.
function setTheme(t){const r=document.documentElement;if(t==='system')r.removeAttribute('data-theme');else r.dataset.theme=t;
document.getElementById('hl-light').media=t==='system'?'(prefers-color-scheme:light)':t==='light'?'all':'not all';
document.getElementById('hl-dark').media=t==='system'?'(prefers-color-scheme:dark)':t==='dark'?'all':'not all';
document.querySelectorAll('.theme button').forEach(b=>b.setAttribute('aria-pressed',b.dataset.t===t));
try{t==='system'?localStorage.removeItem('theme'):localStorage.setItem('theme',t)}catch(e){}}
let THEME='system';try{THEME=localStorage.getItem('theme')||'system'}catch(e){}setTheme(THEME);
</script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js" defer onload="hljs.highlightAll()"></script></head><body>
<div class="top"><a class="brand" href="%sindex.html">Compose SwiftUI</a><span class="sp"></span><a href="%sguide.html">Guide</a><a href="%sreference.html">API</a><a href="%s">GitHub</a><div class="theme" role="group" aria-label="Appearance"><button type="button" data-t="system" title="System" aria-label="System appearance" onclick="setTheme(this.dataset.t)"><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8M12 17v4"/></svg></button><button type="button" data-t="light" title="Light" aria-label="Light appearance" onclick="setTheme(this.dataset.t)"><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/></svg></button><button type="button" data-t="dark" title="Dark" aria-label="Dark appearance" onclick="setTheme(this.dataset.t)"><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/></svg></button></div><script>setTheme(THEME)</script></div>
<div class="wrap">%s<main>%s<footer>SwiftUI is a trademark of Apple Inc. Each reference page links to Apple's documentation of the SwiftUI API it maps.</footer></main></div></body></html>
""" % (esc(title), up, up, up, up, REPO, sidebar(up, current), body)

def write(rel, content):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write(content)

def chips(avail):
    return "".join("<span>%s</span>" % esc(a.strip()) for a in avail.split(",") if a.strip())

def topic_groups(p):
    """Sorts a page's entries into topic groups, like the Topics section of a documentation page."""
    groups = {}
    title = p["title"]
    for e in p["entries"]:
        k = e["kotlin"]
        if p["category"] == "Modifiers":
            g = "Overloads"
        elif k.startswith("fun %s(" % title) or k.startswith("fun %sView(" % title):
            g = "Creating a %s" % title
        elif k.startswith("fun Modifier."):
            g = "Modifiers for %s" % title
        elif k.startswith("constructor"):
            g = "Initializers"
        elif k.startswith("val "):
            g = "Type properties"
        elif k.startswith("fun "):
            g = "Methods"
        else:
            g = "Types"
        groups.setdefault(g, []).append(e)
    order = ["Overloads", "Creating a %s" % title, "Initializers", "Modifiers for %s" % title, "Type properties", "Methods", "Types"]
    return [(g, groups[g]) for g in order if g in groups]

def kind_label(p):
    if p["category"] == "Modifiers":
        return "View modifier"
    if p["category"] == "Views":
        return "View"
    first = p["entries"][0]["swiftDecl"]
    return "Enumeration" if " enum " in " " + first else "Protocol" if "protocol " in first else "Structure"

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

cards = "".join('<a class="card" href="%s"><b>%s</b><span>%s</span></a>' % (page_url(keys[0]), esc(t), esc(", ".join(pages[k]["title"] for k in keys[:4] if k in pages)))
                for t, keys in TOPICS if keys and keys[0] in pages)
index_body = """<div class="hero"><div class="eyebrow">Framework</div><h1>Compose SwiftUI</h1>
<p class="abstract">Write SwiftUI screens in Kotlin with the Compose runtime, and keep your view models.</p>
<div class="chips"><span>iOS 17.0+</span><span>macOS 14.0+</span><span>tvOS 17.0+</span><span>watchOS 10.0+</span></div></div>
<div class="content"><h2>Overview</h2>%s<h2>Topics</h2><div class="grid">%s</div>
<p><a href="reference.html">Browse all %d APIs</a> · <a href="guide.html">Read the guide</a></p></div>""" % (intro_html, cards, len(entries))
write("index.html", layout("Compose SwiftUI", index_body))

toc = "".join('<li><a href="#%s">%s</a></li>' % (slug(t.lower()), esc(t)) for t, _ in guide_parts)
guide_body = '<div class="hero"><div class="eyebrow">Article</div><h1>Guide</h1><p class="abstract">Set up Compose SwiftUI, write screens and use it with other Compose libraries.</p></div><div class="content"><ul>%s</ul>%s</div>' % (
    toc, "".join('<h2 id="%s">%s</h2>%s' % (slug(t.lower()), esc(t), markdown(b)) for t, b in guide_parts))
write("guide.html", layout("Guide", guide_body))

def article(title, abstract, md_html):
    return '<div class="hero"><div class="eyebrow">Article</div><h1>%s</h1><p class="abstract">%s</p></div><div class="content">%s</div>' % (esc(title), esc(abstract), md_html)

perf = dict(sections(readme)).get("Performance", "")
results = open(os.path.join(ROOT, "docs/bench/RESULTS.md")).read()
write("benchmarks.html", layout("Benchmarks", article("Performance", "How Compose SwiftUI compares with plain SwiftUI.", markdown(perf) + markdown(results.split("\n", 1)[1]))))
write("changelog.html", layout("Changelog", article("Changelog", "Changes in each release.", markdown(open(os.path.join(ROOT, "CHANGELOG.md")).read().split("\n", 1)[1]))))
write("contributing.html", layout("Contributing", article("Contributing", "Regenerate the bindings, run the benchmarks, publish a release.", markdown(open(os.path.join(ROOT, "docs/CONTRIBUTING.md")).read().split("\n", 1)[1]))))

search = []
cats = {"Views": [], "Modifiers": [], "Types": []}
for key, p in sorted(pages.items(), key=lambda kv: kv[1]["title"].lower()):
    rel = page_url(key)
    cats[p["category"]].append((p["title"], rel))
    display = p["title"] if p["category"] != "Modifiers" else p["title"] + "(_:)"
    search.append({"n": display, "u": rel})
    abstract, overview, example = CONTENT.get(key, ("", "", ""))
    first = p["entries"][0]
    if not abstract:
        abstract = ("A view modifier that maps SwiftUI's %s." % p["title"]) if p["category"] == "Modifiers" else \
                   ("The Compose SwiftUI form of SwiftUI's %s." % p["title"])
    # The simplest form leads the page: the shortest view or modifier signature, else the type itself.
    wanted = "modifier" if p["category"] == "Modifiers" else "view"
    callables = [e for e in p["entries"] if e["kind"] == wanted and e["kotlin"].startswith("fun ")
                 and not e["kotlin"].startswith("fun %sView(" % p["title"])]  # "XView" only shows a value as a view
    types = sorted((e for e in p["entries"] if e["kotlin"].startswith(("class", "interface"))),
                   key=lambda e: e["swift"] != p["title"])  # the page's own type first
    primary = min(callables, key=lambda e: len(e["kotlin"])) if callables else (types[0] if types else first)
    body = ['<div class="hero"><div class="eyebrow">%s</div><h1>%s</h1><p class="abstract">%s</p><div class="chips">%s</div></div><div class="content">'
            % (kind_label(p), esc(display), inline(abstract), chips(primary["avail"]))]
    body.append('<h2>Declaration</h2><pre class="decl"><code class="language-kotlin">%s</code></pre>' % esc(primary["kotlin"]))
    body.append("<h2>Overview</h2>")
    if overview:
        body.append("<p>%s</p>" % inline(overview))
    if example:
        body.append('<pre><code class="language-kotlin">%s</code></pre>' % esc(example))
    elif primary["kind"] in ("view", "modifier") and sketch(primary):
        body.append('<pre><code class="language-kotlin">%s</code></pre>' % esc(sketch(primary)))
    body.append('<p>This is the Kotlin form of SwiftUI <code>%s</code>: <a href="%s">SwiftUI documentation</a>. '
                'In Swift it reads:</p><pre><code class="language-swift">%s</code></pre>' % (esc(primary["swift"]), apple_url(primary, p), esc(primary["swiftDecl"])))
    body.append("<h2>Topics</h2>")
    for g, items in topic_groups(p):
        body.append("<h3>%s</h3>" % esc(g))
        for e in items:
            meta = "SwiftUI <code>%s</code>" % esc(e["swift"])
            if e["avail"] and e["avail"] != primary["avail"]:
                meta += " · " + esc(e["avail"])
            if e["notes"]:
                meta += " · " + inline(e["notes"])
            body.append('<div class="topic"><div class="sig">%s<div class="meta">%s</div></div></div>' % (esc(e["kotlin"]), meta))
    body.append("</div>")
    write(rel, layout(display, "".join(body), depth=1, current=key))

ref = ['<div class="hero"><div class="eyebrow">API Collection</div><h1>All APIs</h1><p class="abstract">Every SwiftUI API that '
       'Compose SwiftUI supports, generated from Apple\'s SDK and compiled for iOS, macOS, tvOS and watchOS.</p></div><div class="content">',
       '<p>APIs with a Kotlin-specific shape (navigation paths, item presentations, gestures, lazy lists, mixed-style text) are '
       'described in the <a href="guide.html#writing-screens">guide</a>.</p>']
for cat in ("Views", "Modifiers", "Types"):
    ref.append("<h2>%s</h2><div class=\"grid\">%s</div>" % (cat, "".join(
        '<a href="%s">%s</a>' % (rel, esc(t + ("(_:)" if cat == "Modifiers" else ""))) for t, rel in cats[cat])))
ref.append("</div>")
write("reference.html", layout("All APIs", "".join(ref)))
write("search.json", json.dumps(search, separators=(",", ":")))
print("entries=%d pages=%d views=%d modifiers=%d types=%d" % (len(entries), len(pages), len(cats["Views"]), len(cats["Modifiers"]), len(cats["Types"])))
