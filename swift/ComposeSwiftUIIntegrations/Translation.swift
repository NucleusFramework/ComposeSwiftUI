// Translation for Compose SwiftUI: translationPresentation and translationTask (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerTranslation()` at launch.
#if os(iOS) || os(macOS) || os(visionOS)
import SwiftUI
import Translation

public extension ComposeIntegrations {
    @MainActor static func registerTranslation() {
        guard #available(iOS 18.0, macOS 15.0, *) else { return }
        ComposeRegistry.modifiers[200_311] = { v, a, _ in
            let edge = d_Edge(a.expr(3)) ?? .top
            if a.isNil(4) {
                return AnyView(v.translationPresentation(isPresented: a.boolBinding(0), text: a.string(2), arrowEdge: edge))
            }
            let replace = a.callback(4, box: { (s: String) -> Any? in s })
            return AnyView(v.translationPresentation(isPresented: a.boolBinding(0), text: a.string(2), arrowEdge: edge) { replace($0) })
        }
        ComposeRegistry.modifiers[200_312] = { v, a, _ in AnyView(v.modifier(TranslationTaskModifier(a: a))) }
    }
}

@available(iOS 18.0, macOS 15.0, *)
private struct TranslationTaskModifier: ViewModifier {
    let a: Args
    @State private var configuration: TranslationSession.Configuration?

    func body(content: Content) -> some View {
        let texts = a.list(0).compactMap { $0 as? String }
        let source = a.stringOpt(1).map { Locale.Language(identifier: $0) }
        let target = a.stringOpt(2).map { Locale.Language(identifier: $0) }
        let done = a.callback(4, box: { (x: NSArray) -> Any? in x })
        content
            .translationTask(configuration) { session in
                do {
                    let requests = texts.enumerated().map { TranslationSession.Request(sourceText: $0.element, clientIdentifier: "\($0.offset)") }
                    let responses = try await session.translations(from: requests)
                    let ordered = responses.sorted { Int($0.clientIdentifier ?? "0") ?? 0 < Int($1.clientIdentifier ?? "0") ?? 0 }
                    await MainActor.run { done([ordered.map(\.targetText) as NSArray, NSNull()] as NSArray) }
                } catch {
                    await MainActor.run { done([NSArray(), error.localizedDescription] as NSArray) }
                }
            }
            .onChange(of: texts, initial: true) { _, _ in
                if configuration == nil {
                    configuration = TranslationSession.Configuration(source: source, target: target)
                } else {
                    configuration?.invalidate()
                }
            }
    }
}
#endif
