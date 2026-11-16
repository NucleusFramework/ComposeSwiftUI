<!-- group: Getting started -->
# How it works

> The Compose runtime builds a tree of nodes in Kotlin; a Swift renderer observes each node and rebuilds only the SwiftUI views whose data changed.

You don't need this chapter to write screens, but it explains the behavior you will see: why updates are cheap, why
long lists need `LazyForEach`, why bindings are controlled by Kotlin state, and how to plug in your own Swift views.

## The big picture

```bash
Kotlin (your framework)                          Swift (your app target)
@Composable VStack / Text / …                    ComposeView
  → one SNode per view: kind, arguments,   ──▶     one NodeModel (@Observable) per SNode
    modifier chain, children, slots                  → NodeView builds the SwiftUI view for its kind
                                                     → ModChain applies the modifiers in order
Recomposition updates changed SNodes       ──▶   only those NodeViews re-render
Kotlin callbacks, bindings                 ◀──   actions, binding setters, flushCompose()
```

There are three pieces:

- **The Kotlin library** (`dev.nucleusframework:compose-swiftui`): the generated composables, modifiers and value
  types, the hand-written APIs, and a Compose `Applier` that maintains the node tree.
- **The Swift renderer** (`ComposeSwiftUI/`, written into your app by the Gradle plugin): one decoder per view,
  modifier and value, generated from the same SDK symbol graphs as the Kotlin API, plus the hand-written renderers.
- **`ComposeView`**: the SwiftUI view that owns a composition host and renders its root node.

## Composition and the node tree

`composeSwiftUI { content }` creates a `ComposeSwiftUIHost`. When `ComposeView` first appears, the host starts a
Compose `Recomposer` and a `Composition` whose applier builds a tree of `SNode`s. Each SwiftUI view you call in Kotlin
emits exactly one node:

```kotlin skip
// What a generated composable looks like (GenViews.kt)
@Composable
public fun Toggle(title: String, isOn: Boolean, modifier: Modifier = Modifier, onIsOnChange: (Boolean) -> Unit) {
    SwiftView(KIND_TOGGLE, listOf(title, isOn, SValueCallback { onIsOnChange(it as Boolean) }), modifier)
}
```

A node holds:

- **`kind`**: an integer that identifies the SwiftUI initializer (`Toggle(_:isOn:)`, `VStack(alignment:spacing:)`…).
  Overloads of the same view have different kinds.
- **`args`**: the encoded arguments, in declaration order. Strings, numbers and booleans are passed as is; SwiftUI
  values (`Font.title`, `Color.red.opacity(0.5)`, `.borderedProminent`) are `SExpr(id, args)` expressions; callbacks
  are `SAction` / `SValueCallback` handles; secondary view builders are `SSlotRef(index)`.
- **`modifiers`**: the `Modifier` chain, a list of `SModifier(id, args)` in the order you wrote them.
- **children**: the `@ViewBuilder content` (the trailing lambda), and **slot** children for the other builders
  (`label`, `header`, a `sheet`'s content, a `toolbarItem`'s content, an `overlay`).

Nothing is converted to Swift during composition. The tree is plain Kotlin objects; Swift reads it through typed
accessors (`argString`, `argDouble`, `argExpr`…) when it renders, so there is no serialization step and no bridging of
whole lists.

## The Swift side: registry and renderers

The renderer keeps a registry of factories, `ComposeRegistry.views[kind]` and `ComposeRegistry.modifiers[id]`, filled
at start-up by the generated code (`GenViews.swift`, `GenModifiers*.swift`), the hand-written renderers, and the
integrations you register (`ComposeIntegrations.registerMapKit()`…).

For each node, Swift attaches one `NodeModel`, an `@Observable` object, and renders it with a `NodeView`:

1. `NodeView.body` reads the model's `version`, which subscribes the view to that node only.
2. The factory for the node's `kind` decodes the arguments and builds the concrete SwiftUI view, for example
   `Toggle(LocalizedStringKey(a.string(0)), isOn: a.boolBinding(1))`.
3. `ModChain` applies the modifiers one by one with generic recursion, so each step keeps its concrete
   `ModifiedContent` type; the result is type-erased once, as the node's `AnyView`.
4. Children are rendered by a `ForEach` over the child models, identified by stable node ids.

Values go through generated decoders: `d_Font(expr)` turns an `SExpr` back into a SwiftUI `Font`, and so on. A value
that can't be decoded (for example a value from a newer SDK on an older OS) makes that modifier a no-op rather than a
crash.

Availability is checked at this point, on the device. Each factory wraps newer APIs in `if #available(…)`: a modifier
newer than the running OS is skipped, a view newer than the running OS renders as `EmptyView`, and an API that doesn't
exist on a platform at all is compiled out with `#if os(…)`. This is why the app keeps its iOS 17 deployment target
even though the renderer calls iOS 27 APIs, and why the KDoc of each Kotlin function lists its availability.

An unknown kind (an integration file that isn't registered, or a renderer from a different version than the library)
renders as a red "Unsupported view kind N" text; an unknown modifier is skipped.

## Recomposition and Swift updates

When Kotlin state changes, Compose recomposes the affected scopes as usual. Then, for each emitted node:

- If the new arguments have the same shape as the previous ones, nothing is sent to Swift. Callbacks compare by kind
  only: a new lambda doesn't count as a change, the existing `SAction` handle simply starts calling the new lambda.
- Otherwise the node's `args` or `modifiers` are replaced and the node is marked dirty.
- Insertions, removals and moves of children mark the parent dirty.

At the end of the frame, every dirty node notifies its `NodeModel`, which bumps `version`. SwiftUI re-evaluates only
the `NodeView`s that read those versions. A change to one `Text` deep in a screen re-renders that `Text`, not the
screen: this is why the benchmarks show updates as fast as plain SwiftUI or faster (see [Performance](performance.md)).

Because children keep stable identities (node ids, or your `key(…)` values), SwiftUI sees the same views across
updates. Animations, transitions, `matchedGeometryEffect`, scroll positions and the internal state of native controls
(a text field's cursor, a picker's open menu) behave as in a hand-written SwiftUI app. Use `key(…)` around loop items,
as you would in Compose, so that a reordered or filtered list keeps each row's identity.

## Bindings and callbacks: the round trip

SwiftUI controls take a `Binding`. In Kotlin, a binding is two arguments: a value and an `onXChange` callback.

```kotlin
var on by remember { mutableStateOf(false) }
Toggle("Wi-Fi", isOn = on, onIsOnChange = { on = it })
```

On the Swift side, the binding's getter reads the current argument and its setter does three things, synchronously,
on the main thread:

1. writes the new value into the node right away (`setArgSilently`), so a SwiftUI control that reads the binding back
   during the same event (a text field while typing) sees it;
2. calls the Kotlin callback;
3. calls `flushCompose()`, which runs the pending recomposition immediately instead of waiting for the next frame.

The state change is therefore applied in the same SwiftUI transaction as the user's edit. Typing doesn't jump, and an
animation started by `withAnimation` in the callback animates the resulting changes.

Kotlin stays the source of truth: if the callback doesn't update the state (say, it rejects a value), the next
composition produces the old value and the node is reset to it, so the control snaps back. This is the controlled
component model, the same as `TextField(value, onValueChange)` in Jetpack Compose.

Actions (`Button(action)`, `onTapGesture`, `onSubmit`) work the same way: the Kotlin function runs, then
`flushCompose()`. Values that Swift sends back are converted to Kotlin types: numbers to `Int`/`Double`, colors to
`Color(red, green, blue, opacity)`, dates to epoch seconds, URLs to `String`, and "no value" to `null`.

## Threading

Everything runs on the main thread:

- The `Recomposer` runs on `Dispatchers.Main.immediate`, with a frame clock that schedules frames on the main queue.
- State written on another thread (a repository updating a `StateFlow` from `Dispatchers.IO`, for example) is picked
  up through Compose's global snapshot observer, which posts the apply notification to the main queue.
- Swift calls Kotlin callbacks on the main thread, and SwiftUI renders on the main thread.

So the usual Compose rules apply: do I/O in coroutines (`viewModelScope`, `LaunchedEffect`), expose results as state,
and never block a callback.

## Lifetimes

- A `ComposeView` owns its host through a SwiftUI `@StateObject`: one host per `ComposeView` identity. When SwiftUI
  removes the view, the composition is disposed and the host's `ViewModelStore` is cleared, which calls
  `ViewModel.onCleared()` and cancels `viewModelScope`.
- `remember` state lives in the composition, as usual. `LaunchedEffect` and `DisposableEffect` start and stop with
  their call site.
- `LazyForEach` gives each visible row its own small composition, created when SwiftUI asks for the row and disposed
  when the row scrolls away. Row state that must survive scrolling belongs in the item or the view model, not in
  `remember` inside the row.

## What this implies

**For performance.** An update costs work proportional to what changed, so state-heavy screens are cheap. Creating
views costs a bit more than in plain SwiftUI: the tree is built at run time, so each node is one type-erased view, and
composing has a cost of its own. Mounting is 8% to 18% slower than plain SwiftUI, and a plain Kotlin loop composes and
updates every row of a list, visible or not. Use `LazyForEach` for long lists and keep items immutable. Details in
[Performance](performance.md).

**For custom Swift views.** The registry is public, so Swift can render anything the generated API doesn't cover. In
Kotlin, emit a `NativeView` with a key and arguments:

```kotlin
@Composable
fun Rating(value: Double, onValueChange: (Double) -> Unit) {
    NativeView("rating", Modifier.frame(height = 44.0), value, valueCallback<Double> { onValueChange(it) })
}
```

In Swift, register a factory for that key once at launch. The arguments start at index 1 (index 0 is the key), and a
value followed by its callback reads as a `Binding`:

```swift
ComposeRegistry.nativeViews["rating"] = { args in
    AnyView(RatingView(value: args.doubleBinding(1)))
}
```

`NativeView` is how you wrap a `UIViewRepresentable` or a third-party
SwiftUI component. You can also override any generated view or modifier by assigning `ComposeRegistry.views[kind]` or
`ComposeRegistry.modifiers[id]`, though that ties your code to generated ids. More in [Interop](interop.md).

**For Swift-only features.** Anything SwiftUI only accepts as a Swift type (a `@main` app, a widget configuration, an
`AppIntent`, a SwiftData model) stays in Swift; the generated API covers what builds views. See
[Limitations](limitations.md).

## How the API is generated

The Kotlin API and the matching Swift decoders are generated from the symbol graphs of the iOS, macOS, tvOS and
watchOS SDKs, plus the SwiftUI overlays of other frameworks (MapKit, Charts, StoreKit…). Every generated API is
compiled for all four platforms before it ships, and [COVERAGE.md](https://github.com/NucleusFramework/ComposeSwiftUI/blob/main/docs/COVERAGE.md)
records the status of every entry of Apple's SwiftUI documentation. The rules the generator follows to name and shape
Kotlin functions are described in [API mapping](api-mapping.md).
