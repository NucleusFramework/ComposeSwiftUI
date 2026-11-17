package dev.nucleusframework.composeswiftui.gradle

import groovy.json.JsonOutput

/**
 * Arguments of one `@Preview` (see `dev.nucleusframework.composeswiftui.Preview`). Empty and zero values mean
 * "the device's".
 */
internal data class PreviewConfig(
    val name: String = "",
    val group: String = "",
    val device: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val sizeThatFits: Boolean = false,
    val colorScheme: String = "",
    val dynamicTypeSize: String = "",
    val locale: String = "",
    val layoutDirection: String = "",
    val backgroundColor: Long = 0,
) {
    fun toJson(): Map<String, Any> = linkedMapOf(
        "name" to name, "group" to group, "device" to device, "width" to width, "height" to height,
        "sizeThatFits" to sizeThatFits, "colorScheme" to colorScheme, "dynamicTypeSize" to dynamicTypeSize,
        "locale" to locale, "layoutDirection" to layoutDirection, "backgroundColor" to backgroundColor,
    )

    companion object {
        /** From the arguments of the annotation: `name` may be positional, enum values keep their entry name. */
        fun parse(arguments: String): PreviewConfig {
            val values = PreviewScanner.arguments(arguments, positional = listOf("name"))
            fun string(key: String) = values[key]?.let(PreviewScanner::stringValue).orEmpty()
            // Swift case name of the entry: `Dark` → `dark`, `XXLarge` → `xxLarge`.
            fun enum(key: String) = values[key]?.substringAfterLast('.')?.trim()
                ?.takeIf { it != "Unspecified" }
                ?.replace(Regex("^X+|^.")) { it.value.lowercase() }.orEmpty()
            fun int(key: String) = values[key]?.trim()?.removeSuffix("L")?.let(::parseNumber)?.toInt() ?: 0
            return PreviewConfig(
                name = string("name"),
                group = string("group"),
                device = string("device"),
                width = values["width"]?.let { int("width") } ?: int("widthDp"),
                height = values["height"]?.let { int("height") } ?: int("heightDp"),
                sizeThatFits = values["sizeThatFits"]?.trim() == "true",
                colorScheme = enum("colorScheme"),
                dynamicTypeSize = enum("dynamicTypeSize"),
                locale = string("locale"),
                layoutDirection = enum("layoutDirection"),
                backgroundColor = values["backgroundColor"]?.trim()?.removeSuffix("L")?.let(::parseNumber) ?: 0,
            )
        }

        private fun parseNumber(text: String): Long? {
            val clean = text.replace("_", "")
            return when {
                clean.startsWith("0x", ignoreCase = true) -> clean.substring(2).toULongOrNull(16)?.toLong()
                else -> clean.toLongOrNull()
            }
        }
    }
}

/** `@PreviewParameter(Provider::class, limit = n)` on the single parameter of a preview. */
internal data class PreviewParameterSpec(val providerClass: String, val limit: Int)

/**
 * A top-level `@Preview @Composable fun Name()` found in the sources.
 *
 * @property packageName package of the file, empty for the default package
 * @property functionName Kotlin name of the composable
 * @property configs one per `@Preview`, repeated or coming from multipreview annotations
 * @property parameter its `@PreviewParameter`, if any
 * @property source path of the file that declares it
 */
internal data class PreviewFunction(
    val packageName: String,
    val functionName: String,
    val configs: List<PreviewConfig> = listOf(PreviewConfig()),
    val parameter: PreviewParameterSpec? = null,
    val source: String = "",
) {
    val qualifiedName: String get() = if (packageName.isEmpty()) functionName else "$packageName.$functionName"

    /** [qualifiedName] as Kotlin code: a name like `` `Dark mode` `` in backticks. */
    val reference: String get() = qualifiedName.removeSuffix(functionName) +
        if (functionName.matches(Regex("""[A-Za-z_]\w*"""))) functionName else "`$functionName`"

    /** `name` argument of the first annotation, or [functionName]. */
    val displayName: String get() = configs.singleOrNull()?.name?.ifEmpty { null } ?: functionName

    /** Title of variant [index]: `Function - Name` when the function has several variants. */
    fun title(index: Int): String = when {
        configs.size == 1 -> displayName
        else -> "$functionName - ${configs[index].name.ifEmpty { "${index + 1}" }}"
    }

    /** Stable key of variant [index], also its image name: the qualified name, `~<index>` for a variant. */
    fun key(index: Int): String = if (configs.size == 1) qualifiedName else "$qualifiedName~$index"
}

/** A preview with the name of its accessor on the generated `ComposeSwiftUIPreviews` object. */
internal data class PreviewEntry(val function: PreviewFunction, val accessor: String)

/** What one source file declares, before multipreview annotations are resolved across files. */
internal class ScannedFile(
    val packageName: String,
    val imports: Map<String, String>,
    val wildcardImports: List<String>,
    val classes: Set<String>,
    /** Annotation classes of the file: annotations (short name to arguments) on each. */
    val annotationClasses: Map<String, List<Pair<String, String>>>,
    val functions: List<ScannedFunction>,
)

internal class ScannedFunction(
    val name: String,
    val annotations: List<Pair<String, String>>,
    val parameter: Pair<String, String>?,
    val parameterCount: Int,
)

/**
 * Finds previews with a source scan (no compiler involved). Matches functions declared at column 0 whose
 * annotations include `Composable` and `Preview` or a multipreview annotation (any package, any order, with or
 * without arguments, on one or several lines), that are not private, and that take no parameter except one
 * annotated with `@PreviewParameter`.
 */
internal object PreviewScanner {
    private val packageRegex = Regex("""^\s*package\s+([\w.`]+)""", RegexOption.MULTILINE)
    private val importRegex = Regex("""^\s*import\s+([\w`]+(?:\.[\w`]+)*)(\.\*)?(?:\s+as\s+(\w+))?""", RegexOption.MULTILINE)
    private val classRegex = Regex("""^(?:[a-z]+\s+)*(?:class|object|interface)\s+(\w+)""", RegexOption.MULTILINE)
    private val blockCommentRegex = Regex("""/\*[\s\S]*?\*/""")
    private val lineCommentRegex = Regex("""(?<!:)//[^\n]*""")

    // An annotation: `@Name`, `@a.b.Name`, optionally with arguments (two levels of nested parentheses).
    private const val ARGS = """\((?:[^()]|\((?:[^()]|\([^()]*\))*\))*\)"""
    private const val ANNOTATION = """@[\w.]+(?:\s*$ARGS)?"""
    private val functionRegex = Regex(
        """^((?:$ANNOTATION\s+)+)((?:[a-z]+\s+)*)fun\s+(\w+|`[^`\n]+`)\s*\(((?:[^()]|\((?:[^()]|\([^()]*\))*\))*)\)""",
        RegexOption.MULTILINE,
    )
    private val annotationClassRegex = Regex(
        """^((?:$ANNOTATION\s+)+)((?:[a-z]+\s+)*)annotation\s+class\s+(\w+)""",
        RegexOption.MULTILINE,
    )
    private val annotationRegex = Regex("""@([\w.]+)(?:\s*($ARGS))?""")
    private val excludedModifiers = setOf("private", "expect", "suspend", "abstract", "override")

    /** The multipreview annotations of the library, resolved without scanning it. */
    private val builtInMultipreviews: Map<String, List<PreviewConfig>> = mapOf(
        "PreviewLightDark" to listOf(
            PreviewConfig(name = "Light", colorScheme = "light"),
            PreviewConfig(name = "Dark", colorScheme = "dark"),
        ),
        "PreviewDynamicTypeSizes" to listOf(
            PreviewConfig(name = "Extra small text", dynamicTypeSize = "xSmall"),
            PreviewConfig(name = "Default text", dynamicTypeSize = "large"),
            PreviewConfig(name = "Accessibility 1", dynamicTypeSize = "accessibility1"),
            PreviewConfig(name = "Accessibility 5", dynamicTypeSize = "accessibility5"),
        ),
        "PreviewScreenSizes" to listOf(
            PreviewConfig(name = "Small phone", width = 375, height = 667),
            PreviewConfig(name = "Large phone", width = 440, height = 956),
            PreviewConfig(name = "Tablet portrait", width = 820, height = 1180),
            PreviewConfig(name = "Tablet landscape", width = 1180, height = 820),
        ),
        "PreviewLayoutDirections" to listOf(
            PreviewConfig(name = "Left to right", layoutDirection = "leftToRight"),
            PreviewConfig(name = "Right to left", layoutDirection = "rightToLeft"),
        ),
    )

    /** Previews of [sources] (path to text), with multipreview annotations resolved across all of them. */
    fun scanAll(sources: List<Pair<String, String>>): List<PreviewFunction> {
        val files = sources.map { (path, text) -> path to scanFile(text) }
        val scanned = files.map { it.second }
        val annotationClasses = scanned.flatMap { it.annotationClasses.entries }.associate { it.key to it.value }
        val classesByPackage = scanned.groupBy { it.packageName }.mapValues { (_, f) -> f.flatMap { it.classes }.toSet() }
        val resolved = HashMap<String, List<PreviewConfig>>()

        fun configsOf(annotation: String, arguments: String, visiting: Set<String>): List<PreviewConfig> = when {
            annotation == "Preview" -> listOf(PreviewConfig.parse(arguments.removeSurrounding("(", ")")))
            annotation in builtInMultipreviews -> builtInMultipreviews.getValue(annotation)
            annotation in annotationClasses && annotation !in visiting -> resolved.getOrPut(annotation) {
                annotationClasses.getValue(annotation).flatMap { (name, args) -> configsOf(name, args, visiting + annotation) }
            }
            else -> emptyList()
        }

        return files.flatMap { (path, file) ->
            file.functions.mapNotNull { function ->
                val names = function.annotations.map { it.first }
                if ("Composable" !in names) return@mapNotNull null
                val configs = function.annotations.flatMap { (name, args) -> configsOf(name, args, emptySet()) }
                if (configs.isEmpty()) return@mapNotNull null
                val parameter = function.parameter?.let { (provider, limit) ->
                    PreviewParameterSpec(resolveClass(provider, file, classesByPackage), limit.toIntOrNull() ?: Int.MAX_VALUE)
                }
                // Other parameters can't be passed by the generated code.
                if (function.parameterCount != (if (parameter != null) 1 else 0)) return@mapNotNull null
                PreviewFunction(file.packageName, function.name, configs, parameter, path)
            }
        }
    }

    /** Previews of one file, without multipreview annotations declared in other files. */
    fun scan(source: String): List<PreviewFunction> = scanAll(listOf("" to source))

    fun scanFile(source: String): ScannedFile {
        val code = source.replace(blockCommentRegex, " ").replace(lineCommentRegex, "")
        val packageName = packageRegex.find(code)?.groupValues?.get(1)?.replace("`", "").orEmpty()
        val imports = HashMap<String, String>()
        val wildcards = ArrayList<String>()
        for (match in importRegex.findAll(code)) {
            val path = match.groupValues[1].replace("`", "")
            when {
                match.groupValues[2].isNotEmpty() -> wildcards += path
                else -> imports[match.groupValues[3].ifEmpty { path.substringAfterLast('.') }] = path
            }
        }
        val annotationClasses = annotationClassRegex.findAll(code).mapNotNull { match ->
            val (annotationText, modifierText, name) = match.destructured
            if ("private" in modifierText.split(Regex("""\s+"""))) return@mapNotNull null
            name to annotations(annotationText)
        }.toMap()
        val functions = functionRegex.findAll(code).mapNotNull { match ->
            val (annotationText, modifierText, name, parameterText) = match.destructured
            val modifiers = modifierText.split(Regex("""\s+""")).filter { it.isNotEmpty() }
            if (modifiers.any { it in excludedModifiers }) return@mapNotNull null
            val parameters = splitTopLevel(parameterText).filter { it.isNotBlank() }
            val parameter = parameters.singleOrNull()?.let { text ->
                val previewParameter = annotationRegex.findAll(text)
                    .firstOrNull { it.groupValues[1].substringAfterLast('.') == "PreviewParameter" }
                    ?: return@let null
                val values = arguments(previewParameter.groupValues[2].removeSurrounding("(", ")"), listOf("provider", "limit"))
                val provider = values["provider"]?.trim()?.removeSuffix("::class")?.trim() ?: return@let null
                provider to values["limit"]?.trim().orEmpty()
            }
            ScannedFunction(name.removeSurrounding("`"), annotations(annotationText), parameter, parameters.size)
        }.toList()
        val classes = classRegex.findAll(code).map { it.groupValues[1] }.toSet()
        return ScannedFile(packageName, imports, wildcards, classes, annotationClasses, functions)
    }

    private fun annotations(text: String) = annotationRegex.findAll(text).map { annotation ->
        annotation.groupValues[1].substringAfterLast('.') to annotation.groupValues[2]
    }.toList()

    /** Qualified name of the class written [name] in [file], from its imports and the scanned classes. */
    private fun resolveClass(name: String, file: ScannedFile, classesByPackage: Map<String, Set<String>>): String {
        val first = name.substringBefore('.')
        val rest = name.substring(first.length)
        if (first.firstOrNull()?.isLowerCase() == true) return name
        file.imports[first]?.let { return it + rest }
        fun qualified(pkg: String) = if (pkg.isEmpty()) name else "$pkg.$name"
        if (first in classesByPackage[file.packageName].orEmpty()) return qualified(file.packageName)
        file.wildcardImports.firstOrNull { first in classesByPackage[it].orEmpty() }?.let { return qualified(it) }
        return qualified(file.packageName)
    }

    /** Named arguments of an annotation; unnamed ones take the names of [positional] in order. */
    fun arguments(text: String, positional: List<String>): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        splitTopLevel(text).forEachIndexed { index, argument ->
            val named = Regex("""^\s*(\w+)\s*=(?!=)\s*([\s\S]*)$""").find(argument)
            when {
                named != null -> result[named.groupValues[1]] = named.groupValues[2].trim()
                argument.isNotBlank() && index < positional.size -> result[positional[index]] = argument.trim()
            }
        }
        return result
    }

    /** Splits on commas outside parentheses, type arguments and string literals. */
    private fun splitTopLevel(text: String): List<String> {
        val parts = ArrayList<String>()
        var depth = 0
        var inString = false
        val current = StringBuilder()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inString && c == '\\' && i + 1 < text.length -> { current.append(c).append(text[i + 1]); i += 2; continue }
                c == '"' -> inString = !inString
                !inString && (c == '(' || c == '<' || c == '[') -> depth++
                !inString && (c == ')' || (c == '>' && text.getOrNull(i - 1) != '-') || c == ']') -> depth--
                !inString && depth == 0 && c == ',' -> { parts += current.toString(); current.clear(); i++; continue }
            }
            current.append(c)
            i++
        }
        if (current.isNotBlank()) parts += current.toString()
        return parts
    }

    /** The value of a string literal argument, unescaped. */
    fun stringValue(literal: String): String {
        val text = literal.trim()
        if (!text.startsWith("\"")) return ""
        return text.removePrefix("\"").removeSuffix("\"").unescapeKotlin()
    }

    private fun String.unescapeKotlin() = replace(Regex("""\\(.)""")) { match ->
        when (val c = match.groupValues[1]) {
            "n" -> "\n"
            "t" -> "\t"
            else -> c
        }
    }

    /** Sorts the previews and gives each one an accessor name, unique even when two packages share a name. */
    fun entries(previews: List<PreviewFunction>): List<PreviewEntry> {
        val sorted = previews.distinctBy { it.qualifiedName }.sortedWith(compareBy({ it.displayName }, { it.qualifiedName }))
        val clashing = sorted.groupingBy { it.functionName.identifier().lowercaseFirst() }.eachCount().filterValues { it > 1 }.keys
        val used = mutableSetOf<String>()
        return sorted.map { preview ->
            val simple = preview.functionName.identifier().lowercaseFirst()
            val base = if (simple in clashing) {
                (preview.packageName.split('.') + preview.functionName.identifier())
                    .filter { it.isNotEmpty() }
                    .joinToString("") { it.uppercaseFirst() }
                    .lowercaseFirst()
            } else {
                simple
            }
            var accessor = base
            var index = 2
            while (!used.add(accessor)) accessor = "$base${index++}"
            PreviewEntry(preview, accessor)
        }
    }

    /** An accessor name for any function name: `` `Dark mode` `` → `Dark_mode`. */
    private fun String.identifier() = replace(Regex("""\W+"""), "_").let { if (it.first().isDigit()) "_$it" else it }

    private fun String.lowercaseFirst() = replaceFirstChar { it.lowercaseChar() }
    private fun String.uppercaseFirst() = replaceFirstChar { it.uppercaseChar() }
}

internal object PreviewCodegen {
    const val OBJECT_NAME = "ComposeSwiftUIPreviews"
    const val SWIFT_FILE_NAME = "ComposeSwiftUIPreviews.swift"
    const val MANIFEST_FILE_NAME = "previews.json"
    private const val HEADER = "Generated by the dev.nucleusframework.compose-swiftui Gradle plugin. Do not edit."

    /**
     * Kotlin side, in the default package: one function per preview returning a host (Xcode's `#Preview`s), and
     * [host] and [count] by qualified name, used by the IDE's render host so its Swift code never changes.
     */
    fun kotlin(entries: List<PreviewEntry>): String = buildString {
        appendLine("// $HEADER")
        appendLine()
        appendLine("import dev.nucleusframework.composeswiftui.ComposeSwiftUIHost")
        appendLine("import dev.nucleusframework.composeswiftui.composeSwiftUI")
        appendLine()
        appendLine("/** Hosts of the `@Preview` composables, used by the generated `$SWIFT_FILE_NAME`. */")
        appendLine("public object $OBJECT_NAME {")
        for ((function, accessor) in entries) {
            val parameter = function.parameter
            if (parameter == null) {
                appendLine("    public fun $accessor(): ComposeSwiftUIHost = composeSwiftUI { ${function.reference}() }")
            } else {
                appendLine("    public fun $accessor(index: Int): ComposeSwiftUIHost =")
                appendLine("        composeSwiftUI { ${function.reference}(${parameter.values()}.elementAt(index)) }")
            }
        }
        appendLine()
        appendLine("    /** Why the last [count] returned -1: the exception of the `PreviewParameterProvider`. */")
        appendLine("    public var countFailure: String? = null")
        appendLine("        private set")
        appendLine()
        appendLine("    /**")
        appendLine("     * Number of hosts of the preview [id] (its `@PreviewParameter` values), 0 for an unknown preview, -1 when its")
        appendLine("     * provider throws.")
        appendLine("     */")
        appendLine("    public fun count(id: String): Int = try {")
        appendLine("        when (id) {")
        for ((function, _) in entries) {
            val count = function.parameter?.let { "${it.values()}.count()" } ?: "1"
            appendLine("            ${function.qualifiedName.kotlinLiteral()} -> $count")
        }
        appendLine("            else -> 0")
        appendLine("        }")
        appendLine("    } catch (e: Throwable) {")
        appendLine("        countFailure = e.stackTraceToString()")
        appendLine("        -1")
        appendLine("    }")
        appendLine()
        appendLine("    /** Host of the preview [id], for the parameter value [index]. */")
        appendLine("    public fun host(id: String, index: Int): ComposeSwiftUIHost? = when (id) {")
        for ((function, accessor) in entries) {
            val call = if (function.parameter == null) "$accessor()" else "$accessor(index)"
            appendLine("        ${function.qualifiedName.kotlinLiteral()} -> $call")
        }
        appendLine("        else -> null")
        appendLine("    }")
        appendLine("}")
    }

    private fun PreviewParameterSpec.values() =
        "$providerClass().values" + if (limit == Int.MAX_VALUE) "" else ".take($limit)"

    /** Swift side: one `#Preview` per preview variant, and a gallery view listing them all. */
    fun swift(entries: List<PreviewEntry>): String = buildString {
        appendLine("// $HEADER")
        appendLine("// Open this file in Xcode to see the Kotlin @Preview composables in the canvas.")
        appendLine()
        appendLine("import SwiftUI")
        for ((function, accessor) in entries) {
            function.configs.forEachIndexed { index, config ->
                val traits = when {
                    config.width > 0 && config.height > 0 -> ", traits: .fixedLayout(width: ${config.width}, height: ${config.height})"
                    config.sizeThatFits -> ", traits: .sizeThatFitsLayout"
                    else -> ""
                }
                appendLine()
                appendLine("#Preview(${function.title(index).swiftLiteral()}$traits) {")
                val view = if (function.parameter == null) {
                    "ComposeView { $OBJECT_NAME.shared.$accessor() }"
                } else {
                    "TabView {\n        ForEach(0..<max(0, Int($OBJECT_NAME.shared.count(id: ${function.qualifiedName.swiftLiteral()}))), id: \\.self) { index in\n" +
                        "            ComposeView { $OBJECT_NAME.shared.$accessor(index: Int32(index)) }\n        }\n    }\n    .tabViewStyle(.page)"
                }
                appendLine("    $view${config.swiftModifiers()}")
                appendLine("}")
            }
        }
        appendLine()
        appendLine("/// Lists every Kotlin `@Preview`. Show it from your app to browse the previews on a device or simulator.")
        appendLine("public struct ComposeSwiftUIPreviewGallery: View {")
        appendLine("    private struct Entry: Identifiable {")
        appendLine("        let id: Int")
        appendLine("        let name: String")
        appendLine("        let makeHost: () -> ComposeSwiftUIHost")
        appendLine("    }")
        appendLine()
        appendLine("    private var entries: [Entry] {")
        appendLine("        [")
        entries.forEachIndexed { index, (function, accessor) ->
            val call = if (function.parameter == null) "$accessor()" else "$accessor(index: 0)"
            appendLine(
                "            Entry(id: $index, name: ${function.displayName.swiftLiteral()}, " +
                    "makeHost: { $OBJECT_NAME.shared.$call }),",
            )
        }
        appendLine("        ]")
        appendLine("    }")
        append(galleryBody)
    }

    private fun PreviewConfig.swiftModifiers(): String = buildString {
        if (colorScheme.isNotEmpty()) append("\n    .preferredColorScheme(.$colorScheme)")
        if (dynamicTypeSize.isNotEmpty()) append("\n    .dynamicTypeSize(.$dynamicTypeSize)")
        if (locale.isNotEmpty()) append("\n    .environment(\\.locale, Locale(identifier: ${locale.swiftLiteral()}))")
        if (layoutDirection.isNotEmpty()) append("\n    .environment(\\.layoutDirection, .$layoutDirection)")
        if (backgroundColor != 0L) {
            val c = backgroundColor
            fun channel(shift: Int) = "%.3f".format(((c shr shift) and 0xFF) / 255.0)
            append("\n    .background(Color(.sRGB, red: ${channel(16)}, green: ${channel(8)}, blue: ${channel(0)}, opacity: ${channel(24)}))")
        }
    }

    /** Previews and their variants, read by the render host at run time (no Swift rebuild when previews change). */
    fun manifest(entries: List<PreviewEntry>): String = JsonOutput.prettyPrint(
        JsonOutput.toJson(
            mapOf(
                "previews" to entries.map { (function, _) ->
                    mapOf(
                        "id" to function.qualifiedName,
                        "functionName" to function.functionName,
                        "source" to function.source,
                        "parameterized" to (function.parameter != null),
                        "variants" to function.configs.mapIndexed { index, config ->
                            config.toJson() + mapOf("key" to function.key(index), "title" to function.title(index))
                        },
                    )
                },
            ),
        ),
    )

    private val galleryBody = """
        |
        |    @State private var selection: Entry?
        |
        |    public init() {}
        |
        |    // Each preview opens in a sheet: previews often contain their own NavigationStack.
        |    public var body: some View {
        |        let entries = entries
        |        NavigationStack {
        |            Group {
        |                if entries.isEmpty {
        |                    ContentUnavailableView(
        |                        "No previews",
        |                        systemImage: "eye.slash",
        |                        description: Text("Annotate a Kotlin @Composable function with @Preview.")
        |                    )
        |                } else {
        |                    List(entries) { entry in
        |                        Button(entry.name) { selection = entry }
        |                    }
        |                }
        |            }
        |            .navigationTitle("Previews")
        |        }
        |        .sheet(item: ${'$'}selection) { entry in
        |            ComposeView(entry.makeHost).presentationDragIndicator(.visible)
        |        }
        |    }
        |}
        |
    """.trimMargin()

    private fun String.swiftLiteral(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    private fun String.kotlinLiteral(): String =
        "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("$", "\\$") + "\""
}
