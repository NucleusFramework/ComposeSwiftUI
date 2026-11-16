<!-- group: Building screens -->
# Lists

> Build SwiftUI lists from Kotlin data: rows and identity, sections, styles, selection, swipe actions, editing, search, tables and outlines.

A Compose SwiftUI `List` is a real SwiftUI `List`: cell reuse, separators, swipe actions, edit mode and search all come
from SwiftUI. What changes is how rows are produced. SwiftUI's data-driven initializers (`List(data)`,
`ForEach(data, id:)`) need Swift collections and key paths, so in Kotlin you feed rows from a Kotlin list with a loop,
`ForEach(items, key)` or `LazyForEach(items, key)`.

| SwiftUI | Compose SwiftUI |
|---|---|
| `List { … }` | `List { … }` |
| `ForEach(items, id: \.id) { … }` | `ForEach(items, key = { it.id }) { … }` or `for (item in items) key(item.id) { … }` |
| `List(items) { … }` with thousands of rows | `List { LazyForEach(items, key = { it.id }) { … } }` |
| `List(selection: $selected)` | `List(selectedKey, onSelectedKeyChange) { … }` + `Modifier.tag(key)` |
| `List(selection: $set)` | `List(selectedKeys, onSelectedKeysChange) { … }` + `Modifier.tag(key)` |
| `ForEach(…).onDelete { }.onMove { }` | `ForEach(items, key, onDelete = { }, onMove = { _, _ -> })` |
| `Table(rows) { TableColumn… }` | `Table(rows, key) { column("Name") { … } }` |
| `List(tree, children: \.children)` | `List { OutlineGroup(tree, key, children = { it.children }) { … } }` |

## Rows and identity

The examples on this page use a small immutable model:

```kotlin
data class Contact(val id: String, val name: String, val email: String, val favorite: Boolean = false)

data class Folder(val id: String, val name: String, val children: List<Folder>? = null)
```

The simplest list is a Kotlin loop. Wrap each row in `key(...)` so Compose keeps the identity of a row when items are
inserted, removed or reordered; SwiftUI then animates the change correctly instead of rebinding every row:

```kotlin
@Composable
fun ContactList(contacts: List<Contact>) {
    List {
        for (contact in contacts) {
            key(contact.id) {
                VStack(alignment = HorizontalAlignment.leading) {
                    Text(contact.name, modifier = Modifier.font(Font.headline))
                    Text(contact.email, modifier = Modifier.foregroundStyle(ShapeStyle.secondary))
                }
            }
        }
    }
}
```

`ForEach(items, key) { }` is the same thing written the SwiftUI way, and it is also the form that supports deleting and
moving (see below):

```kotlin
@Composable
fun ContactNames(contacts: List<Contact>) {
    List {
        ForEach(contacts, key = { it.id }) { contact ->
            Text(contact.name)
        }
    }
}
```

Keys must be stable and unique within the list. Any Kotlin value with a correct `equals`/`hashCode` works: `String`,
`Int`, enum constants, data classes.

### Long lists

A loop or `ForEach` composes every row up front, which is fine for a few hundred rows. For long lists use
`LazyForEach`: inside a `List`, each row gets its own small composition, created when SwiftUI shows the row and
disposed when it scrolls away.

```kotlin
@Composable
fun AllContacts(contacts: List<Contact>) {
    List {
        LazyForEach(contacts, key = { it.id }) { contact ->
            HStack {
                Text(contact.name)
                Spacer()
                if (contact.favorite) Image(systemName = SFSymbol.starFill, modifier = Modifier.foregroundStyle(Color.yellow))
            }
        }
    }
}
```

Rules that keep it fast:

- Each row should be one view: wrap several views in an `HStack` or `VStack`.
- A row recomposes when its item changes (by `equals`), so use immutable items such as data classes.
- The list is fastest when the `LazyForEach` is its only child. You can still put several in `Section`s.
- `LazyForEach` takes `onDelete` and `onMove` like `ForEach` (swipe to delete, reordering in edit mode).
- A row's `rememberSaveable` state survives the row scrolling off screen and back, as in a Compose `LazyColumn`.

```kotlin
@Composable
fun LongEditableList(rows: List<String>, onRowsChange: (List<String>) -> Unit) {
    List {
        LazyForEach(
            rows,
            key = { it },
            onDelete = { indices -> onRowsChange(rows.filterIndexed { i, _ -> i !in indices }) },
            onMove = { indices, to ->
                // SwiftUI's move(fromOffsets:toOffset:): `to` counts the moved rows still above it
                val moved = indices.map { rows[it] }
                val rest = rows.filterIndexed { i, _ -> i !in indices }
                val at = to - indices.count { it < to }
                onRowsChange(rest.take(at) + moved + rest.drop(at))
            },
        ) { row -> Text(row) }
    }
}
```

The [Performance](performance.md) chapter has numbers for both approaches.

## Sections, headers and footers

`Section` groups rows. Pass a plain title, or composable `header` and `footer` slots:

```kotlin
@Composable
fun SettingsList(contacts: List<Contact>) {
    List {
        Section(title = "Favorites") {
            for (c in contacts.filter { it.favorite }) key(c.id) { Text(c.name) }
        }
        Section(
            header = { Label("Everyone", systemImage = SFSymbol.person2) },
            footer = { Text("${contacts.size} contacts") },
            content = {
                for (c in contacts) key(c.id) { Text(c.name) }
            },
        )
    }
}
```

A section with `isExpanded` can be collapsed by the user (in sidebar-style lists and on macOS); the expanded state is
yours:

```kotlin
var expanded by remember { mutableStateOf(true) }
List(modifier = Modifier.listStyle(ListStyle.sidebar)) {
    Section("Library", isExpanded = expanded, onIsExpandedChange = { expanded = it }) {
        Text("Recently added")
        Text("Artists")
    }
}
```

`Modifier.headerProminence(Prominence.increased)` on a section makes its header larger.

## List styles

`Modifier.listStyle(...)` picks the look. Which styles exist depends on the platform:

| Style | Where |
|---|---|
| `ListStyle.automatic` | Everywhere (platform default) |
| `ListStyle.plain` | Everywhere |
| `ListStyle.insetGrouped` | iOS |
| `ListStyle.grouped` | iOS, tvOS |
| `ListStyle.inset` | iOS, macOS (`ListStyle.inset(alternatesRowBackgrounds = true)` on macOS) |
| `ListStyle.sidebar` | iOS, macOS |
| `ListStyle.bordered`, `ListStyle.bordered(alternatesRowBackgrounds = true)` | macOS |
| `ListStyle.carousel`, `ListStyle.elliptical` | watchOS |

```kotlin
List(modifier = Modifier.listStyle(ListStyle.insetGrouped)) {
    Text("One")
    Text("Two")
}
```

`Modifier.scrollContentBackground(Visibility.hidden)` removes the default list background so a `background` of your own
shows through (iOS, macOS, watchOS).

## Row modifiers

Row modifiers go on the row itself, not on the `List`:

```kotlin
List {
    Text("Highlighted", modifier = Modifier.listRowBackground { ColorView(Color.yellow.opacity(0.3)) })
    Text("No separator", modifier = Modifier.listRowSeparator(Visibility.hidden))
    Text("Red separator", modifier = Modifier.listRowSeparatorTint(Color.red))
    Text(
        "Custom insets",
        modifier = Modifier.listRowInsets(EdgeInsets(top = 4.0, leading = 32.0, bottom = 4.0, trailing = 8.0)),
    )
    Text("Inbox", modifier = Modifier.badge(12))
}
```

`listRowBackground` takes a view, not a color, because SwiftUI's parameter is a view: wrap a color in `ColorView`, or
put any composable there (a gradient, a shape). On the `List`, `Modifier.listRowSpacing(8.0)` and
`Modifier.listSectionSpacing(16.0)` control the spacing (iOS; `listSectionSpacing` also on watchOS).
`listRowSeparator` and `listRowSeparatorTint` are no-ops on tvOS and watchOS, where lists have no separators.

## Selection

### Single selection

`List(selectedKey, onSelectedKeyChange)` holds one selected key (or `null`). Mark each selectable row with
`Modifier.tag(key)`; tapping (or clicking) a row selects and highlights it:

```kotlin
@Composable
fun ContactPicker(contacts: List<Contact>) {
    var selectedId by remember { mutableStateOf<String?>(null) }
    List(selectedKey = selectedId, onSelectedKeyChange = { selectedId = it as String? }) {
        for (c in contacts) key(c.id) {
            Text(c.name, modifier = Modifier.tag(c.id))
        }
    }
}
```

The callback receives `Any?`: cast it back to your key type. Rows without a tag are not selectable. Selection works on
iOS and macOS; on tvOS and watchOS the list is shown without selection.

There is also a non-optional form, `List(selection, onSelectionChange)` (SwiftUI's `List(selection: Binding<Value>)`),
for when a row must always be selected, typically a macOS sidebar; it works on iOS too (single selection). Prefer the `selectedKey` form unless you need that.

Selection is the usual way to drive a `NavigationSplitView`: the sidebar list selects, the detail column shows the
selected item. See [Navigation](navigation.md).

### Multiple selection

`List(selectedKeys, onSelectedKeysChange)` holds a `Set` of keys. On macOS, Command- and Shift-click select several
rows. On iOS, multiple selection is active in edit mode, so add an `EditButton` (or drive edit mode yourself, below):

```kotlin
@Composable
fun BulkEditor(contacts: List<Contact>) {
    var selected by remember { mutableStateOf(setOf<Any>()) }
    NavigationStack {
        List(
            selectedKeys = selected,
            onSelectedKeysChange = { selected = it },
            modifier = Modifier
                .navigationTitle("${selected.size} selected")
                .toolbarItem { EditButton() },
        ) {
            for (c in contacts) key(c.id) { Text(c.name, modifier = Modifier.tag(c.id)) }
        }
    }
}
```

`Modifier.selectionDisabled()` on a row makes it non-selectable while keeping it visible.

## Swipe actions

`Modifier.swipeActions` on a row adds buttons revealed by swiping (iOS, macOS with a trackpad, watchOS). Use
`edge = HorizontalEdge.leading` for the other side, and `allowsFullSwipe` to control whether a full swipe triggers the
first action:

```kotlin
@Composable
fun SwipeableContacts(contacts: List<Contact>, onDelete: (Contact) -> Unit, onToggleFavorite: (Contact) -> Unit) {
    List {
        for (c in contacts) key(c.id) {
            Text(
                c.name,
                modifier = Modifier
                    .swipeActions(content = {
                        Button("Delete", systemImage = SFSymbol.trash, role = ButtonRole.destructive) { onDelete(c) }
                    })
                    .swipeActions(edge = HorizontalEdge.leading, allowsFullSwipe = false, content = {
                        Button("Favorite", systemImage = SFSymbol.star) { onToggleFavorite(c) }
                    }),
            )
        }
    }
}
```

A destructive button gets the red background. Tint other buttons with `Modifier.tint(Color.orange)`.

## Deleting and moving rows

Pass `onDelete` and/or `onMove` to `ForEach` to get SwiftUI's `onDelete(perform:)` and `onMove(perform:)`: swipe to
delete, and the delete and reorder controls in edit mode. Both report indices into the list you passed; update your
state accordingly:

```kotlin
@Composable
fun EditableContacts() {
    var contacts by remember {
        mutableStateOf(listOf(Contact("1", "Ada", "ada@example.com"), Contact("2", "Grace", "grace@example.com")))
    }
    NavigationStack {
        List(modifier = Modifier.navigationTitle("Contacts").toolbarItem { EditButton() }) {
            ForEach(
                contacts,
                key = { it.id },
                onDelete = { indices -> contacts = contacts.filterIndexed { i, _ -> i !in indices } },
                onMove = { indices, destination ->
                    val moving = indices.sorted().map { contacts[it] }
                    val kept = contacts.filterIndexed { i, _ -> i !in indices }
                    val insertAt = destination - indices.count { it < destination }
                    contacts = kept.take(insertAt) + moving + kept.drop(insertAt)
                },
            ) { contact ->
                Text(contact.name)
            }
        }
    }
}
```

`destination` follows SwiftUI's convention: it is an index in the list before the move is applied, which is why the example
subtracts the moved rows above it. `Modifier.deleteDisabled(true)` and `Modifier.moveDisabled(true)` on a row exclude
it.

### Edit mode

`EditButton()` toggles SwiftUI's own edit mode (iOS only). To own the state in Kotlin, for example to
show a "Done" button of your own or to leave edit mode after a bulk action, put `Modifier.editMode` on the list:

```kotlin
var editing by remember { mutableStateOf(false) }
List(modifier = Modifier.editMode(editing, onEditingChange = { editing = it })) {
    Text("Row")
}
Button(if (editing) "Done" else "Edit") { editing = !editing }
```

## Pull to refresh

`Modifier.refreshable { }` adds pull to refresh to a list on iOS (other platforms have no built-in control for it, but
SwiftUI still exposes the action to the views inside). The action is a `suspend` lambda, like Swift's `async`
closure: SwiftUI's spinner stays until the coroutine returns, so call your suspending load directly:

```kotlin
@Composable
fun RefreshableInbox(messages: List<String>, reload: suspend () -> Unit) {
    List(modifier = Modifier.refreshable { reload() }) {
        for (m in messages) key(m) { Text(m) }
    }
}
```

If the view disappears during the refresh, SwiftUI cancels its task and the coroutine is cancelled with it.

## Search

`Modifier.searchable` adds a search field. The text is Kotlin state; filter your data with it. Put it on a view inside a
`NavigationStack` (or a `NavigationSplitView` column) so SwiftUI has a toolbar to place the field in:

```kotlin
@Composable
fun SearchableContacts(contacts: List<Contact>) {
    var query by remember { mutableStateOf("") }
    val results = contacts.filter { query.isBlank() || it.name.contains(query, ignoreCase = true) }
    NavigationStack {
        List(modifier = Modifier.navigationTitle("Contacts").searchable(query, onTextChange = { query = it }, prompt = "Name")) {
            for (c in results) key(c.id) { Text(c.name) }
        }
    }
}
```

When nothing matches, `ContentUnavailableSearch(query)` shows the standard "No Results" view.

Other search modifiers:

- `searchable(text, onTextChange, placement, prompt, suggestions = { … })` shows suggestions while typing. Tag each
  suggestion with `Modifier.searchCompletion("text")` to fill the field when it is picked.
- `searchable(text, onTextChange, isPresented, onIsPresentedChange, …)` lets Kotlin open and close the search field.
- `Modifier.searchScopes(scope, onScopeChange) { … }` adds a scope bar; each scope is a view with `Modifier.tag(...)`
  (not on watchOS).
- `Modifier.searchFocused(isFocused, onFocusChange)` focuses the field (iOS 18, macOS 15).

### Search tokens

Tokens are the chips shown in the search field (iOS, macOS). The Kotlin overload takes a Kotlin list, a key, and a
composable for each token; the user removes a token with backspace, which calls `onTokensChange`:

```kotlin
data class Tag(val id: String, val name: String)

@Composable
fun TaggedSearch(contacts: List<Contact>) {
    var query by remember { mutableStateOf("") }
    var tokens by remember { mutableStateOf(listOf(Tag("fav", "Favorites"))) }
    NavigationStack {
        List(
            modifier = Modifier.searchable(
                text = query,
                onTextChange = { query = it },
                tokens = tokens,
                onTokensChange = { tokens = it },
                key = { it.id },
                prompt = "Search",
            ) { tag -> Label(tag.name, systemImage = SFSymbol.tag) },
        ) {
            val onlyFavorites = tokens.any { it.id == "fav" }
            for (c in contacts.filter { !onlyFavorites || it.favorite }) key(c.id) { Text(c.name) }
        }
    }
}
```

## Tables

`Table` shows rows in columns, with column headers, column resizing on macOS and multi-selection. It is
available on iOS 17.4+ and macOS 14.4+; on older systems and on tvOS and watchOS it falls back to a plain `List` whose
rows show the cells side by side, without headers or selection. On iPhone (compact width) SwiftUI itself shows only the first column, so design that column to stand on
its own.

```kotlin
@Composable
fun ContactTable(contacts: List<Contact>) {
    var selection by remember { mutableStateOf(setOf<Any>()) }
    Table(
        contacts,
        key = { it.id },
        selection = selection,
        onSelectionChange = { selection = it },
    ) {
        column("Name") { Text(it.name) }
        column("Email", minWidth = 180.0) { Text(it.email) }
        column("Favorite", width = 70.0, alignment = TableColumnAlignment.center) {
            if (it.favorite) Image(systemName = SFSymbol.starFill)
        }
    }
}
```

- `column(title, width, minWidth, maxWidth, alignment)`: a fixed `width`, or a flexible range.
- `selection` holds keys; without `onSelectionChange` the rows are not selectable.
- `children = { it.children }` turns the table into a hierarchical one (expandable rows), like
  `Table(_:children:)`.
- `customizable = true` lets the user hide and reorder columns (macOS).
- `Modifier.tableStyle(...)` and, on macOS, `Modifier.alternatingRowBackgrounds()` adjust the look.

## Outlines and disclosure groups

`OutlineGroup(items, key, children)` renders a tree as nested disclosure groups; `children` returns `null` for a
leaf. Inside a `List` it gives SwiftUI's `List(data, children:)`:

```kotlin
@Composable
fun FolderTree(folders: List<Folder>) {
    List(modifier = Modifier.listStyle(ListStyle.sidebar)) {
        OutlineGroup(folders, key = { it.id }, children = { it.children }) { folder ->
            Label(folder.name, systemImage = if (folder.children == null) SFSymbol.doc else SFSymbol.folder)
        }
    }
}
```

Each node remembers its own expanded state. For a single collapsible block, use `DisclosureGroup`, with or without
Kotlin-owned state:

```kotlin
var showAdvanced by remember { mutableStateOf(false) }
Form {
    DisclosureGroup("Advanced", isExpanded = showAdvanced, onIsExpandedChange = { showAdvanced = it }) {
        Toggle("Debug logging", isOn = false, onIsOnChange = {})
    }
}
```

`DisclosureGroup` is available on iOS and macOS (a no-op on tvOS and watchOS).

## Lazy stacks or List?

`List` is a platform list: it brings selection, swipe actions, edit mode, separators, insets and the system look, and
it reuses cells. Use it for anything that looks like a table of rows.

For free-form scrolling content (cards, feeds with custom layout, grids), use a `ScrollView` with `LazyVStack`,
`LazyHStack` or `LazyVGrid`. SwiftUI creates their children lazily as views, but in a Kotlin loop the rows are still
composed up front; put a `LazyForEach` inside to make the Kotlin side lazy too:

```kotlin
@Composable
fun ContactCards(contacts: List<Contact>) {
    ScrollView {
        LazyVStack(spacing = 12.0, pinnedViews = PinnedScrollableViews.sectionHeaders) {
            LazyForEach(contacts, key = { it.id }) { c ->
                Text(c.name, modifier = Modifier.padding().frame(maxWidth = Double.POSITIVE_INFINITY).background(ShapeStyle.thinMaterial, RoundedRectangle(cornerRadius = 12.0)))
            }
        }
    }
}
```

Lazy stacks have no swipe actions, selection or edit mode. To scroll programmatically in either, use
`ScrollViewReader` with rows marked `Modifier.id(...)`, or `Modifier.scrollPosition` (see [Layout](layout.md)).
