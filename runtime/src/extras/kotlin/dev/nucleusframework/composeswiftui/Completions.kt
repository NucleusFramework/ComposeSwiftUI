@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@file:Suppress("FunctionName")

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import kotlin.native.HiddenFromObjC

// Hand-written APIs completing SwiftUI view building: editable lists, the remaining custom styles, text selection,
// accessibility rotors, custom alignments and layout values, mixed-state controls, pasteboard and Services commands
// (macOS), pencil and modifier-key events, AsyncImage requests. Swift side: Completions.swift.
// View kinds 100_050…, modifier ids 100_080…, hand-written values 8 and 9 (custom alignments).

internal object CompletionIds {
    const val EDITABLE_FOR_EACH = 100_050
    const val STYLE_CONTENT = 100_051
    const val STYLE_PROGRESS_FILL = 100_052
    const val SELECTION_TEXT_FIELD = 100_053
    const val SELECTION_TEXT_EDITOR = 100_054
    const val MIXED_TOGGLE = 100_055
    const val MIXED_PICKER = 100_056
    const val REQUEST_IMAGE = 100_057

    const val GROUP_BOX_STYLE = 100_080
    const val LABELED_CONTENT_STYLE = 100_081
    const val CONTROL_GROUP_STYLE = 100_082
    const val FORM_STYLE = 100_083
    const val MENU_STYLE = 100_084
    const val PROGRESS_VIEW_STYLE = 100_085
    const val ROTOR = 100_086
    const val ROTOR_ENTRY = 100_087
    const val ZOOM_ACTION = 100_088
    const val CHART_DESCRIPTOR = 100_089
    const val DIALOG_ICON = 100_090
    const val LAYOUT_VALUE = 100_091
    const val COPYABLE = 100_092
    const val CUTTABLE = 100_093
    const val PASTE_DESTINATION = 100_094
    const val COPY_COMMAND = 100_095
    const val CUT_COMMAND = 100_096
    const val PASTE_COMMAND = 100_097
    const val EXPORT_TO_SERVICES = 100_098
    const val IMPORT_FROM_SERVICES = 100_099
    const val MODIFIER_KEYS = 100_100
    const val PENCIL_DOUBLE_TAP = 100_101

    const val TICK_SLIDER = 100_058
    const val TAB_SECTION = 100_059
    const val SEARCH_UNAVAILABLE = 100_060
    const val TABLE_CHILD_ROWS = 100_061

    const val LINE_HEIGHT = 100_102
    const val ARRANGEMENT_STYLE = 100_103
    const val HINGE_CHANGE = 100_104
    const val REORDER_CONTAINER = 100_105
    const val DRAG_CONTAINER = 100_106
    const val DRAG_CONTAINER_SELECTION = 100_107
    const val DRAG_SESSION = 100_108
    const val DROP_SESSION = 100_109
    const val DROP_CONFIGURATION = 100_110
    const val EDIT_MODE = 100_111
    const val OPEN_URL_ACTION = 100_112
    const val POINTER_IMAGE = 100_113

    const val CUSTOM_HORIZONTAL_ALIGNMENT = 8
    const val CUSTOM_VERTICAL_ALIGNMENT = 9
    const val EXPLICIT_SCHEDULE = 10
    const val IMAGE_PAINT = 11
    const val CUSTOM_DETENT = 12
}

private fun textItems(block: () -> List<String>) = object : TextItemsProvider {
    override fun items(): List<String> = block()
}

// ---- custom styles (continued from Interactive.kt) ---------------------------------------------

/** The content of the container being styled, inside [groupBoxStyle], [labeledContentStyle], [controlGroupStyle],
 * [formStyle] and [menuStyle] bodies (for a menu: the menu itself). */
@HiddenFromObjC
@Composable
public fun StyleContent(modifier: Modifier = Modifier) {
    SwiftView(CompletionIds.STYLE_CONTENT, emptyList(), modifier)
}

/** The completed part of the progress view being styled by [progressViewStyle]: fills its frame's width up to the
 * fraction completed (indeterminate progress fills nothing). Color it with `Modifier.foregroundStyle`. */
@HiddenFromObjC
@Composable
public fun StyleProgressFill(modifier: Modifier = Modifier) {
    SwiftView(CompletionIds.STYLE_PROGRESS_FILL, emptyList(), modifier)
}

/** A custom SwiftUI `GroupBoxStyle`: [body] arranges [StyleLabel] and [StyleContent]. */
@HiddenFromObjC
public fun Modifier.groupBoxStyle(body: @Composable () -> Unit): Modifier =
    element(CompletionIds.GROUP_BOX_STYLE, slotContent(body))

/** A custom SwiftUI `LabeledContentStyle`: [body] arranges [StyleLabel] and [StyleContent]. */
@HiddenFromObjC
public fun Modifier.labeledContentStyle(body: @Composable () -> Unit): Modifier =
    element(CompletionIds.LABELED_CONTENT_STYLE, slotContent(body))

/** A custom SwiftUI `ControlGroupStyle`: [body] places [StyleContent] (the controls). */
@HiddenFromObjC
public fun Modifier.controlGroupStyle(body: @Composable () -> Unit): Modifier =
    element(CompletionIds.CONTROL_GROUP_STYLE, slotContent(body))

/** A custom SwiftUI `FormStyle`: [body] places [StyleContent] (the form's rows). */
@HiddenFromObjC
public fun Modifier.formStyle(body: @Composable () -> Unit): Modifier =
    element(CompletionIds.FORM_STYLE, slotContent(body))

/** A custom SwiftUI `MenuStyle`: [body] decorates [StyleContent] (the menu). */
@HiddenFromObjC
public fun Modifier.menuStyle(body: @Composable () -> Unit): Modifier =
    element(CompletionIds.MENU_STYLE, slotContent(body))

/** A custom SwiftUI `ProgressViewStyle`: [body] draws the progress with [StyleProgressFill] and [StyleLabel]. */
@HiddenFromObjC
public fun Modifier.progressViewStyle(body: @Composable () -> Unit): Modifier =
    element(CompletionIds.PROGRESS_VIEW_STYLE, slotContent(body))

// ---- text selection ----------------------------------------------------------------------------

/** A text selection in UTF-16 offsets: a caret when [start] == [end]. */
@HiddenFromObjC
public data class TextRange(val start: Int, val end: Int)

/**
 * SwiftUI `TextField(_:text:selection:prompt:axis:)` (iOS 18, macOS 15; plain field elsewhere): [selection] is the
 * selected range (null when the field isn't editing).
 */
@HiddenFromObjC
@Composable
public fun TextField(
    title: String,
    text: String,
    selection: TextRange?,
    onTextChange: (String) -> Unit,
    onSelectionChange: (TextRange?) -> Unit,
    modifier: Modifier = Modifier,
    axis: Axis? = null,
) {
    SwiftView(
        CompletionIds.SELECTION_TEXT_FIELD,
        listOf(title, text, SValueCallback { onTextChange(it as String) }, selection?.let { listOf(it.start, it.end) },
            SValueCallback { onSelectionChange(textRange(it)) }, axis),
        modifier,
    )
}

/** SwiftUI `TextEditor(text:selection:)` (iOS 18, macOS 15; plain editor elsewhere). */
@HiddenFromObjC
@Composable
public fun TextEditor(
    text: String,
    selection: TextRange?,
    onTextChange: (String) -> Unit,
    onSelectionChange: (TextRange?) -> Unit,
    modifier: Modifier = Modifier,
) {
    SwiftView(
        CompletionIds.SELECTION_TEXT_EDITOR,
        listOf(text, SValueCallback { onTextChange(it as String) }, selection?.let { listOf(it.start, it.end) },
            SValueCallback { onSelectionChange(textRange(it)) }),
        modifier,
    )
}

private fun textRange(value: Any?): TextRange? =
    (value as? List<*>)?.let { TextRange((it[0] as Number).toInt(), (it[1] as Number).toInt()) }

// ---- mixed-state controls ----------------------------------------------------------------------

/**
 * SwiftUI `Toggle(_:sources:isOn:)`: one toggle for several booleans, shown mixed when they differ; switching it
 * sets every source, reported by index.
 */
@HiddenFromObjC
@Composable
public fun Toggle(title: String, sources: List<Boolean>, onSourceChange: (index: Int, isOn: Boolean) -> Unit, modifier: Modifier = Modifier) {
    SwiftView(
        CompletionIds.MIXED_TOGGLE,
        listOf(title, sources, SValueCallback { v -> val l = v as List<*>; onSourceChange((l[0] as Number).toInt(), l[1] as Boolean) }),
        modifier,
    )
}

/**
 * SwiftUI `Picker(_:sources:selection:content:)`: one picker for several selections (tag the options with
 * `Modifier.tag`), shown mixed when they differ; picking sets every source, reported by index.
 */
@HiddenFromObjC
@Composable
public fun Picker(
    title: String,
    sources: List<Any>,
    onSourceChange: (index: Int, value: Any) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SwiftView(
        CompletionIds.MIXED_PICKER,
        listOf(title, sources, SValueCallback { v -> val l = v as List<*>; onSourceChange((l[0] as Number).toInt(), l[1]!!) }),
        modifier,
        content = content,
    )
}

// ---- AsyncImage request ------------------------------------------------------------------------

/** [AsyncImage] loading [url] with HTTP [headers] (SwiftUI `AsyncImage(request:)`, iOS 27; headers ignored before). */
@HiddenFromObjC
@Composable
public fun AsyncImage(
    url: String,
    headers: Map<String, String>,
    modifier: Modifier = Modifier,
    scale: Double = 1.0,
) {
    SwiftView(CompletionIds.REQUEST_IMAGE, listOf(url, headers.keys.toList(), headers.values.toList(), scale), modifier)
}

// ---- accessibility -----------------------------------------------------------------------------

/** An entry of an [accessibilityRotor]: the view marked `Modifier.id(id)` and the label VoiceOver reads. */
@HiddenFromObjC
public data class RotorEntry(val id: Any, val label: String)

/** SwiftUI `accessibilityRotor(_:entries:…)`: a VoiceOver rotor listing [entries] (views of this scroll view). */
@HiddenFromObjC
public fun Modifier.accessibilityRotor(name: String, entries: List<RotorEntry>): Modifier =
    element(CompletionIds.ROTOR, name, entries.map { it.id }, entries.map { it.label })

/** SwiftUI `accessibilityRotorEntry(id:in:)`: makes this view a rotor entry with [id] in [namespace]. */
@HiddenFromObjC
public fun Modifier.accessibilityRotorEntry(id: Any, namespace: NamespaceId): Modifier =
    element(CompletionIds.ROTOR_ENTRY, id, namespace.id)

/** SwiftUI `accessibilityZoomAction(_:)`: [action] receives +1 to zoom in, -1 to zoom out, and the location. */
@HiddenFromObjC
public fun Modifier.accessibilityZoomAction(action: (direction: Int, location: CGPoint) -> Unit): Modifier =
    element(CompletionIds.ZOOM_ACTION, SValueCallback { v ->
        val d = (v as List<*>).map { (it as Number).toDouble() }
        action(d[0].toInt(), CGPoint(d[1], d[2]))
    })

/** SwiftUI `accessibilityChartDescriptor(_:)` for a bar or line chart of [values] by category (Audio Graphs). */
@HiddenFromObjC
public fun Modifier.accessibilityChartDescriptor(
    title: String,
    xAxisTitle: String,
    yAxisTitle: String,
    values: List<Pair<String, Double>>,
    isContinuous: Boolean = false,
): Modifier = element(
    CompletionIds.CHART_DESCRIPTOR, title, xAxisTitle, yAxisTitle, values.map { it.first }, values.map { it.second }, isContinuous,
)

/** SwiftUI `dialogIcon(_:)`: the icon of the enclosing alert or confirmation dialog. */
@HiddenFromObjC
public fun Modifier.dialogIcon(symbol: SFSymbol): Modifier = element(CompletionIds.DIALOG_ICON, symbol.name)

// ---- custom alignments and layout values -------------------------------------------------------

/**
 * A custom horizontal alignment guide (SwiftUI `HorizontalAlignment(AlignmentID)`), defaulting to the leading edge;
 * [index] (0…7) tells apart up to eight custom guides. Move it per view with `Modifier.alignmentGuide`.
 */
@HiddenFromObjC
public fun customHorizontalAlignment(index: Int): HorizontalAlignment =
    HorizontalAlignment(CompletionIds.CUSTOM_HORIZONTAL_ALIGNMENT, listOf(index.coerceIn(0, 7)))

/** A custom vertical alignment guide (SwiftUI `VerticalAlignment(AlignmentID)`), defaulting to the top edge. */
@HiddenFromObjC
public fun customVerticalAlignment(index: Int): VerticalAlignment =
    VerticalAlignment(CompletionIds.CUSTOM_VERTICAL_ALIGNMENT, listOf(index.coerceIn(0, 7)))

/** SwiftUI `layoutValue(key:value:)`: a value read by a Kotlin [LayoutPolicy] in `LayoutSize.values[key]`. */
@HiddenFromObjC
public fun Modifier.layoutValue(key: String, value: Any?): Modifier = element(CompletionIds.LAYOUT_VALUE, key, value)

// ---- pasteboard and Services (macOS) -----------------------------------------------------------

/** SwiftUI `copyable(_:)` (macOS): Edit › Copy copies [items]. */
@HiddenFromObjC
public fun Modifier.copyable(items: List<String>): Modifier = element(CompletionIds.COPYABLE, items)

/** SwiftUI `cuttable(for:action:)` (macOS): Edit › Cut copies what [action] returns (remove the items there). */
@HiddenFromObjC
public fun Modifier.cuttable(action: () -> List<String>): Modifier = element(CompletionIds.CUTTABLE, textItems(action))

/** SwiftUI `pasteDestination(for:action:)` (macOS): Edit › Paste delivers the pasteboard's text items. */
@HiddenFromObjC
public fun Modifier.pasteDestination(action: (List<String>) -> Unit): Modifier =
    element(CompletionIds.PASTE_DESTINATION, SValueCallback { action((it as List<*>).map { s -> s as String }) })

/** SwiftUI `onCopyCommand(perform:)` (macOS): the text items Edit › Copy puts on the pasteboard. */
@HiddenFromObjC
public fun Modifier.onCopyCommand(action: () -> List<String>): Modifier = element(CompletionIds.COPY_COMMAND, textItems(action))

/** SwiftUI `onCutCommand(perform:)` (macOS): the text items Edit › Cut puts on the pasteboard. */
@HiddenFromObjC
public fun Modifier.onCutCommand(action: () -> List<String>): Modifier = element(CompletionIds.CUT_COMMAND, textItems(action))

/** SwiftUI `onPasteCommand(of:perform:)` (macOS): the pasted text items of [contentTypes]. */
@HiddenFromObjC
public fun Modifier.onPasteCommand(contentTypes: List<String> = listOf("public.plain-text"), action: (List<String>) -> Unit): Modifier =
    element(CompletionIds.PASTE_COMMAND, contentTypes, SValueCallback { action((it as List<*>).map { s -> s as String }) })

/** SwiftUI `exportableToServices(_:)` (macOS): the text items offered to the Services menu. */
@HiddenFromObjC
public fun Modifier.exportableToServices(items: () -> List<String>): Modifier =
    element(CompletionIds.EXPORT_TO_SERVICES, textItems(items))

/** SwiftUI `importableFromServices(for:action:)` (macOS): text a service returns; [action] returns whether it took it. */
@HiddenFromObjC
public fun Modifier.importableFromServices(action: (List<String>) -> Boolean): Modifier =
    element(CompletionIds.IMPORT_FROM_SERVICES, object : DropHandler {
        override fun onDrop(items: List<String>, x: Double, y: Double): Boolean = action(items)
    })

// ---- keyboard modifiers and Apple Pencil ---------------------------------------------------------

/**
 * SwiftUI `onModifierKeysChanged(mask:initial:_:)` (macOS 15): [action] receives the old and new modifier keys as
 * `EventModifiers` raw values (shift 1 shl 17, control 1 shl 18, option 1 shl 19, command 1 shl 20).
 */
@HiddenFromObjC
public fun Modifier.onModifierKeysChanged(initial: Boolean = true, action: (old: Int, new: Int) -> Unit): Modifier =
    element(CompletionIds.MODIFIER_KEYS, initial, SValueCallback { v ->
        val l = (v as List<*>).map { (it as Number).toInt() }
        action(l[0], l[1])
    })

/** SwiftUI `onPencilDoubleTap(perform:)` (iOS 17.5): [action] receives the hover location, if the pencil hovers. */
@HiddenFromObjC
public fun Modifier.onPencilDoubleTap(action: (hoverLocation: CGPoint?) -> Unit): Modifier =
    element(CompletionIds.PENCIL_DOUBLE_TAP, SValueCallback { v -> action((v as? SExpr)?.let { cgValue(it) as CGPoint }) })

// ---- iOS 26 / 27 additions -----------------------------------------------------------------------

/** Line spacing for [lineHeight] (SwiftUI `AttributedString.LineHeight`). */
@HiddenFromObjC
public sealed class LineHeight(internal val kind: Int, internal val value: Double) {
    public object Variable : LineHeight(0, 0.0)
    public object Normal : LineHeight(1, 0.0)
    public object Tight : LineHeight(2, 0.0)
    public object Loose : LineHeight(3, 0.0)
    public class Multiple(factor: Double) : LineHeight(4, factor)
    public class Leading(increase: Double) : LineHeight(5, increase)
    public class Exact(points: Double) : LineHeight(6, points)
}

/** SwiftUI `lineHeight(_:)` (iOS 26, macOS 26): line spacing of the text inside; null resets it. */
@HiddenFromObjC
public fun Modifier.lineHeight(lineHeight: LineHeight?): Modifier =
    element(CompletionIds.LINE_HEIGHT, lineHeight?.kind, lineHeight?.value)

/**
 * SwiftUI `Slider(value:in:…ticks:)` (iOS 26, macOS 26, watchOS 26; no ticks elsewhere): a slider with tick marks at
 * [ticks]; [neutralValue] is where the track fill starts.
 */
@HiddenFromObjC
@Composable
public fun Slider(
    title: String,
    value: Double,
    bounds: ClosedFloatingPointRange<Double>,
    ticks: List<Double>,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    neutralValue: Double? = null,
) {
    SwiftView(
        CompletionIds.TICK_SLIDER,
        listOf(title, value, SValueCallback { onValueChange((it as Number).toDouble()) }, listOf(bounds.start, bounds.endInclusive), ticks, neutralValue),
        modifier,
    )
}

/** A custom SwiftUI `ArrangementViewStyle` (iOS 27.1): [body] places [StyleLabel] (primary) and [StyleContent] (secondary). */
@HiddenFromObjC
public fun Modifier.arrangementViewStyle(body: @Composable () -> Unit): Modifier =
    element(CompletionIds.ARRANGEMENT_STYLE, slotContent(body))

/** SwiftUI `onHingeChange(isEnabled:_:)` (iOS 27.1): the device hinge angle in degrees, old and new (null without a hinge). */
@HiddenFromObjC
public fun Modifier.onHingeChange(isEnabled: Boolean = true, action: (old: Double?, new: Double?) -> Unit): Modifier =
    element(CompletionIds.HINGE_CHANGE, isEnabled, SValueCallback { v ->
        val l = v as List<*>
        action((l[0] as? Number)?.toDouble(), (l[1] as? Number)?.toDouble())
    })

/**
 * SwiftUI `reorderContainer(for:isEnabled:move:)` (iOS 27, macOS 27): children marked `Modifier.draggable(containerItemID)`
 * can be dragged to reorder; [move] receives the moved ids and the id they go before (null = at the end).
 */
@HiddenFromObjC
public fun Modifier.reorderContainer(isEnabled: Boolean = true, move: (sources: List<Any>, before: Any?) -> Unit): Modifier =
    element(CompletionIds.REORDER_CONTAINER, isEnabled, SValueCallback { v ->
        val l = v as List<*>
        move((l[0] as List<*>).filterNotNull(), l[1])
    })

/** SwiftUI `dragContainer(for:in:_:)` (iOS 27, macOS 26): drags the items marked `draggable(containerItemID)` as text. */
@HiddenFromObjC
public fun Modifier.dragContainer(payload: (id: Any) -> String): Modifier =
    element(CompletionIds.DRAG_CONTAINER, object : TextForIdProvider {
        override fun text(id: Any): String = payload(id)
    })

/** SwiftUI `dragContainerSelection(_:containerNamespace:)` (iOS 27, macOS 26): the ids dragged together. */
@HiddenFromObjC
public fun Modifier.dragContainerSelection(ids: List<Any>): Modifier = element(CompletionIds.DRAG_CONTAINER_SELECTION, ids)

/** Phase of a drag or drop session. */
@HiddenFromObjC
public enum class DragDropPhase { Initial, Active, Ending, Ended, DataTransferCompleted, Entering, Exiting }

/** SwiftUI `onDragSessionUpdated(_:)` (iOS 27, macOS 26): phase, location and dragged item index of the drag. */
@HiddenFromObjC
public fun Modifier.onDragSessionUpdated(action: (phase: DragDropPhase, location: CGPoint, draggedItemIndex: Int) -> Unit): Modifier =
    element(CompletionIds.DRAG_SESSION, SValueCallback { v ->
        val d = (v as List<*>).map { (it as Number).toDouble() }
        action(DragDropPhase.entries[d[0].toInt()], CGPoint(d[1], d[2]), d[3].toInt())
    })

/** SwiftUI `onDropSessionUpdated(_:)` (iOS 27, macOS 26): phase, location and item count of a drop over the view. */
@HiddenFromObjC
public fun Modifier.onDropSessionUpdated(action: (phase: DragDropPhase, location: CGPoint, itemsCount: Int) -> Unit): Modifier =
    element(CompletionIds.DROP_SESSION, SValueCallback { v ->
        val d = (v as List<*>).map { (it as Number).toDouble() }
        action(DragDropPhase.entries[d[0].toInt()], CGPoint(d[1], d[2]), d[3].toInt())
    })

/** SwiftUI `dropConfiguration(_:)` (iOS 27, macOS 26): the operation offered for a drop (null = copy). */
@HiddenFromObjC
public fun Modifier.dropConfiguration(operation: (location: CGPoint, itemsCount: Int) -> DropOperation?): Modifier =
    element(CompletionIds.DROP_CONFIGURATION, object : DropOperationProvider {
        override fun operation(x: Double, y: Double, itemsCount: Int): Any? = operation(CGPoint(x, y), itemsCount)
    })

// ---- remaining view-building pieces ------------------------------------------------------------

/** SwiftUI `ContentUnavailableView.search(text:)`: the standard "No results" view, mentioning [text] when given. */
@HiddenFromObjC
@Composable
public fun ContentUnavailableSearch(text: String? = null, modifier: Modifier = Modifier) {
    SwiftView(CompletionIds.SEARCH_UNAVAILABLE, listOf(text), modifier)
}

/** SwiftUI `environment(\\.editMode, $mode)` (iOS): edit mode of the Lists inside, driven by Kotlin state. */
@HiddenFromObjC
public fun Modifier.editMode(isEditing: Boolean, onEditingChange: (Boolean) -> Unit): Modifier =
    element(CompletionIds.EDIT_MODE, isEditing, SValueCallback { onEditingChange(it as Boolean) })

/**
 * SwiftUI `environment(\\.openURL, OpenURLAction { url in … })`: links opened inside (Link, markdown text) go to
 * [handler] first; return true when handled, false to let the system open the URL.
 */
@HiddenFromObjC
public fun Modifier.onOpenUrlAction(handler: (url: String) -> Boolean): Modifier =
    element(CompletionIds.OPEN_URL_ACTION, object : DropHandler {
        override fun onDrop(items: List<String>, x: Double, y: Double): Boolean = handler(items.firstOrNull() ?: "")
    })

/** SwiftUI `pointerStyle(.image(_:hotSpot:))` (macOS 15): an asset-catalog image as the mouse pointer. */
@HiddenFromObjC
public fun Modifier.pointerImage(imageName: String, hotSpot: UnitPoint = UnitPoint.center): Modifier =
    element(CompletionIds.POINTER_IMAGE, imageName, hotSpot)

/** SwiftUI `TimelineSchedule.explicit(_:)`: updates at each of [dates] (epoch seconds). */
@HiddenFromObjC
public class ExplicitTimelineSchedule(dates: List<Double>) : SExpr(CompletionIds.EXPLICIT_SCHEDULE, listOf(dates)), TimelineSchedule

/** SwiftUI `ImagePaint(image:sourceRect:scale:)`: fills with the asset-catalog image [imageName], tiled. */
@HiddenFromObjC
public class ImagePaintStyle(imageName: String, scale: Double = 1.0) :
    SExpr(CompletionIds.IMAGE_PAINT, listOf(imageName, scale)), ShapeStyle

/**
 * A SwiftUI `CustomPresentationDetent`: the sheet height for the largest detent height [height] receives (null =
 * hidden). [index] (0…3) tells apart up to four custom detents.
 */
@HiddenFromObjC
public fun customPresentationDetent(index: Int, height: (maxDetentValue: Double) -> Double?): PresentationDetent =
    PresentationDetent(CompletionIds.CUSTOM_DETENT, listOf(index.coerceIn(0, 3), object : AlignmentGuideProvider {
        override fun compute(width: Double, height: Double): Double = height(width) ?: Double.NaN
    }))
