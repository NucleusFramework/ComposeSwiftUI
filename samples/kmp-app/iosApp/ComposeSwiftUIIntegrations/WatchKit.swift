// WatchKit for Compose SwiftUI: NowPlayingView (runtime Frameworks.kt).
// Add this file to the app target and call `ComposeIntegrations.registerWatchKit()` at launch.
import SwiftUI

#if os(watchOS)
import WatchKit

public extension ComposeIntegrations {
    @MainActor static func registerWatchKit() {
        ComposeRegistry.views[200_323] = { m in ModChain.apply(NowPlayingView(), m, 0) }
    }
}
#endif
