@file:OptIn(kotlin.experimental.ExperimentalObjCRefinement::class, kotlin.io.encoding.ExperimentalEncodingApi::class)
@file:Suppress("FunctionName")

package dev.nucleusframework.composeswiftui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.native.HiddenFromObjC

// Hand-written equivalents of SwiftUI APIs built on Swift protocols that an app implements: preference keys,
// anchors, subview containers, custom animations and transitions, keyframes, text renderers, gesture composition,
// transferables and drop delegates. Swift side: Advanced.swift. View kinds 100_040…, modifier ids 100_060….

internal object AdvancedIds {
    const val ANCHOR_READER = 100_040
    const val SUBVIEWS_READER = 100_041
    const val SUBVIEW = 100_042
    const val SHARE_URL = 100_043

    const val PREFERENCE = 100_060
    const val ON_PREFERENCE_CHANGE = 100_061
    const val ANCHOR_PREFERENCE = 100_062
    const val CONTAINER_VALUE = 100_063
    const val TRANSITION = 100_064
    const val KEYFRAMES = 100_065
    const val TEXT_RENDERER = 100_066
    const val GESTURE = 100_067
    const val DRAGGABLE_URL = 100_068
    const val DROP_URLS = 100_069
    const val DROP_DELEGATE = 100_070
    const val DATA_EXPORTER = 100_071
    const val FILE_MOVER = 100_072

    /** Hand-written value id of a custom [Animation] (see HandwrittenValues in Swift). */
    const val CUSTOM_ANIMATION = 7
}

// ---- preferences -------------------------------------------------------------------------------

/**
 * A SwiftUI `PreferenceKey` declared in Kotlin: values set by [preference] in a subtree are combined with [reduce]
 * (in view order) and read by [onPreferenceChange] on an ancestor.
 */
@HiddenFromObjC
public class PreferenceKey<T>(public val name: String, public val defaultValue: T, public val reduce: (T, T) -> T)

/** SwiftUI `preference(key:value:)`: sets [key] to [value] for this subtree. */
@HiddenFromObjC
public fun <T> Modifier.preference(key: PreferenceKey<T>, value: T): Modifier =
    element(AdvancedIds.PREFERENCE, key.name, value)

/** SwiftUI `onPreferenceChange(_:perform:)`: [action] receives the combined value of [key] below this view. */
@HiddenFromObjC
@Suppress("UNCHECKED_CAST")
public fun <T> Modifier.onPreferenceChange(key: PreferenceKey<T>, action: (T) -> Unit): Modifier =
    element(AdvancedIds.ON_PREFERENCE_CHANGE, key.name, SValueCallback { values ->
        action((values as List<*>).fold(key.defaultValue) { acc, v -> key.reduce(acc, v as T) })
    })

/** Part of a view's bounds published by [anchorPreference]. */
@HiddenFromObjC
public enum class AnchorSource { Bounds, TopLeading, Top, TopTrailing, Leading, Center, Trailing, BottomLeading, Bottom, BottomTrailing }

/** SwiftUI `anchorPreference(key:value:transform:)`: publishes this view's bounds (or one of its points) under [key]. */
@HiddenFromObjC
public fun Modifier.anchorPreference(key: String, source: AnchorSource = AnchorSource.Bounds): Modifier =
    element(AdvancedIds.ANCHOR_PREFERENCE, key, source.ordinal)

/**
 * SwiftUI `overlayPreferenceValue` / `backgroundPreferenceValue` with anchors resolved through a `GeometryReader`:
 * [overlay] receives the frames published under [key] by [anchorPreference] inside [content], in this view's
 * coordinate space (points are zero-size rects).
 */
@HiddenFromObjC
@Composable
public fun AnchorPreferenceReader(
    key: String,
    modifier: Modifier = Modifier,
    background: Boolean = false,
    overlay: @Composable (frames: List<CGRect>) -> Unit,
    content: @Composable () -> Unit,
) {
    var frames by remember { mutableStateOf(emptyList<CGRect>()) }
    SwiftView(
        AdvancedIds.ANCHOR_READER,
        listOf(key, background, SValueCallback { v ->
            frames = (v as List<*>).map { r -> cgValue(r as SExpr) as CGRect }
        }, SSlotRef(0)),
        modifier,
        slots = listOf { overlay(frames) },
        content = content,
    )
}

// ---- subview containers ------------------------------------------------------------------------

/** A value attached to a subview with [containerValue] and read by a [SubviewsReader] layout. */
@HiddenFromObjC
public class ContainerValueKey<T>(public val name: String, public val defaultValue: T)

/** SwiftUI `containerValue(_:_:)`: attaches [value] under [key] to this view, for an enclosing [SubviewsReader]. */
@HiddenFromObjC
public fun <T> Modifier.containerValue(key: ContainerValueKey<T>, value: T): Modifier =
    element(AdvancedIds.CONTAINER_VALUE, key.name, value)

/** The subviews of a [SubviewsReader]: how many there are and the [containerValue]s each one carries. */
@HiddenFromObjC
public class Subviews internal constructor(private val values: List<Map<String, Any?>>) {
    public val count: Int get() = values.size

    @Suppress("UNCHECKED_CAST")
    public fun <T> value(index: Int, key: ContainerValueKey<T>): T =
        if (values.getOrNull(index)?.containsKey(key.name) == true) values[index][key.name] as T else key.defaultValue
}

/**
 * SwiftUI `Group(subviews:transform:)` (iOS 18, macOS 15): [content]'s resolved subviews (one per view, `ForEach`
 * rows included) are laid out by [layout], which places each one with [Subview]. Earlier systems show [content] as is.
 */
@HiddenFromObjC
@Composable
public fun SubviewsReader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
    layout: @Composable (Subviews) -> Unit,
) {
    var subviews by remember { mutableStateOf(Subviews(emptyList())) }
    SwiftView(
        AdvancedIds.SUBVIEWS_READER,
        listOf(SValueCallback { v ->
            subviews = Subviews((v as List<*>).map { entry ->
                val pairs = (entry as List<*>).chunked(2)
                pairs.associate { (k, value) -> k as String to value }
            })
        }, SSlotRef(0)),
        modifier,
        slots = listOf(content),
    ) { layout(subviews) }
}

/** Subview [index] of the enclosing [SubviewsReader]. */
@HiddenFromObjC
@Composable
public fun Subview(index: Int, modifier: Modifier = Modifier) {
    SwiftView(AdvancedIds.SUBVIEW, listOf(index), modifier)
}

// ---- custom animations, transitions, keyframes, text renderers ---------------------------------

/**
 * A SwiftUI `CustomAnimation` whose curve is a Kotlin function: [progress] maps the elapsed fraction of [duration]
 * (0…1) to the animation progress (0 = start value, 1 = end value; may overshoot).
 */
@HiddenFromObjC
public fun customAnimation(duration: Double, progress: (t: Double) -> Double): Animation =
    Animation(AdvancedIds.CUSTOM_ANIMATION, listOf(object : AnimationCurve {
        override fun progress(t: Double): Double = progress(t)
    }, duration))

/** Phase of a [transition]. */
@HiddenFromObjC
public enum class TransitionStage { WillAppear, Identity, DidDisappear }

/** A custom SwiftUI `Transition`: the effects of the inserted / removed view for each [TransitionStage]. */
@HiddenFromObjC
public fun Modifier.transition(effects: (TransitionStage) -> VisualEffects): Modifier =
    element(AdvancedIds.TRANSITION, TransitionStage.entries.map { effects(it).toList() })

/** Interpolation from the previous keyframe in a [keyframeAnimator]. */
@HiddenFromObjC
public enum class KeyframeCurve { Linear, Spring, Cubic, Move }

/** One keyframe of a [keyframeAnimator]: the effects reached after [duration] seconds. */
@HiddenFromObjC
public data class Keyframe(val effects: VisualEffects, val duration: Double, val curve: KeyframeCurve = KeyframeCurve.Linear)

/**
 * SwiftUI `keyframeAnimator(initialValue:trigger:content:keyframes:)`: animates the view's effects through
 * [keyframes] each time [trigger] changes (or in a loop when [repeating]), starting from [initial].
 */
@HiddenFromObjC
public fun Modifier.keyframeAnimator(
    keyframes: List<Keyframe>,
    initial: VisualEffects = VisualEffects(),
    trigger: Any? = null,
    repeating: Boolean = trigger == null,
): Modifier = element(
    AdvancedIds.KEYFRAMES,
    initial.toList(),
    keyframes.map { it.effects.toList() + it.duration + it.curve.ordinal.toDouble() },
    trigger,
    repeating,
)

/** Unit of text animated by [textRenderer]. */
@HiddenFromObjC
public enum class TextGranularity { Line, Run, Glyph }

/**
 * SwiftUI `textRenderer(_:)` (iOS 18, macOS 15): draws the text line by line, run by run or glyph by glyph,
 * each with the effects [effects] returns for its index (and the total count).
 */
@HiddenFromObjC
public fun Modifier.textRenderer(granularity: TextGranularity, effects: (index: Int, count: Int) -> VisualEffects): Modifier =
    element(AdvancedIds.TEXT_RENDERER, granularity.ordinal, object : EffectsProvider {
        override fun effects(phase: Double, isIdentity: Boolean, width: Double, height: Double, minX: Double, minY: Double) =
            effects(phase.toInt(), width.toInt()).toList()
    })

// ---- gestures ----------------------------------------------------------------------------------

/** A SwiftUI gesture built in Kotlin, combinable with [simultaneously], [sequenced] and [exclusively]. */
@HiddenFromObjC
public sealed class GestureSpec {
    internal abstract fun encode(): List<Any?>

    public class Tap(private val count: Int = 1, private val onEnded: (CGPoint) -> Unit) : GestureSpec() {
        override fun encode() = listOf(0, count, SValueCallback { (it as? SExpr)?.let { e -> onEnded(cgValue(e) as CGPoint) } })
    }

    public class LongPress(
        private val minimumDuration: Double = 0.5,
        private val maximumDistance: Double = 10.0,
        private val onChanged: (pressing: Boolean) -> Unit = {},
        private val onEnded: () -> Unit,
    ) : GestureSpec() {
        override fun encode() = listOf(1, minimumDuration, maximumDistance, SValueCallback { onChanged(it as Boolean) }, SAction(onEnded))
    }

    public class Drag(
        private val minimumDistance: Double = 10.0,
        private val onChanged: (DragGestureValue) -> Unit = {},
        private val onEnded: (DragGestureValue) -> Unit = {},
    ) : GestureSpec() {
        override fun encode() = listOf(2, minimumDistance, SValueCallback { onChanged(dragValue(it)) }, SValueCallback { onEnded(dragValue(it)) })
    }

    /** Pinch (iOS, macOS): the magnification factor. */
    public class Magnify(private val onChanged: (Double) -> Unit = {}, private val onEnded: (Double) -> Unit = {}) : GestureSpec() {
        override fun encode() = listOf(3, SValueCallback { onChanged(it as Double) }, SValueCallback { onEnded(it as Double) })
    }

    /** Two-finger rotation (iOS, macOS), in degrees. */
    public class Rotate(private val onChanged: (Double) -> Unit = {}, private val onEnded: (Double) -> Unit = {}) : GestureSpec() {
        override fun encode() = listOf(4, SValueCallback { onChanged(it as Double) }, SValueCallback { onEnded(it as Double) })
    }

    internal class Combined(private val kind: Int, private val first: GestureSpec, private val second: GestureSpec) : GestureSpec() {
        override fun encode() = listOf(kind, first.encode(), second.encode())
    }
}

/** SwiftUI `SimultaneousGesture`: both gestures are recognized together. */
public fun GestureSpec.simultaneously(other: GestureSpec): GestureSpec = GestureSpec.Combined(5, this, other)

/** SwiftUI `SequenceGesture`: [other] can start once this one has ended. */
public fun GestureSpec.sequenced(other: GestureSpec): GestureSpec = GestureSpec.Combined(6, this, other)

/** SwiftUI `ExclusiveGesture`: this one wins when both could be recognized. */
public fun GestureSpec.exclusively(other: GestureSpec): GestureSpec = GestureSpec.Combined(7, this, other)

/** How a [gesture] competes with the gestures of the view's subviews. */
@HiddenFromObjC
public enum class GesturePriority { Normal, High, Simultaneous }

/** SwiftUI `gesture(_:including:)`, `highPriorityGesture` and `simultaneousGesture` (not on tvOS). */
@HiddenFromObjC
public fun Modifier.gesture(gesture: GestureSpec, priority: GesturePriority = GesturePriority.Normal): Modifier =
    element(AdvancedIds.GESTURE, gesture.encode(), priority.ordinal)

// ---- transferables, drop delegates, file exporter / mover ---------------------------------------

/** SwiftUI `draggable(_:)` with a URL payload (iOS, macOS). */
@HiddenFromObjC
public fun Modifier.draggableUrl(url: String): Modifier = element(AdvancedIds.DRAGGABLE_URL, url)

/** SwiftUI `dropDestination(for: URL.self)` (iOS, macOS): [onDrop] receives the dropped URLs and the location. */
@HiddenFromObjC
public fun Modifier.dropDestinationUrls(
    isTargeted: (Boolean) -> Unit = {},
    onDrop: (urls: List<String>, location: CGPoint) -> Boolean,
): Modifier = element(
    AdvancedIds.DROP_URLS,
    object : DropHandler {
        override fun onDrop(items: List<String>, x: Double, y: Double): Boolean = onDrop(items, CGPoint(x, y))
    },
    SValueCallback { isTargeted(it as Boolean) },
)

/** A SwiftUI `DropDelegate` in Kotlin; override what you need. Text and file URLs are loaded before [performDrop]. */
@HiddenFromObjC
public abstract class DropDelegate {
    public open fun validateDrop(location: CGPoint): Boolean = true
    public open fun dropEntered(location: CGPoint) {}
    /** The operation offered while the drag moves over the view; null = SwiftUI's default. */
    public open fun dropUpdated(location: CGPoint): DropOperation? = null
    public open fun dropExited() {}
    public abstract fun performDrop(location: CGPoint, texts: List<String>, urls: List<String>): Boolean
}

/** SwiftUI `onDrop(of:delegate:)` (iOS, macOS): [contentTypes] are type identifiers (`"public.text"`, `"public.url"`…). */
@HiddenFromObjC
public fun Modifier.onDrop(contentTypes: List<String>, delegate: DropDelegate): Modifier = element(
    AdvancedIds.DROP_DELEGATE,
    contentTypes,
    object : DropDelegateHandler {
        override fun validateDrop(x: Double, y: Double) = delegate.validateDrop(CGPoint(x, y))
        override fun dropEntered(x: Double, y: Double) = delegate.dropEntered(CGPoint(x, y))
        override fun dropUpdated(x: Double, y: Double): Any? = delegate.dropUpdated(CGPoint(x, y))
        override fun dropExited() = delegate.dropExited()
        override fun performDrop(x: Double, y: Double, texts: List<String>, urls: List<String>) =
            delegate.performDrop(CGPoint(x, y), texts, urls)
    },
)

/** SwiftUI `ShareLink(item: URL)`: shares [url]; [label] replaces the default share label. */
@HiddenFromObjC
@Composable
public fun ShareLink(
    url: String,
    modifier: Modifier = Modifier,
    subject: String? = null,
    message: String? = null,
    previewTitle: String? = null,
    previewImage: String? = null,
    previewIcon: String? = null,
    label: (@Composable () -> Unit)? = null,
) {
    // previewImage / previewIcon: asset-catalog image names of SwiftUI `SharePreview`
    SwiftView(AdvancedIds.SHARE_URL, listOf(url, subject, message, label != null, previewTitle, previewImage, previewIcon, false), modifier) {
        label?.invoke()
    }
}

/** SwiftUI `ShareLink(item: String, subject:message:preview:)`: shares [text], with an optional preview. */
@HiddenFromObjC
@Composable
public fun ShareLink(
    text: String,
    previewTitle: String,
    modifier: Modifier = Modifier,
    subject: String? = null,
    message: String? = null,
    previewImage: String? = null,
    previewIcon: String? = null,
    label: (@Composable () -> Unit)? = null,
) {
    SwiftView(AdvancedIds.SHARE_URL, listOf(text, subject, message, label != null, previewTitle, previewImage, previewIcon, true), modifier) {
        label?.invoke()
    }
}

/** SwiftUI `fileExporter(isPresented:document:contentType:…)` for binary content (iOS, macOS). */
@HiddenFromObjC
public fun Modifier.fileExporter(
    isPresented: Boolean,
    onIsPresentedChange: (Boolean) -> Unit,
    data: ByteArray,
    contentType: String = "public.data",
    defaultFilename: String? = null,
    onFailure: (message: String) -> Unit = {},
    onCompletion: (url: String) -> Unit = {},
): Modifier = element(
    AdvancedIds.DATA_EXPORTER,
    isPresented,
    SValueCallback { onIsPresentedChange(it as Boolean) },
    // base64: Kotlin arrays aren't bridged to Swift
    kotlin.io.encoding.Base64.encode(data),
    contentType,
    defaultFilename,
    SValueCallback { onCompletion(it as String) },
    SValueCallback { onFailure(it as String) },
)

/** SwiftUI `fileMover(isPresented:file:onCompletion:)` (iOS, macOS): moves the file at [url] where the user picks. */
@HiddenFromObjC
public fun Modifier.fileMover(
    isPresented: Boolean,
    onIsPresentedChange: (Boolean) -> Unit,
    url: String,
    onFailure: (message: String) -> Unit = {},
    onCompletion: (url: String) -> Unit = {},
): Modifier = element(
    AdvancedIds.FILE_MOVER,
    isPresented,
    SValueCallback { onIsPresentedChange(it as Boolean) },
    url,
    SValueCallback { onCompletion(it as String) },
    SValueCallback { onFailure(it as String) },
)
