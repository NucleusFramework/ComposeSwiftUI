// AppIntents, DeviceActivity, WatchKit, ManagedAppDistribution, PermissionKit, ScreenCaptureKit, WorkoutKit and
// DeviceDiscoveryUI for Compose SwiftUI (runtime Frameworks.kt). Each framework registers on its own:
// `ComposeIntegrations.registerAppIntents()`, `registerDeviceActivity()`… Remove the sections of frameworks the app
// doesn't link.
import SwiftUI

#if os(iOS) || os(macOS) || os(watchOS) || os(visionOS)
import AppIntents

public extension ComposeIntegrations {
    @MainActor static func registerAppIntents() {
        #if os(iOS) || os(visionOS)
        ComposeRegistry.views[200_321] = { m in ModChain.apply(ShortcutsLink(action: m.args.action(0)), m, 0) }
        #endif
        ComposeRegistry.modifiers[200_321] = { view, a, _ in
            var v = view
            #if os(iOS) || os(visionOS)
            if let i = a.intOpt(0) {
                let styles: [ShortcutsLinkStyle] = [.automatic, .automaticOutline, .dark, .darkOutline, .light, .lightOutline]
                v = AnyView(v.shortcutsLinkStyle(styles[min(max(i, 0), 5)]))
            }
            #endif
            #if !os(macOS)
            if let i = a.intOpt(1) {
                let style: SiriTipViewStyle = i == 2 || i == 3 ? .dark : i == 4 || i == 5 ? .light : .automatic
                v = AnyView(v.siriTipViewStyle(style))
            }
            #endif
            return v
        }
    }
}
#endif

#if os(iOS)
import DeviceActivity

public extension ComposeIntegrations {
    @MainActor static func registerDeviceActivity() {
        ComposeRegistry.views[200_322] = { m in ModChain.apply(DeviceActivityReport(DeviceActivityReport.Context(m.args.string(0))), m, 0) }
    }
}
#endif

#if os(watchOS)
import WatchKit

public extension ComposeIntegrations {
    @MainActor static func registerWatchKit() {
        ComposeRegistry.views[200_323] = { m in ModChain.apply(NowPlayingView(), m, 0) }
    }
}
#endif

#if os(macOS)
import ManagedAppDistribution

public extension ComposeIntegrations {
    @MainActor static func registerManagedAppDistribution() {
        guard #available(macOS 26.4, *) else { return }
        ComposeRegistry.views[200_324] = { m in
            let a = m.args
            let labels = a.list(0).map { $0 as? String ?? "" }
            let states: [ManagedContentOfferState] = [.notInstalled, .neverInstalled, .installing(progress: nil), .installed, .noninteractive]
            let state = states[min(max(a.int(1), 0), 4)]
            let report = a.callback(2, box: { (i: Int) -> Any? in KotlinInt(value: Int32(i)) })
            let index = { (s: ManagedContentOfferState) -> Int in
                if s == .notInstalled { return 0 }
                if s == .neverInstalled { return 1 }
                if s == .installed { return 3 }
                if s == .noninteractive { return 4 }
                return 2
            }
            func label(_ i: Int) -> String { i < labels.count ? labels[i] : "" }
            return ModChain.apply(ManagedContentView(primaryLabel: label(0), secondaryLabel: label(1), tertiaryLabel: label(2), quaternaryLabel: label(3),
                                                     offerState: state, offerAction: { report(index($0)) }) { SlotView(model: a.slotModel(3)) }, m, 0)
        }
        ComposeRegistry.modifiers[200_322] = { v, a, _ in
            let styles: [ManagedContentStyle] = [.automatic, .compact, .header, .vertical]
            return AnyView(v.managedContentStyle(styles[min(max(a.int(0), 0), 3)]))
        }
    }
}
#endif

#if os(iOS) || os(macOS)
import PermissionKit

public extension ComposeIntegrations {
    @MainActor static func registerPermissionKit() {
        guard #available(iOS 26.0, macOS 26.0, *) else { return }
        ComposeRegistry.views[200_325] = { m in
            let a = m.args
            let handles = a.list(0).compactMap { $0 as? [Any] }.map { h -> CommunicationHandle in
                let kinds: [CommunicationHandle.Kind] = [.phoneNumber, .emailAddress, .custom]
                return CommunicationHandle(value: h.first as? String ?? "", kind: kinds[min(max((h.count > 1 ? h[1] as? NSNumber : nil)?.intValue ?? 0, 0), 2)])
            }
            let slot = a.slotModel(1)
            return ModChain.apply(CommunicationLimitsButton(question: PermissionQuestion(handles: handles)) { SlotView(model: slot) }, m, 0)
        }
    }
}
#endif

#if canImport(_ScreenCaptureKit_SwiftUI)
import ScreenCaptureKit

public extension ComposeIntegrations {
    @MainActor static func registerScreenCaptureKit() {
        guard #available(iOS 27.0, macOS 27.0, tvOS 27.0, *) else { return }
        ComposeRegistry.modifiers[200_323] = { v, a, _ in
            let set = a.callback(1, box: { (s: String?) -> Any? in s ?? NSNull() })
            let item = Binding(get: { a.stringOpt(0).flatMap(URL.init(string:)) }, set: { set($0?.absoluteString) })
            #if os(tvOS)
            if let mode = a.boolOpt(2) { return AnyView(v.recordingEditor(item, mode: mode ? .share : .preview)) }
            #endif
            return AnyView(v.recordingEditor(item))
        }
    }
}
#endif

#if os(iOS) || os(watchOS)
import WorkoutKit
import HealthKit

public extension ComposeIntegrations {
    @MainActor static func registerWorkoutKit() {
        guard #available(iOS 17.0, watchOS 11.0, *) else { return }
        ComposeRegistry.modifiers[200_324] = { v, a, _ in
            let activity = HKWorkoutActivityType(rawValue: UInt(max(a.int(2), 0))) ?? .running
            let g = a.list(3)
            let value = g.count > 1 ? (g[1] as? NSNumber)?.doubleValue ?? 0 : 0
            let goal: WorkoutGoal
            switch (g.first as? NSNumber)?.intValue ?? 0 {
            case 1: goal = .distance(value, .kilometers)
            case 2: goal = .time(value, .minutes)
            case 3: goal = .energy(value, .kilocalories)
            default: goal = .open
            }
            let plan = WorkoutPlan(.goal(SingleGoalWorkout(activity: activity, goal: goal)))
            return AnyView(v.workoutPreview(plan, isPresented: a.boolBinding(0)))
        }
    }
}
#endif

#if os(tvOS)
import DeviceDiscoveryUI
import Network

public extension ComposeIntegrations {
    @MainActor static func registerDeviceDiscoveryUI() {
        guard #available(tvOS 16.0, *) else { return }
        ComposeRegistry.views[200_326] = { m in
            let a = m.args
            let report = a.callback(1, box: { (s: String) -> Any? in s })
            return ModChain.apply(DevicePicker(.applicationService(name: a.string(0)), onSelect: { report("\($0)") }) {
                SlotView(model: a.slotModel(2))
            } fallback: {
                SlotView(model: a.slotModel(3))
            }, m, 0)
        }
    }
}
#endif
