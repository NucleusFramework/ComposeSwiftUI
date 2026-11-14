import SwiftUI

// Hand-written renderers complementing the generated ones (ids >= 100_000).

@MainActor func d_CGPoint(_ v: Any?) -> CGPoint? {
    guard let e = v as? SExpr, e.id == 1 else { return nil }
    let a = Args(expr: e)
    return CGPoint(x: a.double(0), y: a.double(1))
}

@MainActor func d_CGSize(_ v: Any?) -> CGSize? {
    guard let e = v as? SExpr, e.id == 2 else { return nil }
    let a = Args(expr: e)
    return CGSize(width: a.double(0), height: a.double(1))
}

@MainActor func d_CGRect(_ v: Any?) -> CGRect? {
    guard let e = v as? SExpr, e.id == 3 else { return nil }
    let a = Args(expr: e)
    return CGRect(x: a.double(0), y: a.double(1), width: a.double(2), height: a.double(3))
}

@MainActor func d_CGVector(_ v: Any?) -> CGVector? {
    guard let e = v as? SExpr, e.id == 4 else { return nil }
    let a = Args(expr: e)
    return CGVector(dx: a.double(0), dy: a.double(1))
}

@MainActor enum HandwrittenRenderers {
    static func register() {
        ComposeRegistry.views[100_000] = { m in
            let a = m.args
            guard let shape = dp_Shape(a.raw(0)) else { return AnyView(EmptyView()) }
            let fill = dp_ShapeStyle(a.raw(1)).map { AnyShapeStyle($0) }
            let stroke = dp_ShapeStyle(a.raw(2)).map { AnyShapeStyle($0) }
            let lineWidth = a.cgFloat(3)
            let style = d_StrokeStyle(a.raw(4)) ?? StrokeStyle(lineWidth: lineWidth)
            let trimFrom = a.values.count > 5 ? a.cgFloat(5) : 0
            let trimTo = a.values.count > 6 ? a.cgFloat(6) : 1
            func draw<S: Shape>(_ s: S) -> AnyView {
                switch (fill, stroke) {
                case let (f?, st?): return AnyView(s.fill(f).overlay(s.stroke(st, style: style)))
                case let (f?, nil): return AnyView(s.fill(f))
                case let (nil, st?): return AnyView(s.stroke(st, style: style))
                case (nil, nil): return AnyView(s)
                }
            }
            func render<S: Shape>(_ base: S) -> AnyView {
                // trimming only when asked: an identity trim would still change the rendered path type
                trimFrom == 0 && trimTo == 1 ? draw(base) : draw(base.trim(from: trimFrom, to: trimTo))
            }
            return render(shape)
        }
        ComposeRegistry.views[100_001] = { m in
            let report = m.args.callback(0, box: { (size: CGSize) -> Any? in
                [KotlinDouble(value: Double(size.width)), KotlinDouble(value: Double(size.height))] as NSArray
            })
            return AnyView(GeometryReader { proxy in
                ChildrenView(m)
                    .frame(width: proxy.size.width, height: proxy.size.height, alignment: .topLeading)
                    .onAppear { report(proxy.size) }
                    .onChange(of: proxy.size) { _, size in report(size) }
            })
        }
        ComposeRegistry.modifiers[100_000] = { v, a, _ in
            guard let placement = d_ToolbarItemPlacement(a.raw(0)) else { return v }
            let content = a.slot(1)
            return AnyView(v.toolbar { ToolbarItem(placement: placement) { content } })
        }
    }
}

extension ComposeGenerated {
    /// Registers generated + hand-written renderers. Call once at app start.
    @MainActor public static func setUp() {
        registerAll()
        HandwrittenRenderers.register()
    }
}
