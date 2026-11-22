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


def stand_in(params):
    """True when a variant carries a Swift type through a stand-in (view value, converted or lossy type):
    on a Kotlin signature clash, the variant with native types keeps the name."""
    def one(b):
        if b.kind in ("optional", "list", "set", "binding", "callback") and b.inner is not None:
            return one(b.inner)
        return bool(b.conv) or b.lossy or (b.kind == "slot" and b.bare)
    return any(one(b) for _, _, b, _ in (params or []))


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
        # Swift type built from the base kind (see CONVS): (type name, to template, failable, back template, fallback)
        self.conv = None
        # lossy stand-in for a Swift type (Transferable as String...): loses Kotlin signature clashes
        self.lossy = False

    @property
    def nargs(self):
        return 2 if self.kind == "binding" else 1

    @property
    def is_function(self):
        return self.kind in ("action", "async_action", "callback", "callbackn") or (
            self.kind == "optional" and self.inner.kind in ("action", "async_action", "callback", "callbackn"))

    # Kotlin --------------------------------------------------------------------------------

    def kotlin_params(self, name):
        """[(kotlin_name, kotlin_type)] — bindings expand into value + change callback."""
        if self.kind == "binding":
            return [(name, self.inner.kt), ("on" + name[0].upper() + name[1:] + "Change", "(%s) -> Unit" % self.inner.kt)]
        return [(name, self.kt)]

    def kotlin_encode(self, name):
        k = self.kind
        if k == "action":
            return ["SAction(%s)" % name]
        if k == "async_action":
            return ["SAsyncAction(%s)" % name]
        if k == "optional" and self.inner.kind == "action":
            return ["%s?.let { SAction(it) }" % name]
        if k == "optional" and self.inner.kind == "async_action":
            return ["%s?.let { SAsyncAction(it) }" % name]
        if k == "callback":
            return [self.inner.kotlin_receive(name)]
        if k == "callbackn":
            return [self.kotlin_receive_n(name)]
        if k == "optional" and self.inner.kind == "callback":
            return ["%s?.let { f -> %s }" % (name, self.inner.inner.kotlin_receive("f"))]
        if k == "optional" and self.inner.kind == "callbackn":
            return ["%s?.let { f -> %s }" % (name, self.inner.kotlin_receive_n("f"))]
        if k == "binding":
            cb = "on" + name[0].upper() + name[1:] + "Change"
            return [name, self.inner.kotlin_receive(cb)]
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

    def kotlin_receive(self, fn):
        """SValueCallback passing a value sent by Swift to the Kotlin function `fn`."""
        if self.kind == "value":
            # Swift encodes values as plain SExpr (see e_* encoders): rebuild the typed Kotlin value
            ctor = "cgValue(e) as %s" % self.kt if self.kt in CG_VALUES else "%s(e.id, e.args)" % self.kt
            return "SValueCallback { (it as? SExpr)?.let { e -> %s(%s) } }" % (fn, ctor)
        return "SValueCallback { %s(it as %s) }" % (fn, self.kt)

    def kotlin_value(self, x):
        """Kotlin expression of the value `x` (an Any? sent by Swift), or null when it can't be rebuilt."""
        if self.kind == "value":
            ctor = "cgValue(e) as %s" % self.kt if self.kt in CG_VALUES else "%s(e.id, e.args)" % self.kt
            return "((%s as? SExpr)?.let { e -> %s })" % (x, ctor)
        return "(%s as? %s)" % (x, self.kt)

    def kotlin_receive_n(self, fn):
        """SValueCallback unpacking the argument list Swift sends to a multi-parameter callback."""
        vals = ["val a%d = %s ?: return@SValueCallback" % (n, p.kotlin_value("l[%d]" % n)) for n, p in enumerate(self.params)]
        return "SValueCallback { val l = it as List<*>; %s; %s(%s) }" % (
            "; ".join(vals), fn, ", ".join("a%d" % n for n in range(len(self.params))))

    def swift_box(self, x):
        """Swift expression sending the Swift value `x` of this bridge to Kotlin."""
        if self.kind == "value":
            return "(%s(%s) ?? NSNull())" % (self.encoder, x)
        if self.kind == "url":
            return "KBox.string(%s.absoluteString)" % x
        if self.conv:
            return "%s(%s)" % (Bridge(self.kind, self.kt).box(), self.conv[3] % x)
        return "%s(%s)" % (self.box(), x)

    @property
    def encoder(self):
        return "e_" + self.swift_type[2:]

    # Swift ---------------------------------------------------------------------------------

    def swift_decode(self, i, var, fallback, src="a"):
        """Returns (statements, expression) decoding Kotlin arg(s) starting at index i."""
        if self.conv:
            _, tpl, failable = self.conv[:3]
            base = Bridge(self.kind, self.kt)
            stmts, expr = base.swift_decode(i, var, fallback, src)
            if failable:
                return stmts + ["guard let %s = %s else { %s }" % (var, tpl % expr, fallback)], var
            return stmts, tpl % expr
        k = self.kind
        simple = {
            "str": "%s.string(%d)", "bool": "%s.bool(%d)", "int": "%s.int(%d)", "long": "%s.long(%d)", "double": "%s.double(%d)",
            "cgfloat": "%s.cgFloat(%d)", "float": "%s.float(%d)", "hash": "%s.hashable(%d)",
            "lsk": "LocalizedStringKey(%s.string(%d))", "action": "%s.action(%d)",
            "lsr": "LocalizedStringResource(String.LocalizationValue(%s.string(%d)))", "text": "Text(%s.string(%d))",
            "drange": "%s.doubleRange(%d)", "irange": "%s.intRange(%d)", "date": "%s.date(%d)",
        }
        if k in simple:
            return [], simple[k] % (src, i)
        if k == "async_action":
            return ["let %s = %s.asyncAction(%d)" % (var, src, i)], "{ await %s() }" % var
        if k == "callbackn":
            xs = ["x%d" % n for n in range(len(self.params))]
            return ["let %s = %s.callback(%d, box: { (v: NSArray) -> Any? in v })" % (var, src, i)], \
                "{ %s in %s([%s] as NSArray) }" % (", ".join(xs), var, ", ".join(p.swift_box(x) for p, x in zip(self.params, xs)))
        if k == "optional" and self.inner.kind in ("callback", "callbackn"):
            stmts, expr = self.inner.swift_decode(i, var, fallback, src)
            return stmts, "(%s.isNil(%d) ? nil : %s)" % (src, i, expr)
        if k == "callback":
            inner = self.inner
            if inner.kind == "value":
                return [], "%s.callback(%d, box: { %s($0) ?? NSNull() })" % (src, i, inner.encoder)
            if inner.kind == "url":
                return [], "%s.callback(%d, box: { (v: URL) in KBox.string(v.absoluteString) })" % (src, i)
            if inner.conv:
                return [], "%s.callback(%d, box: { (v: %s) in %s(%s) })" % (
                    src, i, inner.conv[0], inner.box(), inner.conv[3] % "v")
            return [], "%s.callback(%d, box: %s)" % (src, i, inner.box())
        if k == "binding":
            inner = self.inner
            if inner.kind == "value":
                return ["guard let %s0 = %s(%s.expr(%d)) else { %s }" % (var, inner.swift_type, src, i, fallback)], \
                    "%s.binding(%d, read: { %s($0.expr($1)) ?? %s0 }, box: { %s($0) ?? NSNull() })" % (
                        src, i, inner.swift_type, var, inner.encoder)
            if inner.conv:
                name, to, failable, back, fallback = inner.conv
                get = to % ("%s.wrappedValue" % var)
                if failable:
                    get = "(%s ?? %s)" % (get, fallback)
                return ["let %s = %s.%sBinding(%d)" % (var, src, inner.binding_kind(), i)], \
                    "Binding<%s>(get: { %s }, set: { %s.wrappedValue = %s })" % (name, get, var, back % "$0")
            return [], "%s.%sBinding(%d)" % (src, inner.binding_kind(), i)
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
            elif el.kind == "str" and el.conv:
                expr = "%s.list(%d).compactMap { ($0 as? String).flatMap { %s } }" % (src, i, el.conv[1] % "$0")
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
            if inner.conv:
                base = Bridge("optional", self.kt, inner=Bridge(inner.kind, inner.kt))
                _, expr = base.swift_decode(i, var, fallback, src)
                return [], "%s.%s { %s }" % (expr, "flatMap" if inner.conv[2] else "map", inner.conv[1] % "$0")
            opt = {
                "str": "%s.stringOpt(%d)", "bool": "%s.boolOpt(%d)", "int": "%s.intOpt(%d)", "long": "%s.longOpt(%d)",
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
                return ["let %s = %s.isNil(%d) ? nil : %s.asyncAction(%d)" % (var, src, i, src, i)], "%s.map { f in { await f() } }" % var
            raise Unsupported("optional of %s" % inner.kind)
        raise Unsupported("decode %s" % k)

    def box(self):
        return {"str": "KBox.string", "bool": "KBox.bool", "int": "KBox.int", "long": "KBox.long", "double": "KBox.double",
                "cgfloat": "KBox.cgFloat", "hash": "KBox.hashable", "float": "KBox.float", "date": "KBox.date"}[self.kind]

    def binding_kind(self):
        if self.kind == "optional" and self.inner.kind == "hash":
            return "optionalHashable"
        return {"str": "string", "bool": "bool", "int": "int", "long": "long", "double": "double", "cgfloat": "cgFloat",
                "hash": "hashable", "float": "float", "date": "date"}[self.kind]


CG_VALUES = ("CGPoint", "CGSize", "CGRect", "CGVector", "CGAffineTransform")

PRIMS = {
    "String": ("str", "String"), "Substring": ("str", "String"), "Bool": ("bool", "Boolean"),
    "Int": ("int", "Int"), "Double": ("double", "Double"), "TimeInterval": ("double", "Double"),
    "CGFloat": ("cgfloat", "Double"), "Float": ("float", "Float"),
    "LocalizedStringKey": ("lsk", "LocalizedKey"), "LocalizedStringResource": ("lsr", "LocalizedKey"),
    "Text": ("text", "String"), "URL": ("url", "String"),
    "Date": ("date", "Double"),
}

# Swift types carried as a Kotlin primitive:
# name -> (base kind, Swift value from the base value, base value back from the Swift value, fallback if failable)
CONVS = {
    **{t: ("int", t + "(truncatingIfNeeded: %s)", "Int(truncatingIfNeeded: %s)", None)
       for t in ("Int8", "Int16", "Int32", "UInt", "UInt8", "UInt16", "UInt32")},
    # 64-bit integers are Kotlin Longs: an Int would truncate them
    **{t: ("long", t + "(truncatingIfNeeded: %s)", "Int64(truncatingIfNeeded: %s)", None) for t in ("Int64", "UInt64")},
    "Character": ("str", "(%s.first ?? \" \")", "String(%s)", None),
    "AttributedString": ("str", "AttributedString(%s)", "String(%s.characters)", None),
    "Locale": ("str", "Locale(identifier: %s)", "%s.identifier", None),
    "Locale.Language": ("str", "Locale.Language(identifier: %s)", "%s.maximalIdentifier", None),
    "UTType": ("str", "UTType(%s)", "%s.identifier", ".item"),
    "TimeZone": ("str", "(TimeZone(identifier: %s) ?? .current)", "%s.identifier", None),
    "Calendar": ("str", "kotlinCalendar(%s)", "kotlinCalendarName(%s)", None),
    # asset-catalog resources of the main bundle, by name
    "ImageResource": ("str", "ImageResource(name: %s, bundle: .main)", None, None),
    "ColorResource": ("str", "ColorResource(name: %s, bundle: .main)", None, None),
}

GENERIC_PRIMS = {
    "StringProtocol": ("str", "String"),
    "BinaryFloatingPoint": ("double", "Double"),
    "BinaryInteger": ("int", "Int"),
    "FixedWidthInteger": ("int", "Int"),
    "Hashable": ("hash", "Any"),
    # ponytail: Transferable payloads travel as text (String is Transferable); files/images need their own API
    "Transferable": ("str", "String"),
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
        self.encoders = set()  # value types sent back to Kotlin (usr)

    def encodable(self, b):
        """Value bridges that Swift can send back to Kotlin (enums and Equatable types, CG values)."""
        if b.kind != "value":
            return False
        if b.kt in CG_VALUES:
            return True
        usr = next(iter(b.refs))
        sym = self.model.syms[usr]
        if sym.kind == "swift.enum" or sym.dotted == "Color" or (
                {"s:SQ", "s:SH"} & self.model.all_protocols(sym) and self._closed(sym)):
            self.encoders.add(usr)
            return True
        return False

    @staticmethod
    def _closed(sym):
        """Equatable struct whose values are all static members: each one maps back to a Kotlin static.
        Types built by initializers or factories (`.height(300)`) can't be sent back."""
        for c in sym.children:
            if c.kind == "swift.init" and "from decoder" not in c.decl:
                return False
            if c.kind == "swift.type.method" and c.returns() in (sym.name, "Self", sym.dotted):
                return False
        return True

    def classify(self, t, ctx, top=True):
        k = t.kind
        if k == "optional":
            if t.args[0].kind == "func" and top:
                inner = self._func(t.args[0], ctx)
                if inner.kind in ("action", "callback", "callbackn"):
                    return Bridge("optional", "(%s)?" % inner.kt, inner=inner)
                raise Unsupported("optional %s" % inner.kind)
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
            ok = self.encodable(inner) or not (inner.conv and inner.conv[3] is None) and inner.kind in ("str", "bool", "int", "long", "double", "cgfloat", "hash", "float", "date") or (
                inner.kind == "optional" and inner.inner.kind == "hash")
            if not ok:
                raise Unsupported("Binding<%s>" % inner.kind)
            return Bridge("binding", inner.kt, inner=inner)
        if name == "ClosedRange" and t.args:
            inner = self.classify(t.args[0], ctx, top=False)
            if inner.kind in ("double", "cgfloat"):
                return Bridge("drange", "ClosedFloatingPointRange<Double>")
            if inner.kind == "date":
                b = Bridge("drange", "ClosedFloatingPointRange<Double>")
                b.conv = ("ClosedRange<Date>", "({ (r: ClosedRange<Double>) in Date(timeIntervalSince1970: r.lowerBound)...Date(timeIntervalSince1970: r.upperBound) })(%s)",
                          False, None, None)
                return b
            if inner.kind == "int" and not inner.conv:
                return Bridge("irange", "IntRange")
            raise Unsupported("ClosedRange<%s>" % inner.kind)
        # `V.Stride` / `V.Value` of a generic numeric parameter: same bridge as V
        if "." in name and name.split(".")[0] in ctx.generic_names and name.split(".")[1] in ("Stride",):
            return self.classify(T("name", name=name.split(".")[0]), ctx, top)
        if name in CG_VALUES:
            return Bridge("value", name, swift_type="d_" + name)
        if name in PRIMS:
            kind, kt = PRIMS[name]
            return Bridge(kind, kt)
        if name in CONVS:
            kind, to, back, fallback = CONVS[name]
            b = Bridge(kind, {"int": "Int", "long": "Long"}.get(kind, "String"))
            b.conv = (name, to, fallback is not None, back, fallback)
            return b
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
                b = Bridge(kind, kt)
                b.lossy = c == "Transferable"
                return b
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
                return Bridge("async_action", "suspend () -> Unit") if t.is_async else Bridge("action", "() -> Unit")
            if len(t.params) == 1 and not t.is_async:
                inner = self.classify(t.params[0], ctx, top=False)
                if self.encodable(inner) or not (inner.conv and inner.conv[3] is None) and inner.kind in (
                        "str", "bool", "int", "long", "double", "cgfloat", "hash", "float", "date", "url"):
                    return Bridge("callback", "(%s) -> Unit" % inner.kt, inner=inner)
                raise Unsupported("callback of %s" % inner.kind)
            if not t.is_async:
                params = [self.classify(p, ctx, top=False) for p in t.params]
                for p in params:
                    if not (self.encodable(p) or not p.conv and p.kind in ("str", "bool", "int", "long", "double", "cgfloat", "hash", "float", "date", "url")):
                        raise Unsupported("closure with %d params" % len(t.params))
                b = Bridge("callbackn", "(%s) -> Unit" % ", ".join(p.kt for p in params))
                b.params = params
                return b
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
