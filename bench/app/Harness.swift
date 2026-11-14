import SwiftUI
#if os(iOS)
import UIKit
typealias PlatformImage = UIImage
#else
import AppKit
typealias PlatformImage = NSImage
#endif

enum Impl: String, CaseIterable, Codable {
    case swiftUI = "SwiftUI"
    case compose = "Compose SwiftUI"
}

@MainActor
enum ScreenFactory {
    static var count: Int { Int(BenchRegistry.shared.count()) }
    static var lastCompositionMs: Double = 0
    static func title(_ i: Int) -> String { BenchRegistry.shared.title(index: Int32(i)) }

    static func view(_ i: Int, _ impl: Impl, _ vm: BenchViewModel) -> AnyView {
        switch impl {
        case .swiftUI: return SwiftScreens.make(i, vm)
        case .compose:
            return AnyView(ComposeView {
                // composition is measured separately from the SwiftUI work
                let t0 = now()
                let host = BenchRegistry.shared.composeHost(index: Int32(i), vm: vm)
                host.start()
                ScreenFactory.lastCompositionMs = (now() - t0) * 1000
                return host
            })
        }
    }
}

func now() -> Double { CFAbsoluteTimeGetCurrent() }

/// Hosts a SwiftUI view off the main UI at a fixed size, forces layout and captures pixels.
@MainActor
final class OffscreenRenderer {
    let size: CGSize
    #if os(iOS)
    private let window: UIWindow
    #else
    private let window: NSWindow
    #endif

    init(size: CGSize) {
        self.size = size
        #if os(iOS)
        let scene = UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first!
        window = UIWindow(windowScene: scene)
        window.frame = CGRect(origin: .zero, size: size)
        window.windowLevel = .normal + 1
        window.isHidden = false
        #else
        window = NSWindow(contentRect: CGRect(origin: CGPoint(x: 40, y: 40), size: size), styleMask: [.borderless],
                          backing: .buffered, defer: false)
        window.isReleasedWhenClosed = false
        window.orderFrontRegardless()
        #endif
    }

    /// Mounts `view` and forces a synchronous layout pass. Returns elapsed seconds.
    func mount(_ makeView: () -> AnyView) -> Double {
        let t0 = now()
        let view = makeView()
        #if os(iOS)
        let host = UIHostingController(rootView: view)
        window.rootViewController = host
        host.view.frame = CGRect(origin: .zero, size: size)
        host.view.layoutIfNeeded()
        #else
        let host = NSHostingView(rootView: view)
        host.frame = CGRect(origin: .zero, size: size)
        window.contentView = host
        host.layoutSubtreeIfNeeded()
        host.displayIfNeeded()
        #endif
        return now() - t0
    }

    /// Forces SwiftUI to apply pending updates and lay out again.
    func relayout() {
        #if os(iOS)
        window.rootViewController?.view.setNeedsLayout()
        window.rootViewController?.view.layoutIfNeeded()
        #else
        window.contentView?.layoutSubtreeIfNeeded()
        window.contentView?.displayIfNeeded()
        #endif
    }

    func unmount() {
        #if os(iOS)
        window.rootViewController = nil
        #else
        window.contentView = nil
        #endif
    }

    func snapshot() -> CGImage? {
        #if os(iOS)
        let format = UIGraphicsImageRendererFormat()
        format.scale = 2
        let image = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            window.drawHierarchy(in: CGRect(origin: .zero, size: size), afterScreenUpdates: true)
        }
        return image.cgImage
        #else
        guard let view = window.contentView, let rep = view.bitmapImageRepForCachingDisplay(in: view.bounds) else { return nil }
        view.cacheDisplay(in: view.bounds, to: rep)
        return rep.cgImage
        #endif
    }
}

/// Pixel comparison of two images; returns the fraction of differing pixels and a diff mask.
enum PixelDiff {
    static func rgba(_ image: CGImage, width: Int, height: Int) -> [UInt8] {
        var data = [UInt8](repeating: 0, count: width * height * 4)
        let ctx = CGContext(data: &data, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
                            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
        ctx.draw(image, in: CGRect(x: 0, y: 0, width: width, height: height))
        return data
    }

    static func compare(_ a: CGImage, _ b: CGImage, tolerance: Int = 10) -> (ratio: Double, diff: CGImage?) {
        let width = min(a.width, b.width), height = min(a.height, b.height)
        let pa = rgba(a, width: width, height: height), pb = rgba(b, width: width, height: height)
        var mask = [UInt8](repeating: 0, count: width * height * 4)
        var differing = 0
        for p in 0..<(width * height) {
            let o = p * 4
            var delta = 0
            for c in 0..<3 { delta = max(delta, abs(Int(pa[o + c]) - Int(pb[o + c]))) }
            if delta > tolerance {
                differing += 1
                mask[o] = 255; mask[o + 3] = 255
            } else {
                let grey = UInt8((Int(pa[o]) + Int(pa[o + 1]) + Int(pa[o + 2])) / 12 + 180)
                mask[o] = grey; mask[o + 1] = grey; mask[o + 2] = grey; mask[o + 3] = 255
            }
        }
        let ctx = CGContext(data: &mask, width: width, height: height, bitsPerComponent: 8, bytesPerRow: width * 4,
                            space: CGColorSpaceCreateDeviceRGB(), bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)
        return (Double(differing) / Double(width * height), ctx?.makeImage())
    }
}

func writePNG(_ image: CGImage, to url: URL) {
    guard let dest = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil) else { return }
    CGImageDestinationAddImage(dest, image, nil)
    CGImageDestinationFinalize(dest)
}

struct ImplResult: Codable {
    var mountMs: [Double] = []
    var compositionMs: [Double] = []
    var updateMs: Double = 0
    var medianMountMs: Double { mountMs.sorted()[mountMs.count / 2] }
}

struct ScreenResult: Codable {
    let index: Int
    let title: String
    var swiftUI = ImplResult()
    var compose = ImplResult()
    var initialDiff: Double = 1
    var afterUpdateDiff: Double = 1
    var medianMountSwiftUI: Double = 0
    var medianMountCompose: Double = 0
}

/// Runs the identity check + benchmarks over every screen and writes JSON / PNGs to `outDir`.
@MainActor
final class BenchRunner {
    let outDir: URL
    let size: CGSize
    let mountRuns = 5
    let updateRuns = 30
    var log: (String) -> Void = { print($0) }

    init(outDir: URL, size: CGSize) {
        self.outDir = outDir
        self.size = size
    }

    private func settle(_ seconds: Double) async {
        try? await Task.sleep(nanoseconds: UInt64(seconds * 1_000_000_000))
    }

    func run(only: [Int]? = nil) async -> [ScreenResult] {
        try? FileManager.default.createDirectory(at: outDir, withIntermediateDirectories: true)
        let renderer = OffscreenRenderer(size: size)
        var results: [ScreenResult] = []
        let indices = only ?? Array(0..<ScreenFactory.count)
        for i in indices {
            var r = ScreenResult(index: SwiftScreens.numbers[i], title: ScreenFactory.title(i))
            var shots: [Impl: CGImage] = [:]
            var after: [Impl: CGImage] = [:]
            for impl in Impl.allCases {
                // warm-up mount (type metadata, caches), not measured
                let warmVM = BenchRegistry.shared.createViewModel(index: Int32(i))
                _ = renderer.mount { ScreenFactory.view(i, impl, warmVM) }
                await settle(0.05)
                renderer.unmount()
                await settle(0.05)
            }
            // interleaved measured mounts
            for _ in 0..<mountRuns {
                for impl in Impl.allCases {
                    let vm = BenchRegistry.shared.createViewModel(index: Int32(i))
                    let t = renderer.mount { ScreenFactory.view(i, impl, vm) }
                    if impl == .swiftUI { r.swiftUI.mountMs.append(t * 1000) } else {
                        r.compose.mountMs.append(t * 1000)
                        r.compose.compositionMs.append(ScreenFactory.lastCompositionMs)
                    }
                    renderer.unmount()
                    await settle(0.02)
                }
            }
            for impl in Impl.allCases {
                let vm = BenchRegistry.shared.createViewModel(index: Int32(i))
                _ = renderer.mount { ScreenFactory.view(i, impl, vm) }
                await settle(0.6)
                renderer.relayout()
                shots[impl] = renderer.snapshot()
                // update benchmark: N deterministic state mutations, each fully applied + laid out
                let t0 = now()
                for _ in 0..<updateRuns {
                    vm.mutate()
                    ComposeSwiftUIHostKt.flushCompose()
                    renderer.relayout()
                }
                let elapsed = (now() - t0) * 1000 / Double(updateRuns)
                if impl == .swiftUI { r.swiftUI.updateMs = elapsed } else { r.compose.updateMs = elapsed }
                await settle(0.6)
                renderer.relayout()
                after[impl] = renderer.snapshot()
                renderer.unmount()
                await settle(0.05)
            }
            r.medianMountSwiftUI = r.swiftUI.medianMountMs
            r.medianMountCompose = r.compose.medianMountMs
            let name = String(format: "screen%02d", SwiftScreens.numbers[i])
            if let a = shots[.swiftUI], let b = shots[.compose] {
                let d = PixelDiff.compare(a, b)
                r.initialDiff = d.ratio
                writePNG(a, to: outDir.appendingPathComponent("\(name)-swiftui.png"))
                writePNG(b, to: outDir.appendingPathComponent("\(name)-compose.png"))
                if let diff = d.diff { writePNG(diff, to: outDir.appendingPathComponent("\(name)-diff.png")) }
            }
            if let a = after[.swiftUI], let b = after[.compose] {
                let d = PixelDiff.compare(a, b)
                r.afterUpdateDiff = d.ratio
                if d.ratio > 0.0005 {
                    writePNG(a, to: outDir.appendingPathComponent("\(name)-after-swiftui.png"))
                    writePNG(b, to: outDir.appendingPathComponent("\(name)-after-compose.png"))
                }
            }
            let comp = r.compose.compositionMs.sorted()
            log(String(format: "[bench] %@  diff=%.4f%% after=%.4f%%  mount swift=%.2fms compose=%.2fms (kotlin %.2fms)  update swift=%.3fms compose=%.3fms",
                       r.title, r.initialDiff * 100, r.afterUpdateDiff * 100, r.medianMountSwiftUI, r.medianMountCompose,
                       comp.isEmpty ? 0 : comp[comp.count / 2], r.swiftUI.updateMs, r.compose.updateMs))
            results.append(r)
        }
        let enc = JSONEncoder()
        enc.outputFormatting = [.prettyPrinted, .sortedKeys]
        if let data = try? enc.encode(results) {
            try? data.write(to: outDir.appendingPathComponent("results.json"))
        }
        log("[bench] DONE \(outDir.path)")
        return results
    }
}
