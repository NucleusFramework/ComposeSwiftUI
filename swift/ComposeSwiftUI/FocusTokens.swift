import SwiftUI
#if canImport(AppKit)
import AppKit
#elseif canImport(UIKit)
import UIKit
#endif

// SwiftUI @FocusedValue and @FocusedSceneValue via token propagation.
// Kotlin side: FocusValues.kt. View kinds 100_068..100_069, modifier ids 100_119..100_120.

// MARK: - SwiftUI keys

private struct KotlinFocusTokensKey: FocusedValueKey {
    typealias Value = [String]
}

private extension FocusedValues {
    var kotlinFocusTokens: [String]? {
        get { self[KotlinFocusTokensKey.self] }
        set { self[KotlinFocusTokensKey.self] = newValue }
    }
}

private struct KotlinFocusChainKey: EnvironmentKey {
    static let defaultValue: [String] = []
}

private struct KotlinSceneDepthKey: EnvironmentKey {
    static let defaultValue: Int = 0
}

private extension EnvironmentValues {
    var kotlinFocusChain: [String] {
        get { self[KotlinFocusChainKey.self] }
        set { self[KotlinFocusChainKey.self] = newValue }
    }

    var kotlinSceneDepth: Int {
        get { self[KotlinSceneDepthKey.self] }
        set { self[KotlinSceneDepthKey.self] = newValue }
    }
}

// MARK: - Token stores

/// Maps lightweight string tokens to the live Kotlin `FocusValueBox` objects for `@FocusedValue`.
@MainActor final class KotlinFocusTokenStore {
    static let shared = KotlinFocusTokenStore()

    private struct Entry {
        let key: String
        let box: AnyObject
    }

    private var entries: [String: Entry] = [:]

    func set(token: String, key: String, box: AnyObject) {
        entries[token] = Entry(key: key, box: box)
    }

    func remove(token: String) {
        entries.removeValue(forKey: token)
    }

    /// Scans the token chain from innermost (last) to outermost (first) and returns the winning Kotlin box.
    func resolve(tokens: [String]?, key: String) -> AnyObject? {
        guard let tokens else { return nil }
        for tag in tokens.reversed() {
            let token = tag.split(separator: "#", maxSplits: 1).first.map(String.init) ?? tag
            if let entry = entries[token], entry.key == key {
                return entry.box
            }
        }
        return nil
    }
}

/// Tracks active scene-level focused values (`@FocusedSceneValue`) across windows, observable by readers.
@MainActor @Observable final class KotlinSceneFocusStore {
    static let shared = KotlinSceneFocusStore()

    private struct Entry {
        let key: String
        let depth: Int
        let order: Int
        let box: AnyObject
    }

    private var entries: [String: Entry] = [:]
    private var nextOrder = 0
    private(set) var revision = 0

    func set(token: String, key: String, depth: Int, box: AnyObject) {
        let order = entries[token]?.order ?? { nextOrder += 1; return nextOrder }()
        entries[token] = Entry(key: key, depth: depth, order: order, box: box)
        revision += 1
    }

    func remove(token: String) {
        if entries.removeValue(forKey: token) != nil {
            revision += 1
        }
    }

    /// Returns the winning box for [key]: deepest view in the hierarchy wins (latest mounted breaks ties).
    func resolve(key: String) -> AnyObject? {
        _ = revision
        var best: Entry?
        for entry in entries.values where entry.key == key {
            if let current = best {
                if entry.depth > current.depth || (entry.depth == current.depth && entry.order > current.order) {
                    best = entry
                }
            } else {
                best = entry
            }
        }
        return best?.box
    }
}

// MARK: - Key-window probe for focusedSceneValue

#if os(macOS)
private struct SceneWindowProbe: NSViewRepresentable {
    let onActiveChange: (Bool) -> Void

    final class ProbeView: NSView {
        var onActiveChange: ((Bool) -> Void)?
        private var observers: [NSObjectProtocol] = []

        override func viewDidMoveToWindow() {
            super.viewDidMoveToWindow()
            detach()
            guard let window else {
                onActiveChange?(false)
                return
            }
            let center = NotificationCenter.default
            let report = { [weak self, weak window] in
                guard let self, let window else { return }
                let active = window.isKeyWindow || (NSApp.keyWindow == nil && window.isVisible)
                self.onActiveChange?(active)
            }
            observers.append(center.addObserver(forName: NSWindow.didBecomeKeyNotification, object: window, queue: .main) { _ in report() })
            observers.append(center.addObserver(forName: NSWindow.didResignKeyNotification, object: window, queue: .main) { _ in report() })
            observers.append(center.addObserver(forName: NSWindow.willCloseNotification, object: window, queue: .main) { [weak self] _ in
                self?.onActiveChange?(false)
            })
            report()
        }

        private func detach() {
            for o in observers { NotificationCenter.default.removeObserver(o) }
            observers.removeAll()
        }

        deinit { detach() }
    }

    func makeNSView(context: Context) -> ProbeView {
        let v = ProbeView(frame: .zero)
        v.onActiveChange = onActiveChange
        return v
    }

    func updateNSView(_ nsView: ProbeView, context: Context) {
        nsView.onActiveChange = onActiveChange
    }
}
#elseif os(iOS) || os(tvOS)
private struct SceneWindowProbe: UIViewRepresentable {
    let onActiveChange: (Bool) -> Void

    final class ProbeView: UIView {
        var onActiveChange: ((Bool) -> Void)?
        private var observers: [NSObjectProtocol] = []

        override func didMoveToWindow() {
            super.didMoveToWindow()
            detach()
            guard let window else {
                onActiveChange?(false)
                return
            }
            let center = NotificationCenter.default
            let report = { [weak self, weak window] in
                guard let self, let window else { return }
                self.onActiveChange?(window.isKeyWindow || window.windowScene?.activationState == .foregroundActive)
            }
            observers.append(center.addObserver(forName: UIWindow.didBecomeKeyNotification, object: window, queue: .main) { _ in report() })
            observers.append(center.addObserver(forName: UIWindow.didResignKeyNotification, object: window, queue: .main) { _ in report() })
            report()
        }

        private func detach() {
            for o in observers { NotificationCenter.default.removeObserver(o) }
            observers.removeAll()
        }

        deinit { detach() }
    }

    func makeUIView(context: Context) -> ProbeView {
        let v = ProbeView(frame: .zero)
        v.isUserInteractionEnabled = false
        v.isHidden = true
        v.onActiveChange = onActiveChange
        return v
    }

    func updateUIView(_ uiView: ProbeView, context: Context) {
        uiView.onActiveChange = onActiveChange
    }
}
#else
private struct SceneWindowProbe: View {
    let onActiveChange: (Bool) -> Void

    var body: some View {
        Color.clear
            .frame(width: 0, height: 0)
            .onAppear { onActiveChange(true) }
            .onDisappear { onActiveChange(false) }
    }
}
#endif

// MARK: - Modifiers and reader views

private struct KotlinFocusTokenModifier: ViewModifier {
    let key: String
    let box: AnyObject

    @Environment(\.kotlinFocusChain) private var chain
    @State private var token = UUID().uuidString

    func body(content: Content) -> some View {
        let tag = "\(token)#\(ObjectIdentifier(box).hashValue)"
        let next = chain + [tag]
        KotlinFocusTokenStore.shared.set(token: token, key: key, box: box)
        return content
            .environment(\.kotlinFocusChain, next)
            .focusedValue(\.kotlinFocusTokens, next)
            .onDisappear {
                KotlinFocusTokenStore.shared.remove(token: token)
            }
    }
}

private struct KotlinSceneFocusTokenModifier: ViewModifier {
    let key: String
    let box: AnyObject

    @Environment(\.kotlinSceneDepth) private var depth
    @State private var token = UUID().uuidString
    @State private var isWindowActive = false

    private var tag: String { "\(token)#\(key)#\(ObjectIdentifier(box).hashValue)" }

    func body(content: Content) -> some View {
        content
            .environment(\.kotlinSceneDepth, depth + 1)
            .background(
                SceneWindowProbe { active in
                    if isWindowActive != active { isWindowActive = active }
                }
                .frame(width: 0, height: 0)
                .allowsHitTesting(false)
            )
            .onChange(of: tag, initial: true) { _, _ in sync() }
            .onChange(of: isWindowActive, initial: true) { _, _ in sync() }
            .onDisappear {
                KotlinSceneFocusStore.shared.remove(token: token)
            }
    }

    private func sync() {
        if isWindowActive {
            KotlinSceneFocusStore.shared.set(token: token, key: key, depth: depth, box: box)
        } else {
            KotlinSceneFocusStore.shared.remove(token: token)
        }
    }
}

private struct FocusedValueReaderView: View {
    let m: NodeModel
    @FocusedValue(\.kotlinFocusTokens) private var tokens

    var body: some View {
        let _ = m.version
        let key = m.args.string(0)
        let callback = m.args.raw(1) as? SValueCallback
        let signature = tokens?.joined(separator: "|") ?? ""
        ChildrenView(m)
            .onChange(of: signature, initial: true) { _, _ in
                let box = KotlinFocusTokenStore.shared.resolve(tokens: tokens, key: key)
                callback?.invoke(value: box)
                ComposeSwiftUIHostKt.flushCompose()
            }
    }
}

private struct FocusedSceneValueReaderView: View {
    let m: NodeModel

    var body: some View {
        let _ = m.version
        let key = m.args.string(0)
        let callback = m.args.raw(1) as? SValueCallback
        let revision = KotlinSceneFocusStore.shared.revision
        ChildrenView(m)
            .onChange(of: revision, initial: true) { _, _ in
                let box = KotlinSceneFocusStore.shared.resolve(key: key)
                callback?.invoke(value: box)
                ComposeSwiftUIHostKt.flushCompose()
            }
    }
}

@MainActor enum FocusTokenRenderers {
    static func register() {
        ComposeRegistry.views[100_068] = { m in ModChain.apply(FocusedValueReaderView(m: m), m, 0) }
        ComposeRegistry.views[100_069] = { m in ModChain.apply(FocusedSceneValueReaderView(m: m), m, 0) }

        ComposeRegistry.modifiers[100_119] = { v, a, _ in
            guard let box = a.raw(1) as AnyObject? else { return v }
            return AnyView(v.modifier(KotlinFocusTokenModifier(key: a.string(0), box: box)))
        }
        ComposeRegistry.modifiers[100_120] = { v, a, _ in
            guard let box = a.raw(1) as AnyObject? else { return v }
            return AnyView(v.modifier(KotlinSceneFocusTokenModifier(key: a.string(0), box: box)))
        }
    }
}
