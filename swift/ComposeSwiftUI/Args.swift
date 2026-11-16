import SwiftUI

/// Typed access to the arguments Kotlin encoded for a view init, a modifier or an expression.
/// Reads go through Kotlin's typed accessors (`SArgs`): no NSArray bridging, no dynamic casts.
/// Swift renderers spell "no value" as `NSNull()` (it fits in arrays); Kotlin expects `null`.
@inline(__always) func kotlinValue(_ v: Any?) -> Any? { v is NSNull ? nil : v }

public struct Args {
    let node: SNode?
    let src: any SArgs
    /// nil for view init args, otherwise the index of the modifier in the node's chain.
    let modifierIndex: Int?

    init(node: SNode?, src: any SArgs, modifierIndex: Int?) {
        self.node = node
        self.src = src
        self.modifierIndex = modifierIndex
    }

    public init(expr: SExpr) {
        self.init(node: nil, src: expr, modifierIndex: nil)
    }

    @inline(__always) private func k(_ i: Int) -> Int32 { Int32(truncatingIfNeeded: i) }

    public func raw(_ i: Int) -> Any? { src.argAt(index: k(i)) }
    public func isNil(_ i: Int) -> Bool { src.argIsNull(index: k(i)) }

    public func string(_ i: Int) -> String { src.argString(index: k(i)) ?? "" }
    public func bool(_ i: Int) -> Bool { src.argBool(index: k(i)) }
    public func int(_ i: Int) -> Int { Int(src.argInt(index: k(i))) }
    public func double(_ i: Int) -> Double { src.argDouble(index: k(i)) }
    public func float(_ i: Int) -> Float { Float(src.argDouble(index: k(i))) }
    public func cgFloat(_ i: Int) -> CGFloat { CGFloat(src.argDouble(index: k(i))) }
    public func expr(_ i: Int) -> SExpr? { src.argExpr(index: k(i)) }
    public func exprList(_ i: Int) -> [SExpr] { src.argExprList(index: k(i)) }
    public func list(_ i: Int) -> [Any] { src.argList(index: k(i)).map { $0 as Any } }
    public func date(_ i: Int) -> Date { Date(timeIntervalSince1970: double(i)) }
    public func dateOpt(_ i: Int) -> Date? { isNil(i) ? nil : date(i) }
    public func hashable(_ i: Int) -> AnyHashable { AnyHashable(KHashable(raw(i) as? NSObject ?? NSNull())) }
    public func hashableOpt(_ i: Int) -> AnyHashable? { (raw(i) as? NSObject).map { AnyHashable(KHashable($0)) } }
    public func doubleRange(_ i: Int) -> ClosedRange<Double> {
        let l = list(i).compactMap { ($0 as? NSNumber)?.doubleValue }
        guard l.count == 2, l[0] <= l[1] else { return 0...1 }
        return l[0]...l[1]
    }
    public func intRange(_ i: Int) -> ClosedRange<Int> {
        let l = list(i).compactMap { ($0 as? NSNumber)?.intValue }
        guard l.count == 2, l[0] <= l[1] else { return 0...1 }
        return l[0]...l[1]
    }
    public func doubleRangeOpt(_ i: Int) -> ClosedRange<Double>? { isNil(i) ? nil : doubleRange(i) }
    public func intRangeOpt(_ i: Int) -> ClosedRange<Int>? { isNil(i) ? nil : intRange(i) }

    public func stringOpt(_ i: Int) -> String? { src.argString(index: k(i)) }
    public func boolOpt(_ i: Int) -> Bool? { isNil(i) ? nil : bool(i) }
    public func intOpt(_ i: Int) -> Int? { isNil(i) ? nil : int(i) }
    public func doubleOpt(_ i: Int) -> Double? { isNil(i) ? nil : double(i) }
    public func cgFloatOpt(_ i: Int) -> CGFloat? { isNil(i) ? nil : cgFloat(i) }

    // MARK: Callbacks

    /// A Kotlin `() -> Unit`, followed by a synchronous recomposition.
    public func action(_ i: Int) -> () -> Void {
        guard let action = src.argAction(index: k(i)) else { return {} }
        return {
            action.invoke()
            ComposeSwiftUIHostKt.flushCompose()
        }
    }

    public func actionOpt(_ i: Int) -> (() -> Void)? {
        isNil(i) ? nil : action(i)
    }

    /// A Kotlin `(T) -> Unit`; `box` converts the Swift value to its Kotlin representation.
    public func callback<T>(_ i: Int, box: @escaping (T) -> Any? = { $0 }) -> (T) -> Void {
        guard let callback = src.argCallback(index: k(i)) else { return { _ in } }
        return {
            callback.invoke(value: kotlinValue(box($0)))
            ComposeSwiftUIHostKt.flushCompose()
        }
    }

    // MARK: Bindings (encoded as value at `i`, setter callback at `i + 1`)

    public func binding<T>(_ i: Int, read: @escaping (Args, Int) -> T, box: @escaping (T) -> Any?) -> Binding<T> {
        let callback = src.argCallback(index: k(i + 1))
        guard let node = self.node else {
            // Nested expression (e.g. `TextEditor(text:)` inside `TextEditorView`): not addressable
            // for silent writes, so keep the last written value locally until Kotlin re-renders.
            let cell = BindingBox(read(self, i))
            return Binding(
                get: { cell.value },
                set: { newValue in
                    cell.value = newValue
                    callback?.invoke(value: kotlinValue(box(newValue)))
                    ComposeSwiftUIHostKt.flushCompose()
                }
            )
        }
        let modifierIndex = self.modifierIndex
        return Binding(
            get: {
                // Read the live value: it may have been written silently since this render.
                if let modifierIndex {
                    let modifier = node.modifierAt(index: Int32(modifierIndex))
                    return read(Args(node: node, src: modifier, modifierIndex: modifierIndex), i)
                }
                return read(Args(node: node, src: node, modifierIndex: nil), i)
            },
            set: { newValue in
                let boxed = kotlinValue(box(newValue))
                if let modifierIndex {
                    node.setModifierArgSilently(modifierIndex: Int32(modifierIndex), argIndex: Int32(i), value: boxed)
                } else {
                    node.setArgSilently(index: Int32(i), value: boxed)
                }
                callback?.invoke(value: boxed)
                ComposeSwiftUIHostKt.flushCompose()
            }
        )
    }

    public func stringBinding(_ i: Int) -> Binding<String> {
        binding(i, read: { $0.string($1) }, box: { $0 as NSString })
    }

    public func boolBinding(_ i: Int) -> Binding<Bool> {
        binding(i, read: { $0.bool($1) }, box: { KotlinBoolean(value: $0) })
    }

    public func doubleBinding(_ i: Int) -> Binding<Double> {
        binding(i, read: { $0.double($1) }, box: { KotlinDouble(value: $0) })
    }

    public func intBinding(_ i: Int) -> Binding<Int> {
        binding(i, read: { $0.int($1) }, box: { KotlinInt(value: Int32($0)) })
    }

    public func cgFloatBinding(_ i: Int) -> Binding<CGFloat> {
        binding(i, read: { $0.cgFloat($1) }, box: { KotlinDouble(value: Double($0)) })
    }

    public func floatBinding(_ i: Int) -> Binding<Float> {
        binding(i, read: { $0.float($1) }, box: { KotlinDouble(value: Double($0)) })
    }

    public func dateBinding(_ i: Int) -> Binding<Date> {
        binding(i, read: { $0.date($1) }, box: { KBox.date($0) })
    }

    public func hashableBinding(_ i: Int) -> Binding<AnyHashable> {
        binding(i, read: { $0.hashable($1) }, box: { KBox.hashable($0) })
    }

    public func optionalHashableBinding(_ i: Int) -> Binding<AnyHashable?> {
        binding(i, read: { $0.hashableOpt($1) }, box: { $0.map { KBox.hashable($0) } ?? NSNull() })
    }

    // MARK: Slots

    /// The node of the slot passed at `i` (nil when absent).
    public func slotModel(_ i: Int) -> NodeModel? {
        let index = src.argSlot(index: k(i))
        guard index >= 0, let node else { return nil }
        return node.slot(index: index).map(NodeModel.of)
    }

    public func slot(_ i: Int) -> SlotView {
        let index = src.argSlot(index: k(i))
        guard index >= 0, let node else { return SlotView(model: nil) }
        return SlotView(model: node.slot(index: index).map(NodeModel.of))
    }
}

private final class BindingBox<T> {
    var value: T
    init(_ value: T) { self.value = value }
}

/// Kotlin boxing helpers for values sent back to Kotlin.
public enum KBox {
    public static func bool(_ v: Bool) -> Any { KotlinBoolean(value: v) }
    public static func int(_ v: Int) -> Any { KotlinInt(value: Int32(v)) }
    public static func double(_ v: Double) -> Any { KotlinDouble(value: v) }
    public static func cgFloat(_ v: CGFloat) -> Any { KotlinDouble(value: Double(v)) }
    public static func string(_ v: String) -> Any { v as NSString }
    public static func date(_ v: Date) -> Any { KotlinDouble(value: v.timeIntervalSince1970) }
    public static func float(_ v: Float) -> Any { KotlinDouble(value: Double(v)) }
    public static func hashable(_ v: AnyHashable) -> Any { (v.base as? KHashable)?.object ?? (v.base as AnyObject) }
}

/// Hashable wrapper keeping the original Kotlin object (AnyHashable would normalize a Kotlin `Int`
/// box into a Swift `Int`, which then comes back to Kotlin as a plain NSNumber).
public struct KHashable: Hashable {
    public let object: NSObject

    public init(_ object: NSObject) {
        self.object = object
    }

    public static func == (lhs: KHashable, rhs: KHashable) -> Bool { lhs.object.isEqual(rhs.object) }
    public func hash(into hasher: inout Hasher) { hasher.combine(object.hash) }
}
