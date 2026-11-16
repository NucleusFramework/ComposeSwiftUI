@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@file:Suppress("FunctionName")

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.native.HiddenFromObjC

// SwiftUI's rich-text TextEditor (iOS 26 / macOS 26) on a StyledString: AttributedTextSelection, the AttributedString
// editing helpers and attributedTextFormattingDefinition. Swift side: Completions.swift (RichTextEditorView).

internal object RichTextIds {
    const val EDITOR = 100_067
    const val FORMATTING_DEFINITION = 100_114
}

/**
 * SwiftUI `AttributedTextSelection`: the selected [ranges] (UTF-16 offsets; one empty range is the insertion point)
 * and the [typingStyle] that text typed at the insertion point gets.
 */
@HiddenFromObjC
public data class RichTextSelection(val ranges: List<TextRange>, val typingStyle: SpanStyle? = null) {
    public constructor(range: TextRange, typingStyle: SpanStyle? = null) : this(listOf(range), typingStyle)

    /** From the start of the first range to the end of the last one. */
    val range: TextRange get() = TextRange(ranges.first().start, ranges.last().end)

    val isInsertionPoint: Boolean get() = ranges.all { it.start == it.end }

    public companion object {
        /** An insertion point at [offset]. */
        public fun insertionPoint(offset: Int, typingStyle: SpanStyle? = null): RichTextSelection =
            RichTextSelection(TextRange(offset, offset), typingStyle)
    }
}

/**
 * SwiftUI `AttributedString.transformAttributes(in: &selection)`: restyles the selected runs, or the typing style at an
 * insertion point, and returns the new text with the updated selection.
 */
@HiddenFromObjC
public fun StyledString.transformStyle(
    selection: RichTextSelection,
    transform: (SpanStyle) -> SpanStyle,
): Pair<StyledString, RichTextSelection> {
    if (selection.isInsertionPoint) {
        val base = selection.typingStyle ?: typingStyleAt(selection.range.start)
        return this to selection.copy(typingStyle = transform(base))
    }
    var text = this
    for (range in selection.ranges) text = text.transformStyle(range, transform)
    return text to selection.copy(typingStyle = selection.typingStyle?.let(transform))
}

/**
 * SwiftUI `AttributedTextSelection.attributes(in:)`: the styles of the selected runs, or the typing style at an
 * insertion point. Use it to show a formatting control's state (bold when every style is bold).
 */
@HiddenFromObjC
public fun StyledString.styles(selection: RichTextSelection): List<SpanStyle> {
    if (selection.isInsertionPoint) return listOf(selection.typingStyle ?: typingStyleAt(selection.range.start))
    return selection.ranges.flatMap { subSequence(it).runs.map { run -> run.second } }.distinct()
}

/**
 * SwiftUI `AttributedString.replaceSelection(_:withCharacters:)`: replaces the selection with [characters] in the
 * typing style, and returns the new text with an insertion point after them.
 */
@HiddenFromObjC
public fun StyledString.replaceSelection(selection: RichTextSelection, characters: String): Pair<StyledString, RichTextSelection> =
    replaceSelection(selection, StyledString(characters, selection.typingStyle ?: typingStyleAt(selection.range.start)))

/** SwiftUI `AttributedString.replaceSelection(_:with:)`: replaces the selection with [replacement]. */
@HiddenFromObjC
public fun StyledString.replaceSelection(selection: RichTextSelection, replacement: StyledString): Pair<StyledString, RichTextSelection> {
    var text = this
    // later ranges first, so the earlier offsets stay valid; the replacement goes into the first range
    selection.ranges.drop(1).sortedByDescending { it.start }.forEach { text = text.replace(it, StyledString("")) }
    val first = selection.ranges.first()
    text = text.replace(first, replacement)
    val caret = first.start + replacement.text.length
    return text to RichTextSelection.insertionPoint(caret, selection.typingStyle)
}

/** The style typed text takes at [offset]: the style of the character before it, like the system editors. */
private fun StyledString.typingStyleAt(offset: Int): SpanStyle =
    (if (offset > 0) styleAt(offset - 1) else styleAt(0)) ?: SpanStyle()

/**
 * SwiftUI `TextEditor(text: Binding<AttributedString>, selection:)` (iOS 26, macOS 26): a rich-text editor. Formatting
 * the user applies (⌘B, ⌘I, ⌘U, the Format menu, fonts, colors, alignment) comes back in [onTextChange]; attributes
 * [SpanStyle] has no property for are kept in [SpanStyle.nativeAttributes]. Older systems show a plain-text editor.
 */
@HiddenFromObjC
@Composable
public fun TextEditor(
    text: StyledString,
    onTextChange: (StyledString) -> Unit,
    modifier: Modifier = Modifier,
    selection: RichTextSelection? = null,
    onSelectionChange: (RichTextSelection) -> Unit = {},
) {
    // Swift keeps its own AttributedString: it reloads the runs (or the selection) only when the revision changes,
    // i.e. when Kotlin passes a value that isn't the one Swift last reported.
    val sync = remember { RichTextSync() }
    val revision = remember(text) { if (text == sync.textFromSwift) sync.revision else ++sync.revision }
    val selectionRevision = remember(selection) {
        if (selection == null || selection == sync.selectionFromSwift) sync.selectionRevision else ++sync.selectionRevision
    }
    SwiftView(
        RichTextIds.EDITOR,
        listOf(
            revision, text.encodeRuns(sync::changed), selectionRevision, selection?.let(sync::encode),
            SValueCallback { onTextChange(sync.decodeText(it as List<*>)) },
            SValueCallback { onSelectionChange(sync.decodeSelection(it as List<*>)) },
        ),
        modifier,
    )
}

/** What the editor last reported, to recognize its own values and the runs Kotlin changed since. */
private class RichTextSync {
    var textFromSwift: StyledString? = null
    var selectionFromSwift: RichTextSelection? = null
    var revision = 0
    var selectionRevision = 0
    private val reported = HashMap<Int, SpanStyle>()

    fun decodeText(values: List<*>): StyledString = decodeStyledRuns(values).also {
        it.runs.forEach { (_, style) -> record(style) }
        textFromSwift = it
    }

    fun decodeSelection(values: List<*>): RichTextSelection {
        val r = values[0] as List<*>
        val ranges = (r.indices step 2).map { TextRange((r[it] as Number).toInt(), (r[it + 1] as Number).toInt()) }
        val typing = (values[1] as? List<*>)?.let { decodeStyle(it, 0) }?.also(::record)
        return RichTextSelection(ranges.ifEmpty { listOf(TextRange(0, 0)) }, typing).also { selectionFromSwift = it }
    }

    fun encode(selection: RichTextSelection): List<Any?> = listOf(
        selection.ranges.flatMap { listOf(it.start, it.end) },
        selection.typingStyle?.let { ArrayList<Any?>(STYLE_STRIDE).apply { encodeStyle(it, changed = changed(it)) } },
    )

    fun changed(style: SpanStyle): Int =
        style.nativeAttributes?.let { reported[it.token] }?.let { changedFields(it, style) } ?: ALL_STYLE_FIELDS

    private fun record(style: SpanStyle) {
        style.nativeAttributes?.let { reported[it.token] = style }
    }
}

/**
 * SwiftUI `AttributedTextFormattingDefinition`: the [constraints] a rich [TextEditor] applies to the formatting the
 * user picks. Set it with [Modifier.attributedTextFormattingDefinition].
 */
@HiddenFromObjC
public class AttributedTextFormattingDefinition(public vararg val constraints: AttributedTextValueConstraint)

/**
 * SwiftUI `AttributedTextFormatting.ValueConstraint`: the values an attribute may take; any other value becomes the
 * default. `values = setOf(null)` turns the attribute off (the editor hides its controls).
 */
@HiddenFromObjC
public class AttributedTextValueConstraint private constructor(internal val key: Int, internal val values: List<Any?>, internal val default: Any?) {
    public companion object {
        public fun font(values: Set<Font?>, default: Font? = null): AttributedTextValueConstraint = of(0, values, default)
        public fun foregroundColor(values: Set<Color?>, default: Color? = null): AttributedTextValueConstraint = of(1, values, default)
        public fun backgroundColor(values: Set<Color?>, default: Color? = null): AttributedTextValueConstraint = of(2, values, default)
        public fun underlineStyle(values: Set<Text.LineStyle?>, default: Text.LineStyle? = null): AttributedTextValueConstraint = of(3, values, default)
        public fun strikethroughStyle(values: Set<Text.LineStyle?>, default: Text.LineStyle? = null): AttributedTextValueConstraint = of(4, values, default)
        public fun kerning(values: Set<Double?>, default: Double? = null): AttributedTextValueConstraint = of(5, values, default)
        public fun tracking(values: Set<Double?>, default: Double? = null): AttributedTextValueConstraint = of(6, values, default)
        public fun baselineOffset(values: Set<Double?>, default: Double? = null): AttributedTextValueConstraint = of(7, values, default)
        public fun alignment(values: Set<AttributedTextAlignment?>, default: AttributedTextAlignment? = null): AttributedTextValueConstraint =
            AttributedTextValueConstraint(8, values.map { it?.name }, default?.name)

        /** Genmoji and other adaptive image glyphs: `false` keeps them out of the text. */
        public fun adaptiveImageGlyph(allowed: Boolean): AttributedTextValueConstraint =
            AttributedTextValueConstraint(9, listOf(allowed), null)

        private fun of(key: Int, values: Set<Any?>, default: Any?) = AttributedTextValueConstraint(key, values.toList(), default)
    }
}

/** SwiftUI `attributedTextFormattingDefinition(_:)`: constrains the formatting of the rich [TextEditor]s inside. */
@HiddenFromObjC
public fun Modifier.attributedTextFormattingDefinition(definition: AttributedTextFormattingDefinition): Modifier =
    element(RichTextIds.FORMATTING_DEFINITION, *definition.constraints.flatMap { listOf(it.key, it.values, it.default) }.toTypedArray())
