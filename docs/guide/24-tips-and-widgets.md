<!-- group: Apple frameworks -->
# Tips and widgets

> Show TipKit tips from Kotlin, style widget and Live Activity content, and write home screen widgets whose timeline and views come from Kotlin.

Two integration files cover this chapter: `TipKit.swift` for TipKit and `WidgetKit.swift` for the WidgetKit and
ActivityKit view APIs, plus the two Swift types that let a widget extension take its timeline and views from Kotlin.

| Kotlin API | Framework | Availability |
|---|---|---|
| `Tip`, `TipView`, `Modifier.tipStyle` | TipKit | iOS 17, macOS 14, tvOS 17, watchOS 10 |
| `Modifier.popoverTip` | TipKit | iOS 17, macOS 14, tvOS 18 (not watchOS) |
| `Modifier.tipAnchor`, `TipView(isPresented, anchorId)` | TipKit | iOS 26, macOS 26, tvOS 26, watchOS 26 |
| `Modifier.widgetStyle`, `AccessoryWidgetBackground` | WidgetKit | iOS, macOS, watchOS |
| `Modifier.widgetLabel` | WidgetKit | iOS, watchOS |
| `Modifier.activityStyle` | ActivityKit | iOS |
| `Modifier.controlWidgetText` | WidgetKit | iOS 18, macOS 26, watchOS 26 |
| `WidgetImage` | WidgetKit | iOS 18, macOS 15, watchOS 11 |
| `AccessoryWidgetGroup` | WidgetKit | watchOS 11 |
| `WidgetTimeline` + `ComposeTimelineProvider` + `ComposeWidgetView` | WidgetKit | iOS, macOS, watchOS |

## TipKit

### Setup

`registerTipKit` registers the renderers and calls `Tips.configure` with the options you pass. Call it once at launch:

```swift
import TipKit

@main
struct MyApp: App {
    init() {
        #if DEBUG
        try? Tips.resetDatastore()       // forget which tips were shown; must run before Tips.configure
        // Tips.showAllTipsForTesting()  // ignore display rules while developing
        #endif
        ComposeIntegrations.registerTipKit([.displayFrequency(.immediate), .datastoreLocation(.applicationDefault)])
    }
    var body: some Scene { WindowGroup { ComposeView { MainKt.app() } } }
}
```

TipKit stores each tip's status (shown, closed by the user, closed by an action) in its datastore, keyed by the tip's
`id`, and persists it across launches. `resetDatastore()` and `showAllTipsForTesting()` are Swift-only calls; make them
before `registerTipKit` (they must precede `Tips.configure`). Display frequency and the datastore location are the
`Tips.ConfigurationOption`s you pass.

### Defining tips

A Kotlin `Tip` is a plain value: an id, a title, an optional message and SF Symbol, and actions:

```kotlin
object AppTips {
    val favorite = Tip(
        id = "favorite-tip",
        title = "Save as favorite",
        message = "Tap the star to find this recipe again quickly.",
        systemImage = "star",
    )

    val sync = Tip(
        id = "sync-tip",
        title = "Sync across devices",
        message = "Turn on iCloud sync in Settings.",
        systemImage = "icloud",
        actions = listOf(TipAction(id = "open-settings", title = "Open Settings")),
    )
}
```

Keep the `id` stable: changing it makes TipKit treat the tip as new and show it again.

### Inline tips and popovers

`TipView` shows the tip inline, `Modifier.popoverTip` in a popover attached to a view. `onAction` receives the id of the
tapped `TipAction`:

```kotlin
@Composable
fun RecipeScreen(onOpenSettings: () -> Unit) {
    List {
        TipView(AppTips.sync, arrowEdge = Edge.bottom, onAction = { id -> if (id == "open-settings") onOpenSettings() })
        HStack {
            Text("Carbonara")
            Spacer()
            Image(systemName = SFSymbol("star"), modifier = Modifier.popoverTip(AppTips.favorite, arrowEdge = Edge.top))
        }
    }
}
```

The `X` button closes the tip for good: TipKit records it and does not show it again.

### Controlling when a tip shows

TipKit's rules, parameters and events (`#Rule`, `@Parameter`, `Tips.Event`) are Swift macros and types and are not
exposed to Kotlin. Instead, from iOS 26 / macOS 26, decide in Kotlin with `isPresented`:

```kotlin
@Composable
fun FavoriteButton(viewCount: Int, onToggle: () -> Unit) {
    var tipShown by remember { mutableStateOf(true) }
    Button(
        modifier = Modifier.popoverTip(
            AppTips.favorite,
            isPresented = viewCount >= 3 && tipShown,
            onPresentedChange = { tipShown = it },
        ),
        action = onToggle,
    ) {
        Image(systemName = SFSymbol("star"))
    }
}
```

`isPresented = null` (the default) leaves the decision to TipKit. On earlier OS versions `isPresented` is ignored and the
tip follows TipKit's own state. A tip that needs real rules (show after the third launch, after an event) is best
declared in Swift and shown with a Swift view.

### Anchors (iOS 26)

`Modifier.tipAnchor(id)` marks a view; a `TipView` with the same `anchorId` points its arrow at it:

```kotlin
VStack {
    TipView(AppTips.favorite, anchorId = "star-button")
    Image(systemName = SFSymbol("star"), modifier = Modifier.tipAnchor("star-button"))
}
```

### Styling tips

`Modifier.tipStyle` groups the TipKit style modifiers:

```kotlin
TipView(
    AppTips.sync,
    modifier = Modifier.tipStyle(
        background = Color.blue.opacity(0.1),
        cornerRadius = 16.0,
        imageSize = CGSize(32.0, 32.0),
        imageStyles = listOf(Color.blue),
        miniTip = true,
    ),
)
```

| Parameter | SwiftUI modifier |
|---|---|
| `background` | `tipBackground(_:)` |
| `cornerRadius`, `antialiased` | `tipCornerRadius(_:antialiased:)` |
| `imageSize` | `tipImageSize(_:)` |
| `imageStyles` (one to three) | `tipImageStyle(_:)`, `tipImageStyle(_:_:)`, `tipImageStyle(_:_:_:)` (iOS 18) |
| `backgroundInteraction` | `tipBackgroundInteraction(_:)` (iOS 26, not watchOS) |
| `miniTip = true` | `tipViewStyle(.miniTip)` |

## Widget and Live Activity modifiers

These modifiers style Compose SwiftUI content that is displayed in a widget, a watch complication or a Live Activity.
Register them in the target that renders that content (usually the widget extension) with
`ComposeIntegrations.registerWidgetKit()`.

```kotlin
@Composable
fun StepsComplication(steps: Int, goal: Int) {
    ZStack(
        modifier = Modifier
            .widgetLabel("${steps * 100 / goal}% of goal")
            .widgetStyle(url = "myapp://activity", accentable = true),
    ) {
        AccessoryWidgetBackground()
        Gauge(value = steps.toDouble(), bounds = 0.0..goal.toDouble(), modifier = Modifier.padding(4.0)) {
            Text("Steps")
        }
    }
}
```

| Kotlin | SwiftUI |
|---|---|
| `widgetStyle(accentable)` | `widgetAccentable(_:)` |
| `widgetStyle(url)` | `widgetURL(_:)`: the URL the app opens with when the widget is tapped |
| `widgetStyle(curvesContent)` | `widgetCurvesContent(_:)` (iOS 17, watchOS 10) |
| `widgetStyle(containerBackground)` | `containerBackground(_:for: .widget)` (required on iOS 17+ for removable backgrounds) |
| `widgetStyle(accessoryGroupStyle)` | `accessoryWidgetGroupStyle(_:)` (watchOS 11) |
| `widgetLabel(text)`, `widgetLabel { … }` | `widgetLabel(_:)`, `widgetLabel(label:)` (iOS, watchOS accessory widgets) |
| `activityStyle(backgroundTint, systemActionForegroundColor)` | `activityBackgroundTint(_:)`, `activitySystemActionForegroundColor(_:)` |
| `activityStyle(belowIfTooWide = true)` | `dynamicIsland(verticalPlacement: .belowIfTooWide)` |
| `controlWidgetText(status, actionHint)` | `controlWidgetStatus(_:)`, `controlWidgetActionHint(_:)` |

`WidgetImage` is an `Image` with `widgetAccentedRenderingMode`, for images that should keep their colors (or be
desaturated) on tinted home screens; `AccessoryWidgetGroup` is the watchOS 11 grouped accessory layout:

```kotlin
VStack {
    WidgetImage(name = "AlbumArt", renderingMode = WidgetAccentedRenderingMode.fullColor)
    WidgetImage(systemName = "cloud.sun.fill", renderingMode = WidgetAccentedRenderingMode.accented)
}
```

## Widgets in Kotlin

A widget extension must have a Swift `@main` `Widget` (WidgetKit requires it), but the timeline and the views can be
Kotlin. The library provides three pieces:

- Kotlin `WidgetTimeline` / `WidgetTimelineEntry`: the entries (a date, a `String` input and an optional relevance) and
  the reload policy.
- Swift `ComposeTimelineProvider`: a `TimelineProvider` that calls your Kotlin function with the widget family
  (`"systemSmall"`, `"accessoryCircular"`…) and turns the result into a WidgetKit `Timeline`. The placeholder and the
  snapshot use its first entry.
- Swift `ComposeWidgetView`: renders an entry with a Kotlin `composeSwiftUI { }` host, given the entry's input and the
  family.

### The Kotlin side

Put this in the shared module (for example `Weather.kt`, so Swift sees `WeatherKt`):

```kotlin
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

data class Forecast(val temperature: Int, val condition: String, val symbol: String) {
    fun encode() = "$temperature|$condition|$symbol"

    companion object {
        fun decode(input: String): Forecast? = input.split('|').takeIf { it.size == 3 }?.let {
            Forecast(it[0].toIntOrNull() ?: 0, it[1], it[2])
        }
    }
}

fun timeline(family: String): WidgetTimeline {
    val now = NSDate().timeIntervalSince1970
    // Read cached data here (an App Group container or shared UserDefaults); widgets can't wait on the network.
    val forecasts = listOf(Forecast(21, "Sunny", "sun.max.fill"), Forecast(19, "Cloudy", "cloud.fill"), Forecast(17, "Rain", "cloud.rain.fill"))
    val entries = forecasts.mapIndexed { hour, forecast -> WidgetTimelineEntry(date = now + hour * 3600.0, input = forecast.encode()) }
    return WidgetTimeline(entries, reloadAfter = now + 3 * 3600.0)
}

fun widget(input: String, family: String) = composeSwiftUI {
    val forecast = Forecast.decode(input)
    VStack(
        alignment = HorizontalAlignment.leading,
        spacing = 4.0,
        modifier = Modifier.widgetStyle(containerBackground = Color.blue, url = "myapp://forecast"),
    ) {
        if (forecast == null) {
            Text("No data")
        } else {
            Image(systemName = SFSymbol(forecast.symbol), modifier = Modifier.font(Font.title))
            Text("${forecast.temperature}°", modifier = Modifier.font(Font.largeTitle))
            if (family != "systemSmall") Text(forecast.condition)
        }
    }
}
```

`WidgetTimeline(entries, reloadAfter, never)` maps to WidgetKit's reload policies: `reloadAfter` (epoch seconds) is
`.after(date)`, `never = true` is `.never`, and neither is `.atEnd`. Dates are epoch seconds throughout.

The input is a `String`: encode what the view needs (a few fields, or JSON with kotlinx.serialization) rather than
passing objects. Each entry is rendered from its input alone.

### The Swift side

The widget extension target needs the Kotlin framework (embedded like in the app), the `ComposeSwiftUI` renderer folder
and `ComposeSwiftUIIntegrations/WidgetKit.swift` as members:

```swift
import SwiftUI
import WidgetKit
import Shared

struct WeatherWidget: Widget {
    var body: some WidgetConfiguration {
        StaticConfiguration(kind: "weather", provider: ComposeTimelineProvider { family in WeatherKt.timeline(family: family) }) { entry in
            ComposeWidgetView(entry) { input, family in WeatherKt.widget(input: input, family: family) }
        }
        .configurationDisplayName("Weather")
        .description("The forecast for the next hours.")
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryRectangular])
    }
}

@main
struct WeatherWidgets: WidgetBundle {
    init() { ComposeIntegrations.registerWidgetKit() }
    var body: some Widget { WeatherWidget() }
}
```

From the app, ask WidgetKit to refresh after your data changes with `WidgetCenter.shared.reloadTimelines(ofKind:)`
in Swift: WidgetKit is a Swift-only framework, not reachable from Kotlin/Native.

### What to keep in mind

- A widget is rendered once per entry and archived: the composition must produce its final content synchronously from
  the input. `LaunchedEffect`, network calls, animations and timers don't update a widget. For a live clock or countdown,
  use `Text(date, style = Text.DateStyle.timer)`, which the system updates.
- Data shared between the app and the extension goes through an App Group (a shared container or `UserDefaults(suiteName:)`);
  the extension is a separate process.
- Interactive widgets (`Button(intent:)`, `Toggle(intent:)`) take an `AppIntent`, a Swift type: keep those controls in
  Swift. Taps on Kotlin content open the app through `widgetStyle(url = …)`; handle the URL with `onOpenURL` in the app.
- Widget memory is limited (about 30 MB): keep the timeline small.

### What stays in Swift

- The `@main` `Widget` / `WidgetBundle` and the `WidgetConfiguration` modifiers (`supportedFamilies`,
  `configurationDisplayName`, `contentMarginsDisabled`…).
- Configurable widgets (`AppIntentConfiguration`) and control widgets (`ControlWidget`, `ControlWidgetButton`), which are
  built on App Intents. A control's view can still use `Modifier.controlWidgetText`.
- Live Activities: the `ActivityAttributes` type, `ActivityConfiguration` and the Dynamic Island regions are Swift, as
  is starting, updating and ending an activity with `Activity.request(…)`. The views inside can be Kotlin, with
  `ComposeView(input:)` and `Modifier.activityStyle` for the lock screen presentation.
