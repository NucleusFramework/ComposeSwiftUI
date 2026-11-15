# Changelog

## Unreleased

### Added

- APIs that take a view as a value instead of a `@ViewBuilder` closure: `Modifier.listRowBackground { … }`,
  `Modifier.mask { … }`, the `background`/`overlay(_:alignment:)` and `navigationBarItems` variants, `AnyView`,
  `FillShapeView`/`StrokeShapeView`/`StrokeBorderShapeView`, `MenuButton`, and the remaining `NavigationLink`,
  `Picker` and `Slider` overloads (`NavigationLinkTag`, `NavigationLinkIsActive`, `PickerSelection`, `SliderValue`).
- Fixed-width integers (`Int8` … `UInt64`) as `Int`, and `UTType`, `Locale`, `Locale.Language`, `Character` and
  `AttributedString` as `String`, for plain, optional and list parameters, bindings and callbacks. New APIs include
  the `OptionSet` `rawValue` initializers, `Modifier.typesettingLanguage`, `TextEditor(text:)` over an
  `AttributedString`, and the `contentType` variants of `NewDocumentButton`.
- `Modifier.onOpenURL { url -> }`: callbacks receive a `URL` as its absolute string.
- Asset-catalog `ImageResource`/`ColorResource` parameters, by name: `Button(title, image = "logo") { }`, and the
  `image:` variants of `Menu`, `Toggle`, `Picker`, `ControlGroup`.
- `ClosedRange<Date>` parameters as `ClosedFloatingPointRange<Double>` of epoch seconds: `DatePicker(…, range = …)`,
  `ProgressView(timerInterval = …)`, `Text(timerInterval = …)`.
- `Transferable` payloads as `String` (`ShareLink(item = "…")`), and `CGAffineTransform` (`Modifier.transformEffect`).
- Bindings and callbacks of SwiftUI value types, when Swift can send the value back: enums, closed sets of static
  values and `Color`. New: `ColorPicker(selection, onSelectionChange)`,
  `NavigationSplitView(columnVisibility, onColumnVisibilityChange, preferredCompactColumn, …)`,
  `Modifier.onTapGesture(coordinateSpace = …) { point -> }`, `Modifier.onContinuousHover`,
  `Modifier.accessibilityAdjustableAction`, `Modifier.accessibilityScrollAction`, `Modifier.onPencilSqueeze`.
  A color picked by the user arrives as `Color(red, green, blue, opacity)` in sRGB.
- Hand-written APIs for SwiftUI views whose content depends on SwiftUI-only values: `AsyncImage` with load
  states (`AsyncImageContent` shows the image), `ScrollViewReader` (`ScrollProxy.scrollTo`), `TimelineView`, `Canvas`
  (a Kotlin `DrawScope`), `Table` with columns and selection (iOS 17.4+, macOS 14.4+), `OutlineGroup`,
  `Modifier.onKeyPress`, `scrollTransition`, `visualEffect`, `alignmentGuide`, `navigationDestination(item:)`, and
  custom button, toggle and label styles written in Kotlin (`StyleLabel`, `StyleIcon`).
- `List(selectedKeys, onSelectedKeysChange)` (multiple selection), `searchable` with tokens, `NumberField`
  (`TextField(value:format:)`), `focused(value, focusedValue)`, `searchFocused`, `accessibilityFocused`,
  `glassEffectID`/`glassEffectUnion`, `focusScope`, `onGeometryChange`, `onScrollGeometryChange`,
  `onScrollTargetVisibilityChange`, `fileImporter`, `fileExporter` (text), `PasteButton`, `draggable`/`dropDestination`
  (text), `phaseAnimator`, `symbolEffect`, `MeshGradient`, `keyboardType`, `textContentType`, `MultiDatePicker`, and
  `Image(uiImage)` / `Image(nsImage)` in the platform source sets.
- `rotation3DEffect`, `dynamicTypeSize(min, max)`, `containerRelativeFrame` with a Kotlin length function,
  `presentationDetents(detents, selection)`, `accessibilityLinkedGroup`, `accessibilityLabeledPair`,
  `prefersDefaultFocus`, and inline SF Symbols in styled text (`StyledStringBuilder.appendSymbol`).
- `Modifier.environment(name = value)` for the 60 settable `EnvironmentValues` (`colorScheme`, `locale`, `font`,
  `lineLimit`, `controlSize`…), SwiftUI's `.environment(\.name, value)`.
- Optional closures (`Stepper(onIncrement, onDecrement)`, `onDeleteCommand`, `onExitCommand`, `onMoveCommand`, the
  `onDismiss` of `sheet`/`fullScreenCover`, `onPressingChanged` of `onLongPressGesture`) and callbacks with several
  parameters (`onScrollPhaseChange { old, new -> }`, `onChange(value) { old, new -> }`).

### Changed

- A trailing lambda now binds the main closure, as in Swift: `Slider(value, bounds) { value = it }` and
  `Stepper(…) { }` pass it to `onValueChange` (it used to go to `onEditingChanged`), and optional closures such as
  `onPressingChanged` come before `perform`.
- `NewDocumentButtonSource` (deprecated alias) is now `NewDocumentButtonContentType`.
- The Swift renderer has two new files, `Interactive.swift` and `Generated/GenEncoders.swift`. If you added the
  `ComposeSwiftUI` folder to Xcode as a group, add them to the app target once.

## 0.4.1

### Added

- IntelliJ plugin: *Code › Render Compose SwiftUI Previews* renders the previews of the current file again, with
  ⌥⇧⌘P on macOS (Ctrl+Alt+Shift+P elsewhere).

### Fixed

- IntelliJ plugin: the preview editor reads the file and the preview images in the background. It used to do it on
  the UI thread when the file was opened, which the IDE reports as an error and which could freeze it briefly.

## 0.4.0

### Changed (breaking)

- `systemImage` and `systemName` parameters take an `SFSymbol` instead of a `String`. Constants are generated for
  every SF Symbol (`SFSymbol.starFill`, with availability in their KDoc); `SFSymbol("name")` covers custom or newer
  symbols. Migrate with `systemImage = "star.fill"` → `systemImage = SFSymbol.starFill`.
- `SwiftUIEnvironment` exposes `ColorScheme`, `DynamicTypeSize`, `UserInterfaceSizeClass` and `LayoutDirection`
  instead of strings. An absent size class is now `null` (it used to be an empty string).

### Added

- `Label` and `ContentUnavailableView` overloads that take an asset-catalog `image` with a `LocalizedKey` title. They
  used to clash with the `systemImage` overloads.

### Fixed

- `renderComposeSwiftUIPreviews` links static frameworks that use C libraries, such as SQLDelight's SQLite driver.
  It passes the framework's `linkerOpts` to the preview host and strips dead code like Xcode does. If symbols are
  still missing, the error says to declare those libraries with `linkerOpts`.

## 0.3.2

### Fixed

- `renderComposeSwiftUIPreviews` works with the configuration cache in modules that use Compose Multiplatform
  resources. It used to fail with "cannot serialize object of type AssembleTargetResourcesTask".
- The IntelliJ plugin asks for an IDE restart after it is installed. Without the restart, the previews stayed blank.

## 0.3.1

### Added

- IntelliJ IDEA / Android Studio plugin (`intellij-plugin`): Kotlin files with `@Preview` open in a code | preview split
  editor, with a gutter icon per preview. Previews are rendered with real SwiftUI on the iOS simulator by the new
  `renderComposeSwiftUIPreviews` Gradle task, and again on every save.

### Fixed

- `TextEditorView(TextEditor(text:))` was read-only: bindings inside nested values were sent to SwiftUI as constants.
  They now update the text and call the Kotlin `onTextChange` callback.

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
