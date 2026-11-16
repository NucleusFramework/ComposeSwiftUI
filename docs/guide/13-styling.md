<!-- group: Building screens -->
# Styling

> Colors and shape styles, backgrounds and overlays, shapes and clipping, shadows, Liquid Glass, color schemes, tints, built-in and Kotlin-written control styles, and theming patterns.

Styling in Compose SwiftUI is SwiftUI styling: the same `ShapeStyle` values, the same modifiers, applied in the order
you chain them. There is no Material theme and no `MaterialTheme.colorScheme`; the system colors, materials and
semantic styles already adapt to light and dark mode, accessibility settings and each platform. This chapter shows the
Kotlin spelling and the few places where Kotlin needs its own API (custom control styles).

| SwiftUI | Compose SwiftUI |
|---|---|
| `.foregroundStyle(.secondary)` | `Modifier.foregroundStyle(ShapeStyle.secondary)` |
| `.background(.thinMaterial, in: .rect(cornerRadius: 12))` | `Modifier.background(ShapeStyle.thinMaterial, RoundedRectangle(cornerRadius = 12.0))` |
| `.background { Color.red }` | `Modifier.background { ColorView(Color.red) }` |
| `Color(red: 0.2, green: 0.4, blue: 0.9)` | `Color(red = 0.2, green = 0.4, blue = 0.9)` |
| `Color("Brand")` (asset catalog) | `Color(name = "Brand")` |
| `.clipShape(.capsule)` | `Modifier.clipShape(Capsule())` |
| `.glassEffect(.regular, in: .capsule)` | `Modifier.glassEffect(Glass.regular, Capsule())` |
| `.buttonStyle(.borderedProminent)` | `Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent)` |
| `struct MyStyle: ButtonStyle { … }` | `Modifier.buttonStyle { isPressed -> … StyleLabel() … }` |

## Colors and shape styles

A `ShapeStyle` is anything that can fill: a `Color`, a gradient, a material, or a semantic style such as `.secondary`.
Every modifier that takes a style (`foregroundStyle`, `background`, `overlay`, `border`, `fill`) accepts all of them.

### Colors

```kotlin
VStack {
    Text("System red", modifier = Modifier.foregroundStyle(Color.red))
    Text("Custom RGB", modifier = Modifier.foregroundStyle(Color(red = 0.15, green = 0.45, blue = 0.85)))
    Text("Display P3", modifier = Modifier.foregroundStyle(Color(colorSpace = Color.RGBColorSpace.displayP3, red = 1.0, green = 0.3, blue = 0.2)))
    Text("Half transparent", modifier = Modifier.foregroundStyle(Color.blue.opacity(0.5)))
    Text("From the asset catalog", modifier = Modifier.foregroundStyle(Color(name = "Brand")))
    Text("App accent", modifier = Modifier.foregroundStyle(Color.accentColor))
}
```

The named system colors (`Color.red`, `Color.indigo`, …) are dynamic: they shift slightly between light and dark mode
and with Increase Contrast. Fixed RGB colors don't, so for brand colors prefer an asset-catalog color with light and
dark variants (`Color(name = "Brand")` looks the color up in the app bundle's asset catalog). `Color.mix(...)` blends
two colors (iOS 18, macOS 15).

### Semantic and hierarchical styles

Semantic styles describe a role instead of a color, and resolve against the current context:

| Style | Use |
|---|---|
| `ShapeStyle.primary`, `secondary`, `tertiary`, `quaternary`, `quinary` | Text and symbol hierarchy |
| `ShapeStyle.tint` | The current tint (accent color by default) |
| `ShapeStyle.background`, `ShapeStyle.foreground` | The environment's background and foreground |
| `ShapeStyle.fill`, `ShapeStyle.separator`, `ShapeStyle.placeholder`, `ShapeStyle.link`, `ShapeStyle.selection` | System fills and roles |
| `ShapeStyle.windowBackground` | The window background |

```kotlin
VStack(alignment = HorizontalAlignment.leading, modifier = Modifier.foregroundStyle(Color.indigo)) {
    Text("Title")
    Text("Subtitle", modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
}
```

`ShapeStyle.secondary` is relative to the current foreground style: here it is a lighter indigo, not gray. That is why
setting `foregroundStyle` once on a container and using hierarchical styles inside keeps a consistent look.
`foregroundStyle(primary, secondary)` and `foregroundStyle(primary, secondary, tertiary)` set the levels explicitly;
multicolor SF Symbols use them for their layers.

### Gradients

```kotlin
val sunrise = LinearGradient(colors = listOf(Color.orange, Color.pink), startPoint = UnitPoint.topLeading, endPoint = UnitPoint.bottomTrailing)
VStack(spacing = 12.0) {
    Text("Linear", modifier = Modifier.padding().background(sunrise, RoundedRectangle(cornerRadius = 10.0)))
    Text(
        "Radial",
        modifier = Modifier.padding().background(
            RadialGradient(colors = listOf(Color.yellow, Color.clear), center = UnitPoint.center, startRadius = 0.0, endRadius = 60.0),
        ),
    )
    Text(
        "Angular",
        modifier = Modifier.padding().background(AngularGradient(colors = listOf(Color.red, Color.blue, Color.red), center = UnitPoint.center)),
    )
    Text(
        "Stops",
        modifier = Modifier.foregroundStyle(
            LinearGradient(
                stops = listOf(Gradient.Stop(Color.blue, 0.0), Gradient.Stop(Color.purple, 0.7)),
                startPoint = UnitPoint.leading,
                endPoint = UnitPoint.trailing,
            ),
        ),
    )
}
```

To show a gradient as a view on its own, use `LinearGradientView(gradient)` (and the matching `…View` for the other
kinds). `MeshGradient` is covered in [Drawing and animation](drawing-and-animation.md).

### Materials

Materials are translucent blurs of what is behind them. They adapt to light and dark mode and keep text readable over
images and maps:

```kotlin
ZStack(alignment = Alignment.bottom) {
    Image(name = "Mountains", label = "Mountains", modifier = Modifier.resizable().scaledToFill())
    Text("Mont Blanc", modifier = Modifier.padding().background(ShapeStyle.ultraThinMaterial, Capsule()).padding())
}
```

From thinnest to thickest: `ultraThinMaterial`, `thinMaterial`, `regularMaterial`, `thickMaterial`,
`ultraThickMaterial`, plus `ShapeStyle.bar` for toolbar-like surfaces. Text on a material automatically gets a
vibrant treatment when it uses hierarchical styles (`secondary`, …).

## Foreground, background and overlay

- `Modifier.foregroundStyle(style)` colors text, symbols and shapes inside, and is inherited by children.
- `Modifier.background(style)` fills behind the view, extending under the safe area by default when it reaches it.
- `Modifier.background(style, shape)` fills a shape behind the view: the common "card" or "pill".
- `Modifier.background { … }` and `Modifier.overlay { … }` put any composable behind or in front, sized to the view.
- `Modifier.overlay(style, shape)` fills a shape in front.

```kotlin
@Composable
fun AvatarBadge(initials: String, unread: Int) {
    Text(
        initials,
        modifier = Modifier
            .font(Font.title2)
            .frame(width = 56.0, height = 56.0)
            .background(Color.teal, Circle())
            .foregroundStyle(Color.white)
            .overlay(alignment = Alignment.topTrailing) {
                if (unread > 0) {
                    Text("$unread", modifier = Modifier.font(Font.caption2).padding(4.0).background(Color.red, Capsule()))
                }
            },
    )
}
```

Order matters exactly as in SwiftUI: `padding().background(…)` colors the padded area, `background(…).padding()` adds
transparent space around the colored view.

## Shapes, clipping and borders

Shapes are values (`Circle()`, `Capsule()`, `Rectangle()`, `Ellipse()`, `RoundedRectangle(cornerRadius = 12.0)`,
`UnevenRoundedRectangle(topLeadingRadius = 16.0, topTrailingRadius = 16.0)`) that you pass to modifiers. To draw one as
a view, use `ShapeView`:

```kotlin
HStack(spacing = 16.0) {
    ShapeView(Circle(), fill = Color.green, modifier = Modifier.frame(width = 24.0, height = 24.0))
    ShapeView(RoundedRectangle(cornerRadius = 8.0), stroke = Color.gray, lineWidth = 2.0, modifier = Modifier.frame(width = 60.0, height = 30.0))
    ShapeView(Circle(), stroke = Color.blue, lineWidth = 4.0, trimFrom = 0.0, trimTo = 0.7, modifier = Modifier.frame(width = 30.0, height = 30.0))
}
```

Clipping and borders:

```kotlin
Image(name = "Portrait", label = "Portrait", modifier = Modifier
    .resizable()
    .scaledToFill()
    .frame(width = 80.0, height = 80.0)
    .clipShape(Circle())
    .overlay { ShapeView(Circle(), stroke = Color.white, lineWidth = 2.0) }
    .shadow(color = Color.black.opacity(0.25), radius = 6.0, y = 3.0))
```

- `clipShape(shape)` clips content to any shape; `clipped()` clips to the frame.
- `cornerRadius(r)` still works but is deprecated by Apple; prefer `clipShape(RoundedRectangle(cornerRadius = r))`.
- `border(style, width)` draws a rectangular border inside the frame. For a rounded or circular border, overlay a
  stroked `ShapeView` as above.
- `shadow(color, radius, x, y)` shadows everything drawn by the view; on a container, apply it to a background shape
  instead of the whole stack, or every child gets its own shadow.
- `contentShape(shape)` changes the hit-testing area without changing how the view looks.
- `mask { … }` masks with any composable, using its alpha.

Custom shapes (`PathShape { … }`) and canvas drawing are in [Drawing and animation](drawing-and-animation.md).

## Liquid Glass

On iOS 26, macOS 26 and the other 26 releases, `Modifier.glassEffect(glass, shape)` gives a view the Liquid Glass
material: a refractive, light-reacting surface used by the system bars and controls.

```kotlin
HStack(spacing = 16.0, modifier = Modifier.padding().glassEffect(Glass.regular, Capsule())) {
    Image(systemName = SFSymbol.playFill)
    Text("Now playing")
}
```

- `Glass.regular` is the standard glass, `Glass.clear` is more transparent (for rich backgrounds), `Glass.identity`
  disables the effect (useful to toggle it without changing the view tree).
- `Glass.regular.tint(Color.blue)` tints it; `Glass.regular.interactive()` makes it react to touches (scale, shimmer),
  for custom controls.
- Glass can't sample other glass. Group nearby glass views in a `GlassEffectContainer(spacing = …)` so they render
  together and blend into each other when close.
- Buttons get glass with `Modifier.buttonStyle(PrimitiveButtonStyle.glass)` and
  `PrimitiveButtonStyle.glassProminent`.

On older systems the modifier is a no-op, so the view shows without glass: give it a fallback background if it needs
one. A simple approach is to branch on your own availability flag (for example passed from Swift with
`#available`) and use a material on older systems.

### Morphing glass with glassEffectID and glassEffectUnion

Inside a `GlassEffectContainer`, glass views with an id morph into each other when they appear and disappear. Ids need
a namespace, which `NamespaceScope` provides:

```kotlin
@Composable
fun GlassToolbar() {
    var expanded by remember { mutableStateOf(false) }
    NamespaceScope { ns ->
        GlassEffectContainer(spacing = 20.0) {
            HStack(spacing = 20.0) {
                Button("Edit", systemImage = SFSymbol.pencil, modifier = Modifier.glassEffect(Glass.regular.interactive(), Circle()).glassEffectID("edit", ns)) {
                    withAnimation { expanded = !expanded }
                }
                if (expanded) {
                    Button("Share", systemImage = SFSymbol.squareAndArrowUp, modifier = Modifier.glassEffect(Glass.regular.interactive(), Circle()).glassEffectID("share", ns)) {}
                }
            }
        }
    }
}
```

Change the state inside `withAnimation` to get the morph. `Modifier.glassEffectUnion(id, ns)` merges several views into
one glass shape (for example a segmented group of buttons), and `glassEffectTransition(…)` picks how a glass view
enters and leaves.

## Color scheme

- `Modifier.preferredColorScheme(ColorScheme.dark)` forces the scheme of the whole presentation (the window, or the
  sheet it is in). `null` follows the system.
- `Modifier.environment(colorScheme = ColorScheme.dark)` changes the scheme for one subtree only, for example a dark
  card in a light screen.
- To read the current scheme, use `EnvironmentReader`:

```kotlin
EnvironmentReader { env ->
    val shadowOpacity = if (env.isDark) 0.0 else 0.2
    Text("Card", modifier = Modifier.padding().background(ShapeStyle.background, RoundedRectangle(cornerRadius = 12.0)).shadow(color = Color.black.opacity(shadowOpacity), radius = 8.0))
}
```

Most of the time you don't need to read it: system colors, materials and asset-catalog colors adapt by themselves.
Read it only for decisions colors can't express (a different image, no shadow in dark mode).

## Tint and accent color

`Modifier.tint(color)` sets the accent of controls inside: buttons, toggles, sliders, links, the selection in lists,
`ShapeStyle.tint`. Put it high in the tree to theme a whole screen, or on one control:

```kotlin
Form(modifier = Modifier.tint(Color.purple)) {
    Toggle("Notifications", isOn = true, onIsOnChange = {})
    Button("Sign out", role = ButtonRole.destructive) {}
    Slider(value = 0.5, bounds = 0.0..1.0, onValueChange = {})
}
```

The app-wide accent color is the `AccentColor` entry of the Xcode asset catalog; `Color.accentColor` refers to it.

## Built-in control styles

Controls take SwiftUI's styles through their style modifiers, which apply to every matching control inside:

| Modifier | Values |
|---|---|
| `buttonStyle(PrimitiveButtonStyle.…)` | `automatic`, `bordered`, `borderedProminent`, `borderless`, `plain`, `glass`, `glassProminent`, `link` (macOS), `card` (tvOS) |
| `buttonBorderShape(ButtonBorderShape.…)` | `capsule`, `circle`, `roundedRectangle`, `roundedRectangle(radius)` |
| `controlSize(ControlSize.…)` | `mini`, `small`, `regular`, `large`, `extraLarge` |
| `toggleStyle(ToggleStyle.…)` | `automatic`, `switch`, `button`, `checkbox` (macOS) |
| `labelStyle(LabelStyle.…)` | `automatic`, `titleAndIcon`, `iconOnly`, `titleOnly` |
| `pickerStyle`, `textFieldStyle`, `progressViewStyle`, `gaugeStyle`, `menuStyle`, `groupBoxStyle`, `listStyle`, `tableStyle` | See [Controls](controls.md) and [Lists](lists.md) |

```kotlin
HStack(modifier = Modifier.controlSize(ControlSize.large)) {
    Button("Cancel", modifier = Modifier.buttonStyle(PrimitiveButtonStyle.bordered)) {}
    Button("Continue", modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderedProminent)) {}
}
Label("Favorites", systemImage = SFSymbol.starFill, modifier = Modifier.labelStyle(LabelStyle.iconOnly))
Toggle("Bold", isOn = true, onIsOnChange = {}, modifier = Modifier.toggleStyle(ToggleStyle.button))
```

## Custom styles written in Kotlin

SwiftUI's custom styles are protocols (`ButtonStyle`, `ToggleStyle`, `LabelStyle`…) implemented in Swift. In Kotlin
you pass the style's body as a composable lambda instead. Inside it, placeholders stand for the parts of the styled
control:

- `StyleLabel()`: the control's label (the button title, the toggle label, the label title).
- `StyleIcon()`: the label's icon, in a `labelStyle`.
- `StyleContent()`: the content of a group box, labeled content, control group, form or menu.
- `StyleProgressFill()`: the completed part of a progress view.

Modifiers on a placeholder style that part. The style applies to every matching control inside the modified view,
like SwiftUI's environment-based styles.

### Button style

The body receives `isPressed`:

```kotlin
fun Modifier.pillButtonStyle(color: Color): Modifier = buttonStyle { isPressed ->
    StyleLabel(
        Modifier
            .font(Font.headline)
            .padding(EdgeInsets(top = 10.0, leading = 20.0, bottom = 10.0, trailing = 20.0))
            .foregroundStyle(Color.white)
            .background(color.opacity(if (isPressed) 0.7 else 1.0), Capsule())
            .scaleEffect(if (isPressed) 0.96 else 1.0),
    )
}

@Composable
fun Checkout() {
    VStack(spacing = 12.0, modifier = Modifier.pillButtonStyle(Color.indigo)) {
        Button("Pay now") {}
        Button("Add to wishlist") {}
    }
}
```

Both states are composed once, up front, and SwiftUI switches between them, so one style costs one composition however
many buttons use it. The body is a style, not per-button code: it can't read which button it is drawing, so pass
anything else (like the color) as a parameter of your extension, as above.

### Toggle style

The body receives `isOn`; tapping anywhere on it toggles:

```kotlin
fun Modifier.checklistToggleStyle(): Modifier = toggleStyle { isOn ->
    HStack {
        Image(
            systemName = if (isOn) SFSymbol.checkmarkCircleFill else SFSymbol.circle,
            modifier = Modifier.foregroundStyle(if (isOn) ShapeStyle.tint else ShapeStyle.secondary),
        )
        StyleLabel(Modifier.strikethrough(isOn))
    }
}

@Composable
fun Checklist() {
    var milk by remember { mutableStateOf(false) }
    VStack(alignment = HorizontalAlignment.leading, modifier = Modifier.checklistToggleStyle()) {
        Toggle("Milk", isOn = milk, onIsOnChange = { milk = it })
    }
}
```

### Label style

The body arranges `StyleIcon()` and `StyleLabel()` (the title):

```kotlin
fun Modifier.badgeLabelStyle(): Modifier = labelStyle {
    VStack(spacing = 4.0) {
        StyleIcon(Modifier.font(Font.title2).foregroundStyle(ShapeStyle.tint))
        StyleLabel(Modifier.font(Font.caption))
    }
}
```

The same pattern exists for `groupBoxStyle { StyleLabel(); StyleContent() }`, `labeledContentStyle`,
`controlGroupStyle`, `formStyle`, `menuStyle` and `progressViewStyle { StyleProgressFill(); StyleLabel() }`.

## Theming patterns

There is no theme object to configure: SwiftUI's environment is the theme. A few patterns keep an app consistent
without fighting the platform.

### Reusable modifiers

The equivalent of a custom `ViewModifier` is a Kotlin extension on `Modifier`. It composes with everything else and
needs no wrapper view:

```kotlin
fun Modifier.card(): Modifier = this
    .padding()
    .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
    .background(ShapeStyle.background, RoundedRectangle(cornerRadius = 16.0))
    .shadow(color = Color.black.opacity(0.08), radius = 10.0, y = 4.0)

fun Modifier.sectionTitle(): Modifier = this
    .font(Font.headline)
    .foregroundStyle(ShapeStyle.secondary)
    .textCase(Text.Case.uppercase)
```

### Design tokens in a CompositionLocal

Brand values that SwiftUI doesn't model (spacing scale, corner radii, brand palette) fit a Kotlin
`CompositionLocal`. Composables inside presented sheets, list rows and style bodies see it too, because they are part
of the same composition:

```kotlin
data class AppTheme(
    val brand: Color = Color(name = "Brand"),
    val cornerRadius: Double = 14.0,
    val spacing: Double = 12.0,
)

val LocalAppTheme = staticCompositionLocalOf { AppTheme() }

@Composable
fun ThemedApp(content: @Composable () -> Unit) {
    val theme = AppTheme()
    CompositionLocalProvider(LocalAppTheme provides theme) {
        VStack(modifier = Modifier.tint(theme.brand).pillButtonStyle(theme.brand)) {
            content()
        }
    }
}

@Composable
fun ThemedCard(title: String) {
    val theme = LocalAppTheme.current
    Text(title, modifier = Modifier.padding(theme.spacing).background(ShapeStyle.thinMaterial, RoundedRectangle(cornerRadius = theme.cornerRadius)))
}
```

Combine the two: SwiftUI's own environment (`tint`, `foregroundStyle`, control styles, `environment(colorScheme = …)`)
for everything SwiftUI controls understand, and a `CompositionLocal` for the values only your Kotlin code reads. Keep
colors in the asset catalog when they need light and dark variants; a Kotlin `Color(red, green, blue)` is the same in
both modes.
