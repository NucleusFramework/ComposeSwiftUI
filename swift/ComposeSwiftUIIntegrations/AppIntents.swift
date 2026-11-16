// AppIntents for Compose SwiftUI: ShortcutsLink and the Shortcuts / Siri tip styles (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerAppIntents()` at launch.
import SwiftUI

#if os(iOS) || os(macOS) || os(watchOS) || os(visionOS)
import AppIntents

public extension ComposeIntegrations {
    @MainActor static func registerAppIntents() {
        #if os(iOS) || os(visionOS)
        ComposeRegistry.views[200_321] = { m in ModChain.apply(ShortcutsLink(action: m.args.action(0)), m, 0) }
        #endif
        ComposeRegistry.modifiers[200_321] = { view, a, _ in
            var v = view
            #if os(iOS) || os(visionOS)
            if let i = a.intOpt(0) {
                let styles: [ShortcutsLinkStyle] = [.automatic, .automaticOutline, .dark, .darkOutline, .light, .lightOutline]
                v = AnyView(v.shortcutsLinkStyle(styles[min(max(i, 0), 5)]))
            }
            #endif
            #if !os(macOS)
            if let i = a.intOpt(1) {
                let style: SiriTipViewStyle = i == 2 || i == 3 ? .dark : i == 4 || i == 5 ? .light : .automatic
                v = AnyView(v.siriTipViewStyle(style))
            }
            #endif
            return v
        }
    }
}
#endif
