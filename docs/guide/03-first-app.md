<!-- group: Getting started -->
# Your first app

> Build a small task list in Kotlin, step by step: a view model, a SwiftUI List with swipe to delete, a detail screen, a sheet to add tasks and a toggle filter, then show it from Swift.

This tutorial builds a task list app. Everything except the last step is Kotlin in `commonMain`; it assumes the
plugin and the renderer are set up as described in [Installation](installation.md).

The finished app has:

- a list of tasks, with a button to mark each one done and swipe to delete;
- a "Show completed" toggle;
- a detail screen to edit a task, pushed with the system navigation;
- a sheet with a form to add a task;
- an empty state when there is nothing to show.

## 1. The model and the view model

The state lives in a regular `androidx.lifecycle.ViewModel` exposing a `StateFlow`. Nothing here depends on Compose
SwiftUI: the same class could back an Android screen.

```kotlin
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Task(
    val id: Int,
    val title: String,
    val notes: String = "",
    val isDone: Boolean = false,
)

class TasksViewModel : ViewModel() {
    private val _tasks = MutableStateFlow(
        listOf(
            Task(1, "Buy milk"),
            Task(2, "Book the flight", notes = "Window seat"),
            Task(3, "Water the plants", isDone = true),
        ),
    )
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    private var nextId = 4

    fun add(title: String) = _tasks.update { it + Task(nextId++, title.trim()) }

    fun toggle(id: Int) = edit(id) { it.copy(isDone = !it.isDone) }

    fun rename(id: Int, title: String) = edit(id) { it.copy(title = title) }

    fun setNotes(id: Int, notes: String) = edit(id) { it.copy(notes = notes) }

    fun delete(ids: Set<Int>) = _tasks.update { list -> list.filterNot { it.id in ids } }

    private fun edit(id: Int, change: (Task) -> Task) =
        _tasks.update { list -> list.map { if (it.id == id) change(it) else it } }
}
```

`Task` is an immutable data class. That matters: Compose skips a row whose `Task` is equal to the previous one, and
`LazyForEach` recomposes a row only when its item changed. See [Performance](performance.md).

## 2. A row

Start with the smallest piece, one row. It takes plain values and a callback, so it is easy to preview and reuse.

```kotlin
@Composable
fun TaskRow(task: Task, onToggle: () -> Unit) {
    HStack(spacing = 12.0) {
        Button(action = onToggle, modifier = Modifier.buttonStyle(PrimitiveButtonStyle.borderless)) {
            Image(
                systemName = if (task.isDone) SFSymbol.checkmarkCircleFill else SFSymbol.circle,
                modifier = Modifier.font(Font.title2).foregroundStyle(if (task.isDone) ShapeStyle.green else ShapeStyle.secondary),
            )
        }
        VStack(alignment = HorizontalAlignment.leading, spacing = 2.0) {
            Text(task.title, modifier = Modifier.strikethrough(task.isDone).foregroundStyle(if (task.isDone) ShapeStyle.secondary else ShapeStyle.primary))
            if (task.notes.isNotEmpty()) {
                Text(task.notes, modifier = Modifier.font(Font.caption).foregroundStyle(ShapeStyle.secondary).lineLimit(1))
            }
        }
    }
}
```

The same row in SwiftUI would read:

```swift
HStack(spacing: 12) {
    Button(action: onToggle) {
        Image(systemName: task.isDone ? "checkmark.circle.fill" : "circle")
            .font(.title2)
            .foregroundStyle(task.isDone ? .green : .secondary)
    }
    .buttonStyle(.borderless)
    VStack(alignment: .leading, spacing: 2) { … }
}
```

The translation is mechanical: argument labels become named arguments, the `@ViewBuilder` closure becomes the
trailing lambda, modifiers move into `modifier = Modifier…` in the same order, `.green` on a `ShapeStyle` parameter
becomes `ShapeStyle.green`, and `CGFloat` becomes `Double`. [API mapping](api-mapping.md) has the full set of rules.

The `.borderless` button style matters inside a `List`: without it, SwiftUI makes the whole row the button, and a tap
anywhere would toggle the task instead of opening it.

Add a preview while you are here:

```kotlin
@Preview(name = "Task row")
@Composable
fun TaskRowPreview() {
    List {
        TaskRow(Task(1, "Buy milk", notes = "Oat milk"), onToggle = {})
        TaskRow(Task(2, "Water the plants", isDone = true), onToggle = {})
    }
}
```

It renders in IntelliJ IDEA / Android Studio with the IDE plugin, and in Xcode through the generated
`ComposeSwiftUIPreviews.swift`. See [Previews](previews.md).

## 3. The list

The list screen reads the tasks, filters them with Kotlin state, and lets the user delete with a swipe.

```kotlin
@Composable
fun TaskList(
    tasks: List<Task>,
    showCompleted: Boolean,
    onShowCompletedChange: (Boolean) -> Unit,
    onToggle: (Int) -> Unit,
    onDelete: (Set<Int>) -> Unit,
    detail: @Composable (Task) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible = if (showCompleted) tasks else tasks.filterNot { it.isDone }
    List(modifier = modifier) {
        Section {
            Toggle("Show completed", isOn = showCompleted, onIsOnChange = onShowCompletedChange)
        }
        Section(title = "Tasks") {
            ForEach(
                visible,
                key = { it.id },
                onDelete = { indices -> onDelete(indices.map { visible[it].id }.toSet()) },
            ) { task ->
                NavigationLink(destination = { detail(task) }) {
                    TaskRow(task, onToggle = { onToggle(task.id) })
                }
            }
        }
    }
}
```

A few things to notice:

- `Toggle` is a controlled control. It shows `isOn`, and a tap calls `onIsOnChange`; nothing changes until the
  callback updates the state that feeds `isOn`. This is how every SwiftUI `Binding` maps to Kotlin.
- `ForEach(items, key, onDelete)` is SwiftUI's `ForEach(data, id:)` with `.onDelete(perform:)`: inside a `List` it
  enables swipe to delete and the Edit mode delete buttons. `onDelete` receives the indices in `visible`, which the
  screen turns into ids.
- `NavigationLink(destination = { … })` pushes the destination with the system animation, back button and swipe back.
  It only works inside a `NavigationStack`, which the root screen provides in step 6.
- Without `onDelete`, a plain `for (task in visible) key(task.id) { … }` does the same thing as `ForEach`. For lists of
  hundreds of rows or more, use `LazyForEach` instead (see [Lists](lists.md)).

## 4. The detail screen

The detail screen edits one task. It receives the current `Task` and callbacks, never a mutable object, so the list
and the detail always show the same data.

```kotlin
@Composable
fun TaskDetail(task: Task, onRename: (String) -> Unit, onNotesChange: (String) -> Unit, onToggle: () -> Unit) {
    Form(modifier = Modifier.navigationTitle(task.title)) {
        Section(title = "Task") {
            TextField("Title", text = task.title, onTextChange = onRename)
            Toggle("Done", isOn = task.isDone, onIsOnChange = { onToggle() })
        }
        Section(title = "Notes") {
            TextField("Notes", text = task.notes, axis = Axis.vertical, onTextChange = onNotesChange, modifier = Modifier.lineLimit(3..8))
        }
    }
}
```

`TextField` is controlled too: each keystroke calls `onTextChange`, the view model updates, and the field recomposes
with the new text before SwiftUI draws the next frame, so typing never lags or jumps. The navigation title follows
the title as you type, because both read the same state.

## 5. The new-task sheet

The sheet has its own local state for the text being typed. It is plain `remember` state: it only lives while the
sheet is shown.

```kotlin
@Composable
fun NewTaskSheet(onAdd: (String) -> Unit, onCancel: () -> Unit) {
    var title by remember { mutableStateOf("") }
    NavigationStack {
        Form(
            modifier = Modifier
                .navigationTitle("New task")
                .toolbarItem(ToolbarItemPlacement.cancellationAction) {
                    Button("Cancel", action = onCancel)
                }
                .toolbarItem(ToolbarItemPlacement.confirmationAction) {
                    Button("Add", modifier = Modifier.disabled(title.isBlank()), action = { onAdd(title) })
                },
        ) {
            TextField("Title", text = title, onTextChange = { title = it }, modifier = Modifier.onSubmit { if (title.isNotBlank()) onAdd(title) })
        }
    }
}
```

SwiftUI's `.toolbar { ToolbarItem(placement:) { … } }` is `Modifier.toolbarItem(placement) { … }`, one per item.
`Modifier.toolbar { … }` also exists for content without a placement.

## 6. Putting it together

The root composable owns the view model and the UI state that isn't business state (whether the sheet is shown,
the filter), and wires the callbacks:

```kotlin
@Composable
fun TasksApp() {
    val vm = viewModel { TasksViewModel() }
    val tasks by vm.tasks.collectAsState()
    var showCompleted by remember { mutableStateOf(true) }
    var showNewTask by remember { mutableStateOf(false) }

    // Screen modifiers, shared by the list and the empty state.
    val screen = Modifier
        .navigationTitle("Tasks")
        .toolbarItem(ToolbarItemPlacement.primaryAction) {
            Button("New task", systemImage = SFSymbol.plus) { showNewTask = true }
        }
        .sheet(isPresented = showNewTask, onIsPresentedChange = { showNewTask = it }) {
            NewTaskSheet(
                onAdd = { vm.add(it); showNewTask = false },
                onCancel = { showNewTask = false },
            )
        }

    NavigationStack {
        if (tasks.isEmpty()) {
            ContentUnavailableView("No tasks", systemImage = SFSymbol.checklist, description = "Tap + to add one.", modifier = screen)
        } else {
            TaskList(
                tasks = tasks,
                showCompleted = showCompleted,
                onShowCompletedChange = { showCompleted = it },
                onToggle = vm::toggle,
                onDelete = vm::delete,
                detail = { task -> TaskDetailEntry(vm, task.id) },
                modifier = screen,
            )
        }
    }
}

/** Reads the task by id, so the detail follows edits and disappears cleanly if the task is deleted. */
@Composable
fun TaskDetailEntry(vm: TasksViewModel, id: Int) {
    val tasks by vm.tasks.collectAsState()
    val task = tasks.firstOrNull { it.id == id } ?: return
    TaskDetail(
        task = task,
        onRename = { vm.rename(id, it) },
        onNotesChange = { vm.setNotes(id, it) },
        onToggle = { vm.toggle(id) },
    )
}

fun tasksApp() = composeSwiftUI { TasksApp() }
```

Why `TaskDetailEntry` looks the task up again: the `NavigationLink` destination is captured when the row is
composed. Passing the id and reading the latest state from the view model keeps the detail in sync with every edit.

`viewModel { }` returns the same `TasksViewModel` for as long as the `ComposeView` hosting `tasksApp()` lives, like
`viewModel()` in Jetpack Compose. The view model is cleared when that `ComposeView` goes away.

A `Modifier` is a plain immutable value: build it once, pass it around, chain it. Here the same chain goes on
whichever view is shown, so the title, the toolbar button and the sheet stay in place when the last task is deleted.
As in SwiftUI, `navigationTitle` and `toolbar` go on a view inside the `NavigationStack`, not on the stack itself.

## 7. Show it from Swift

The file is `Tasks.kt`, so Kotlin/Native exposes `tasksApp()` as `TasksKt.tasksApp()`:

```swift
import SwiftUI

@main
struct TasksApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeView { TasksKt.tasksApp() }
        }
    }
}
```

Build and run. The list, the swipe actions, the push to the detail, the sheet and the toggle are the system's own
SwiftUI views; only their state and callbacks are in Kotlin.

## 8. Programmatic navigation

`NavigationLink(destination:)` is the simplest way to push. When Kotlin needs to own the navigation state, for
example to open a task right after creating it or to restore a deep link, use the path form of `NavigationStack`:
SwiftUI's `NavigationStack(path:)` with `navigationDestination(for:)`.

```kotlin
@Composable
fun TasksWithPath(vm: TasksViewModel) {
    val tasks by vm.tasks.collectAsState()
    var path by remember { mutableStateOf(listOf<Int>()) }
    NavigationStack(
        path = path,
        onPathChange = { path = it },
        destination = { id -> TaskDetailEntry(vm, id) },
    ) {
        List(modifier = Modifier.navigationTitle("Tasks")) {
            for (task in tasks) key(task.id) {
                NavigationLink(value = task.id) { Text(task.title) }
            }
            Button("Add and open") {
                vm.add("New task")
                path = listOf(vm.tasks.value.last().id)
            }
        }
    }
}
```

`NavigationLink(value = task.id)` pushes its value (`onPathChange` receives `path + task.id`), and assigning `path`
pushes from code, as "Add and open" does; the back button and swipe back call `onPathChange` with the shortened list. The path
holds any Kotlin values (ids, sealed route classes). If you use Navigation 3, `NavDisplay(backStack)` does the same
with a `NavBackStack`; see [Navigation](navigation.md).

## Where to go next

- [How it works](how-it-works.md): what happens between `Text("…")` in Kotlin and a SwiftUI `Text` on screen.
- [API mapping](api-mapping.md): translate any SwiftUI snippet to Kotlin.
- [Lists](lists.md), [Navigation](navigation.md) and [Presentation](presentation.md) go deeper into what this
  tutorial used.
