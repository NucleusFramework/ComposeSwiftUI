import SwiftUI

@main
struct SampleApp: App {
    init() {
        // Lists the Kotlin @Preview composables (generated in ComposeSwiftUI/ComposeSwiftUIPreviews.swift).
        ComposeRegistry.nativeViews["previewGallery"] = { _ in AnyView(ComposeSwiftUIPreviewGallery()) }
    }

    var body: some Scene {
        WindowGroup {
            ComposeView { SampleAppKt.sampleApp() }
        }
    }
}
