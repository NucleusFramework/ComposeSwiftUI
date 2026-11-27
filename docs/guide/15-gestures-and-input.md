<!-- group: Building screens -->
# Gestures and input

> Taps, long presses, drags, pinches and composed gestures, drag and drop, focus, the keyboard, hover, Apple Pencil, URLs and the Digital Crown.

SwiftUI's gestures are values (`DragGesture().onChanged { }`) attached with `.gesture(_:)`. Compose SwiftUI exposes
them as modifiers whose callbacks receive plain Kotlin values. The callbacks run on the main thread and recompose
synchronously, so a drag that writes Kotlin state moves the view in the same frame.

Most interactive controls (`Button`, `Toggle`, `Slider`, `TextField`) need no gesture at all: see
[Controls](controls.md). Reach for gestures for custom interactions.

## Taps

```kotlin
var taps by remember { mutableStateOf(0) }
var lastPoint by remember { mutableStateOf("") }
VStack(spacing = 12.0) {
    // SwiftUI onTapGesture(count:perform:)
    Text("Tapped $taps times", modifier = Modifier.onTapGesture { taps++ })
    // Double tap
    Text("Double tap me", modifier = Modifier.onTapGesture(count = 2) { taps = 0 })
    // SpatialTapGesture: the location in the view's coordinate space
    ShapeView(
        Rectangle(),
        fill = Color.blue.opacity(0.2),
        modifier = Modifier
            .frame(height = 120.0)
            .onTapGesture { x, y -> lastPoint = "${x.toInt()}, ${y.toInt()}" },
    )
    Text(lastPoint)
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `.onTapGesture { }` | `Modifier.onTapGesture { }` |
| `.onTapGesture(count: 2) { }` | `Modifier.onTapGesture(count = 2) { }` |
| `.onTapGesture { location in }` / `SpatialTapGesture` | `Modifier.onTapGesture { x, y -> }` |
| `.onTapGesture(coordinateSpace: .global) { location in }` | `Modifier.onTapGesture(coordinateSpace = CoordinateSpace.global) { point -> }` |
| `.onLongPressGesture(minimumDuration:perform:onPressingChanged:)` | `Modifier.onLongPressGesture(minimumDuration = 0.5, onPressingChanged = { }) { }` |

Taps are not available on tvOS: use a `Button` there, which the focus engine can select. A transparent area does not
receive taps; give it a `Modifier.contentShape(Rectangle())`, as in SwiftUI.

## Long press

```kotlin
var pressing by remember { mutableStateOf(false) }
var armed by remember { mutableStateOf(false) }
Text(
    if (armed) "Armed" else "Hold to arm",
    modifier = Modifier
        .padding()
        .background(if (pressing) Color.orange else Color.gray.opacity(0.2))
        .onLongPressGesture(minimumDuration = 1.0, onPressingChanged = { pressing = it }) { armed = true },
)
```

`onLongPressGesture` exists on iOS, macOS and watchOS; tvOS has its own variant without a distance.

## Drag, magnify and rotate

`dragGesture`, `magnifyGesture` and `rotateGesture` attach the SwiftUI `DragGesture`, `MagnifyGesture` and
`RotateGesture`. `DragGestureValue` carries the translation, location, start location, predicted end translation and
velocity, each split into `X` / `Y` doubles:

```kotlin
@Composable
fun DraggableCard() {
    var offsetX by remember { mutableStateOf(0.0) }
    var offsetY by remember { mutableStateOf(0.0) }
    var dragX by remember { mutableStateOf(0.0) }
    var dragY by remember { mutableStateOf(0.0) }
    Text(
        "Drag me",
        modifier = Modifier
            .padding()
            .background(Color.yellow)
            .offset(x = offsetX + dragX, y = offsetY + dragY)
            .dragGesture(
                onChanged = { dragX = it.translationX; dragY = it.translationY },
                onEnded = {
                    withAnimation(Animation.spring) {
                        offsetX += it.predictedEndTranslationX
                        offsetY += it.predictedEndTranslationY
                        dragX = 0.0
                        dragY = 0.0
                    }
                },
            ),
    )
}
```

```kotlin
@Composable
fun ZoomablePhoto() {
    var scale by remember { mutableStateOf(1.0) }
    var pinch by remember { mutableStateOf(1.0) }
    var angle by remember { mutableStateOf(0.0) }
    Image(
        systemName = SFSymbol.photo,
        modifier = Modifier
            .font(Font.system(size = 120.0))
            .scaleEffect(scale * pinch)
            .rotationEffect(Angle.degrees(angle))
            .magnifyGesture(onChanged = { pinch = it }, onEnded = { scale *= it; pinch = 1.0 })
            .rotateGesture(simultaneous = true, onChanged = { angle = it }),
    )
}
```

- `dragGesture(minimumDistance = 10.0, …)` sets the distance before the drag starts; `0.0` makes it a touch-down
  tracker.
- The magnification factor is relative to the start of the gesture (1.0 = unchanged); the rotation is in degrees.
- `simultaneous = true` attaches the gesture with `simultaneousGesture`, so it runs together with the gestures of
  the view's children (a drag on a `ScrollView` row, for example) instead of competing with them.
- Magnify and rotate are multi-touch gestures for iOS and macOS (trackpad). Drags are not available on tvOS.

## Composed gestures

`Modifier.gesture` takes a `GestureSpec` built in Kotlin. Specs combine with `simultaneously`, `sequenced` and
`exclusively`, the Kotlin forms of `SimultaneousGesture`, `SequenceGesture` and `ExclusiveGesture`:

```kotlin
var pickedUp by remember { mutableStateOf(false) }
var dragX by remember { mutableStateOf(0.0) }
// Long press, then drag: the classic "pick up and move".
val pickUpAndMove = GestureSpec.LongPress(minimumDuration = 0.3, onEnded = { pickedUp = true })
    .sequenced(
        GestureSpec.Drag(
            onChanged = { dragX = it.translationX },
            onEnded = { pickedUp = false; dragX = 0.0 },
        ),
    )
Text(
    "Hold, then drag",
    modifier = Modifier
        .padding()
        .background(if (pickedUp) Color.green else Color.gray.opacity(0.2))
        .offset(x = dragX)
        .gesture(pickUpAndMove),
)
```

```kotlin
var scale by remember { mutableStateOf(1.0) }
var degrees by remember { mutableStateOf(0.0) }
val pinchAndTurn = GestureSpec.Magnify(onChanged = { scale = it })
    .simultaneously(GestureSpec.Rotate(onChanged = { degrees = it }))
// A double tap wins over a single tap.
val taps = GestureSpec.Tap(count = 2) { scale = 1.0 }.exclusively(GestureSpec.Tap { _ -> degrees = 0.0 })
Image(
    systemName = SFSymbol.leafFill,
    modifier = Modifier
        .scaleEffect(scale)
        .rotationEffect(Angle.degrees(degrees))
        .gesture(pinchAndTurn)
        .gesture(taps, priority = GesturePriority.High),
)
```

`GestureSpec` has `Tap(count) { point -> }`, `LongPress(minimumDuration, maximumDistance, onChanged, onEnded)`,
`Drag(minimumDistance, onChanged, onEnded)`, `Magnify` and `Rotate`. `GesturePriority.High` maps to
`highPriorityGesture` (wins over the children's gestures) and `GesturePriority.Simultaneous` to
`simultaneousGesture`. Composed gestures are not available on tvOS. SwiftUI's `@GestureState` has no Kotlin
equivalent: reset your state in `onEnded`, as in the examples.

## Haptics

`Modifier.sensoryFeedback(feedback, trigger)` plays a haptic (or sound, on watchOS) each time `trigger` changes:

```kotlin
var saved by remember { mutableStateOf(0) }
Button("Save", modifier = Modifier.sensoryFeedback(SensoryFeedback.success, trigger = saved)) { saved++ }
```

## Drag and drop

Drag and drop works with text and URL payloads on iOS and macOS (SwiftUI's `Transferable` types other than text and
URLs have no Kotlin form):

```kotlin
@Composable
fun TagBoard(tags: List<String>, onTagDropped: (String) -> Unit) {
    var targeted by remember { mutableStateOf(false) }
    VStack(spacing = 16.0) {
        HStack {
            for (tag in tags) {
                Text(tag, modifier = Modifier.padding(8.0).background(Color.blue.opacity(0.2)).draggable(tag))
            }
        }
        Text(
            "Drop a tag here",
            modifier = Modifier
                .frame(width = 240.0, height = 100.0)
                .background(if (targeted) Color.green.opacity(0.3) else Color.gray.opacity(0.1))
                .dropDestination(isTargeted = { targeted = it }) { items, _ ->
                    items.forEach(onTagDropped)
                    true
                },
        )
    }
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `.draggable("text")` | `Modifier.draggable("text")` |
| `.draggable(url)` | `Modifier.draggableUrl("https://…")` |
| `.dropDestination(for: String.self) { items, location in }` | `Modifier.dropDestination { items, location -> true }` |
| `.dropDestination(for: URL.self) { urls, location in }` | `Modifier.dropDestinationUrls { urls, location -> true }` |
| `.onDrop(of:delegate:)` | `Modifier.onDrop(contentTypes, object : DropDelegate() { … })` |

For full control (validation, the operation shown while hovering, enter and exit events), implement a
`DropDelegate`. Text and file URLs are loaded before `performDrop` is called:

```kotlin
@Composable
fun FileDropZone(onFiles: (List<String>) -> Unit) {
    val currentOnFiles by rememberUpdatedState(onFiles)
    val delegate = remember {
        object : DropDelegate() {
            override fun dropUpdated(location: CGPoint): DropOperation? = DropOperation.copy
            override fun performDrop(location: CGPoint, texts: List<String>, urls: List<String>): Boolean {
                currentOnFiles(urls)
                return urls.isNotEmpty()
            }
        }
    }
    Text("Drop files", modifier = Modifier.padding(40.0).onDrop(listOf("public.file-url"), delegate))
}
```

`DropDelegate` is hidden from Objective-C, so a named subclass in a module that builds an Apple framework must be
annotated too: `@OptIn(ExperimentalObjCRefinement::class) @HiddenFromObjC class MyDelegate : DropDelegate()`. An
anonymous object, as above, needs nothing.

Copy and paste go through `Modifier.copyable(items)`, `cuttable { }`, `pasteDestination { }`, the
`onCopyCommand` / `onCutCommand` / `onPasteCommand` modifiers (macOS menu commands) and `PasteButton { strings -> }`.

## Focus

SwiftUI's `@FocusState` is Kotlin state. `Modifier.focused(isFocused, onFocusChange)` binds a `Boolean`;
`Modifier.focused(value, focusedValue, onFocusChange)` binds an enum or any value, like `focused(_:equals:)`:

```kotlin
enum class Field { Email, Password }

@Composable
fun LoginForm(onSubmit: (String, String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    // Initial value = SwiftUI defaultFocus: the email field is focused when the form appears.
    var focus by remember { mutableStateOf<Field?>(Field.Email) }
    Form {
        TextField(
            "Email",
            text = email,
            onTextChange = { email = it },
            modifier = Modifier
                .focused(value = Field.Email, focusedValue = focus, onFocusChange = { focus = it as Field? })
                .keyboardType(KeyboardType.EmailAddress)
                .textContentType(TextContentType.Username)
                .textInputAutocapitalization(TextInputAutocapitalization.never)
                .submitLabel(SubmitLabel.next)
                .onSubmit { focus = Field.Password },
        )
        SecureField(
            "Password",
            text = password,
            onTextChange = { password = it },
            modifier = Modifier
                .focused(value = Field.Password, focusedValue = focus, onFocusChange = { focus = it as Field? })
                .textContentType(TextContentType.Password)
                .submitLabel(SubmitLabel.done)
                .onSubmit { focus = null; onSubmit(email, password) },
        )
    }
}
```

Setting the state to `null` (or `false`) dismisses the keyboard, as assigning `nil` to a `@FocusState` does.

| SwiftUI | Compose SwiftUI |
|---|---|
| `@FocusState var f: Bool` + `.focused($f)` | `var f by remember { mutableStateOf(false) }` + `Modifier.focused(f) { f = it }` |
| `.focused($field, equals: .email)` | `Modifier.focused(Field.Email, field) { field = it as Field? }` |
| `.defaultFocus($field, .email)` | initial value of the Kotlin state |
| `.focusable()`, `.focusable(interactions:)` | `Modifier.focusable()`, `Modifier.focusable(interactions = FocusInteractions.edit)` |
| `.focusEffectDisabled()` | `Modifier.focusEffectDisabled()` |
| `.focusSection()` (macOS, tvOS) | `Modifier.focusSection()` |
| `@Namespace` + `.focusScope(ns)` + `.prefersDefaultFocus(in: ns)` | `NamespaceScope { ns -> … Modifier.focusScope(ns) … Modifier.prefersDefaultFocus(namespace = ns) }` |
| `.searchFocused($f)` (iOS 18, macOS 15) | `Modifier.searchFocused(f) { f = it }` |
| `.accessibilityFocused($f)` | `Modifier.accessibilityFocused(f) { f = it }` (see [Accessibility](accessibility.md)) |

`focusScope` and `prefersDefaultFocus` work on macOS, tvOS and watchOS (SwiftUI ignores them on iOS):

```kotlin
NamespaceScope { ns ->
    VStack(modifier = Modifier.focusScope(ns)) {
        Button("Cancel") { }
        Button("Play", modifier = Modifier.prefersDefaultFocus(namespace = ns)) { }
    }
}
```

### Focused values and focused scene values

SwiftUI's `@FocusedValue` and `@FocusedSceneValue` are bridged with `FocusedValueKey<T>`, `Modifier.focusedValue`,
`Modifier.focusedSceneValue`, `FocusedValueReader` and `FocusedSceneValueReader`. SwiftUI propagates lightweight
string tokens through its native focus hierarchy while the Kotlin objects (including data classes, ViewModels or
lambdas) stay intact in Kotlin memory:

```kotlin
data class EditorActions(val title: String, val onSave: () -> Unit)

val ActiveEditorKey = FocusedValueKey<EditorActions>("activeEditor")

@Composable
fun EditorWithToolbar() {
    var text by remember { mutableStateOf("") }
    var saved by remember { mutableStateOf("") }
    VStack(modifier = Modifier.focusedSceneValue(ActiveEditorKey, EditorActions("Untitled") { saved = text })) {
        FocusedSceneValueReader(ActiveEditorKey) { actions ->
            HStack {
                Text(actions?.title ?: "No document")
                Button("Save") { actions?.onSave?.invoke() }
            }
        }
        TextField(
            "Notes",
            text = text,
            onTextChange = { text = it },
            modifier = Modifier.focusedValue(ActiveEditorKey, EditorActions("Notes field") { saved = text }),
        )
        FocusedValueReader(ActiveEditorKey) { focused ->
            Text("Focused: ${focused?.title ?: "none"} · Saved: $saved")
        }
    }
}
```

When nested views publish the same `FocusedValueKey`, the innermost focused view wins automatically, and unmounting or
moving focus clears the reader back to `null` (or falls back to an enclosing value). For menu bar `CommandMenu` actions
declared in Swift, you can still share a `StateFlow` or ViewModel between Kotlin and Swift (see [Platforms](platforms.md)).

## Keyboard

### Text input

| Modifier | What it does | Platforms |
|---|---|---|
| `keyboardType(KeyboardType.NumberPad)` | UIKit keyboard (`EmailAddress`, `DecimalPad`, `Url`, `PhonePad`…) | iOS, tvOS |
| `textContentType(TextContentType.OneTimeCode)` | AutoFill hint (`Username`, `Password`, `NewPassword`, `EmailAddress`…) | iOS, tvOS; macOS for username, password, one-time code |
| `textInputAutocapitalization(…)` | `never`, `words`, `sentences`, `characters` | iOS, tvOS, watchOS |
| `autocorrectionDisabled()` | turns autocorrection off | all |
| `submitLabel(SubmitLabel.search)` | the return key's label | all |
| `onSubmit { }` | return pressed in a text field (`SubmitTriggers.search` for the search field) | all |
| `scrollDismissesKeyboard(ScrollDismissesKeyboardMode.interactively)` | keyboard dismissal while scrolling | all |

### Key presses

`onKeyPress` handles hardware keys on the focused view (iOS with a keyboard, iPadOS, macOS, tvOS remote keys; not
watchOS). Return `true` when you handled the press:

```kotlin
var position by remember { mutableStateOf(0) }
Text(
    "Position $position",
    modifier = Modifier
        .focusable()
        .onKeyPress(keys = setOf("j", "k")) { press ->
            when (press.key) {
                "j" -> position++
                "k" -> position--
            }
            true
        },
)
```

`KeyPressEvent` has `characters` (with modifiers applied), `key`, `phase` (`Down`, `Repeat`, `Up`; filter with the
`phases` parameter) and the `shift`, `control`, `option` and `command` flags. An empty `keys` set receives every key.

### Shortcuts

`keyboardShortcut` attaches a shortcut to a `Button` or `Toggle` (iOS with a hardware keyboard, macOS):

```kotlin
HStack {
    Button("Save", modifier = Modifier.keyboardShortcut(KeyEquivalent.character("s"))) { }
    Button("Cancel", modifier = Modifier.keyboardShortcut(KeyEquivalent.escape, modifiers = EventModifiers(rawValue = 0))) { }
    Button("Export", modifier = Modifier.keyboardShortcut(
        KeyEquivalent.character("e"),
        // Swift's [.shift, .command]
        modifiers = EventModifiers.shift + EventModifiers.command,
    )) { }
}
```

On macOS, `Modifier.onModifierKeysChanged { old, new -> }` (macOS 15) reports modifier keys as raw `EventModifiers`
bits, and `onMoveCommand`, `onExitCommand` and `onDeleteCommand` receive the arrow keys, Escape and Delete when the
view has focus.

## Hover and pointer

```kotlin
var hovering by remember { mutableStateOf(false) }
var inside by remember { mutableStateOf(false) }
var pointer by remember { mutableStateOf<CGPoint?>(null) }
VStack {
    Text(
        "Hover me",
        modifier = Modifier
            .padding()
            .background(if (hovering) Color.blue.opacity(0.2) else Color.clear)
            .onHover { hovering = it },
    )
    ShapeView(
        Rectangle(),
        fill = if (inside) Color.green else Color.gray,
        modifier = Modifier.frame(width = 200.0, height = 100.0).onContinuousHover { phase ->
            inside = phase != HoverPhase.ended
            pointer = phase.location
        },
    )
}
```

| Modifier | Platforms |
|---|---|
| `onHover { isHovering -> }` | iOS (pointer, Apple Pencil hover), macOS |
| `onContinuousHover { phase -> }` | iOS, macOS, tvOS |
| `hoverEffect(HoverEffect.lift)` | iOS (pointer), tvOS (focus) |
| `pointerStyle(PointerStyle.link)` | macOS 15 |
| `help("Tooltip")` | tooltip on macOS (and with a pointer on iPadOS) |

The coordinate space defaults to `CoordinateSpace.local`; pass `coordinateSpace = CoordinateSpace.global` (or a
named space) to change it. The callback receives `HoverPhase.active(point)` or `HoverPhase.ended`: compare with
`HoverPhase.ended` to know whether the pointer is inside, and read `phase.location` (a `CGPoint?`, null once the
pointer has left) for its coordinates in that space. `hoverEffect()` without arguments uses `HoverEffect.automatic`.

## Apple Pencil

`Modifier.onPencilDoubleTap { hoverLocation -> }` and `Modifier.onPencilSqueeze { phase -> }` handle the Apple Pencil
double tap and squeeze (iOS 17.5+, macOS 14.5+). The hover location is a `CGPoint?`, `null` when the pencil is not
hovering:

```kotlin
var eraser by remember { mutableStateOf(false) }
Text(if (eraser) "Eraser" else "Pen", modifier = Modifier.onPencilDoubleTap { _ -> eraser = !eraser })
```

For drawing with the pencil, PencilKit's `PKCanvasView` is a UIKit view: embed it with `UIKitView` (see
[Interop](interop.md)).

## Opening URLs

`Modifier.onOpenURL { url -> }` receives the URLs the app is opened with (custom schemes, universal links), as a
string. Attach it to a view that is on screen, typically the root of your Kotlin content; the URL scheme itself is
declared in the app's `Info.plist`:

```kotlin
@Composable
fun AppRoot(onDeepLink: (String) -> Unit) {
    NavigationStack(modifier = Modifier.onOpenURL { url -> onDeepLink(url) }) {
        Text("Home")
    }
}
```

`Modifier.onOpenUrlAction { url -> handled }` intercepts the links opened from inside the view (a `Link`, Markdown
links in a `LocalizedKey` text): return `true` to handle a link in Kotlin, `false` to let the system open it.

## Digital Crown (watchOS)

`digitalCrownRotation` binds the crown to a Kotlin `Double`. It is a no-op on the other platforms, so the code can
stay in `commonMain`:

```kotlin
var volume by remember { mutableStateOf(5.0) }
Text(
    "Volume ${volume.toInt()}",
    modifier = Modifier
        .focusable()
        .digitalCrownRotation(
            binding = volume,
            onBindingChange = { volume = it },
            minValue = 0.0,
            through = 10.0,
            stride = 1.0,
            sensitivity = DigitalCrownRotationalSensitivity.low,
        ),
)
```

Overloads take `onIdle`, `isContinuous` and `isHapticFeedbackEnabled`, and a `detent` variant snaps to `Int` steps.
`digitalCrownAccessory` and `handGestureShortcut` (watchOS double tap, iOS 18 / watchOS 11) are available too. tvOS
remote buttons come through `onMoveCommand`, `onExitCommand` and `onPlayPauseCommand`; see [Platforms](platforms.md).
