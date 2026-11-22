// GameSave for Compose SwiftUI: gameSaveSyncingAlert (runtime AppFrameworks.kt). Add this file to the app target
// and call `ComposeIntegrations.registerGameSave()` at launch.
import SwiftUI

#if (os(iOS) || os(macOS)) && compiler(>=6.4)  // SDK 27 only: absent with Xcode 26
import GameSave

public extension ComposeIntegrations {
    @MainActor static func registerGameSave() {
        guard #available(iOS 27.0, macOS 27.0, *) else { return }
        ComposeRegistry.modifiers[200_356] = { v, a, _ in AnyView(v.modifier(GameSaveAlert(a: a))) }
    }
}

@available(iOS 27.0, macOS 27.0, *)
private struct GameSaveAlert: ViewModifier {
    let a: Args
    @State private var directory: GameSaveSyncedDirectory?

    func body(content: Content) -> some View {
        let report = a.callback(2, box: { (s: String) -> Any? in s })
        content
            .gameSaveSyncingAlert(directory: $directory) {
                switch directory?.state {
                case .ready(let url), .offline(let url), .local(let url): report(url.absoluteString)
                default: break
                }
            }
            .onChange(of: a.bool(0), initial: true) { _, open in
                directory = open ? GameSaveSyncedDirectory.openDirectory(containerIdentifier: a.stringOpt(1)) : nil
            }
    }
}
#endif
