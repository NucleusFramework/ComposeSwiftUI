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

    const val MULTI_SELECTION_LIST = 100_030
    const val NUMBER_FIELD = 100_031
    const val PASTE_BUTTON = 100_032
    const val MESH_GRADIENT = 100_033
    const val MULTI_DATE_PICKER = 100_034

    const val SEARCH_TOKENS = 100_028
    const val SEARCH_FOCUSED = 100_029
    const val ACCESSIBILITY_FOCUSED = 100_030
    const val GLASS_EFFECT_ID = 100_031
    const val GLASS_EFFECT_UNION = 100_032
    const val FOCUS_SCOPE = 100_033
    const val GEOMETRY_CHANGE = 100_034
    const val SCROLL_GEOMETRY_CHANGE = 100_035
    const val SCROLL_TARGET_VISIBILITY = 100_036
    const val FILE_IMPORTER = 100_037
    const val FILE_EXPORTER = 100_038
    const val DROP_DESTINATION = 100_039
    const val DRAGGABLE = 100_040
    const val PHASE_ANIMATOR = 100_041
    const val SYMBOL_EFFECT = 100_042
    const val KEYBOARD_TYPE = 100_043
    const val TEXT_CONTENT_TYPE = 100_044
    const val ROTATION_3D = 100_045
    const val DYNAMIC_TYPE_RANGE = 100_046
    const val CONTAINER_RELATIVE_FRAME = 100_047
    const val PRESENTATION_DETENTS = 100_048
    const val ACCESSIBILITY_LINKED_GROUP = 100_049
    const val ACCESSIBILITY_LABELED_PAIR = 100_050
    const val PREFERS_DEFAULT_FOCUS = 100_051
}

// ---- AsyncImage --------------------------------------------------------------------------------

/** Load state of an [AsyncImage]. */
@HiddenFromObjC
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
@HiddenFromObjC
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
@HiddenFromObjC
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

@HiddenFromObjC
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
@HiddenFromObjC
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
@HiddenFromObjC
public class TableColumns<T> internal constructor() {
    internal class Column<T>(
        val title: String,
        val width: List<Double?>,
        val alignment: TableColumnAlignment?,
        val content: @Composable (T) -> Unit,
    )

    internal val columns = ArrayList<Column<T>>()

    public fun column(
        title: String,
        width: Double? = null,
        minWidth: Double? = null,
        maxWidth: Double? = null,
        alignment: TableColumnAlignment? = null,
        content: @Composable (T) -> Unit,
    ) {
        columns += Column(title, listOf(minWidth, width, maxWidth), alignment, content)
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
    children: ((T) -> List<T>?)? = null,
    customizable: Boolean = false,
    columns: TableColumns<T>.() -> Unit,
) {
    // children: SwiftUI `Table(_:children:…)` disclosure rows; customizable: columns the user can hide and reorder
    val cols = TableColumns<T>().apply(columns).columns
    SwiftView(
        InteractiveIds.TABLE,
        listOf(
            cols.map { it.title },
            cols.map { it.width },
            selection.toList(),
            onSelectionChange?.let { cb -> SValueCallback { cb((it as List<*>).filterNotNull().toSet()) } },
            children != null,
            cols.map { it.alignment },
            customizable,
        ),
        modifier,
    ) { TableRows(rows, key, children, cols) }
}

@Composable
private fun <T> TableRows(rows: List<T>, key: (T) -> Any, children: ((T) -> List<T>?)?, cols: List<TableColumns.Column<T>>) {
    for (row in rows) {
        val k = key(row)
        key(k) {
            SwiftView(InteractiveIds.TABLE_ROW, listOf(k), Modifier) {
                for (col in cols) SwiftView(InteractiveIds.GROUP, emptyList(), Modifier) { col.content(row) }
                val sub = children?.invoke(row)
                if (sub != null) SwiftView(CompletionIds.TABLE_CHILD_ROWS, emptyList(), Modifier) { TableRows(sub, key, children, cols) }
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
@HiddenFromObjC
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
@HiddenFromObjC
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

// ---- selection, search tokens, number fields ---------------------------------------------------

/**
 * SwiftUI `List(selection: Binding<SelectionValue?>)`: tapping a row marked `Modifier.tag(key)` selects it (null: no
 * selection) and highlights it, on iOS, macOS and visionOS.
 */
@HiddenFromObjC
@Composable
public fun List(
    selectedKey: Any?,
    onSelectedKeyChange: (Any?) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwiftView(100_063, listOf(selectedKey, SValueCallback { onSelectedKeyChange(it) }), modifier, content = content)
}

/**
 * SwiftUI `List(selection: Binding<Set<…>>)`: rows marked `Modifier.tag(key)` can be selected together (on iOS, in
 * edit mode: add an `EditButton`). Single selection is `List(selectedKey, onSelectedKeyChange)`.
 */
@HiddenFromObjC
@Composable
public fun List(
    selectedKeys: Set<Any>,
    onSelectedKeysChange: (Set<Any>) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwiftView(
        InteractiveIds.MULTI_SELECTION_LIST,
        listOf(selectedKeys.toList(), SValueCallback { onSelectedKeysChange((it as List<*>).filterNotNull().toSet()) }),
        modifier,
        content = content,
    )
}

/**
 * SwiftUI `searchable(text:tokens:prompt:token:)` (iOS, macOS): search text plus [tokens] shown as chips by [token];
 * the user removes a token with backspace, which calls [onTokensChange].
 */
@HiddenFromObjC
public fun <T> Modifier.searchable(
    text: String,
    onTextChange: (String) -> Unit,
    tokens: List<T>,
    onTokensChange: (List<T>) -> Unit,
    key: (T) -> Any,
    prompt: String? = null,
    token: @Composable (T) -> Unit,
): Modifier = element(
    InteractiveIds.SEARCH_TOKENS,
    text,
    SValueCallback { onTextChange(it as String) },
    tokens.size,
    SValueCallback { kept -> onTokensChange((kept as List<*>).map { tokens[(it as Number).toInt()] }) },
    prompt,
    slotContent {
        for (t in tokens) key(key(t)) { SwiftView(InteractiveIds.GROUP, emptyList(), Modifier) { token(t) } }
    },
)

/** Number style of a [NumberField]. */
@HiddenFromObjC
public sealed class NumberFormat(internal val code: String?) {
    public object Number : NumberFormat(null)
    public object Percent : NumberFormat(null)
    /** A currency amount, [currencyCode] being an ISO 4217 code such as "EUR". */
    public class Currency(currencyCode: String) : NumberFormat(currencyCode)

    internal val kind: Int get() = when (this) { Number -> 0; Percent -> 1; is Currency -> 2 }
}

/**
 * SwiftUI `TextField(_:value:format:prompt:)` for a number: SwiftUI parses and formats the text with [format] and
 * calls [onValueChange] when the user commits a valid value.
 */
@HiddenFromObjC
@Composable
public fun NumberField(
    title: String,
    value: Double,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    format: NumberFormat = NumberFormat.Number,
    prompt: String? = null,
) {
    SwiftView(
        InteractiveIds.NUMBER_FIELD,
        listOf(title, value, SValueCallback { onValueChange((it as Number).toDouble()) }, format.kind, format.code, prompt),
        modifier,
    )
}

// ---- focus -------------------------------------------------------------------------------------

/**
 * SwiftUI `focused(_:equals:)`: this field is focused while [focusedValue] equals [value]; focusing it calls
 * [onFocusChange] with [value], leaving it with null.
 */
@HiddenFromObjC
public fun Modifier.focused(value: Any, focusedValue: Any?, onFocusChange: (Any?) -> Unit): Modifier =
    focused(isFocused = focusedValue == value, onFocusChange = { focused ->
        if (focused) onFocusChange(value) else if (focusedValue == value) onFocusChange(null)
    })

/** SwiftUI `searchFocused(_:)` (iOS 18, macOS 15): focus of the search field of the enclosing `searchable`. */
@HiddenFromObjC
public fun Modifier.searchFocused(isFocused: Boolean, onFocusChange: (Boolean) -> Unit): Modifier =
    element(InteractiveIds.SEARCH_FOCUSED, isFocused, SValueCallback { onFocusChange(it as Boolean) })

/** SwiftUI `accessibilityFocused(_:)`: VoiceOver focus of this view. */
@HiddenFromObjC
public fun Modifier.accessibilityFocused(isFocused: Boolean, onFocusChange: (Boolean) -> Unit): Modifier =
    element(InteractiveIds.ACCESSIBILITY_FOCUSED, isFocused, SValueCallback { onFocusChange(it as Boolean) })

// ---- namespaces: Liquid Glass, focus scopes -----------------------------------------------------

/** SwiftUI `glassEffectID(_:in:)` (iOS 26): glass shapes with an id morph into each other inside a `GlassEffectContainer`. */
@HiddenFromObjC
public fun Modifier.glassEffectID(id: Any, namespace: NamespaceId): Modifier =
    element(InteractiveIds.GLASS_EFFECT_ID, id, namespace.id)

/** SwiftUI `glassEffectUnion(id:namespace:)` (iOS 26): views with the same id share one glass shape. */
@HiddenFromObjC
public fun Modifier.glassEffectUnion(id: Any, namespace: NamespaceId): Modifier =
    element(InteractiveIds.GLASS_EFFECT_UNION, id, namespace.id)

/** SwiftUI `focusScope(_:)` (macOS, tvOS, watchOS): limits default focus to this view, for `prefersDefaultFocus`. */
@HiddenFromObjC
public fun Modifier.focusScope(namespace: NamespaceId): Modifier = element(InteractiveIds.FOCUS_SCOPE, namespace.id)

// ---- geometry ----------------------------------------------------------------------------------

/** SwiftUI `onGeometryChange(for:of:action:)`: [action] receives the view frame in the global coordinate space. */
@HiddenFromObjC
public fun Modifier.onGeometryChange(action: (frame: CGRect) -> Unit): Modifier =
    element(InteractiveIds.GEOMETRY_CHANGE, SValueCallback { (it as? SExpr)?.let { e -> action(cgValue(e) as CGRect) } })

/** Scroll position of a scroll view, for [onScrollGeometryChange]. */
@HiddenFromObjC
public data class ScrollMetrics(
    val contentOffset: CGPoint,
    val contentSize: CGSize,
    val containerSize: CGSize,
)

/** SwiftUI `onScrollGeometryChange(for:of:action:)` (iOS 18, macOS 15): offset and sizes while scrolling. */
@HiddenFromObjC
public fun Modifier.onScrollGeometryChange(action: (ScrollMetrics) -> Unit): Modifier =
    element(InteractiveIds.SCROLL_GEOMETRY_CHANGE, SValueCallback {
        val d = (it as List<*>).map { v -> (v as Number).toDouble() }
        action(ScrollMetrics(CGPoint(d[0], d[1]), CGSize(d[2], d[3]), CGSize(d[4], d[5])))
    })

/**
 * SwiftUI `onScrollTargetVisibilityChange(idType:threshold:_:)` (iOS 18, macOS 15): the ids (`Modifier.id`) of the
 * scroll view's children that are at least [threshold] visible.
 */
@HiddenFromObjC
public fun Modifier.onScrollTargetVisibilityChange(threshold: Double = 0.5, action: (List<Any>) -> Unit): Modifier =
    element(InteractiveIds.SCROLL_TARGET_VISIBILITY, threshold, SValueCallback { action((it as List<*>).filterNotNull()) })

// ---- files, pasteboard, drag and drop -----------------------------------------------------------

/**
 * SwiftUI `fileImporter(isPresented:allowedContentTypes:allowsMultipleSelection:onCompletion:)` (iOS, macOS):
 * [onCompletion] receives the picked file URLs, which are security-scoped (start accessing them before reading).
 */
@HiddenFromObjC
public fun Modifier.fileImporter(
    isPresented: Boolean,
    onIsPresentedChange: (Boolean) -> Unit,
    allowedContentTypes: List<String>,
    allowsMultipleSelection: Boolean = false,
    onFailure: (message: String) -> Unit = {},
    onCompletion: (urls: List<String>) -> Unit,
): Modifier = element(
    InteractiveIds.FILE_IMPORTER,
    isPresented,
    SValueCallback { onIsPresentedChange(it as Boolean) },
    allowedContentTypes,
    allowsMultipleSelection,
    SValueCallback { onCompletion((it as List<*>).map { u -> u as String }) },
    SValueCallback { onFailure(it as String) },
)

/** SwiftUI `fileExporter(isPresented:document:contentType:defaultFilename:onCompletion:)` for a text file (iOS, macOS). */
@HiddenFromObjC
public fun Modifier.fileExporter(
    isPresented: Boolean,
    onIsPresentedChange: (Boolean) -> Unit,
    text: String,
    contentType: String = "public.plain-text",
    defaultFilename: String? = null,
    onFailure: (message: String) -> Unit = {},
    onCompletion: (url: String) -> Unit = {},
): Modifier = element(
    InteractiveIds.FILE_EXPORTER,
    isPresented,
    SValueCallback { onIsPresentedChange(it as Boolean) },
    text,
    contentType,
    defaultFilename,
    SValueCallback { onCompletion(it as String) },
    SValueCallback { onFailure(it as String) },
)

/** SwiftUI `PasteButton(payloadType: String.self)` (iOS, macOS): pastes the pasteboard's text items. */
@HiddenFromObjC
@Composable
public fun PasteButton(modifier: Modifier = Modifier, onPaste: (List<String>) -> Unit) {
    SwiftView(InteractiveIds.PASTE_BUTTON, listOf(SValueCallback { onPaste((it as List<*>).map { s -> s as String }) }), modifier)
}

/** SwiftUI `draggable(_:)` with a text payload (iOS, macOS). */
@HiddenFromObjC
public fun Modifier.draggable(text: String): Modifier = element(InteractiveIds.DRAGGABLE, text)

/**
 * SwiftUI `dropDestination(for: String.self, action:isTargeted:)` (iOS, macOS): [onDrop] receives the dropped text
 * items and the drop location, and returns whether it accepted them.
 */
@HiddenFromObjC
public fun Modifier.dropDestination(
    isTargeted: (Boolean) -> Unit = {},
    onDrop: (items: List<String>, location: CGPoint) -> Boolean,
): Modifier = element(
    InteractiveIds.DROP_DESTINATION,
    object : DropHandler {
        override fun onDrop(items: List<String>, x: Double, y: Double): Boolean = onDrop(items, CGPoint(x, y))
    },
    SValueCallback { isTargeted(it as Boolean) },
)

// ---- animations and symbols --------------------------------------------------------------------

/**
 * SwiftUI `phaseAnimator(_:trigger:content:animation:)`: the view goes through [phases] in order, animated with
 * [animation]; it loops while [trigger] is null, and runs once each time [trigger] changes otherwise.
 */
@HiddenFromObjC
public fun Modifier.phaseAnimator(phases: List<VisualEffects>, trigger: Any? = null, animation: Animation? = null): Modifier =
    element(InteractiveIds.PHASE_ANIMATOR, phases.map { it.toList() }, trigger, animation)

/** SF Symbol animations for [symbolEffect]. */
@HiddenFromObjC
public enum class SymbolEffectKind { Bounce, Pulse, VariableColor, Scale, Appear, Disappear, Wiggle, Rotate, Breathe }

/** SwiftUI `symbolEffect(_:options:isActive:)`: a continuous symbol animation while [isActive]. */
@HiddenFromObjC
public fun Modifier.symbolEffect(effect: SymbolEffectKind, isActive: Boolean = true): Modifier =
    element(InteractiveIds.SYMBOL_EFFECT, effect.ordinal, isActive, null, false)

/** SwiftUI `symbolEffect(_:options:value:)`: plays [effect] once each time [value] changes. */
@HiddenFromObjC
public fun Modifier.symbolEffect(effect: SymbolEffectKind, value: Any): Modifier =
    element(InteractiveIds.SYMBOL_EFFECT, effect.ordinal, true, value, true)

/** A mesh of colors (SwiftUI `MeshGradient`, iOS 18): [points] are `x, y` pairs in unit space, row by row. */
@HiddenFromObjC
@Composable
public fun MeshGradient(
    width: Int,
    height: Int,
    points: List<Double>,
    colors: List<Color>,
    modifier: Modifier = Modifier,
    smoothsColors: Boolean = true,
) {
    SwiftView(InteractiveIds.MESH_GRADIENT, listOf(width, height, points, colors, smoothsColors), modifier)
}

// ---- keyboard and text input --------------------------------------------------------------------

/** UIKit keyboards for [keyboardType] (iOS, tvOS). */
@HiddenFromObjC
public enum class KeyboardType {
    Default, AsciiCapable, NumbersAndPunctuation, Url, NumberPad, PhonePad, NamePhonePad, EmailAddress, DecimalPad,
    Twitter, WebSearch, AsciiCapableNumberPad,
}

/** SwiftUI `keyboardType(_:)` (iOS, tvOS; ignored elsewhere). */
@HiddenFromObjC
public fun Modifier.keyboardType(type: KeyboardType): Modifier = element(InteractiveIds.KEYBOARD_TYPE, type.ordinal)

/** Semantic content of a text field, for [textContentType] (AutoFill, keyboard suggestions). */
@HiddenFromObjC
public enum class TextContentType {
    Username, Password, NewPassword, OneTimeCode, EmailAddress, TelephoneNumber, Name, GivenName, FamilyName,
    FullStreetAddress, PostalCode, AddressCity, Url, CreditCardNumber,
}

/** SwiftUI `textContentType(_:)` (iOS, tvOS, macOS; macOS knows username, password and one-time code). */
@HiddenFromObjC
public fun Modifier.textContentType(type: TextContentType): Modifier = element(InteractiveIds.TEXT_CONTENT_TYPE, type.ordinal)

// ---- MultiDatePicker ---------------------------------------------------------------------------

/** A calendar day, for [MultiDatePicker]. */
@HiddenFromObjC
public data class Day(val year: Int, val month: Int, val day: Int)

/** SwiftUI `MultiDatePicker(_:selection:)` (iOS; nothing elsewhere): pick several days. */
@HiddenFromObjC
@Composable
public fun MultiDatePicker(
    title: String,
    selection: Set<Day>,
    onSelectionChange: (Set<Day>) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwiftView(
        InteractiveIds.MULTI_DATE_PICKER,
        listOf(
            title,
            selection.map { listOf(it.year, it.month, it.day) },
            SValueCallback { days ->
                onSelectionChange((days as List<*>).map { d ->
                    val c = (d as List<*>).map { (it as Number).toInt() }
                    Day(c[0], c[1], c[2])
                }.toSet())
            },
        ),
        modifier,
    )
}

// ---- 3D rotation, type size ranges, container-relative frames, sheet detents --------------------

/** SwiftUI `rotation3DEffect(_:axis:anchor:anchorZ:perspective:)` around the axis ([x], [y], [z]). */
@HiddenFromObjC
public fun Modifier.rotation3DEffect(
    angle: Angle,
    x: Double,
    y: Double,
    z: Double,
    anchor: UnitPoint = UnitPoint.center,
    anchorZ: Double = 0.0,
    perspective: Double = 1.0,
): Modifier = element(InteractiveIds.ROTATION_3D, angle, x, y, z, anchor, anchorZ, perspective)

/** SwiftUI `dynamicTypeSize(_:)` with a range: limits Dynamic Type between [min] and [max] (null = unbounded). */
@HiddenFromObjC
public fun Modifier.dynamicTypeSize(min: DynamicTypeSize? = null, max: DynamicTypeSize? = null): Modifier =
    element(InteractiveIds.DYNAMIC_TYPE_RANGE, min, max)

/**
 * SwiftUI `containerRelativeFrame(_:alignment:_:)`: [length] maps the container length along an axis
 * (0 horizontal, 1 vertical) to this view's length.
 */
@HiddenFromObjC
public fun Modifier.containerRelativeFrame(
    axes: Axis.Set,
    alignment: Alignment = Alignment.center,
    length: (containerLength: Double, axis: Int) -> Double,
): Modifier = element(
    InteractiveIds.CONTAINER_RELATIVE_FRAME,
    axes,
    alignment,
    object : LengthProvider {
        override fun length(length: Double, axis: Int): Double = length(length, axis)
    },
)

/** SwiftUI `presentationDetents(_:selection:)`: the sheet detent, among [detents], driven by Kotlin state. */
@HiddenFromObjC
public fun Modifier.presentationDetents(
    detents: List<PresentationDetent>,
    selection: PresentationDetent,
    onSelectionChange: (PresentationDetent) -> Unit,
): Modifier = element(
    InteractiveIds.PRESENTATION_DETENTS,
    detents,
    detents.indexOf(selection),
    SValueCallback { i -> detents.getOrNull((i as Number).toInt())?.let(onSelectionChange) },
)

// ---- accessibility groups and default focus -----------------------------------------------------

/** SwiftUI `accessibilityLinkedGroup(id:in:)`: VoiceOver links the views sharing [id]. */
@HiddenFromObjC
public fun Modifier.accessibilityLinkedGroup(id: Any, namespace: NamespaceId): Modifier =
    element(InteractiveIds.ACCESSIBILITY_LINKED_GROUP, id, namespace.id)

/** SwiftUI `accessibilityLabeledPair(role:id:in:)`: pairs a label view and its content for VoiceOver. */
@HiddenFromObjC
public fun Modifier.accessibilityLabeledPair(role: AccessibilityLabeledPairRole, id: Any, namespace: NamespaceId): Modifier =
    element(InteractiveIds.ACCESSIBILITY_LABELED_PAIR, role, id, namespace.id)

/** SwiftUI `prefersDefaultFocus(_:in:)` (macOS, tvOS, watchOS), inside a [focusScope]. */
@HiddenFromObjC
public fun Modifier.prefersDefaultFocus(prefersDefaultFocus: Boolean = true, namespace: NamespaceId): Modifier =
    element(InteractiveIds.PREFERS_DEFAULT_FOCUS, prefersDefaultFocus, namespace.id)
