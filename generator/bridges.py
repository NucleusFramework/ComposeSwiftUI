"""Maps Swift parameter types to Kotlin types, Kotlin encodings and Swift decodings."""

import re

from swifttypes import T, parse_type as _parse_type


def parse_type(s):
    try:
        return _parse_type(s)
    except Exception:
        raise Unsupported("unparsable type %s" % s)

KOTLIN_KEYWORDS = {
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface",
    "is", "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias",
    "typeof", "val", "var", "when", "while",
}


class Unsupported(Exception):
    pass


SYMBOL_LABELS = ("systemImage", "systemName")


def symbolize(label, b):
    """`systemImage:` / `systemName:` strings are SF Symbol names: typed as `SFSymbol` in Kotlin."""
    if label not in SYMBOL_LABELS:
        return b
    if b.kind == "str":
        return Bridge("str", "SFSymbol")
    if b.kind == "optional" and b.inner.kind == "str":
        return Bridge("optional", "SFSymbol?", inner=Bridge("str", "SFSymbol"))
    return b


class Bridge:
    """
    kind: str | bool | int | double | cgfloat | float | lsk | url | value | proto | hash | action |
          async_action | callback | binding | slot | list | set | drange | irange | optional
    """

    def __init__(self, kind, kt, swift_type=None, inner=None, refs=None, cb_kind=None):
        self.kind = kind
        self.kt = kt
        self.swift_type = swift_type
        self.inner = inner
        self.refs = set(refs or ())
        self.cb_kind = cb_kind
        # slot passed to Swift as a view value (`V`) instead of a `() -> V` builder closure
        self.bare = False

    @property
    def nargs(self):
        return 2 if self.kind == "binding" else 1

    @property
    def is_function(self):
        return self.kind in ("action", "async_action", "callback") or (
            self.kind == "optional" and self.inner.kind in ("action", "async_action"))

    # Kotlin --------------------------------------------------------------------------------

    def kotlin_params(self, name):
        """[(kotlin_name, kotlin_type)] — bindings expand into value + change callback."""
        if self.kind == "binding":
            return [(name, self.inner.kt), ("on" + name[0].upper() + name[1:] + "Change", "(%s) -> Unit" % self.inner.kt)]
        return [(name, self.kt)]

    def kotlin_encode(self, name):
        k = self.kind
        if k in ("action", "async_action"):
            return ["SAction(%s)" % name]
        if k == "optional" and self.inner.kind in ("action", "async_action"):
            return ["%s?.let { SAction(it) }" % name]
        if k == "callback":
            return ["SValueCallback { %s(it as %s) }" % (name, self.inner.kt)]
        if k == "binding":
            cb = "on" + name[0].upper() + name[1:] + "Change"
            return [name, "SValueCallback { %s(it as %s) }" % (cb, self.inner.kt)]
        if k == "slot":
            return ["slotContent(%s)" % name]
        if k in ("drange", "irange"):
            return ["listOf(%s.start, %s.endInclusive)" % (name, name)]
        if k == "optional" and self.inner.kind in ("drange", "irange"):
            return ["%s?.let { listOf(it.start, it.endInclusive) }" % name]
        if k in ("list", "set"):
            return ["%s.toList()" % name]
        if k in ("lsk", "lsr"):
            return ["%s.key" % name]
        if self.kt == "SFSymbol":
            return ["%s.name" % name]
        if k == "optional" and self.inner.kt == "SFSymbol":
            return ["%s?.name" % name]
        if k == "optional" and self.inner.kind in ("lsk", "lsr"):
            return ["%s?.key" % name]
        return [name]

    # Swift ---------------------------------------------------------------------------------

    def swift_decode(self, i, var, fallback, src="a"):
        """Returns (statements, expression) decoding Kotlin arg(s) starting at index i."""
        k = self.kind
        simple = {
            "str": "%s.string(%d)", "bool": "%s.bool(%d)", "int": "%s.int(%d)", "double": "%s.double(%d)",
            "cgfloat": "%s.cgFloat(%d)", "float": "%s.float(%d)", "hash": "%s.hashable(%d)",
            "lsk": "LocalizedStringKey(%s.string(%d))", "action": "%s.action(%d)",
            "lsr": "LocalizedStringResource(String.LocalizationValue(%s.string(%d)))", "text": "Text(%s.string(%d))",
            "drange": "%s.doubleRange(%d)", "irange": "%s.intRange(%d)", "date": "%s.date(%d)",
        }
        if k in simple:
            return [], simple[k] % (src, i)
        if k == "async_action":
            return ["let %s = %s.action(%d)" % (var, src, i)], "{ %s() }" % var
        if k == "callback":
            return [], "%s.callback(%d, box: %s)" % (src, i, self.inner.box())
        if k == "binding":
            return [], "%s.%sBinding(%d)" % (src, self.inner.binding_kind(), i)
        if k == "slot":
            return [], ("%s.slot(%d)" if self.bare else "{ %s.slot(%d) }") % (src, i)
        if k == "url":
            return ["guard let %s = URL(string: %s.string(%d)) else { %s }" % (var, src, i, fallback)], var
        if k == "value":
            return ["guard let %s = %s(%s.expr(%d)) else { %s }" % (var, self.swift_type, src, i, fallback)], var
        if k == "proto":
            # opened through a local generic function by the emitter (see Generator.open_call)
            return ["guard let %s = %s(%s.expr(%d)) else { %s }" % (var, self.swift_type, src, i, fallback)], "q" + var[1:]
        if k in ("list", "set"):
            el = self.inner
            if el.kind == "value":
                expr = "%s.exprList(%d).compactMap { %s($0) }" % (src, i, el.swift_type)
            elif el.kind == "str":
                expr = "%s.list(%d).compactMap { $0 as? String }" % (src, i)
            elif el.kind in ("double", "cgfloat"):
                expr = "%s.list(%d).compactMap { ($0 as? NSNumber).map { %s($0.doubleValue) } }" % (
                    src, i, "CGFloat" if el.kind == "cgfloat" else "Double")
            else:
                raise Unsupported("collection element")
            return [], ("Set(%s)" % expr) if k == "set" else expr
        if k == "optional":
            inner = self.inner
            opt = {
                "str": "%s.stringOpt(%d)", "bool": "%s.boolOpt(%d)", "int": "%s.intOpt(%d)",
                "double": "%s.doubleOpt(%d)", "cgfloat": "%s.cgFloatOpt(%d)", "action": "%s.actionOpt(%d)",
                "lsk": "%s.stringOpt(%d).map { LocalizedStringKey($0) }", "hash": "%s.hashableOpt(%d)",
                "lsr": "%s.stringOpt(%d).map { LocalizedStringResource(String.LocalizationValue($0)) }",
                "text": "%s.stringOpt(%d).map { Text($0) }",
                "drange": "%s.doubleRangeOpt(%d)", "irange": "%s.intRangeOpt(%d)",
                "float": "%s.doubleOpt(%d).map { Float($0) }", "date": "%s.dateOpt(%d)",
            }
            if inner.kind in opt:
                return [], opt[inner.kind] % (src, i)
            if inner.kind == "url":
                return [], "%s.stringOpt(%d).flatMap { URL(string: $0) }" % (src, i)
            if inner.kind == "value":
                return [], "%s(%s.expr(%d))" % (inner.swift_type, src, i)
            if inner.kind == "async_action":
                return ["let %s = %s.actionOpt(%d)" % (var, src, i)], "%s.map { f in { f() } }" % var
            raise Unsupported("optional of %s" % inner.kind)
        raise Unsupported("decode %s" % k)

    def box(self):
        return {"str": "KBox.string", "bool": "KBox.bool", "int": "KBox.int", "double": "KBox.double",
                "cgfloat": "KBox.cgFloat", "hash": "KBox.hashable", "float": "KBox.float", "date": "KBox.date"}[self.kind]

    def binding_kind(self):
        if self.kind == "optional" and self.inner.kind == "hash":
            return "optionalHashable"
        return {"str": "string", "bool": "bool", "int": "int", "double": "double", "cgfloat": "cgFloat",
                "hash": "hashable", "float": "float", "date": "date"}[self.kind]


PRIMS = {
    "String": ("str", "String"), "Substring": ("str", "String"), "Bool": ("bool", "Boolean"),
    "Int": ("int", "Int"), "Double": ("double", "Double"), "TimeInterval": ("double", "Double"),
    "CGFloat": ("cgfloat", "Double"), "Float": ("float", "Float"),
    "LocalizedStringKey": ("lsk", "LocalizedKey"), "LocalizedStringResource": ("lsr", "LocalizedKey"),
    "Text": ("text", "String"), "URL": ("url", "String"),
    "Date": ("date", "Double"),
}

GENERIC_PRIMS = {
    "StringProtocol": ("str", "String"),
    "BinaryFloatingPoint": ("double", "Double"),
    "BinaryInteger": ("int", "Int"),
    "FixedWidthInteger": ("int", "Int"),
    "Hashable": ("hash", "Any"),
    "Strideable": ("int", "Int"),
    "Equatable": ("hash", "Any"),
}

BUILDER_ATTRS = ("@ViewBuilder", "@ContentBuilder")


class Ctx:
    def __init__(self, sym, scope, generic_names, conf, same, attrs=()):
        self.sym = sym
        self.scope = scope
        self.generic_names = generic_names
        self.conf = conf
        self.same = same
        self.attrs = attrs


class Classifier:
    def __init__(self, model, values, protos):
        self.model = model
        self.values = values  # usr -> ValueType
        self.protos = protos  # usr -> ProtoType

    def classify(self, t, ctx, top=True):
        k = t.kind
        if k == "optional":
            inner = self.classify(t.args[0], ctx, top=False)
            if inner.kind == "slot" and inner.bare:
                # ponytail: `V?` view values are always passed, Kotlin has no way to omit them
                return inner
            if inner.kind in ("proto", "slot", "binding", "callback", "optional", "list", "set"):
                raise Unsupported("optional %s" % inner.kind)
            kt = inner.kt if inner.kt.startswith("(") is False else "(%s)" % inner.kt
            return Bridge("optional", kt + "?", inner=inner, refs=inner.refs)
        if k in ("array", "set"):
            inner = self.classify(t.args[0], ctx, top=False)
            if inner.kind not in ("value", "str", "double", "cgfloat"):
                raise Unsupported("collection of %s" % inner.kind)
            kt = ("List<%s>" if k == "array" else "Set<%s>") % inner.kt
            return Bridge("list" if k == "array" else "set", kt, inner=inner, refs=inner.refs)
        if k == "func":
            if not top:
                raise Unsupported("nested function type")
            return self._func(t, ctx)
        if k == "some":
            base = t.args[0]
            if base.kind == "name":
                return self._constrained([base.name], ctx, t)
            raise Unsupported("some composition")
        if k == "any":
            base = t.args[0]
            if base.kind == "name":
                return self._constrained([base.name], ctx, t)
            raise Unsupported("any composition")
        if k != "name":
            raise Unsupported("type %s" % k)
        name = t.name
        if name in ctx.same and name in ctx.generic_names:
            return self.classify(parse_type(ctx.same[name]), ctx, top)
        if name in ctx.generic_names:
            return self._constrained(ctx.conf.get(name, []), ctx, t)
        if name == "Binding" and t.args:
            inner = self.classify(t.args[0], ctx, top=False)
            ok = inner.kind in ("str", "bool", "int", "double", "cgfloat", "hash", "float", "date") or (
                inner.kind == "optional" and inner.inner.kind == "hash")
            if not ok:
                raise Unsupported("Binding<%s>" % inner.kind)
            return Bridge("binding", inner.kt, inner=inner)
        if name == "ClosedRange" and t.args:
            inner = self.classify(t.args[0], ctx, top=False)
            if inner.kind in ("double", "cgfloat"):
                return Bridge("drange", "ClosedFloatingPointRange<Double>")
            if inner.kind == "date":
                raise Unsupported("ClosedRange<Date>")
            if inner.kind == "int":
                return Bridge("irange", "IntRange")
            raise Unsupported("ClosedRange<%s>" % inner.kind)
        # `V.Stride` / `V.Value` of a generic numeric parameter: same bridge as V
        if "." in name and name.split(".")[0] in ctx.generic_names and name.split(".")[1] in ("Stride",):
            return self.classify(T("name", name=name.split(".")[0]), ctx, top)
        if name in ("CGPoint", "CGSize", "CGRect", "CGVector"):
            return Bridge("value", name, swift_type="d_" + name)
        if name in PRIMS:
            kind, kt = PRIMS[name]
            return Bridge(kind, kt)
        if name == "Self" and ctx.scope:
            sym = self.model.lookup_type(".".join(ctx.scope))
        else:
            sym = self.model.lookup_type(name, ctx.scope)
        if sym is None:
            raise Unsupported("unknown type %s" % name)
        if sym.kind == "swift.typealias":
            m = re.search(r"=\s*(.+)$", sym.decl)
            if not m:
                raise Unsupported("typealias %s" % name)
            return self.classify(parse_type(m.group(1)), Ctx(ctx.sym, sym.path[:-1], ctx.generic_names, ctx.conf, ctx.same), top)
        if sym.usr in self.values:
            vt = self.values[sym.usr]
            return Bridge("value", vt.kt_name, swift_type=vt.decoder, refs={sym.usr})
        if sym.kind == "swift.protocol" and sym.usr in self.protos:
            p = self.protos[sym.usr]
            b = Bridge("proto", p.kt_name, swift_type=p.decoder, refs={sym.usr})
            b.proto_swift = ".".join(sym.path)
            return b
        raise Unsupported("type %s not bridged" % sym.dotted)

    def _constrained(self, constraints, ctx, t):
        for c in constraints:
            if c in GENERIC_PRIMS:
                kind, kt = GENERIC_PRIMS[c]
                return Bridge(kind, kt)
        for c in constraints:
            ps = self.model.lookup_type(c)
            if ps is not None and ps.usr in self.protos:
                p = self.protos[ps.usr]
                b = Bridge("proto", p.kt_name, swift_type=p.decoder, refs={ps.usr})
                b.proto_swift = ".".join(ps.path)
                return b
        if "View" in constraints:
            b = Bridge("slot", "@Composable () -> Unit")
            b.bare = True
            return b
        raise Unsupported("generic constrained to %s" % (constraints or "nothing"))

    def _func(self, t, ctx):
        res = t.result
        is_void = res.kind == "name" and res.name in ("Void", "()")
        if is_void:
            if not t.params:
                return Bridge("async_action" if t.is_async else "action", "() -> Unit")
            if len(t.params) == 1 and not t.is_async:
                inner = self.classify(t.params[0], ctx, top=False)
                if inner.kind in ("str", "bool", "int", "double", "cgfloat", "hash", "float", "date"):
                    return Bridge("callback", "(%s) -> Unit" % inner.kt, inner=inner)
                raise Unsupported("callback of %s" % inner.kind)
            raise Unsupported("closure with %d params" % len(t.params))
        # builder closures: () -> Content where Content: View
        if t.params:
            raise Unsupported("builder with parameters")
        is_view = False
        if res.kind == "name" and res.name in ctx.generic_names:
            cs = ctx.conf.get(res.name, [])
            is_view = "View" in cs or (not cs and any(a in BUILDER_ATTRS for a in ctx.attrs))
        elif res.kind == "some" and res.args[0].kind == "name" and res.args[0].name == "View":
            is_view = True
        if is_view:
            return Bridge("slot", "@Composable () -> Unit")
        raise Unsupported("closure returning %r" % res)


def kotlin_name(name):
    return "`%s`" % name if name in KOTLIN_KEYWORDS else name


def kotlin_param_name(name):
    if name in KOTLIN_KEYWORDS:
        return name + "Value"
    if name in ("modifier",):
        return name + "Value"
    return name
