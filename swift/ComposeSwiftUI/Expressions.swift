import SwiftUI

// Symbolic expression evaluator, Animatable + InsettableShape, and Swift-evaluated visualEffect/scrollTransition.
// Kotlin side: Expressions.kt. Value id 16, modifier ids 100_116..100_117.

/// Arbitrary-length vector of animatable scalars so Kotlin shapes can animate any number of parameters natively.
public struct KotlinAnimatableVector: VectorArithmetic, Sendable {
    public var values: [Double]

    public init(_ values: [Double] = []) {
        self.values = values
    }

    public static var zero: KotlinAnimatableVector { KotlinAnimatableVector([]) }

    public var magnitudeSquared: Double {
        values.reduce(0.0) { $0 + $1 * $1 }
    }

    public mutating func scale(by rhs: Double) {
        for i in values.indices { values[i] *= rhs }
    }

    public static func + (lhs: Self, rhs: Self) -> Self {
        let count = max(lhs.values.count, rhs.values.count)
        return Self((0..<count).map { i in
            (i < lhs.values.count ? lhs.values[i] : 0) + (i < rhs.values.count ? rhs.values[i] : 0)
        })
    }

    public static func += (lhs: inout Self, rhs: Self) { lhs = lhs + rhs }

    public static func - (lhs: Self, rhs: Self) -> Self {
        let count = max(lhs.values.count, rhs.values.count)
        return Self((0..<count).map { i in
            (i < lhs.values.count ? lhs.values[i] : 0) - (i < rhs.values.count ? rhs.values[i] : 0)
        })
    }

    public static func -= (lhs: inout Self, rhs: Self) { lhs = lhs - rhs }

    public static func == (lhs: Self, rhs: Self) -> Bool {
        let count = max(lhs.values.count, rhs.values.count)
        for i in 0..<count {
            let a = i < lhs.values.count ? lhs.values[i] : 0
            let b = i < rhs.values.count ? rhs.values[i] : 0
            if a != b { return false }
        }
        return true
    }
}

/// Evaluation inputs for a compiled `ScalarExpr`: positional `args` (rect/geometry/phase) and animatable `params`.
public struct KotlinExprFrame: Sendable {
    public let args: [Double]
    public let params: [Double]

    public init(args: [Double], params: [Double] = []) {
        self.args = args
        self.params = params
    }
}

/// Compiles an encoded Kotlin `ScalarExpr` AST once into a Swift closure evaluated with zero bridge calls per frame.
public enum KotlinExpr {
    public typealias Evaluator = @Sendable (KotlinExprFrame) -> Double

    public static func compile(_ raw: Any?) -> Evaluator {
        if let n = raw as? NSNumber {
            let c = n.doubleValue
            return { _ in c }
        }
        if let d = raw as? Double {
            return { _ in d }
        }
        guard let list = raw as? [Any], let op = (list.first as? NSNumber)?.intValue else {
            return { _ in 0 }
        }
        func index(_ i: Int) -> Int {
            i < list.count ? ((list[i] as? NSNumber)?.intValue ?? 0) : 0
        }
        switch op {
        case 0: // arg(i)
            let idx = index(1)
            return { idx >= 0 && idx < $0.args.count ? $0.args[idx] : 0 }
        case 1: // param(i)
            let idx = index(1)
            return { idx >= 0 && idx < $0.params.count ? $0.params[idx] : 0 }
        case 2: // neg
            let a = compile(list.count > 1 ? list[1] : nil)
            return { -a($0) }
        case 3: // abs
            let a = compile(list.count > 1 ? list[1] : nil)
            return { Swift.abs(a($0)) }
        case 4: // sqrt
            let a = compile(list.count > 1 ? list[1] : nil)
            return { let v = a($0); return v >= 0 ? Foundation.sqrt(v) : 0 }
        case 5: // sin
            let a = compile(list.count > 1 ? list[1] : nil)
            return { Foundation.sin(a($0)) }
        case 6: // cos
            let a = compile(list.count > 1 ? list[1] : nil)
            return { Foundation.cos(a($0)) }
        case 7: // floor
            let a = compile(list.count > 1 ? list[1] : nil)
            return { Foundation.floor(a($0)) }
        case 8: // ceil
            let a = compile(list.count > 1 ? list[1] : nil)
            return { Foundation.ceil(a($0)) }
        case 9: // round
            let a = compile(list.count > 1 ? list[1] : nil)
            return { Foundation.round(a($0)) }
        case 10: // add
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) + b($0) }
        case 11: // sub
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) - b($0) }
        case 12: // mul
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) * b($0) }
        case 13: // div
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { let d = b($0); return d == 0 ? 0 : a($0) / d }
        case 14: // mod
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { let d = b($0); return d == 0 ? 0 : a($0).truncatingRemainder(dividingBy: d) }
        case 15: // pow
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { Foundation.pow(a($0), b($0)) }
        case 16: // min
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { Swift.min(a($0), b($0)) }
        case 17: // max
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { Swift.max(a($0), b($0)) }
        case 18: // lt
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) < b($0) ? 1 : 0 }
        case 19: // le
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) <= b($0) ? 1 : 0 }
        case 20: // gt
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) > b($0) ? 1 : 0 }
        case 21: // ge
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) >= b($0) ? 1 : 0 }
        case 22: // eq
            let a = compile(list.count > 1 ? list[1] : nil)
            let b = compile(list.count > 2 ? list[2] : nil)
            return { a($0) == b($0) ? 1 : 0 }
        case 23: // select
            let cond = compile(list.count > 1 ? list[1] : nil)
            let t = compile(list.count > 2 ? list[2] : nil)
            let f = compile(list.count > 3 ? list[3] : nil)
            return { cond($0) != 0 ? t($0) : f($0) }
        default:
            return { _ in 0 }
        }
    }
}

private enum CompiledPathCommand: Sendable {
    case moveTo(KotlinExpr.Evaluator, KotlinExpr.Evaluator)
    case lineTo(KotlinExpr.Evaluator, KotlinExpr.Evaluator)
    case quadTo(KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator)
    case curveTo(KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator)
    case close
    case ellipse(KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator)
    case rect(KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator)
    case arc(KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, Bool)
    case relativeArc(KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator)
    case roundedRect(KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, KotlinExpr.Evaluator, RoundedCornerStyle)

    static func compile(_ raw: Any) -> CompiledPathCommand? {
        guard let list = raw as? [Any], let op = (list.first as? NSNumber)?.intValue else { return nil }
        func expr(_ i: Int) -> KotlinExpr.Evaluator { KotlinExpr.compile(i < list.count ? list[i] : nil) }
        func flag(_ i: Int) -> Bool { i < list.count && ((list[i] as? NSNumber)?.intValue ?? 0) != 0 }
        switch op {
        case 0: return .moveTo(expr(1), expr(2))
        case 1: return .lineTo(expr(1), expr(2))
        case 2: return .quadTo(expr(1), expr(2), expr(3), expr(4))
        case 3: return .curveTo(expr(1), expr(2), expr(3), expr(4), expr(5), expr(6))
        case 4: return .close
        case 5: return .ellipse(expr(1), expr(2), expr(3), expr(4))
        case 6: return .rect(expr(1), expr(2), expr(3), expr(4))
        case 7: return .arc(expr(1), expr(2), expr(3), expr(4), expr(5), flag(6))
        case 8: return .relativeArc(expr(1), expr(2), expr(3), expr(4), expr(5))
        case 9: return .roundedRect(expr(1), expr(2), expr(3), expr(4), expr(5), flag(6) ? .continuous : .circular)
        default: return nil
        }
    }
}

/// Custom SwiftUI `InsettableShape` & `Animatable` compiled from Kotlin `AnimatablePathShape`.
public struct KotlinAnimatablePathShape: InsettableShape, Sendable {
    public var animatableData: KotlinAnimatableVector
    private var insetAmount: CGFloat = 0
    private let commands: [CompiledPathCommand]

    fileprivate init(parameters: [Double], commands: [CompiledPathCommand]) {
        self.animatableData = KotlinAnimatableVector(parameters)
        self.commands = commands
    }

    public func inset(by amount: CGFloat) -> KotlinAnimatablePathShape {
        var copy = self
        copy.insetAmount += amount
        return copy
    }

    public func path(in rect: CGRect) -> Path {
        let r = rect.insetBy(dx: insetAmount, dy: insetAmount)
        let frame = KotlinExprFrame(
            args: [Double(r.minX), Double(r.minY), Double(max(0, r.width)), Double(max(0, r.height))],
            params: animatableData.values
        )
        var p = Path()
        for cmd in commands {
            switch cmd {
            case let .moveTo(x, y):
                p.move(to: CGPoint(x: x(frame), y: y(frame)))
            case let .lineTo(x, y):
                p.addLine(to: CGPoint(x: x(frame), y: y(frame)))
            case let .quadTo(cx, cy, x, y):
                p.addQuadCurve(to: CGPoint(x: x(frame), y: y(frame)), control: CGPoint(x: cx(frame), y: cy(frame)))
            case let .curveTo(c1x, c1y, c2x, c2y, x, y):
                p.addCurve(
                    to: CGPoint(x: x(frame), y: y(frame)),
                    control1: CGPoint(x: c1x(frame), y: c1y(frame)),
                    control2: CGPoint(x: c2x(frame), y: c2y(frame))
                )
            case .close:
                p.closeSubpath()
            case let .ellipse(x, y, w, h):
                p.addEllipse(in: CGRect(x: x(frame), y: y(frame), width: max(0, w(frame)), height: max(0, h(frame))))
            case let .rect(x, y, w, h):
                p.addRect(CGRect(x: x(frame), y: y(frame), width: max(0, w(frame)), height: max(0, h(frame))))
            case let .arc(cx, cy, rad, start, end, clockwise):
                p.addArc(
                    center: CGPoint(x: cx(frame), y: cy(frame)),
                    radius: max(0, rad(frame)),
                    startAngle: .degrees(start(frame)),
                    endAngle: .degrees(end(frame)),
                    clockwise: clockwise
                )
            case let .relativeArc(cx, cy, rad, start, delta):
                p.addRelativeArc(
                    center: CGPoint(x: cx(frame), y: cy(frame)),
                    radius: max(0, rad(frame)),
                    startAngle: .degrees(start(frame)),
                    delta: .degrees(delta(frame))
                )
            case let .roundedRect(x, y, w, h, cr, style):
                let radius = max(0, cr(frame))
                p.addRoundedRect(
                    in: CGRect(x: x(frame), y: y(frame), width: max(0, w(frame)), height: max(0, h(frame))),
                    cornerSize: CGSize(width: radius, height: radius),
                    style: style
                )
            }
        }
        return p
    }
}

private struct CompiledVisualEffects: Sendable {
    let opacity: KotlinExpr.Evaluator
    let scale: KotlinExpr.Evaluator
    let offsetX: KotlinExpr.Evaluator
    let offsetY: KotlinExpr.Evaluator
    let rotationDegrees: KotlinExpr.Evaluator
    let blurRadius: KotlinExpr.Evaluator

    private static func compileAt(_ rawList: [Any], _ index: Int, default fallback: Double) -> KotlinExpr.Evaluator {
        if index < rawList.count { return KotlinExpr.compile(rawList[index]) }
        return { _ in fallback }
    }

    init(_ rawList: [Any]) {
        opacity = Self.compileAt(rawList, 0, default: 1)
        scale = Self.compileAt(rawList, 1, default: 1)
        offsetX = Self.compileAt(rawList, 2, default: 0)
        offsetY = Self.compileAt(rawList, 3, default: 0)
        rotationDegrees = Self.compileAt(rawList, 4, default: 0)
        blurRadius = Self.compileAt(rawList, 5, default: 0)
    }

    func apply<E: VisualEffect>(to effect: E, frame: KotlinExprFrame) -> some VisualEffect {
        let op = opacity(frame)
        let sc = CGFloat(scale(frame))
        let ox = CGFloat(offsetX(frame))
        let oy = CGFloat(offsetY(frame))
        let rot = rotationDegrees(frame)
        let bl = CGFloat(Swift.max(0.0, blurRadius(frame)))
        return effect
            .opacity(op)
            .scaleEffect(sc)
            .offset(x: ox, y: oy)
            .rotationEffect(.degrees(rot))
            .blur(radius: bl)
    }
}

@MainActor enum ExpressionRenderers {
    static func decode(_ e: SExpr) -> Any? {
        guard e.id == 16 else { return nil }
        let a = Args(expr: e)
        let params = a.list(0).compactMap { ($0 as? NSNumber)?.doubleValue }
        let commands = a.list(1).compactMap { CompiledPathCommand.compile($0) }
        return KotlinAnimatablePathShape(parameters: params, commands: commands)
    }

    static func register() {
        ComposeRegistry.modifiers[100_116] = { v, a, _ in
            let compiled = CompiledVisualEffects(a.list(0))
            return AnyView(v.visualEffect { effect, geometry in
                let frame = geometry.frame(in: .global)
                let inputs = KotlinExprFrame(args: [
                    Double(frame.width), Double(frame.height), Double(frame.minX), Double(frame.minY),
                ])
                return compiled.apply(to: effect, frame: inputs)
            })
        }

        ComposeRegistry.modifiers[100_117] = { v, a, _ in
            let configuration = d_ScrollTransitionConfiguration(a.expr(0)) ?? .interactive
            let compiled = CompiledVisualEffects(a.list(2))
            if let axis = d_Axis(a.expr(1)) {
                return AnyView(v.scrollTransition(configuration, axis: axis) { effect, phase in
                    compiled.apply(to: effect, frame: KotlinExprFrame(args: [phase.value]))
                })
            }
            return AnyView(v.scrollTransition(configuration) { effect, phase in
                compiled.apply(to: effect, frame: KotlinExprFrame(args: [phase.value]))
            })
        }
    }
}
