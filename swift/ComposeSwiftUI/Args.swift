import SwiftUI

/// Typed access to the arguments Kotlin encoded for a view init or a modifier.
public struct Args {
    let node: SNode?
    public let values: [Any]
    /// nil for view init args, otherwise the index of the modifier in the node's chain.
    let modifierIndex: Int?

    init(node: SNode?, values: [Any], modifierIndex: Int?) {
        self.node = node
        self.values = values
        self.modifierIndex = modifierIndex
    }

    public init(expr: SExpr) {
        self.init(node: nil, values: expr.args, modifierIndex: nil)
    }

    @inline(__always) public func raw(_ i: Int) -> Any? {
        guard i < values.count else { return nil }
        let value = values[i]
        return value is NSNull ? nil : value
    }

    public func isNil(_ i: Int) -> Bool { raw(i) == nil }

    public func string(_ i: Int) -> String { raw(i) as? String ?? "" }
    public func bool(_ i: Int) -> Bool { (raw(i) as? NSNumber)?.boolValue ?? false }
    public func int(_ i: Int) -> Int { (raw(i) as? NSNumber)?.intValue ?? 0 }
    public func double(_ i: Int) -> Double { (raw(i) as? NSNumber)?.doubleValue ?? 0 }
    public func float(_ i: Int) -> Float { (raw(i) as? NSNumber)?.floatValue ?? 0 }
    public func cgFloat(_ i: Int) -> CGFloat { CGFloat(double(i)) }
    public func expr(_ i: Int) -> SExpr? { raw(i) as? SExpr }
    public func list(_ i: Int) -> [Any] { raw(i) as? [Any] ?? [] }
    public func date(_ i: Int) -> Date { Date(timeIntervalSince1970: double(i)) }
    public func dateOpt(_ i: Int) -> Date? { doubleOpt(i).map { Date(timeIntervalSince1970: $0) } }
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

    public func stringOpt(_ i: Int) -> String? { raw(i) as? String }
    public func boolOpt(_ i: Int) -> Bool? { (raw(i) as? NSNumber)?.boolValue }
    public func intOpt(_ i: Int) -> Int? { (raw(i) as? NSNumber)?.intValue }
    public func doubleOpt(_ i: Int) -> Double? { (raw(i) as? NSNumber)?.doubleValue }
    public func cgFloatOpt(_ i: Int) -> CGFloat? { doubleOpt(i).map { CGFloat($0) } }

    // MARK: Callbacks

    /// A Kotlin `() -> Unit`, followed by a synchronous recomposition.
    public func action(_ i: Int) -> () -> Void {
        guard let action = raw(i) as? SAction else { return {} }
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
        guard let callback = raw(i) as? SValueCallback else { return { _ in } }
        return {
            callback.invoke(value: box($0))
            ComposeSwiftUIHostKt.flushCompose()
        }
    }

    // MARK: Bindings (encoded as value at `i`, setter callback at `i + 1`)

    public func binding<T>(_ i: Int, read: @escaping (Args, Int) -> T, box: @escaping (T) -> Any?) -> Binding<T> {
        guard let node = self.node else { return .constant(read(self, i)) }
        let modifierIndex = self.modifierIndex
        let callback = raw(i + 1) as? SValueCallback
        return Binding(
            get: {
                // Read the live value: it may have been written silently since this render.
                if let modifierIndex {
                    let modifier = node.modifiers[modifierIndex]
                    return read(Args(node: node, values: modifier.args, modifierIndex: modifierIndex), i)
                }
                return read(Args(node: node, values: node.args, modifierIndex: nil), i)
            },
            set: { newValue in
                let boxed = box(newValue)
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

    public func slot(_ i: Int) -> SlotView {
        guard let ref = raw(i) as? SSlotRef, let node else { return SlotView(model: nil) }
        return SlotView(model: node.slot(index: ref.index).map(NodeModel.of))
    }
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
