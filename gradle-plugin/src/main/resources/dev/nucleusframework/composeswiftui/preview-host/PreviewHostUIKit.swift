// The preview host on the iOS simulator: one UIWindow per preview, in the app's scene.

#if os(iOS)
import notify
import SwiftUI
import UIKit

@main
struct ComposeSwiftUIPreviewHost: App {
    init() {
        ComposeInspection.enabled = true
    }

    var body: some Scene {
        WindowGroup { HostRoot() }
    }
}

private struct HostRoot: View {
    var body: some View {
        // `Run on Simulator`: the preview, live, full screen on the simulator.
        if let unit = Host.runUnit {
            PreviewContent(unit: unit, host: unit.makeHost())
        } else {
            Text("Rendering previews…")
                .foregroundStyle(.secondary)
                .task {
                    guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first else { exit(1) }
                    PreviewHostPlatform.scene = scene
                    await Host.serve()
                }
        }
    }
}

@MainActor
enum PreviewHostPlatform {
    fileprivate static var scene: UIWindowScene!

    static func makeSurface(unit: PreviewUnit, host: ComposeSwiftUIHost, interactive: Bool) -> PreviewSurface {
        UIKitSurface(unit: unit, host: host, scene: scene, interactive: interactive)
    }

    /// The state follows the foreground: only the foreground app of a simulator renders.
    static func observeActivity(_ update: @escaping @MainActor (Bool) -> Void) {
        for (name, active) in [(UIApplication.didEnterBackgroundNotification, false), (UIApplication.didBecomeActiveNotification, true)] {
            NotificationCenter.default.addObserver(forName: name, object: nil, queue: .main) { _ in
                MainActor.assumeIsolated { update(active) }
            }
        }
    }

    /// Slow motion for the animation inspector: the simulator's *Slow Animations* (UIKit and SwiftUI animations follow
    /// its `UIAnimationDragCoefficient`), on for a [factor] below 1. The slowdown is the simulator's own.
    static func setAnimationSpeed(_ factor: Double) {
        let name = "com.apple.UIKit.SimulatorSlowMotionAnimationState"
        var token: Int32 = 0
        guard notify_register_check(name, &token) == NOTIFY_STATUS_OK else { return }
        notify_set_state(token, factor < 1 ? 1 : 0)
        notify_post(name)
        notify_cancel(token)
    }
}

@MainActor
private final class UIKitSurface: PreviewSurface {
    private let job: PreviewJob
    private let window: UIWindow
    private let controller: UIHostingController<PreviewContent>
    private let screen: CGSize
    private let touches = TouchSynthesizer()

    init(unit: PreviewUnit, host: ComposeSwiftUIHost, scene: UIWindowScene, interactive: Bool) {
        job = unit.job
        controller = UIHostingController(rootView: PreviewContent(unit: unit, host: host))
        window = UIWindow(windowScene: scene)
        screen = scene.screen.bounds.size
        let size = PreviewWindow.configure(controller, window: window, job: job, screen: screen)
        window.rootViewController = controller
        window.frame = CGRect(origin: .zero, size: size)
        if interactive {
            window.windowLevel = .normal + 1
            window.makeKeyAndVisible()
        } else {
            window.isHidden = false
        }
    }

    var bounds: CGRect { window.bounds }
    var scale: CGFloat { window.screen.scale }
    var pointerAvailable: Bool { TouchSynthesizer.isAvailable }

    func fitContent() {
        if job.sizeThatFits { window.frame.size = PreviewWindow.size(of: job, controller: controller, screen: screen) }
    }

    func snapshot(scale: CGFloat, afterScreenUpdates: Bool) -> CGImage? {
        let format = UIGraphicsImageRendererFormat()
        format.scale = scale
        return UIGraphicsImageRenderer(bounds: window.bounds, format: format).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: afterScreenUpdates)
        }.cgImage
    }

    func close() {
        window.isHidden = true
        window.rootViewController = nil
    }

    func pointer(_ phase: PointerPhase, at point: CGPoint) {
        touches.send(phase, at: point, in: window)
    }

    func scroll(at point: CGPoint, by delta: CGPoint) {
        var view = window.hitTest(point, with: nil)
        while let current = view, !(current is UIScrollView) { view = current.superview }
        guard let scrollView = view as? UIScrollView else { return }
        let inset = scrollView.adjustedContentInset
        let maxX = max(-inset.left, scrollView.contentSize.width - scrollView.bounds.width + inset.right)
        let maxY = max(-inset.top, scrollView.contentSize.height - scrollView.bounds.height + inset.bottom)
        let offset = CGPoint(
            x: min(max(scrollView.contentOffset.x + delta.x, -inset.left), maxX),
            y: min(max(scrollView.contentOffset.y + delta.y, -inset.top), maxY)
        )
        scrollView.setContentOffset(offset, animated: false)
    }

    func type(_ text: String) { KeyboardSynthesizer.type(text) }

    func deleteBackward() { KeyboardSynthesizer.deleteBackward() }
}

/// The window of a preview, set up from its `@Preview` arguments: size, size classes, appearance, direction.
@MainActor
private enum PreviewWindow {
    /// Configures [window] and [controller] for [job] and returns the window size.
    static func configure(_ controller: UIHostingController<PreviewContent>, window: UIWindow, job: PreviewJob, screen: CGSize) -> CGSize {
        if job.width > 0 || job.height > 0 || job.sizeThatFits {
            // A component, not a screen: no status bar or home indicator insets.
            controller.safeAreaRegions = []
            let width = CGFloat(job.width > 0 ? job.width : Int(screen.width))
            controller.traitOverrides.horizontalSizeClass = width >= 700 ? .regular : .compact
            if job.height > 0 { controller.traitOverrides.verticalSizeClass = job.height < 500 ? .compact : .regular }
        }
        switch job.colorScheme {
        case "dark": window.overrideUserInterfaceStyle = .dark
        case "light": window.overrideUserInterfaceStyle = .light
        default: break
        }
        if job.layoutDirection == "rightToLeft" { controller.traitOverrides.layoutDirection = .rightToLeft }
        controller.view.backgroundColor = .systemBackground
        return size(of: job, controller: controller, screen: screen)
    }

    /// Size of the window: the `@Preview` size, else the screen; the content's height with `sizeThatFits`.
    static func size(of job: PreviewJob, controller: UIHostingController<PreviewContent>, screen: CGSize) -> CGSize {
        let width = job.width > 0 ? CGFloat(job.width) : screen.width
        var height = job.height > 0 ? CGFloat(job.height) : screen.height
        if job.sizeThatFits {
            let fitting = controller.sizeThatFits(in: CGSize(width: width, height: job.height > 0 ? height : .greatestFiniteMagnitude))
            if fitting.height.isFinite && fitting.height > 0 { height = min(fitting.height, job.height > 0 ? height : 4 * screen.height) }
            let fittingWidth = job.width > 0 ? width : min(max(fitting.width, 1), width)
            return CGSize(width: ceil(fittingWidth), height: ceil(height))
        }
        return CGSize(width: width, height: height)
    }
}
#endif
