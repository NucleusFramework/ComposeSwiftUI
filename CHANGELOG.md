# Changelog

## Unreleased

### Added

- `@Preview` for Compose SwiftUI composables, rendered in Xcode's canvas. The Gradle plugin finds top-level,
  parameterless `@Preview @Composable` functions (any annotation named `Preview` works) and writes
  `ComposeSwiftUIPreviews.swift` next to the renderer, with one Swift `#Preview` per function and a
  `ComposeSwiftUIPreviewGallery` view that lists them all. The task is `generateComposeSwiftUIPreviews`.
- The sample app has two previews and opens the gallery from its home screen.

## 0.2.1

### Added

- `UIKitView`, `UIViewControllerView`, `AppKitView` and `NSViewControllerView` take a `sizing` parameter.
  `NativeViewSizing.FitHeight` sizes the embedded view to its content at the proposed width, so a paragraph of variable
  length is neither cut nor followed by a gap. `FitContent` uses the content's size in both directions. `Fill`, the
  default, keeps the previous behavior.
- The same four functions take an `update` callback that runs on every recomposition, to push Kotlin state into the
  embedded view.

## 0.2.0

### Added

- `TabView(selection, onSelectionChange)` and `Tab(title, systemImage, value)`: the selected tab is Kotlin state, so
  code can switch tabs (for example a button in one tab that opens another). Tapping the tab bar calls
  `onSelectionChange`.
- Mixed-style text: `Text(StyledString)`, built with `buildStyledString { withStyle(SpanStyle(...)) { append(...) } }`
  like Compose's `buildAnnotatedString`. A `SpanStyle` sets color, font, font size, weight, design, italic, underline,
  strikethrough, monospaced, baseline offset and kerning. Nested styles merge. This replaces `Text + Text` and
  `AttributedString`, which Kotlin can't express.
- The sample app shows both: a tab bar switched from Kotlin and a styled paragraph.

## 0.1.1

- GitHub releases are created automatically when a version is tagged.

## 0.1.0

- First release: SwiftUI views and modifiers generated from Apple's SDK for iOS, macOS, tvOS and watchOS, the Compose
  runtime host, `viewModel { }`, hand-written APIs for navigation, presentations, gestures, layouts and shapes,
  Navigation 3 `NavDisplay`, UIKit and AppKit embedding, and the `dev.nucleusframework.compose-swiftui` Gradle plugin.
