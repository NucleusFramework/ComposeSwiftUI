#!/usr/bin/env python3
"""
Generates the Compose SwiftUI bindings from the SwiftUI symbol graphs:
  - Kotlin: value types / protocols, @Composable views, Modifier extensions
  - Swift: decoders and renderers registered into ComposeRegistry
  - coverage.json: status of every public SwiftUI symbol

Usage: generate.py <symbolgraph-root> <project-root>
Items listed in generator/rejected.json (filled by verify.py from compiler errors) are skipped.
"""

import json
import os
import re
import sys

sys.path.insert(0, os.path.dirname(__file__))

from bridges import Bridge, Classifier, stand_in, Ctx, Unsupported, kotlin_name, kotlin_param_name, symbolize  # noqa: E402
import model as model_module  # noqa: E402
from model import DEPLOYMENT, OSNAME, Model  # noqa: E402

PLATFORMS = model_module.PLATFORMS
from swifttypes import parse_type  # noqa: E402

PKG = "dev.nucleusframework.composeswiftui"
FIRST_MEMBER_ID = 1000

# Types that stay views only (never Kotlin value classes).
VIEW_ONLY = {"Text", "Image", "AnyView", "EmptyView", "Namespace", "Never", "TupleView", "ModifiedContent",
             "Spacer", "Divider", "ProgressView", "Label"}
# Protocols that are never bridged as values.
PROTO_BLOCKLIST = {"View", "App", "Scene", "Commands", "ToolbarContent", "CustomizableToolbarContent",
                   "DynamicProperty", "PreferenceKey", "EnvironmentKey", "ViewModifier", "Gesture",
                   "Animatable", "VectorArithmetic", "TableColumnContent", "TableRowContent",
                   "AccessibilityRotorContent", "FocusedValueKey", "ContainerValueKey", "Layout",
                   "TextRenderer", "TextAttribute", "ShapeView", "Keyframes", "KeyframeTrackContent",
                   "WidgetConfiguration", "PreviewProvider", "UIViewRepresentable", "NSViewRepresentable",
                   "UIViewControllerRepresentable", "NSViewControllerRepresentable", "ToolbarItemContent",
                   "CustomAnimation", "AnimatableModifier", "GeometryEffect", "EnvironmentalModifier",
                   "DocumentGroupLaunchActions", "Transition"}


def vstr(v):
    return "%d.%d" % v


class Item:
    """A generated unit (type member, view init, modifier) with platform availability."""

    def __init__(self, key, sym):
        self.key = key
        self.sym = sym
        self.id = None
        self.platforms = {}  # plat -> introduced (tuple)

    def compute_platforms(self, gen, refs):
        plats = {}
        for p in PLATFORMS:
            if p not in self.sym.platforms:
                continue
            intro = self.sym.platforms[p]["introduced"] or (0, 0)
            ok = True
            for r in refs:
                tp = gen.type_platforms(r)
                if p not in tp:
                    ok = False
                    break
                intro = max(intro, tp[p])
            if ok:
                plats[p] = intro
        self.platforms = plats
        return plats


def platform_guard(plats):
    """(#if line or None, #available condition or None)."""
    if not plats:
        return None, None
    ifline = None
    if set(plats) != set(PLATFORMS):
        ifline = "#if " + " || ".join("os(%s)" % OSNAME[p] for p in sorted(plats))
    need = any(plats[p] > DEPLOYMENT[p] for p in plats)
    avail = None
    if need:
        parts = []
        for p in PLATFORMS:
            v = plats.get(p, DEPLOYMENT[p])
            parts.append("%s %s" % (OSNAME[p], vstr(max(v, DEPLOYMENT[p]))))
        avail = ", ".join(parts) + ", *"
    return ifline, avail


def avail_doc(plats):
    names = OSNAME
    out = []
    for p in PLATFORMS:
        if p in plats:
            out.append("%s %s+" % (names[p], vstr(max(plats[p], DEPLOYMENT[p]))))
        else:
            out.append("%s: unavailable (no-op)" % names[p])
    return ", ".join(out)


class ValueType:
    def __init__(self, sym):
        self.sym = sym
        self.usr = sym.usr
        self.kt_name = ".".join(sym.path)
        self.decoder = "d_" + "_".join(sym.path)
        self.members = []  # (Item, kind, params[(label, ktname, bridge, default)], extra)
        self.is_view = False


class ProtoType:
    def __init__(self, sym):
        self.sym = sym
        self.usr = sym.usr
        self.kt_name = ".".join(sym.path)
        self.decoder = "dp_" + "_".join(sym.path)
        self.conformers = []
        self.statics = []  # members declared on the protocol with Self == X


class Out:
    """Line buffer recording which lines belong to which item (for compiler error mapping)."""

    def __init__(self, path):
        self.path = path
        self.lines = []
        self.ranges = []

    def add(self, text, key=None):
        start = len(self.lines) + 1
        self.lines.extend(text.split("\n"))
        if key:
            self.ranges.append((start, len(self.lines), key))

    def write(self):
        os.makedirs(os.path.dirname(self.path), exist_ok=True)
        with open(self.path, "w") as f:
            f.write("\n".join(self.lines) + "\n")


class Generator:
    def __init__(self, sg_root, project):
        global PLATFORMS
        self.model = Model(sg_root)
        PLATFORMS = self.model.platforms
        self.project = project
        rej_path = os.path.join(project, "generator", "rejected.json")
        self.rejected = json.load(open(rej_path)) if os.path.exists(rej_path) else {}
        self.coverage = {}
        self.values = {}
        self.protos = {}
        self.classifier = Classifier(self.model, self.values, self.protos)
        self.views = []
        self.modifiers = []
        self.self_modifiers = {}  # view type usr -> [items]
        self.next_member = FIRST_MEMBER_ID
        self.line_map = []

    # ---- helpers ------------------------------------------------------------------------

    def status(self, sym, status, detail=""):
        prev = self.coverage.get(sym.usr)
        if prev and prev["status"] == "generated" and status != "generated":
            return
        self.coverage[sym.usr] = {"status": status, "detail": detail}

    def type_platforms(self, usr):
        s = self.model.syms[usr]
        out = {}
        for p, info in s.platforms.items():
            out[p] = info["introduced"] or (0, 0)
        return out

    def ctx_for(self, sym, attrs=()):
        names, conf, same = sym.constraints()
        # generic parameters of the enclosing type
        parent = self.model.syms.get(sym.parent)
        scope = sym.path[:-1]
        if parent is not None:
            pn, pc, ps = parent.constraints()
            names |= pn
            for k, v in pc.items():
                conf.setdefault(k, []).extend(v)
            for k, v in ps.items():
                same.setdefault(k, v)
        return Ctx(sym, scope, names, conf, same, attrs)

    def bridge_params(self, sym, drop_unsupported_defaults=True):
        """[(label, internal, bridge, default_str)] or raises Unsupported."""
        params = sym.params()
        if params is None:
            raise Unsupported("unparsable declaration")
        out = []
        for label, internal, type_str, default, attrs in params:
            try:
                t = parse_type(type_str)
            except Exception:
                if default is not None and drop_unsupported_defaults:
                    continue
                raise Unsupported("unparsable type %s" % type_str)
            try:
                b = self.classifier.classify(t, self.ctx_for(sym, attrs))
            except Unsupported as e:
                if default is not None and drop_unsupported_defaults:
                    continue  # omitted: Swift uses its default
                raise Unsupported("param %s: %s" % (internal, e))
            out.append((label, internal, symbolize(label, b), default))
        return out

    def kotlin_default(self, b, default):
        if default is None:
            return None
        d = default.strip()
        target = b.inner if b.kind == "optional" else b
        if d == "nil":
            return "null" if b.kind == "optional" else None
        if d.startswith("{") and d.endswith("}"):
            if b.kind in ("action", "async_action", "callback"):
                return "{}"
            return None
        if d == ".init()":
            d = "Self()"

        if d in ("true", "false"):
            return d if target.kind == "bool" else None
        if re.match(r"^-?\d+(\.\d+)?$", d):
            if target.kind in ("double", "cgfloat"):
                return d if "." in d else d + ".0"
            if target.kind == "float":
                return (d if "." in d else d + ".0") + "f"
            if target.kind == "int":
                return d if "." not in d else None
            return None
        if d.startswith('"') and target.kind in ("str", "text"):
            return d
        if d.startswith('"') and target.kind in ("lsk", "lsr"):
            return "LocalizedKey(%s)" % d
        if d == "[]" and b.kind in ("list", "set"):
            return "emptyList()" if b.kind == "list" else "emptySet()"
        if target.kind == "value":
            if not target.refs:
                return None
            vt = self.values[next(iter(target.refs))]
            m = re.match(r"^\.([A-Za-z_][A-Za-z0-9_]*)$", d)
            if m:
                for item, kind, params, _ in vt.members:
                    if kind in ("static", "case", "pstatic") and item.sym.name == m.group(1) and item.key not in self.rejected:
                        return "%s.%s" % (vt.kt_name, kotlin_name(m.group(1)))
                return None
            m = re.match(r"^\.([A-Za-z_][A-Za-z0-9_]*)\(\)$", d)
            if m:
                for item, kind, params, _ in vt.members:
                    if kind in ("sfunc", "casefn", "pfunc") and item.sym.name == m.group(1) and item.key not in self.rejected \
                            and all(p[3] is not None for p in params):
                        return "%s.%s()" % (vt.kt_name, kotlin_name(m.group(1)))
                return None
            m = re.match(r"^([A-Za-z_.]+)\(\)$", d)
            if m and (m.group(1).split(".")[-1] == vt.sym.name or m.group(1) == "Self"):
                for item, kind, params, _ in vt.members:
                    if kind == "init" and item.key not in self.rejected and all(p[3] is not None for p in params):
                        return "%s()" % vt.kt_name
            return None
        if target.kind == "proto":
            p = self.protos[next(iter(target.refs))]
            m = re.match(r"^\.([A-Za-z_][A-Za-z0-9_]*)$", d)
            if m:
                for item, vt, kind in p.statics:
                    if item.sym.name == m.group(1) and kind == "pstatic" and item.key not in self.rejected:
                        return "%s.%s" % (p.kt_name, kotlin_name(m.group(1)))
            return None
        return None

    # ---- value types & protocols ----------------------------------------------------------

    def candidate_types(self):
        out = []
        for s in self.model.syms.values():
            if s.kind not in ("swift.struct", "swift.enum"):
                continue
            if s.module not in ("SwiftUI", "SwiftUICore"):
                continue
            if s.generics.get("parameters"):
                continue
            if any(p in VIEW_ONLY for p in s.path[:1]) and len(s.path) == 1:
                continue
            if s.path[0].startswith("_") or any(c.startswith("_") for c in s.path):
                continue
            if "ke:" in s.usr:
                continue
            if "type:" + s.usr in self.rejected:
                continue
            out.append(s)
        return out

    def member_candidates(self, t):
        """Members of t producing a t value (and protocol statics with Self == t)."""
        res = []
        for m in t.children:
            if m.path[-1].startswith("_"):
                continue
            ret = m.returns()
            kind = None
            if m.kind == "swift.type.property" and ret in (t.name, "Self", t.dotted):
                kind = "static"
            elif m.kind == "swift.enum.case":
                kind = "case" if not m.params() else "casefn"
            elif m.kind == "swift.init" and not m.decl.lstrip().startswith(("init?", "nonisolated init?")) and "init?" not in m.decl:
                kind = "init"
            elif m.kind == "swift.type.method" and ret in (t.name, "Self", t.dotted):
                kind = "sfunc"
            elif m.kind == "swift.method" and ret in (t.name, "Self", t.dotted):
                kind = "method"
            if kind:
                res.append((m, kind))
        return res

    def protocol_statics(self):
        """[(sym, target_type_usr, kind)] for `extension P where Self == X { static ... }`."""
        out = []
        for s in self.model.syms.values():
            if s.kind not in ("swift.type.property", "swift.type.method"):
                continue
            parent = self.model.syms.get(s.parent)
            if parent is None or parent.kind != "swift.protocol":
                continue
            ret = s.returns() or ""
            if ret.startswith("some") or ret.startswith("any"):
                continue
            for c in s.extension.get("constraints") or []:
                if c["kind"] == "sameType" and c["lhs"] == "Self" and c.get("rhsPrecise") in self.model.syms:
                    out.append((s, c["rhsPrecise"], "pstatic" if s.kind == "swift.type.property" else "pfunc"))
        return out

    def build_types(self):
        cands = self.candidate_types()
        pstatics = self.protocol_statics()
        proto_syms = [s for s in self.model.syms.values() if s.kind == "swift.protocol"
                      and s.module in ("SwiftUI", "SwiftUICore") and s.name not in PROTO_BLOCKLIST
                      and not s.name.startswith("_") and "type:" + s.usr not in self.rejected]
        # fixpoint: a type is bridged when it has at least one constructible member
        bridged = set()
        while True:
            self.values.clear()
            self.protos.clear()
            for s in cands:
                if s.usr in bridged:
                    self.values[s.usr] = ValueType(s)
            for p in proto_syms:
                conformers = [u for u in bridged if self.model.conforms_to(self.model.syms[u], p.usr)]
                if conformers:
                    self.protos[p.usr] = ProtoType(p)
            new = set(bridged)
            for s in cands:
                if s.usr in new:
                    continue
                for m, kind in self.member_candidates(s):
                    if self._member_ok(m, kind, s):
                        new.add(s.usr)
                        break
                else:
                    for ps, target, kind in pstatics:
                        if target == s.usr and self._member_ok(ps, kind, s):
                            new.add(s.usr)
                            break
            if new == bridged:
                break
            bridged = new
        # members
        for vt in sorted(self.values.values(), key=lambda v: v.kt_name):
            for m, kind in sorted(self.member_candidates(vt.sym), key=lambda x: x[0].usr):
                self._add_member(vt, m, kind)
        for ps, target, kind in sorted(pstatics, key=lambda x: x[0].usr):
            if target in self.values:
                item = self._add_member(self.values[target], ps, kind)
                if item is not None and ps.parent in self.protos:
                    self.protos[ps.parent].statics.append((item, self.values[target], kind))
            else:
                self.status(ps, "skipped", "result type not bridged")
        for p in self.protos.values():
            p.conformers = sorted([vt for vt in self.values.values() if self.model.conforms_to(vt.sym, p.usr)],
                                  key=lambda v: v.kt_name)
        for vt in self.values.values():
            vt.is_view = self.model.conforms_to(vt.sym, self.model.view_usr)

    def _member_ok(self, m, kind, t):
        if not m.platforms:
            return False
        if "member:" + m.usr in self.rejected:
            return False
        try:
            self.bridge_params(m)
            return True
        except Unsupported:
            return False

    def _add_member(self, vt, m, kind):
        key = "member:" + m.usr
        if key in self.rejected:
            self.status(m, "rejected", self.rejected[key])
            return None
        try:
            params = self.bridge_params(m)
        except Unsupported as e:
            self.status(m, "skipped", str(e))
            return None
        item = Item(key, m)
        refs = {vt.usr}
        for p in params:
            refs |= p[2].refs
        if not item.compute_platforms(self, refs):
            self.status(m, "skipped", "unavailable on the supported platforms")
            return None
        item.id = self.next_member
        self.next_member += 1
        vt.members.append((item, kind, params, None))
        self.status(m, "generated", "value member of %s" % vt.kt_name)
        return item

    # ---- views & modifiers ------------------------------------------------------------------

    def build_views(self):
        view_usr = self.model.view_usr
        kind_id = 1
        for s in sorted(self.model.syms.values(), key=lambda x: x.dotted):
            if s.kind != "swift.struct" or s.module not in ("SwiftUI", "SwiftUICore"):
                continue
            if not self.model.conforms_to(s, view_usr) or s.name.startswith("_") or len(s.path) != 1:
                continue
            if s.usr in self.values:
                # value + view (Color, shapes, gradients): one composable rendering a value
                item = Item("valueview:" + s.usr, s)
                if item.compute_platforms(self, {s.usr}) and item.key not in self.rejected:
                    item.id = kind_id
                    kind_id += 1
                    self.views.append((item, "valueview", self.values[s.usr], None))
                continue
            for init in sorted([c for c in s.children if c.kind == "swift.init"], key=lambda x: x.usr):
                key = "view:" + init.usr
                if key in self.rejected:
                    self.status(init, "rejected", self.rejected[key])
                    continue
                if "init?" in init.decl:
                    self.status(init, "skipped", "failable init")
                    continue
                try:
                    params = self.bridge_params(init)
                except Unsupported as e:
                    self.status(init, "skipped", str(e))
                    continue
                item = Item(key, init)
                refs = set()
                for p in params:
                    refs |= p[2].refs
                if not item.compute_platforms(self, refs):
                    self.status(init, "skipped", "unavailable on the supported platforms")
                    continue
                item.id = kind_id
                kind_id += 1
                self.views.append((item, "init", s, params))
                self.status(init, "generated", "view")
        # self-modifiers: methods of view types returning the same type (Image.resizable...)
        mod_id = 1
        for s in sorted(self.model.syms.values(), key=lambda x: x.dotted):
            if s.kind == "swift.method" and len(s.path) == 2 and s.path[0] == "View":
                mod_id = self._add_modifier(s, mod_id, None)
        for item, kind, vt, params in self.views:
            if kind != "init":
                continue
        view_types = {}
        for item, kind, s, params in self.views:
            if kind == "init" and not s.generics.get("parameters"):
                view_types[s.usr] = s
        existing = {(i.sym.name, tuple(b.kt for _, _, b, _ in p)): i for i, p in self.modifiers}
        for usr, s in sorted(view_types.items(), key=lambda x: x[1].dotted):
            for m in sorted(s.children, key=lambda x: x.usr):
                if m.kind == "swift.method" and m.returns() in (s.name, "Self"):
                    try:
                        params = self.bridge_params(m)
                    except Unsupported as e:
                        self.status(m, "skipped", str(e))
                        continue
                    sig = (m.name, tuple(b.kt for _, _, b, _ in params))
                    if sig in existing:
                        # Same Kotlin call as the View modifier: at the head of the chain Swift would pick
                        # the concrete type's method (Image.symbolRenderingMode, Text.font...), so the view
                        # factory applies it under the View modifier's id.
                        shared = Item("selfmod:" + m.usr, m)
                        refs = set()
                        for p in params:
                            refs |= p[2].refs
                        if shared.compute_platforms(self, refs) and shared.key not in self.rejected:
                            shared.id = existing[sig].id
                            shared.self_type = s
                            self.self_modifiers.setdefault(usr, []).append((shared, params))
                        self.status(m, "covered", "same as View.%s (applied on %s at the head of the chain)" % (m.name, s.name))
                        continue
                    before = len(self.modifiers)
                    mod_id = self._add_modifier(m, mod_id, s)
                    if len(self.modifiers) > before:
                        self.self_modifiers.setdefault(usr, []).append(self.modifiers[-1])
        return

    def _add_modifier(self, s, mod_id, self_type):
        key = "mod:" + s.usr
        if key in self.rejected:
            self.status(s, "rejected", self.rejected[key])
            return mod_id
        try:
            params = self.bridge_params(s)
        except Unsupported as e:
            self.status(s, "skipped", str(e))
            return mod_id
        item = Item(key, s)
        item.self_type = self_type
        refs = set()
        for p in params:
            refs |= p[2].refs
        if self_type is not None:
            refs.add(self_type.usr) if self_type.usr in self.values else None
        if not item.compute_platforms(self, refs):
            self.status(s, "skipped", "unavailable on the supported platforms")
            return mod_id
        item.id = mod_id
        self.modifiers.append((item, params))
        self.status(s, "generated", "modifier" if self_type is None else "%s modifier" % self_type.name)
        return mod_id + 1

    # ---- Kotlin emission --------------------------------------------------------------------

    PREPOSITIONS = {"_", "in", "of", "for", "at", "by", "with", "to", "from", "on", "into", "as", "using", "is"}

    def kt_name_for(self, label, internal):
        return kotlin_param_name(internal if label in self.PREPOSITIONS else label)

    NULL_DEFAULT_KINDS = {"value": None, "str": "String", "bool": "Bool", "int": "Int", "double": "Double",
                          "cgfloat": "CGFloat", "float": "Float"}

    def null_default(self, b, default):
        """True when a Swift default can't be written in Kotlin: Kotlin gets `T? = null`."""
        return default is not None and self.kotlin_default(b, default) is None and b.kind in self.NULL_DEFAULT_KINDS

    def kt_params(self, params, for_view=False):
        """Kotlin parameter declarations + encode expressions (in Swift order)."""
        decls, encs = [], []
        used = set()
        for label, internal, b, default in params:
            name = self.kt_name_for(label, internal)
            while name in used:
                name += "_"
            used.add(name)
            kd = self.kotlin_default(b, default)
            if self.null_default(b, default):
                decls.append((name, b.kt + "?", "null", b))
                encs.append(name)
                continue
            for kn, kt in b.kotlin_params(name):
                decls.append((kn, kt, kd if kn == name else None, b))
            encs.extend(b.kotlin_encode(name))
        return decls, encs

    def kdoc(self, sym, plats, extra=""):
        decl = sym.decl.replace("*/", "* /")
        lines = ["/**", " * SwiftUI `%s`" % sym.dotted, " *", " * ```swift", " * %s" % decl.strip(), " * ```",
                 " * Availability: %s" % avail_doc(plats)]
        if extra:
            lines.append(" * " + extra)
        lines.append(" */")
        if sym.deprecated:
            lines.append('@Deprecated("Deprecated in SwiftUI")')
        return "\n".join(lines)

    def emit_kotlin(self):
        root = os.path.join(self.project, "runtime", "src", "generated", "kotlin", "dev", "nucleusframework", "composeswiftui")
        outs = []
        header = "// GENERATED by generator/generate.py from the SwiftUI symbol graphs. Do not edit.\n" \
                 "@file:Suppress(\"unused\", \"UNCHECKED_CAST\", \"RedundantVisibilityModifier\", \"FunctionName\", \"ClassName\", \"PropertyName\", \"ObjectPropertyName\", \"RemoveRedundantQualifierName\")\n\n" \
                 "@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)\n\n" \
                 "package %s\n\nimport androidx.compose.runtime.Composable\nimport kotlin.native.HiddenFromObjC\n" % PKG
        # --- types: group by top-level name
        tree = {}
        for vt in self.values.values():
            tree.setdefault(vt.sym.path, {})["value"] = vt
        for p in self.protos.values():
            tree.setdefault(p.sym.path, {})["proto"] = p
        # ensure containers exist
        for path in list(tree):
            for i in range(1, len(path)):
                tree.setdefault(path[:i], {})
        tops = sorted({p[0] for p in tree})
        chunks = [tops[i::6] for i in range(6)]
        for ci, chunk in enumerate(chunks):
            out = Out(os.path.join(root, "GenTypes%d.kt" % ci))
            out.add(header)
            for top in chunk:
                self._emit_kt_type(out, (top,), tree, 0)
            outs.append(out)
        # --- views
        out = Out(os.path.join(root, "GenViews.kt"))
        out.add(header)
        seen = set()
        merged = {}

        def lsk(params):
            return any(b.kind in ("lsk", "lsr", "text") or (b.kind == "optional" and b.inner.kind in ("lsk", "lsr", "text")) for _, _, b, _ in (params or []))

        def asset(params):
            # asset-catalog image overloads lose the base name to `systemImage` ones
            return any(lbl == "image" for lbl, internal, _, _ in (params or []))
        for item, kind, s, params in sorted(self.views, key=lambda v: (v[0].sym.deprecated, stand_in(v[3]), lsk(v[3]), asset(v[3]) and v[2].name != "Image")):
            if kind == "valueview":
                vt = s
                pname = vt.sym.name[0].lower() + vt.sym.name[1:]
                sig = (vt.sym.name + "View", (vt.kt_name,))
                if sig in seen:
                    continue
                seen.add(sig)
                code = "%s\n@HiddenFromObjC\n@Composable\npublic fun %sView(%s: %s, modifier: Modifier = Modifier) {\n    SwiftView(%d, listOf(%s), modifier)\n}\n" % (
                    self.kdoc(vt.sym, item.platforms, "Renders the value as a view."), vt.sym.name, pname, vt.kt_name, item.id, pname)
                out.add(code, item.key)
                continue
            decls, encs = self.kt_params(params, for_view=True)
            builders = [(d, e) for d, e in zip(self._expand(params), encs)]
            # choose children slot
            slot_params = [(lbl, internal, b) for lbl, internal, b, _ in params if b.kind == "slot"]
            children = None
            for lbl, internal, b in slot_params:
                if internal == "content":
                    children = internal
            if children is None and len(slot_params) == 1:
                children = slot_params[0][1]
            req, opt, acts, slots = [], [], [], []
            args_exprs = []
            slot_names = []
            used = set()
            for (label, internal, b, default) in params:
                name = self.kt_name_for(label, internal)
                while name in used:
                    name += "_"
                used.add(name)
                if b.kind == "slot":
                    if internal == children:
                        slots.append(("content", "@Composable () -> Unit", None))
                    else:
                        slots.append((name, "@Composable () -> Unit", None))
                        args_exprs.append("SSlotRef(%d)" % len(slot_names))
                        slot_names.append(name)
                    continue
                kd = self.kotlin_default(b, default)
                if self.null_default(b, default):
                    opt.append((name, b.kt + "?", "null"))
                    args_exprs.append(name)
                    continue
                for kn, kt in b.kotlin_params(name):
                    entry = (kn, kt, kd if kn == name else None)
                    if b.is_function or kt.startswith("("):
                        acts.append(entry)
                    elif entry[2] is None:
                        req.append(entry)
                    else:
                        opt.append(entry)
                args_exprs.extend(b.kotlin_encode(name))
            ordered = req + [("modifier", "Modifier", "Modifier")] + opt + acts + slots
            name = s.name
            types = tuple(t.rstrip("?") for _, t, _ in ordered)
            suffixed = False
            if (name, types) in seen:
                # Kotlin cannot overload on parameter names: this variant is reachable through the merged
                # label-dispatch function `Name(label = …)`; the suffixed name stays as a deprecated alias.
                labels = [lbl for lbl, _, b, _ in params if lbl != "_" and b.kind != "slot"]
                if labels:
                    name = name + labels[0][0].upper() + labels[0][1:]
                    suffixed = True
            if (name, types) in seen:
                self.status(item.sym, "covered", "duplicate Kotlin signature")
                item.id_dup = True
                continue
            seen.add((name, types))
            variant = {"ordered": ordered, "args": args_exprs, "slots": slot_names, "children": children is not None,
                       "kind": item.id, "item": item, "name": name}
            merged.setdefault(s.name, {"base": [], "alias": []})["alias" if suffixed else "base"].append(variant)
            plist = ",\n    ".join("%s: %s%s" % (n, t, " = %s" % d if d is not None else "") for n, t, d in ordered)
            call = "SwiftView(%d, listOf(%s), modifier" % (item.id, ", ".join(args_exprs))
            if slot_names:
                call += ", slots = listOf(%s)" % ", ".join(slot_names)
            if children is not None:
                call += ", content = content"
            call += ")"
            deprecation = ""
            if suffixed and not item.sym.deprecated:
                deprecation = '@Deprecated("Use %s(%s = …)", level = DeprecationLevel.WARNING)\n' % (s.name, [n for n, _, _ in ordered if n != "modifier"][0])
            code = "%s\n%s@HiddenFromObjC\n@Composable\npublic fun %s(\n    %s,\n) {\n    %s\n}\n" % (self.kdoc(item.sym, item.platforms), deprecation, name, plist, call)
            out.add(code, item.key)
        for base_name, groups in sorted(merged.items()):
            if groups["alias"]:
                code = self._merged_view(base_name, groups["alias"])
                if code:
                    out.add(code)
        outs.append(out)
        # --- modifiers
        mods_out = [Out(os.path.join(root, "GenModifiers%d.kt" % i)) for i in range(4)]
        for o in mods_out:
            o.add(header)
        seen = set()
        merged_mods = {}

        def lsk_m(params):
            return any(b.kind in ("lsk", "lsr", "text") or (b.kind == "optional" and b.inner.kind in ("lsk", "lsr", "text")) for _, _, b, _ in params)
        for n, (item, params) in enumerate(sorted(self.modifiers, key=lambda x: (x[0].sym.deprecated, stand_in(x[1]), lsk_m(x[1])))):
            decls, encs = self.kt_params(params)
            types = tuple(t.rstrip("?") for _, t, _, _ in decls)
            mname = item.sym.name
            suffixed = False
            if (mname, types) in seen:
                labels = [lbl for lbl, _, b, _ in params if lbl != "_"]
                if labels:
                    mname = mname + labels[0][0].upper() + labels[0][1:]
                    suffixed = True
            if (mname, types) in seen:
                self.status(item.sym, "covered", "duplicate Kotlin signature")
                continue
            seen.add((mname, types))
            if suffixed:
                merged_mods.setdefault(item.sym.name, []).append(
                    {"ordered": [(kn, kt, kd) for kn, kt, kd, _ in decls], "encs": encs, "id": item.id})
            plist = ", ".join("%s: %s%s" % (kn, kt, " = %s" % kd if kd is not None else "") for kn, kt, kd, _ in decls)
            extra = ""
            if getattr(item, "self_type", None) is not None:
                extra = "Applies to `%s` views only." % item.self_type.name
            deprecation = ""
            if suffixed and not item.sym.deprecated and decls:
                deprecation = '@Deprecated("Use %s(%s = …)", level = DeprecationLevel.WARNING)\n' % (item.sym.name, decls[0][0])
            code = "%s\n%s@HiddenFromObjC\npublic fun Modifier.%s(%s): Modifier = element(%d%s)\n" % (
                self.kdoc(item.sym, item.platforms, extra), deprecation, kotlin_name(mname), plist, item.id,
                "".join(", " + e for e in encs))
            mods_out[n % 4].add(code, item.key)
        for base_name, variants in sorted(merged_mods.items()):
            code = self._merged_modifier(base_name, variants)
            if code:
                mods_out[0].add(code)
        outs.extend(mods_out)
        for o in outs:
            o.write()
            self.line_map.append((o.path, o.ranges))

    def _merged_modifier(self, base_name, variants):
        """`Modifier.name(labelA = …)` dispatching to the label variant whose parameters were given."""
        union, order, kept = {}, [], []
        def base_t(t):
            return t[:-1] if t.endswith("?") and not t.startswith("(") else t
        for v in variants:
            if any(n in union and union[n] != base_t(t) for n, t, d in v["ordered"]):
                continue
            kept.append(v)
            for n, t, d in v["ordered"]:
                if n not in union:
                    union[n] = base_t(t)
                    order.append(n)
        if not kept:
            return None
        def is_lambda(t):
            return t.startswith("(") or t.startswith("@Composable")
        names = sorted(order, key=lambda n: is_lambda(union[n]))
        plist = ", ".join("%s: %s = null" % (n, ("(%s)?" % union[n]) if is_lambda(union[n]) else union[n] + "?") for n in names)
        branches = []
        for v in sorted(kept, key=lambda v: (-len([1 for n, t, d in v["ordered"] if d is None and not t.endswith("?")]), len(v["ordered"]))):
            vn = {n for n, _, _ in v["ordered"]}
            required = [n for n, t, d in v["ordered"] if d is None and not t.endswith("?")]
            cond = " && ".join(["%s != null" % n for n in required] + ["%s == null" % n for n in names if n not in vn]) or "true"
            lets = ["val %s = %s!!" % (n, n) for n in required]
            lets += ["val %s = %s ?: %s" % (n, n, d) for n, t, d in v["ordered"] if n not in required and d is not None and d != "null"]
            body = "; ".join(lets + ["element(%d%s)" % (v["id"], "".join(", " + e for e in v["encs"]))])
            branches.append("        %s -> run { %s }" % (cond, body))
        branches.append('        else -> error("No SwiftUI %s overload takes this combination of arguments")' % base_name)
        return ("/** SwiftUI `%s` overloads that differ only by argument labels, selected by the labels you pass. */\n"
                "@HiddenFromObjC\n@Suppress(\"NAME_SHADOWING\")\npublic fun Modifier.%s(%s): Modifier =\n    when {\n%s\n    }\n") % (
            base_name, kotlin_name(base_name), plist, "\n".join(branches))

    def _merged_view(self, base_name, variants):
        """One `Name(labelA = …, labelB = …)` function dispatching at runtime to the label variant whose
        parameters were given: Kotlin can't overload on parameter names, Swift can."""
        union, order = {}, []
        kept = []
        for v in variants:
            ok = True
            for n, t, d in v["ordered"]:
                base_t = t[:-1] if t.endswith("?") and not t.startswith("(") else t
                if n in union and union[n] != base_t:
                    ok = False
            if not ok:
                continue
            kept.append(v)
            for n, t, d in v["ordered"]:
                base_t = t[:-1] if t.endswith("?") and not t.startswith("(") else t
                if n not in union:
                    union[n] = base_t
                    order.append(n)
        if not kept:
            return None
        def is_lambda(t):
            return t.startswith("(") or t.startswith("@Composable")
        values = [n for n in order if n != "modifier" and not is_lambda(union[n])]
        lambdas = [n for n in order if is_lambda(union[n]) and n != "content"]
        tail = ["content"] if "content" in union else []
        def decl(n):
            t = union[n]
            return "%s: %s = null" % (n, ("(%s)?" % t) if is_lambda(t) else t + "?")
        plist = ",\n    ".join([decl(n) for n in values] + ["modifier: Modifier = Modifier"] + [decl(n) for n in lambdas + tail])
        branches = []
        # most required params first; on ties the smallest variant first (exact match before optional extras)
        for v in sorted(kept, key=lambda v: (-len([1 for n, t, d in v["ordered"] if n != "modifier" and d is None and not t.endswith("?")]), len(v["ordered"]))):
            names = {n for n, _, _ in v["ordered"]} - {"modifier"}
            required = [n for n, t, d in v["ordered"] if n != "modifier" and d is None and not t.endswith("?")]
            others = [n for n in values + lambdas + tail if n not in names]
            cond = " && ".join(["%s != null" % n for n in required] + ["%s == null" % n for n in others]) or "true"
            lets = []
            for n, t, d in v["ordered"]:
                if n == "modifier":
                    continue
                if n in required:
                    lets.append("val %s = %s!!" % (n, n))
                elif d is not None and d != "null":
                    lets.append("val %s = %s ?: %s" % (n, n, d))
            call = "SwiftView(%d, listOf(%s), modifier" % (v["kind"], ", ".join(v["args"]))
            if v["slots"]:
                call += ", slots = listOf(%s)" % ", ".join(v["slots"])
            if v["children"]:
                call += ", content = content"
            call += ")"
            body = "; ".join(lets + [call])
            branches.append("        %s -> { %s }" % (cond, body))
        branches.append('        else -> error("No SwiftUI %s initializer takes this combination of arguments")' % base_name)
        doc = "/**\n * SwiftUI `%s` initializers that differ only by argument labels, selected by the labels you pass\n * (e.g. `Image(systemName = SFSymbol.star)`).\n */" % base_name
        return "%s\n@HiddenFromObjC\n@Composable\n@Suppress(\"NAME_SHADOWING\")\npublic fun %s(\n    %s,\n) {\n    when {\n%s\n    }\n}\n" % (
            doc, base_name, plist, "\n".join(branches))

    def _expand(self, params):
        out = []
        for label, internal, b, default in params:
            out.extend([b] * len(b.kotlin_encode(internal)))
        return out

    def _emit_kt_type(self, out, path, tree, depth):
        node = tree.get(path, {})
        ind = "    " * depth
        name = path[-1]
        vt = node.get("value")
        p = node.get("proto")
        children = sorted([k for k in tree if len(k) == len(path) + 1 and k[:len(path)] == path])
        body = []
        if vt is not None:
            supers = ["SExpr(id, args)"]
            for pu in sorted(self.model.all_protocols(vt.sym)):
                if pu in self.protos:
                    supers.append(self.protos[pu].kt_name)
            out.add("%s%s\n%s%spublic class %s internal constructor(id: Int, args: List<Any?>) : %s {" % (
                ind, self._indent(self.kdoc(vt.sym, self.type_platforms(vt.usr)), ind), ind, "@HiddenFromObjC " if depth == 0 else "", name, ", ".join(supers)), "type:" + vt.usr)
            statics, members = [], []
            seen_sigs = set()
            for item, kind, params, _ in sorted(vt.members, key=lambda x: (x[0].sym.deprecated, stand_in(x[2]))):
                if item.sym.parent != vt.usr and kind in ("pstatic", "pfunc"):
                    continue  # declared on the protocol companion
                if item.key in self.rejected:
                    continue
                decls, encs = self.kt_params(params)
                group = "static" if kind in ("static", "case", "sfunc", "casefn") else kind
                sig = (group, item.sym.name if kind != "init" else "<init>",
                       tuple(kt.rstrip("?") for _, kt, _, _ in decls) if kind not in ("static", "case") else ("<val>",))
                if sig in seen_sigs:
                    self.status(item.sym, "covered", "duplicate Kotlin signature")
                    continue
                seen_sigs.add(sig)
                plist = ", ".join("%s: %s%s" % (kn, kt, " = %s" % kd if kd is not None else "") for kn, kt, kd, _ in decls)
                args = "listOf(%s)" % ", ".join(encs) if encs else "emptyList()"
                doc = self._indent(self.kdoc(item.sym, item.platforms), ind + "        ")
                nm = kotlin_name(item.sym.name)
                if kind in ("static", "case"):
                    statics.append(("%s\n%s        public val %s: %s get() = %s(%d, emptyList())" % (doc, ind, nm, vt.kt_name, vt.kt_name, item.id), item.key))
                elif kind in ("sfunc", "casefn"):
                    statics.append(("%s\n%s        public fun %s(%s): %s = %s(%d, %s)" % (doc, ind, nm, plist, vt.kt_name, vt.kt_name, item.id, args), item.key))
                elif kind == "init":
                    members.append(("%s\n%s    public constructor(%s) : this(%d, %s)" % (doc[4:] if False else self._indent(self.kdoc(item.sym, item.platforms), ind + "    "), ind, plist, item.id, args), item.key))
                elif kind == "method":
                    margs = "listOf(%s)" % ", ".join(["this"] + encs)
                    members.append(("%s\n%s    public fun %s(%s): %s = %s(%d, %s)" % (self._indent(self.kdoc(item.sym, item.platforms), ind + "    "), ind, nm, plist, vt.kt_name, vt.kt_name, item.id, margs), item.key))
            for code, key in members:
                out.add(code, key)
            if statics:
                out.add("%s    public companion object {" % ind)
                for code, key in statics:
                    out.add(code, key)
                out.add("%s    }" % ind)
        elif p is not None:
            supers = [self.protos[u].kt_name for u in sorted(p.sym.conforms) if u in self.protos and u != p.usr]
            out.add("%s%s\n%s%spublic interface %s%s {" % (ind, self._indent(self.kdoc(p.sym, self.type_platforms(p.usr)), ind), ind, "@HiddenFromObjC " if depth == 0 else "", name,
                                                         (" : " + ", ".join(supers)) if supers else ""), "type:" + p.usr)
            if p.statics:
                out.add("%s    public companion object {" % ind)
                for item, target, kind in p.statics:
                    vt = target
                    params = [m[2] for m in vt.members if m[0] is item][0]
                    decls, encs = self.kt_params(params)
                    plist = ", ".join("%s: %s%s" % (kn, kt, " = %s" % kd if kd is not None else "") for kn, kt, kd, _ in decls)
                    args = "listOf(%s)" % ", ".join(encs) if encs else "emptyList()"
                    doc = self._indent(self.kdoc(item.sym, item.platforms), ind + "        ")
                    nm = kotlin_name(item.sym.name)
                    if kind == "pstatic":
                        out.add("%s\n%s        public val %s: %s get() = %s(%d, emptyList())" % (doc, ind, nm, vt.kt_name, vt.kt_name, item.id), item.key)
                    else:
                        out.add("%s\n%s        public fun %s(%s): %s = %s(%d, %s)" % (doc, ind, nm, plist, vt.kt_name, vt.kt_name, item.id, args), item.key)
                out.add("%s    }" % ind)
        else:
            out.add("%s/** Namespace for SwiftUI `%s` nested types. */\n%s%spublic object %s {" % (ind, ".".join(path), ind, "@HiddenFromObjC " if depth == 0 else "", name))
        for c in children:
            self._emit_kt_type(out, c, tree, depth + 1)
        out.add("%s}\n" % ind)

    @staticmethod
    def _indent(text, ind):
        return "\n".join(ind + l if l else l for l in text.split("\n")).lstrip()

    # ---- Swift emission ---------------------------------------------------------------------

    def swift_call_args(self, params, fallback, src="a", start=0, view_children=None):
        stmts, args = [], []
        self.opened = []
        i = start
        for n, (label, internal, b, default) in enumerate(params):
            if b.kind == "slot" and view_children is not None and internal == view_children:
                expr = "ChildrenView(m)" if b.bare else "{ ChildrenView(m) }"
            elif self.null_default(b, default):
                swift_t = b.conv[0] if b.conv else self.NULL_DEFAULT_KINDS[b.kind] or b.swift_type[2:].replace("_", ".")
                m = re.match(r"^([A-Z][A-Za-z0-9_]*)(.*)$", default)
                if m and b.kind == "value":
                    target = self.model.syms[next(iter(b.refs))] if b.refs else None
                    resolved = self.model.lookup_type(m.group(1), target.path if target else ())
                    if resolved is not None:
                        default = resolved.dotted + m.group(2)
                if b.kind == "value":
                    dec = "(%s(%s.expr(%d)) ?? %s)" % (b.swift_type, src, i, default)
                else:
                    _, dec = b.swift_decode(i, "p%d" % n, fallback, src)
                stmts.append("let p%d: %s = %s.isNil(%d) ? %s : %s" % (n, swift_t, src, i, default, dec))
                expr = "p%d" % n
                i += 1
            else:
                s, expr = b.swift_decode(i, "p%d" % n, fallback, src)
                stmts.extend(s)
                i += b.nargs
                if b.kind == "proto":
                    self.opened.append(("p%d" % n, "q%d" % n, b.proto_swift))
            args.append(expr if label == "_" else "%s: %s" % (label, expr))
        return stmts, args

    def open_call(self, result_type, expr):
        """Statements returning `expr`, opening protocol existentials through a local generic func."""
        if not self.opened:
            return ["return %s" % expr]
        gens = ", ".join("Q%s: %s" % (q[1:], proto) for _, q, proto in self.opened)
        ps = ", ".join("_ %s: Q%s" % (q, q[1:]) for _, q, _ in self.opened)
        return ["@MainActor func open<%s>(%s) -> %s {" % (gens, ps, result_type), "    return %s" % expr, "}",
                "return open(%s)" % ", ".join(p for p, _, _ in self.opened)]

    def swift_wrap(self, plats, body_lines, fallback_return, indent="        "):
        ifline, avail = platform_guard(plats)
        lines = []
        if avail:
            lines.append(indent + "if #available(%s) {" % avail)
            lines.extend(indent + "    " + l for l in body_lines)
            lines.append(indent + "}")
            lines.append(indent + fallback_return)
        else:
            lines.extend(indent + l for l in body_lines)
        return ifline, lines

    def emit_swift(self):
        root = os.path.join(self.project, "swift", "ComposeSwiftUI", "Generated")
        header = "// GENERATED by generator/generate.py from the SwiftUI symbol graphs. Do not edit.\nimport SwiftUI\nimport UniformTypeIdentifiers\n"
        outs = []
        # --- decoders
        dec_outs = [Out(os.path.join(root, "GenDecoders%d.swift" % i)) for i in range(6)]
        for o in dec_outs:
            o.add(header)
        for n, vt in enumerate(sorted(self.values.values(), key=lambda v: v.kt_name)):
            o = dec_outs[n % len(dec_outs)]
            tp = self.type_platforms(vt.usr)
            ifline, avail = platform_guard(tp)
            if ifline:
                o.add(ifline)
            attr = "@available(%s)\n" % avail if avail else ""
            swift_t = ".".join(vt.sym.path)
            o.add("%s@MainActor func %s(_ e: SExpr?) -> %s? {\n    guard let e else { return nil }\n    let a = Args(expr: e)\n    switch e.id {" % (
                attr, vt.decoder, swift_t), "type:" + vt.usr)
            for item, kind, params, _ in vt.members:
                try:
                    stmts, args = self.swift_call_args(params, "return nil", start=1 if kind == "method" else 0)
                except Unsupported as e:
                    self.rejected[item.key] = str(e)
                    continue
                if kind in ("static", "case", "pstatic"):
                    expr = ".%s" % self._swift_member(item.sym.name)
                elif kind in ("sfunc", "casefn", "pfunc"):
                    expr = ".%s(%s)" % (self._swift_member(item.sym.name), ", ".join(args))
                elif kind == "init":
                    expr = "%s(%s)" % (swift_t, ", ".join(args))
                else:
                    stmts = ["guard let base = %s(a.expr(0)) else { return nil }" % vt.decoder] + stmts
                    expr = "base.%s(%s)" % (self._swift_member(item.sym.name), ", ".join(args))
                body = stmts + self.open_call(swift_t + "?", expr)
                # availability relative to the enclosing decoder
                plats = dict(item.platforms)
                rel = {p: v for p, v in plats.items() if v > max(tp.get(p, (0, 0)), DEPLOYMENT[p])}
                case_if, _ = platform_guard({p: plats[p] for p in plats}) if set(plats) != set(tp) & set(PLATFORMS) else (None, None)
                lines = []
                if case_if:
                    lines.append("    " + case_if)
                lines.append("    case %d:" % item.id)
                if rel:
                    _, av = platform_guard({p: plats.get(p, DEPLOYMENT[p]) for p in PLATFORMS})
                    lines.append("        if #available(%s) {" % av)
                    lines.extend("            " + l for l in body)
                    lines.append("        }")
                    lines.append("        return nil")
                else:
                    lines.extend("        " + l for l in body)
                if case_if:
                    lines.append("    #endif")
                o.add("\n".join(lines), item.key)
            o.add("    default: return nil\n    }\n}")
            if ifline:
                o.add("#endif")
        # protocol decoders
        po = Out(os.path.join(root, "GenProtocolDecoders.swift"))
        po.add(header)
        for p in sorted(self.protos.values(), key=lambda x: x.kt_name):
            tp = self.type_platforms(p.usr)
            ifline, avail = platform_guard(tp)
            if ifline:
                po.add(ifline)
            attr = "@available(%s)\n" % avail if avail else ""
            po.add("%s@MainActor func %s(_ e: SExpr?) -> (any %s)? {\n    guard let e else { return nil }\n    switch e.id {" % (
                attr, p.decoder, ".".join(p.sym.path)), "type:" + p.usr)
            for vt in p.conformers:
                ids = [str(it.id) for it, k, prm, _ in vt.members if it.key not in self.rejected]
                if not ids:
                    continue
                vp = self.type_platforms(vt.usr)
                both = {q: max(vp[q], tp.get(q, (0, 0))) for q in vp if q in tp}
                if not both:
                    continue
                cif, _ = platform_guard(both) if set(both) != set(tp) else (None, None)
                rel = {q: v for q, v in both.items() if v > max(tp.get(q, (0, 0)), DEPLOYMENT[q])}
                lines = []
                if cif:
                    lines.append("    " + cif)
                lines.append("    case %s:" % ", ".join(ids))
                if rel:
                    _, av = platform_guard({q: both.get(q, DEPLOYMENT[q]) for q in PLATFORMS})
                    lines.append("        if #available(%s) { return %s(e) }" % (av, vt.decoder))
                    lines.append("        return nil")
                else:
                    lines.append("        return %s(e)" % vt.decoder)
                if cif:
                    lines.append("    #endif")
                po.add("\n".join(lines), "type:" + vt.usr)
            po.add("    default: return HandwrittenValues.decode(e) as? any %s\n    }\n}" % ".".join(p.sym.path))
            if ifline:
                po.add("#endif")
        # --- views
        vo = Out(os.path.join(root, "GenViews.swift"))
        vo.add(header)
        vo.add("@MainActor enum GeneratedViews {\n    static func register() {\n        var r = ComposeRegistry.views")
        for item, kind, s, params in self.views:
            if getattr(item, "id_dup", False):
                continue
            if kind == "valueview":
                vt = s
                body = ["let a = m.args", "guard let v = %s(a.expr(0)) else { return AnyView(EmptyView()) }" % vt.decoder,
                        "return ModChain.apply(v, m, 0)"]
                ifline, lines = self.swift_wrap(item.platforms, body, "return AnyView(EmptyView())", "            ")
            else:
                slot_params = [(lbl, internal, b) for lbl, internal, b, _ in params if b.kind == "slot"]
                children = None
                for lbl, internal, b in slot_params:
                    if internal == "content":
                        children = internal
                if children is None and len(slot_params) == 1:
                    children = slot_params[0][1]
                # slots are referenced by SSlotRef args; recompute arg positions
                try:
                    stmts, args = self.swift_call_args(params, "return AnyView(EmptyView())", view_children=children)
                except Unsupported as e:
                    self.rejected[item.key] = str(e)
                    continue
                call = "%s(%s)" % (s.name, ", ".join(args))
                if s.usr in self.self_modifiers:
                    # applies the leading concrete-type methods and records how many it consumed
                    call = "sm_%s(%s, m)" % (s.name, call)
                body = ["let a = m.args"] + stmts + self.open_call("AnyView", "ModChain.apply(%s, m, m.consumedPrefix)" % call)
                ifline, lines = self.swift_wrap(item.platforms, body, "return AnyView(EmptyView())", "            ")
            code = []
            if ifline:
                code.append(ifline)
            code.append("        r[%d] = { m in" % item.id)
            code.extend(lines)
            code.append("        }")
            if ifline:
                code.append("#endif")
            vo.add("\n".join(code), item.key)
        vo.add("        ComposeRegistry.views = r\n    }\n}")
        # self modifiers
        smo = Out(os.path.join(root, "GenSelfModifiers.swift"))
        smo.add(header)
        all_self_ids = []
        for usr, mods in sorted(self.self_modifiers.items(), key=lambda x: self.model.syms[x[0]].name):
            s = self.model.syms[usr]
            smo.add("@MainActor func sm_%s(_ base: %s, _ m: NodeModel) -> %s {\n    // Applies the leading run of modifiers that are methods of %s, like Swift overload resolution.\n    var r = base\n    var consumed = 0\n    chain: for (index, mod) in m.modifiers.enumerated() {\n        let a = Args(node: m.node, src: mod, modifierIndex: index)\n        switch mod.id {" % (s.name, s.name, s.name, s.name))
            seen_ids = set()
            for item, params in mods:
                if item.id in seen_ids:
                    continue
                try:
                    stmts, args = self.swift_call_args(params, "break chain")
                except Unsupported as e:
                    self.rejected[item.key] = str(e)
                    continue
                call = "r.%s(%s)" % (self._swift_member(item.sym.name), ", ".join(args))
                if self.opened:
                    # protocol-typed parameter (Text.foregroundStyle<S: ShapeStyle>...): open the existential
                    gens = ", ".join("Q%s: %s" % (q[1:], proto) for _, q, proto in self.opened)
                    ps = ", ".join("_ %s: Q%s" % (q, q[1:]) for _, q, _ in self.opened)
                    body = stmts + ["@MainActor func open<%s>(%s) -> %s { %s }" % (gens, ps, s.name, call),
                                    "r = open(%s)" % ", ".join(p for p, _, _ in self.opened)]
                else:
                    body = stmts + ["r = %s" % call]
                ifline, avail = platform_guard(item.platforms)
                lines = []
                if ifline:
                    lines.append("        " + ifline)
                lines.append("        case %d:" % item.id)
                if avail:
                    lines.append("            guard #available(%s) else { break chain }" % avail)
                lines.extend("            " + l for l in body)
                if ifline:
                    lines.append("        #endif")
                smo.add("\n".join(lines), item.key)
                seen_ids.add(item.id)
                all_self_ids.append(item.id)
            smo.add("        default: break chain\n        }\n        consumed += 1\n    }\n    m.consumedPrefix = consumed\n    return r\n}\n")
        smo.add("let generatedSelfModifierIds: Set<Int32> = [%s]" % ", ".join(str(i) for i in sorted(set(all_self_ids))))
        # modifiers: generic recursive chain — the concrete view type is kept at every step, so a node
        # costs a single AnyView whatever the number of modifiers.
        nchunks = 8
        mo = [Out(os.path.join(root, "GenModifiers%d.swift" % i)) for i in range(nchunks)]
        for i, o in enumerate(mo):
            o.add(header)
            o.add("@MainActor enum GeneratedChain%d {\n    @_semantics(\"optimize.sil.specialize.generic.never\")\n    static func apply<V: View>(_ v: V, _ id: Int, _ a: Args, _ m: NodeModel, _ i: Int) -> AnyView? {\n        switch id {" % i)
        next_step = "return ModChain.apply(v, m, i + 1)"
        for item, params in self.modifiers:
            if getattr(item, "self_type", None) is not None:
                continue
            o = mo[item.id % nchunks]
            try:
                stmts, args = self.swift_call_args(params, next_step)
            except Unsupported as e:
                self.rejected[item.key] = str(e)
                continue
            body = stmts + self.open_call("AnyView", "ModChain.apply(v.%s(%s), m, i + 1)" % (self._swift_member(item.sym.name), ", ".join(args)))
            ifline, lines = self.swift_wrap(item.platforms, body, next_step, "            ")
            code = []
            if ifline:
                code.append(ifline)
            code.append("        case %d:" % item.id)
            code.extend(lines)
            if ifline:
                code.append("#endif")
            o.add("\n".join(code), item.key)
        for o in mo:
            o.add("        default: return nil\n        }\n    }\n}")
        reg = Out(os.path.join(root, "GenRegister.swift"))
        reg.add(header)
        reg.add("@MainActor enum GeneratedChain {\n    @_semantics(\"optimize.sil.specialize.generic.never\")\n    static func apply<V: View>(_ v: V, _ id: Int, _ a: Args, _ m: NodeModel, _ i: Int) -> AnyView? {\n        switch id %% %d {\n%s\n        default: return nil\n        }\n    }\n}\n" % (
            nchunks, "\n".join("        case %d: return GeneratedChain%d.apply(v, id, a, m, i)" % (i, i) for i in range(nchunks))))
        reg.add("public enum ComposeGenerated {\n    @MainActor public static func registerAll() {\n        GeneratedViews.register()\n        ComposeRegistry.selfModifierIds = generatedSelfModifierIds\n    }\n}")
        for o in dec_outs + [po, vo, smo] + mo + [reg]:
            o.write()
            self.line_map.append((o.path, o.ranges))

    @staticmethod
    def _swift_member(name):
        return "`%s`" % name if name in ("default", "init", "self", "Type", "repeat", "case", "in", "is", "as", "where") else name

    # ---- coverage ------------------------------------------------------------------------

    def finish(self):
        for s in self.model.syms.values():
            if s.usr in self.coverage:
                continue
            if s.module not in ("SwiftUI", "SwiftUICore"):
                continue
            k = s.kind
            if k in ("swift.typealias", "swift.associatedtype"):
                self.status(s, "n/a", "type alias / associated type (no runtime surface)")
            elif k in ("swift.func.op",):
                self.status(s, "n/a", "operator")
            elif k in ("swift.property", "swift.subscript", "swift.type.subscript"):
                self.status(s, "n/a", "instance property / subscript (read side, not a view-building API)")
            elif k == "swift.protocol":
                self.status(s, "generated" if s.usr in self.protos else "skipped",
                            "Kotlin interface" if s.usr in self.protos else "protocol without bridgeable conformers")
            elif k in ("swift.struct", "swift.enum", "swift.class"):
                if s.usr in self.values:
                    self.status(s, "generated", "Kotlin value class")
                else:
                    self.status(s, "skipped", "type not constructible from Kotlin (generic or no bridgeable members)")
            elif k == "swift.macro":
                self.status(s, "n/a", "Swift macro (compile-time)")
            else:
                self.status(s, "skipped", "not a view, modifier or value-producing member")
        out = []
        for usr, st in self.coverage.items():
            s = self.model.syms[usr]
            out.append({"usr": usr, "path": s.dotted, "kind": s.kind, "status": st["status"], "detail": st["detail"],
                        "platforms": sorted(s.platforms)})
        out.sort(key=lambda x: x["path"])
        with open(os.path.join(self.project, "generator", "coverage.json"), "w") as f:
            json.dump(out, f, indent=1)
        with open(os.path.join(self.project, "generator", "linemap.json"), "w") as f:
            json.dump(self.line_map, f)
        with open(os.path.join(self.project, "generator", "rejected.json"), "w") as f:
            json.dump(self.rejected, f, indent=1, sort_keys=True)

    opened = []

    def run(self):
        self.build_types()
        self.build_views()
        self.emit_kotlin()
        self.emit_swift()
        self.finish()
        stats = {}
        for c in self.coverage.values():
            stats[c["status"]] = stats.get(c["status"], 0) + 1
        print("value types: %d, protocols: %d, members: %d, views: %d, modifiers: %d" % (
            len(self.values), len(self.protos), sum(len(v.members) for v in self.values.values()),
            len(self.views), len(self.modifiers)))
        print("coverage:", stats)


if __name__ == "__main__":
    Generator(sys.argv[1], sys.argv[2]).run()
