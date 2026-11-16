// ClassKitUI for Compose SwiftUI: assigned document labels, submission button and events (runtime AppFrameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerClassKitUI()` at launch.
import SwiftUI

#if os(iOS) || os(macOS)
import ClassKitUI

public extension ComposeIntegrations {
    @MainActor static func registerClassKitUI() {
        guard #available(iOS 27.0, macOS 27.0, *) else { return }
        func url(_ s: String) -> URL { s.contains("://") ? URL(string: s) ?? URL(fileURLWithPath: s) : URL(fileURLWithPath: s) }
        ComposeRegistry.views[200_350] = { m in
            ModChain.apply(AssignedDocumentLabel(role: m.args.int(0) == 1 ? .date : .status, documentURL: url(m.args.string(1))), m, 0)
        }
        ComposeRegistry.views[200_351] = { m in ModChain.apply(AssignedDocumentSubmissionButton(documentURL: url(m.args.string(0))), m, 0) }
        ComposeRegistry.modifiers[200_357] = { view, a, _ in
            var v = view
            func ask(_ i: Int) -> @Sendable (URL) async -> Bool {
                let f = UncheckedSendable(a.raw(i))
                return { u in await MainActor.run { (kotlinInvoke(f.value, [u.absoluteString]) as? NSNumber)?.boolValue ?? true } }
            }
            if !a.isNil(0) { v = AnyView(v.onAssignedDocumentWillSubmit(ask(0))) }
            if !a.isNil(1) { let r = a.callback(1, box: { (s: String) -> Any? in s }); v = AnyView(v.onAssignedDocumentDidSubmit { r($0.absoluteString) }) }
            if !a.isNil(2) { v = AnyView(v.onAssignedDocumentWillWithdraw(ask(2))) }
            if !a.isNil(3) { let r = a.callback(3, box: { (s: String) -> Any? in s }); v = AnyView(v.onAssignedDocumentDidWithdraw { r($0.absoluteString) }) }
            return v
        }
    }
}

#endif
