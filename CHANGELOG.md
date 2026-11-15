# Changelog

## Unreleased

### Added

- IntelliJ IDEA / Android Studio plugin (`intellij-plugin`): Kotlin files with `@Preview` open in a code | preview split
  editor, with a gutter icon per preview. Previews are rendered with real SwiftUI on the iOS simulator by the new
  `renderComposeSwiftUIPreviews` Gradle task, and again on every save.

## 0.3.0

### Added

- `@Preview` for Compose SwiftUI composables, rendered in Xcode's canvas. The Gradle plugin finds top-level,
  parameterless `@Preview @Composable` functions (any annotation named `Preview` works) and writes
  `ComposeSwiftUIPreviews.swift` next to the renderer, with one Swift `#Preview` per function and a
  `ComposeSwiftUIPreviewGallery` view that lists them all. The task is `generateComposeSwiftUIPreviews`.
- `LazyForEach(items, key) { item -> }` for long lists: inside a `List` or a lazy stack, a row is composed only while
  SwiftUI shows it, and disposed when it scrolls away. A 10,000-row list now mounts in 28 ms instead of 909 ms on the
  iOS simulator, and a one-row change takes 0.4 ms instead of 4.5 ms.
- A scenario benchmark (`scripts/run_scenarios.sh`): long lists (mount, scrolling, one-row change, every row changed)
  and animations, compared with plain SwiftUI using native `@Observable` models.
- The sample app has two previews, a 5,000-row list and opens the preview gallery from its home screen.

### Changed

- Generated containers render their content through `ChildrenView`, and `flushCompose()` runs up to four passes so that
  state written by effects is shown in the same SwiftUI update.

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
