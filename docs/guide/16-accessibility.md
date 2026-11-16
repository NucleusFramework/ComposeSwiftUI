<!-- group: Building screens -->
# Accessibility

> VoiceOver labels, traits and actions, element grouping, rotors, accessibility focus, Dynamic Type and reduced motion, and how to test them.

Compose SwiftUI renders real SwiftUI views, so the accessibility tree is SwiftUI's: a `Button` is a button for
VoiceOver, a `Toggle` reports its state, `Text` scales with Dynamic Type, and Voice Control, Switch Control and Full
Keyboard Access work without extra code. This chapter covers what you add on top, with the same modifiers as in
SwiftUI.

## Labels, values and hints

```kotlin
@Composable
fun RatingView(stars: Int) {
    HStack(
        modifier = Modifier
            .accessibilityElement(children = AccessibilityChildBehavior.ignore)
            .accessibilityLabel("Rating")
            .accessibilityValue("$stars out of 5")
            .accessibilityHint("Swipe up or down to change"),
    ) {
        for (i in 1..5) {
            Image(systemName = if (i <= stars) SFSymbol.starFill else SFSymbol.star)
        }
    }
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `.accessibilityLabel("Close")` | `Modifier.accessibilityLabel("Close")` |
| `.accessibilityLabel(Text("close.label"))` (localized) | `Modifier.accessibilityLabel(LocalizedKey("close.label"))` |
| `.accessibilityValue("50%")` | `Modifier.accessibilityValue("50%")` |
| `.accessibilityHint("Opens the profile")` | `Modifier.accessibilityHint("Opens the profile")` |
| `.accessibilityInputLabels(["Close", "Dismiss"])` | `Modifier.accessibilityInputLabels(listOf("Close", "Dismiss"))` (Voice Control) |
| `.accessibilityIdentifier("closeButton")` | `Modifier.accessibilityIdentifier("closeButton")` (UI tests) |
| `.accessibilityHidden(true)` | `Modifier.accessibilityHidden(true)` |
| `.accessibilitySortPriority(1)` | `Modifier.accessibilitySortPriority(1.0)` |

A plain `String` is read verbatim; a `LocalizedKey` is looked up in the app's string tables, as for `Text`. Most of
these modifiers also have an `isEnabled` overload, to apply them conditionally without changing the modifier chain.

Icon-only buttons need a label. `Label` (or `Button(title, systemImage)`) gives VoiceOver the title even when the
style shows only the icon:

```kotlin
HStack {
    Button(action = {}) { Label("Delete", systemImage = SFSymbol.trash) }
    Button(action = {}, modifier = Modifier.accessibilityLabel("Share")) {
        Image(systemName = SFSymbol.squareAndArrowUp)
    }
}
```

## Traits and headings

```kotlin
VStack(alignment = HorizontalAlignment.leading) {
    Text("Settings", modifier = Modifier.font(Font.title).accessibilityAddTraits(AccessibilityTraits.isHeader))
    Text("Account", modifier = Modifier.accessibilityHeading(AccessibilityHeadingLevel.h2))
    Text("Selected plan: Pro", modifier = Modifier.accessibilityAddTraits(AccessibilityTraits.isSelected))
    // A tappable card that is not a Button: tell VoiceOver it acts like one.
    Text(
        "Open details",
        modifier = Modifier
            .onTapGesture { }
            .accessibilityAddTraits(AccessibilityTraits.isButton)
            .accessibilityRemoveTraits(AccessibilityTraits.isStaticText),
    )
}
```

Combine traits with `+` (or `union`): `AccessibilityTraits.isButton + AccessibilityTraits.isSelected`. Prefer a real
`Button` over `onTapGesture` + `isButton`: it is focusable, works with Full Keyboard Access and the tvOS focus engine,
and gets the right traits for free.

## Grouping elements

`accessibilityElement(children)` controls how a container appears to assistive technologies:

- `AccessibilityChildBehavior.combine`: one element whose label joins the children's labels. Good for a row of an
  icon, a title and a subtitle.
- `AccessibilityChildBehavior.ignore`: one element that hides its children; give it your own label and value.
- `AccessibilityChildBehavior.contain`: keeps the children as separate elements, grouped for navigation.

```kotlin
data class Message(val id: String, val sender: String, val preview: String, val unread: Boolean)

@Composable
fun MessageRow(message: Message) {
    HStack(modifier = Modifier.accessibilityElement(children = AccessibilityChildBehavior.combine)) {
        if (message.unread) {
            ShapeView(Circle(), fill = Color.blue, modifier = Modifier.frame(width = 8.0, height = 8.0).accessibilityHidden(true))
        }
        VStack(alignment = HorizontalAlignment.leading) {
            Text(message.sender, modifier = Modifier.font(Font.headline))
            Text(message.preview, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
        }
    }
}
```

`accessibilityChildren { }` replaces the children VoiceOver sees with other views, and
`accessibilityRepresentation { }` makes a custom view behave like a standard control for accessibility:

```kotlin
var volume by remember { mutableStateOf(0.5) }
Canvas(
    modifier = Modifier
        .frame(width = 200.0, height = 24.0)
        .accessibilityRepresentation {
            Slider(value = volume, bounds = 0.0..1.0, onValueChange = { volume = it }) { Text("Volume") }
        },
) {
    fill(Color.gray.opacity(0.3)) { rect(0.0, 0.0, width, height) }
    fill(Color.accentColor) { rect(0.0, 0.0, width * volume, height) }
}
```

## Actions

Custom actions appear in the VoiceOver actions rotor, so swipe or long-press interactions stay reachable:

```kotlin
@Composable
fun MailRow(subject: String, onArchive: () -> Unit, onFlag: () -> Unit, onOpen: () -> Unit) {
    Text(
        subject,
        modifier = Modifier
            .onTapGesture { onOpen() }
            .accessibilityAction { onOpen() }
            .accessibilityAction(named = "Archive") { onArchive() }
            .accessibilityAction(named = "Flag") { onFlag() },
    )
}
```

`accessibilityAction(actionKind)` handles the standard kinds: `AccessibilityActionKind.default` (double tap),
`escape` (two-finger scrub, to dismiss), `magicTap` (two-finger double tap, the screen's main action), `delete` and
`showMenu`. `accessibilityActions { }` declares several actions as `Button`s at once.

### Adjustable and scroll actions

An adjustable element responds to VoiceOver's swipe up and down, like a slider:

```kotlin
@Composable
fun StarRating(stars: Int, onChange: (Int) -> Unit) {
    HStack(
        modifier = Modifier
            .accessibilityElement(children = AccessibilityChildBehavior.ignore)
            .accessibilityLabel("Rating")
            .accessibilityValue("$stars stars")
            .accessibilityAdjustableAction { direction ->
                when (direction) {
                    AccessibilityAdjustmentDirection.increment -> onChange(minOf(stars + 1, 5))
                    AccessibilityAdjustmentDirection.decrement -> onChange(maxOf(stars - 1, 0))
                }
            },
    ) {
        for (i in 1..5) Image(systemName = if (i <= stars) SFSymbol.starFill else SFSymbol.star)
    }
}
```

`accessibilityScrollAction { edge -> }` handles the three-finger scroll on a custom pager, and
`accessibilityZoomAction { direction, location -> }` the zoom gesture (`+1` in, `-1` out).

## Rotors

A rotor lets VoiceOver users jump between items of one kind (headings, unread messages, errors). Mark the targets
with `Modifier.id(…)` and list them in `accessibilityRotor` on the scroll view:

```kotlin
@Composable
fun Inbox(messages: List<Message>) {
    ScrollView(
        modifier = Modifier.accessibilityRotor(
            "Unread",
            messages.filter { it.unread }.map { RotorEntry(id = it.id, label = it.sender) },
        ),
    ) {
        LazyVStack(alignment = HorizontalAlignment.leading) {
            for (message in messages) {
                MessageRow(message, modifier = Modifier.id(message.id))
            }
        }
    }
}

@Composable
fun MessageRow(message: Message, modifier: Modifier) {
    Text("${message.sender}: ${message.preview}", modifier = modifier)
}
```

For entries outside the scroll view, `Modifier.accessibilityRotorEntry(id, namespace)` with a `NamespaceScope` marks
a view as a rotor target. Rotors built from Swift `AccessibilityRotorContent` (system rotors, ranges of text) are not
available.

## Accessibility focus

`Modifier.accessibilityFocused(isFocused, onFocusChange)` is SwiftUI's `@AccessibilityFocusState`, driven by Kotlin
state. Set it to move VoiceOver's cursor, for example to an error after a failed submit:

```kotlin
@Composable
fun SignUpForm(onSubmit: () -> String?) {
    var error by remember { mutableStateOf<String?>(null) }
    var errorFocused by remember { mutableStateOf(false) }
    Form {
        error?.let {
            Text(
                it,
                modifier = Modifier
                    .foregroundStyle(Color.red)
                    .accessibilityFocused(errorFocused) { focused -> errorFocused = focused },
            )
        }
        Button("Create account") {
            error = onSubmit()
            errorFocused = error != null
        }
    }
}
```

Starting with `true` is the Kotlin form of `accessibilityDefaultFocus`: the element gets VoiceOver focus when it
appears.

## Linked groups and labeled pairs

Both take a namespace from `NamespaceScope`:

```kotlin
NamespaceScope { ns ->
    Grid {
        GridRow {
            Text("Name", modifier = Modifier.accessibilityLabeledPair(AccessibilityLabeledPairRole.label, "name", ns))
            Text("Ada Lovelace", modifier = Modifier.accessibilityLabeledPair(AccessibilityLabeledPairRole.content, "name", ns))
        }
        GridRow {
            Text("Email", modifier = Modifier.accessibilityLabeledPair(AccessibilityLabeledPairRole.label, "email", ns))
            Text("ada@example.com", modifier = Modifier.accessibilityLabeledPair(AccessibilityLabeledPairRole.content, "email", ns))
        }
    }
}
```

`accessibilityLabeledPair` tells VoiceOver which view labels which (useful in grids and custom forms);
`accessibilityLinkedGroup(id, ns)` links views that belong together, so VoiceOver can jump between them.

## Custom content and speech

`accessibilityCustomContent(label, value)` adds secondary details that VoiceOver reads on demand ("More content"
rotor), without making the main label long:

```kotlin
Text(
    "Flight AF 123",
    modifier = Modifier
        .accessibilityCustomContent("Gate", "K42")
        .accessibilityCustomContent("Boarding", "10:15"),
)
```

Speech modifiers tune how VoiceOver pronounces text: `speechSpellsOutCharacters()` for codes,
`speechAlwaysIncludesPunctuation()`, `speechAdjustedPitch(…)`, `speechAnnouncementsQueued()`.
`accessibilityChartDescriptor(title, xAxisTitle, yAxisTitle, values)` gives a custom chart an Audio Graph (Swift Charts
from the [Charts integration](/charts.html) have one already).

## Dynamic Type

Text styles (`Font.body`, `Font.headline`, `Font.title`…) scale with the user's text size automatically. Fixed sizes
(`Font.system(size = 17.0)`) do not: prefer text styles, and let layouts grow (avoid fixed heights on text).

Read the current size with `EnvironmentReader` to adapt the layout, for example to stack a row vertically at
accessibility sizes:

```kotlin
@Composable
fun ContactRow(name: String, phone: String) {
    EnvironmentReader { env ->
        val large = env.dynamicTypeSize.ordinalIndex() >= DynamicTypeSize.accessibility1.ordinalIndex()
        if (large) {
            VStack(alignment = HorizontalAlignment.leading) { Text(name); Text(phone) }
        } else {
            HStack { Text(name); Spacer(); Text(phone) }
        }
    }
}

private val typeSizes = listOf(
    DynamicTypeSize.xSmall, DynamicTypeSize.small, DynamicTypeSize.medium, DynamicTypeSize.large,
    DynamicTypeSize.xLarge, DynamicTypeSize.xxLarge, DynamicTypeSize.xxxLarge,
    DynamicTypeSize.accessibility1, DynamicTypeSize.accessibility2, DynamicTypeSize.accessibility3,
    DynamicTypeSize.accessibility4, DynamicTypeSize.accessibility5,
)

private fun DynamicTypeSize.ordinalIndex() = typeSizes.indexOf(this)
```

`DynamicTypeSize` is a SwiftUI value, not a Kotlin enum, hence the small ordering helper. `ViewThatFits` is often
simpler: it picks the first child that fits, without reading the size at all. `EnvironmentReader` reports after the
first layout pass, so the first composition sees the default size (`large`).

To cap or force the size of part of the screen:

```kotlin
VStack {
    // A compact toolbar that must not grow past xxxLarge
    HStack(modifier = Modifier.dynamicTypeSize(max = DynamicTypeSize.xxxLarge)) {
        Text("Inbox")
    }
    // Forced size, handy in previews
    Text("Preview at AX3", modifier = Modifier.dynamicTypeSize(DynamicTypeSize.accessibility3))
}
```

`Modifier.accessibilityShowsLargeContentViewer()` shows the large content viewer (long press at accessibility sizes)
for small fixed-size controls such as tab bar items.

## Reduce motion and other preferences

SwiftUI already adapts its own system animations to Reduce Motion, but your custom `withAnimation`, `phaseAnimator`
or `keyframeAnimator` effects keep running. `EnvironmentReader` exposes the accessibility preferences: `reduceMotion`,
`reduceTransparency`, `differentiateWithoutColor`, `invertColors`, `voiceOverEnabled` and `boldText`.

```kotlin
@Composable
fun ExpandingCard() {
    var expanded by remember { mutableStateOf(false) }
    EnvironmentReader { env ->
        Button(if (expanded) "Collapse" else "Expand") {
            withAnimation(if (env.reduceMotion) null else Animation.spring) { expanded = !expanded }
        }
        if (expanded) {
            Text("Details", modifier = Modifier.background(
                if (env.reduceTransparency) Color.gray else Color.gray.opacity(0.3)))
        }
    }
}
```

`withAnimation(null) { }` applies the change without animation. Like every `EnvironmentReader` value, the preferences
arrive after the first layout pass (the first composition sees `false`). In a larger app, read them once near the
root and provide them through a `CompositionLocal`.

## Testing

- **Accessibility Inspector** (Xcode › Open Developer Tool) inspects the element tree of the simulator or a Mac app
  and runs an audit. The tree is SwiftUI's, so it looks like a native app's.
- **VoiceOver** on a device is the real test: check reading order, that every interactive element has a label, and
  that custom gestures have an action equivalent.
- **Dynamic Type**: Xcode's Environment Overrides panel, or a preview with
  `Modifier.dynamicTypeSize(DynamicTypeSize.accessibility3)` on the root (see [Previews](previews.md)).
- **UI tests**: XCUITest queries by accessibility identifier, label and trait. Set `accessibilityIdentifier` on the
  elements your tests touch:

```kotlin
Button("Log in", modifier = Modifier.accessibilityIdentifier("login.submit")) { }
```

```swift
func testLogin() {
    let app = XCUIApplication()
    app.launch()
    app.buttons["login.submit"].tap()
    XCTAssertTrue(app.staticTexts["Welcome"].waitForExistence(timeout: 2))
}
```

- `performAccessibilityAudit()` in an XCUITest (iOS 17+, macOS 14+) checks contrast, labels and hit regions
  automatically.
