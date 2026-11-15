package dev.nucleusframework.composeswiftui

/**
 * Marks a top-level, parameterless `@Composable` function as a preview.
 *
 * The `dev.nucleusframework.compose-swiftui` Gradle plugin finds these functions in the sources and generates
 * a Swift `#Preview` for each one in `ComposeSwiftUIPreviews.swift`, next to the Swift renderer, so the
 * preview renders in Xcode's canvas with real SwiftUI. The same file declares `ComposeSwiftUIPreviewGallery`,
 * a SwiftUI view that lists every preview.
 *
 * Detection is by source scan, so any annotation named `Preview` works too (for example
 * `org.jetbrains.compose.ui.tooling.preview.Preview`). The function must not be private.
 *
 * @param name Title of the preview in Xcode and in the gallery. Defaults to the function name.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class Preview(val name: String = "")
