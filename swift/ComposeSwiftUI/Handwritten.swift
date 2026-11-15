import SwiftUI

// Hand-written renderers complementing the generated ones (ids >= 100_000).

@MainActor func d_CGPoint(_ e: SExpr?) -> CGPoint? {
    guard let e, e.id == 1 else { return nil }
    let a = Args(expr: e)
    return CGPoint(x: a.double(0), y: a.double(1))
}

@MainActor func d_CGSize(_ e: SExpr?) -> CGSize? {
    guard let e, e.id == 2 else { return nil }
    let a = Args(expr: e)
    return CGSize(width: a.double(0), height: a.double(1))
}

@MainActor func d_CGRect(_ e: SExpr?) -> CGRect? {
    guard let e, e.id == 3 else { return nil }
    let a = Args(expr: e)
    return CGRect(x: a.double(0), y: a.double(1), width: a.double(2), height: a.double(3))
}

@MainActor func d_CGVector(_ e: SExpr?) -> CGVector? {
    guard let e, e.id == 4 else { return nil }
    let a = Args(expr: e)
    return CGVector(dx: a.double(0), dy: a.double(1))
}

@MainActor enum HandwrittenRenderers {
    static func register() {
        ComposeRegistry.views[100_000] = { m in
            let a = m.args
            guard let shape = dp_Shape(a.expr(0)) else { return AnyView(EmptyView()) }
            let fill = dp_ShapeStyle(a.expr(1)).map { AnyShapeStyle($0) }
            let stroke = dp_ShapeStyle(a.expr(2)).map { AnyShapeStyle($0) }
            let lineWidth = a.cgFloat(3)
            let style = d_StrokeStyle(a.expr(4)) ?? StrokeStyle(lineWidth: lineWidth)
            let trimFrom = a.cgFloat(5)
            let trimTo = a.isNil(6) ? 1 : a.cgFloat(6)
            @MainActor func draw<S: Shape>(_ s: S) -> AnyView {
                switch (fill, stroke) {
                case let (f?, st?): return ModChain.apply(s.fill(f).overlay(s.stroke(st, style: style)), m, 0)
                case let (f?, nil): return ModChain.apply(s.fill(f), m, 0)
                case let (nil, st?): return ModChain.apply(s.stroke(st, style: style), m, 0)
                case (nil, nil): return ModChain.apply(s, m, 0)
                }
            }
            @MainActor func render<S: Shape>(_ base: S) -> AnyView {
                // trimming only when asked: an identity trim would still change the rendered path type
                trimFrom == 0 && trimTo == 1 ? draw(base) : draw(base.trim(from: trimFrom, to: trimTo))
            }
            return render(shape)
        }
        ComposeRegistry.views[100_001] = { m in
            let report = m.args.callback(0, box: { (size: CGSize) -> Any? in
                [KotlinDouble(value: Double(size.width)), KotlinDouble(value: Double(size.height))] as NSArray
            })
            return ModChain.apply(GeometryReader { proxy in
                ChildrenView(m)
                    .frame(width: proxy.size.width, height: proxy.size.height, alignment: .topLeading)
                    .onAppear { report(proxy.size) }
                    .onChange(of: proxy.size) { _, size in report(size) }
            }, m, 0)
        }
        ComposeRegistry.modifiers[100_000] = { v, a, _ in
            guard let placement = d_ToolbarItemPlacement(a.expr(0)) else { return v }
            let content = a.slot(1)
            return AnyView(v.toolbar { ToolbarItem(placement: placement) { content } })
        }
    }
}

extension ComposeGenerated {
    @MainActor private static let once: Void = setUp()

    /// Registers the renderers on first use (called by ComposeView; calling it yourself is optional).
    @MainActor public static func ensureSetUp() {
        _ = once
    }

    /// Registers generated + hand-written renderers.
    @MainActor public static func setUp() {
        registerAll()
        HandwrittenRenderers.register()
        ExtraRenderers.register()
    }
}

// MARK: - Extras (Extras.kt): data-driven navigation, environment, focus, gestures, namespaces,
// custom layouts / shapes, native views, withAnimation.

/// Values hand-written in Kotlin (ids < 1000) that generated protocol decoders may receive.
@MainActor enum HandwrittenValues {
    static func decode(_ e: SExpr) -> Any? {
        switch e.id {
        case 1: return d_CGPoint(e)
        case 2: return d_CGSize(e)
        case 3: return d_CGRect(e)
        case 4: return d_CGVector(e)
        case 5: return KotlinPathShape(commands: Args(expr: e).list(0).compactMap { ($0 as? NSNumber)?.doubleValue })
        default: return nil
        }
    }
}

/// Shape built from Kotlin `PathBuilder` commands in unit coordinates.
struct KotlinPathShape: Shape {
    let commands: [Double]

    func path(in rect: CGRect) -> Path {
        var p = Path()
        func pt(_ x: Double, _ y: Double) -> CGPoint { CGPoint(x: rect.minX + x * rect.width, y: rect.minY + y * rect.height) }
        var i = 0
        let c = commands
        while i < c.count {
            switch Int(c[i]) {
            case 0: p.move(to: pt(c[i + 1], c[i + 2])); i += 3
            case 1: p.addLine(to: pt(c[i + 1], c[i + 2])); i += 3
            case 2: p.addQuadCurve(to: pt(c[i + 3], c[i + 4]), control: pt(c[i + 1], c[i + 2])); i += 5
            case 3: p.addCurve(to: pt(c[i + 5], c[i + 6]), control1: pt(c[i + 1], c[i + 2]), control2: pt(c[i + 3], c[i + 4])); i += 7
            case 4: p.closeSubpath(); i += 1
            case 5: p.addEllipse(in: CGRect(origin: pt(c[i + 1], c[i + 2]), size: CGSize(width: c[i + 3] * rect.width, height: c[i + 4] * rect.height))); i += 5
            case 6: p.addRect(CGRect(origin: pt(c[i + 1], c[i + 2]), size: CGSize(width: c[i + 3] * rect.width, height: c[i + 4] * rect.height))); i += 5
            default: i = c.count
            }
        }
        return p
    }
}

/// Swift implementation of a Kotlin `SwiftAction`.
final class ClosureAction: NSObject, SwiftAction {
    let block: () -> Void
    init(_ block: @escaping () -> Void) { self.block = block }
    func run() { block() }
}

private struct ComposeNamespacesKey: EnvironmentKey {
    static let defaultValue: [Int64: Namespace.ID] = [:]
}

extension EnvironmentValues {
    var composeNamespaces: [Int64: Namespace.ID] {
        get { self[ComposeNamespacesKey.self] }
        set { self[ComposeNamespacesKey.self] = newValue }
    }
}

struct NamespaceScopeView: View {
    let m: NodeModel
    let id: Int64
    @Namespace private var ns
    @Environment(\.composeNamespaces) private var namespaces

    var body: some View {
        var all = namespaces
        all[id] = ns
        return ChildrenView(m).environment(\.composeNamespaces, all)
    }
}

struct MatchedGeometryModifier: ViewModifier {
    let id: KHashable
    let namespace: Int64
    let properties: MatchedGeometryProperties
    let anchor: UnitPoint
    let isSource: Bool
    @Environment(\.composeNamespaces) private var namespaces

    func body(content: Content) -> some View {
        if let ns = namespaces[namespace] {
            content.matchedGeometryEffect(id: id, in: ns, properties: properties, anchor: anchor, isSource: isSource)
        } else {
            content
        }
    }
}

struct TransitionSourceModifier: ViewModifier {
    let id: KHashable
    let namespace: Int64
    let zoom: Bool
    @Environment(\.composeNamespaces) private var namespaces

    func body(content: Content) -> some View {
        if let ns = namespaces[namespace] {
            if zoom {
                #if os(iOS) || os(tvOS) || os(watchOS)
                if #available(iOS 18.0, tvOS 18.0, watchOS 11.0, *) {
                    content.navigationTransition(.zoom(sourceID: id, in: ns))
                } else {
                    content
                }
                #else
                content
                #endif
            } else if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
                content.matchedTransitionSource(id: id, in: ns)
            } else {
                content
            }
        } else {
            content
        }
    }
}

struct FocusBridge: ViewModifier {
    let isFocused: Bool
    let onChange: (Bool) -> Void
    @FocusState private var focused: Bool

    func body(content: Content) -> some View {
        content
            .focused($focused)
            .onAppear { focused = isFocused }
            .onChange(of: isFocused) { _, value in focused = value }
            .onChange(of: focused) { _, value in if value != isFocused { onChange(value) } }
    }
}

struct EnvironmentReporter: View {
    let m: NodeModel
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    #if os(iOS)
    @Environment(\.horizontalSizeClass) private var horizontalSizeClass
    @Environment(\.verticalSizeClass) private var verticalSizeClass
    #endif
    @Environment(\.locale) private var locale
    @Environment(\.layoutDirection) private var layoutDirection
    @Environment(\.isEnabled) private var isEnabled
    @Environment(\.isPresented) private var isPresented
    @Environment(\.dismiss) private var dismiss

    private var snapshot: [String] {
        var hsc = "", vsc = ""
        #if os(iOS)
        hsc = horizontalSizeClass.map { $0 == .compact ? "compact" : "regular" } ?? ""
        vsc = verticalSizeClass.map { $0 == .compact ? "compact" : "regular" } ?? ""
        #endif
        return [colorScheme == .dark ? "dark" : "light", String(describing: dynamicTypeSize), hsc, vsc,
                locale.identifier, layoutDirection == .rightToLeft ? "rightToLeft" : "leftToRight",
                isEnabled ? "1" : "0", isPresented ? "1" : "0"]
    }

    var body: some View {
        let s = snapshot
        return ChildrenView(m)
            .onAppear { report(s) }
            .onChange(of: s) { _, value in report(value) }
    }

    private func report(_ s: [String]) {
        let dismiss = self.dismiss
        let values: [Any] = [s[0] as NSString, s[1] as NSString, s[2].isEmpty ? NSNull() : s[2] as NSString,
                             s[3].isEmpty ? NSNull() : s[3] as NSString, s[4] as NSString, s[5] as NSString,
                             KotlinBoolean(value: s[6] == "1"), KotlinBoolean(value: s[7] == "1"),
                             ClosureAction { dismiss() }]
        m.args.callback(0, box: { (v: [Any]) -> Any? in v as NSArray })(values)
    }
}

/// A SwiftUI `Layout` delegating to a Kotlin `LayoutPolicy`.
struct KotlinLayout: Layout {
    let policy: LayoutPolicy

    private func sizes(_ subviews: Subviews) -> [LayoutSize] {
        subviews.map { let s = $0.sizeThatFits(.unspecified); return LayoutSize(width: Double(s.width), height: Double(s.height)) }
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let r = policy.sizeThatFits(proposedWidth: proposal.width.map { KotlinDouble(value: Double($0)) },
                                    proposedHeight: proposal.height.map { KotlinDouble(value: Double($0)) },
                                    subviews: sizes(subviews))
        return CGSize(width: r.width, height: r.height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let placements = policy.placeSubviews(width: Double(bounds.width), height: Double(bounds.height), subviews: sizes(subviews))
        for (i, subview) in subviews.enumerated() where i < placements.count {
            let p = placements[i]
            subview.place(at: CGPoint(x: bounds.minX + p.x, y: bounds.minY + p.y), anchor: .topLeading, proposal: .unspecified)
        }
    }
}

@MainActor enum ExtraRenderers {
    static func register() {
        // NavigationStack(path:) driven by Kotlin; destinations are slots indexed by path position.
        ComposeRegistry.views[100_002] = { m in
            let node = m.node
            let pop = m.args.callback(1, box: KBox.int)
            let path = Binding<[Int]>(get: { Array(0..<Int(node.argInt(index: 0))) }, set: { pop($0.count) })
            return ModChain.apply(NavigationStack(path: path) {
                ChildrenView(m).navigationDestination(for: Int.self) { i in SlotView(model: m.slot(i)) }
            }, m, 0)
        }
        ComposeRegistry.views[100_003] = { m in ModChain.apply(EnvironmentReporter(m: m), m, 0) }
        ComposeRegistry.views[100_004] = { m in ModChain.apply(NamespaceScopeView(m: m, id: Int64(m.args.int(0))), m, 0) }
        ComposeRegistry.views[100_005] = { m in
            guard let policy = m.args.raw(0) as? LayoutPolicy else { return AnyView(ChildrenView(m)) }
            return ModChain.apply(KotlinLayout(policy: policy) { ChildrenView(m) }, m, 0)
        }
        ComposeRegistry.views[100_006] = { m in
            let a = m.args
            guard let factory = ComposeRegistry.nativeViews[a.string(0)] else {
                return AnyView(Text("No native view '\(a.string(0))'").foregroundStyle(.red))
            }
            return ModChain.apply(factory(a), m, 0)
        }

        ComposeRegistry.modifiers[100_001] = { v, a, _ in
            AnyView(v.modifier(FocusBridge(isFocused: a.bool(0), onChange: a.callback(1, box: KBox.bool))))
        }
        #if !os(tvOS)
        ComposeRegistry.modifiers[100_002] = { v, a, _ in
            func values(_ g: DragGesture.Value) -> Any? {
                [g.translation.width, g.translation.height, g.location.x, g.location.y, g.startLocation.x, g.startLocation.y,
                 g.predictedEndTranslation.width, g.predictedEndTranslation.height, g.velocity.width, g.velocity.height]
                    .map { KotlinDouble(value: Double($0)) } as NSArray
            }
            let changed = a.callback(2, box: values), ended = a.callback(3, box: values)
            let gesture = DragGesture(minimumDistance: a.cgFloat(0)).onChanged { changed($0) }.onEnded { ended($0) }
            return a.bool(1) ? AnyView(v.simultaneousGesture(gesture)) : AnyView(v.gesture(gesture))
        }
        #if os(iOS) || os(macOS)
        ComposeRegistry.modifiers[100_003] = { v, a, _ in
            let changed = a.callback(1, box: KBox.double), ended = a.callback(2, box: KBox.double)
            let gesture = MagnifyGesture().onChanged { changed(Double($0.magnification)) }.onEnded { ended(Double($0.magnification)) }
            return a.bool(0) ? AnyView(v.simultaneousGesture(gesture)) : AnyView(v.gesture(gesture))
        }
        ComposeRegistry.modifiers[100_004] = { v, a, _ in
            let changed = a.callback(1, box: KBox.double), ended = a.callback(2, box: KBox.double)
            let gesture = RotateGesture().onChanged { changed($0.rotation.degrees) }.onEnded { ended($0.rotation.degrees) }
            return a.bool(0) ? AnyView(v.simultaneousGesture(gesture)) : AnyView(v.gesture(gesture))
        }
        #endif
        ComposeRegistry.modifiers[100_005] = { v, a, _ in
            let tapped = a.callback(1, box: { (p: CGPoint) -> Any? in [KotlinDouble(value: Double(p.x)), KotlinDouble(value: Double(p.y))] as NSArray })
            return AnyView(v.gesture(SpatialTapGesture(count: a.int(0)).onEnded { tapped($0.location) }))
        }
        #endif
        ComposeRegistry.modifiers[100_006] = { v, a, _ in
            AnyView(v.modifier(MatchedGeometryModifier(
                id: KHashable(a.raw(0) as? NSObject ?? NSNull()), namespace: Int64(a.int(1)),
                properties: d_MatchedGeometryProperties(a.expr(2)) ?? .frame, anchor: d_UnitPoint(a.expr(3)) ?? .center,
                isSource: a.bool(4))))
        }
        ComposeRegistry.modifiers[100_007] = { v, a, _ in
            AnyView(v.modifier(TransitionSourceModifier(id: KHashable(a.raw(0) as? NSObject ?? NSNull()), namespace: Int64(a.int(1)), zoom: false)))
        }
        ComposeRegistry.modifiers[100_008] = { v, a, _ in
            AnyView(v.modifier(TransitionSourceModifier(id: KHashable(a.raw(0) as? NSObject ?? NSNull()), namespace: Int64(a.int(1)), zoom: true)))
        }

        SwiftUIBridge.shared.animationRunner = { expr, block in
            withAnimation(d_Animation(expr)) {
                _ = block()
                ComposeSwiftUIHostKt.flushCompose()
            }
        }
    }
}
