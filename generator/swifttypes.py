"""Minimal parser for Swift type expressions and declaration parameter lists."""

import re

SWIFT_KEYWORDS_ATTRS = ("@escaping", "@autoclosure", "@Sendable", "@MainActor", "@preconcurrency",
                        "sending", "__owned", "inout", "borrowing", "consuming", "isolated")


class T:
    """Type AST node. kind: name | optional | array | set | dict | func | tuple | some | any | unknown"""

    def __init__(self, kind, name=None, args=None, params=None, result=None, is_async=False,
                 throws=False, attrs=None):
        self.kind = kind
        self.name = name
        self.args = args or []
        self.params = params or []
        self.result = result
        self.is_async = is_async
        self.throws = throws
        self.attrs = attrs or []

    def __repr__(self):
        if self.kind == "name":
            return self.name + ("<%s>" % ", ".join(map(repr, self.args)) if self.args else "")
        if self.kind == "optional":
            return "%r?" % self.args[0]
        if self.kind == "array":
            return "[%r]" % self.args[0]
        if self.kind == "func":
            return "(%s)%s -> %r" % (", ".join(map(repr, self.params)), " async" if self.is_async else "", self.result)
        if self.kind in ("some", "any"):
            return "%s %r" % (self.kind, self.args[0])
        return "<%s %s>" % (self.kind, self.name)


class _P:
    def __init__(self, s):
        self.toks = re.findall(r"->|\.\.\.|[A-Za-z_][A-Za-z_0-9]*|@[A-Za-z_]+(?:\([^)]*\))?|[()\[\]<>?!,:.&=]|\S", s)
        self.i = 0

    def peek(self, k=0):
        j = self.i + k
        return self.toks[j] if j < len(self.toks) else None

    def take(self, expected=None):
        t = self.peek()
        if expected is not None and t != expected:
            raise ValueError("expected %r got %r in %r" % (expected, t, self.toks))
        self.i += 1
        return t

    def type(self):
        attrs = []
        while self.peek() and (self.peek().startswith("@") or self.peek() in ("sending", "inout", "__owned", "borrowing", "consuming")):
            attrs.append(self.take())
        t = self.peek()
        if t in ("some", "any"):
            self.take()
            base = self.composition()
            res = T(t, args=[base])
        elif t == "(":
            res = self.paren_or_func()
        elif t == "[":
            self.take("[")
            a = self.type()
            if self.peek() == ":":
                self.take(":")
                b = self.type()
                self.take("]")
                res = T("dict", args=[a, b])
            else:
                self.take("]")
                res = T("array", args=[a])
        else:
            res = self.named()
        while self.peek() in ("?", "!"):
            self.take()
            res = T("optional", args=[res])
        if self.peek() == "." and self.peek(1) in ("Type", "Protocol"):
            self.take(); self.take()
            res = T("unknown", name="metatype")
        res.attrs = attrs + res.attrs
        return res

    def composition(self):
        base = self.named()
        while self.peek() == "&":
            self.take()
            self.named()
            base = T("unknown", name="composition")
        return base

    def named(self):
        parts = [self.take()]
        args = []
        while True:
            if self.peek() == "<":
                self.take("<")
                args = [self.type()]
                while self.peek() == ",":
                    self.take(",")
                    args.append(self.type())
                self.take(">")
            if self.peek() == "." and self.peek(1) and re.match(r"[A-Za-z_]", self.peek(1)) and self.peek(1) not in ("Type", "Protocol"):
                self.take(".")
                parts.append(self.take())
                continue
            break
        name = ".".join(parts)
        if name in ("Set",) and args:
            return T("set", args=args)
        if name == "Optional" and args:
            return T("optional", args=args)
        if name == "Array" and args:
            return T("array", args=args)
        return T("name", name=name, args=args)

    def paren_or_func(self):
        self.take("(")
        elems = []
        while self.peek() != ")":
            # tuple labels: `label: Type`
            if self.peek(1) == ":" and re.match(r"[A-Za-z_]", self.peek() or ""):
                self.take(); self.take(":")
            elems.append(self.type())
            if self.peek() == ",":
                self.take(",")
        self.take(")")
        is_async = False
        throws = False
        while self.peek() in ("async", "throws", "rethrows"):
            t = self.take()
            if t == "async":
                is_async = True
            else:
                throws = True
                if self.peek() == "(":  # typed throws
                    depth = 0
                    while True:
                        x = self.take()
                        if x == "(":
                            depth += 1
                        elif x == ")":
                            depth -= 1
                            if depth == 0:
                                break
        if self.peek() == "->":
            self.take("->")
            result = self.type()
            return T("func", params=elems, result=result, is_async=is_async, throws=throws)
        if len(elems) == 1:
            return elems[0]
        if not elems:
            return T("name", name="Void")
        return T("tuple", args=elems)


def parse_type(s):
    s = s.strip()
    if s.endswith("..."):
        # variadic: bridged as a single value (Swift call passes one element)
        return parse_type(s[:-3])
    p = _P(s)
    t = p.type()
    if p.peek() is not None:
        raise ValueError("trailing tokens in %r: %r" % (s, p.toks[p.i:]))
    return t


def split_top(s, sep=","):
    out, depth, cur, quote = [], 0, [], False
    for k, ch in enumerate(s):
        if ch == ">" and k > 0 and s[k - 1] == "-":
            cur.append(ch)
            continue
        if ch == '"':
            quote = not quote
        if not quote:
            if ch in "([<{":
                depth += 1
            elif ch in ")]>}":
                depth -= 1
            elif ch == sep and depth == 0:
                out.append("".join(cur))
                cur = []
                continue
        cur.append(ch)
    if "".join(cur).strip():
        out.append("".join(cur))
    return out


def decl_params(decl, name):
    """Returns [(label, internal, type_str, default_or_None, attrs)] from a declaration string."""
    # find the parameter list following the base name (skip generic clause)
    m = re.search(r"\b(?:init|func\s+" + re.escape(name) + r"|static\s+func\s+" + re.escape(name) + r")\b", decl)
    start = m.end() if m else 0
    i = start
    if i < len(decl) and decl[i] == "?":
        i += 1
    if i < len(decl) and decl[i] == "<":
        depth = 0
        while i < len(decl):
            if decl[i] == "<":
                depth += 1
            elif decl[i] == ">":
                depth -= 1
                if depth == 0:
                    i += 1
                    break
            i += 1
    while i < len(decl) and decl[i] != "(":
        i += 1
    depth = 0
    j = i
    while j < len(decl):
        if decl[j] == ">" and decl[j - 1] == "-":
            pass
        elif decl[j] == "(":
            depth += 1
        elif decl[j] == ")":
            depth -= 1
            if depth == 0:
                break
        j += 1
    inner = decl[i + 1:j]
    params = []
    for raw in split_top(inner):
        raw = raw.strip()
        if not raw:
            continue
        attrs = []
        while raw.startswith("@"):
            a = re.match(r"@[A-Za-z_]+(\([^)]*\))?\s*", raw)
            attrs.append(a.group(0).strip())
            raw = raw[a.end():]
        head, _, rest = raw.partition(":")
        names = head.split()
        label = names[0]
        internal = names[1] if len(names) > 1 else names[0]
        default = None
        parts = split_top(rest, "=")
        type_str = parts[0].strip()
        if len(parts) > 1:
            default = "=".join(parts[1:]).strip()
        # attributes may also appear after the colon
        while type_str.startswith("@"):
            a = re.match(r"@[A-Za-z_]+(\([^)]*\))?\s*", type_str)
            attrs.append(a.group(0).strip())
            type_str = type_str[a.end():]
        params.append((label, internal, type_str, default, attrs))
    return params
