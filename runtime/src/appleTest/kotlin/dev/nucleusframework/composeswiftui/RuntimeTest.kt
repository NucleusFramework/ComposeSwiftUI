@file:OptIn(ExperimentalComposeSwiftUIApi::class)

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import dev.nucleusframework.composeswiftui.testing.runComposeSwiftUITest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RuntimeTest {
    private val errors = ArrayList<String>()

    @BeforeTest
    fun installErrorHook() {
        ComposeSwiftUI.onError = { source, e -> errors += "$source:${e.message}" }
    }

    @AfterTest
    fun removeErrorHook() {
        ComposeSwiftUI.onError = null
    }

    @Test
    fun clickRecomposes() = runComposeSwiftUITest {
        setContent {
            var count by remember { mutableIntStateOf(0) }
            VStack {
                Button("Increment", action = { count++ })
                Text("count=$count")
            }
        }
        onNodeWithText("count=0").assertExists()
        onNodeWithText("Increment").performClick()
        onNodeWithText("count=1").assertExists()
        onNodeWithText("count=0").assertDoesNotExist()
    }

    @Test
    fun findsByAccessibilityIdentifierAndWritesBindings() = runComposeSwiftUITest {
        setContent {
            var name by remember { mutableStateOf("") }
            VStack {
                TextField("Name", text = name, onTextChange = { name = it }, modifier = Modifier.accessibilityIdentifier("name"))
                Text("hello $name")
            }
        }
        onNodeWithAccessibilityIdentifier("name").performTextInput("Ada")
        onNodeWithText("hello Ada").assertExists()
    }

    @Test
    fun applierInsertsMovesAndRemovesChildren() = runComposeSwiftUITest {
        val items = mutableStateListOf("a", "b", "c")
        setContent {
            VStack { for (item in items) key(item) { Text(item) } }
        }
        val stack = host.root.children.single()
        fun texts() = stack.children.map { it.args.first() }
        assertEquals(listOf("a", "b", "c"), texts())
        val removed = stack.children[1]
        removed.observer = RecordingObserver()
        items.removeAt(1)
        waitForIdle()
        assertEquals(listOf("a", "c"), texts())
        assertNull(removed.observer, "a removed node lets go of its Swift model")
        items.add(0, items.removeAt(1))
        waitForIdle()
        assertEquals(listOf("c", "a"), texts())
        assertSame(stack.children, stack.children, "children are filtered once per change, not per read")
    }

    @Test
    fun recompositionKeepsCallbackHandlesAndSwapsTheirTarget() = runComposeSwiftUITest {
        var label by mutableStateOf("first")
        val clicks = ArrayList<String>()
        setContent {
            val current = label
            Button("Tap", action = { clicks += current })
        }
        val node = onNodeWithText("Tap").node
        val observer = RecordingObserver().also { node.observer = it }
        val action = node.args.filterIsInstance<SAction>().single()
        label = "second"
        waitForIdle()
        assertSame(action, node.args.filterIsInstance<SAction>().single(), "Swift keeps calling the same handle")
        assertEquals(0, observer.changes, "a new closure alone doesn't re-render the view")
        action.invoke()
        assertEquals(listOf("second"), clicks)
    }

    @Test
    fun argumentChangeNotifiesTheNode() = runComposeSwiftUITest {
        var text by mutableStateOf("a")
        setContent { Text(text) }
        val node = host.root.children.single()
        val observer = RecordingObserver().also { node.observer = it }
        val before = node.args
        text = "b"
        waitForIdle()
        assertNotSame(before, node.args)
        assertEquals("b", node.args.first())
        assertEquals(1, observer.changes)
    }

    @Test
    fun silentBindingWriteIsResyncedWithComposition() = runComposeSwiftUITest {
        setContent {
            // A controlled field that refuses edits
            TextField("Name", text = "fixed", onTextChange = { })
        }
        val node = host.root.children.single()
        val index = node.args.indexOf("fixed")
        node.setArgSilently(index, "typed")
        assertEquals("typed", node.args[index])
        node.args.filterIsInstance<SValueCallback>().single().invoke("typed")
        host.root.children.single().updateArgs(node.args.toMutableList().also { it[index] = "fixed" })
        assertEquals("fixed", node.args[index])
    }

    @Test
    fun callbackExceptionIsReportedNotThrown() = runComposeSwiftUITest {
        setContent { Button("Boom", action = { error("boom") }) }
        onNodeWithText("Boom").performClick()
        assertEquals(listOf("Callback:boom"), errors)
    }

    @Test
    fun compositionExceptionFailsOnlyThatHost() {
        var broken by mutableStateOf(false)
        val failing = ComposeSwiftUIHost { if (broken) error("render boom") else Text("ok") }
        val healthy = ComposeSwiftUIHost { Text("healthy") }
        failing.start()
        healthy.start()
        broken = true
        flushCompose()
        assertNotNull(failing.failure)
        assertTrue(failing.root.children.isEmpty())
        assertEquals(listOf("Composition:render boom"), errors)
        assertNull(healthy.failure)
        failing.dispose()
        healthy.dispose()
    }

    @Test
    fun effectExceptionIsReported() = runComposeSwiftUITest {
        setContent { LaunchedEffect(Unit) { error("effect boom") } }
        waitUntil { errors.isNotEmpty() }
        assertEquals("Composition:effect boom", errors.single())
        assertNotNull(host.failure)
    }

    @Test
    fun lifecycleFollowsVisibilityAndScenePhase() {
        val host = ComposeSwiftUIHost { }
        assertEquals(Lifecycle.State.INITIALIZED, host.lifecycle.currentState)
        host.start()
        assertEquals(Lifecycle.State.CREATED, host.lifecycle.currentState)
        host.setVisible(true)
        assertEquals(Lifecycle.State.RESUMED, host.lifecycle.currentState)
        host.setScenePhase("inactive")
        assertEquals(Lifecycle.State.STARTED, host.lifecycle.currentState)
        host.setScenePhase("background")
        assertEquals(Lifecycle.State.CREATED, host.lifecycle.currentState)
        host.dispose()
        assertEquals(Lifecycle.State.DESTROYED, host.lifecycle.currentState)
    }

    @Test
    fun rememberSaveableSurvivesSaveAndRestore() {
        val content = @androidx.compose.runtime.Composable {
            var count by rememberSaveable { mutableIntStateOf(0) }
            Button("count=$count", action = { count++ })
        }
        val first = ComposeSwiftUIHost(content).apply { start() }
        first.root.children.single().args.filterIsInstance<SAction>().single().invoke()
        flushCompose()
        val saved = first.saveState()
        first.dispose()
        val second = ComposeSwiftUIHost(content).apply {
            restoreState(saved)
            start()
        }
        assertEquals("count=1", second.root.children.single().args.first())
        second.dispose()
    }

    @Test
    fun flushRunsOnlyTheHostsWithPendingWork() {
        var a by mutableIntStateOf(0)
        val hostA = ComposeSwiftUIHost { Text("a=$a") }.apply { start() }
        val hostB = ComposeSwiftUIHost { Text("b") }.apply { start() }
        val framesB = hostB.frameCount
        a = 1
        flushCompose()
        assertEquals("a=1", hostA.root.children.single().args.first())
        assertEquals(framesB, hostB.frameCount, "an untouched host runs no frame")
        hostA.dispose()
        hostB.dispose()
    }

    @Test
    fun stateCodecRoundTrips() {
        val value = mapOf(
            "int" to 1, "long" to 9_007_199_254_740_993L, "double" to Double.NaN, "float" to 1.5f, "string" to "a\"b\n",
            "bool" to true, "null" to null, "list" to listOf(1, "x", listOf(2L)), "ints" to intArrayOf(1, 2),
            "state" to mutableIntStateOf(3), "boxed" to mutableStateOf("s"),
        )
        @Suppress("UNCHECKED_CAST")
        val decoded = StateCodec.decode(StateCodec.encode(value)) as Map<String, Any?>
        assertEquals(1, decoded["int"])
        assertEquals(9_007_199_254_740_993L, decoded["long"])
        assertTrue((decoded["double"] as Double).isNaN())
        assertEquals(1.5f, decoded["float"])
        assertEquals("a\"b\n", decoded["string"])
        assertEquals(true, decoded["bool"])
        assertTrue("null" in decoded && decoded["null"] == null)
        assertEquals(listOf(1, "x", listOf(2L)), decoded["list"])
        assertEquals(listOf(1, 2), (decoded["ints"] as IntArray).toList())
        assertEquals(3, (decoded["state"] as androidx.compose.runtime.MutableIntState).intValue)
        assertEquals("s", (decoded["boxed"] as androidx.compose.runtime.MutableState<*>).value)
    }

    private class RecordingObserver : SNodeObserver {
        var changes = 0
        override fun nodeDidChange() {
            changes++
        }
    }
}
