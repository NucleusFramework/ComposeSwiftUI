<!-- group: Building screens -->
# Navigation

> Push screens with NavigationStack, build sidebars with NavigationSplitView, switch sections with TabView, fill the toolbar, and keep a Navigation 3 back stack.

Navigation containers are SwiftUI's own: you get the native push animation, swipe back, large titles, sidebars that
collapse on iPhone, and the system tab bar. The difference from Swift is where navigation state lives. SwiftUI's
type-based routing (`navigationDestination(for: Route.self)`) relies on Swift types and `Hashable` values, so Compose
SwiftUI drives navigation from Kotlin state instead: a Kotlin list is the path, a Kotlin value is the selected tab or
sidebar row.

| SwiftUI | Compose SwiftUI |
|---|---|
| `NavigationStack { … }` | `NavigationStack { … }` |
| `NavigationStack(path: $path) { … }.navigationDestination(for:)` | `NavigationStack(path, onPathChange, destination = { route -> }) { … }` |
| `NavigationLink { Detail() } label: { Text("Open") }` | `NavigationLink(destination = { Detail() }, label = { Text("Open") })` |
| `.navigationDestination(isPresented: $show) { … }` | `Modifier.navigationDestination(isPresented, onIsPresentedChange) { … }` |
| `.navigationDestination(item: $item) { … }` | `Modifier.navigationDestination(item, onDismiss) { item -> }` |
| `NavigationSplitView { … } detail: { … }` | `NavigationSplitView(sidebar = { … }, detail = { … })` |
| `TabView(selection: $tab) { Tab(…, value:) { … } }` | `TabView(tab, onSelectionChange) { Tab(…, value = …) { … } }` |
| `.toolbar { ToolbarItem(placement: .primaryAction) { … } }` | `Modifier.toolbarItem(ToolbarItemPlacement.primaryAction) { … }` |

## NavigationStack

A `NavigationStack` hosts a root view; titles and toolbar items are modifiers on the screen inside it, as in SwiftUI:

```kotlin
@Composable
fun SettingsRoot() {
    NavigationStack {
        List(modifier = Modifier.navigationTitle("Settings")) {
            NavigationLink(destination = { AboutScreen() }, label = { Label("About", systemImage = SFSymbol.infoCircle) })
        }
    }
}

@Composable
fun AboutScreen() {
    Text("Version 1.0", modifier = Modifier.navigationTitle("About"))
}
```

`NavigationLink(destination, label)` is fine for static links: the destination is composed when the link is pushed.
Inside a `List`, it gets the disclosure chevron. Don't nest a `NavigationStack` inside another one; each screen pushed
on a stack is just a view.

### A path owned by Kotlin

For anything beyond static links, keep the path in Kotlin state. `NavigationStack(path, onPathChange, destination)`
shows `root`, then one `destination(route)` per element of `path`. Push by appending; SwiftUI's back button and swipe
call `onPathChange` with the truncated path:

```kotlin
sealed interface Route {
    data class Contact(val id: String) : Route
    data object Settings : Route
}

@Composable
fun ContactsNavigation(contactIds: List<String>) {
    var path by remember { mutableStateOf(listOf<Route>()) }
    NavigationStack(
        path = path,
        onPathChange = { path = it },
        destination = { route ->
            when (route) {
                is Route.Contact -> Text("Contact ${route.id}", modifier = Modifier.navigationTitle(route.id))
                Route.Settings -> Text("Settings", modifier = Modifier.navigationTitle("Settings"))
            }
        },
    ) {
        List(modifier = Modifier.navigationTitle("Contacts").toolbarItem { Button("Settings", systemImage = SFSymbol.gear) { path = path + Route.Settings } }) {
            for (id in contactIds) key(id) {
                Button(id) { path = path + Route.Contact(id) }
            }
        }
    }
}
```

Because the path is plain state, deep links, "pop to root" (`path = emptyList()`) and pushing several screens at once
(`path = listOf(a, b)`) are one assignment. Use data classes or objects as routes: each element's identity is its
`equals`.

SwiftUI's value-based `NavigationLink(value:)` needs `navigationDestination(for:)`, which has no Kotlin equivalent, so
it does not push onto a Kotlin path. Use a `Button` that appends to the path (as above). A `Button` row has no
disclosure chevron; add one yourself (`Image(systemName = SFSymbol.chevronRight)`) if you want it.

### Destinations driven by a flag or an item

`Modifier.navigationDestination(isPresented, onIsPresentedChange)` pushes a view while a Boolean is true, and the
`item` variant pushes while a value is non-null. They are handy for a push triggered by an event rather than a row,
for example after a successful save:

```kotlin
var created by remember { mutableStateOf<String?>(null) }
VStack(modifier = Modifier.navigationDestination(item = created, onDismiss = { created = null }) { name ->
    Text("Created $name")
}) {
    Button("Create") { created = "Project 1" }
}
```

Attach them to a view inside the `NavigationStack`, not to the stack itself.

### Zoom transitions

On iOS 18+, a pushed screen can zoom out of the view that opened it. Mark the source with
`Modifier.matchedTransitionSource(id, namespace)` and the destination with `Modifier.zoomTransition(id, namespace)`,
both under the same `NamespaceScope`:

```kotlin
var path by remember { mutableStateOf(listOf<String>()) }
NamespaceScope { ns ->
    NavigationStack(path, onPathChange = { path = it }, destination = { photo ->
        Text(photo, modifier = Modifier.zoomTransition(photo, ns))
    }) {
        Button("Open photo", modifier = Modifier.matchedTransitionSource("sunset", ns)) { path = path + "sunset" }
    }
}
```

## Navigation 3

If your app uses the Navigation 3 runtime (`NavKey`, `entryProvider`, a back stack), keep it and display the back stack
with `NavDisplay` from Compose SwiftUI: you keep your routing code and get SwiftUI navigation.

```kotlin skip
val backStack = remember { mutableStateListOf<Route>(Route.Home) }
val entries = entryProvider<Route> {
    entry<Route.Home> { HomeScreen(onOpen = { backStack.add(it) }) }
    entry<Route.Detail> { key -> DetailScreen(key.id) }
}
NavDisplay(backStack) { route -> entries(route).Content() }
```

The first key is the root, the others are pushed. Push with `backStack.add(...)`; the SwiftUI back button and swipe
remove keys from the list. `NavDisplay` accepts any observable `MutableList` (a `NavBackStack` or
`mutableStateListOf(...)`), so it also works without Navigation 3. Import `dev.nucleusframework.composeswiftui.NavDisplay`,
not the one from `navigation3-ui`, which draws with Compose UI.

## NavigationSplitView

`NavigationSplitView` shows two or three columns on iPad and Mac and collapses into a stack on iPhone. The usual
pattern drives the detail column from the sidebar's list selection:

```kotlin
@Composable
fun MailSplit(mailboxes: List<String>) {
    var selected by remember { mutableStateOf<String?>(mailboxes.firstOrNull()) }
    NavigationSplitView(
        sidebar = {
            List(
                selectedKey = selected,
                onSelectedKeyChange = { selected = it as String? },
                modifier = Modifier.navigationTitle("Mail"),
            ) {
                for (box in mailboxes) key(box) {
                    Label(box, systemImage = SFSymbol.tray, modifier = Modifier.tag(box))
                }
            }
        },
        detail = {
            val box = selected
            if (box == null) {
                ContentUnavailableView("No Mailbox", systemImage = SFSymbol.tray)
            } else {
                Text(box, modifier = Modifier.navigationTitle(box))
            }
        },
    )
}
```

On iPhone, selecting a row pushes the detail column, and going back returns to the sidebar. For a three-column layout, pass `content` as well (sidebar, content list, detail). The detail column
can host its own `NavigationStack` for deeper pushes.

### Column visibility and the compact column

Pass `columnVisibility` to show or hide columns from Kotlin (the sidebar button updates it), and
`preferredCompactColumn` to choose which column iPhone shows first:

```kotlin
var visibility by remember { mutableStateOf(NavigationSplitViewVisibility.all) }
var compactColumn by remember { mutableStateOf(NavigationSplitViewColumn.sidebar) }
NavigationSplitView(
    columnVisibility = visibility,
    preferredCompactColumn = compactColumn,
    onColumnVisibilityChange = { visibility = it },
    onPreferredCompactColumnChange = { compactColumn = it },
    sidebar = { Text("Sidebar", modifier = Modifier.navigationSplitViewColumnWidth(min = 200.0, ideal = 260.0, max = 320.0)) },
    detail = {
        Button("Focus on detail") { visibility = NavigationSplitViewVisibility.detailOnly }
    },
)
```

`NavigationSplitViewVisibility` is `all`, `doubleColumn`, `detailOnly` or `automatic`.
`Modifier.navigationSplitViewColumnWidth(...)` goes on a column's content.
`Modifier.navigationSplitViewStyle(NavigationSplitViewStyle.balanced)` (or `prominentDetail`) on the split view picks
how columns share space.

## TabView

Without selection state, a `TabView` just lists `Tab`s:

```kotlin
TabView {
    Tab("Home", systemImage = SFSymbol.house) { Text("Home") }
    Tab("Settings", systemImage = SFSymbol.gear) { Text("Settings") }
}
```

To switch tabs from code (a deep link, a "See all" button on another tab), give each tab a `value` and hold the
selection in Kotlin:

```kotlin
enum class AppTab { Home, Search, Profile }

@Composable
fun AppTabs() {
    var tab by remember { mutableStateOf(AppTab.Home) }
    TabView(selection = tab, onSelectionChange = { tab = it }) {
        Tab("Home", systemImage = SFSymbol.house, value = AppTab.Home) {
            NavigationStack {
                Button("Go to profile") { tab = AppTab.Profile }
            }
        }
        Tab("Search", systemImage = SFSymbol.magnifyingglass, value = AppTab.Search) { Text("Search") }
        Tab("Profile", systemImage = SFSymbol.personCropCircle, value = AppTab.Profile) { Text("Profile") }
    }
}
```

Each tab usually owns its own `NavigationStack`, so each keeps its own back stack. Put the `TabView` outside the
stacks, not inside one.

### Styles and sidebars

- `Modifier.tabViewStyle(TabViewStyle.page)` turns the tabs into horizontally paged content (onboarding, carousels);
  `TabViewStyle.page(indexDisplayMode = …)` controls the dots.
- `TabViewStyle.sidebarAdaptable` (iOS 18, macOS 15) shows a tab bar that can become a sidebar on iPad and Mac. Group
  tabs with `TabSection("Library") { Tab(…) }`; every tab then needs a `value`.
- `Modifier.tabBarMinimizeBehavior(TabBarMinimizeBehavior.onScrollDown)` (iOS 26) shrinks the tab bar while
  scrolling, and `Modifier.tabViewBottomAccessory { … }` (iOS 26) adds a view above it, like a mini player.

```kotlin
var tab by remember { mutableStateOf("inbox") }
TabView(tab, onSelectionChange = { tab = it }, modifier = Modifier.tabViewStyle(TabViewStyle.sidebarAdaptable)) {
    Tab("Inbox", systemImage = SFSymbol.tray, value = "inbox") { Text("Inbox") }
    TabSection("Folders") {
        Tab("Work", systemImage = SFSymbol.folder, value = "work") { Text("Work") }
        Tab("Personal", systemImage = SFSymbol.folder, value = "personal") { Text("Personal") }
    }
}
```

## Toolbars and titles

### Titles

`Modifier.navigationTitle("…")` sets the title of the screen it is on. Related modifiers:

| Modifier | Effect | Availability |
|---|---|---|
| `navigationTitle(title)` | Screen title | All |
| `navigationTitle(title, onTitleChange)` | Renamable title (tap the title on iOS, document-style) | All |
| `navigationSubtitle(subtitle)` | Subtitle under the title | macOS 14, iOS 26 |
| `toolbarTitleDisplayMode(ToolbarTitleDisplayMode.inline)` | `large`, `inline`, `inlineLarge`, `automatic` | All |
| `navigationBarTitleDisplayMode(NavigationBarItem.TitleDisplayMode.inline)` | Older iOS/watchOS form of the same | iOS, watchOS |
| `toolbarTitleMenu { … }` | A menu attached to the title | All |
| `navigationBarBackButtonHidden()` | Hides the back button | All |

### Toolbar items

`Modifier.toolbarItem(placement) { … }` is SwiftUI's `.toolbar { ToolbarItem(placement:) { … } }`. Chain it once per
item:

```kotlin
@Composable
fun EditorScreen(onSave: () -> Unit, onCancel: () -> Unit) {
    Text(
        "Draft",
        modifier = Modifier
            .navigationTitle("New Note")
            .toolbarTitleDisplayMode(ToolbarTitleDisplayMode.inline)
            .toolbarItem(ToolbarItemPlacement.cancellationAction) { Button("Cancel", action = onCancel) }
            .toolbarItem(ToolbarItemPlacement.confirmationAction) { Button("Save", action = onSave) }
            .toolbarItem(ToolbarItemPlacement.bottomBar) { Text("Edited just now") },
    )
}
```

Prefer semantic placements (`primaryAction`, `confirmationAction`, `cancellationAction`, `destructiveAction`,
`secondaryAction`, `navigation`, `status`): SwiftUI puts them where each platform expects. Explicit positions exist too:
`topBarLeading`, `topBarTrailing` and `bottomBar` (iOS), `keyboard` (above the software keyboard). An item with several
views (`HStack`) or a `Menu` works like SwiftUI's `ToolbarItemGroup`.

`Modifier.toolbar { … }` takes plain content (SwiftUI places it with the `automatic` placement), and
`Modifier.toolbar(removing = ToolbarDefaultItemKind.sidebarToggle)` removes a default item.

### Toolbar appearance

```kotlin
Text(
    "Content",
    modifier = Modifier
        .navigationTitle("Themed")
        .toolbarBackground(Color.indigo, ToolbarPlacement.navigationBar)
        .toolbarBackground(Visibility.visible, ToolbarPlacement.navigationBar)
        .toolbarColorScheme(ColorScheme.dark, ToolbarPlacement.navigationBar),
)
```

`toolbarBackground(style, bars)` only shows when the bar background is visible, hence the second call.
`toolbar(Visibility.hidden, ToolbarPlacement.tabBar)` hides a bar for one screen (for example the tab bar on a pushed
detail screen); `toolbarRole(ToolbarRole.editor)` changes how the iPad toolbar lays out items.
