// Host app run by the `renderComposeSwiftUIPreviews` Gradle task on the iOS simulator: shows each Kotlin
// @Preview full screen, snapshots the window to `<output>/<qualified name>.png`, then exits.
// The list of previews (`composeSwiftUIRenderedPreviews`) is generated next to this file.

import SwiftUI
import UIKit

@main
struct ComposeSwiftUIPreviewHost: App {
    var body: some Scene {
        WindowGroup { PreviewRunner() }
    }
}

private struct PreviewRunner: View {
    @State private var index = 0

    var body: some View {
        let previews = composeSwiftUIRenderedPreviews
        Group {
            if index < previews.count {
                ComposeView(previews[index].makeHost).id(index)
            }
        }
        .task(id: index) {
            guard index < previews.count else { exit(0) }
            // Lets the composition apply and SwiftUI lay out (and run its first animations).
            try? await Task.sleep(for: .milliseconds(Self.settleMilliseconds))
            snapshot(to: Self.outputDirectory.appendingPathComponent("\(previews[index].id).png"))
            index += 1
        }
    }

    private func snapshot(to file: URL) {
        guard let window = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
        else { return }
        let image = UIGraphicsImageRenderer(bounds: window.bounds).image { _ in
            window.drawHierarchy(in: window.bounds, afterScreenUpdates: true)
        }
        do {
            try image.pngData()?.write(to: file)
            print("Rendered \(file.lastPathComponent)")
        } catch {
            print("Failed to write \(file.path): \(error)")
        }
    }

    private static let environment = ProcessInfo.processInfo.environment
    private static let outputDirectory = URL(fileURLWithPath: environment["COMPOSE_SWIFTUI_PREVIEW_OUTPUT"] ?? NSTemporaryDirectory())
    private static let settleMilliseconds = Int(environment["COMPOSE_SWIFTUI_PREVIEW_SETTLE_MS"] ?? "") ?? 800
}
