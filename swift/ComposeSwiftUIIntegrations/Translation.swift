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
                if configuration == nil { configuration = Self.configuration(source, target, a.intOpt(3)) } else { configuration?.invalidate() }
            }
            // A new language pair or strategy needs a new configuration: invalidate() only reruns the same one.
            .onChange(of: "\(a.stringOpt(1) ?? "")|\(a.stringOpt(2) ?? "")|\(a.intOpt(3) ?? -1)") { _, _ in
                configuration = Self.configuration(source, target, a.intOpt(3))
            }
    }

    private static func configuration(_ source: Locale.Language?, _ target: Locale.Language?, _ strategy: Int?) -> TranslationSession.Configuration {
        if let strategy, #available(iOS 26.4, macOS 26.4, *) {
            return TranslationSession.Configuration(source: source, target: target, preferredStrategy: strategy == 1 ? .lowLatency : .highFidelity)
        }
        return TranslationSession.Configuration(source: source, target: target)
    }
}
#endif
