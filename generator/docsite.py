#!/usr/bin/env python3
"""Builds the documentation website (GitHub Pages) into an output directory.

usage: docsite.py <repo-root> <out-dir>

- Reference: one page per SwiftUI symbol supported by Compose SwiftUI, built from the generated Kotlin sources: the
  Swift declaration, the Kotlin equivalent, availability, a usage sketch and a link to Apple's page for the full
  explanation. Apple's documentation text is not copied.
- Guides: the README sections, CHANGELOG and benchmark results, rendered from Markdown.
"""
import glob, hashlib, html, json, os, re, shutil, struct, subprocess, sys, urllib.request, zlib
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from doc_content import CONTENT, TOPICS

ROOT, OUT = sys.argv[1], sys.argv[2]
GEN = os.path.join(ROOT, "runtime/src/generated/kotlin/dev/nucleusframework/composeswiftui")
APPLE = "https://developer.apple.com/documentation/swiftui/"
REPO = "https://github.com/NucleusFramework/ComposeSwiftUI"
SITE = "https://nucleusframework.github.io/ComposeSwiftUI/"

def latest_version():
    """The latest GitHub release tag (e.g. "v0.3.0"); falls back to the newest git tag, then to "dev"."""
    try:
        req = urllib.request.Request("https://api.github.com/repos/NucleusFramework/ComposeSwiftUI/releases/latest",
                                     headers={"Accept": "application/vnd.github+json"})
        if os.environ.get("GITHUB_TOKEN"):
            req.add_header("Authorization", "Bearer " + os.environ["GITHUB_TOKEN"])
        return json.load(urllib.request.urlopen(req, timeout=10))["tag_name"]
    except Exception:
        tags = subprocess.run(["git", "-C", ROOT, "tag", "--sort=-v:refname"], capture_output=True, text=True).stdout.split()
        return tags[0] if tags else "dev"

VERSION = latest_version()
ASSETS_V = hashlib.sha256(open(__file__, "rb").read()).hexdigest()[:10]  # changes with the CSS and JS below
# Full-text search: Pagefind (open source) indexes the built pages; the search then runs in the browser, offline too.
PAGEFIND = "pagefind@1.5.2"

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

# ---- syntax highlighting at build time (Xcode-like classes, no client-side highlighter) ---------------------------

KEYWORDS = {
    "kotlin": "fun val var class interface object enum data sealed open override private public internal protected return if "
              "else when for while do in is as by get set import package typealias this super null true false try catch "
              "finally throw suspend inline reified companion init constructor lateinit const abstract annotation "
              "operator infix vararg out where break continue it",
    "swift": "func let var struct class enum protocol extension import return if else guard switch case default for in "
             "while repeat true false nil self Self some any static private public internal fileprivate open init deinit "
             "throws rethrows try catch inout where associatedtype typealias async await nonisolated mutating override "
             "final convenience required lazy weak unowned do break continue",
    "bash": "if then else fi for in do done export cd echo",
}
TOKEN = re.compile(r"""(?P<c>//[^\n]*|/\*.*?\*/|\#[^\n]*)|(?P<s>\"\"\".*?\"\"\"|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\\n])*')"""
                   r"|(?P<m>@\w+)|(?P<n>\b\d+(?:\.\d+)?[fFL]?\b)|(?P<w>\b[A-Za-z_]\w*\b)", re.S)

def highlight(code, lang):
    """Code as HTML with token classes: keywords, strings, numbers, comments, annotations and type names."""
    lang = {"kt": "kotlin", "kts": "kotlin", "sh": "bash", "shell": "bash", "zsh": "bash"}.get(lang, lang)
    if lang not in KEYWORDS:
        return esc(code)
    keywords = set(KEYWORDS[lang].split())
    out, pos = [], 0
    for m in TOKEN.finditer(code):
        kind, text = m.lastgroup, m.group()
        if kind == "c" and text.startswith("#") != (lang == "bash"):
            continue  # "#" comments only in shell scripts, "//" ones elsewhere
        if kind == "w":
            kind = "k" if text in keywords else "t" if text[0].isupper() and lang != "bash" else None
        if kind == "m" and lang == "bash":
            kind = None
        out.append(esc(code[pos:m.start()]))
        out.append('<span class="%s">%s</span>' % (kind, esc(text)) if kind else esc(text))
        pos = m.end()
    return "".join(out) + esc(code[pos:])

def code_block(code, lang, cls=""):
    return '<pre%s><code>%s</code></pre>' % (' class="%s"' % cls if cls else "", highlight(code, lang))

def markdown(md):
    out, lines, i = [], md.split("\n"), 0
    while i < len(lines):
        l = lines[i]
        if l.lstrip().startswith("```"):  # fences may be indented under a list item
            indent = len(l) - len(l.lstrip())
            lang = l.strip()[3:].strip()
            code = []
            i += 1
            while i < len(lines) and not lines[i].lstrip().startswith("```"):
                code.append(lines[i][indent:] if not lines[i][:indent].strip() else lines[i].lstrip()); i += 1
            out.append(code_block("\n".join(code), lang))
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
            heads = cells(rows[0])
            langs = ["swift" if h.lower() in ("swiftui", "swift") else "kotlin" for h in heads]
            colour = lambda c, lang: re.sub(r"<code>(.*?)</code>", lambda m: "<code>%s</code>" % highlight(html.unescape(m.group(1)), lang), inline(c))
            t = ["<table><thead><tr>" + "".join("<th>%s</th>" % inline(c) for c in heads) + "</tr></thead><tbody>"]
            for r in rows[2:]:
                t.append("<tr>" + "".join("<td>%s</td>" % colour(c, langs[i] if i < len(langs) else "kotlin") for i, c in enumerate(cells(r))) + "</tr>")
            out.append("".join(t) + "</tbody></table>")
        elif re.match(r"^\s*(- |\d+\. )", l):
            number = re.match(r"^\s*(\d+)\. ", l)  # a list split by a code block keeps its numbering
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
            tag = "ol" if number else "ul"
            start = ' start="%s"' % number.group(1) if number and number.group(1) != "1" else ""
            out.append("<%s%s>%s</%s>" % (tag, start, "".join("<li>%s</li>" % inline(x) for x in items), tag))
        elif l.strip():
            para = [l]
            while i + 1 < len(lines) and lines[i + 1].strip() and not re.match(r"^(#|\s*```|\||\s*- |\s*\d+\. )", lines[i + 1]):
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
:root{--bg:#fff;--fg:#1d1d1f;--muted:#6e6e73;--faint:#86868b;--line:#d2d2d7;--soft:#f5f5f7;--code:#f5f5f7;--accent:#0066cc;--side:#fff;--sel:#e8e8ed;--pill:#fff;
--kw:#ad3da4;--ty:#703daa;--str:#d12f1b;--num:#272ad8;--com:#707f8c;--fn:#3e8087}
@media (prefers-color-scheme:dark){:root:not([data-theme=light]){--bg:#000;--fg:#f5f5f7;--muted:#a1a1a6;--faint:#86868b;--line:#424245;--soft:#1d1d1f;--code:#1d1d1f;--accent:#2997ff;--side:#000;--sel:#333336;--pill:#4a4a4e;
--kw:#ff7ab2;--ty:#dabaff;--str:#ff8170;--num:#d9c97c;--com:#7f8c98;--fn:#67b7a4;color-scheme:dark}}
:root[data-theme=dark]{--bg:#000;--fg:#f5f5f7;--muted:#a1a1a6;--faint:#86868b;--line:#424245;--soft:#1d1d1f;--code:#1d1d1f;--accent:#2997ff;--side:#000;--sel:#333336;--pill:#4a4a4e;
--kw:#ff7ab2;--ty:#dabaff;--str:#ff8170;--num:#d9c97c;--com:#7f8c98;--fn:#67b7a4;color-scheme:dark}
*{box-sizing:border-box}html,body{margin:0}
body{font:17px/1.47 -apple-system,BlinkMacSystemFont,"SF Pro Text","Helvetica Neue",sans-serif;background:var(--bg);color:var(--fg);-webkit-font-smoothing:antialiased}
a{color:var(--accent);text-decoration:none}a:hover{text-decoration:underline}
/* top bar */
.top{height:52px;border-bottom:1px solid var(--line);display:flex;align-items:center;gap:24px;padding:0 22px;position:sticky;top:0;background:color-mix(in srgb,var(--bg) 85%,transparent);backdrop-filter:saturate(180%) blur(20px);z-index:2}
.top .brand{font-weight:600;color:var(--fg);font-size:21px;letter-spacing:-.01em;white-space:nowrap}
.narrow{display:none!important}.side .filter .narrow{margin-top:10px}.top a{font-size:14px;color:var(--fg)}.top .sp{flex:1}
.theme{display:inline-flex;gap:2px;padding:2px;border-radius:999px;background:var(--soft);border:1px solid var(--line)}
.theme button{display:grid;place-items:center;width:28px;height:24px;border:0;border-radius:999px;background:none;color:var(--muted);cursor:pointer;transition:background .2s,color .2s}
.theme button:hover{color:var(--fg)}.theme button[aria-pressed=true]{background:var(--pill);color:var(--fg);box-shadow:0 1px 3px rgba(0,0,0,.15)}
.theme button:focus-visible{outline:2px solid var(--accent);outline-offset:1px}
.searchbtn{display:flex;align-items:center;gap:8px;height:32px;padding:0 8px 0 10px;border:1px solid var(--line);border-radius:8px;background:var(--soft);color:var(--muted);font:14px -apple-system,sans-serif;cursor:pointer;min-width:200px}
.searchbtn span{flex:1;text-align:left}.searchbtn kbd{font:12px -apple-system,sans-serif;border:1px solid var(--line);border-radius:4px;padding:0 5px;background:var(--bg)}
.searchbtn:hover{color:var(--fg)}.searchbtn:focus-visible{outline:2px solid var(--accent)}
dialog.search{width:min(680px,calc(100vw - 24px));margin:12vh auto 0;padding:0;border:1px solid color-mix(in srgb,var(--line) 70%,transparent);border-radius:18px;
background:color-mix(in srgb,var(--bg) 82%,transparent);backdrop-filter:saturate(180%) blur(30px);-webkit-backdrop-filter:saturate(180%) blur(30px);color:var(--fg);
box-shadow:0 24px 80px rgba(0,0,0,.28),0 2px 8px rgba(0,0,0,.08);overflow:hidden}
dialog.search[open]{animation:pop .16s ease-out}@keyframes pop{from{opacity:0;transform:scale(.97) translateY(-6px)}}
dialog.search::backdrop{background:rgba(0,0,0,.18)}
.sbar{display:flex;align-items:center;gap:12px;padding:0 18px;height:60px}.sbar svg{width:22px;height:22px;color:var(--muted);flex:none}
.sbar input{flex:1;min-width:0;border:0;outline:0;background:none;color:var(--fg);font:400 22px -apple-system,BlinkMacSystemFont,sans-serif;letter-spacing:-.01em}
.sbar input::-webkit-search-cancel-button{display:none}.sbar input::placeholder{color:var(--faint)}
.sbar kbd{font:500 11px -apple-system,sans-serif;color:var(--muted);border:1px solid var(--line);border-radius:5px;padding:2px 6px}
#sres{max-height:min(460px,60vh);overflow:auto;padding:0 8px}#sres:not(:empty){border-top:1px solid var(--line);padding:8px}
#sres .hit{display:flex;gap:12px;align-items:flex-start;padding:9px 12px;border-radius:10px;color:var(--fg)}
#sres .hit:hover{text-decoration:none}#sres .hit.sel{background:var(--accent);color:#fff}
#sres .hit .ic{margin-top:3px}#sres .hit.sel .ic{border-color:rgba(255,255,255,.8);color:#fff}
#sres .t{display:block;font-weight:600;font-size:15px}#sres .t i{font-style:normal;font-weight:400;color:var(--muted)}
#sres .x{display:block;font-size:13px;line-height:1.4;color:var(--muted);overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical}
#sres mark{background:none;color:inherit;font-weight:600}#sres .hit.sel .t i,#sres .hit.sel .x{color:rgba(255,255,255,.85)}
.scancel{display:none;border:0;background:none;color:var(--accent);font:17px -apple-system,sans-serif;padding:8px 0 8px 4px;cursor:pointer}
#sres .msg{padding:22px 12px;text-align:center;color:var(--muted);font-size:14px}
.top .ver{margin-left:-12px;font-size:12px;font-weight:500;color:var(--muted);border:1px solid var(--line);border-radius:999px;padding:1px 8px}.top .ver:hover{text-decoration:none;color:var(--fg)}
.skip{position:absolute;left:-9999px;top:8px;z-index:9;padding:8px 12px;border-radius:8px;background:var(--bg);color:var(--accent);border:1px solid var(--line)}.skip:focus{left:12px}
a:focus-visible,.side input:focus-visible{outline:2px solid var(--accent);outline-offset:2px;border-radius:4px}
@media(max-width:359px){.top .ver{display:none}}
main{view-transition-name:content}::view-transition-old(content),::view-transition-new(content){animation-duration:.14s}
::view-transition-old(root),::view-transition-new(root){animation:none}
@media (prefers-reduced-motion:reduce){*,*::before,*::after,::backdrop{transition:none!important;animation:none!important}}
/* sidebar */
.sidetoggle{display:grid;place-items:center;width:32px;height:32px;margin-left:-6px;border:0;border-radius:8px;background:none;color:var(--fg);cursor:pointer}
.sidetoggle:hover{background:var(--soft)}.sidetoggle:focus-visible{outline:2px solid var(--accent)}.sidetoggle svg{width:19px;height:19px}
.side{transition:margin-left .25s ease,visibility .25s}.noside .side{margin-left:-321px;visibility:hidden}
#tree[hidden]{display:none}.side .none{color:var(--muted);padding:5px 10px;margin:0}
.wrap{display:flex;min-height:calc(100vh - 52px)}
.side{width:320px;flex:none;border-right:1px solid var(--line);background:var(--side);position:sticky;top:52px;height:calc(100vh - 52px);display:flex;flex-direction:column;font-size:15px}
.side .scroll{flex:1;overflow:auto;padding:14px 12px 20px}
.side h4{margin:14px 10px 4px;font-size:15px;font-weight:600;color:var(--muted)}#tree h4:first-child{margin-top:4px}
.side a{display:flex;align-items:center;gap:8px;padding:5px 10px;border-radius:8px;color:var(--fg);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.side a:hover{text-decoration:none;background:var(--soft)}.side a.on{background:var(--sel);font-weight:600}
.ic{flex:none;display:inline-grid;place-items:center;width:15px;height:15px;border:1px solid var(--faint);border-radius:4px;font:600 10px/1 -apple-system,sans-serif;color:var(--faint)}
.side .filter{border-top:1px solid var(--line);padding:12px}
.side input{width:100%;padding:9px 12px;border:1px solid var(--line);border-radius:10px;background:var(--bg);color:var(--fg);font-size:15px}
.side input:focus{outline:2px solid var(--accent);outline-offset:-1px}
/* page */
main{flex:1;min-width:0}
.hero,.content{max-width:920px;padding-left:80px;padding-right:80px}
.hero{padding-top:44px}.hero,.content{overflow-wrap:break-word}
.crumbs{font-size:15px;color:var(--muted);margin-bottom:22px;overflow-wrap:anywhere}.crumbs a{color:var(--fg);text-decoration:underline;text-underline-offset:2px}.crumbs i{font-style:normal;margin:0 6px}
.eyebrow{color:var(--muted);font-size:21px;font-weight:500;margin-bottom:6px}
.hero h1{font-size:44px;line-height:1.08;margin:0 0 14px;font-weight:700;letter-spacing:-.015em;overflow-wrap:anywhere}
.abstract{font-size:21px;line-height:1.38;margin:0 0 14px;font-weight:500}
.avail{font-size:15px;color:var(--fg);margin-bottom:8px}.avail span+span::before{content:"|";color:var(--faint);margin:0 10px}
.content{padding-top:12px;padding-bottom:72px}
h2{font-size:34px;line-height:1.1;margin:48px 0 20px;padding-top:44px;border-top:1px solid var(--line);font-weight:700;letter-spacing:-.01em}
h2.first{border-top:0;padding-top:0;margin-top:16px}
h3{font-size:24px;margin:34px 0 14px;font-weight:600}
p{margin:0 0 16px}
code{font:.88em "SF Mono",ui-monospace,Menlo,monospace}p code,li code,td code{color:var(--fg)}:not(pre)>code{overflow-wrap:anywhere}
pre{background:var(--code);padding:14px 16px;border-radius:15px;overflow:auto;font-size:15px;line-height:1.66;margin:0 0 24px}
pre code{font-size:15px}.decl{margin:24px 0 0}
/* syntax colours (Xcode-like) */
code .k,code .m{color:var(--kw)}code .t{color:var(--ty)}code .s{color:var(--str)}code .n{color:var(--num)}code .c{color:var(--com);font-style:italic}
/* topics */
.topic{padding:0 0 18px}
.topic .sig{font:15px/1.6 "SF Mono",ui-monospace,Menlo,monospace;color:var(--muted);overflow-wrap:anywhere}.topic .sig b{font-weight:400;color:var(--accent)}
.topic .meta{color:var(--fg);font-size:16px;margin:2px 0 0 40px}.topic .meta code{color:var(--muted)}
table{border-collapse:collapse;width:100%;margin:12px 0 24px;font-size:15px}th,td{border-bottom:1px solid var(--line);padding:9px 8px;text-align:left;vertical-align:top}th{font-weight:600}
.grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(260px,100%),1fr));gap:4px 16px;margin:16px 0}.grid>a{min-width:0;overflow-wrap:anywhere}
.card{display:block;border:1px solid var(--line);border-radius:16px;padding:18px 20px;transition:border-color .2s}.card:hover{text-decoration:none;border-color:var(--faint)}
.card b{display:block;margin-bottom:4px;color:var(--fg)}.card span{color:var(--muted);font-size:14px}
.grid:has(.card){gap:14px}
#results a{padding:5px 10px}
footer{color:var(--muted);font-size:13px;padding:22px 80px;background:var(--soft);border-top:1px solid var(--line)}
@media(max-width:820px){table{display:block;overflow-x:auto}
dialog.search{width:100vw;max-width:100vw;height:100dvh;max-height:100dvh;margin:0;border:0;border-radius:0;background:var(--bg)}
.sbar{height:56px;padding:0 14px}.sbar input{font-size:18px}.sbar kbd{display:none}.scancel{display:block}
#sres{max-height:calc(100dvh - 56px)}#sres .x{-webkit-line-clamp:3}.wide{display:none!important}.narrow{display:flex!important}.top{gap:10px;padding:0 12px}.top .brand{font-size:18px}.top .ver{margin-left:-4px}
.side .filter .narrow{justify-content:center}.searchbtn{min-width:0}.searchbtn span,.searchbtn kbd{display:none}.side{position:fixed;left:0;z-index:3;margin-left:-321px;visibility:hidden;box-shadow:0 10px 30px rgba(0,0,0,.2)}.mside .side{margin-left:0;visibility:visible}.hero,.content,footer{padding-left:20px;padding-right:20px}.hero h1{font-size:34px}}
"""

def page_url(key):
    return "reference/%s.html" % slug(key)

def sidebar(up, current):
    out = ['<nav class="side" aria-label="Documentation"><div class="scroll"><div id="results"></div><div id="tree">',
           '<h4>Essentials</h4><a href="%sindex.html">Overview</a><a href="%sguide.html">Guide</a><a href="%sreference.html">All APIs</a>' % (up, up, up)]
    for title, keys in TOPICS:
        links = [k for k in keys if k in pages]
        if not links:
            continue
        out.append("<h4>%s</h4>" % esc(title))
        for k in links:
            name = pages[k]["title"]
            name = name if not k.startswith("modifier-") else name + "(_:)"
            out.append('<a href="%s%s"%s><span class="ic">%s</span>%s</a>' % (up, page_url(k), ' class="on" aria-current="page"' if k == current else "", "M" if k.startswith("modifier-") else kind_label(pages[k])[0], esc(name)))
    out.append('<h4>More</h4><a href="%sbenchmarks.html">Benchmarks</a><a href="%schangelog.html">Changelog</a><a href="%scontributing.html">Contributing</a><a class="narrow" href="%s">GitHub</a>' % (up, up, up, REPO))
    out.append("""</div></div><div class="filter"><input id="q" placeholder="Filter" autocomplete="off" aria-label="Filter"><div class="narrow">%s</div></div></nav>""" % THEME_SWITCH)
    return "".join(out)

SVG = '<svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">%s</svg>'
THEMES = [("system", '<rect x="2" y="4" width="20" height="13" rx="2"/><path d="M8 21h8M12 17v4"/>'),
          ("light", '<circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4"/>'),
          ("dark", '<path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>')]
THEME_SWITCH = '<div class="theme" role="group" aria-label="Appearance">%s</div>' % "".join(
    '<button type="button" data-t="%s" title="%s" aria-label="%s appearance">%s</button>' % (t, t.title(), t.title(), SVG % d) for t, d in THEMES)
SIDE_TOGGLE = '<button type="button" class="sidetoggle" title="Toggle sidebar" aria-label="Toggle sidebar">%s</button>' % (
    SVG % '<rect x="3" y="4" width="18" height="16" rx="2.5"/><path d="M9 4v16M5.5 8h1.5M5.5 11h1.5"/>')

SEARCH_BOX = ('<button type="button" class="searchbtn" aria-label="Search the documentation">%s<span>Search</span><kbd>⌘K</kbd></button>'
              '<dialog class="search" aria-label="Search"><div class="sbar">%s<input id="sq" type="search" placeholder="Search Compose SwiftUI" aria-label="Search Compose SwiftUI"'
              ' role="combobox" aria-expanded="false" aria-autocomplete="list" aria-controls="sres" autocomplete="off" spellcheck="false"><kbd>esc</kbd><button type="button" class="scancel">Cancel</button></div><div id="sres" role="listbox"></div></dialog>'
              % (SVG % '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>', SVG % '<circle cx="11" cy="11" r="7"/><path d="m20 20-3.5-3.5"/>'))

SUMMARY = "Write SwiftUI screens in Kotlin with the Compose runtime, and keep your view models."

def layout(title, body, depth=0, current=None, kind="Article", description=SUMMARY):
    up = "../" * depth
    description = esc(re.sub(r"[`*]", "", description))
    meta = ('<meta name="description" content="%s"><meta property="og:type" content="website"><meta property="og:site_name" content="Compose SwiftUI">'
            '<meta property="og:title" content="%s"><meta property="og:description" content="%s"><meta property="og:image" content="%sog-image.png">'
            '<meta name="twitter:card" content="summary_large_image">' % (description, esc(title), description, SITE))
    # The theme and the sidebar state are applied in <head>, before first paint, to avoid a flash.
    return """<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>%s | Compose SwiftUI</title>%s
<link rel="stylesheet" href="%sstyle.css?v=%s"><link rel="manifest" href="%smanifest.webmanifest">
<link rel="icon" href="%sicon-192.png"><link rel="apple-touch-icon" href="%sicon-192.png">
<meta name="theme-color" content="#ffffff" media="(prefers-color-scheme:light)"><meta name="theme-color" content="#000000" media="(prefers-color-scheme:dark)">
<script>try{const t=localStorage.getItem('theme');if(t)document.documentElement.dataset.theme=t;if(localStorage.getItem('side')==='closed')document.documentElement.classList.add('noside')}catch(e){}</script>
<link rel="alternate" type="text/plain" title="LLM documentation" href="%sllms.txt">
<script src="%sapp.js?v=%s" defer></script></head><body><a class="skip" href="#main">Skip to content</a>
<div class="top">%s<a class="brand" href="%sindex.html">Compose SwiftUI</a><a class="ver" href="%s/releases/tag/%s" title="Latest release">%s</a><span class="sp"></span>%s<a class="wide" href="%sguide.html">Guide</a><a class="wide" href="%sreference.html">API</a><a class="wide" href="%s">GitHub</a><span class="wide">%s</span></div>
<div class="wrap">%s<main id="main" data-pagefind-body data-pagefind-meta="kind:%s">%s<footer data-pagefind-ignore>SwiftUI is a trademark of Apple Inc. Each reference page links to Apple's documentation of the SwiftUI API it maps. <a href="%slegal.html">Legal and privacy</a></footer></main></div></body></html>
""" % (esc(title), meta, up, ASSETS_V, up, up, up, up, up, ASSETS_V, SIDE_TOGGLE, up, REPO, VERSION, esc(VERSION), SEARCH_BOX, up, up, REPO, THEME_SWITCH, sidebar(up, current), kind, body, up)

def write(rel, content):
    path = os.path.join(OUT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, "w").write(content)

def chips(avail):
    return "".join("<span>%s</span>" % esc(a.strip()) for a in avail.split(",") if a.strip())

def crumbs(up, *parts):
    """Breadcrumb trail: (label, href) pairs, the last one being the current page."""
    links = ['<a href="%s%s">%s</a>' % (up, h, esc(t)) for t, h in parts[:-1]]
    return '<nav class="crumbs" data-pagefind-ignore>%s<i>/</i>%s</nav>' % ("<i>/</i>".join(links), esc(parts[-1][0]))

def sig_html(k):
    """Topic signature: declared names and parameter labels in the accent colour, the rest muted."""
    return re.sub(r"\b([A-Za-z_]\w*)(?=\(|:)", r"<b>\1</b>", esc(k))

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
index_body = """<div class="hero"><div class="eyebrow" data-pagefind-ignore>Framework</div><h1>Compose SwiftUI</h1>
<p class="abstract">Write SwiftUI screens in Kotlin with the Compose runtime, and keep your view models.</p>
<div class="avail" data-pagefind-ignore><span>iOS 17.0+</span><span>macOS 14.0+</span><span>tvOS 17.0+</span><span>watchOS 10.0+</span></div></div>
<div class="content"><h2>Overview</h2>%s<h2>Topics</h2><div class="grid" data-pagefind-ignore>%s</div>
<p><a href="reference.html">Browse all %d APIs</a> · <a href="guide.html">Read the guide</a></p></div>""" % (intro_html, cards, len(entries))
write("index.html", layout("Compose SwiftUI", index_body))

toc = "".join('<li><a href="#%s">%s</a></li>' % (slug(t.lower()), esc(t)) for t, _ in guide_parts)
guide_body = '<div class="hero">' + crumbs("", ("Compose SwiftUI", "index.html"), ("Guide", "")) + '<div class="eyebrow" data-pagefind-ignore>Article</div><h1>Guide</h1><p class="abstract">Set up Compose SwiftUI, write screens and use it with other Compose libraries.</p></div><div class="content"><ul>%s</ul>%s</div>' % (
    toc, "".join('<h2 id="%s">%s</h2>%s' % (slug(t.lower()), esc(t), markdown(b)) for t, b in guide_parts))
write("guide.html", layout("Guide", guide_body, description="Set up Compose SwiftUI, write screens and use it with other Compose libraries."))

def article_page(title, abstract, md_html):
    return layout(title, article(title, abstract, md_html), description=abstract)

def article(title, abstract, md_html):
    return '<div class="hero">%s<div class="eyebrow" data-pagefind-ignore>Article</div><h1>%s</h1><p class="abstract">%s</p></div><div class="content">%s</div>' % (
        crumbs("", ("Compose SwiftUI", "index.html"), (title, "")), esc(title), esc(abstract), md_html)

LEGAL = """## Publisher

This site documents Compose SwiftUI, an open-source project published by Elie Gambache. Contact: [GitHub issues](%(repo)s/issues).

## Hosting

GitHub Pages, operated by GitHub, Inc., 88 Colin P. Kelly Jr. Street, San Francisco, CA 94107, USA.

## License

The source code and this documentation are under the [MIT License](%(repo)s/blob/main/LICENSE).

## Trademarks

SwiftUI, Swift, Xcode, iOS, macOS, tvOS and watchOS are trademarks of Apple Inc. Compose SwiftUI is not affiliated with or endorsed by Apple. Other names are trademarks of their respective owners.

## Privacy

- No cookies, no analytics, no account.
- Your appearance and sidebar choices are stored in your browser and never sent anywhere.
- A service worker keeps a copy of the site in your browser so it works offline. Clearing the site data removes it.
- Pages are served by GitHub Pages. Like any web server, it receives your IP address and browser details: see the [GitHub privacy statement](https://docs.github.com/site-policy/privacy-policies/github-general-privacy-statement).
""" % {"repo": REPO}
write("legal.html", article_page("Legal and privacy", "Who publishes this site, and what it does with your data.", markdown(LEGAL)))

perf = dict(sections(readme)).get("Performance", "")
results = open(os.path.join(ROOT, "docs/bench/RESULTS.md")).read()
write("benchmarks.html", article_page("Performance", "How Compose SwiftUI compares with plain SwiftUI.", markdown(perf) + markdown(results.split("\n", 1)[1])))
write("changelog.html", article_page("Changelog", "Changes in each release.", markdown(open(os.path.join(ROOT, "CHANGELOG.md")).read().split("\n", 1)[1])))
write("contributing.html", article_page("Contributing", "Regenerate the bindings, run the benchmarks, publish a release.", markdown(open(os.path.join(ROOT, "docs/CONTRIBUTING.md")).read().split("\n", 1)[1])))

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
    body = ['<div class="hero">%s<div class="eyebrow" data-pagefind-ignore>%s</div><h1>%s</h1><p class="abstract">%s</p><div class="avail" data-pagefind-ignore>%s</div></div><div class="content">'
            % (crumbs("../", ("Compose SwiftUI", "index.html"), ("APIs", "reference.html"), (display, "")),
               kind_label(p), esc(display), inline(abstract), chips(primary["avail"]))]
    body.append(code_block(primary["kotlin"], "kotlin", "decl"))
    body.append("<h2>Overview</h2>")
    if overview:
        body.append("<p>%s</p>" % inline(overview))
    if example:
        body.append(code_block(example, "kotlin"))
    elif primary["kind"] in ("view", "modifier") and sketch(primary):
        body.append(code_block(sketch(primary), "kotlin"))
    body.append('<p>This is the Kotlin form of SwiftUI <code>%s</code>: <a href="%s" target="_blank" rel="noopener">SwiftUI documentation</a>. '
                'In Swift it reads:</p>%s' % (esc(primary["swift"]), apple_url(primary, p), code_block(primary["swiftDecl"], "swift")))
    body.append("<h2>Topics</h2>")
    for g, items in topic_groups(p):
        body.append("<h3>%s</h3>" % esc(g))
        for e in items:
            meta = "SwiftUI <code>%s</code>" % esc(e["swift"])
            if e["avail"] and e["avail"] != primary["avail"]:
                meta += " · " + esc(e["avail"])
            if e["notes"]:
                meta += " · " + inline(e["notes"])
            body.append('<div class="topic"><div class="sig">%s</div><div class="meta">%s</div></div>' % (sig_html(e["kotlin"]), meta))
    body.append("</div>")
    write(rel, layout(display, "".join(body), depth=1, current=key, kind=kind_label(p), description=abstract))

ref = ['<div class="hero">' + crumbs("", ("Compose SwiftUI", "index.html"), ("All APIs", "")) + '<div class="eyebrow" data-pagefind-ignore>API Collection</div><h1>All APIs</h1><p class="abstract">Every SwiftUI API that '
       'Compose SwiftUI supports, generated from Apple\'s SDK and compiled for iOS, macOS, tvOS and watchOS.</p></div><div class="content">',
       '<p>APIs with a Kotlin-specific shape (navigation paths, item presentations, gestures, lazy lists, mixed-style text) are '
       'described in the <a href="guide.html#writing-screens">guide</a>.</p>']
for cat in ("Views", "Modifiers", "Types"):
    ref.append("<h2>%s</h2><div class=\"grid\" data-pagefind-ignore>%s</div>" % (cat, "".join(
        '<a href="%s">%s</a>' % (rel, esc(t + ("(_:)" if cat == "Modifiers" else ""))) for t, rel in cats[cat])))
ref.append("</div>")
write("reference.html", layout("All APIs", "".join(ref), description="Every SwiftUI API that Compose SwiftUI supports."))
write("search.json", json.dumps(search, separators=(",", ":")))
print("entries=%d pages=%d views=%d modifiers=%d types=%d" % (len(entries), len(pages), len(cats["Views"]), len(cats["Modifiers"]), len(cats["Types"])))


# ---- offline app: shared script, manifest, icons and service worker --------------------------------------------

APP_JS = r"""// Shared client code: theme, sidebar, filter, single-page navigation and offline support.
const BASE = new URL('.', document.currentScript.src);
const root = document.documentElement;
const narrow = matchMedia('(max-width:820px)');

function store(k, v) { try { v == null ? localStorage.removeItem(k) : localStorage.setItem(k, v) } catch (e) {} }
const escHtml = s => s.replace(/[&<>"]/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;'})[c]);

function setTheme(t) {
  if (t === 'system') root.removeAttribute('data-theme'); else root.dataset.theme = t;
  store('theme', t === 'system' ? null : t);
  syncTheme();
}
function syncTheme() {
  const t = root.dataset.theme || 'system';
  document.querySelectorAll('.theme button').forEach(b => b.setAttribute('aria-pressed', b.dataset.t === t));
}

// Wide screens collapse the sidebar (remembered); narrow screens open it as an overlay.
function toggleSidebar() {
  if (narrow.matches) root.classList.toggle('mside');
  else store('side', root.classList.toggle('noside') ? 'closed' : null);
}

let index = null;
async function filter(query) {
  const s = query.trim().toLowerCase(), res = document.getElementById('results'), tree = document.getElementById('tree');
  if (!s) { res.innerHTML = ''; tree.hidden = false; return }
  index ??= await (await fetch(new URL('search.json', BASE))).json();
  tree.hidden = true;
  const hits = index.filter(e => e.n.toLowerCase().includes(s))
    .sort((a, b) => a.n.toLowerCase().indexOf(s) - b.n.toLowerCase().indexOf(s) || a.n.length - b.n.length).slice(0, 50);
  res.innerHTML = hits.map(e => `<a href="${new URL(e.u, BASE)}">${escHtml(e.n)}</a>`).join('') || '<p class="none">No results</p>';
}

const isMac = /Mac|iPhone|iPad/.test(navigator.platform);

// Single-page navigation. The shell (top bar, sidebar, search) is rendered once and stays; a link only swaps the
// content of <main>. Pages come from the service worker's cache (or are prefetched on hover), so a swap is instant.
const pages = new Map();  // url -> Promise of the page's HTML
const pageKey = url => url.origin + url.pathname;
const load = url => {
  const key = pageKey(url);
  if (!pages.has(key)) pages.set(key, fetch(key).then(r => r.ok ? r.text() : Promise.reject(r.status)).catch(e => { pages.delete(key); throw e }));
  return pages.get(key);
};
const internal = a => {
  const url = new URL(a.href);
  return url.origin === location.origin && url.pathname.startsWith(BASE.pathname) && !a.target && !/\.(txt|json|xml|png)$/.test(url.pathname) ? url : null;
};
function markCurrent() {
  const path = location.pathname.replace(/\/$/, '/index.html');
  const side = document.querySelector('.side');
  side.querySelectorAll('a.on').forEach(a => { a.classList.remove('on'); a.removeAttribute('aria-current') });
  const current = [...side.querySelectorAll('#tree a[href]')].find(a => new URL(a.href).pathname.replace(/\/$/, '/index.html') === path);
  current?.classList.add('on');
  current?.setAttribute('aria-current', 'page');
  current?.scrollIntoView({block: 'nearest'});
}
let here = location.pathname;
async function go(url, push) {
  let doc;
  try { doc = new DOMParser().parseFromString(await load(url), 'text/html') } catch (e) { location.href = url; return }
  const swap = () => {
    // The URL changes together with the content, so relative links always match the page they are on.
    if (push) {
      history.replaceState({y: scrollY}, '');  // where to come back to with the back button
      history.pushState({y: 0}, '', url);
    }
    here = location.pathname;
    document.title = doc.title;
    document.querySelector('main').replaceWith(doc.querySelector('main'));
    root.classList.remove('mside');
    markCurrent();
    const target = url.hash && document.getElementById(decodeURIComponent(url.hash.slice(1)));
    target ? target.scrollIntoView() : scrollTo(0, push ? 0 : history.state?.y ?? 0);
  };
  if (document.startViewTransition && !matchMedia('(prefers-reduced-motion:reduce)').matches) document.startViewTransition(swap);
  else swap();
  checkForUpdate();
}
history.scrollRestoration = 'manual';
addEventListener('popstate', () => { if (location.pathname !== here) go(new URL(location.href), false) });

// Prefetch on intent: hover, touch or keyboard focus.
const prefetch = e => { const a = e.target.closest?.('a[href]'), url = a && internal(a); if (url && url.pathname !== location.pathname) load(url).catch(() => {}) };
document.addEventListener('pointerover', prefetch);
document.addEventListener('touchstart', prefetch, {passive: true});
document.addEventListener('focusin', prefetch);

// The shell's links are written relative to the first page; make them absolute once so they survive navigation.
document.querySelectorAll('.top a[href], .side a[href]').forEach(a => a.setAttribute('href', a.href));
syncTheme();
if (!isMac) document.querySelectorAll('.searchbtn kbd').forEach(k => k.textContent = 'Ctrl K');
document.querySelector('.side a.on')?.scrollIntoView({block: 'nearest'});

document.addEventListener('click', e => {
  const button = e.target.closest('button');
  if (button?.dataset.t) return setTheme(button.dataset.t);
  if (button?.classList.contains('sidetoggle')) return toggleSidebar();
  const a = e.target.closest('a[href]'), url = a && internal(a);
  if (!url || e.button || e.metaKey || e.ctrlKey || e.shiftKey || e.altKey) return;
  if (url.pathname === location.pathname && url.hash) return;  // in-page anchor: the browser scrolls
  e.preventDefault();
  go(url, true);
});
// Search: a Spotlight-like dialog over Pagefind's JS API (the index is built with the site, so it also works offline).
let pagefind = null, searchSeq = 0;
const getPagefind = async () => pagefind ??= await import(new URL('pagefind/pagefind.js', BASE).href).then(async pf => {
  await pf.options({baseUrl: BASE.pathname});
  return pf;
});
const badge = kind => kind === 'Article' ? '¶' : kind === 'View modifier' ? 'M' : (kind || '¶')[0];

function openSearch() {
  const dialog = document.querySelector('dialog.search');
  if (!dialog.open) dialog.showModal();
  dialog.querySelector('#sq').select();
  getPagefind();  // warm up while the user types
}
async function runSearch(query) {
  const seq = ++searchSeq, list = document.getElementById('sres');
  if (!query.trim()) { list.innerHTML = ''; select(null); return }
  const pf = await getPagefind();
  const found = await pf.search(query);
  const norm = t => t.toLowerCase().replace(/[^a-z0-9]/g, ''), q = norm(query);
  const rank = d => { const t = norm(d.meta.title.replace(/\(.*$/, '')); return t === q ? 0 : t.startsWith(q) ? 1 : 2 };
  const hits = (await Promise.all(found.results.slice(0, 30).map(r => r.data())))
    .map((d, i) => [rank(d), i, d]).sort((a, b) => a[0] - b[0] || a[1] - b[1]).slice(0, 12).map(x => x[2]);
  if (seq !== searchSeq) return;  // a newer query is on its way
  list.innerHTML = hits.map((d, i) => {
    const sub = d.sub_results?.find(s => s.url !== d.url);  // a heading inside an article
    const title = sub ? `${escHtml(d.meta.title)} <i>› ${escHtml(sub.title)}</i>` : escHtml(d.meta.title);
    return `<a class="hit" id="hit-${i}" role="option" href="${(sub || d).url}"><span class="ic">${badge(d.meta.kind)}</span>` +
      `<span><span class="t">${title}</span><span class="x">${(sub || d).excerpt}</span></span></a>`;
  }).join('') || `<div class="msg">No results for “${escHtml(query)}”</div>`;
  select(list.querySelector('.hit'));
}
function select(hit) {
  document.querySelectorAll('#sres .hit.sel').forEach(h => { h.classList.remove('sel'); h.removeAttribute('aria-selected') });
  hit?.classList.add('sel');
  hit?.setAttribute('aria-selected', 'true');
  const input = document.getElementById('sq');
  input.setAttribute('aria-expanded', String(!!document.querySelector('#sres .hit')));
  if (hit) input.setAttribute('aria-activedescendant', hit.id); else input.removeAttribute('aria-activedescendant');
  hit?.scrollIntoView({block: 'nearest'});
}

document.addEventListener('keydown', e => {
  const typing = /^(INPUT|TEXTAREA)$/.test(document.activeElement?.tagName);
  if ((e.key === 'k' && (e.metaKey || e.ctrlKey)) || (e.key === '/' && !typing)) { e.preventDefault(); openSearch(); return }
  if (e.key === 'Escape') root.classList.remove('mside');
  if (e.target.id !== 'sq') return;
  const sel = document.querySelector('#sres .hit.sel');
  if (e.key === 'ArrowDown' || e.key === 'ArrowUp') {
    e.preventDefault();
    select((e.key === 'ArrowDown' ? sel?.nextElementSibling : sel?.previousElementSibling) || sel);
  } else if (e.key === 'Enter' && sel) {
    e.preventDefault();
    sel.click();
  }
});
document.addEventListener('mousemove', e => { const h = e.target.closest('#sres .hit'); if (h && !h.classList.contains('sel')) select(h) });
document.addEventListener('click', e => {
  // The overlay sidebar (narrow screens) closes on a tap outside it or on any link inside it.
  if (root.classList.contains('mside') && (!e.target.closest('.side, .sidetoggle') || e.target.closest('.side a[href]'))) root.classList.remove('mside');
  if (e.target.closest('.searchbtn')) openSearch();
  else if (e.target.closest('#sres .hit')) document.querySelector('dialog.search').close();  // same-page anchors don't swap the body
  else if (e.target.matches('dialog.search') || e.target.closest('.scancel')) document.querySelector('dialog.search').close();
});
document.addEventListener('input', e => { if (e.target.id === 'sq') runSearch(e.target.value) });
document.addEventListener('input', e => { if (e.target.id === 'q') filter(e.target.value) });
// Offline: the service worker serves the whole release from its cache. When a new release is published, the
// browser installs the new worker, which replaces the cache and takes over; the page then reloads onto it.
function checkForUpdate() { navigator.serviceWorker?.getRegistration().then(r => r?.update()).catch(() => {}) }
if ('serviceWorker' in navigator) {
  const hadWorker = !!navigator.serviceWorker.controller;
  navigator.serviceWorker.register(new URL('sw.js', BASE)).catch(() => {});
  navigator.serviceWorker.addEventListener('controllerchange', () => { if (hadWorker) location.reload() });
}

"""

SW_JS = r"""// Generated by docsite.py. VERSION is a hash of the release, so every publish changes this file: the browser
// installs it, it caches the new release, then deletes the previous one.
const PREFIX = 'composeswiftui-', VERSION = PREFIX + '%s', MANIFEST = '__files.json';
const FILES = %s;  // path -> content hash

// Files whose hash did not change are copied from the previous release's cache; the others are downloaded. A file
// that fails is skipped (it is fetched from the network when needed), so one bad request never blocks an update.
async function precache() {
  const cache = await caches.open(VERSION);
  let previous = null, known = {};
  for (const key of await caches.keys()) {
    if (!key.startsWith(PREFIX) || key === VERSION) continue;
    const c = await caches.open(key), m = await c.match(MANIFEST);
    if (m) { previous = c; known = await m.json(); break }
  }
  const queue = Object.entries(FILES);
  const worker = async () => {
    for (let next; (next = queue.shift());) {
      const [path, hash] = next;
      try {
        let res = known[path] === hash ? await previous.match(path) : null;
        res ??= await fetch(new Request(path, {cache: 'reload'}));
        if (res.ok) await cache.put(path, res);
      } catch (e) {}
    }
  };
  await Promise.all(Array.from({length: 8}, worker));
  await cache.put(MANIFEST, new Response(JSON.stringify(FILES)));
}

self.addEventListener('install', e => { self.skipWaiting(); e.waitUntil(precache()) });

self.addEventListener('activate', e => e.waitUntil(
  caches.keys()
    .then(keys => Promise.all(keys.filter(k => k.startsWith(PREFIX) && k !== VERSION).map(k => caches.delete(k))))
    .then(() => self.clients.claim())));

self.addEventListener('fetch', e => {
  if (e.request.method !== 'GET') return;
  e.respondWith(caches.open(VERSION)
    .then(c => c.match(e.request, {ignoreSearch: true}))
    .then(r => r || fetch(e.request)));
});
"""

def brand_png(width, height):
    """A white "</>" on a blue gradient, dependency-free: the app icon (full bleed, inside the maskable safe zone)
    and the Open Graph image."""
    k = min(width, height) / 512
    ox, oy = (width - 512 * k) / 2, (height - 512 * k) / 2
    segments = [((196, 168), (116, 256)), ((116, 256), (196, 344)), ((316, 168), (396, 256)), ((396, 256), (316, 344)),
                ((282, 150), (230, 362))]
    segments = [((ox + ax * k, oy + ay * k), (ox + bx * k, oy + by * k)) for (ax, ay), (bx, by) in segments]
    radius = 17 * k
    x0, x1 = int(ox + 95 * k), int(ox + 418 * k)  # the symbol's bounding box: no distance maths outside it
    y0, y1 = int(oy + 128 * k), int(oy + 384 * k)

    def dist(px, py, seg):
        (ax, ay), (bx, by) = seg
        dx, dy = bx - ax, by - ay
        t = max(0.0, min(1.0, ((px - ax) * dx + (py - ay) * dy) / (dx * dx + dy * dy)))
        return ((px - ax - t * dx) ** 2 + (py - ay - t * dy) ** 2) ** 0.5

    rows = []
    for y in range(height):
        f = y / height
        bg = bytes(int(c) for c in (10 + (0 - 10) * f, 132 + (102 - 132) * f, 255 + (204 - 255) * f))  # #0a84ff to the accent #0066cc
        row = bytearray(b"\0") + bg * width
        if y0 <= y <= y1:
            for x in range(x0, x1 + 1):
                alpha = max(0.0, min(1.0, radius - min(dist(x + .5, y + .5, sg) for sg in segments) + .5))
                if alpha:
                    row[1 + 3 * x:4 + 3 * x] = bytes(int(c + (255 - c) * alpha) for c in bg)
        rows.append(bytes(row))

    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)
    return (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
            + chunk(b"IDAT", zlib.compress(b"".join(rows), 9)) + chunk(b"IEND", b""))

write("app.js", APP_JS)
for n in (192, 512):
    open(os.path.join(OUT, "icon-%d.png" % n), "wb").write(brand_png(n, n))
open(os.path.join(OUT, "og-image.png"), "wb").write(brand_png(1200, 630))
write("manifest.webmanifest", json.dumps({
    "name": "Compose SwiftUI", "short_name": "Compose SwiftUI",
    "description": "Documentation of Compose SwiftUI: SwiftUI screens in Kotlin.",
    "start_url": "./", "scope": "./", "display": "standalone", "background_color": "#ffffff", "theme_color": "#ffffff",
    "icons": [{"src": "icon-192.png", "sizes": "192x192", "type": "image/png"},
              {"src": "icon-512.png", "sizes": "512x512", "type": "image/png"},
              {"src": "icon-512.png", "sizes": "512x512", "type": "image/png", "purpose": "maskable"}]}, indent=1))

html_pages = sorted(os.path.relpath(f, OUT) for f in glob.glob(os.path.join(OUT, "**", "*.html"), recursive=True))
write("sitemap.xml", '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n%s</urlset>\n'
      % "".join("<url><loc>%s%s</loc></url>\n" % (SITE, "" if f == "index.html" else f) for f in html_pages))
write("robots.txt", "User-agent: *\nAllow: /\nSitemap: %ssitemap.xml\n" % SITE)

# ---- llms.txt (llmstxt.org): what an agent needs to write Compose SwiftUI code -----------------------------------

DEFAULT_AVAIL = "iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+"

def api_text(key, p):
    """One API page as plain text: every Kotlin declaration with the SwiftUI API it maps."""
    name = p["title"] + ("(_:)" if p["category"] == "Modifiers" else "")
    abstract = CONTENT.get(key, ("",))[0]
    lines = ["### %s" % name, ""] + ([abstract, ""] if abstract else [])
    for e in p["entries"]:
        extra = "".join(" · " + x for x in (e["avail"] if e["avail"] != DEFAULT_AVAIL else "", e["notes"]) if x)
        lines.append("- `%s` ← SwiftUI `%s`%s" % (collapse(e["kotlin"]), e["swift"], extra))
    return "\n".join(lines) + "\n"

summary = "Write SwiftUI screens in Kotlin with the Compose runtime: composables build a tree of real SwiftUI views."
rules = """Latest release: %s (Gradle plugin `id("dev.nucleusframework.compose-swiftui") version "%s"`).
Platforms: iOS 17.0+, macOS 14.0+, tvOS 17.0+, watchOS 10.0+ unless an API says otherwise.

Key rules for writing Compose SwiftUI code:
- Views are Kotlin functions named like SwiftUI (`Text`, `VStack`, `Button`...); Swift labels become named arguments (`spacing = 8.0`).
- Modifiers are a chain passed as `modifier = Modifier.padding().font(Font.title)`, applied in chain order as in Swift.
- Bindings are a value plus a change callback (`isOn = on, onIsOnChange = { on = it }`); state uses Compose `remember`/`mutableStateOf`.
- `CGFloat` is `Double`; leading-dot Swift members are qualified (`.secondary` → `ShapeStyle.secondary`).
- A screen is `fun myScreen() = composeSwiftUI { ... }`, shown from Swift with `ComposeView { MyScreenKt.myScreen() }`.
- Only APIs listed in the reference exist; for anything Kotlin-specific (navigation, sheets, lists, styled text) follow the guide.""" % (VERSION, VERSION.lstrip("v"))

llms = ["# Compose SwiftUI", "", "> " + summary, "", rules, "",
        "## Docs", "",
        "- [Full documentation for LLMs](%sllms-full.txt): the guide and every API mapping in one file" % SITE,
        "- [Guide](%sguide.html): setup, writing screens, previews, interop, how it works, limitations" % SITE,
        "- [All APIs](%sreference.html): every supported SwiftUI view, modifier and type" % SITE,
        "- [Source code](%s)" % REPO, ""]
for t, keys in TOPICS:
    links = [k for k in keys if k in pages]
    if links:
        llms += ["## %s" % t, ""] + ["- [%s](%s%s)" % (pages[k]["title"], SITE, page_url(k)) for k in links] + [""]
llms += ["## Optional", "", "- [Benchmarks](%sbenchmarks.html)" % SITE, "- [Changelog](%schangelog.html)" % SITE, ""]
write("llms.txt", "\n".join(llms))

full = ["# Compose SwiftUI", "", "> " + summary, "", rules, "", "# Guide", ""]
full += ["## %s\n\n%s\n" % (t, b.strip()) for t, b in guide_parts]
full += ["# API reference", "", "Format: `Kotlin declaration` ← SwiftUI API it maps · availability when it differs from the default · notes.", ""]
for cat in ("Views", "Modifiers", "Types"):
    full += ["## %s" % cat, ""] + [api_text(k, p) for k, p in sorted(pages.items(), key=lambda kv: kv[1]["title"].lower())
                                   if p["category"] == cat]
write("llms-full.txt", "\n".join(full))

subprocess.run(["npx", "--yes", PAGEFIND, "--site", OUT, "--quiet"], check=True)
for f in glob.glob(os.path.join(OUT, "pagefind", "pagefind-*ui*")) + glob.glob(os.path.join(OUT, "pagefind", "pagefind-highlight.js")):
    os.remove(f)  # Pagefind's own UIs: the site has its own search dialog

files = sorted(os.path.relpath(f, OUT) for f in glob.glob(os.path.join(OUT, "**", "*"), recursive=True)
               if os.path.isfile(f) and not f.endswith(".nojekyll"))
hashes = {f: hashlib.sha256(open(os.path.join(OUT, f), "rb").read()).hexdigest()[:12] for f in files}
hashes["./"] = hashes["index.html"]
release = hashlib.sha256(json.dumps(hashes, sort_keys=True).encode()).hexdigest()[:16]
write("sw.js", SW_JS % (release, json.dumps(hashes, separators=(",", ":"))))
