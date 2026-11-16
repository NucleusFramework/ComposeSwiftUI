<!-- group: Platforms and tools -->
# Limitations

> What Compose SwiftUI doesn't do, why, and what to use instead: visionOS, Swift entry points, Swift-typed APIs, and the SwiftUI entries marked not applicable.

Compose SwiftUI covers the SwiftUI API that builds views: every entry of Apple's SwiftUI documentation is either
generated, covered by an equivalent overload, or classified as not applicable with a reason in
[COVERAGE.md](https://github.com/NucleusFramework/ComposeSwiftUI/blob/main/docs/COVERAGE.md). This chapter explains
the boundaries.

## The numbers

| Status in COVERAGE.md | Entries |
|---|---|
| Generated and compiler-verified | 3,798 (46%) |
| Covered by an equivalent overload or a hand-written API | 1,201 (15%) |
| Not applicable | 3,221 (39%) |
| Rejected, skipped or unmatched | 0 |

"Not applicable" sounds large, but most of it is not UI you could build: read-only properties of values SwiftUI hands
you (754), type aliases and associated types (221), result-builder plumbing (104), protocol requirements that only
make sense for a Swift type (342), and 550 visionOS-only entries. The sections below cover the categories that matter
when you write an app.

## visionOS

visionOS is not supported, because Kotlin/Native has no visionOS target: there is no way to compile your Kotlin
framework for it. The 550 visionOS-only SwiftUI entries (immersive spaces, ornaments, volumes, spatial gestures…) are
listed as such in COVERAGE.md.

Integration files contain `os(visionOS)` branches because Apple's frameworks declare them, but they are never
compiled into a Compose SwiftUI app. If Kotlin/Native gains a visionOS target, the generator can pick these APIs up.

## Swift entry points stay in Swift

Some SwiftUI declarations are entry points that the system finds in your Swift code at compile time. They can't come
from a Kotlin framework, so they stay in Swift, and host Compose SwiftUI content:

| Declaration | What you write in Swift | What comes from Kotlin |
|---|---|---|
| `@main struct App`, `WindowGroup`, `Window`, `Settings`, `MenuBarExtra` | The app and its scenes | Each scene's content: `ComposeView { … }` |
| Scene modifiers and `commands` (`windowStyle`, `defaultSize`, `CommandMenu`…) | On the scene, in Swift | The views and actions they show |
| `DocumentGroup`, `FileDocument` | The document app and its document type | The editor view |
| `Widget`, `WidgetBundle`, `StaticConfiguration` | A few-line `@main` in the extension | The timeline (`WidgetTimeline`) and the views, through `ComposeTimelineProvider` and `ComposeWidgetView` |
| `ControlWidget` configuration and templates | The control, in the extension | Widget-side modifiers such as `controlWidgetText` |
| `UIHostingController`, `NSHostingView`, app delegate adaptors | UIKit / AppKit hosting | Whatever `ComposeView` shows |
| `#Preview` | Nothing | `@Preview` on a Kotlin composable; the plugin generates the `#Preview` |

A widget extension, for example, keeps this much Swift:

```swift
@main struct WeatherWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "weather", provider: ComposeTimelineProvider { family in WeatherKt.timeline(family: family) }) { entry in
            ComposeWidgetView(entry) { input, family in WeatherKt.widget(input: input, family: family) }
        }
    }
}
```

See [Tips and widgets](tips-and-widgets.md).

## Swift-typed APIs

Some APIs take values that only exist as Swift types: a type conforming to a Swift protocol, a Swift macro's output,
or a key path. Kotlin can't create them.

- **App Intents.** `AppIntent`, `AppEntity` and the APIs that take an intent value (`Button(intent:)`,
  `Toggle(isOn:intent:)`, `accessibilityAction(intent:)`) need Swift types compiled into the app or extension. `ShortcutsLink` and the App Intents
  views that don't take an intent are available (`registerAppIntents()`). Declare intents in Swift and call your
  Kotlin code from their `perform()`.
- **SwiftData.** `@Model`, `@Query` and `modelContainer` are Swift macros and Swift persistence. Persist with a Kotlin
  library (SQLDelight, Room, DataStore) and expose the data as state.
- **Observation and Combine.** `ObservableObject`, `@Observable`, `@Published` and `onReceive(_ publisher:)` are
  replaced by Kotlin state: `StateFlow` with `collectAsState()`, `LaunchedEffect` to collect flows.
- **Property wrappers.** `@State`, `@Binding`, `@Environment`, `@FocusState`, `@Namespace`, `@AppStorage`,
  `@SceneStorage`, `@FocusedValue`: Kotlin state replaces them (`remember`, hoisted values and callbacks,
  `EnvironmentReader`, `Modifier.focused`, `NamespaceScope`, your own storage for persisted values).
- **Key paths.** `ForEach(data, id: \.id)`, `Table` columns by key path, `environment(\.key, value)`: Kotlin lambdas
  replace them (`key = { it.id }`, `column("Name") { Text(it.name) }`, `Modifier.environment(name = value)` for the 60
  settable environment values).
- **Swift data types.** `FileDocument`, `Transferable` types other than text and URLs, `NSUserActivity` (Handoff),
  `AttributedString` attributes and rich-text selection. `AttributedString` parameters take a plain `String`; use
  `Text(buildStyledString { … })` for mixed styles. Drag and drop, sharing and file export carry text, URLs or bytes.
- **StoreKit pricing values.** Products are identified by their App Store Connect ids, and purchases, transactions and
  subscription statuses arrive as Kotlin data classes; a few values that only exist as `Product.SubscriptionInfo`
  members (pricing terms) have no Kotlin counterpart.

When you need one of these, write the piece in Swift and bring it into the Kotlin screen with a `NativeView`, or pass
values in with `ComposeView(input:)`. See [Interop](interop.md).

## Protocols you implement

In SwiftUI you conform your own types to `View`, `ViewModifier`, `Layout`, `Shape`, `ButtonStyle`, `PreferenceKey` and
so on. Kotlin can't conform to Swift protocols, so each one has a Kotlin equivalent:

| Swift protocol | Kotlin equivalent |
|---|---|
| `View` | A `@Composable` function |
| `ViewModifier` | A function returning a `Modifier` chain |
| `Layout` | `CustomLayout(policy)` with a `LayoutPolicy` |
| `Shape` | `PathShape { … }` and the shape operations |
| `ButtonStyle`, `ToggleStyle`, `LabelStyle` and other styles | `Modifier.buttonStyle { isPressed -> }` and the other style lambdas |
| `PreferenceKey`, `ContainerValueKey`, `LayoutValueKey` | `preference` / `onPreferenceChange`, `containerValue`, `layoutValue` |
| `Transition`, `CustomAnimation`, `TextRenderer`, `DropDelegate` | `Modifier.transition { stage -> }`, `customAnimation`, `textRenderer`, `onDrop` with a Kotlin delegate |

These are hand-written, so they cover what views need rather than every protocol requirement: a Kotlin layout
measures subviews at their ideal size, for example. See [Kotlin APIs](/kotlin-apis.html).

## Data-driven containers take Kotlin lists

`List(data)`, `ForEach(data)`, `Table(rows)`, `OutlineGroup(data, children:)` and `searchable(tokens:)` take Swift
collections and key paths. In Kotlin, they take Kotlin lists and lambdas: a loop or `LazyForEach` for `List` and
`ForEach`, and the hand-written `Table(rows, key)`, `OutlineGroup(items, key, children)` and
`Modifier.searchable(…, tokens, …)`. `Table` needs iOS 17.4 or macOS 14.4.

## Smaller gaps

- **Integers.** Swift's fixed-width integers (`Int8` … `UInt64`) are Kotlin `Int`, truncated on the Swift side.
- **Values SwiftUI hands to closures** (`GeometryProxy`, `ScrollViewProxy`, `ChartProxy`, layout and style
  configurations) are exposed through hand-written APIs that pass what views need (a size, a `scrollTo`, a value at a
  position), not the full Swift type.
- **Environment reads.** `EnvironmentReader` exposes the commonly read values (color scheme, Dynamic Type size, size
  classes, locale, layout direction, enabled and presented state, `dismiss`, scene phase, accessibility preferences),
  and they arrive after the first layout pass.

## Platform and tooling constraints

- **Apple only.** Compose SwiftUI renders SwiftUI, so its UI code runs on iOS, macOS, tvOS and watchOS. Share view
  models and logic with Android; write the Android UI with Compose UI, or use Compose Multiplatform where one UI for
  every platform matters more than native views. See [Introduction](introduction.md).
- **Xcode 27.2 to build.** The renderer references the newest SwiftUI APIs, so an older Xcode can't compile it. The
  deployment target stays at iOS 17, macOS 14, tvOS 17 and watchOS 10.
- **Performance.** Mounting costs 8% to 18% more than plain SwiftUI and scrolled-in rows cost more to create; long
  lists need `LazyForEach`. See [Performance](performance.md).
- **Pixel parity.** On iOS, 2 of the 50 benchmark screens differ from plain SwiftUI by less than a pixel (one SF
  Symbol, and a search field placeholder after updates).
- **IDE previews** render on the iOS simulator only, need Xcode and macOS, and the IDE plugin is installed from disk
  for now (it isn't on the JetBrains Marketplace yet). Xcode previews work for every platform. See
  [Previews](previews.md).
- **Kotlin/Native build times.** A Kotlin change relinks the framework before Xcode can run the app, as in any KMP
  app; previews reuse the compiled renderer, so after the first run they only cost the link.

## When something is missing

If an API you need isn't in the [reference](/reference.html), look it up in COVERAGE.md: the reason column says
whether it is visionOS-only, Swift-only, covered by another overload, or replaced by a Kotlin API. For anything Swift
can do and Kotlin can't express, a `NativeView` with a small Swift factory bridges the gap without leaving the Kotlin
screen.
