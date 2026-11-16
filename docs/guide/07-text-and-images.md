<!-- group: Building screens -->
# Text and images

> Text, fonts, localized and Markdown strings, mixed-style text built in Kotlin, labels, SF Symbols, asset and remote images.

Text and images are rendered by SwiftUI's own `Text` and `Image`, so Dynamic Type, localization, accessibility and SF
Symbol rendering behave exactly as in a Swift app. This chapter covers how to feed them from Kotlin.

## Text

`Text(String)` shows the string verbatim (SwiftUI's `Text(verbatim:)`). This is what you want for data coming from your
models: it is never looked up in a string table and never parsed as Markdown.

```kotlin
@Composable
fun Greeting(name: String, unread: Int) {
    VStack(alignment = HorizontalAlignment.leading) {
        Text("Hello, $name", modifier = Modifier.font(Font.largeTitle).bold())
        Text("$unread unread messages", modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
    }
}
```

### Fonts and text modifiers

| SwiftUI | Compose SwiftUI |
|---|---|
| `.font(.headline)` | `Modifier.font(Font.headline)` |
| `.font(.system(size: 28, weight: .bold, design: .rounded))` | `Modifier.font(Font.system(size = 28.0, weight = Font.Weight.bold, design = Font.Design.rounded))` |
| `.font(.title.weight(.semibold))` | `Modifier.font(Font.title.weight(Font.Weight.semibold))` |
| `.font(.body.monospaced())` | `Modifier.font(Font.body.monospaced())` |
| `.font(.custom("Avenir", size: 17, relativeTo: .body))` | `Modifier.font(Font.custom("Avenir", 17.0, relativeTo = Font.TextStyle.body))` |
| `.fontWeight(.medium)`, `.bold()`, `.italic()` | `Modifier.fontWeight(Font.Weight.medium)`, `Modifier.bold()`, `Modifier.italic()` |
| `.foregroundStyle(.secondary)` | `Modifier.foregroundStyle(ShapeStyle.secondary)` |
| `.lineLimit(2)`, `.lineLimit(2...4)` | `Modifier.lineLimit(2)`, `Modifier.lineLimit(2..4)` |
| `.truncationMode(.middle)` | `Modifier.truncationMode(Text.TruncationMode.middle)` |
| `.multilineTextAlignment(.center)` | `Modifier.multilineTextAlignment(TextAlignment.center)` |
| `.textSelection(.enabled)` | `Modifier.textSelection(TextSelectability.enabled)` (iOS, macOS) |
| `.monospacedDigit()`, `.kerning(1)`, `.tracking(1)` | `Modifier.monospacedDigit()`, `Modifier.kerning(1.0)`, `Modifier.tracking(1.0)` |
| `.textCase(.uppercase)` | `Modifier.textCase(Text.Case.uppercase)` |
| `.minimumScaleFactor(0.5)` | `Modifier.minimumScaleFactor(0.5)` |

Use the semantic text styles (`Font.body`, `Font.headline`…) whenever you can: they follow the user's Dynamic Type
setting. A fixed `Font.system(size = …)` does not scale; `Font.custom(name, size, relativeTo = …)` does.

Like in SwiftUI, `font`, `foregroundStyle`, `lineLimit` and the other text modifiers are inherited through the
environment, so you can set them once on a container:

```kotlin
VStack(alignment = HorizontalAlignment.leading, modifier = Modifier.font(Font.footnote).foregroundStyle(ShapeStyle.secondary)) {
    Text("Version 2.4.1")
    Text("Build 1203")
}
```

### Localized strings and Markdown

A SwiftUI string literal is a `LocalizedStringKey`: it is looked up in the app's string tables and parsed as inline
Markdown. In Kotlin, wrap the key in `LocalizedKey` (or use the `.localized` extension) to get the same behavior:

```kotlin
VStack {
    // looked up in Localizable.xcstrings of the app bundle
    Text(LocalizedKey("settings.title"))
    Text("onboarding.welcome".localized, tableName = "Onboarding")

    // a key with no translation is shown as is, parsed as Markdown
    Text(LocalizedKey("**Bold**, *italic*, `code` and [a link](https://developer.apple.com)"))
}
```

Most views with a title have a `LocalizedKey` overload (`Button(titleKey = …)`, `Label(titleKey = …)`,
`Toggle`, `Section`…), next to the `String` one that shows text verbatim.

If your strings live in Compose Multiplatform resources instead, resolve them in Kotlin and pass a `String`; see
[Interop](interop.md) for `stringResource`.

### Dates and timers

Dates cross the bridge as seconds since 1970 (`Double`), matching `Date.timeIntervalSince1970`. SwiftUI formats them
in the user's locale, and the relative, offset and timer styles update by themselves without recomposition:

```kotlin
@Composable
fun PostMeta(postedAt: Double, eventStart: Double, eventEnd: Double) {
    VStack(alignment = HorizontalAlignment.leading) {
        Text(date = postedAt, style = Text.DateStyle.relative)   // "2 hours, 5 minutes"
        Text(date = postedAt, style = Text.DateStyle.date)       // "16 November 2026"
        Text(dates = eventStart..eventEnd)                       // "10:00 - 11:30"
        Text(timerInterval = eventStart..eventEnd, countsDown = true, modifier = Modifier.monospacedDigit())
    }
}
```

SwiftUI's `Text(value, format:)` for numbers is not available: format numbers in Kotlin and pass the string. For
input, `NumberField` parses and formats numbers in Swift (see [Controls](controls.md)).

A counter that animates its digits uses a content transition:

```kotlin
var count by remember { mutableStateOf(0) }
Button(action = { withAnimation { count++ } }) {
    Text("$count", modifier = Modifier.contentTransition(ContentTransition.numericText(value = count.toDouble())))
}
```

## Mixed-style text

SwiftUI builds mixed-style text by concatenating `Text` values or with an `AttributedString`. In Kotlin, build a
`StyledString` with `buildStyledString`, the counterpart of Compose's `buildAnnotatedString`, and show it with
`Text(StyledString)`:

```kotlin
@Composable
fun PriceLine(price: String, oldPrice: String) {
    Text(buildStyledString {
        withStyle(SpanStyle(fontWeight = Font.Weight.bold, color = Color.green)) { append(price) }
        append("  ")
        withStyle(SpanStyle(strikethrough = true, color = Color.gray, fontSize = 13.0)) { append(oldPrice) }
    })
}
```

`SpanStyle` has `color`, `font` (or `fontSize` for the system font), `fontWeight`, `fontDesign`, `italic`,
`underline`, `strikethrough`, `monospaced`, `baselineOffset` and `kerning`. Nested `withStyle` blocks merge their
styles, and a property a run doesn't set comes from the `Text`'s modifiers:

```kotlin
// every run is footnote-sized; only the name is bold
Text(buildStyledString {
    withStyle(SpanStyle(fontWeight = Font.Weight.semibold)) { append("Ada") }
    append(" replied to your comment")
}, modifier = Modifier.font(Font.footnote))
```

### Inline SF Symbols

`appendSymbol` inserts an SF Symbol in the text flow (SwiftUI's `Text("\(Image(systemName:))")`). It takes the style
of the run it is in, and scales with the font:

```kotlin
@Composable
fun Rating(stars: Int, reviews: Int) {
    Text(buildStyledString {
        withStyle(SpanStyle(color = Color.orange)) {
            repeat(stars) { appendSymbol(SFSymbol.starFill) }
        }
        append(" $reviews reviews")
    })
}
```

`StyledString`s concatenate with `+` and nest with `append(styledString)`. They are plain immutable values, so you can
build them in a view model. They have no links and no Markdown: for those, use a `LocalizedKey`.

## Labels

`Label` pairs a title with an icon, and adapts to its context: lists, menus, toolbars and buttons decide whether to
show the title, the icon or both.

```kotlin
VStack(alignment = HorizontalAlignment.leading) {
    Label("Favorites", systemImage = SFSymbol.starFill)
    Label("Brand", image = "BrandIcon")             // an image from the asset catalog
    Label(title = { Text("Custom") }, icon = { ShapeView(Circle(), fill = Color.purple, modifier = Modifier.frame(width = 12.0, height = 12.0)) })
}
```

Choose what is shown with `Modifier.labelStyle(LabelStyle.iconOnly)` (`titleOnly`, `titleAndIcon`, `automatic`), or
write a custom style in Kotlin; see [Controls](controls.md).

## Images

### SF Symbols

`Image(systemName = …)` takes an `SFSymbol`. The `SFSymbol` companion has one constant per symbol of the SDK (about
8,000), named in camel case: `house.fill` is `SFSymbol.houseFill`, `01.circle` is `SFSymbol._01Circle`. Each constant's
KDoc gives its OS availability. For a symbol not in the list (custom symbols, newer SDKs), use the constructor:
`SFSymbol("my.custom.symbol")`.

Symbols are text-like: size them with `font` or `imageScale`, color them with `foregroundStyle`.

```kotlin
HStack(spacing = 20.0) {
    Image(systemName = SFSymbol.heartFill, modifier = Modifier.font(Font.title).foregroundStyle(Color.red))
    Image(systemName = SFSymbol.cloudSunRainFill, modifier = Modifier.symbolRenderingMode(SymbolRenderingMode.multicolor))
    Image(systemName = SFSymbol.personCircle, modifier = Modifier
        .symbolRenderingMode(SymbolRenderingMode.palette)
        .foregroundStyle(Color.white, Color.blue))
    Image(systemName = SFSymbol.wifi, variableValue = 0.5)   // variable symbol, half filled
    Image(systemName = SFSymbol.bell, modifier = Modifier.symbolVariant(SymbolVariants.slash).imageScale(Image.Scale.large))
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `.symbolRenderingMode(.hierarchical)` | `Modifier.symbolRenderingMode(SymbolRenderingMode.hierarchical)` |
| `.symbolVariant(.fill)` | `Modifier.symbolVariant(SymbolVariants.fill)` |
| `.imageScale(.large)` | `Modifier.imageScale(Image.Scale.large)` |
| `.symbolEffect(.pulse)` | `Modifier.symbolEffect(SymbolEffectKind.Pulse)` |
| `.symbolEffect(.bounce, value: count)` | `Modifier.symbolEffect(SymbolEffectKind.Bounce, value = count)` |
| `.contentTransition(.symbolEffect)` | `Modifier.contentTransition(ContentTransition.symbolEffect)` |

Symbol effects come in two forms. The `isActive` form runs a continuous effect while the flag is true; the `value`
form plays a discrete effect each time the value changes:

```kotlin
@Composable
fun SyncIcon(isSyncing: Boolean, lastSync: Long) {
    HStack {
        Image(systemName = SFSymbol.arrowTriangle2Circlepath, modifier = Modifier.symbolEffect(SymbolEffectKind.Rotate, isActive = isSyncing))
        Image(systemName = SFSymbol.checkmarkCircle, modifier = Modifier.symbolEffect(SymbolEffectKind.Bounce, value = lastSync))
    }
}
```

`SymbolEffectKind` has `Bounce`, `Pulse`, `VariableColor`, `Scale`, `Appear`, `Disappear`, `Wiggle`, `Rotate` and
`Breathe`, with SwiftUI's rules:

- `Wiggle`, `Rotate` and `Breathe` need iOS 18 / macOS 15 (and the matching tvOS and watchOS); they do nothing before.
- A continuous `Bounce` (`isActive`) also needs iOS 18 / macOS 15.
- The `value` form plays `Bounce`, `Pulse`, `VariableColor`, `Wiggle`, `Rotate` and `Breathe`. `Scale`, `Appear` and
  `Disappear` only exist as continuous effects.

### Asset catalog images

Images from the app's asset catalog are referenced by name. As in SwiftUI, give a label for VoiceOver, or mark the
image as decorative:

```kotlin
VStack {
    Image(name = "Hero", label = "Mountains at sunrise", modifier = Modifier.resizable().scaledToFit())
    Image(decorative = "Divider")
}
```

The asset catalog belongs to the Xcode app target, not to the Kotlin framework: add the images there. Image modifiers
follow SwiftUI:

```kotlin
Image(name = "Avatar", label = "Profile picture", modifier = Modifier
    .resizable()
    .scaledToFill()
    .frame(width = 64.0, height = 64.0)
    .clipShape(Circle()))
```

`resizable()` must come first, as in Swift: it only applies to the image itself. `renderingMode(Image.TemplateRenderingMode.template)`
tints an asset with `foregroundStyle`, and `interpolation(Image.Interpolation.none)` keeps pixel art sharp.

### Remote images: AsyncImage

`AsyncImage(url)` downloads and shows an image with SwiftUI's loader. The simple form shows a placeholder while
loading:

```kotlin
AsyncImage("https://example.com/cover.jpg", modifier = Modifier.frame(width = 120.0, height = 120.0))
```

To control each phase, pass content. It receives an `AsyncImageState` (`Empty`, `Success`, `Failure`), and
`AsyncImageContent()` stands for the loaded image, so image modifiers such as `resizable()` apply to it:

```kotlin
@Composable
fun CoverArt(url: String?) {
    AsyncImage(url, animation = Animation.default, modifier = Modifier.frame(width = 160.0, height = 160.0)) { state ->
        when (state) {
            AsyncImageState.Empty -> ProgressView()
            AsyncImageState.Success -> AsyncImageContent(modifier = Modifier.resizable().scaledToFill())
            AsyncImageState.Failure -> Image(systemName = SFSymbol.photo, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
        }
    }
}
```

The URL is a `String`; `null` stays in the `Empty` phase. `animation` animates the phase changes (SwiftUI's
`transaction`). For an authenticated download, `AsyncImage(url, headers = mapOf("Authorization" to "Bearer …"))` uses
`AsyncImage(request:)` on iOS 27 / macOS 27; earlier systems load the URL without the headers.

`AsyncImage` doesn't cache beyond `URLCache`. For a large feed with heavy images, load them in Kotlin or with a Swift
image library behind a `NativeView` (see [Interop](interop.md)).

### Platform images

A `UIImage` (iOS, tvOS) or an `NSImage` (macOS) created in Kotlin can be shown directly. These overloads live in the
platform source sets, `iosMain` and `macosMain`:

```kotlin skip
// iosMain
@Composable
fun Thumbnail(data: NSData) {
    val image = remember(data) { UIImage(data = data) }
    Image(image, modifier = Modifier.resizable().scaledToFit())
}
```

```kotlin skip
// macosMain
@Composable
fun AppIcon(path: String) {
    val image = remember(path) { NSWorkspace.sharedWorkspace.iconForFile(path) }
    Image(image, modifier = Modifier.frame(width = 32.0, height = 32.0))
}
```

The image is passed by reference, so creating it once (`remember`, or in a view model) avoids re-sending it on every
recomposition.

## Placeholders and redaction

`Modifier.redacted(RedactionReasons.placeholder)` draws text and images as gray placeholders, which is handy while
data loads. `Modifier.privacySensitive()` marks content hidden when the system redacts private data (widgets on a
locked screen, for example).

```kotlin
@Composable
fun ArticleRow(title: String?, isLoading: Boolean) {
    Text(title ?: "Loading a title of typical length", modifier = if (isLoading) Modifier.redacted(RedactionReasons.placeholder) else Modifier)
}
```
