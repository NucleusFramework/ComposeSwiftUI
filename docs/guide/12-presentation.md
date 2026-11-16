<!-- group: Building screens -->
# Presentation

> Present sheets, full-screen covers, popovers, inspectors, alerts, confirmation dialogs and file pickers from Kotlin state, and dismiss them cleanly.

Every SwiftUI presentation is a modifier bound to state: SwiftUI shows the sheet while a `Binding<Bool>` is true, or
while an optional item is non-nil, and writes back `false`/`nil` when the user dismisses it. In Compose SwiftUI the
binding becomes a value plus a callback, so presentation state is ordinary Kotlin state that you can keep in a
composable or a view model.

| SwiftUI | Compose SwiftUI |
|---|---|
| `.sheet(isPresented: $show) { … }` | `Modifier.sheet(isPresented = show, onIsPresentedChange = { show = it }) { … }` |
| `.sheet(item: $item) { item in … }` | `Modifier.sheet(item, onDismiss = { item = null }) { item -> }` |
| `.fullScreenCover(…)`, `.popover(…)` | Same two forms |
| `.alert("Title", isPresented: $show) { … } message: { … }` | `Modifier.alert("Title", show, { show = it }, actions = { … }, message = { … })` |
| `.alert(_:isPresented:presenting:)` | `Modifier.alert(title = { … }, item, onDismiss, actions = { }, message = { })` |
| `.presentationDetents([.medium, .large], selection: $d)` | `Modifier.presentationDetents(listOf(…), d) { d = it }` |
| `@Environment(\.dismiss)` | `EnvironmentReader { env -> env.dismiss() }`, or set your state to false |

The modifier goes on the presenting view, usually the screen's root (a `List`, a `VStack`), not on the button that
opens it.

## Sheets

The Boolean form is enough when the sheet needs no input:

```kotlin
@Composable
fun ProfileScreen() {
    var showSettings by remember { mutableStateOf(false) }
    VStack(
        modifier = Modifier.sheet(isPresented = showSettings, onIsPresentedChange = { showSettings = it }) {
            NavigationStack {
                Text("Settings", modifier = Modifier.navigationTitle("Settings").toolbarItem(ToolbarItemPlacement.confirmationAction) {
                    Button("Done") { showSettings = false }
                })
            }
        },
    ) {
        Button("Settings") { showSettings = true }
    }
}
```

Closing is just `showSettings = false`. When the user swipes the sheet down, SwiftUI calls `onIsPresentedChange(false)`
and your state follows. The optional `onDismiss` parameter runs after the sheet is gone, whatever closed it.

### Presenting an item

When the sheet shows one of many things, present an item. The sheet is up while the item is non-null, and the content
receives the non-null value, so there is no `!!` and no stale Boolean:

```kotlin
data class Photo(val id: String, val title: String)

@Composable
fun Gallery(photos: List<Photo>) {
    var opened by remember { mutableStateOf<Photo?>(null) }
    List(modifier = Modifier.sheet(item = opened, onDismiss = { opened = null }) { photo ->
        Text(photo.title, modifier = Modifier.font(Font.largeTitle))
    }) {
        for (p in photos) key(p.id) {
            Button(p.title) { opened = p }
        }
    }
}
```

Setting another item while a sheet is up replaces its content. The item forms exist for `fullScreenCover`, `popover`,
`alert`, `confirmationDialog` and `navigationDestination` too.

## Sheet size and behavior

Presentation modifiers go on the sheet's content, not on the presenting view:

```kotlin
var show by remember { mutableStateOf(false) }
Button("Filters", modifier = Modifier.sheet(isPresented = show, onIsPresentedChange = { show = it }) {
    Form(modifier = Modifier.presentationDetents(setOf(PresentationDetent.medium)).presentationDragIndicator(Visibility.visible)) {
        Toggle("Only favorites", isOn = true, onIsOnChange = {})
    }
}) { show = true }
```

The modifiers you will use most:

| Modifier | Effect |
|---|---|
| `presentationDetents(setOf(PresentationDetent.medium, PresentationDetent.large))` | Heights the sheet can rest at (iOS, iPadOS compact) |
| `presentationDetents(detents, selection, onSelectionChange)` | Same, with the current detent in Kotlin state |
| `presentationDragIndicator(Visibility.visible)` | Shows the grabber |
| `presentationBackgroundInteraction(PresentationBackgroundInteraction.enabled(upThrough = PresentationDetent.medium))` | Keeps the view behind interactive (maps-style sheets) |
| `presentationBackground(Material.thin)` or `presentationBackground { … }` | Sheet background |
| `presentationCornerRadius(24.0)` | Corner radius |
| `presentationCompactAdaptation(PresentationAdaptation.popover)` | How a popover or sheet adapts on iPhone |
| `presentationSizing(PresentationSizing.form)` | Sheet size on iPad and Mac (iOS 18, macOS 15) |
| `interactiveDismissDisabled()` | Blocks swipe-to-dismiss (unsaved changes) |

A sheet with selectable detents, the current one held in Kotlin:

```kotlin
@Composable
fun DetentSheet() {
    val detents = listOf(PresentationDetent.fraction(0.25), PresentationDetent.medium, PresentationDetent.large)
    var detent by remember { mutableStateOf(detents[1]) }
    var show by remember { mutableStateOf(true) }
    Text("Map", modifier = Modifier.sheet(isPresented = show, onIsPresentedChange = { show = it }) {
        VStack(
            modifier = Modifier
                .presentationDetents(detents, detent) { detent = it }
                .presentationDragIndicator(Visibility.visible)
                .presentationBackgroundInteraction(PresentationBackgroundInteraction.enabled(upThrough = PresentationDetent.medium))
                .interactiveDismissDisabled(),
        ) {
            Text("Results")
            Button("Expand") { detent = PresentationDetent.large }
        }
    })
}
```

`PresentationDetent.height(300.0)` and `PresentationDetent.fraction(0.4)` cover most needs. For a height computed from
the available space, `customPresentationDetent(index) { maxHeight -> maxHeight * 0.3 }` is the Kotlin version of a
`CustomPresentationDetent` (up to four per app, told apart by `index`).

## Full-screen covers

`fullScreenCover` takes the whole screen and has no swipe to dismiss, so always give the user a way out:

```kotlin
var onboarding by remember { mutableStateOf(true) }
Text("Home", modifier = Modifier.fullScreenCover(isPresented = onboarding, onIsPresentedChange = { onboarding = it }) {
    VStack(spacing = 20.0) {
        Text("Welcome", modifier = Modifier.font(Font.largeTitle))
        Button("Get started") { onboarding = false }
    }
})
```

It exists on iOS, tvOS and watchOS. On macOS it is a no-op: use a `sheet` there (an `expect`/`actual` helper or a
`Modifier` chosen per platform keeps one call site).

## Popovers

On iPad and Mac a popover is a bubble anchored to its view; on iPhone SwiftUI shows it as a sheet unless you ask for a
popover with `presentationCompactAdaptation`:

```kotlin
var showInfo by remember { mutableStateOf(false) }
Button(
    "Info",
    systemImage = SFSymbol.infoCircle,
    modifier = Modifier.popover(isPresented = showInfo, onIsPresentedChange = { showInfo = it }, arrowEdge = Edge.bottom) {
        Text("Synced 2 minutes ago", modifier = Modifier.padding().presentationCompactAdaptation(PresentationAdaptation.popover))
    },
) { showInfo = true }
```

Unlike a sheet, a popover attaches to the view it modifies, so put it on the button. Popovers are iOS and macOS only.

## Alerts

An alert has a title, buttons (`actions`) and an optional `message`. Buttons dismiss the alert when tapped; give
destructive and cancel buttons their `role` so SwiftUI styles and orders them:

```kotlin
@Composable
fun DeleteButton(onDelete: () -> Unit) {
    var confirm by remember { mutableStateOf(false) }
    Button(
        "Delete account",
        role = ButtonRole.destructive,
        modifier = Modifier.alert(
            "Delete your account?",
            isPresented = confirm,
            onIsPresentedChange = { confirm = it },
            actions = {
                Button("Delete", role = ButtonRole.destructive, action = onDelete)
                Button("Cancel", role = ButtonRole.cancel) {}
            },
            message = { Text("This cannot be undone.") },
        ),
    ) { confirm = true }
}
```

With only one action (or none: SwiftUI adds an OK button), use the overload without `message`. An alert can also hold a
`TextField` in its actions for a quick input (iOS, macOS).

### Alerts for an item

Errors are the typical case: the alert is up while the error is non-null, and the title and message come from it:

```kotlin
data class UiError(val title: String, val detail: String)

@Composable
fun ErrorHost(error: UiError?, onDismiss: () -> Unit) {
    Text(
        "Content",
        modifier = Modifier.alert(
            title = { it.title },
            item = error,
            onDismiss = onDismiss,
            actions = { Button("OK") {} },
            message = { Text(it.detail) },
        ),
    )
}
```

This fits a view model nicely: expose `error: StateFlow<UiError?>`, collect it, and clear it in `onDismiss`.

## Confirmation dialogs

A confirmation dialog is an action sheet on iPhone, a popover on iPad and a sheet-style dialog on macOS:

```kotlin
var choosing by remember { mutableStateOf(false) }
Button(
    "Share",
    modifier = Modifier.confirmationDialog(
        "Share photo",
        isPresented = choosing,
        onIsPresentedChange = { choosing = it },
        titleVisibility = Visibility.visible,
        actions = {
            Button("Copy link") {}
            Button("Save to Files") {}
            Button("Delete", role = ButtonRole.destructive) {}
        },
        message = { Text("Choose what to do with this photo.") },
    ),
) { choosing = true }
```

The item form `confirmationDialog(title = { … }, item, onDismiss, actions = { item -> })` works like the alert one.
`Modifier.dialogIcon(SFSymbol.…)` sets the dialog icon and `Modifier.dialogSeverity(DialogSeverity.critical)` marks a
critical dialog (both shown on macOS), and `dialogSuppressionToggle(isSuppressed, onIsSuppressedChange)` adds a "Do not
show again" checkbox (macOS).

## Inspectors

`Modifier.inspector` shows a trailing panel (iOS 17, macOS 14): a column next to the content on iPad and Mac, a sheet
on iPhone. Size it with `inspectorColumnWidth` on the inspector content:

```kotlin
var showInspector by remember { mutableStateOf(true) }
Text(
    "Document",
    modifier = Modifier
        .inspector(isPresented = showInspector, onIsPresentedChange = { showInspector = it }) {
            Form(modifier = Modifier.inspectorColumnWidth(min = 200.0, ideal = 260.0, max = 400.0)) {
                Text("Properties")
            }
        }
        .toolbarItem { Button("Inspector", systemImage = SFSymbol.sidebarRight) { showInspector = !showInspector } },
)
```

## File importer and exporter

The file panels are presentations too (iOS, macOS). Content types are Uniform Type Identifiers as strings
(`"public.image"`, `"public.json"`, `"com.adobe.pdf"`):

```kotlin
var importing by remember { mutableStateOf(false) }
Button(
    "Import",
    modifier = Modifier.fileImporter(
        isPresented = importing,
        onIsPresentedChange = { importing = it },
        allowedContentTypes = listOf("public.json"),
        allowsMultipleSelection = true,
        onFailure = { message -> println(message) },
    ) { urls ->
        // file:// URLs, security-scoped: read them with platform APIs
        println(urls)
    },
) { importing = true }
```

The URLs are security-scoped: on iOS and in a sandboxed macOS app, call `startAccessingSecurityScopedResource()` on the
`NSURL` before reading, and `stopAccessingSecurityScopedResource()` after.

`fileExporter` writes a document the user places: a `text` overload for text formats and a `data: ByteArray` overload
for binary content. `fileMover` moves an existing file.

```kotlin
var exporting by remember { mutableStateOf(false) }
Button(
    "Export CSV",
    modifier = Modifier.fileExporter(
        isPresented = exporting,
        onIsPresentedChange = { exporting = it },
        text = "name,email\nAda,ada@example.com\n",
        contentType = "public.comma-separated-values-text",
        defaultFilename = "contacts.csv",
        onCompletion = { url -> println("Saved to $url") },
    ),
) { exporting = true }
```

SwiftUI's `FileDocument`-based `fileExporter` and `DocumentGroup` are not available (see
[Limitations](limitations.md)).

## Dismissing from inside

The simplest way to close a presentation from its content is to change the state that presents it, as in the examples
above: pass a lambda such as `onClose = { show = false }` down to the presented screen. When the presented content
can't see that state (a reusable screen shown from several places), read SwiftUI's `dismiss` action with
`EnvironmentReader`:

```kotlin
@Composable
fun CloseButton() {
    EnvironmentReader { env ->
        if (env.isPresented) {
            Button("Close") { env.dismiss() }
        }
    }
}
```

`env.dismiss()` dismisses the closest presentation (sheet, cover, popover) or pops the current screen of a
`NavigationStack`, exactly like `@Environment(\.dismiss)`. `env.isPresented` tells whether the view is presented at
all. Environment values arrive after the first layout pass, so the first composition sees the defaults.

## Platform differences

| Presentation | iOS / iPadOS | macOS | tvOS | watchOS |
|---|---|---|---|---|
| `sheet` | Card sheet with detents (iPhone), form sheet (iPad) | Window-modal sheet | Full screen | Full screen |
| `fullScreenCover` | Yes | No-op | Yes | Yes |
| `popover` | Sheet on iPhone unless adapted, bubble on iPad | Popover | No-op | No-op |
| `inspector` | Sheet on iPhone, column on iPad | Trailing column | No-op | No-op |
| `alert`, `confirmationDialog` | Yes | Yes | Yes | Yes |
| `fileImporter`, `fileExporter` | Yes | Yes | No | No |

Detents only apply where the sheet is a card (iPhone, and iPad in compact width).
