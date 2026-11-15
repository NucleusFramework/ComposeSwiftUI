import SwiftUI
#if os(iOS)
import UIKit
#else
import AppKit
#endif

// Scenario benchmarks (see bench/shared/.../scenarios/Scenarios.kt). The SwiftUI side uses idiomatic native models:
// one @Observable object per row, so a change re-evaluates only the views that read it. This is the fair baseline,
// unlike the 50-screen benchmark where SwiftUI observes the whole Kotlin state through one ObservableObject.

// MARK: - Native SwiftUI models and views

@Observable final class NativeMailRow: Identifiable {
    let id: Int
    var title: String
    let subtitle: String
    let time: String
    var unread: Bool
    let icon: String

    init(_ i: Int) {
        let icons = ["envelope.fill", "star.fill", "paperclip", "flag.fill", "bell.fill"]
        id = i
        title = "Sender \(i)"
        subtitle = "Subject line number \(i), with a few more words to fill the row"
        time = "\(i % 12 + 1):\(String(format: "%02d", i % 60))"
        unread = i % 3 == 0
        icon = icons[i % icons.count]
    }
}

@Observable final class NativeInbox {
    var rows: [NativeMailRow]
    private var revision = 0

    init(count: Int) { rows = (0..<count).map(NativeMailRow.init) }

    func toggleOne(_ index: Int) { rows[index].unread.toggle() }

    func renameAll() {
        revision += 1
        for (i, row) in rows.enumerated() { row.title = "Sender \(i) · r\(revision)" }
    }
}

struct NativeInboxView: View {
    let inbox: NativeInbox

    var body: some View {
        NavigationStack {
            List {
                ForEach(inbox.rows) { NativeMailRowView(row: $0) }
            }
            .listStyle(.plain)
            .navigationTitle("Inbox")
        }
    }
}

struct NativeMailRowView: View {
    let row: NativeMailRow

    var body: some View {
        HStack(spacing: 12) {
            Circle().fill(row.unread ? Color.blue : Color.clear).frame(width: 10, height: 10)
            Image(systemName: row.icon).foregroundStyle(Color.orange).frame(width: 24)
            VStack(alignment: .leading, spacing: 2) {
                Text(row.title).font(.headline)
                Text(row.subtitle).font(.subheadline).foregroundStyle(.secondary).lineLimit(1)
            }
            Spacer()
            Text(row.time).font(.caption).foregroundStyle(.secondary)
        }
    }
}

@Observable final class NativeBars {
    var phase = false
    let count: Int
    init(count: Int) { self.count = count }
    func toggle() { withAnimation(.easeInOut(duration: 0.35)) { phase.toggle() } }
}

struct NativeBarsView: View {
    let bars: NativeBars

    var body: some View {
        NavigationStack {
            HStack(alignment: .bottom, spacing: 2) {
                ForEach(0..<bars.count, id: \.self) { i in
                    RoundedRectangle(cornerRadius: 2)
                        .fill(bars.phase ? Color.blue : Color.orange)
                        .frame(height: ScenariosKt.barHeight(i: Int32(i), phase: bars.phase))
                }
            }
            .padding()
            .navigationTitle("Bars")
        }
    }
}

// MARK: - Runner

struct ScenarioResult: Codable {
    var name: String
    var impl: String
    var mountMs: Double = 0
    var scrollMsPerPage: Double = 0
    var toggleOneMs: Double = 0
    var renameAllMs: Double = 0
    var animationApplyMs: Double = 0
    var animationFrames: Int = 0
    var animationHitches: Int = 0
    var diff: Double = 1
}

/// Measures the scenarios for both implementations and writes `scenarios.json`.
@MainActor
final class ScenarioRunner {
    let outDir: URL
    let size: CGSize
    var log: (String) -> Void = { print($0) }

    init(outDir: URL, size: CGSize) {
        self.outDir = outDir
        self.size = size
    }

    private func settle(_ seconds: Double) async {
        try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
    }

    private func median(_ values: [Double]) -> Double { values.sorted()[values.count / 2] }

    func run(listSizes: [Int], lazy: Bool) async -> [ScenarioResult] {
        try? FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)
        let renderer = OffscreenRenderer(size: size)
        var results: [ScenarioResult] = []
        for n in listSizes {
            var shots: [Impl: CGImage] = [:]
            var pair: [ScenarioResult] = []
            for impl in Impl.allCases {
                var r = ScenarioResult(name: "list-\(n)", impl: impl.rawValue)
                // mount: median of 3 (after one warm-up)
                var mounts: [Double] = []
                for run in 0..<4 {
                    let (view, _) = makeList(n, impl, lazy)
                    let t = renderer.mount { view }
                    if run > 0 { mounts.append(t * 1000) }
                    renderer.unmount()
                    await settle(0.05)
                }
                r.mountMs = median(mounts)

                let (view, model) = makeList(n, impl, lazy)
                _ = renderer.mount { view }
                await settle(0.5)
                renderer.relayout()
                shots[impl] = renderer.snapshot()

                // one visible row changes
                let toggles = 30
                var t0 = now()
                for k in 0..<toggles {
                    model.toggleOne(3 + k % 5)
                    ComposeSwiftUIHostKt.flushCompose()
                    renderer.relayout()
                }
                r.toggleOneMs = (now() - t0) * 1000 / Double(toggles)

                // every row changes
                let renames = 5
                t0 = now()
                for _ in 0..<renames {
                    model.renameAll()
                    ComposeSwiftUIHostKt.flushCompose()
                    renderer.relayout()
                }
                r.renameAllMs = (now() - t0) * 1000 / Double(renames)

                // scrolling: 60 pages, each laid out
                let pages = 60
                t0 = now()
                let scrolled = scroll(renderer, pages: pages)
                r.scrollMsPerPage = scrolled ? (now() - t0) * 1000 / Double(pages) : -1
                renderer.unmount()
                await settle(0.1)
                pair.append(r)
            }
            if let a = shots[.swiftUI], let b = shots[.compose] {
                let d = PixelDiff.compare(a, b)
                for i in pair.indices { pair[i].diff = d.ratio }
                writePNG(a, to: outDir.appendingPathComponent("list-\(n)-swiftui.png"))
                writePNG(b, to: outDir.appendingPathComponent("list-\(n)-compose.png"))
            }
            for r in pair { report(r) }
            results += pair
        }
        results += await runBars(renderer, count: 60)
        let enc = JSONEncoder()
        enc.outputFormatting = [.prettyPrinted, .sortedKeys]
        if let data = try? enc.encode(results) { try? data.write(to: outDir.appendingPathComponent("scenarios.json")) }
        log("[scenario] DONE \(outDir.path)")
        return results
    }

    private func report(_ r: ScenarioResult) {
        log(String(format: "[scenario] %@ %@  mount=%.2fms scroll=%.2fms/page toggleOne=%.3fms renameAll=%.2fms anim apply=%.3fms frames=%d hitches=%d diff=%.4f%%",
                   r.name, r.impl, r.mountMs, r.scrollMsPerPage, r.toggleOneMs, r.renameAllMs, r.animationApplyMs,
                   r.animationFrames, r.animationHitches, r.diff * 100))
    }

    /// The scenario's view for one implementation, and an adapter to mutate its model.
    private func makeList(_ n: Int, _ impl: Impl, _ lazy: Bool) -> (AnyView, ListModel) {
        switch impl {
        case .swiftUI:
            let inbox = NativeInbox(count: n)
            return (AnyView(NativeInboxView(inbox: inbox)), ListModel(toggleOne: inbox.toggleOne, renameAll: inbox.renameAll))
        case .compose:
            let scenario = LongListScenario(count: Int32(n), lazy: lazy)
            return (AnyView(ComposeView { scenario.host() }),
                    ListModel(toggleOne: { scenario.toggleOne(index: Int32($0)) }, renameAll: scenario.renameAll))
        }
    }

    /// Scrolls the first scroll view in the window by one page per step, laying out after each step.
    private func scroll(_ renderer: OffscreenRenderer, pages: Int) -> Bool {
        #if os(iOS)
        guard let root = renderer.rootView, let scrollView = Self.firstScrollView(in: root) else { return false }
        let page = scrollView.bounds.height
        for p in 1...pages {
            scrollView.contentOffset.y = min(CGFloat(p) * page, max(0, scrollView.contentSize.height - page))
            scrollView.layoutIfNeeded()
        }
        #else
        guard let root = renderer.rootView, let scrollView = Self.firstScrollView(in: root) else { return false }
        let page = scrollView.contentView.bounds.height
        for p in 1...pages {
            scrollView.contentView.scroll(to: CGPoint(x: 0, y: CGFloat(p) * page))
            scrollView.reflectScrolledClipView(scrollView.contentView)
            scrollView.layoutSubtreeIfNeeded()
            scrollView.displayIfNeeded()
        }
        #endif
        return true
    }

    #if os(iOS)
    private static func firstScrollView(in view: UIView) -> UIScrollView? {
        if let s = view as? UIScrollView, s.contentSize.height > s.bounds.height { return s }
        for sub in view.subviews { if let s = firstScrollView(in: sub) { return s } }
        return nil
    }
    #else
    private static func firstScrollView(in view: NSView) -> NSScrollView? {
        if let s = view as? NSScrollView { return s }
        for sub in view.subviews { if let s = firstScrollView(in: sub) { return s } }
        return nil
    }
    #endif

    private func runBars(_ renderer: OffscreenRenderer, count: Int) async -> [ScenarioResult] {
        var pair: [ScenarioResult] = []
        var shots: [Impl: CGImage] = [:]
        for impl in Impl.allCases {
            var r = ScenarioResult(name: "bars-\(count)", impl: impl.rawValue)
            let toggle: () -> Void
            let view: AnyView
            switch impl {
            case .swiftUI:
                let bars = NativeBars(count: count)
                view = AnyView(NativeBarsView(bars: bars)); toggle = bars.toggle
            case .compose:
                let bars = BarsScenario(count: Int32(count))
                view = AnyView(ComposeView { bars.host() }); toggle = bars.toggle
            }
            r.mountMs = renderer.mount { view } * 1000
            await settle(0.5)
            // apply cost of an animated change, then frame pacing while 10 animations run
            var applies: [Double] = []
            let sampler = FrameSampler()
            sampler.start()
            for _ in 0..<10 {
                let t0 = now()
                toggle()
                ComposeSwiftUIHostKt.flushCompose()
                renderer.relayout()
                applies.append((now() - t0) * 1000)
                await settle(0.4)
            }
            sampler.stop()
            r.animationApplyMs = median(applies)
            r.animationFrames = sampler.frames
            r.animationHitches = sampler.hitches
            await settle(0.3)
            renderer.relayout()
            shots[impl] = renderer.snapshot()
            renderer.unmount()
            await settle(0.1)
            pair.append(r)
        }
        if let a = shots[.swiftUI], let b = shots[.compose] {
            let d = PixelDiff.compare(a, b)
            for i in pair.indices { pair[i].diff = d.ratio }
        }
        for r in pair { report(r) }
        return pair
    }
}

struct ListModel {
    let toggleOne: (Int) -> Void
    let renameAll: () -> Void
}

/// Counts display frames and hitches (frames that took more than 1.5 refresh intervals).
@MainActor
final class FrameSampler: NSObject {
    private(set) var frames = 0
    private(set) var hitches = 0
    private var last: CFTimeInterval = 0
    #if os(iOS)
    private var link: CADisplayLink?
    #else
    private var timer: Timer?
    #endif

    func start() {
        #if os(iOS)
        link = CADisplayLink(target: self, selector: #selector(tick(_:)))
        link?.add(to: .main, forMode: .common)
        #else
        // macOS: sample the main run loop at 120 Hz; a late tick means the main thread was busy
        timer = Timer.scheduledTimer(withTimeInterval: 1.0 / 120, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.record(CACurrentMediaTime(), interval: 1.0 / 120) }
        }
        #endif
    }

    func stop() {
        #if os(iOS)
        link?.invalidate()
        #else
        timer?.invalidate()
        #endif
    }

    #if os(iOS)
    @objc private func tick(_ link: CADisplayLink) {
        record(link.timestamp, interval: link.duration)
    }
    #endif

    private func record(_ t: CFTimeInterval, interval: CFTimeInterval) {
        if last > 0, t - last > interval * 1.5 { hitches += 1 }
        last = t
        frames += 1
    }
}
