import SwiftUI

extension ComposeIntegrations {
    /// Registers the integrations listed in `composeSwiftUI { integrations = listOf(…) }`. Called by
    /// `ComposeGenerated.setUp()`. The Gradle plugin rewrites this file with the calls; without a list it is empty and
    /// the app calls the `register…()` functions it needs.
    @MainActor static func registerConfigured() {}
}
