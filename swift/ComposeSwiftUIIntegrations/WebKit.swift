// WebKit for Compose SwiftUI: WebView and the webView modifiers (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerWebKit()` at launch.
#if os(iOS) || os(macOS) || os(visionOS)
import SwiftUI
import WebKit

public extension ComposeIntegrations {
    @MainActor static func registerWebKit() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        ComposeRegistry.views[200_301] = { m in ModChain.apply(KotlinWebView(m: m), m, 0) }
        ComposeRegistry.modifiers[200_304] = { v, a, _ in WebModifiers.style(v, a) }
    }
}

/// The Kotlin `WebViewController`: navigation commands on the page.
@available(iOS 26.0, macOS 26.0, *)
@MainActor final class WebPageBox: NSObject, @preconcurrency KotlinFunction {
    let page = WebPage()

    func invoke(args: [Any]) -> Any? {
        let a = args
        switch (a.first as? NSNumber)?.intValue ?? -1 {
        case 0: if let s = a[1] as? String, let url = URL(string: s) { page.load(URLRequest(url: url)) }
        case 1: page.load(html: a[1] as? String ?? "", baseURL: (a.count > 2 ? a[2] as? String : nil).flatMap(URL.init(string:)) ?? URL(string: "about:blank")!)
        case 2: page.reload()
        case 3: page.stopLoading()
        case 4: if let item = page.backForwardList.backList.last { page.load(item) }
        case 5: if let item = page.backForwardList.forwardList.first { page.load(item) }
        case 6:
            let script = a[1] as? String ?? ""
            let done = a.count > 2 ? a[2] as? SValueCallback : nil
            Task { @MainActor in
                do {
                    let result = try await page.callJavaScript(script)
                    done?.invoke(value: result.map { "\($0)" } ?? NSNull())
                } catch {
                    done?.invoke(value: error.localizedDescription)
                }
                ComposeSwiftUIHostKt.flushCompose()
            }
        default: break
        }
        return nil
    }
}

@available(iOS 26.0, macOS 26.0, *)
private struct KotlinWebView: View {
    let m: NodeModel
    @State private var box = WebPageBox()
    @State private var loadedURL: String?

    var body: some View {
        let _ = m.version
        let a = m.args
        let page = box.page
        let state: [Any] = [page.title, (page.url?.absoluteString as Any?) ?? NSNull(), KotlinBoolean(value: page.isLoading),
                            KotlinDouble(value: page.estimatedProgress), KotlinBoolean(value: !page.backForwardList.backList.isEmpty),
                            KotlinBoolean(value: !page.backForwardList.forwardList.isEmpty)]
        let key = "\(page.title)|\(page.url?.absoluteString ?? "")|\(page.isLoading)|\(Int(page.estimatedProgress * 100))"
        menu(WebView(page), a)
            .task(id: a.stringOpt(0)) {
                guard let s = a.stringOpt(0), s != loadedURL, let url = URL(string: s) else { return }
                loadedURL = s
                page.load(URLRequest(url: url))
            }
            .onAppear {
                if !a.isNil(1) { a.callback(1, box: { (b: WebPageBox) -> Any? in b })(box) }
            }
            .onChange(of: key, initial: true) { _, _ in
                if !a.isNil(2) { a.callback(2, box: { (x: NSArray) -> Any? in x })(state as NSArray) }
            }
    }

    @ViewBuilder private func menu(_ view: some View, _ a: Args) -> some View {
        #if os(macOS)
        if let slot = a.slotModel(3) {
            let report = a.callback(4, box: { (s: String?) -> Any? in s ?? NSNull() })
            view.webViewContextMenu { element in
                SlotView(model: slot).onAppear { report(element.linkURL?.absoluteString) }
            }
        } else {
            view
        }
        #else
        view
        #endif
    }
}

@available(iOS 26.0, macOS 26.0, *)
@MainActor private enum WebModifiers {
    static func style(_ view: AnyView, _ a: Args) -> AnyView {
        var v = view
        if let i = a.intOpt(0) {
            let b: WebView.BackForwardNavigationGesturesBehavior = i == 1 ? .enabled : i == 2 ? .disabled : .automatic
            v = AnyView(v.webViewBackForwardNavigationGestures(b))
        }
        if let i = a.intOpt(1) {
            let b: WebView.MagnificationGesturesBehavior = i == 1 ? .enabled : i == 2 ? .disabled : .automatic
            v = AnyView(v.webViewMagnificationGestures(b))
        }
        if let i = a.intOpt(2) {
            let b: WebView.LinkPreviewBehavior = i == 1 ? .enabled : i == 2 ? .disabled : .automatic
            v = AnyView(v.webViewLinkPreviews(b))
        }
        if let i = a.intOpt(3) {
            let b: WebView.ElementFullscreenBehavior = i == 1 ? .enabled : i == 2 ? .disabled : .automatic
            v = AnyView(v.webViewElementFullscreenBehavior(b))
        }
        if let enabled = a.boolOpt(4) {
            v = enabled ? AnyView(v.webViewTextSelection(.enabled)) : AnyView(v.webViewTextSelection(.disabled))
        }
        if let vis = d_Visibility(a.expr(5)) { v = AnyView(v.webViewContentBackground(vis)) }
        if let behavior = d_ScrollInputBehavior(a.expr(6)), let kind = d_ScrollInputKind(a.expr(7)) {
            v = AnyView(v.webViewScrollInputBehavior(behavior, for: kind))
        }
        if let position = d_ScrollPosition(a.expr(8)) {
            v = AnyView(v.webViewScrollPosition(.constant(position)))
        }
        if !a.isNil(9) {
            let report = a.callback(9, box: { (l: [Double]) -> Any? in l.map { KotlinDouble(value: $0) } as NSArray })
            v = AnyView(v.webViewOnScrollGeometryChange(for: [Double].self, of: { g in
                [g.contentOffset.x, g.contentOffset.y, g.contentSize.width, g.contentSize.height].map { Double($0) }
            }) { _, new in report(new) })
        }
        return v
    }
}
#endif
