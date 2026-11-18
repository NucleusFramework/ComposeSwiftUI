// The preview host on the Mac: an accessory app (no Dock icon, never activated) with one borderless window per
// preview, placed off screen, and captured as the WindowServer composites it. `Run on Mac` shows the preview in a regular window instead.

#if os(macOS)
import AppKit
import SwiftUI

@main
enum ComposeSwiftUIPreviewHost {
    static func main() {
        MainActor.assumeIsolated {
            ComposeInspection.enabled = true
            let delegate = AppDelegate()
            let app = NSApplication.shared
            app.delegate = delegate
            withExtendedLifetime(delegate) { app.run() }
        }
    }
}

@MainActor
private final class AppDelegate: NSObject, NSApplicationDelegate {
    private var runSurface: AppKitSurface?
    private var activity: NSObjectProtocol?

    func applicationDidFinishLaunching(_ notification: Notification) {
        if let unit = Host.runUnit {
            NSApp.setActivationPolicy(.regular)
            runSurface = AppKitSurface(unit: unit, host: unit.makeHost(), mode: .window)
            NSApp.activate()
        } else {
            NSApp.setActivationPolicy(.accessory)
            // App Nap would throttle a host that is never in the foreground.
            activity = ProcessInfo.processInfo.beginActivity(options: [.userInitiated, .latencyCritical], reason: "Rendering previews")
            Task { await Host.serve() }
        }
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool { runSurface != nil }
}

@MainActor
enum PreviewHostPlatform {
    static func makeSurface(unit: PreviewUnit, host: ComposeSwiftUIHost, interactive: Bool) -> PreviewSurface {
        AppKitSurface(unit: unit, host: host, mode: .offscreen)
    }

    /// The Mac host renders in the background: nothing to report.
    static func observeActivity(_ update: @escaping @MainActor (Bool) -> Void) {}

    /// The Mac has no system-wide slow animations: the live preview keeps its speed.
    static func setAnimationSpeed(_ factor: Double) {}
}

/// A borderless window can't become key by default: the live preview needs it for clicks and typing.
private final class PreviewWindow: NSWindow {
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { true }
}

/// Paints the window background under the preview, which `cacheDisplay` doesn't draw.
private final class BackgroundView: NSView {
    override func draw(_ dirtyRect: NSRect) {
        NSColor.windowBackgroundColor.setFill()
        dirtyRect.fill()
    }
}

@MainActor
private final class AppKitSurface: PreviewSurface {
    enum Mode { case offscreen, window }

    /// A Mac preview without a size: a typical window, as there is no device screen.
    static let defaultSize = CGSize(width: 800, height: 600)

    private let job: PreviewJob
    private let window: NSWindow
    private let controller: NSHostingController<PreviewContent>
    private let container = BackgroundView()

    init(unit: PreviewUnit, host: ComposeSwiftUIHost, mode: Mode) {
        job = unit.job
        controller = NSHostingController(rootView: PreviewContent(unit: unit, host: host))
        // The window gets the preview's size, not the content's.
        controller.sizingOptions = []
        window = PreviewWindow(
            contentRect: CGRect(origin: .zero, size: Self.defaultSize),
            styleMask: mode == .window ? [.titled, .closable, .miniaturizable, .resizable] : [.borderless],
            backing: .buffered,
            defer: false
        )
        window.isReleasedWhenClosed = false
        switch job.colorScheme {
        case "dark": window.appearance = NSAppearance(named: .darkAqua)
        case "light": window.appearance = NSAppearance(named: .aqua)
        default: break
        }
        controller.view.autoresizingMask = [.width, .height]
        container.addSubview(controller.view)
        window.contentView = container
        controller.view.frame = container.bounds
        window.setContentSize(size())
        switch mode {
        case .window:
            window.title = unit.title
            window.center()
            window.makeKeyAndOrderFront(nil)
        case .offscreen:
            // On screen for AppKit (it lays out, animates and draws), off screen for the user.
            window.setFrameOrigin(CGPoint(x: -30_000, y: -30_000))
            window.orderFrontRegardless()
        }
    }

    var bounds: CGRect { container.bounds }
    var scale: CGFloat { NSScreen.main?.backingScaleFactor ?? 2 }
    var pointerAvailable: Bool { true }

    /// Size of the window: the `@Preview` size, else a typical window; the content's height with `sizeThatFits`.
    private func size() -> CGSize {
        let width = job.width > 0 ? CGFloat(job.width) : Self.defaultSize.width
        var height = job.height > 0 ? CGFloat(job.height) : Self.defaultSize.height
        if job.sizeThatFits {
            let fitting = controller.sizeThatFits(in: CGSize(width: width, height: job.height > 0 ? height : .greatestFiniteMagnitude))
            if fitting.height.isFinite && fitting.height > 0 { height = min(fitting.height, job.height > 0 ? height : 4 * Self.defaultSize.height) }
            let fittingWidth = job.width > 0 ? width : min(max(fitting.width, 1), width)
            return CGSize(width: ceil(fittingWidth), height: ceil(height))
        }
        return CGSize(width: width, height: height)
    }

    func fitContent() {
        guard job.sizeThatFits else { return }
        let size = size()
        if size != container.bounds.size {
            // Keeps the top left corner in place (window frames grow upwards).
            let top = window.frame.maxY
            window.setContentSize(size)
            window.setFrameTopLeftPoint(CGPoint(x: window.frame.minX, y: top))
        }
    }

    func snapshot(scale: CGFloat, afterScreenUpdates: Bool) -> CGImage? {
        if afterScreenUpdates {
            container.layoutSubtreeIfNeeded()
            window.displayIfNeeded()
        }
        // As composited by the WindowServer (vibrancy, materials: `cacheDisplay` draws vibrant text black), else drawn by AppKit.
        if let image = WindowCapture.image(of: window, scale: scale) { return image }
        let bounds = container.bounds
        guard let rep = NSBitmapImageRep(
            bitmapDataPlanes: nil,
            pixelsWide: max(Int((bounds.width * scale).rounded(.up)), 1),
            pixelsHigh: max(Int((bounds.height * scale).rounded(.up)), 1),
            bitsPerSample: 8, samplesPerPixel: 4, hasAlpha: true, isPlanar: false,
            colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0
        ) else { return nil }
        rep.size = bounds.size
        container.cacheDisplay(in: bounds, to: rep)
        return rep.cgImage
    }

    func close() {
        window.orderOut(nil)
        window.contentView = nil
    }

    /// [point] (from the top left of the preview) in the window's coordinates.
    private func windowPoint(_ point: CGPoint) -> CGPoint {
        container.convert(CGPoint(x: point.x, y: container.bounds.height - point.y), to: nil)
    }

    /// Mouse events go through the app's queue, like real ones: controls track the mouse by reading the queue.
    func pointer(_ phase: PointerPhase, at point: CGPoint) {
        if !window.isKeyWindow { window.makeKey() }
        let type: NSEvent.EventType = switch phase {
        case .began: .leftMouseDown
        case .moved: .leftMouseDragged
        case .ended: .leftMouseUp
        }
        guard let event = NSEvent.mouseEvent(
            with: type, location: windowPoint(point), modifierFlags: [], timestamp: ProcessInfo.processInfo.systemUptime,
            windowNumber: window.windowNumber, context: nil, eventNumber: 0, clickCount: 1,
            pressure: phase == .ended ? 0 : 1
        ) else { return }
        NSApp.postEvent(event, atStart: false)
    }

    func scroll(at point: CGPoint, by delta: CGPoint) {
        let local = windowPoint(point)
        guard let superview = container.superview,
              let scrollView = container.hitTest(superview.convert(local, from: nil))?.enclosingScrollView,
              let document = scrollView.documentView else { return }
        let clip = scrollView.contentView
        var origin = clip.bounds.origin
        origin.x += delta.x
        origin.y += clip.isFlipped ? delta.y : -delta.y
        origin.x = min(max(origin.x, document.frame.minX), max(document.frame.minX, document.frame.maxX - clip.bounds.width))
        origin.y = min(max(origin.y, document.frame.minY), max(document.frame.minY, document.frame.maxY - clip.bounds.height))
        clip.scroll(to: origin)
        scrollView.reflectScrolledClipView(clip)
    }

    func type(_ text: String) {
        window.firstResponder?.insertText(text)
    }

    func deleteBackward() {
        window.firstResponder?.deleteBackward(nil)
    }
}
/// The window as the WindowServer composites it (vibrancy, materials). An app may capture its own windows without the
/// screen recording permission, even off screen. Looked up at run time: the SDK marks the call obsolete.
@MainActor
enum WindowCapture {
    private typealias CreateImage = @convention(c) (CGRect, UInt32, UInt32, UInt32) -> Unmanaged<CGImage>?
    private static let create: CreateImage? = dlsym(dlopen(nil, RTLD_NOW), "CGWindowListCreateImage").map { unsafeBitCast($0, to: CreateImage.self) }

    static func image(of window: NSWindow, scale: CGFloat) -> CGImage? {
        // kCGWindowListOptionIncludingWindow, kCGWindowImageBoundsIgnoreFraming | (1x: kCGWindowImageNominalResolution)
        let options: UInt32 = (1 << 0) | (scale <= 1 ? (1 << 4) : 0)
        return create?(.null, 1 << 3, UInt32(window.windowNumber), options)?.takeRetainedValue()
    }
}
#endif
