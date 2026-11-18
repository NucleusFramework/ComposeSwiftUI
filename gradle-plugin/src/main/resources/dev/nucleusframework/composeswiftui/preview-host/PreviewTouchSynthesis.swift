// Touches synthesized inside the preview host, so the IDE can drive a live preview with the mouse: a digitizer
// IOHIDEvent routed to the window's rendering context and enqueued in UIKit, which turns it into touches exactly as
// for a real finger (the technique of Lyft's Hammer). Private API, which is fine for a simulator-only development tool.

#if os(iOS)
import UIKit

@MainActor
final class TouchSynthesizer {
    private typealias CreateHandEvent = @convention(c) (
        CFAllocator?, UInt64, UInt32, UInt32, UInt32, UInt32, UInt32,
        Double, Double, Double, Double, Double, Bool, Bool, UInt32
    ) -> OpaquePointer?
    private typealias CreateFingerEvent = @convention(c) (
        CFAllocator?, UInt64, UInt32, UInt32, UInt32,
        Double, Double, Double, Double, Double, Double, Double, Double, Double, Double, Bool, Bool, UInt32
    ) -> OpaquePointer?
    private typealias SetIntegerValue = @convention(c) (OpaquePointer, UInt32, Int, UInt32) -> Void
    private typealias AppendEvent = @convention(c) (OpaquePointer, OpaquePointer, UInt32) -> Void
    private typealias SetSenderID = @convention(c) (OpaquePointer, UInt64) -> Void
    private typealias SetDigitizerInfo = @convention(c) (OpaquePointer, UInt32, UInt8, UInt8, CFString?, Double, Float) -> Void
    private typealias EnqueueEvent = @convention(c) (NSObject, Selector, OpaquePointer) -> Void

    private static let iokit = dlopen("/System/Library/Frameworks/IOKit.framework/IOKit", RTLD_NOW)
    private static let backBoard = dlopen("/System/Library/PrivateFrameworks/BackBoardServices.framework/BackBoardServices", RTLD_NOW)

    private static func symbol<T>(_ library: UnsafeMutableRawPointer?, _ name: String, _: T.Type) -> T? {
        dlsym(library, name).map { unsafeBitCast($0, to: T.self) }
    }

    private static let createHand = symbol(iokit, "IOHIDEventCreateDigitizerEvent", CreateHandEvent.self)
    private static let createFinger = symbol(iokit, "IOHIDEventCreateDigitizerFingerEventWithQuality", CreateFingerEvent.self)
    private static let setInteger = symbol(iokit, "IOHIDEventSetIntegerValueWithOptions", SetIntegerValue.self)
    private static let append = symbol(iokit, "IOHIDEventAppendEvent", AppendEvent.self)
    private static let setSenderID = symbol(iokit, "IOHIDEventSetSenderID", SetSenderID.self)
    private static let setDigitizerInfo = symbol(backBoard, "BKSHIDEventSetDigitizerInfo", SetDigitizerInfo.self)
    private static let enqueueSelector = NSSelectorFromString("_enqueueHIDEvent:")

    private static let transducerHand: UInt32 = 3
    private static let eventRange: UInt32 = 1 << 0
    private static let eventTouch: UInt32 = 1 << 1
    private static let eventPosition: UInt32 = 1 << 2
    private static let fieldDisplayIntegrated: UInt32 = (11 << 16) | 25
    private static let fieldBuiltIn: UInt32 = 4
    private static let fieldOptions: UInt32 = 0xF000_0000
    private static let senderID: UInt64 = 0x0000_0001_0000_0042

    static var isAvailable: Bool {
        createHand != nil && createFinger != nil && setInteger != nil && append != nil && setDigitizerInfo != nil &&
            UIApplication.shared.responds(to: enqueueSelector)
    }

    /// Sends one phase of the finger at [point] (window coordinates) to [window].
    func send(_ phase: PointerPhase, at point: CGPoint, in window: UIWindow) {
        guard Self.isAvailable, let event = hidEvent(phase, window.convert(point, to: nil)) else { return }
        Self.setSenderID?(event, Self.senderID)
        // Routes the event to the window's rendering context, as backboardd does for a real finger.
        let contextID = (window.value(forKey: "_contextId") as? NSNumber)?.uint32Value ?? 0
        Self.setDigitizerInfo?(event, contextID, 0, 0, nil, 0, 0)
        let application = UIApplication.shared
        unsafeBitCast(application.method(for: Self.enqueueSelector), to: EnqueueEvent.self)(application, Self.enqueueSelector, event)
        Unmanaged<CFTypeRef>.fromOpaque(UnsafeRawPointer(event)).release()
    }

    private func hidEvent(_ phase: PointerPhase, _ point: CGPoint) -> OpaquePointer? {
        guard let createHand = Self.createHand, let createFinger = Self.createFinger,
              let setInteger = Self.setInteger, let append = Self.append else { return nil }
        let time = mach_absolute_time()
        let mask = phase == .moved ? Self.eventPosition : (Self.eventRange | Self.eventTouch)
        let touching = phase != .ended
        guard let hand = createHand(kCFAllocatorDefault, time, Self.transducerHand, 0, 0, mask, 0, 0, 0, 0, 0, 0, touching, touching, 0) else {
            return nil
        }
        setInteger(hand, Self.fieldDisplayIntegrated, 1, Self.fieldOptions)
        setInteger(hand, Self.fieldBuiltIn, 1, Self.fieldOptions)
        if let finger = createFinger(
            kCFAllocatorDefault, time, 1, 2, mask, Double(point.x), Double(point.y), 0, 0, 0, 5, 5, 1, 1, 1, touching, touching, 0
        ) {
            setInteger(finger, Self.fieldDisplayIntegrated, 1, Self.fieldOptions)
            append(hand, finger, 0)
            Unmanaged<CFTypeRef>.fromOpaque(UnsafeRawPointer(finger)).release()
        }
        return hand
    }
}

/// Typing into the focused field (a SwiftUI `TextField` is a UIKit text input underneath).
@MainActor
enum KeyboardSynthesizer {
    static func type(_ text: String) {
        (FirstResponder.find() as? UIKeyInput)?.insertText(text)
    }

    static func deleteBackward() {
        (FirstResponder.find() as? UIKeyInput)?.deleteBackward()
    }
}

@MainActor
private enum FirstResponder {
    static weak var found: UIResponder?

    static func find() -> UIResponder? {
        found = nil
        UIApplication.shared.sendAction(#selector(UIResponder.csuiReportFirstResponder(_:)), to: nil, from: nil, for: nil)
        return found
    }
}

extension UIResponder {
    @objc fileprivate func csuiReportFirstResponder(_ sender: Any?) {
        MainActor.assumeIsolated { FirstResponder.found = self }
    }
}
#endif
