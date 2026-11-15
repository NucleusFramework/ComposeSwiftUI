@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@file:Suppress("FunctionName")

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC
import kotlin.time.Clock

// Hand-written APIs for SwiftUI views and modifiers whose content depends on values only SwiftUI has
// (load phases, scroll proxies, timeline dates, key presses, drawing contexts, table rows, style states).
// Swift side: Interactive.swift. View kinds 100_020…, modifier ids 100_020….

internal object InteractiveIds {
    const val ASYNC_IMAGE = 100_020
    const val ASYNC_IMAGE_CONTENT = 100_021
    const val SCROLL_VIEW_READER = 100_022
    const val TIMELINE_VIEW = 100_023
    const val CANVAS = 100_024
    const val TABLE = 100_025
    const val TABLE_ROW = 100_026
    const val GROUP = 100_027
    const val STYLE_LABEL = 100_028
    const val STYLE_ICON = 100_029

    const val KEY_PRESS = 100_020
    const val SCROLL_TRANSITION = 100_021
    const val VISUAL_EFFECT = 100_022
    const val HORIZONTAL_GUIDE = 100_023
    const val VERTICAL_GUIDE = 100_024
    const val BUTTON_STYLE = 100_025
    const val TOGGLE_STYLE = 100_026
    const val LABEL_STYLE = 100_027
}

// ---- AsyncImage --------------------------------------------------------------------------------

/** Load state of an [AsyncImage]. */
public enum class AsyncImageState { Empty, Success, Failure }

/**
 * SwiftUI `AsyncImage(url:scale:transaction:content:)`: [content] runs for each [AsyncImageState]; in
 * [AsyncImageState.Success], [AsyncImageContent] shows the loaded image (Image modifiers such as `resizable()` apply).
 */
@HiddenFromObjC
@Composable
public fun AsyncImage(
    url: String?,
    modifier: Modifier = Modifier,
    scale: Double = 1.0,
    animation: Animation? = null,
    content: @Composable (AsyncImageState) -> Unit,
) {
    var phase by remember(url) { mutableStateOf(AsyncImageState.Empty) }
    SwiftView(
        InteractiveIds.ASYNC_IMAGE,
        listOf(url, scale, animation, SValueCallback { phase = AsyncImageState.entries[(it as Number).toInt()] }),
        modifier,
    ) { content(phase) }
}

/** The image loaded by the enclosing [AsyncImage] (nothing in other phases). */
@HiddenFromObjC
@Composable
public fun AsyncImageContent(modifier: Modifier = Modifier) {
    SwiftView(InteractiveIds.ASYNC_IMAGE_CONTENT, emptyList(), modifier)
}

// ---- ScrollViewReader --------------------------------------------------------------------------

/** SwiftUI `ScrollViewProxy`: scrolls the enclosing scroll view to the view marked `Modifier.id(id)`. */
public class ScrollProxy internal constructor(private val handle: () -> ScrollProxyHandle?) {
    public fun scrollTo(id: Any, anchor: UnitPoint? = null) {
        handle()?.scrollTo(id, anchor)
    }
}

/** SwiftUI `ScrollViewReader`: call `proxy.scrollTo(id)` from an action, inside `withAnimation` to animate. */
@HiddenFromObjC
@Composable
public fun ScrollViewReader(modifier: Modifier = Modifier, content: @Composable (ScrollProxy) -> Unit) {
    val holder = remember { arrayOfNulls<ScrollProxyHandle>(1) }
    val proxy = remember { ScrollProxy { holder[0] } }
    SwiftView(InteractiveIds.SCROLL_VIEW_READER, listOf(SValueCallback { holder[0] = it as? ScrollProxyHandle }), modifier) {
        content(proxy)
    }
}

// ---- TimelineView ------------------------------------------------------------------------------

/**
 * SwiftUI `TimelineView(schedule) { context in }`: [content] recomposes at each date of [schedule]
 * (`TimelineSchedule.everyMinute`, `.periodic(from, by)`, `.animation`…), with the date in epoch seconds.
 */
@HiddenFromObjC
@Composable
public fun TimelineView(schedule: TimelineSchedule, modifier: Modifier = Modifier, content: @Composable (date: Double) -> Unit) {
    var date by remember { mutableStateOf(Clock.System.now().toEpochMilliseconds() / 1000.0) }
    SwiftView(InteractiveIds.TIMELINE_VIEW, listOf(schedule, SValueCallback { date = (it as Number).toDouble() }), modifier) {
        content(date)
    }
}

// ---- onKeyPress --------------------------------------------------------------------------------

/** A key press seen by [onKeyPress]. */
public class KeyPressEvent internal constructor(
    /** The characters the key produces (with modifiers applied). */
    public val characters: String,
    /** The key, as a character (`KeyEquivalent.character`). */
    public val key: String,
    public val phase: KeyPressPhase,
    private val modifierBits: Int,
) {
    public val shift: Boolean get() = modifierBits and (1 shl 17) != 0
    public val control: Boolean get() = modifierBits and (1 shl 18) != 0
    public val option: Boolean get() = modifierBits and (1 shl 19) != 0
    public val command: Boolean get() = modifierBits and (1 shl 20) != 0
}

public enum class KeyPressPhase { Down, Repeat, Up }

/**
 * SwiftUI `onKeyPress(keys:phases:action:)` (not on watchOS): [action] returns true when it handled the
 * press. [keys] filters on key characters (`"a"`, `"\r"`…); empty = every key.
 */
@HiddenFromObjC
public fun Modifier.onKeyPress(
    keys: Set<String> = emptySet(),
    phases: Set<KeyPressPhase> = setOf(KeyPressPhase.Down, KeyPressPhase.Repeat),
    action: (KeyPressEvent) -> Boolean,
): Modifier = element(
    InteractiveIds.KEY_PRESS,
    object : KeyPressHandler {
        override fun onKeyPress(characters: String, key: String, modifiers: Int, phase: Int): Boolean =
            action(KeyPressEvent(characters, key, KeyPressPhase.entries[phase], modifiers))
    },
    phases.sumOf { 1 shl it.ordinal },
    keys.toList(),
)

// ---- Canvas ------------------------------------------------------------------------------------

/**
 * Drawing commands of a [Canvas], in points. State changes (translate, scale, rotate, opacity, clip) apply to
 * the following commands; wrap them in [withState] to scope them.
 */
public class DrawScope internal constructor(public val width: Double, public val height: Double) {
    internal val commands = ArrayList<Any?>()

    private fun path(build: PathBuilder.() -> Unit) = PathBuilder().apply(build).commands.toList()

    /** Fills a path built in points (`moveTo`, `lineTo`, `rect`, `ellipse`…). */
    public fun fill(style: ShapeStyle, path: PathBuilder.() -> Unit) { commands.add(listOf(1, path(path), style)) }

    public fun stroke(style: ShapeStyle, lineWidth: Double = 1.0, path: PathBuilder.() -> Unit) {
        commands.add(listOf(2, path(path), style, lineWidth))
    }

    /** Draws [text] at ([x], [y]), positioned by [anchor]. */
    public fun drawText(
        text: String,
        x: Double,
        y: Double,
        font: Font? = null,
        color: Color? = null,
        anchor: UnitPoint = UnitPoint.center,
    ) { commands.add(listOf(3, text, x, y, font, color, anchor)) }

    /** Draws an SF Symbol centered at ([x], [y]). */
    public fun drawSymbol(symbol: SFSymbol, x: Double, y: Double, font: Font? = null, color: Color? = null) {
        commands.add(listOf(11, symbol.name, x, y, font, color))
    }

    public fun translate(x: Double, y: Double) { commands.add(listOf(4, x, y)) }
    public fun scale(x: Double, y: Double = x) { commands.add(listOf(5, x, y)) }
    public fun rotate(degrees: Double) { commands.add(listOf(6, degrees)) }
    public fun opacity(value: Double) { commands.add(listOf(7, value)) }
    public fun clip(path: PathBuilder.() -> Unit) { commands.add(listOf(10, path(path))) }

    /** Runs [block] on a copy of the drawing state: its translate/scale/opacity/clip don't leak. */
    public fun withState(block: DrawScope.() -> Unit) {
        commands.add(listOf(8))
        block()
        commands.add(listOf(9))
    }
}

/** SwiftUI `Canvas { context, size in }`, drawn by [onDraw] in Kotlin each time SwiftUI renders it. */
@HiddenFromObjC
@Composable
public fun Canvas(modifier: Modifier = Modifier, opaque: Boolean = false, onDraw: DrawScope.() -> Unit) {
    val renderer = object : CanvasRenderer {
        override fun draw(width: Double, height: Double): List<Any?> = DrawScope(width, height).apply(onDraw).commands
    }
    SwiftView(InteractiveIds.CANVAS, listOf(renderer, opaque), modifier)
}

// ---- Table -------------------------------------------------------------------------------------

/** Columns of a [Table]: one [column] call per column, in order. */
public class TableColumns<T> internal constructor() {
    internal class Column<T>(
        val title: String,
        val width: List<Double?>,
        val content: @Composable (T) -> Unit,
    )

    internal val columns = ArrayList<Column<T>>()

    public fun column(
        title: String,
        width: Double? = null,
        minWidth: Double? = null,
        maxWidth: Double? = null,
        content: @Composable (T) -> Unit,
    ) {
        columns += Column(title, listOf(minWidth, width, maxWidth), content)
    }
}

/**
 * SwiftUI `Table(rows, selection:) { TableColumn… }` (iOS 17.4+, macOS 14.4+; a plain list elsewhere): one row per
 * item of [rows], identified by [key]. [selection] holds the selected keys; pass [onSelectionChange] to make rows
 * selectable.
 */
@HiddenFromObjC
@Composable
public fun <T> Table(
    rows: List<T>,
    key: (T) -> Any,
    modifier: Modifier = Modifier,
    selection: Set<Any> = emptySet(),
    onSelectionChange: ((Set<Any>) -> Unit)? = null,
    columns: TableColumns<T>.() -> Unit,
) {
    val cols = TableColumns<T>().apply(columns).columns
    SwiftView(
        InteractiveIds.TABLE,
        listOf(
            cols.map { it.title },
            cols.map { it.width },
            selection.toList(),
            onSelectionChange?.let { cb -> SValueCallback { cb((it as List<*>).filterNotNull().toSet()) } },
        ),
        modifier,
    ) {
        for (row in rows) {
            val k = key(row)
            key(k) {
                SwiftView(InteractiveIds.TABLE_ROW, listOf(k), Modifier) {
                    for (col in cols) SwiftView(InteractiveIds.GROUP, emptyList(), Modifier) { col.content(row) }
                }
            }
        }
    }
}

// ---- OutlineGroup ------------------------------------------------------------------------------

/**
 * SwiftUI `OutlineGroup(data, children:)`: [items] as a tree of disclosure groups; [children] returns null for a
 * leaf. Put it in a `List` for SwiftUI's `List(data, children:)`.
 */
@HiddenFromObjC
@Composable
public fun <T> OutlineGroup(
    items: List<T>,
    key: (T) -> Any,
    children: (T) -> List<T>?,
    content: @Composable (T) -> Unit,
) {
    for (item in items) {
        key(key(item)) {
            val sub = children(item)
            if (sub == null) {
                content(item)
            } else {
                var expanded by remember { mutableStateOf(false) }
                DisclosureGroup(
                    isExpanded = expanded,
                    onIsExpandedChange = { expanded = it },
                    content = { OutlineGroup(sub, key, children, content) },
                    label = { content(item) },
                )
            }
        }
    }
}

// ---- scroll transitions, visual effects, alignment guides --------------------------------------

/** Effects returned by [scrollTransition] / [visualEffect]; defaults leave the view unchanged. */
public data class VisualEffects(
    val opacity: Double = 1.0,
    val scale: Double = 1.0,
    val offsetX: Double = 0.0,
    val offsetY: Double = 0.0,
    val rotationDegrees: Double = 0.0,
    val blurRadius: Double = 0.0,
) {
    internal fun toList() = listOf(opacity, scale, offsetX, offsetY, rotationDegrees, blurRadius)
}

/** Geometry of a view for [visualEffect]: its size and its frame origin in the global coordinate space. */
public class EffectGeometry internal constructor(
    public val width: Double,
    public val height: Double,
    public val minX: Double,
    public val minY: Double,
)

/**
 * SwiftUI `scrollTransition(_:axis:transition:)`: [transition] maps the phase value (-1 top/leading, 0 identity,
 * 1 bottom/trailing) to effects. Called by SwiftUI while scrolling.
 */
@HiddenFromObjC
public fun Modifier.scrollTransition(
    configuration: ScrollTransitionConfiguration = ScrollTransitionConfiguration.interactive,
    axis: Axis? = null,
    transition: (phase: Double, isIdentity: Boolean) -> VisualEffects,
): Modifier = element(
    InteractiveIds.SCROLL_TRANSITION,
    configuration,
    axis,
    object : EffectsProvider {
        override fun effects(phase: Double, isIdentity: Boolean, width: Double, height: Double, minX: Double, minY: Double) =
            transition(phase, isIdentity).toList()
    },
)

/** SwiftUI `visualEffect { content, geometry in }`: effects from the view's geometry, computed at layout time. */
@HiddenFromObjC
public fun Modifier.visualEffect(effect: (EffectGeometry) -> VisualEffects): Modifier = element(
    InteractiveIds.VISUAL_EFFECT,
    object : EffectsProvider {
        override fun effects(phase: Double, isIdentity: Boolean, width: Double, height: Double, minX: Double, minY: Double) =
            effect(EffectGeometry(width, height, minX, minY)).toList()
    },
)

/** SwiftUI `alignmentGuide(_:computeValue:)` for a horizontal guide, from the view's width and height. */
@HiddenFromObjC
public fun Modifier.alignmentGuide(guide: HorizontalAlignment, computeValue: (width: Double, height: Double) -> Double): Modifier =
    element(InteractiveIds.HORIZONTAL_GUIDE, guide, object : AlignmentGuideProvider {
        override fun compute(width: Double, height: Double) = computeValue(width, height)
    })

/** SwiftUI `alignmentGuide(_:computeValue:)` for a vertical guide. */
@HiddenFromObjC
public fun Modifier.alignmentGuide(guide: VerticalAlignment, computeValue: (width: Double, height: Double) -> Double): Modifier =
    element(InteractiveIds.VERTICAL_GUIDE, guide, object : AlignmentGuideProvider {
        override fun compute(width: Double, height: Double) = computeValue(width, height)
    })

// ---- navigationDestination(item:) --------------------------------------------------------------

/** SwiftUI `navigationDestination(item:destination:)`: pushed while [item] is non-null. */
@HiddenFromObjC
public fun <T : Any> Modifier.navigationDestination(item: T?, onDismiss: () -> Unit, destination: @Composable (T) -> Unit): Modifier =
    navigationDestination(
        isPresented = item != null,
        onIsPresentedChange = { if (!it) onDismiss() },
        destination = { if (item != null) destination(item) },
    )

// ---- custom styles -----------------------------------------------------------------------------

/** The label of the button / toggle being styled, inside [buttonStyle], [toggleStyle] and [labelStyle] bodies. */
@HiddenFromObjC
@Composable
public fun StyleLabel(modifier: Modifier = Modifier) {
    SwiftView(InteractiveIds.STYLE_LABEL, emptyList(), modifier)
}

/** The icon of the label being styled, inside a [labelStyle] body. */
@HiddenFromObjC
@Composable
public fun StyleIcon(modifier: Modifier = Modifier) {
    SwiftView(InteractiveIds.STYLE_ICON, emptyList(), modifier)
}

/**
 * A custom SwiftUI `ButtonStyle` for every button inside: [body] draws one button, around [StyleLabel], for its
 * pressed state. Both states are composed up front, so styling many buttons costs one composition.
 */
@HiddenFromObjC
public fun Modifier.buttonStyle(body: @Composable (isPressed: Boolean) -> Unit): Modifier =
    element(InteractiveIds.BUTTON_STYLE, slotContent { body(false) }, slotContent { body(true) })

/** A custom SwiftUI `ToggleStyle`: [body] draws the toggle for [isOn]; tapping it toggles. */
@HiddenFromObjC
public fun Modifier.toggleStyle(body: @Composable (isOn: Boolean) -> Unit): Modifier =
    element(InteractiveIds.TOGGLE_STYLE, slotContent { body(false) }, slotContent { body(true) })

/** A custom SwiftUI `LabelStyle`: [body] arranges [StyleIcon] and [StyleLabel] (the title). */
@HiddenFromObjC
public fun Modifier.labelStyle(body: @Composable () -> Unit): Modifier =
    element(InteractiveIds.LABEL_STYLE, slotContent(body))
