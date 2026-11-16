// JournalingSuggestions for Compose SwiftUI: JournalingSuggestionsPicker (runtime AppFrameworks.kt; iOS devices).
// Add this file to the app target and call `ComposeIntegrations.registerJournalingSuggestions()` at launch.
import SwiftUI

#if canImport(JournalingSuggestions)
import JournalingSuggestions

public extension ComposeIntegrations {
    @MainActor static func registerJournalingSuggestions() {
        guard #available(iOS 17.2, *) else { return }
        ComposeRegistry.views[200_349] = { m in
            let a = m.args
            let done = Journaling.report(a, 1)
            if let label = a.slotModel(2) {
                return ModChain.apply(JournalingSuggestionsPicker { SlotView(model: label) } onCompletion: { await done($0) }, m, 0)
            }
            return ModChain.apply(JournalingSuggestionsPicker(LocalizedStringKey(a.string(0))) { await done($0) }, m, 0)
        }
        ComposeRegistry.modifiers[200_355] = { v, a, _ in
            let done = Journaling.report(a, 2)
            return AnyView(v.journalingSuggestionsPicker(isPresented: a.boolBinding(0)) { await done($0) })
        }
    }
}

@available(iOS 17.2, *)
@MainActor private enum Journaling {
    static func report(_ a: Args, _ i: Int) -> (JournalingSuggestion) async -> Void {
        let cb = a.callback(i, box: { (x: NSArray) -> Any? in x })
        return { s in
            let start: Any = s.date.map { KotlinDouble(value: $0.start.timeIntervalSince1970) } ?? NSNull()
            let end: Any = s.date.map { KotlinDouble(value: $0.end.timeIntervalSince1970) } ?? NSNull()
            await MainActor.run { cb([s.title, start, end, KotlinInt(value: Int32(s.items.count))] as NSArray) }
        }
    }
}
#endif
