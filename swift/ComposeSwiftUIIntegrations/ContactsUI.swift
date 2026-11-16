// ContactsUI for Compose SwiftUI: ContactAccessButton and contactAccessPicker (runtime AppFrameworks.kt). Add this
// file to the app target and call `ComposeIntegrations.registerContactsUI()` at launch.
import SwiftUI

#if os(iOS)
import ContactsUI

public extension ComposeIntegrations {
    @MainActor static func registerContactsUI() {
        guard #available(iOS 18.0, *) else { return }
        ComposeRegistry.views[200_344] = { m in
            let a = m.args
            let report = a.callback(3, box: { (ids: [String]) -> Any? in ids as NSArray })
            let emails = a.isNil(1) ? nil : Set(a.list(1).compactMap { $0 as? String })
            let phones = a.isNil(2) ? nil : Set(a.list(2).compactMap { $0 as? String })
            return ModChain.apply(ContactAccessButton(queryString: a.string(0), ignoredEmails: emails, ignoredPhoneNumbers: phones,
                                                      approvalCallback: { report($0) }), m, 0)
        }
        ComposeRegistry.modifiers[200_348] = { view, a, _ in
            var v = view
            if let i = a.intOpt(0) {
                let captions: [ContactAccessButton.Caption] = [.defaultText, .email, .phone]
                v = AnyView(v.contactAccessButtonCaption(captions[min(max(i, 0), 2)]))
            }
            if !a.isNil(1) || !a.isNil(2) || !a.isNil(3) {
                v = AnyView(v.contactAccessButtonStyle(ContactAccessButton.Style(imageTrailingEdgePadding: a.cgFloatOpt(1), imageWidth: a.cgFloatOpt(2),
                                                                                  imageColor: d_Color(a.expr(3)))))
            }
            return v
        }
        ComposeRegistry.modifiers[200_349] = { v, a, _ in
            let report = a.callback(2, box: { (ids: [String]) -> Any? in ids as NSArray })
            return AnyView(v.contactAccessPicker(isPresented: a.boolBinding(0)) { report($0) })
        }
    }
}

#endif
