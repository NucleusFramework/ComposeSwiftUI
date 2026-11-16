<!-- group: Building screens -->
# State and data

> Compose state, hoisting, view models and flows, effects, SwiftUI environment values, and passing data between Swift and Kotlin.

Compose SwiftUI uses the Compose runtime unchanged. `remember`, `mutableStateOf`, `derivedStateOf`, `LaunchedEffect`,
`CompositionLocal`, `ViewModel` and `collectAsState` work exactly as they do in Compose UI, and your existing state
holders don't change. SwiftUI's own state machinery (`@State`, `@Binding`, `@Observable`, `@Environment`) is not used
from Kotlin: Kotlin owns the state, and SwiftUI only renders it.

When state changes, Compose recomposes the composables that read it, and only the views whose arguments changed are
updated on the Swift side. [How it works](how-it-works.md) explains the pipeline.

## Local state

```kotlin
@Composable
fun Counter() {
    var count by remember { mutableStateOf(0) }
    HStack(spacing = 16.0) {
        Button("-") { count-- }
        Text("$count", modifier = Modifier.font(Font.title).monospacedDigit())
        Button("+") { count++ }
    }
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `@State private var count = 0` | `var count by remember { mutableStateOf(0) }` |
| `@State private var items: [Item] = []` | `val items = remember { mutableStateListOf<Item>() }` |
| a computed property | `val total by remember { derivedStateOf { items.sumOf { it.price } } }`, or a plain `val` |
| `@Binding var value` | a `value` parameter plus an `onValueChange` callback |
| `@Observable` model | a `ViewModel` exposing `StateFlow`s, or any class with `mutableStateOf` properties |
| `@Environment(\.colorScheme)` | `EnvironmentReader { env -> env.colorScheme }` |

`derivedStateOf` is worth it when the derived value changes less often than its inputs (a "has unsaved changes" flag
computed from a large form, a scroll offset turned into a boolean). For a cheap computation, a plain `val` in the
composable is simpler.

### What about rememberSaveable?

`rememberSaveable` is available (the runtime depends on `runtime-saveable`), so code shared with Android compiles
unchanged. Inside a `LazyForEach`, a row's saveable state is kept while the row scrolls off screen and restored when
it comes back. Elsewhere it behaves like `remember`: state lives as long as its `ComposeView`, which SwiftUI keeps
across tab switches and navigation pushes, and is lost when the process ends. State that must survive that belongs in
a view model backed by persistence.

## Hoisting and bindings

Controls don't own their value: they take it plus a change callback (see [Controls](controls.md)). That makes
hoisting natural. A reusable component exposes the same pair, and the caller decides where the state lives:

```kotlin
@Composable
fun QuantityRow(title: String, quantity: Int, onQuantityChange: (Int) -> Unit) {
    Stepper("$title: $quantity", value = quantity, bounds = 0..99, onValueChange = onQuantityChange)
}

@Composable
fun Cart() {
    var apples by remember { mutableStateOf(1) }
    var pears by remember { mutableStateOf(0) }
    Form {
        QuantityRow("Apples", apples, onQuantityChange = { apples = it })
        QuantityRow("Pears", pears, onQuantityChange = { pears = it })
        Text("Total: ${apples + pears}")
    }
}
```

Because the callback decides what to store, validation and transformation happen in one place, and the control shows
whatever the state ends up being:

```kotlin
var code by remember { mutableStateOf("") }
// digits only, at most 6
TextField("Code", text = code, onTextChange = { new -> code = new.filter(Char::isDigit).take(6) })
```

## View models and flows

`viewModel { }` returns a `androidx.lifecycle.ViewModel` scoped to the current `ComposeView`: the same instance across
recompositions, cleared (`onCleared`, `viewModelScope` cancelled) when the `ComposeView` goes away. Collect its flows
with `collectAsState`:

```kotlin
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class Profile(val name: String, val bio: String)

sealed interface ProfileState {
    data object Loading : ProfileState
    data class Loaded(val profile: Profile) : ProfileState
    data class Failed(val message: String) : ProfileState
}

class ProfileViewModel(private val load: suspend () -> Profile) : ViewModel() {
    private val _state = MutableStateFlow<ProfileState>(ProfileState.Loading)
    val state: StateFlow<ProfileState> = _state

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = ProfileState.Loading
            _state.value = try {
                ProfileState.Loaded(load())
            } catch (e: Exception) {
                ProfileState.Failed(e.message ?: "Unknown error")
            }
        }
    }
}

@Composable
fun ProfileScreen(load: suspend () -> Profile) {
    val vm = viewModel { ProfileViewModel(load) }
    val state by vm.state.collectAsState()
    when (val s = state) {
        ProfileState.Loading -> ProgressView()
        is ProfileState.Loaded -> VStack {
            Text(s.profile.name, modifier = Modifier.font(Font.title))
            Text(s.profile.bio)
        }
        is ProfileState.Failed -> ContentUnavailableView(
            label = { Label("Couldn't load the profile", systemImage = SFSymbol.exclamationmarkTriangle) },
            description = { Text(s.message) },
            actions = { Button("Retry", action = vm::refresh) },
        )
    }
}
```

A few notes:

- `viewModel(key = "…") { }` keeps several instances of the same class apart.
- A `Flow` that isn't a `StateFlow` needs an initial value: `flow.collectAsState(initial = …)`.
- `collectAsStateWithLifecycle` is not available: there is no `LifecycleOwner` in a `ComposeView`. `collectAsState`
  stops collecting when the composition is disposed, which is what you want in most cases. To pause work while a
  screen is hidden, see the lifecycle section below.
- `StateFlow` updates from any thread are fine. Snapshot state writes from background threads are fine too; they are
  applied on the main thread.

A view model that outlives one screen (an app-wide session, a cache) can be created in Kotlin and handed to several
screens, or passed in from Swift, instead of being created by `viewModel { }`.

## CompositionLocals and the SwiftUI environment

Two environments coexist, and they don't mix.

Compose `CompositionLocal`s work inside the Kotlin tree as usual, including in slots such as sheet content, overlays
and `LazyForEach` rows. Use them for your own dependencies:

```kotlin
data class Theme(val accent: Color, val cornerRadius: Double)

val LocalTheme = staticCompositionLocalOf { Theme(accent = Color.blue, cornerRadius = 12.0) }

@Composable
fun ThemedCard(title: String) {
    val theme = LocalTheme.current
    Text(title, modifier = Modifier
        .padding()
        .background(theme.accent.opacity(0.15), shape = RoundedRectangle(cornerRadius = theme.cornerRadius)))
}

@Composable
fun ThemedScreen() {
    CompositionLocalProvider(LocalTheme provides Theme(accent = Color.orange, cornerRadius = 20.0)) {
        ThemedCard("Orange card")
    }
}
```

SwiftUI environment values are set with `Modifier.environment(…)`, which has one named parameter per writable
environment value. They flow down the SwiftUI view tree, as in Swift:

```kotlin
VStack(modifier = Modifier.environment(colorScheme = ColorScheme.dark).environment(locale = "fr_FR")) {
    DatePicker("Date", selection = 0.0, onSelectionChange = {})
}
```

To read SwiftUI environment values in Kotlin, place an `EnvironmentReader`. Its content receives a
`SwiftUIEnvironment` with `colorScheme`, `dynamicTypeSize`, `horizontalSizeClass`, `verticalSizeClass`, `locale`,
`layoutDirection`, `isEnabled`, `isPresented`, `dismiss`, `scenePhase` and the accessibility preferences
(`reduceMotion`, `reduceTransparency`, `differentiateWithoutColor`, `invertColors`, `voiceOverEnabled`, `boldText`):

```kotlin
@Composable
fun AdaptiveDetail(onShare: () -> Unit) {
    EnvironmentReader { env ->
        if (env.horizontalSizeClass == UserInterfaceSizeClass.compact) {
            VStack { DetailContent(onShare) }
        } else {
            HStack { DetailContent(onShare) }
        }
    }
}

@Composable
fun DetailContent(onShare: () -> Unit) {
    EnvironmentReader { env ->
        Text("Detail", modifier = Modifier.foregroundStyle(if (env.isDark) Color.white else Color.black))
        Button("Share", action = onShare)
        if (env.isPresented) Button("Close", action = env.dismiss)
    }
}
```

The values are read where the reader sits, after SwiftUI's first layout pass: the first composition sees defaults
(light, large, no size class) and recomposes when the real values arrive. Don't base an irreversible decision on the
first value. It also reports `isSearching` (inside a `searchable`), `increasedContrast` and `supportsMultipleWindows`.

### Environment actions

`env.actions` holds SwiftUI's environment actions: `openURL(url)`, `openWindow(id, value)` and `dismissWindow(id)`
(iPad, Mac), `openSettings()` (macOS), `dismissSearch()`, `refresh()` and `rename()`. `canRefresh` tells whether a
`refreshable` applies. An action an OS doesn't have does nothing. SwiftUI doesn't expose `refresh` to the rows of a
`List`, so put the reader under the view that carries `refreshable` (or call your own refresh function).

```kotlin
@Composable
fun HelpLinks() {
    EnvironmentReader { env ->
        Button("Open the help center") { env.actions.openURL("https://example.com/help") }
        if (env.supportsMultipleWindows) Button("Open in a new window") { env.actions.openWindow(id = "help") }
        if (env.isSearching) Button("Cancel search") { env.actions.dismissSearch() }
    }
}
```

## Effects

Compose effects work as usual. Each `ComposeView` runs its composition on the main thread with a main-thread
coroutine context, so `LaunchedEffect` and `rememberCoroutineScope` launch on the main dispatcher.

```kotlin
@Composable
fun SearchResults(query: String, search: suspend (String) -> List<String>) {
    var results by remember { mutableStateOf(emptyList<String>()) }
    LaunchedEffect(query) {
        kotlinx.coroutines.delay(300)   // debounce: a new query cancels this coroutine
        results = search(query)
    }
    List {
        for (r in results) key(r) { Text(r) }
    }
}
```

| SwiftUI | Compose SwiftUI |
|---|---|
| `.task { await load() }` | `Modifier.task { load() }`, or `LaunchedEffect(Unit) { load() }` |
| `.task(id: query) { … }` | `Modifier.task(id = query) { … }`, or `LaunchedEffect(query) { … }` |
| `.onChange(of: value) { old, new in … }` | `LaunchedEffect(value) { … }`, or `snapshotFlow { value }` in a coroutine |
| `.onAppear { }`, `.onDisappear { }` | `Modifier.onAppear { }`, `Modifier.onDisappear { }` |
| cleanup in `.onDisappear` | `DisposableEffect(key) { onDispose { } }` |
| `Task { }` from a button | `val scope = rememberCoroutineScope()`, then `scope.launch { }` |

`Modifier.task { }` takes a `suspend` lambda and follows SwiftUI's rules: the coroutine starts when the view appears
and is cancelled when it disappears (Swift task cancellation cancels the coroutine), and `task(id = key)` restarts it
when `key` changes. `LaunchedEffect` follows the composition instead (next section). Use `task` for work tied to what
is on screen, `LaunchedEffect` for work tied to the composable's life:

```kotlin
@Composable
fun LiveScore(matchId: String, scores: (String) -> kotlinx.coroutines.flow.Flow<String>) {
    var score by remember { mutableStateOf("–") }
    // Collects only while the text is visible, even inside an unselected tab.
    Text(score, modifier = Modifier.task(id = matchId) { scores(matchId).collect { score = it } })
}
```

The generated `Modifier.onChange(value) { }` exists too; `LaunchedEffect(value)` is usually simpler.

### Composition is not visibility

Compose effects follow the composition, not the screen. A view is composed as soon as it is in the SwiftUI tree, even
when SwiftUI doesn't show it: the unselected tabs of a `TabView`, the content of a closed `DisclosureGroup`. A
`LaunchedEffect` in those starts immediately and keeps running.

When the work should follow what the user sees, use `onAppear` / `onDisappear`, which SwiftUI calls when the view
actually appears:

```kotlin
@Composable
fun LiveTicker(start: () -> Unit, stop: () -> Unit) {
    Text("Live prices", modifier = Modifier
        .onAppear { start() }
        .onDisappear { stop() })
}
```

`LazyForEach` rows are the exception: they are composed only while visible, so their effects do follow scrolling.

## Lifecycle: onAppear, onDisappear and scene phase

A `ComposeView` owns one composition:

1. When SwiftUI creates the `ComposeView`, the Kotlin function you passed builds a host and composes it once, before
   the first frame.
2. While it lives, state changes recompose it; it is never recreated by SwiftUI body re-evaluations.
3. When the `ComposeView` leaves the hierarchy, the composition is disposed: `DisposableEffect`s run `onDispose`,
   coroutines are cancelled, and its view models are cleared.

The app's scene phase (active, inactive, background) is in `EnvironmentReader`:

```kotlin
@Composable
fun SyncWhileActive(sync: suspend () -> Unit) {
    EnvironmentReader { env ->
        if (env.scenePhase == ScenePhase.active) {
            LaunchedEffect(Unit) { sync() }   // cancelled when the app goes inactive or to the background
        }
    }
}
```

## Passing data from Swift

### Parameters

The Kotlin entry point is a plain function, so it can take parameters, callbacks included:

```kotlin
fun productScreen(productId: String, onClose: () -> Unit) = composeSwiftUI {
    NavigationStack {
        Text("Product $productId", modifier = Modifier.navigationTitle("Product"))
        Button("Close", action = onClose)
    }
}
```

```swift
ComposeView { ProductScreenKt.productScreen(productId: product.id, onClose: { dismiss() }) }
```

The closure runs once, when SwiftUI first creates the `ComposeView`: later values of `product.id` are not seen. That
is fine for an identity that never changes during the view's life (SwiftUI recreates the view when its identity
changes, for example with `.id(product.id)`), but not for a value that changes.

### Inputs

For a value that changes, use `ComposeView(input:)`. Every new value is pushed into the composition, and `hostInput()`
reads it, recomposing what depends on it:

```swift
struct ProfileTab: View {
    @AppStorage("isPro") private var isPro = false
    let userId: String

    var body: some View {
        ComposeView(input: "\(userId)|\(isPro)") { ProfileTabKt.profileTab() }
    }
}
```

```kotlin
fun profileTab() = composeSwiftUI {
    val (userId, pro) = hostInput<String>().split("|")
    Text("User $userId", modifier = Modifier.font(Font.headline))
    if (pro == "true") Text("Pro")
}
```

The input must be `Hashable` in Swift; Kotlin receives the bridged object, so the type you read with `hostInput<T>()`
must match what Kotlin/Native gives you: a `String` for a Swift `String`, a Kotlin class instance for a Kotlin object
created in Swift (Kotlin classes are `NSObject`s, hence `Hashable`, compared with `equals`). A Kotlin data class is the
cleanest way to pass several values:

```kotlin
data class ProfileInput(val userId: String, val isPro: Boolean)

fun profileTabTyped() = composeSwiftUI {
    val input = hostInput<ProfileInput>()
    Text("User ${input.userId}")
    if (input.isPro) Text("Pro")
}
```

```swift
ComposeView(input: ProfileInput(userId: userId, isPro: isPro)) { ProfileTabKt.profileTabTyped() }
```

The first composition already sees the initial input.

### Observing Kotlin state from Swift

Plain SwiftUI code that needs a Kotlin `StateFlow` (a badge count in a Swift tab bar, for example) can observe it with
`watchFlow`, which calls back on the main thread and returns a handle to cancel:

```swift
@MainActor final class BadgeModel: ObservableObject {
    @Published var count = 0
    private var watcher: FlowWatcher?

    init(inbox: InboxViewModel) {
        watcher = FlowWatchKt.watchFlow(flow: inbox.unreadCount) { [weak self] value in
            self?.count = (value as? KotlinInt)?.intValue ?? 0
        }
    }

    deinit { watcher?.cancel() }
}
```

## Common pitfalls

- **Casting selections.** Pickers, `scrollPosition` and `List` selection report `Any`. Tag options with values of
  exactly one type and cast in the callback (`{ size = it as Size }`). An `Int` tag stays an `Int`; don't mix `Int`
  and `Long`.
- **Mutable collections in state.** `mutableStateOf(mutableListOf())` doesn't notice `add`. Use `mutableStateListOf`,
  or store immutable lists and replace them (`items = items + item`).
- **Unstable list items.** `LazyForEach` and `key` compare items by `equals`. Data classes work; classes with identity
  equality recompose rows on every change.
- **Geometry feedback loops.** Writing a size from `GeometryReader` or `onGeometryChange` into state that changes the
  size again oscillates. Derive a coarse value and only write it when it changes.
- **Heavy work in composition.** Composables run on the main thread and may run often. Sort, filter and format in a
  view model, or at least in `remember(key) { }`.
- **Effects in hidden content.** A `LaunchedEffect` in an unselected tab runs. Use `onAppear` for visibility.
- **Expecting restoration.** Nothing is saved across process death; persist what matters.
- **Creating a host per body evaluation.** Always create the host inside the `ComposeView { }` closure. A host built
  elsewhere and passed in would be shared, and disposed by the first `ComposeView` that disappears.
