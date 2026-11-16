// WorkoutKit for Compose SwiftUI: workoutPreview (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerWorkoutKit()` at launch.
import SwiftUI

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
