package dev.nucleusframework.composeswiftui

import kotlin.reflect.KClass

/**
 * Marks a top-level `@Composable` function as a preview. Repeat it, or put it on an annotation class (a multipreview,
 * like [PreviewLightDark]), to render several variants of the same function.
 *
 * The `dev.nucleusframework.compose-swiftui` Gradle plugin finds these functions in the sources and generates
 * a Swift `#Preview` for each one in `ComposeSwiftUIPreviews.swift`, next to the Swift renderer, so the
 * preview renders in Xcode's canvas with real SwiftUI. The same file declares `ComposeSwiftUIPreviewGallery`,
 * a SwiftUI view that lists every preview. The IDE plugin renders them next to the code.
 *
 * Detection is by source scan, so any annotation named `Preview` works too (for example
 * `org.jetbrains.compose.ui.tooling.preview.Preview`). The function must not be private, and takes no parameter
 * except one annotated with [PreviewParameter]. Arguments must be literals (or enum entries).
 *
 * @param name Title of the preview. Defaults to the function name.
 * @param group Group of the preview, to filter the previews in the IDE.
 * @param device Simulator (name or UDID) that renders this preview in the IDE. Defaults to the module's `previewDevice`.
 * @param width Width in points of the preview. Defaults to the device screen.
 * @param height Height in points of the preview. Defaults to the device screen, or to the content with [sizeThatFits].
 * @param sizeThatFits Sizes the preview to its content instead of the device screen, like Xcode's `.sizeThatFitsLayout`.
 * @param colorScheme Light or dark appearance. Defaults to the device's.
 * @param dynamicTypeSize Dynamic Type size (text scale). Defaults to the device's.
 * @param locale Locale identifier, e.g. `"fr_FR"`. Defaults to the device's.
 * @param layoutDirection Left-to-right or right-to-left layout. Defaults to the locale's.
 * @param backgroundColor Background behind the preview, as `0xAARRGGBB`. Defaults to the system background.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Repeatable
public annotation class Preview(
    val name: String = "",
    val group: String = "",
    val device: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val sizeThatFits: Boolean = false,
    val colorScheme: PreviewColorScheme = PreviewColorScheme.Unspecified,
    val dynamicTypeSize: PreviewDynamicTypeSize = PreviewDynamicTypeSize.Unspecified,
    val locale: String = "",
    val layoutDirection: PreviewLayoutDirection = PreviewLayoutDirection.Unspecified,
    val backgroundColor: Long = 0,
)

public enum class PreviewColorScheme { Unspecified, Light, Dark }

public enum class PreviewDynamicTypeSize {
    Unspecified, XSmall, Small, Medium, Large, XLarge, XXLarge, XXXLarge,
    Accessibility1, Accessibility2, Accessibility3, Accessibility4, Accessibility5,
}

public enum class PreviewLayoutDirection { Unspecified, LeftToRight, RightToLeft }

/** Light and dark variants of a preview. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Preview(name = "Light", colorScheme = PreviewColorScheme.Light)
@Preview(name = "Dark", colorScheme = PreviewColorScheme.Dark)
public annotation class PreviewLightDark

/** A preview at the smallest, the default and two accessibility Dynamic Type sizes. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Preview(name = "Extra small text", dynamicTypeSize = PreviewDynamicTypeSize.XSmall)
@Preview(name = "Default text", dynamicTypeSize = PreviewDynamicTypeSize.Large)
@Preview(name = "Accessibility 1", dynamicTypeSize = PreviewDynamicTypeSize.Accessibility1)
@Preview(name = "Accessibility 5", dynamicTypeSize = PreviewDynamicTypeSize.Accessibility5)
public annotation class PreviewDynamicTypeSizes

/** A preview at the sizes of a small iPhone, a large iPhone, an iPad in portrait and an iPad in landscape. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Preview(name = "Small phone", width = 375, height = 667)
@Preview(name = "Large phone", width = 440, height = 956)
@Preview(name = "Tablet portrait", width = 820, height = 1180)
@Preview(name = "Tablet landscape", width = 1180, height = 820)
public annotation class PreviewScreenSizes

/** A preview left-to-right and right-to-left. */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
@Preview(name = "Left to right", layoutDirection = PreviewLayoutDirection.LeftToRight)
@Preview(name = "Right to left", layoutDirection = PreviewLayoutDirection.RightToLeft)
public annotation class PreviewLayoutDirections

/** Values of a [PreviewParameter]: one preview is rendered per value. Needs a public no-argument constructor. */
public interface PreviewParameterProvider<T> {
    public val values: Sequence<T>
}

/**
 * Renders the preview once per value of [provider], passed to this parameter. At most one parameter of a preview.
 *
 * @param limit Maximum number of values rendered.
 */
@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.VALUE_PARAMETER)
public annotation class PreviewParameter(val provider: KClass<out PreviewParameterProvider<*>>, val limit: Int = Int.MAX_VALUE)
