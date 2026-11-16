// HealthKitUI for Compose SwiftUI: healthDataAccessRequest (runtime AppFrameworks.kt). Add this file to the app
// target, enable the HealthKit capability and call `ComposeIntegrations.registerHealthKitUI()` at launch.
import SwiftUI

#if os(iOS) || os(watchOS)
import HealthKit
import HealthKitUI

public extension ComposeIntegrations {
    @MainActor static func registerHealthKitUI() {
        guard #available(iOS 17.0, watchOS 10.2, *) else { return }
        ComposeRegistry.modifiers[200_347] = { v, a, _ in
            let report = a.callback(4, box: { (s: String?) -> Any? in s ?? NSNull() })
            let done: @Sendable (Result<Bool, any Error>) -> Void = { r in
                Task { @MainActor in
                    if case .failure(let e) = r { report(e.localizedDescription) } else { report(nil) }
                }
            }
            let trigger = a.hashableOpt(2)
            #if os(iOS)
            if let id = a.stringOpt(3), let type = HealthTypes.type(id) {
                return AnyView(v.healthDataAccessRequest(store: HealthTypes.store, objectType: type, trigger: trigger, completion: done))
            }
            #endif
            let read = Set(a.list(0).compactMap { ($0 as? String).flatMap(HealthTypes.type) })
            let share = Set(a.list(1).compactMap { ($0 as? String).flatMap(HealthTypes.type) as? HKSampleType })
            if share.isEmpty {
                return AnyView(v.healthDataAccessRequest(store: HealthTypes.store, readTypes: read, trigger: trigger, completion: done))
            }
            return AnyView(v.healthDataAccessRequest(store: HealthTypes.store, shareTypes: share, readTypes: read, trigger: trigger, completion: done))
        }
    }
}

enum HealthTypes {
    static let store = HKHealthStore()

    /// The HealthKit type of an identifier (quantity, category, characteristic, correlation, workout or vision prescription).
    static func type(_ id: String) -> HKObjectType? {
        if id == HKWorkoutTypeIdentifier { return HKObjectType.workoutType() }
        if #available(iOS 16.0, watchOS 9.0, *), id == HKVisionPrescriptionTypeIdentifier { return HKObjectType.visionPrescriptionType() }
        return HKObjectType.quantityType(forIdentifier: .init(rawValue: id))
            ?? HKObjectType.categoryType(forIdentifier: .init(rawValue: id))
            ?? HKObjectType.characteristicType(forIdentifier: .init(rawValue: id))
            ?? HKObjectType.correlationType(forIdentifier: .init(rawValue: id))
    }
}
#endif
