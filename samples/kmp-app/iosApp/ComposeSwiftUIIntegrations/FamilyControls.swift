// FamilyControls for Compose SwiftUI: FamilyActivityPicker and its labels (runtime AppFrameworks.kt). Add this file
// to the app target, enable the Family Controls capability and call `ComposeIntegrations.registerFamilyControls()`.
import SwiftUI

#if os(iOS)
import FamilyControls
import ManagedSettings

public extension ComposeIntegrations {
    @MainActor static func registerFamilyControls() {
        guard #available(iOS 16.0, *) else { return }
        ComposeRegistry.views[200_345] = { m in
            let a = m.args
            return ModChain.apply(FamilyActivityPicker(headerText: a.stringOpt(2), footerText: a.stringOpt(3), selection: FamilyJSON.binding(a, 0)), m, 0)
        }
        ComposeRegistry.modifiers[200_350] = { v, a, _ in
            if #available(iOS 26.2, *), !a.isNil(4) {
                return AnyView(v.familyActivityPicker(title: a.stringOpt(4), headerText: a.stringOpt(5), footerText: a.stringOpt(6),
                                                      isPresented: a.boolBinding(0), selection: FamilyJSON.binding(a, 2)))
            }
            return AnyView(v.familyActivityPicker(headerText: a.stringOpt(5), footerText: a.stringOpt(6), isPresented: a.boolBinding(0),
                                                  selection: FamilyJSON.binding(a, 2)))
        }
        ComposeRegistry.views[200_346] = { m in
            let a = m.args
            let data = Data(a.string(0).utf8)
            let label: AnyView
            switch a.int(1) {
            case 0: label = (try? JSONDecoder().decode(ApplicationToken.self, from: data)).map { AnyView(Label($0)) } ?? AnyView(EmptyView())
            case 1: label = (try? JSONDecoder().decode(ActivityCategoryToken.self, from: data)).map { AnyView(Label($0)) } ?? AnyView(EmptyView())
            default: label = (try? JSONDecoder().decode(WebDomainToken.self, from: data)).map { AnyView(Label($0)) } ?? AnyView(EmptyView())
            }
            return ModChain.apply(label, m, 0)
        }
    }
}

/// A FamilyActivitySelection as its JSON encoding, which Kotlin keeps opaque.
@available(iOS 16.0, *)
@MainActor enum FamilyJSON {
    static func binding(_ a: Args, _ i: Int) -> Binding<FamilyActivitySelection> {
        let report = a.callback(i + 1, box: { (s: String) -> Any? in s })
        return Binding(get: {
            a.stringOpt(i).flatMap { try? JSONDecoder().decode(FamilyActivitySelection.self, from: Data($0.utf8)) } ?? FamilyActivitySelection()
        }, set: { selection in
            if let data = try? JSONEncoder().encode(selection) { report(String(decoding: data, as: UTF8.self)) }
        })
    }
}
#endif
