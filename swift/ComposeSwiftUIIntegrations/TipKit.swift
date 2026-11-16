// TipKit for Compose SwiftUI: TipView, popoverTip and the tip style modifiers (runtime AppFrameworks.kt). Add this
// file to the app target and call `ComposeIntegrations.registerTipKit()` at launch: it runs `Tips.configure(options)`.
import SwiftUI
import TipKit

public extension ComposeIntegrations {
    @MainActor static func registerTipKit(_ options: [Tips.ConfigurationOption] = []) {
        guard #available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *) else { return }
        try? Tips.configure(options)
        ComposeRegistry.views[200_340] = { m in ModChain.apply(KotlinTipView(m: m), m, 0) }
        #if !os(watchOS)
        ComposeRegistry.modifiers[200_340] = { v, a, _ in TipModifiers.popover(v, a) }
        #endif
        ComposeRegistry.modifiers[200_341] = { v, a, _ in TipModifiers.style(v, a) }
        if #available(iOS 26.0, macOS 26.0, tvOS 26.0, watchOS 26.0, *) {
            ComposeRegistry.modifiers[200_342] = { v, a, _ in AnyView(v.tipAnchor(a.string(0))) }
        }
    }
}

/// A Kotlin `Tip`: id, title, message, SF Symbol and actions.
@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
struct KotlinTip: Tip {
    let tipID: String
    let titleText: String
    let messageText: String?
    let systemImage: String?
    let actionList: [[String]]
    var maxDisplayCount: Int? = nil
    var maxDisplayDuration: Double? = nil
    var ignoresDisplayFrequency = false

    var id: String { tipID }
    var title: Text { Text(titleText) }
    var message: Text? { messageText.map { Text($0) } }
    var image: Image? { systemImage.map { Image(systemName: $0) } }
    var actions: [Tips.Action] { actionList.map { Tips.Action(id: $0[0], title: $0[1]) } }
    var options: [any TipOption] {
        var o: [any TipOption] = []
        if let maxDisplayCount { o.append(Tips.MaxDisplayCount(maxDisplayCount)) }
        if let maxDisplayDuration, #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) { o.append(Tips.MaxDisplayDuration(maxDisplayDuration)) }
        if ignoresDisplayFrequency { o.append(Tips.IgnoresDisplayFrequency(true)) }
        return o
    }

    @MainActor static func decode(_ a: Args, _ i: Int) -> KotlinTip? {
        guard !a.isNil(i) else { return nil }
        let l = a.list(i)
        func s(_ k: Int) -> String? { k < l.count ? l[k] as? String : nil }
        let acts = (l.count > 4 ? l[4] as? [Any] ?? [] : []).compactMap { $0 as? [Any] }.map { $0.map { $0 as? String ?? "" } }
        func value(_ k: Int) -> NSNumber? { k < l.count ? l[k] as? NSNumber : nil }
        let tip = KotlinTip(tipID: s(0) ?? "", titleText: s(1) ?? "", messageText: s(2), systemImage: s(3), actionList: acts,
                            maxDisplayCount: value(6)?.intValue, maxDisplayDuration: value(7)?.doubleValue,
                            ignoresDisplayFrequency: value(8)?.boolValue ?? false)
        // The user did what the tip suggests: TipKit stops showing it (it persists, so this is idempotent).
        if value(5)?.boolValue == true { tip.invalidate(reason: .actionPerformed) }
        return tip
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
private struct KotlinTipView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let a = m.args
        let tip = KotlinTip.decode(a, 0)
        let edge = d_Edge(a.expr(1))
        let report = a.callback(5, box: { (s: String) -> Any? in s })
        let action: @MainActor @Sendable (Tips.Action) -> Void = { report($0.id) }
        if #available(iOS 26.0, macOS 26.0, tvOS 26.0, watchOS 26.0, *) {
            let presented: Binding<Bool>? = a.isNil(2) ? nil : a.boolBinding(2)
            if let anchor = a.stringOpt(4) {
                TipView(tip, isPresented: presented, arrowEdge: edge, anchorID: anchor, action: action)
            } else {
                TipView(tip, isPresented: presented, arrowEdge: edge, action: action)
            }
        } else {
            TipView(tip ?? KotlinTip(tipID: "", titleText: "", messageText: nil, systemImage: nil, actionList: []), arrowEdge: edge ?? .top, action: action)
        }
    }
}

@available(iOS 17.0, macOS 14.0, tvOS 17.0, watchOS 10.0, *)
@MainActor private enum TipModifiers {
    #if !os(watchOS)
    static func popover(_ v: AnyView, _ a: Args) -> AnyView {
        let tip = KotlinTip.decode(a, 0)
        let report = a.callback(6, box: { (s: String) -> Any? in s })
        let action: @MainActor @Sendable (Tips.Action) -> Void = { report($0.id) }
        if #available(iOS 26.0, macOS 26.0, tvOS 26.0, *) {
            let presented: Binding<Bool>? = a.isNil(4) ? nil : a.boolBinding(4)
            let anchor = d_PopoverAttachmentAnchor(a.expr(3)) ?? .rect(.bounds)
            #if !os(macOS)
            if let edges = d_Edge_Set(a.expr(2)) {
                return AnyView(v.popoverTip(tip, isPresented: presented, attachmentAnchor: anchor, arrowEdges: edges, action: action))
            }
            #endif
            return AnyView(v.popoverTip(tip, isPresented: presented, attachmentAnchor: anchor, arrowEdge: d_Edge(a.expr(1)), action: action))
        }
        guard let tip, #available(tvOS 18.0, *) else { return v }
        return AnyView(v.popoverTip(tip, arrowEdge: d_Edge(a.expr(1)) ?? .top, action: action))
    }
    #endif

    static func style(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        if let s = dp_ShapeStyle(a.expr(0)) { v = AnyView(v.tipBackground(AnyShapeStyle(s))) }
        if let r = a.cgFloatOpt(1) { v = AnyView(v.tipCornerRadius(r, antialiased: a.bool(2))) }
        if let size = d_CGSize(a.expr(3)) { v = AnyView(v.tipImageSize(size)) }
        if #available(iOS 18.0, macOS 15.0, tvOS 18.0, watchOS 11.0, *) {
            let styles = a.exprList(4).compactMap { dp_ShapeStyle($0).map { AnyShapeStyle($0) } }
            switch styles.count {
            case 0: break
            case 1: v = AnyView(v.tipImageStyle(styles[0]))
            case 2: v = AnyView(v.tipImageStyle(styles[0], styles[1]))
            default: v = AnyView(v.tipImageStyle(styles[0], styles[1], styles[2]))
            }
        }
        #if !os(watchOS)
        if #available(iOS 26.0, macOS 26.0, tvOS 26.0, *), let i = d_PresentationBackgroundInteraction(a.expr(5)) {
            v = AnyView(v.tipBackgroundInteraction(i))
        }
        #endif
        if a.bool(6) { v = AnyView(v.tipViewStyle(.miniTip)) }
        return v
    }
}
