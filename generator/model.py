"""Loads SwiftUI symbol graphs (iOS + macOS) into a merged, queryable model."""

import glob
import json
import os

from swifttypes import decl_params, parse_type

PLATFORMS = ("ios", "macos")
DOMAIN = {"ios": "iOS", "macos": "macOS"}
DEPLOYMENT = {"ios": (17, 0), "macos": (14, 0)}


def _version(v):
    if not v:
        return None
    return (v.get("major", 0), v.get("minor", 0))


class Sym:
    def __init__(self, raw):
        self.usr = raw["identifier"]["precise"]
        self.kind = raw["kind"]["identifier"]
        self.path = tuple(raw["pathComponents"])
        self.title = raw["names"]["title"]
        self.decl = "".join(f["spelling"] for f in raw.get("declarationFragments", []))
        self.fragments = raw.get("declarationFragments", [])
        self.generics = raw.get("swiftGenerics") or {}
        self.extension = raw.get("swiftExtension") or {}
        self.fsig = raw.get("functionSignature")
        self.module = None
        # platform -> {"introduced": (maj, min) or None, "deprecated": bool}; absent = unavailable
        self.platforms = {}
        self.parent = None
        self.conforms = set()
        self.children = []
        self._params = None

    @property
    def dotted(self):
        return ".".join(self.path)

    @property
    def name(self):
        return self.path[-1].split("(")[0]

    @property
    def deprecated(self):
        """Hard-deprecated (a real deprecation version, not the soft `100000` marker)."""
        return any(p["deprecated"] is not None and p["deprecated"][0] < 1000 for p in self.platforms.values())

    def params(self):
        if self._params is None:
            if self.kind in ("swift.init", "swift.method", "swift.type.method", "swift.func", "swift.enum.case"):
                try:
                    name = "init" if self.kind == "swift.init" else self.name
                    if self.kind == "swift.enum.case":
                        self._params = _case_params(self.decl)
                    else:
                        self._params = decl_params(self.decl, name)
                except Exception:
                    self._params = None
            else:
                self._params = []
        return self._params

    def constraints(self):
        """Generic parameter name -> list of constraint protocol names; plus sameType map."""
        conf, same = {}, {}
        for src in (self.generics.get("constraints") or []) + (self.extension.get("constraints") or []):
            if src["kind"] == "conformance":
                conf.setdefault(src["lhs"], []).append(src["rhs"])
            elif src["kind"] == "sameType":
                same[src["lhs"]] = src["rhs"]
            elif src["kind"] == "superclass":
                conf.setdefault(src["lhs"], []).append(src["rhs"])
        names = {p["name"] for p in (self.generics.get("parameters") or [])}
        # the symbol graph sometimes drops constraints: also read the textual `where` clause
        if " where " in self.decl:
            clause = self.decl.rsplit(" where ", 1)[1]
            for part in clause.split(","):
                part = part.strip()
                if " : " in part:
                    lhs, rhs = [x.strip() for x in part.split(" : ", 1)]
                    for r in rhs.split("&"):
                        r = r.strip()
                        if r and r not in conf.get(lhs, []):
                            conf.setdefault(lhs, []).append(r)
                elif " == " in part:
                    lhs, rhs = [x.strip() for x in part.split(" == ", 1)]
                    same.setdefault(lhs, rhs)
        return names, conf, same

    def returns(self):
        """Declared return type string (methods / static funcs / properties)."""
        d = self.decl
        if self.kind in ("swift.property", "swift.type.property"):
            head = d.split("{")[0]
            return head.split(":", 1)[1].strip() if ":" in head else None
        if "->" in d:
            r = d.rsplit("->", 1)[1]
            r = r.split(" where ")[0]
            return r.strip()
        return None


def _case_params(decl):
    # `case name(label: Type, ...)` or `case name`
    if "(" not in decl:
        return []
    inner = decl[decl.index("(") + 1:decl.rindex(")")]
    out = []
    from swifttypes import split_top
    for i, raw in enumerate(split_top(inner)):
        raw = raw.strip()
        if ":" in raw and not raw.startswith("("):
            label, t = raw.split(":", 1)
            out.append((label.strip(), label.strip(), t.strip(), None, []))
        else:
            out.append(("_", "p%d" % i, raw, None, []))
    return out


class Model:
    def __init__(self, sg_root):
        self.syms = {}
        for plat in PLATFORMS:
            for f in sorted(glob.glob(os.path.join(sg_root, plat, "*.symbols.json"))):
                d = json.load(open(f))
                module = d["module"]["name"]
                for raw in d["symbols"]:
                    usr = raw["identifier"]["precise"]
                    s = self.syms.get(usr)
                    if s is None:
                        s = Sym(raw)
                        s.module = module
                        self.syms[usr] = s
                    elif plat == "ios":
                        # prefer the iOS spelling of the declaration
                        s.decl = "".join(fr["spelling"] for fr in raw.get("declarationFragments", []))
                    info = {"introduced": None, "deprecated": None}
                    available = True
                    for a in raw.get("availability") or []:
                        if a.get("domain") in (DOMAIN[plat], "*", None):
                            if a.get("isUnconditionallyUnavailable") or a.get("obsoleted"):
                                available = False
                            if a.get("isUnconditionallyDeprecated"):
                                info["deprecated"] = (0, 0)
                            elif a.get("deprecated") and a.get("domain") == DOMAIN[plat]:
                                info["deprecated"] = _version(a["deprecated"])
                            if a.get("domain") == DOMAIN[plat] and a.get("introduced"):
                                info["introduced"] = _version(a["introduced"])
                    if available:
                        s.platforms[plat] = info
                for r in d["relationships"]:
                    src = self.syms.get(r["source"])
                    if src is None:
                        continue
                    if r["kind"] == "memberOf":
                        src.parent = r["target"]
                    elif r["kind"] in ("conformsTo", "inheritsFrom"):
                        src.conforms.add(r.get("targetFallback", "") if r["target"] not in self.syms else r["target"])
                        src.conforms.add(r["target"])
        for s in self.syms.values():
            if s.parent in self.syms:
                self.syms[s.parent].children.append(s)
        self.by_path = {}
        for s in self.syms.values():
            if s.kind in ("swift.struct", "swift.enum", "swift.protocol", "swift.class", "swift.typealias"):
                self.by_path.setdefault(s.dotted, s)
        self.view_usr = self.by_path["View"].usr

    def lookup_type(self, name, scope=()):
        """Resolves a (possibly dotted) type name from the given enclosing path scope."""
        scope = list(scope)
        while True:
            cand = ".".join(scope + [name])
            if cand in self.by_path:
                return self.by_path[cand]
            if not scope:
                return None
            scope.pop()

    def conforms_to(self, sym, proto_usr, seen=None):
        seen = seen or set()
        if sym.usr in seen:
            return False
        seen.add(sym.usr)
        if proto_usr in sym.conforms:
            return True
        for c in sym.conforms:
            cs = self.syms.get(c)
            if cs is not None and cs.kind == "swift.protocol" and self.conforms_to(cs, proto_usr, seen):
                return True
        return False

    def all_protocols(self, sym, seen=None):
        """Transitive protocol conformances of a type (protocol USRs)."""
        out = set()
        stack = list(sym.conforms)
        while stack:
            u = stack.pop()
            if u in out:
                continue
            out.add(u)
            ps = self.syms.get(u)
            if ps is not None:
                stack.extend(ps.conforms)
        return out
