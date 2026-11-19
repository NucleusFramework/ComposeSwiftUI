# Changelog

## 0.9.3

### Added

- *iOS Simulator* tool window: *Lock*, *Volume Up* and *Volume Down*, real hardware buttons pressed through Xcode's
  CoreDevice (a small helper compiled on the Mac on first use), as Xcode's device window does.
- A phone outline icon for the *iOS Simulator* tool window, the *iOS App* configuration and the simulators of the
  toolbar, like Android Studio's for its devices.

### Fixed

- Remote Mac: previews' Gradle renders and the *iOS App* configuration's Xcode builds take turns on the Mac (a lock
  file, with `lockf`). Running both at once left Kotlin/Native's incremental caches inconsistent: the next link failed
  with "is cached, but its dependency isn't". *Stop* also ends the Xcode build.
- Previews: when the simulator lost the preview host (its files are there but it isn't registered, after an erase or a
  crash of the simulator's services), it is installed again instead of failing to launch every time.

## 0.9.2

### Added

- *iOS Simulator* tool window: *Rotate Left* and *Rotate Right*; the screen, touches and the Home gesture follow the
  app's orientation.

### Fixed

- *iOS Simulator* tool window with a foldable simulator (iPhone Duo): it shows the screen the posture lights, instead of
  the dark one. Its touches go through CoreDevice, which the stream can't reach yet: it is view only (sending them
  crashed the simulator's backboardd). Its screenshots capture the shown screen.
- Remote Mac: paths come back mapped when `~/.compose-swiftui/projects` on the Mac is a symbolic link to another disk.
- Simulators of a device's own runtime are listed as `iOS 27.2`, not `iOS 27.2.iPhone19.4`.

## 0.9.1

### Fixed

- *Copy Android Studio mcp.json* copies a whole `mcp.json` (`{"mcpServers": {"compose-swiftui": …}}`), valid as pasted
  into Android Studio's *MCP Servers › JSON View*; the entry alone was rejected there.

## 0.9.0

### Added

- Apple development from Linux and Windows with a remote Mac: set *Settings › Tools › Compose SwiftUI Previews ›
  Remote Mac* to an SSH destination, and the IDE plugin works as on a Mac. The project is synchronized there with
  rsync (only what changed); Gradle builds and renders in the Mac's daemon; images, results and compiler errors come
  back with this machine's paths. Live literals, interactive mode (its stream forwarded over SSH), the agent tools and
  *Run Preview* work the same.
- The *iOS Simulator* tool window: a simulator of the Mac (this one or the remote one) live in the IDE, like Android
  Studio's *Running Devices*. The screen is read from the simulator's framebuffer and touches, keys and the Home
  gesture are sent as HID events, headless: no Simulator window, no screen recording permission. The mouse touches,
  drags and scrolls, the keyboard types. Beside the screen: Home, the app switcher, a screenshot, shake, light or dark
  appearance. It connects to the booted simulator when it opens.
- The *iOS App* run configuration: builds the app's Xcode project on the Mac, installs it on a simulator and runs it,
  with its output in the Run tool window; *Stop* ends it on the simulator. With a remote Mac, it is created for the
  project's Xcode project, and the toolbar lists the Mac's simulators next to it.
- `./gradlew -p intellij-plugin runAndroidStudio -PandroidStudio=<install dir>` runs the IDE plugin in a sandbox
  Android Studio, with the sample.

### Fixed

- Previews use the app's asset catalogs: named colors and images (`Color("Brand")`, `Image("logo")`) rendered empty, as
  the preview host didn't have them. The `.xcassets` of the app's sources (the folder of `swiftOutputDir`) are compiled
  into it; `composeSwiftUI { previewAssetCatalogs }` sets others.
- Android Studio: the preview pane shows for files with previews. Android Studio's own Kotlin editor was selected
  instead, leaving the previews in a tab at the bottom of the editor.

## 0.8.2

### Added

- The agent tools in Android Studio and any MCP client: the IDE plugin serves them on an MCP server of its own
  (`http://127.0.0.1:64360/mcp`, streamable HTTP) when the IDE has no MCP Server plugin, for Android Studio's agent
  (`mcp.json`), Claude Code, Codex or Gemini CLI. *Settings › Tools › Compose SwiftUI Previews* shows its address
  and copies the configuration.

## 0.8.1

### Added

- Previews of macOS apps in the IDE: in a module with the macOS target and no iOS simulator target, previews render on
  the Mac itself, with AppKit's SwiftUI; elsewhere `@Preview(device = "mac")` (or `previewDevice.set("mac")`) sends
  them there. A background host app without a Dock icon renders them off screen, captured as the WindowServer draws
  them, with live literals, click-to-code, the agent tools, interactive mode (real mouse and key events) and *Run
  Preview* in a window. The macOS framework is only linked when a preview renders on the Mac.
- Previews for AI agents, like Android Studio's agent tools for Compose previews:
  - IDE plugin: tools on the IDE's MCP server (`list_swiftui_previews`, `render_swiftui_preview`,
    `render_swiftui_preview_code`, `check_swiftui_preview`, `compare_swiftui_preview_to_image`, `compare_images`,
    `verify_batch_visual_parity`) for Claude Code, Junie, Codex or any agent connected to the IDE;
  - `listComposeSwiftUIPreviews`, and `renderComposeSwiftUIPreviews` with `-PcomposeSwiftUI.previewFile` and
    `-PcomposeSwiftUI.golden`, writing `report.md` and a component tree per preview (`<key>.tree.txt`);
  - UI checks of every rendered preview: touch targets, text contrast, small text, overlapping texts, clipped views;
  - image comparison with expected images or folders of screenshots (mismatch, PSNR, diff and side-by-side images);
  - scratch previews: throwaway code in `build/composeSwiftUI/scratch` rendered without touching the sources.
- The layout of a rendered preview records the strings of each view (text, label, SF Symbol name).

### Changed

- *Run on Simulator* is now *Run Preview*, as Mac previews open on the Mac.

## 0.8.0

### Added

- `@Preview` arguments: `group`, `device`, `width` / `height`, `sizeThatFits`, `colorScheme`, `dynamicTypeSize`,
  `locale`, `layoutDirection` and `backgroundColor`, applied in the IDE and in Xcode's `#Preview`s.
- Variants: `@Preview` is repeatable and works on annotation classes (multipreviews), with `@PreviewLightDark`,
  `@PreviewDynamicTypeSizes`, `@PreviewScreenSizes` and `@PreviewLayoutDirections` provided.
- `@PreviewParameter(Provider::class, limit)` and `PreviewParameterProvider`: one preview per sample value.
- IDE plugin, at parity with Android Studio's Compose preview:
  - previews render when a file opens and on save (or while typing, or only on demand), only while the pane is
    visible, and show *Out of date* when the code changed;
  - list, grid and gallery layouts, zoom (toolbar, Ctrl/⌘ + wheel, fit), group filter;
  - the caret selects the preview of its function, and clicking a preview moves the caret to its function;
  - *Run on Simulator* opens a preview live on the simulator; copy or save a preview image; render one preview or the
    whole module;
  - compile errors as links to their line, and the Kotlin exception of a crashing preview in its place;
  - click a view of a preview to go to its code (hover outlines it and names the call);
  - live literals: string, number and boolean edits show in the previews within half a second, without a build;
  - interactive mode in the pane: the preview runs live, the mouse taps, drags and scrolls (real touches), the keyboard
    types; *Select Component* goes to a view's code, *Slow Animations* slows animations down, and *Record Animation*
    replays them frame by frame on a timeline (the animation inspector);
  - an inspection for `@Preview` functions that can't render, with quick fixes;
  - *Settings › Tools › Compose SwiftUI Previews*.

- `ComposeSwiftUIHost.inspectable` and `setLiteralPatches(…)`: the hooks the IDE previews use (source of each node,
  literal edits without recompiling). The library is compiled with Compose source information.

### Changed

- The Gradle plugin turns on Compose source information (`composeCompiler { includeSourceInformation }`) except in
  Xcode Release builds, for click-to-code and live literals. A value set in `composeCompiler { }` wins.
- Faster previews: only the previews of the open file render, up to 8 at once, each captured as soon as it stops
  changing (no more fixed 800 ms per preview). The host app stays running on the simulator between renders, is only
  compiled once per library version (adding a preview or changing the app's API no longer recompiles it), and is
  relinked and reinstalled only when the framework changed. Simulators of different `device`s render in parallel, and
  the IDE turns on Kotlin/Native incremental compilation. A re-render without a Kotlin change takes about a second,
  down from six or more.
- A preview that throws no longer stops the others: it shows its Kotlin exception. So do a
  `PreviewParameterProvider` that throws or has no values, and a preview that blocks the main thread (stopped after
  30 s). A preview that crashed renders first and alone next time, so it costs one host restart, not three.
- Previews render on a simulator of their own, *Compose SwiftUI Previews*, booted in the background, with one host app
  per module: modules and projects no longer replace each other's app.
- Previews named in backticks (``fun `Dark mode`()``) render.
- The IDE plugin needs IntelliJ IDEA / Android Studio 2026.2 (build 262) running on Java 25.
- IDE previews render on save again (0.6.2 made them on demand): each render now only costs what changed. Choose
  *Only when asked* in the settings to keep the 0.6.2 behavior.

## 0.7.1

### Added

- `Modifier.toolbarItem(placement, sharedBackgroundVisibility) { }`: SwiftUI's
  `ToolbarContent.sharedBackgroundVisibility(_:)` (iOS 26 / macOS 26, ignored on tvOS, watchOS and earlier systems).
  `Visibility.hidden` drops the Liquid Glass capsule behind the item.

## 0.7.0

### Added

- `TextEditor(StyledString, onTextChange, selection, onSelectionChange)`: SwiftUI's rich-text editor (iOS 26 /
  macOS 26). Formatting from ⌘B / ⌘I / ⌘U, the Format menu and its panel (fonts, sizes, colors, alignment, line
  height) comes back as a `StyledString`; attributes with no `SpanStyle` property are kept in
  `SpanStyle.nativeAttributes`.
- `RichTextSelection` (SwiftUI `AttributedTextSelection`: ranges and typing style) with `transformStyle`, `styles` and
  `replaceSelection`, the counterparts of `transformAttributes(in:)`, `attributes(in:)` and `replaceSelection`.
- `Modifier.attributedTextFormattingDefinition(AttributedTextFormattingDefinition(AttributedTextValueConstraint…))`.
- `SpanStyle.backgroundColor`, `tracking`, `link` (also drawn by `Text(StyledString)`) and `alignment`;
  `StyledString(text, style)`, `subSequence`, `replace`.
- macOS: ⌘B / ⌘I and the Format menu need `.commands { TextFormattingCommands() }` on the app's scene, as in SwiftUI.

### Fixed

- `Text(StyledString)`: an italic run with its own font or weight is drawn italic.

## 0.6.2

### Changed

- IDE plugin: previews render only on demand (the editor's *Render Previews* button, a preview's gutter icon, or
  ⌥⇧⌘P / Ctrl+Alt+Shift+P), no longer on every save or when a file opens. Each render links the framework and drives
  the simulator, which saturated the machine under IntelliJ's frequent autosaves.

## 0.6.1

### Added

- `NavigationLink(value = v) { … }` inside a Kotlin `NavigationStack(path, onPathChange, destination)` pushes `v`,
  with the native disclosure chevron.
- `Modifier.refreshable` and `Modifier.task` take `suspend () -> Unit`: the refresh indicator stays until the
  coroutine ends, and cancelling the SwiftUI task cancels it.
- `Tab(role = TabRole.search, badge = "3")`.
- Option sets combine with `+` / `union`: `Modifier.padding(Edge.Set.top + Edge.Set.leading, 8.0)` (also `Axis.Set`,
  `EventModifiers`, `SafeAreaRegions`, `AccessibilityTraits`…).
- `Text(value, NumberFormat.Percent, fractionDigits = 1..1)`: locale-aware number, percent and currency text.
- `EnvironmentReader` exposes the scene phase and the accessibility preferences (reduce motion, reduce
  transparency, differentiate without color, invert colors, VoiceOver, bold text).
- `HoverPhase.location`, `nativeAction { }` / `nativeCallback { }` for `NativeView` arguments.
- `WebViewController.evaluateJavaScript(script, onError = …)`: JavaScript errors no longer reach `onResult`.
- Gradle plugin: `composeSwiftUI { integrations.set(listOf("MapKit", "Charts")) }` writes only those integration
  files.
- `AnyLayout(if (wide) HStackLayout() else VStackLayout()) { … }`: switching layouts keeps the children's identity.
- `LazyForEach(items, key, onDelete = …, onMove = …)`.
- `rememberSaveable` (the runtime depends on `runtime-saveable`); a `LazyForEach` row keeps its saveable state while it
  scrolls off screen.
- TipKit: `Tip(invalidated = true)` invalidates the tip; `maxDisplayCount`, `maxDisplayDuration`,
  `ignoresDisplayFrequency` options.
- `PayWithApplePayButton`: `onAuthorize` and `merchantSession` are `suspend`, so the sheet waits for your server.
- `EnvironmentReader`: `env.actions` (`openURL`, `openWindow`, `dismissWindow`, `openSettings`, `dismissSearch`,
  `refresh`, `rename`), `isSearching`, `increasedContrast`, `supportsMultipleWindows`.
- `ScaledMetric(24.0) { size -> }` (`@ScaledMetric`), and tab customization: `TabView(customization, onCustomizationChange)`
  with `Tab(customizationId = …)` / `TabSection(customizationId = …)` (iPad sidebar, Mac).
- `nativeSuspendFunction { }`: a Kotlin `suspend` function a `NativeView` renderer awaits with `kotlinAwait`.

### Changed

- `EditButton()` and `TextEditor(text, onTextChange = …)` are composables (`EditButtonView(EditButton())` and
  `TextEditorView(…)` are gone).
- `AppServices.swift` and `SystemServices.swift` are split into one integration file per framework
  (`HealthKitUI.swift`, `FamilyControls.swift`, `AppIntents.swift`…), so an app links only what it uses.
- The Gradle plugin adds the library to `appleMain` when the module also targets Android or the JVM.
- `Modifier.glassEffect()`: the shape defaults to SwiftUI's `DefaultGlassEffectShape()`.

### Fixed

- `Canvas` paths stopped at `arc`, `relativeArc`, `tangentArc` and `roundedRect`.
- `Modifier.onContinuousHover { }` and `Modifier.hoverEffect()` were ambiguous without their first argument.
- `translationTask` ignored `preferredStrategy` and changes of `source` / `target`.
- `Map` replaced a camera, user-location or item position with a region after the first camera change.
- StoreKit: the redeem-code button visibility overrode restore purchases on macOS 14; subscription states arrive as
  `subscribed`, `expired`, `inBillingRetryPeriod`, `inGracePeriod` or `revoked`.
- `SpriteView` kept its first scene when the scene file or class changed.
- `healthDataAccessRequest(objectType)` couldn't resolve the vision prescription type.
- KDoc availability of `List(selection:)` on iOS, `SubscriptionOfferView`, PassKit, `familyActivityPicker` and
  `workoutPreview`.

## 0.6.0

### Added

- A full guide on the documentation site: 32 chapters (getting started, building screens, every Apple framework,
  platforms, interop, previews, performance, troubleshooting, limitations) whose 333 Kotlin examples compile
  (`scripts/check_docs.py`).
- Opt-in integrations for PhotosUI, WebKit, AVKit and QuickLook, AuthenticationServices, CoreLocationUI,
  LocalAuthentication, PassKit, MusicKit, Translation, SpriteKit, SceneKit, GameController, RealityKit, AppIntents,
  DeviceActivity, WatchKit, ManagedAppDistribution, PermissionKit, ScreenCaptureKit, WorkoutKit and DeviceDiscoveryUI
  (see the README table).
- TipKit (`TipView`, `popoverTip`, tip styles), WidgetKit and ActivityKit view modifiers (`widgetStyle`, `widgetLabel`,
  `activityStyle`, `controlWidgetText`, `AccessoryWidgetGroup`, `WidgetImage`), HealthKitUI, ContactsUI,
  FamilyControls, FinanceKitUI, ImagePlayground, JournalingSuggestions, GameSave, ClassKitUI, SecurityUI,
  AutomatedDeviceEnrollment, and the CoreLocation / CoreMotion anchors: the SwiftUI APIs these frameworks declare in
  their own module.
- Widgets written in Kotlin: `WidgetTimeline` from Kotlin, `ComposeTimelineProvider` and `ComposeWidgetView` in
  WidgetKit.swift (the extension keeps a few-line Swift `@main`).
- `spatialAnchors(nearbyInteractionSession = …)`: NearbyInteraction origin anchor with an `NISession` created in Kotlin.
- `recordingEditor` on iOS and tvOS devices, with the tvOS `mode`.
- Every entry of Apple's SwiftUI documentation index is now generated, bridged or classified in `docs/COVERAGE.md`
  (none left unmatched); visionOS-only APIs are listed as such.
- `List(selectedKey, onSelectedKeyChange) { }`: single selection on every platform. Rows are selected by their
  `Modifier.tag(key)`.
- A Frameworks section in the docs site sidebar, linking MapKit, Swift Charts, StoreKit and the other frameworks.

### Fixed

- Callbacks that report "no value" (dismissing a sheet, clearing a selection, a JavaScript call without result)
  delivered an `NSNull` object to Kotlin instead of `null`: `== null` checks never matched and `as String?` casts
  failed. The renderer now converts it to `null`.
- `List(selection, onSelectionChange)` did nothing on iOS: it now selects and highlights the tagged row.
- `renderComposeSwiftUIPreviews` failed to link apps whose Kotlin libraries use `sqlite3` (SQLDelight) or `zlib`:
  the preview host now links both.

## 0.5.0

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
- Protocols implemented in Kotlin: `PreferenceKey` with `preference`/`onPreferenceChange` and `anchorPreference`
  (`AnchorPreferenceReader`), `ContainerValueKey` with `containerValue` and `SubviewsReader`/`Subview` (SwiftUI's
  `Group(subviews:)`), `customAnimation { time, duration -> progress }`, `transition { stage -> Modifier }`,
  `keyframeAnimator`, `textRenderer`, `DropDelegate` (`onDrop`), `customHorizontalAlignment`/`customVerticalAlignment`,
  `customPresentationDetent`, `layoutValue`, `accessibilityRotor`, `accessibilityChartDescriptor` and
  `ExplicitTimelineSchedule`.
- Composed gestures: `Modifier.gesture(GestureSpec, priority)` with `simultaneously`, `sequenced` and `exclusively`.
- `ForEach(items, key, onDelete, onMove)` (swipe to delete and drag to reorder in a `List`), `withAnimation(…,
  completion)`, `TabSection`, `ShareLink(url)`/`ShareLink(text)` with a preview, `draggableUrl`, `dropDestinationUrls`,
  `fileExporter` with binary data, `fileMover`.
- Custom styles written in Kotlin for `GroupBox`, `LabeledContent`, `ControlGroup`, `Form`, `Menu` and `ProgressView`
  (`StyleContent`, `StyleProgressFill`).
- `TextField`/`TextEditor` with a text selection (`TextRange`), `Toggle(sources)` and `Picker(sources)` (mixed
  state), `AsyncImage(url, headers)`, `Slider` with ticks, `lineHeight`, `editMode`, `onOpenUrlAction`,
  `pointerImage`, `dialogIcon`, `ContentUnavailableSearch`, `ImagePaintStyle`, `onHingeChange`, `reorderContainer`,
  `dragContainer`, `onDragSessionUpdated`/`onDropSessionUpdated`, `dropConfiguration`, and the macOS commands
  (`copyable`, `cuttable`, `pasteDestination`, `onCopyCommand`, `onCutCommand`, `onPasteCommand`,
  `exportableToServices`, `importableFromServices`, `onModifierKeysChanged`), plus `onPencilDoubleTap`.
- Shape operations (`offset`, `rotation`, `scale`, `trim`, `transform`, `size`, `union`, `intersection`, `subtracting`,
  `symmetricDifference`, `lineIntersection`, `lineSubtraction`, `inset`, `strokedShape`), the full `Path` API in
  `PathBuilder` (arcs, tangent arcs, rounded rectangles, lines, rectangles, nested paths), `Gradient.colorSpace` and
  `Text(text, variant)`.
- Overloads that Kotlin can't tell apart (same parameter types, different labels) are no longer dropped: they are named
  after their first label (`ConcentricRectangle.uniformBottomCorners(…)`, `Shape.rectUniformLeadingCorners(…)`,
  `Font.customName(_, fixedSize)`, `AccessibilityActionCategory.name(…)`…).
- MapKit (`ComposeIntegrations.registerMapKit()`): `Map` with Kotlin-controlled position, bounds, interaction modes,
  marker and point-of-interest selection; `Marker`, `Annotation`, `MapCircle`, `MapPolyline`, `MapPolygon`,
  `UserAnnotation`; map controls; `MapReader`; `LookAroundPreview` and `lookAroundViewer`; map item detail sheet and
  popover; `mapStyle`, `onMapCameraChange`, camera keyframes.
- Swift Charts (`ComposeIntegrations.registerCharts()`): every mark with String, number or date values; vectorized and
  function plots; 3D charts and surfaces; styles by value, annotations, masks; axes and axis marks with Kotlin labels;
  scales, legend, selection, scrolling; `ChartProxy` for overlays, backgrounds and gestures.
- StoreKit (`ComposeIntegrations.registerStoreKit()`): `ProductView`, `StoreView`, `SubscriptionStoreView` with option
  groups, sections and period sets, `SubscriptionOfferView`; store styles and buttons; subscription, offer code, refund
  and App Store overlay sheets; purchase callbacks and options; introductory, promotional and preferred offers; product,
  entitlement and subscription status tasks; `requestReview` and `purchase`.
- Every entry of Apple's SwiftUI documentation that builds UI is now generated or hand-written;
  [docs/COVERAGE.md](docs/COVERAGE.md) gives the reason for each one that doesn't apply to Kotlin.

### Changed

- A trailing lambda now binds the main closure, as in Swift: `Slider(value, bounds) { value = it }` and
  `Stepper(…) { }` pass it to `onValueChange` (it used to go to `onEditingChanged`), and optional closures such as
  `onPressingChanged` come before `perform`.
- `NewDocumentButtonSource` (deprecated alias) is now `NewDocumentButtonContentType`.
- The Swift renderer has four new files, `Interactive.swift`, `Advanced.swift`, `Completions.swift` and
  `Generated/GenEncoders.swift`. If you added the `ComposeSwiftUI` folder to Xcode as a group, add them to the app
  target once.
- The integrations are opt-in: add `swift/ComposeSwiftUIIntegrations/MapKit.swift`, `Charts.swift` or `StoreKit.swift`
  to the app and call its `register…()` once.

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
