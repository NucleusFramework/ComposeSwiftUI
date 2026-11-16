<!-- group: Getting started -->
# API mapping

> The rules that turn any SwiftUI view, modifier or value into its Kotlin form, so you can read Apple's documentation and write the Kotlin directly.

The Kotlin API is generated from Apple's SwiftUI SDK, so it is SwiftUI's API with Kotlin syntax. Once you know a
dozen rules you can translate any SwiftUI snippet without looking anything up. This chapter lists them, with the
exceptions.

## At a glance

| SwiftUI | Compose SwiftUI |
|---|---|
| `Text("Hi").font(.title).padding()` | `Text("Hi", modifier = Modifier.font(Font.title).padding())` |
| `VStack(alignment: .leading, spacing: 8) { … }` | `VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { … }` |
| `Image(systemName: "star.fill")` | `Image(systemName = SFSymbol.starFill)` |
| `Button("Save") { save() }` | `Button("Save") { save() }` |
| `Button("Delete", role: .destructive) { … }` | `Button("Delete", role = ButtonRole.destructive) { … }` |
| `Section { … } header: { Text("A") }` | `Section(header = { Text("A") }, content = { … })` |
| `Label { Text("A") } icon: { Image(…) }` | `Label(title = { Text("A") }, icon = { Image(…) })` |
| `Toggle("Wi-Fi", isOn: $on)` | `Toggle("Wi-Fi", isOn = on, onIsOnChange = { on = it })` |
| `TextField("Name", text: $name)` | `TextField("Name", text = name, onTextChange = { name = it })` |
| `Slider(value: $v, in: 0...1)` | `Slider(value = v, bounds = 0.0..1.0, onValueChange = { v = it })` |
| `.foregroundStyle(.secondary)` | `Modifier.foregroundStyle(ShapeStyle.secondary)` |
| `.buttonStyle(.borderedProminent)` | `Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent)` |
| `.padding(.horizontal, 16)` | `Modifier.padding(Edge.Set.horizontal, 16.0)` |
| `.frame(maxWidth: .infinity)` | `Modifier.frame(maxWidth = Double.POSITIVE_INFINITY)` |
| `Color.red.opacity(0.5)` | `Color.red.opacity(0.5)` |
| `.font(.system(size: 17, weight: .semibold))` | `Modifier.font(Font.system(size = 17.0, weight = Font.Weight.semibold))` |
| `.environment(\.colorScheme, .dark)` | `Modifier.environment(colorScheme = ColorScheme.dark)` |
| `ForEach(items, id: \.id) { … }` | `for (item in items) key(item.id) { … }`, or `ForEach(items, key = { it.id }) { … }` |
| `List { ForEach(…) }` with thousands of rows | `List { LazyForEach(items, key = { it.id }) { … } }` |
| `.listRowBackground(Color.red)` | `Modifier.listRowBackground { ColorView(Color.red) }` |
| `"**Bold**"` or a localization key | `LocalizedKey("hello.title")`; a plain `String` is shown verbatim |

## Views are composables

Every SwiftUI view initializer is a `@Composable` function with the view's name. Each initializer is its own
overload: `Text(verbatim:)`, `Text(date:style:)`, `Image(systemName:)`, `Image(decorative:)`…

```kotlin
VStack(alignment = HorizontalAlignment.leading, spacing = 4.0) {
    Text("Ada Lovelace", modifier = Modifier.font(Font.headline))
    Text("Mathematician", modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
}
```

Value types that are also views (shapes, colors, gradients) are values in Kotlin, and are shown with a `…View`
composable or with `ShapeView`:

```kotlin
HStack {
    ColorView(Color.orange, modifier = Modifier.frame(width = 20.0, height = 20.0))
    CircleView(Circle(), modifier = Modifier.frame(width = 20.0, height = 20.0))
    ShapeView(RoundedRectangle(cornerRadius = 8.0), fill = Color.blue, modifier = Modifier.frame(width = 40.0, height = 20.0))
}
```

## Argument labels become named arguments

A Swift argument label becomes the Kotlin parameter name: `spacing: 8` is `spacing = 8.0`, `systemImage: "star"` is
`systemImage = SFSymbol.star`. An unlabeled first argument (`_ title`) is positional in Kotlin, as in Swift:
`Toggle("Wi-Fi", …)`, `Button("Save") { … }`.

Parameters follow Swift's order, except that `modifier` comes right after the required leading arguments and the
closures come last. Use named arguments whenever the call has more than one or two arguments; it reads like Swift and
survives overload changes.

## Closures become lambdas

- The main `@ViewBuilder` closure (`content`) is the trailing lambda: `VStack { … }`, `List { … }`, `Form { … }`.
- The other builders are named lambda parameters: `label = { … }`, `header = { … }`, `footer = { … }`,
  `icon = { … }`, `currentValueLabel = { … }`.
- An action closure is a plain Kotlin function: `Button("Save", action = vm::save)`, `Modifier.onAppear { … }`.
- When a view has an action and a label, the trailing lambda is the main closure, as in Swift: `Button(action =
  { … }) { Text("Save") }` passes the label as trailing lambda, `Slider(value, bounds) { value = it }` passes the
  value callback.
- An optional closure is a nullable function type: `Stepper(onIncrement = { … }, onDecrement = null) { … }`.

```kotlin
Section(
    header = { Text("Account") },
    footer = { Text("Signed in with iCloud.") },
    content = {
        LabeledContent("Name", value = "Ada")
        LabeledContent("Plan", value = "Family")
    },
)
```

A SwiftUI parameter that takes a view as a value rather than a builder (`.listRowBackground(Color.red)`,
`.mask(Circle())`, `.background(Color.red, alignment:)`) also takes a composable lambda in Kotlin:

```kotlin
Text("Masked", modifier = Modifier.padding().mask { CircleView(Circle()) })
```

## Modifiers are a Modifier chain, in order

Every view composable has a `modifier: Modifier = Modifier` parameter. SwiftUI modifiers are extension functions on
`Modifier`, applied in the order you chain them, exactly as in Swift:

```kotlin
// .padding().background(.yellow): yellow behind the padded text
Text("Padded, then filled", modifier = Modifier.padding().background(Color.yellow))
// .background(.yellow).padding(): yellow only behind the text
Text("Filled, then padded", modifier = Modifier.background(Color.yellow).padding())
```

A `Modifier` is an immutable value: store it in a `val`, return it from a function, combine two chains with `then`.

```kotlin
val card = Modifier
    .padding()
    .background(ShapeStyle.background, RoundedRectangle(cornerRadius = 12.0))
    .shadow(radius = 2.0)
Text("Card", modifier = card.then(Modifier.padding(Edge.Set.horizontal, 16.0)))
```

Modifiers that present something (`sheet`, `alert`, `popover`, `toolbarItem`, `contextMenu`, `swipeActions`,
`overlay`) take their content as a composable lambda, like any other closure.

## Bindings become a value and a callback

A SwiftUI `Binding<T>` parameter `x` becomes two parameters: `x: T` with the current value and `onXChange: (T) -> Unit`
called when the user changes it. The control shows exactly what you pass; it changes only when your state changes.

```kotlin
var name by remember { mutableStateOf("") }
var notify by remember { mutableStateOf(true) }
var volume by remember { mutableStateOf(0.5) }
var guests by remember { mutableStateOf(2) }
Form {
    TextField("Name", text = name, onTextChange = { name = it })
    Toggle("Notifications", isOn = notify, onIsOnChange = { notify = it })
    Slider(value = volume, bounds = 0.0..1.0, onValueChange = { volume = it })
    Stepper("Guests: $guests", value = guests, bounds = 1..10, onValueChange = { guests = it })
}
```

| SwiftUI binding | Kotlin |
|---|---|
| `isOn: $on` | `isOn = on, onIsOnChange = { on = it }` |
| `text: $text` | `text = text, onTextChange = { text = it }` |
| `selection: $selection` | `selection = selection, onSelectionChange = { selection = it }` |
| `isPresented: $show` | `isPresented = show, onIsPresentedChange = { show = it }` |
| `value: $value` | `value = value, onValueChange = { value = it }` |
| `isExpanded: $expanded` | `isExpanded = expanded, onIsExpandedChange = { expanded = it }` |
| `columnVisibility: $visibility` | `columnVisibility = visibility, onColumnVisibilityChange = { visibility = it }` |
| `.constant(true)` | pass the value and an empty callback: `isOn = true, onIsOnChange = {}` |

Selections that SwiftUI types generically (`Picker`, `TabView`, `List`) take `Any` and identify choices with
`Modifier.tag(value)`, as in Swift. Cast in the callback, or use the typed hand-written forms where they exist
(`TabView(selection, onSelectionChange)` is generic):

```kotlin
var flavor by remember { mutableStateOf("vanilla") }
Picker("Flavor", selection = flavor, onSelectionChange = { flavor = it as String }) {
    Text("Vanilla", modifier = Modifier.tag("vanilla"))
    Text("Chocolate", modifier = Modifier.tag("chocolate"))
}
```

`@State`, `@StateObject`, `@Observable` and `@Binding` themselves have no Kotlin form: `remember { mutableStateOf() }`,
view models and state hoisting replace them. See [State and data](state-and-data.md).

## Numbers, strings and other Swift types

| Swift | Kotlin |
|---|---|
| `CGFloat`, `Double`, `Float` | `Double` (write `8.0`, not `8`) |
| `Int`, `Int8` … `UInt64` | `Int` (fixed-width types are truncated on the Swift side) |
| `.infinity` | `Double.POSITIVE_INFINITY` |
| `ClosedRange<Double>`, `ClosedRange<Int>` | `0.0..1.0`, `1..10` |
| `Date` | `Double`, seconds since 1970 (epoch seconds) |
| `ClosedRange<Date>` | `ClosedFloatingPointRange<Double>` of epoch seconds |
| `URL` | `String` (the absolute URL) |
| `UTType`, `Locale`, `Locale.Language`, `Character` | `String`: `"public.png"`, `"fr_FR"`, `"fr"`, one character |
| `AttributedString` | `String` (attributes dropped); for styled text use `Text(StyledString)` |
| `ImageResource`, `ColorResource` (asset catalog) | `String`, the asset name: `Button("Logo", image = "logo") { }` |
| SF Symbol name (`systemImage:`, `systemName:`) | `SFSymbol`: `SFSymbol.starFill`, or `SFSymbol("custom.name")` |
| `LocalizedStringKey`, `LocalizedStringResource` | `LocalizedKey("key")`, or `"key".localized` |
| `Transferable` payload | `String` (text) or a URL string |

Strings deserve a note. In Swift, `Text("Hello")` takes a `LocalizedStringKey`: it is looked up in your string tables
and parsed as Markdown. In Kotlin, a `String` is shown **verbatim**, which is what you want for data. To localize or
use Markdown, pass a `LocalizedKey`:

```kotlin
Text("Literal **not bold**")
Text(LocalizedKey("Welcome **back**"))
Button(LocalizedKey("settings.save")) { }
```

## Enum cases and static members

SwiftUI's implicit member syntax (`.title`, `.secondary`, `.leading`) needs the type in Kotlin. The type is the one
SwiftUI declares the member on, usually the parameter's type:

| SwiftUI | Kotlin |
|---|---|
| `.font(.title)` | `Modifier.font(Font.title)` |
| `.foregroundStyle(.secondary)` | `Modifier.foregroundStyle(ShapeStyle.secondary)` |
| `.foregroundStyle(.red)` | `Modifier.foregroundStyle(Color.red)` or `ShapeStyle.red` |
| `.fontWeight(.bold)` | `Modifier.fontWeight(Font.Weight.bold)` |
| `alignment: .leading` (in a `VStack`) | `alignment = HorizontalAlignment.leading` |
| `.frame(alignment: .topLeading)` | `Modifier.frame(alignment = Alignment.topLeading)` |
| `.listStyle(.insetGrouped)` | `Modifier.listStyle(ListStyle.insetGrouped)` |
| `.buttonStyle(.bordered)` | `Modifier.buttonStyle(PrimitiveButtonStyle.bordered)` |
| `.toggleStyle(.switch)` | `Modifier.toggleStyle(ToggleStyle.switch)` |
| `.textFieldStyle(.roundedBorder)` | `Modifier.textFieldStyle(TextFieldStyle.roundedBorder)` |
| `.animation(.spring, value: x)` | `Modifier.animation(Animation.spring, value = x)` |
| `.clipShape(.capsule)` | `Modifier.clipShape(Capsule())` or `Shape.capsule` |
| `.background(.ultraThinMaterial)` | `Modifier.background(ShapeStyle.ultraThinMaterial)` |
| `Text(date, style: .relative)` | `Text(date = epochSeconds, style = Text.DateStyle.relative)` |

Nested Swift types keep their nesting: `Font.Weight.bold`, `Text.DateStyle.relative`, `Edge.Set.horizontal`,
`Axis.Set.vertical`. Static factories are companion functions (`Font.system(size = 17.0)`,
`Animation.spring(duration = 0.4)`), initializers are constructors (`Color(red = 1.0, green = 0.5, blue = 0.0)`,
`RoundedRectangle(cornerRadius = 8.0)`), and instance methods that return a new value are member functions
(`Color.red.opacity(0.5)`, `Font.body.bold()`, `Animation.easeIn.delay(0.2)`).

```kotlin
Text(
    "Styled",
    modifier = Modifier
        .font(Font.system(size = 20.0, weight = Font.Weight.semibold, design = Font.Design.rounded))
        .foregroundStyle(Color(red = 0.9, green = 0.3, blue = 0.2))
        .padding(Edge.Set.horizontal, 12.0)
        .background(Color.orange.opacity(0.15), Capsule()),
)
```

Option sets (`Edge.Set`, `Axis.Set`…) are classes with the predefined values as constants (`Edge.Set.horizontal`,
`Edge.Set.all`) and a `rawValue` constructor. Swift's array literal `[.top, .leading]` is written with `+` (or
`union`): `Edge.Set.top + Edge.Set.leading`. This works for `Edge.Set`, `Axis.Set`, `HorizontalEdge.Set`,
`VerticalEdge.Set`, `EventModifiers`, `SafeAreaRegions`, `ContentShapeKinds`, `AccessibilityTraits`,
`AccessibilityTechnologies`, `MatchedGeometryProperties`, `GestureMask` and `SearchSuggestionsPlacement.Set`.

```kotlin
Text("Inset", modifier = Modifier.padding(Edge.Set.top + Edge.Set.leading, 12.0))
```

## ShapeStyle and Color

`ShapeStyle` parameters (`foregroundStyle`, `background`, `fill`, `stroke`, `tint`) accept any style value: a `Color`,
a gradient, a `Material`, or one of the hierarchical styles on `ShapeStyle` (`primary`, `secondary`, `tertiary`,
`tint`, `separator`, `background`…). Colors exist both as `Color.red` and `ShapeStyle.red`, matching SwiftUI.

```kotlin
VStack(spacing = 8.0) {
    Text("Primary", modifier = Modifier.foregroundStyle(ShapeStyle.primary))
    Text("Tinted", modifier = Modifier.foregroundStyle(ShapeStyle.tint))
    Text(
        "Gradient",
        modifier = Modifier.foregroundStyle(
            ShapeStyle.linearGradient(colors = listOf(Color.purple, Color.blue), startPoint = UnitPoint.leading, endPoint = UnitPoint.trailing),
        ),
    )
}
```

Asset catalog colors are `Color(name = "BrandBlue")`. A color sent back by SwiftUI (from a `ColorPicker`) arrives as
`Color(red, green, blue, opacity)` in sRGB.

## Optional parameters and defaults

- A Swift parameter with a default value has the same default in Kotlin: `VStack { }` uses SwiftUI's default
  alignment and spacing.
- A Swift optional (`spacing: CGFloat? = nil`) is a nullable Kotlin parameter with `= null`.
- An optional closure (`onDismiss: (() -> Void)? = nil`) is a nullable function type.

Some SwiftUI views and modifiers have initializers that differ only by labels or types Kotlin can't distinguish. For
those, the API also has a combined overload where every parameter is nullable and the initializer is chosen from the
arguments you pass, for example `Button(titleKey = …, systemImage = …, role = …, action = …)`. Passing a combination
no SwiftUI initializer accepts throws at run time with a message naming the view, so prefer the specific overloads
when they exist.

## Overloads Kotlin can't tell apart

Two Swift overloads with the same parameter types but different labels would clash in Kotlin. They are kept, under a
name derived from the distinguishing label:

| Swift | Kotlin |
|---|---|
| `Font.system(size:weight:design:)` with optional weight and design | `Font.systemSize(size, weight, design)` |
| `Font.custom(_:fixedSize:)` | `Font.customName(name, fixedSize)` |
| `NavigationLink(_:tag:selection:)`, `NavigationLink(_:isActive:)` | `NavigationLinkTag(…)`, `NavigationLinkIsActive(…)` |
| `Image(_:variableValue:bundle:)` | `ImageVariableValue(name, variableValue)` |
| `Slider(value:in:step:label:onEditingChanged:)` | `SliderValue(value, bounds, step, onValueChange) { … }` |
| `.rect(uniformLeadingCorners:uniformTrailingCorners:)` | `Shape.rectUniformLeadingCorners(…)` |
| `ConcentricRectangle(…uniformBottomCorners:)` | `ConcentricRectangle.uniformBottomCorners(…)` |

If autocompletion shows a name with a suffix like these, it is the same Swift initializer; the KDoc shows the exact
Swift declaration.

## Kotlin-specific forms

Some SwiftUI features depend on Swift closures, property wrappers, key paths or Swift types. They have hand-written
APIs with a Kotlin shape; the main ones:

| SwiftUI | Compose SwiftUI |
|---|---|
| `NavigationStack(path:)` + `navigationDestination(for:)` | `NavigationStack(path, onPathChange, destination = { value -> }) { … }` |
| `TabView(selection:)` + `Tab(_:systemImage:value:)` | `TabView(selection, onSelectionChange) { Tab("Home", systemImage = SFSymbol.house, value = AppTab.Home) { … } }` |
| `.toolbar { ToolbarItem(placement:) { … } }` | `Modifier.toolbarItem(ToolbarItemPlacement.primaryAction) { … }` |
| `sheet(item:)`, `fullScreenCover(item:)`, `popover(item:)` | `Modifier.sheet(item, onDismiss) { item -> }`, same for the others |
| `ForEach(data).onDelete { }.onMove { }` | `ForEach(items, key = { it.id }, onDelete = { indices -> }, onMove = { indices, to -> }) { }` |
| `List(selection: $id)` | `List(selectedKey, onSelectedKeyChange) { … }`, rows marked `Modifier.tag(key)` |
| `@Environment(\.colorScheme)`, `\.dismiss`… | `EnvironmentReader { env -> }` |
| `@FocusState` + `.focused($x)` | `Modifier.focused(isFocused, onFocusChange)` |
| `withAnimation(.spring) { … }` | `withAnimation(Animation.spring) { … }` |
| `GeometryReader { proxy in … }` | `GeometryReader { size -> }` |
| `Text` concatenation, `AttributedString` | `Text(buildStyledString { … })` |
| `Canvas { context, size in … }` | `Canvas { … }` with a Kotlin draw scope |
| Custom `Layout`, custom `Shape` | `CustomLayout(policy) { … }`, `PathShape { moveTo(…); lineTo(…) }` |
| Custom `ButtonStyle` | `Modifier.buttonStyle { isPressed -> StyleLabel(…) }` |
| A Swift view (`UIViewRepresentable`, a third-party view) | `NativeView("key")` + `ComposeRegistry.nativeViews["key"]` in Swift |

[Kotlin APIs](/kotlin-apis.html) lists all of them with examples. The chapters of this guide introduce each one where
it belongs (navigation, presentation, drawing, gestures…).

## Finding an API

1. Find the SwiftUI API you want in Apple's documentation, as you would for a Swift app.
2. Look it up in the [reference](/reference.html): every supported SwiftUI symbol has a page with its Swift
   declaration, the Kotlin form and a link to Apple's page. The [Kotlin APIs](/kotlin-apis.html) page covers the
   hand-written ones.
3. Or type the name in the IDE: the KDoc of each generated function starts with the SwiftUI symbol it maps
   (`SwiftUI View.glassEffect(_:in:)`), shows the Swift declaration, and ends with its availability per platform, so
   searching for a Swift label or name works.
4. If an API seems missing, check
   [COVERAGE.md](https://github.com/NucleusFramework/ComposeSwiftUI/blob/main/docs/COVERAGE.md): it classifies every
   entry of Apple's SwiftUI documentation as generated, covered by another overload, or not applicable, with the reason
   and the Kotlin replacement.

Availability is part of the KDoc (`iOS 26.0+, macOS 26.0+`, or `watchOS: unavailable (no-op)`). Calling a newer API on
an older OS is safe: the modifier is skipped and the view renders as an empty view. When the fallback must look
different, branch on the OS version yourself; see [Platforms](platforms.md).
