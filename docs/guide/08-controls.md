<!-- group: Building screens -->
# Controls

> Buttons, toggles, pickers, text input, menus, progress and forms: native SwiftUI controls bound to Kotlin state, plus custom control styles written in Kotlin.

Every control in this chapter is the real SwiftUI control: it looks, animates and behaves like the platform's, on each
platform. What changes is how you bind it to state. SwiftUI takes a `Binding`; Compose SwiftUI takes the current
value and a callback, the way Compose UI does:

| SwiftUI | Compose SwiftUI |
|---|---|
| `Toggle("Wi-Fi", isOn: $wifi)` | `Toggle("Wi-Fi", isOn = wifi, onIsOnChange = { wifi = it })` |
| `TextField("Name", text: $name)` | `TextField("Name", text = name, onTextChange = { name = it })` |
| `Picker("Size", selection: $size) { … }` | `Picker("Size", selection = size, onSelectionChange = { size = it as Size }) { … }` |

The rule is general: a binding parameter `x` becomes `x` plus `onXChange`. The control is controlled by Kotlin. When
the user edits it, Swift calls your callback, the state changes, and Compose recomposes before SwiftUI draws the next
frame, so typing and dragging don't jump. If the callback ignores the new value, the control snaps back. See
[State and data](state-and-data.md) for where to keep that state.

Controls that a platform doesn't have compile everywhere and render nothing there (for example `Slider`, `Stepper` and
`DatePicker` on tvOS, `Menu` and `ControlGroup` on watchOS). The availability notes below say where.

## Buttons

```kotlin
@Composable
fun ButtonExamples(onSave: () -> Unit, onDelete: () -> Unit) {
    VStack(spacing = 12.0) {
        Button("Save", action = onSave)
        Button("Share", systemImage = SFSymbol.squareAndArrowUp) { }
        Button("Delete", role = ButtonRole.destructive, action = onDelete)

        // any content as the label
        Button(action = onSave) {
            HStack {
                Image(systemName = SFSymbol.icloudAndArrowUp)
                Text("Upload")
            }
        }
    }
}
```

Roles (`ButtonRole.destructive`, `ButtonRole.cancel`; `close` and `confirm` on iOS 26 / macOS 26) tell SwiftUI how to
present the button: destructive buttons are red, and alerts, menus and dialogs order buttons by role.

Built-in styles are applied with `Modifier.buttonStyle`, and they apply to every button inside the modified view:

```kotlin
VStack {
    Button("Continue", modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent).controlSize(ControlSize.large)) { }
    Button("Cancel", modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered).tint(Color.red)) { }
    Button("Learn more", modifier = Modifier.buttonStyle(PrimitiveButtonStyle.plain)) { }
    HStack(modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderless)) {
        Button("Edit") { }
        Button("Duplicate") { }
    }
}
```

`PrimitiveButtonStyle` also has `glass` and `glassProminent` (iOS 26 / macOS 26), `link` (macOS) and `card` (tvOS).
`Modifier.buttonBorderShape(ButtonBorderShape.capsule)` and `Modifier.controlSize(…)` adjust bordered buttons.
`Modifier.disabled(true)` disables a control and everything inside it.

A button that should fill the width puts the frame on its label, as in SwiftUI:

```kotlin
Button(action = {}, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent)) {
    Text("Create account", modifier = Modifier.frame(maxWidth = Double.POSITIVE_INFINITY))
}
```

Button actions are plain Kotlin lambdas, called on the main thread. Start asynchronous work from them with a
coroutine scope (a view model's `viewModelScope`, or `rememberCoroutineScope()`).

## Toggle

```kotlin
var notifications by remember { mutableStateOf(true) }
var sounds by remember { mutableStateOf(false) }
Form {
    Toggle("Notifications", isOn = notifications, onIsOnChange = { notifications = it })
    Toggle("Sounds", systemImage = SFSymbol.speakerWave2, isOn = sounds, onIsOnChange = { sounds = it })
    Toggle(isOn = sounds, onIsOnChange = { sounds = it }) {
        VStack(alignment = HorizontalAlignment.leading) {
            Text("Play sounds")
            Text("Only when the app is open", modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary))
        }
    }
}
```

`Modifier.toggleStyle(ToggleStyle.switch)` is the default on iOS; `ToggleStyle.button` and `ToggleStyle.checkbox`
(macOS) are the alternatives. A toggle that drives several values at once, shown as mixed when they differ, is
`Toggle(title, sources = listOf(…), onSourceChange = { index, isOn -> })`.

## Slider and Stepper

```kotlin
@Composable
fun VolumeControls(volume: Double, onVolumeChange: (Double) -> Unit, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Form {
        Slider(value = volume, bounds = 0.0..1.0, onValueChange = onVolumeChange)
        Slider(
            value = volume,
            bounds = 0.0..1.0,
            step = 0.1,
            onValueChange = onVolumeChange,
            label = { Text("Volume") },
            minimumValueLabel = { Image(systemName = SFSymbol.speakerFill) },
            maximumValueLabel = { Image(systemName = SFSymbol.speakerWave3Fill) },
        )
        Stepper("Quantity: $quantity", value = quantity, bounds = 1..10, onValueChange = onQuantityChange)
    }
}
```

`onEditingChanged` reports when a drag starts and ends, which is the moment to commit an expensive change (save,
network) while still updating the UI on every `onValueChange`. `Stepper(onIncrement = …, onDecrement = …)` covers
steppers that don't map to a number. Slider and Stepper are unavailable on tvOS.

## Picker

A `Picker` shows its options as children; each option is tagged with `Modifier.tag(value)`, and the picker reports the
tag of the chosen option. Any Kotlin value works as a tag (enum entries, ids, data classes): it is compared with
`equals`. The selection arrives typed `Any`, so cast it back:

```kotlin
enum class Flavor(val label: String) { Vanilla("Vanilla"), Chocolate("Chocolate"), Strawberry("Strawberry") }

@Composable
fun FlavorPicker(flavor: Flavor, onFlavorChange: (Flavor) -> Unit) {
    Picker("Flavor", selection = flavor, onSelectionChange = { onFlavorChange(it as Flavor) }) {
        for (option in Flavor.entries) key(option) {
            Text(option.label, modifier = Modifier.tag(option))
        }
    }
}
```

Every option needs a tag of the same type as the selection: an untagged option can't be selected, and a selection
that matches no tag shows nothing selected. Options can be `Label`s too.

Styles:

```kotlin
var mode by remember { mutableStateOf("list") }
Picker("View", selection = mode, onSelectionChange = { mode = it as String }, modifier = Modifier.pickerStyle(PickerStyle.segmented)) {
    Label("List", systemImage = SFSymbol.listBullet, modifier = Modifier.tag("list"))
    Label("Grid", systemImage = SFSymbol.squareGrid2x2, modifier = Modifier.tag("grid"))
}
```

| Style | Where |
|---|---|
| `PickerStyle.automatic` | menu in a form on iOS, pop-up button on macOS |
| `PickerStyle.menu` | iOS, macOS, tvOS |
| `PickerStyle.segmented` | iOS, macOS, tvOS |
| `PickerStyle.inline` | lists and forms |
| `PickerStyle.navigationLink` | pushes a selection list (iOS, watchOS; needs a `NavigationStack`) |
| `PickerStyle.wheel` | iOS, watchOS |
| `PickerStyle.palette` | iOS, macOS (in menus and toolbars) |
| `PickerStyle.radioGroup` | macOS |

`Picker(title, sources = …, onSourceChange = { index, value -> })` is the mixed-selection variant, and the overloads
with `currentValueLabel` (iOS 18 / macOS 15) customize the collapsed label.

## Dates

Dates are `Double` seconds since 1970, like `Date.timeIntervalSince1970`.

```kotlin
@Composable
fun ReminderForm(due: Double, onDueChange: (Double) -> Unit, now: Double) {
    Form {
        DatePicker("Due", selection = due, onSelectionChange = onDueChange)
        DatePicker(
            "Day",
            selection = due,
            range = now..(now + 365 * 86_400.0),
            displayedComponents = DatePickerComponents.date,
            onSelectionChange = onDueChange,
        )
        DatePicker("Time", selection = due, displayedComponents = DatePickerComponents.hourAndMinute, onSelectionChange = onDueChange)
    }
}
```

`Modifier.datePickerStyle(DatePickerStyle.graphical)` shows a calendar; `compact`, `wheel` (iOS), `field` and
`stepperField` (macOS) are the others. `DatePicker` is unavailable on tvOS.

`MultiDatePicker` (iOS only, nothing elsewhere) selects several days. Days are `Day(year, month, day)` values, not
timestamps, so there is no time zone to get wrong:

```kotlin
var days by remember { mutableStateOf(setOf(Day(2026, 11, 20))) }
MultiDatePicker("Availability", selection = days, onSelectionChange = { days = it })
```

## Text input

```kotlin
@Composable
fun SignInFields(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Form {
        TextField("Email", text = email, onTextChange = onEmailChange, modifier = Modifier
            .keyboardType(KeyboardType.EmailAddress)
            .textContentType(TextContentType.EmailAddress)
            .textInputAutocapitalization(TextInputAutocapitalization.never)
            .autocorrectionDisabled())
        SecureField("Password", text = password, onTextChange = onPasswordChange, modifier = Modifier
            .textContentType(TextContentType.Password)
            .submitLabel(SubmitLabel.go)
            .onSubmit(action = onSubmit))
    }
}
```

- `prompt` is the placeholder shown inside the field (the title is used for the label in forms on macOS):
  `TextField("Name", text = name, prompt = "Jane Appleseed", onTextChange = { name = it })`.
- `axis = Axis.vertical` makes a field grow with its text; combine it with `Modifier.lineLimit(3..6)`.
- `Modifier.textFieldStyle(TextFieldStyle.roundedBorder)` draws a border outside of forms (`plain`, `roundedBorder`;
  `squareBorder` on macOS).
- `keyboardType` (iOS, tvOS) and `textContentType` (iOS, tvOS, macOS) are Kotlin enums: `KeyboardType.NumberPad`,
  `TextContentType.OneTimeCode`…
- `Modifier.focused(isFocused, onFocusChange)` and the `focused(value, focusedValue, onFocusChange)` overload replace
  `@FocusState`; [Gestures and input](gestures-and-input.md) covers focus.

`TextField(title, text, selection, onTextChange, onSelectionChange)` and its `TextEditor` counterpart also bind the
selection as a `TextRange` of UTF-16 offsets (iOS 18 / macOS 15; a plain field earlier).

### Multi-line text

A plain `TextEditor` is built from a `TextEditor` value passed to `TextEditorView`:

```kotlin
var notes by remember { mutableStateOf("") }
TextEditorView(TextEditor(notes, onTextChange = { notes = it }), modifier = Modifier.frame(minHeight = 120.0))
```

For a few lines, a vertical `TextField` is usually nicer: it has a placeholder and sizes itself.

### Numbers

`NumberField` is SwiftUI's `TextField(value:format:)` for numbers. SwiftUI parses and formats the text in the user's
locale and calls `onValueChange` when the user commits a valid number (on submit or when the field loses focus), not on
every keystroke:

```kotlin
var amount by remember { mutableStateOf(0.0) }
var tip by remember { mutableStateOf(0.15) }
Form {
    NumberField("Amount", value = amount, onValueChange = { amount = it }, format = NumberFormat.Currency("EUR"))
    NumberField("Tip", value = tip, onValueChange = { tip = it }, format = NumberFormat.Percent)
}
```

## ColorPicker

```kotlin
var tint by remember { mutableStateOf(Color.blue) }
ColorPicker("Accent color", selection = tint, supportsOpacity = false, onSelectionChange = { tint = it })
```

The selection is a SwiftUI `Color` value you can pass to any modifier. ColorPicker is available on iOS and macOS.

## Menus and control groups

```kotlin
@Composable
fun DocumentMenu(onRename: () -> Unit, onDuplicate: () -> Unit, onDelete: () -> Unit, onOpen: () -> Unit) {
    HStack {
        Menu("Actions", systemImage = SFSymbol.ellipsisCircle) {
            Button("Rename", systemImage = SFSymbol.pencil, action = onRename)
            Button("Duplicate", systemImage = SFSymbol.plusSquareOnSquare, action = onDuplicate)
            Divider()
            Menu("Share") {
                Button("Copy link") { }
                Button("Email") { }
            }
            Button("Delete", systemImage = SFSymbol.trash, role = ButtonRole.destructive, action = onDelete)
        }

        // a button with a menu of secondary actions
        Menu("Open", primaryAction = onOpen) {
            Button("Open in new window") { }
        }
    }
}
```

A `Picker` or a `Toggle` inside a menu becomes a checkmarked item. `Modifier.menuStyle(…)`, `menuOrder` and
`menuIndicator` work as in SwiftUI.

`ControlGroup` groups related buttons; it shows them side by side in a toolbar and as a compact row in a menu:

```kotlin
ControlGroup {
    Button("Decrease", systemImage = SFSymbol.minus) { }
    Button("Increase", systemImage = SFSymbol.plus) { }
}
```

Menu and ControlGroup are unavailable on watchOS. Context menus and toolbars are covered in
[Presentation](presentation.md) and [Navigation](navigation.md).

## Links and sharing

```kotlin
VStack {
    Link("Privacy policy", destination = "https://example.com/privacy")
    Link(destination = "https://developer.apple.com") {
        Label("Developer site", systemImage = SFSymbol.safari)
    }
    ShareLink(url = "https://example.com/post/42", subject = "A good read", previewTitle = "My post")
    ShareLink(text = "Meet at 7pm, usual place", previewTitle = "Plan")
    PasteButton { strings -> println(strings) }
}
```

URLs are strings. `Link` opens the URL with the system (`openURL`). `ShareLink` shares a URL or a text with the share
sheet (iOS, macOS, watchOS); its `label` slot replaces the default icon and title. `PasteButton` (iOS, macOS) reads
text items from the pasteboard without the paste permission prompt.

## Progress and gauges

```kotlin
@Composable
fun DownloadStatus(progress: Double?, battery: Double) {
    VStack(spacing = 16.0) {
        // indeterminate when the value is null
        ProgressView("Downloading", value = progress, total = 1.0)
        ProgressView()  // a spinner

        Gauge(value = battery, bounds = 0.0..1.0, label = { Text("Battery") }, currentValueLabel = { Text("${(battery * 100).toInt()}%") })
    }
}
```

`Modifier.progressViewStyle(ProgressViewStyle.circular)` and `Modifier.gaugeStyle(GaugeStyle.accessoryCircular)`
choose the look; the `accessory` gauge styles are the ones used in widgets and complications. A progress view over a
date range, `ProgressView(timerInterval = start..end)`, animates by itself. Gauge is unavailable on tvOS.

## LabeledContent, Form and Section

`LabeledContent` puts a label next to a value, aligned the way the platform's settings screens do:

```kotlin
Form {
    Section(header = { Text("Account") }, footer = { Text("Your email is never shared.") }, content = {
        LabeledContent("Name", value = "Ada Lovelace")
        LabeledContent("Plan") {
            Text("Pro", modifier = Modifier.foregroundStyle(Color.purple))
        }
    })
    Section("Preferences") {
        Toggle("Dark mode", isOn = false, onIsOnChange = {})
    }
}
```

`Form` is a `List` with control-friendly styling; `Modifier.formStyle(FormStyle.grouped)` gives the iOS settings look
on macOS too (`columns` is the macOS default). `Section(title) { … }` takes a string header; `Section(isExpanded,
onIsExpandedChange)` makes it collapsible in sidebars.

Watch the trailing lambda: for `Section(content, header)` and `Section(content, header, footer)`, the last parameter
is a slot other than `content`. Pass every slot by name, as above, rather than `Section(header = { … }) { … }`, which
doesn't compile.

## A complete form

State hoisted in a view model, controls bound to it:

```kotlin
data class Settings(
    val displayName: String = "",
    val notifications: Boolean = true,
    val frequency: Int = 1,
    val volume: Double = 0.5,
)

class SettingsViewModel : androidx.lifecycle.ViewModel() {
    private val _state = kotlinx.coroutines.flow.MutableStateFlow(Settings())
    val state: kotlinx.coroutines.flow.StateFlow<Settings> = _state

    fun update(transform: (Settings) -> Settings) {
        _state.value = transform(_state.value)
    }
}

@Composable
fun SettingsScreen() {
    val vm = viewModel { SettingsViewModel() }
    val settings by vm.state.collectAsState()
    NavigationStack {
        Form(modifier = Modifier.navigationTitle("Settings")) {
            Section("Profile") {
                TextField("Display name", text = settings.displayName, onTextChange = { v -> vm.update { it.copy(displayName = v) } })
            }
            Section("Notifications") {
                Toggle("Enabled", isOn = settings.notifications, onIsOnChange = { v -> vm.update { it.copy(notifications = v) } })
                Picker("Frequency", selection = settings.frequency, onSelectionChange = { v -> vm.update { it.copy(frequency = v as Int) } }) {
                    Text("Hourly", modifier = Modifier.tag(0))
                    Text("Daily", modifier = Modifier.tag(1))
                    Text("Weekly", modifier = Modifier.tag(2))
                }
                Slider(value = settings.volume, bounds = 0.0..1.0, onValueChange = { v -> vm.update { it.copy(volume = v) } })
            }
        }
    }
}
```

## Custom styles in Kotlin

SwiftUI's style protocols (`ButtonStyle`, `ToggleStyle`, `LabelStyle`…) can be implemented in Kotlin. Each one is a
modifier taking a composable body that draws one control; placeholders such as `StyleLabel()` stand for the parts
SwiftUI provides (the button's label, the toggle's title). Like SwiftUI styles, they apply to every matching control
inside the modified view.

| SwiftUI protocol | Kotlin | Placeholders |
|---|---|---|
| `ButtonStyle` | `Modifier.buttonStyle { isPressed -> }` | `StyleLabel()` |
| `ToggleStyle` | `Modifier.toggleStyle { isOn -> }` | `StyleLabel()` |
| `LabelStyle` | `Modifier.labelStyle { }` | `StyleIcon()`, `StyleLabel()` |
| `ProgressViewStyle` | `Modifier.progressViewStyle { }` | `StyleProgressFill()`, `StyleLabel()` |
| `GroupBoxStyle` | `Modifier.groupBoxStyle { }` | `StyleLabel()`, `StyleContent()` |
| `LabeledContentStyle` | `Modifier.labeledContentStyle { }` | `StyleLabel()`, `StyleContent()` |
| `ControlGroupStyle`, `FormStyle`, `MenuStyle` | `Modifier.controlGroupStyle { }`, `formStyle { }`, `menuStyle { }` | `StyleContent()` |

A button style that scales down while pressed:

```kotlin
val PillButtonStyle = Modifier.buttonStyle { isPressed ->
    StyleLabel(modifier = Modifier
        .font(Font.headline)
        .foregroundStyle(Color.white)
        .padding(Edge.Set.horizontal, 20.0)
        .padding(Edge.Set.vertical, 10.0)
        .background(Color.accentColor, shape = Capsule())
        .scaleEffect(if (isPressed) 0.95 else 1.0)
        .opacity(if (isPressed) 0.8 else 1.0))
}

@Composable
fun Toolbar() {
    HStack(modifier = PillButtonStyle) {
        Button("Accept") { }
        Button("Decline") { }
    }
}
```

The body is composed once per state up front (pressed and not pressed for a button, on and off for a toggle), so a
style applied to many controls costs one composition, and SwiftUI switches between the two without calling Kotlin.
Keep the body free of state reads that change often; a style is a template, not a place for per-button data.

A checkbox toggle for iOS, which has no built-in one. Tapping the drawn toggle flips it:

```kotlin
val CheckboxToggleStyle = Modifier.toggleStyle { isOn ->
    HStack {
        Image(systemName = if (isOn) SFSymbol.checkmarkSquareFill else SFSymbol.square, modifier = Modifier
            .foregroundStyle(if (isOn) Color.accentColor else Color.secondary))
        StyleLabel()
    }
}
```

A label style that stacks the icon above the title, and a thin progress bar:

```kotlin
val VerticalLabelStyle = Modifier.labelStyle {
    VStack(spacing = 4.0) {
        StyleIcon(modifier = Modifier.font(Font.title2))
        StyleLabel(modifier = Modifier.font(Font.caption))
    }
}

val ThinProgressStyle = Modifier.progressViewStyle {
    VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
        StyleLabel(modifier = Modifier.font(Font.caption))
        ZStack(alignment = Alignment.leading) {
            ShapeView(Capsule(), fill = Color.gray.opacity(0.2), modifier = Modifier.frame(height = 4.0))
            StyleProgressFill(modifier = Modifier.frame(height = 4.0).foregroundStyle(Color.green))
        }
    }
}
```

Because these are ordinary `Modifier` values, you can keep them in a shared file, combine them with `then`, and apply
them at the root of a screen to style everything below.
