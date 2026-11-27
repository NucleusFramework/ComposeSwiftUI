package dev.nucleusframework.composeswiftui.idea.mcp

import dev.nucleusframework.composeswiftui.idea.PreviewAgent

/**
 * The agent tools, shared by both MCP front ends: the IDE's MCP server (`ComposeSwiftUIMcpToolset`, whose annotations
 * read the constants below) and the plugin's own server (`StandaloneMcpServer`, for IDEs without it, like Android
 * Studio). Never references the MCP Server plugin's classes, which may be absent.
 */
internal object PreviewTools {
    const val LIST = """
        |Lists the Compose SwiftUI @Preview functions of a Kotlin file (or of the whole project) with their variants:
        |the index of each variant (multipreview annotations, repeated @Preview, @PreviewParameter values) is the
        |previewIndex of the other tools. Compose SwiftUI previews render real SwiftUI on the iOS simulator, or on the Mac
        |(`device = "mac"`, or a macOS-only module), locally on macOS or via the configured Remote Mac from Linux/Windows.
        """
    const val LIST_FILE = "Kotlin file, absolute or relative to the project root. Empty: every module that rendered previews once"

    const val RENDER = """
        |Renders a Compose SwiftUI preview with real SwiftUI on the iOS simulator or the Mac (locally on macOS, or via
        |the configured Remote Mac over SSH from Linux or Windows) and describes it: the PNG path (open it to see the
        |preview), the component tree (every view with its source line, frame in points and text) and UI checks (touch
        |targets, text contrast, small text, overlaps, clipping). On failure: compiler errors with their lines, or the
        |Kotlin exception of the preview. Saves open files and builds what changed first. Render once before a visual
        |change to know the "before" state, then after each edit to verify it. composableName may also be a @Composable
        |without parameters that is not a preview.
        """
    const val RENDER_FILE = "Kotlin file declaring the preview, absolute or relative to the project root"
    const val RENDER_NAME = "Simple name of the preview function. Empty: every preview of the file"
    const val RENDER_INDEX = "0-based variant to render (see list_swiftui_previews). -1: all of them"

    const val RENDER_CODE = """
        |Renders throwaway Compose SwiftUI code without editing the project, like an in-memory preview: prototype a UI,
        |try several variants (light and dark with @Preview(colorScheme = PreviewColorScheme.Dark), Dynamic Type sizes,
        |iPhone and iPad with width/height, the Mac with device = "mac", right-to-left) or call a composable of the
        |project with sample values. The code is a whole Kotlin file without a package line; it sees the declarations
        |of the context file's package and imports, plus androidx.compose.runtime and dev.nucleusframework.composeswiftui.
        |Its @Preview functions are rendered (top-level, not private), or previews calling composableNames when it has none.
        """
    const val RENDER_CODE_CODE = "Kotlin code with @Preview @Composable functions (no package line)"
    const val RENDER_CODE_CONTEXT = "A Kotlin file of the module the code belongs to: gives the dependencies, package and imports"
    const val RENDER_CODE_NAMES = "Composables without parameters to preview when the code has no @Preview"

    const val CHECK = """
        |UI checks of a rendered Compose SwiftUI preview, like Android Studio's UI Check: accessibility (touch targets
        |under 44 pt), color contrast of texts (WCAG AA, estimated from the image), typography (text under 11 pt) and
        |layout (texts overlapping, views clipped by the preview's width). Renders the preview first. Use variants
        |(Dynamic Type sizes, screen sizes, right-to-left) to check adaptive layouts.
        """
    const val CHECK_FILE = "Kotlin file declaring the preview"
    const val CHECK_INDEX = "0-based variant. -1: all of them"

    const val COMPARE = """
        |Renders a Compose SwiftUI preview and compares it with an expected image (a design mock, a screenshot of the
        |app, an earlier render): mismatch %, PSNR, the regions that differ with the view and source line under each,
        |and a side-by-side image (expected, actual, differences in red) to open. Iterate on the code until it matches.
        |Give the preview the size of the expected image (@Preview width and height, in points) for a fair comparison.
        """
    const val COMPARE_NAME = "Simple name of the preview function"
    const val COMPARE_IMAGE = "PNG or JPEG of the expected UI, absolute or relative to the project root"
    const val COMPARE_INDEX = "0-based variant to compare"

    const val COMPARE_IMAGES = """
        |Compares two images pixel by pixel: JSON with the size, mismatch %, MSE, RMSE, PSNR and the regions that
        |differ. With diffOutputPath, writes expected, actual and differences side by side there.
        """
    const val COMPARE_IMAGES_1 = "Expected image"
    const val COMPARE_IMAGES_2 = "Actual image (scaled to the expected one's size when they differ)"
    const val COMPARE_IMAGES_DIFF = "PNG to write the comparison to. Empty: none"

    const val PARITY = """
        |Compares two folders of screenshots, paired by file name (e.g. build/composeSwiftUI/previews copied before a
        |change, and the same folder after it), and writes a Markdown report with a diff image per pair that differs.
        |Returns the counts and the report.
        """
    const val PARITY_BEFORE = "Folder of the expected PNGs"
    const val PARITY_AFTER = "Folder of the new PNGs"
    const val PARITY_REPORT = "Markdown report to write. Empty: build/composeSwiftUI/parity/report.md"
    const val PARITY_THRESHOLD = "Largest share of differing pixels, in percent, for a pair to match"

    const val APPLE_GRADLE = """
        |Compiles, links, or tests Kotlin Multiplatform / Compose SwiftUI code for iOS and macOS by running Gradle tasks
        |on the Mac (locally on macOS, or on the configured Remote Mac over SSH from Linux or Windows — sources are synced
        |automatically and compiler errors come back with local file paths). Use this tool whenever you need to compile or
        |test for iOS or macOS (for example :shared:compileKotlinIosSimulatorArm64, :shared:compileKotlinMacosArm64,
        |:shared:linkDebugFrameworkIosSimulatorArm64, :shared:iosSimulatorArm64Test, :shared:macosArm64Test). Saves open
        |IDE files first.
        """
    const val APPLE_GRADLE_TASKS = "Gradle task(s) to run for iOS or macOS (e.g. [\":shared:compileKotlinIosSimulatorArm64\"])"
    const val APPLE_GRADLE_ARGS = "Optional extra Gradle CLI arguments (e.g. [\"--tests\", \"com.example.MyTest\"])"
    const val APPLE_GRADLE_ROOT = "Directory containing gradlew / settings.gradle[.kts], absolute or relative to the project root. Empty: the project root"

    const val XCODE_BUILD = """
        |Compiles or tests an iOS or macOS Xcode project (.xcodeproj or .xcworkspace) with xcodebuild on the Mac (locally
        |on macOS, or on the configured Remote Mac over SSH from Linux or Windows — sources are synced automatically and
        |Swift/Kotlin compiler errors come back with local file paths). Supports iOS simulator (platform = "ios"), macOS
        |(platform = "macos"), and generic iOS device (platform = "ios-device"), and actions "build", "build-for-testing",
        |or "test". Saves open IDE files first.
        """
    const val XCODE_PROJECT = "Path to .xcodeproj or .xcworkspace, absolute or relative to the project root. Empty: auto-detects the project's Xcode project/workspace"
    const val XCODE_SCHEME = "Xcode scheme to build. Empty: defaults to the project or workspace name"
    const val XCODE_PLATFORM = "Target platform: \"ios\" (iOS simulator, default), \"macos\" (Mac), or \"ios-device\" (generic iOS device). Ignored when destination is set"
    const val XCODE_CONFIG = "Build configuration, e.g. \"Debug\" (default) or \"Release\""
    const val XCODE_ACTION = "xcodebuild action: \"build\" (default), \"build-for-testing\", or \"test\""
    const val XCODE_DESTINATION = "Explicit xcodebuild -destination value (e.g. \"platform=iOS Simulator,name=iPhone 17 Pro\" or \"platform=macOS\"). Empty: chosen from platform"

    /** A parameter of a tool: JSON Schema [type], and its [default] (null: required). */
    class Param(val name: String, val type: String, val description: String, val default: Any? = null, val isPath: Boolean = false)

    class Tool(val name: String, description: String, val params: List<Param>, val run: suspend PreviewAgent.(Args) -> String) {
        val description = description.trimMargin().trim()
    }

    /** The arguments of a call, defaults applied. */
    class Args(private val values: Map<String, Any?>) {
        fun string(name: String) = values[name]?.toString().orEmpty()
        fun int(name: String) = (values[name] as? Number)?.toInt() ?: values[name]?.toString()?.toDoubleOrNull()?.toInt() ?: 0
        fun double(name: String) = (values[name] as? Number)?.toDouble() ?: values[name]?.toString()?.toDoubleOrNull() ?: 0.0
        fun strings(name: String) = (values[name] as? List<*>)?.map { it.toString() }.orEmpty()
    }

    val all = listOf(
        Tool("list_swiftui_previews", LIST, listOf(Param("filePath", "string", LIST_FILE, "", isPath = true))) {
            list(it.string("filePath"))
        },
        Tool(
            "render_swiftui_preview", RENDER,
            listOf(
                Param("filePath", "string", RENDER_FILE, isPath = true),
                Param("composableName", "string", RENDER_NAME, ""),
                Param("previewIndex", "integer", RENDER_INDEX, -1),
            ),
        ) { render(it.string("filePath"), it.string("composableName"), it.int("previewIndex")) },
        Tool(
            "render_swiftui_preview_code", RENDER_CODE,
            listOf(
                Param("code", "string", RENDER_CODE_CODE),
                Param("contextFilePath", "string", RENDER_CODE_CONTEXT, isPath = true),
                Param("composableNames", "array", RENDER_CODE_NAMES, emptyList<String>()),
            ),
        ) { renderCode(it.string("code"), it.strings("composableNames"), it.string("contextFilePath")) },
        Tool(
            "check_swiftui_preview", CHECK,
            listOf(
                Param("filePath", "string", CHECK_FILE, isPath = true),
                Param("composableName", "string", RENDER_NAME, ""),
                Param("previewIndex", "integer", CHECK_INDEX, -1),
            ),
        ) { render(it.string("filePath"), it.string("composableName"), it.int("previewIndex"), includeTree = false) },
        Tool(
            "compare_swiftui_preview_to_image", COMPARE,
            listOf(
                Param("filePath", "string", CHECK_FILE, isPath = true),
                Param("composableName", "string", COMPARE_NAME),
                Param("expectedImagePath", "string", COMPARE_IMAGE, isPath = true),
                Param("previewIndex", "integer", COMPARE_INDEX, 0),
            ),
        ) { render(it.string("filePath"), it.string("composableName"), it.int("previewIndex"), it.string("expectedImagePath")) },
        Tool(
            "compare_images", COMPARE_IMAGES,
            listOf(
                Param("image1Path", "string", COMPARE_IMAGES_1, isPath = true),
                Param("image2Path", "string", COMPARE_IMAGES_2, isPath = true),
                Param("diffOutputPath", "string", COMPARE_IMAGES_DIFF, "", isPath = true),
            ),
        ) { compareImages(it.string("image1Path"), it.string("image2Path"), it.string("diffOutputPath")) },
        Tool(
            "verify_batch_visual_parity", PARITY,
            listOf(
                Param("beforeDirectoryPath", "string", PARITY_BEFORE, isPath = true),
                Param("afterDirectoryPath", "string", PARITY_AFTER, isPath = true),
                Param("reportOutputPath", "string", PARITY_REPORT, "", isPath = true),
                Param("thresholdPercent", "number", PARITY_THRESHOLD, 0.5),
            ),
        ) {
            verifyParity(it.string("beforeDirectoryPath"), it.string("afterDirectoryPath"), it.string("reportOutputPath"), it.double("thresholdPercent"))
        },
        Tool(
            "run_apple_gradle", APPLE_GRADLE,
            listOf(
                Param("tasks", "array", APPLE_GRADLE_TASKS),
                Param("arguments", "array", APPLE_GRADLE_ARGS, emptyList<String>()),
                Param("rootProjectPath", "string", APPLE_GRADLE_ROOT, "", isPath = true),
            ),
        ) { runAppleGradle(it.strings("tasks"), it.strings("arguments"), it.string("rootProjectPath")) },
        Tool(
            "build_xcode_project", XCODE_BUILD,
            listOf(
                Param("xcodeProject", "string", XCODE_PROJECT, "", isPath = true),
                Param("scheme", "string", XCODE_SCHEME, ""),
                Param("platform", "string", XCODE_PLATFORM, "ios"),
                Param("configuration", "string", XCODE_CONFIG, "Debug"),
                Param("action", "string", XCODE_ACTION, "build"),
                Param("destination", "string", XCODE_DESTINATION, ""),
            ),
        ) {
            buildXcodeProject(
                it.string("xcodeProject"),
                it.string("scheme"),
                it.string("platform"),
                it.string("configuration"),
                it.string("action"),
                it.string("destination"),
            )
        },
    )
}
