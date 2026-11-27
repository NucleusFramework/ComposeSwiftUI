@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@file:Suppress("FunctionName")

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC

// SwiftUI @FocusedValue and @FocusedSceneValue via token propagation.
// Kotlin values stay in Kotlin (wrapped in FocusValueBox so arbitrary objects, lambdas, or ViewModels never lose
// type fidelity across the bridge); SwiftUI propagates only a lightweight `[String]` token chain.
// Swift side: FocusTokens.swift. View kinds 100_068..100_069, modifier ids 100_119..100_120.

internal object FocusIds {
    const val FOCUSED_VALUE_READER = 100_068
    const val FOCUSED_SCENE_VALUE_READER = 100_069

    const val FOCUSED_VALUE = 100_119
    const val FOCUSED_SCENE_VALUE = 100_120
}

/** Opaque Kotlin wrapper keeping arbitrary Kotlin values intact without ObjC bridging. */
internal class FocusValueBox(val key: String, val value: Any?)

/**
 * A key identifying a value published through SwiftUI's focus hierarchy ([focusedValue], [focusedSceneValue]) and
 * observed with [FocusedValueReader] or [FocusedSceneValueReader].
 *
 * Values of any Kotlin type [T] (state holders, data classes, lambdas) are supported: SwiftUI propagates only
 * string tokens while the actual Kotlin objects stay in Kotlin memory.
 */
@HiddenFromObjC
public class FocusedValueKey<T>(public val name: String)

/**
 * SwiftUI `focusedValue(_:_:)`: publishes [value] under [key] while this view or one of its descendants has focus.
 * When nested views publish the same [key], the innermost focused view wins.
 */
@HiddenFromObjC
public fun <T> Modifier.focusedValue(key: FocusedValueKey<T>, value: T): Modifier =
    element(FocusIds.FOCUSED_VALUE, key.name, FocusValueBox(key.name, value))

/**
 * SwiftUI `focusedSceneValue(_:_:)`: publishes [value] under [key] while this view's scene / window is active
 * (regardless of which specific control inside the scene has focus).
 */
@HiddenFromObjC
public fun <T> Modifier.focusedSceneValue(key: FocusedValueKey<T>, value: T): Modifier =
    element(FocusIds.FOCUSED_SCENE_VALUE, key.name, FocusValueBox(key.name, value))

/**
 * Reads the currently focused view's value for [key] (SwiftUI `@FocusedValue`), recomposing [content] whenever focus
 * moves or the focused view updates its published value. Receives `null` when no focused view publishes [key].
 */
@HiddenFromObjC
@Composable
@Suppress("UNCHECKED_CAST")
public fun <T> FocusedValueReader(
    key: FocusedValueKey<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T?) -> Unit,
) {
    var value by remember { mutableStateOf<T?>(null) }
    SwiftView(
        FocusIds.FOCUSED_VALUE_READER,
        listOf(key.name, SValueCallback { raw ->
            val next = (raw as? FocusValueBox)?.value as T?
            if (next != value) value = next
        }),
        modifier,
    ) { content(value) }
}

/**
 * Reads the active scene's value for [key] (SwiftUI `@FocusedSceneValue`), recomposing [content] whenever the active
 * window changes or the scene updates its published value. Receives `null` when no active scene publishes [key].
 */
@HiddenFromObjC
@Composable
@Suppress("UNCHECKED_CAST")
public fun <T> FocusedSceneValueReader(
    key: FocusedValueKey<T>,
    modifier: Modifier = Modifier,
    content: @Composable (T?) -> Unit,
) {
    var value by remember { mutableStateOf<T?>(null) }
    SwiftView(
        FocusIds.FOCUSED_SCENE_VALUE_READER,
        listOf(key.name, SValueCallback { raw ->
            val next = (raw as? FocusValueBox)?.value as T?
            if (next != value) value = next
        }),
        modifier,
    ) { content(value) }
}
