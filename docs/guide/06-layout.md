<!-- group: Building screens -->
# Layout

> Stacks, frames, grids, scroll views, geometry and custom layouts: SwiftUI's layout system, driven from Kotlin.

Compose SwiftUI does not have a layout engine of its own. Every container you call (`VStack`, `Grid`, `ScrollView`…)
becomes the SwiftUI container of the same name, and SwiftUI lays it out. The rules are SwiftUI's: a parent proposes a
size, the child picks its own size, and the parent places it. If you know SwiftUI layout, you already know this
chapter; if you come from Compose UI, the main differences are listed at the end.

Lengths are points, typed `Double` (SwiftUI's `CGFloat`). A `null` spacing or padding means "the platform default",
exactly like omitting the argument in Swift.

## Stacks

| SwiftUI | Compose SwiftUI |
|---|---|
| `VStack(alignment: .leading, spacing: 8) { … }` | `VStack(alignment = HorizontalAlignment.leading, spacing = 8.0) { … }` |
| `HStack(alignment: .firstTextBaseline) { … }` | `HStack(alignment = VerticalAlignment.firstTextBaseline) { … }` |
| `ZStack(alignment: .bottomTrailing) { … }` | `ZStack(alignment = Alignment.bottomTrailing) { … }` |
| `Spacer()`, `Spacer(minLength: 0)` | `Spacer()`, `Spacer(minLength = 0.0)` |
| `Divider()` | `Divider()` |

```kotlin
@Composable
fun ProfileHeader(name: String, handle: String, followers: Int) {
    HStack(alignment = VerticalAlignment.center, spacing = 12.0, modifier = Modifier.padding()) {
        ZStack(alignment = Alignment.bottomTrailing) {
            Image(systemName = SFSymbol.personCropCircleFill, modifier = Modifier.font(Font.system(size = 48.0)))
            ShapeView(Circle(), fill = Color.green, modifier = Modifier.frame(width = 12.0, height = 12.0))
        }
        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
            Text(name, modifier = Modifier.font(Font.headline))
            Text(handle, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
        }
        Spacer()
        Text("$followers", modifier = Modifier.font(Font.title3.monospacedDigit()))
    }
}
```

`Spacer` expands along the axis of its stack and is ignored by a `ZStack`. `Divider` draws a horizontal line in a
`VStack` and a vertical one in an `HStack`.

Children are emitted in order, like any composable content. A Kotlin `if` adds or removes a child; a `for` loop emits
one child per item. Wrap loop bodies in `key(id) { … }` so SwiftUI keeps each child's identity when items move (see
[Lists](lists.md)).

`Group { … }` applies modifiers to several children without adding a container:

```kotlin
VStack {
    Group {
        Text("Title")
        Text("Subtitle")
    }
    // Group's modifier is applied to each child, as in SwiftUI
}
```

## Frames and padding

Modifiers apply in the order you chain them, exactly as in Swift: `padding` then `background` paints the padding,
`background` then `padding` doesn't.

```kotlin
VStack(spacing = 16.0) {
    // fixed size
    ShapeView(RoundedRectangle(cornerRadius = 12.0), fill = Color.orange, modifier = Modifier.frame(width = 80.0, height = 80.0))

    // fill the available width, content aligned to the leading edge
    Text("Full width", modifier = Modifier
        .frame(maxWidth = Double.POSITIVE_INFINITY, alignment = Alignment.leading)
        .padding()
        .background(Color.blue.opacity(0.15)))

    // padding on some edges only
    Text("Indented", modifier = Modifier.padding(Edge.Set.horizontal, 24.0))
    Text("Exact insets", modifier = Modifier.padding(EdgeInsets(top = 4.0, leading = 8.0, bottom = 4.0, trailing = 8.0)))
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `.frame(width: 100, height: 40)` | `Modifier.frame(width = 100.0, height = 40.0)` |
| `.frame(maxWidth: .infinity)` | `Modifier.frame(maxWidth = Double.POSITIVE_INFINITY)` |
| `.padding()`, `.padding(8)` | `Modifier.padding()`, `Modifier.padding(8.0)` |
| `.padding(.horizontal, 16)` | `Modifier.padding(Edge.Set.horizontal, 16.0)` |
| `.fixedSize()`, `.fixedSize(horizontal: false, vertical: true)` | `Modifier.fixedSize()`, `Modifier.fixedSize(horizontal = false, vertical = true)` |
| `.layoutPriority(1)` | `Modifier.layoutPriority(1.0)` |
| `.aspectRatio(16/9, contentMode: .fit)` | `Modifier.aspectRatio(16.0 / 9.0, contentMode = ContentMode.fit)` |

`frame(width, height, alignment)` and `frame(minWidth, idealWidth, maxWidth, minHeight, idealHeight, maxHeight,
alignment)` are two overloads, as in SwiftUI. Pass arguments by name to pick the one you want.

When siblings compete for space, `layoutPriority` decides who shrinks last, and `fixedSize` lets a view keep its ideal
size instead of being truncated:

```kotlin
HStack {
    Text("A long title that may need to be truncated", modifier = Modifier.lineLimit(1))
    Text("Always visible", modifier = Modifier.layoutPriority(1.0))
}
```

## Positioning and layering

`offset` moves a view after layout (its siblings don't move), `position` places its center in the parent's
coordinate space, and `zIndex` changes the drawing order inside a `ZStack`.

```kotlin
ZStack {
    ShapeView(Circle(), fill = Color.blue, modifier = Modifier.frame(width = 60.0, height = 60.0).zIndex(1.0))
    ShapeView(Circle(), fill = Color.red, modifier = Modifier.frame(width = 60.0, height = 60.0).offset(x = 30.0, y = 0.0))
}
```

`overlay` and `background` take content and an alignment; they are sized by the view they decorate, which makes them
the usual tool for badges:

```kotlin
Image(systemName = SFSymbol.bell, modifier = Modifier
    .font(Font.title)
    .overlay(alignment = Alignment.topTrailing) {
        ShapeView(Circle(), fill = Color.red, modifier = Modifier.frame(width = 10.0, height = 10.0).offset(x = 4.0, y = -4.0))
    })
```

## Alignment guides

`Modifier.alignmentGuide` moves the point a view aligns on. The lambda receives the view's size and returns the
offset of the guide from the view's origin:

```kotlin
VStack(alignment = HorizontalAlignment.leading) {
    Text("Aligned on its leading edge")
    Text("Shifted right by 20 points", modifier = Modifier.alignmentGuide(HorizontalAlignment.leading) { _, _ -> -20.0 })
    Text("Centered on the stack's leading line", modifier = Modifier.alignmentGuide(HorizontalAlignment.leading) { width, _ -> width / 2 })
}
```

Custom alignments (SwiftUI's `AlignmentID`) come from `customHorizontalAlignment(index)` and
`customVerticalAlignment(index)`; the index (0 to 7) tells guides apart. A custom guide defaults to the leading (or
top) edge, and you move it on the views that should line up. This aligns the values of a form-like block on their
colons, across nested stacks:

```kotlin
val colon = customHorizontalAlignment(0)

@Composable
fun AlignedFields() {
    VStack(alignment = colon, spacing = 8.0) {
        HStack {
            Text("Name:", modifier = Modifier.alignmentGuide(colon) { width, _ -> width })
            Text("Ada Lovelace")
        }
        HStack {
            Text("Occupation:", modifier = Modifier.alignmentGuide(colon) { width, _ -> width })
            Text("Mathematician")
        }
    }
}
```

## Grids

`Grid` and `GridRow` build a table-like layout that sizes every column to its widest cell. Cells are the children of
each `GridRow`; a child placed directly in the `Grid` spans every column.

```kotlin
Grid(alignment = Alignment.leading, horizontalSpacing = 16.0, verticalSpacing = 8.0) {
    GridRow {
        Text("Plan", modifier = Modifier.font(Font.headline))
        Text("Price", modifier = Modifier.font(Font.headline).gridColumnAlignment(HorizontalAlignment.trailing))
    }
    Divider()
    GridRow {
        Text("Basic")
        Text("4.99")
    }
    GridRow {
        Text("Pro")
        Text("9.99")
    }
    GridRow {
        Text("Prices include VAT", modifier = Modifier.gridCellColumns(2).foregroundStyle(ShapeStyle.secondary))
    }
}
```

`gridCellColumns`, `gridColumnAlignment`, `gridCellAnchor` and `gridCellUnsizedAxes` work as in SwiftUI.

For a large or scrolling collection, use `LazyVGrid` (or `LazyHGrid`) inside a `ScrollView`. Columns are described by
`GridItem`s:

```kotlin
@Composable
fun PhotoGrid(photos: List<String>) {
    ScrollView {
        LazyVGrid(columns = listOf(GridItem(GridItem.Size.adaptive(minimum = 100.0))), spacing = 4.0) {
            for (url in photos) key(url) {
                AsyncImage(url, modifier = Modifier.aspectRatio(1.0, contentMode = ContentMode.fill))
            }
        }
    }
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `GridItem(.flexible())` | `GridItem(GridItem.Size.flexible())` or `GridItem()` |
| `GridItem(.fixed(80))` | `GridItem(GridItem.Size.fixed(80.0))` |
| `GridItem(.adaptive(minimum: 100), spacing: 4)` | `GridItem(GridItem.Size.adaptive(minimum = 100.0), spacing = 4.0)` |
| `Array(repeating: GridItem(), count: 3)` | `List(3) { GridItem() }` |

## Lazy stacks

`LazyVStack` and `LazyHStack` create their children only when they scroll into view. Put them in a `ScrollView`. With
a Kotlin loop, SwiftUI is lazy but Kotlin still composes every child up front; use `LazyForEach` so each row is also
composed lazily on the Kotlin side:

```kotlin
data class Message(val id: Long, val author: String, val text: String)

@Composable
fun MessageFeed(messages: List<Message>) {
    ScrollView {
        LazyVStack(alignment = HorizontalAlignment.leading, spacing = 12.0) {
            LazyForEach(messages, key = { it.id }) { message ->
                VStack(alignment = HorizontalAlignment.leading) {
                    Text(message.author, modifier = Modifier.font(Font.headline))
                    Text(message.text)
                }
            }
        }
    }
}
```

Each `LazyForEach` row should be one view (wrap several in a stack), and items should be immutable values: a row
recomposes when its item changes by `equals`. [Lists](lists.md) and [Performance](performance.md) go into the details.

`pinnedViews` keeps section headers or footers visible while scrolling:

```kotlin
ScrollView {
    LazyVStack(pinnedViews = PinnedScrollableViews.sectionHeaders) {
        Section(header = { Text("Today", modifier = Modifier.padding().background(Color.white)) }, content = {
            for (i in 1..30) key(i) { Text("Item $i") }
        })
    }
}
```

## Scroll views

`ScrollView(axes = Axis.Set.horizontal)` scrolls horizontally; the default is vertical. Indicators, insets and bounce
are modifiers:

```kotlin
ScrollView(axes = Axis.Set.horizontal, modifier = Modifier
    .scrollIndicators(ScrollIndicatorVisibility.hidden)
    .contentMargins(Edge.Set.horizontal, 20.0)) {
    LazyHStack(spacing = 12.0) {
        for (i in 1..20) key(i) {
            ShapeView(RoundedRectangle(cornerRadius = 16.0), fill = Color.indigo, modifier = Modifier.frame(width = 140.0, height = 90.0))
        }
    }
}
```

Other scroll modifiers map one to one: `scrollDisabled`, `scrollBounceBehavior`, `scrollClipDisabled`,
`scrollDismissesKeyboard`, `scrollContentBackground`, `defaultScrollAnchor`, `refreshable`.

### Snapping and paging

`scrollTargetBehavior` makes a scroll view stop on pages or on views. With `viewAligned`, mark the container whose
children are the targets with `scrollTargetLayout()`:

```kotlin
ScrollView(axes = Axis.Set.horizontal, modifier = Modifier.scrollTargetBehavior(ScrollTargetBehavior.viewAligned)) {
    LazyHStack(spacing = 16.0, modifier = Modifier.scrollTargetLayout()) {
        for (i in 1..10) key(i) {
            Text("Card $i", modifier = Modifier
                .frame(width = 280.0, height = 180.0)
                .background(Color.teal.opacity(0.3)))
        }
    }
}
```

`ScrollTargetBehavior.paging` pages by the scroll view's size instead.

### Reading and setting the scroll position

There are two ways to scroll programmatically. Both identify children with `Modifier.id(…)`; any Kotlin value with a
proper `equals` works as an id.

`scrollPosition` binds the id of the child at the top (or at `anchor`) to Kotlin state. It is a two-way binding:
SwiftUI writes it while the user scrolls, and setting it scrolls the view.

```kotlin
@Composable
fun ChapterReader(chapters: List<String>) {
    var current by remember { mutableStateOf<Any?>(chapters.first()) }
    VStack {
        Text("Reading: $current", modifier = Modifier.font(Font.caption))
        ScrollView(modifier = Modifier.scrollPosition(id = current, onIdChange = { current = it })) {
            LazyVStack(modifier = Modifier.scrollTargetLayout()) {
                for (chapter in chapters) key(chapter) {
                    Text(chapter, modifier = Modifier.frame(height = 400.0).id(chapter))
                }
            }
        }
        Button("Back to start") { withAnimation { current = chapters.first() } }
    }
}
```

`ScrollViewReader` is the imperative variant: its content receives a `ScrollProxy`, and `proxy.scrollTo(id, anchor)`
scrolls to a child. Call it from an action, inside `withAnimation` to animate:

```kotlin
ScrollViewReader { proxy ->
    ScrollView {
        LazyVStack {
            for (i in 0 until 100) key(i) { Text("Row $i", modifier = Modifier.id(i)) }
        }
    }
    Button("Bottom") { withAnimation { proxy.scrollTo(99, anchor = UnitPoint.bottom) } }
}
```

### Observing scrolling

| SwiftUI | Compose SwiftUI | Availability |
|---|---|---|
| `onScrollGeometryChange(for:of:action:)` | `Modifier.onScrollGeometryChange { metrics -> }` (`contentOffset`, `contentSize`, `containerSize`) | iOS 18, macOS 15 |
| `onScrollTargetVisibilityChange(idType:threshold:_:)` | `Modifier.onScrollTargetVisibilityChange { ids -> }` | iOS 18, macOS 15 |
| `onScrollVisibilityChange(threshold:_:)` | `Modifier.onScrollVisibilityChange { visible -> }` on a child | iOS 18, macOS 15 |
| `onScrollPhaseChange(_:)` | `Modifier.onScrollPhaseChange { old, new -> }` | iOS 18, macOS 15 |
| `scrollTransition { content, phase in … }` | `Modifier.scrollTransition { phase, isIdentity -> VisualEffects(…) }` | iOS 17, macOS 14 |

```kotlin
@Composable
fun CollapsingTitle(rows: List<String>) {
    var scrolled by remember { mutableStateOf(false) }
    ScrollView(modifier = Modifier.onScrollGeometryChange { metrics -> scrolled = metrics.contentOffset.y > 40.0 }) {
        LazyVStack {
            for (row in rows) key(row) {
                Text(row, modifier = Modifier.padding().scrollTransition { phase, _ ->
                    // phase is -1 (entering), 0 (fully visible) or 1 (leaving), interpolated
                    VisualEffects(opacity = 1 - kotlin.math.abs(phase) * 0.7, scale = 1 - kotlin.math.abs(phase) * 0.1)
                })
            }
        }
    }
}
```

Geometry callbacks fire often while scrolling. Derive a coarse value (a boolean, an index) before writing it to state,
as above, so that only real changes recompose.

## Reading sizes: GeometryReader and onGeometryChange

`GeometryReader` gives its content the size proposed to it. Unlike SwiftUI's closure, the Kotlin content receives only
the size (a `CGSize`), not a proxy:

```kotlin
GeometryReader { size ->
    HStack(spacing = 0.0) {
        ColorView(Color.green, modifier = Modifier.frame(width = size.width * 0.3))
        ColorView(Color.gray.opacity(0.2))
    }
}
```

The size is measured by SwiftUI and sent back to Kotlin, so the first composition sees `CGSize(0.0, 0.0)` and the
content recomposes once the real size arrives. Design the content so that a zero size renders sensibly (or nothing),
and keep in mind that `GeometryReader`, as in SwiftUI, takes all the space it is offered.

To observe a view's frame without changing its layout, use `Modifier.onGeometryChange`. The frame is in the global
coordinate space:

```kotlin
var headerHeight by remember { mutableStateOf(0.0) }
Text("Header", modifier = Modifier.padding().onGeometryChange { frame -> headerHeight = frame.height })
```

## ViewThatFits

`ViewThatFits` shows the first child that fits in the available space, optionally on one axis only:

```kotlin
ViewThatFits(axes = Axis.Set.horizontal) {
    HStack {
        Button("Save draft") {}
        Button("Discard") {}
        Button("Publish") {}
    }
    Menu("Actions") {
        Button("Save draft") {}
        Button("Discard") {}
        Button("Publish") {}
    }
}
```

## containerRelativeFrame

`containerRelativeFrame` sizes a view relative to its nearest container (the scroll view, or the window). The
`count`/`span` form divides the container into columns; the lambda form computes the length yourself:

```kotlin
ScrollView(axes = Axis.Set.horizontal) {
    LazyHStack(spacing = 10.0) {
        for (i in 1..12) key(i) {
            ShapeView(RoundedRectangle(cornerRadius = 12.0), fill = Color.mint, modifier = Modifier
                // three cards per screen width, 10 points between them
                .containerRelativeFrame(Axis.Set.horizontal, count = 3, span = 1, spacing = 10.0)
                .frame(height = 100.0))
        }
    }
}
```

```kotlin
Text("Half the width", modifier = Modifier.containerRelativeFrame(Axis.Set.horizontal) { length, _ -> length / 2 })
```

The lambda runs synchronously during SwiftUI layout. Keep it pure and cheap: don't read or write Compose state in it.

## Safe area

| SwiftUI | Compose SwiftUI |
|---|---|
| `.ignoresSafeArea()` | `Modifier.ignoresSafeArea()` |
| `.ignoresSafeArea(.keyboard, edges: .bottom)` | `Modifier.ignoresSafeArea(SafeAreaRegions.keyboard, Edge.Set.bottom)` |
| `.safeAreaPadding(.horizontal, 20)` | `Modifier.safeAreaPadding(Edge.Set.horizontal, 20.0)` |
| `.safeAreaInset(edge: .bottom) { … }` | `Modifier.safeAreaInset(VerticalEdge.bottom) { … }` |

`safeAreaInset` is the right way to pin a bar above or below a scroll view: the scroll content gets the extra inset,
so its last row can still scroll above the bar.

```kotlin
@Composable
fun ComposerScreen(messages: List<Message>, draft: String, onDraftChange: (String) -> Unit, onSend: () -> Unit) {
    ScrollView(modifier = Modifier.safeAreaInset(VerticalEdge.bottom) {
        HStack {
            TextField("Message", text = draft, onTextChange = onDraftChange)
            Button("Send", systemImage = SFSymbol.arrowUpCircleFill, action = onSend)
        }
    }) {
        LazyVStack(alignment = HorizontalAlignment.leading) {
            LazyForEach(messages, key = { it.id }) { Text(it.text, modifier = Modifier.padding()) }
        }
    }
}
```

`ColorView(Color.blue, modifier = Modifier.ignoresSafeArea())` is the usual full-screen background, typically as the
first child of a `ZStack`.

## Custom layouts

A SwiftUI `Layout` can be written in Kotlin: implement `LayoutPolicy` and pass it to `CustomLayout`. SwiftUI calls the
policy synchronously during layout with the size of each subview, and the policy returns its own size and the
top-leading position of each subview.

```kotlin
/** Places children left to right and wraps to a new line when the width runs out. */
class FlowLayout(private val spacing: Double = 8.0) : LayoutPolicy {
    override fun sizeThatFits(proposedWidth: Double?, proposedHeight: Double?, subviews: List<LayoutSize>): LayoutSize {
        val maxWidth = proposedWidth ?: Double.POSITIVE_INFINITY
        val places = arrange(maxWidth, subviews)
        val width = places.indices.maxOfOrNull { places[it].x + subviews[it].width } ?: 0.0
        val height = places.indices.maxOfOrNull { places[it].y + subviews[it].height } ?: 0.0
        return LayoutSize(width, height)
    }

    override fun placeSubviews(width: Double, height: Double, subviews: List<LayoutSize>): List<LayoutPlacement> =
        arrange(width, subviews)

    private fun arrange(maxWidth: Double, subviews: List<LayoutSize>): List<LayoutPlacement> {
        var x = 0.0
        var y = 0.0
        var lineHeight = 0.0
        return subviews.map { size ->
            if (x > 0 && x + size.width > maxWidth) {
                x = 0.0
                y += lineHeight + spacing
                lineHeight = 0.0
            }
            LayoutPlacement(x, y).also {
                x += size.width + spacing
                lineHeight = maxOf(lineHeight, size.height)
            }
        }
    }
}

@Composable
fun TagCloud(tags: List<String>) {
    val layout = remember { FlowLayout(spacing = 6.0) }
    CustomLayout(layout, modifier = Modifier.padding()) {
        for (tag in tags) key(tag) {
            Text(tag, modifier = Modifier
                .padding(Edge.Set.horizontal, 10.0)
                .padding(Edge.Set.vertical, 4.0)
                .background(Color.accentColor.opacity(0.15), shape = Capsule()))
        }
    }
}
```

A few points that differ from Swift's `Layout` protocol:

- Subviews are measured once, at their ideal size (an unspecified proposal). The policy can't propose a size to a
  subview; constrain children with `frame` if they must fit a width.
- Per-subview data (SwiftUI's `LayoutValueKey`) is attached with `Modifier.layoutValue("key", value)` and read in the
  policy from `LayoutSize.values["key"]`.
- `remember` the policy (or make it an `object`): a new instance is a new argument, and SwiftUI lays the container out
  again.

### Switching layouts

`AnyLayout` takes an `HStackLayout`, `VStackLayout`, `ZStackLayout` or `GridLayout` value. Changing the layout keeps
the children's identity, so their state survives and SwiftUI animates them to their new places (a Kotlin `if`
between an `HStack` and a `VStack` would recreate them):

```kotlin
@Composable
fun LayoutSwitcher(wide: Boolean) {
    AnyLayout(if (wide) HStackLayout(spacing = 8.0) else VStackLayout(spacing = 8.0), modifier = Modifier.animation(Animation.default, value = wide)) {
        Text("A")
        Text("B")
        Text("C")
    }
}
```

## Coming from Compose UI

- There are no `Modifier.fillMaxWidth()` or `weight()`: use `frame(maxWidth = Double.POSITIVE_INFINITY)` and `Spacer`,
  or `layoutPriority`.
- Stacks center their children by default (`HorizontalAlignment.center` / `VerticalAlignment.center`); Compose's
  `Column` and `Row` align to the start.
- `padding()` without arguments uses the platform's default spacing, not zero.
- `Modifier` order matters the same way, but the vocabulary is SwiftUI's: `background` and `overlay` take views,
  `clipShape` clips, `frame` sizes.
- Measurement happens in Swift. Kotlin only learns sizes through `GeometryReader`, `onGeometryChange` and the scroll
  geometry callbacks, one frame later.
