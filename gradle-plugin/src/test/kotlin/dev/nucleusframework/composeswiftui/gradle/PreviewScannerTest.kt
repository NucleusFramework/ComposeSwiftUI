package dev.nucleusframework.composeswiftui.gradle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PreviewScannerTest {
    @Test
    fun findsASimplePreview() {
        val previews = PreviewScanner.scan(
            """
            package a.b

            @Preview(name = "Hello")
            @Composable
            fun HelloPreview() {}
            """.trimIndent(),
        )
        assertEquals(listOf("a.b.HelloPreview"), previews.map { it.qualifiedName })
        assertEquals("Hello", previews.single().displayName)
    }

    @Test
    fun skipsPrivateNestedAndParameterizedFunctions() {
        val previews = PreviewScanner.scan(
            """
            @Preview @Composable private fun A() {}
            object O {
                @Preview @Composable fun B() {}
            }
            @Preview @Composable fun C(x: Int) {}
            @Preview @Composable fun D(x: () -> Unit, y: Int) {}
            @Composable fun E() {}
            """.trimIndent(),
        )
        assertTrue(previews.isEmpty(), previews.toString())
    }

    @Test
    fun readsEveryArgument() {
        val config = PreviewScanner.scan(
            """
            @Preview(
                "Dark", group = "G", device = "iPad Pro", width = 320, height = 0x10, sizeThatFits = true,
                colorScheme = PreviewColorScheme.Dark, dynamicTypeSize = PreviewDynamicTypeSize.XXXLarge,
                locale = "fr_FR", layoutDirection = PreviewLayoutDirection.RightToLeft, backgroundColor = 0xFF00FF00,
            )
            @Composable
            fun P() {}
            """.trimIndent(),
        ).single().configs.single()
        assertEquals(
            PreviewConfig(
                name = "Dark", group = "G", device = "iPad Pro", width = 320, height = 16, sizeThatFits = true,
                colorScheme = "dark", dynamicTypeSize = "xxxLarge", locale = "fr_FR", layoutDirection = "rightToLeft",
                backgroundColor = 0xFF00FF00,
            ),
            config,
        )
    }

    @Test
    fun repeatedAndMultipreviewAnnotations() {
        val previews = PreviewScanner.scanAll(
            listOf(
                "a.kt" to """
                    package a

                    @Preview(name = "Small", width = 100)
                    @Preview(name = "Big", width = 500)
                    annotation class Sizes

                    @Sizes
                    @PreviewLightDark
                    annotation class All
                """.trimIndent(),
                "b.kt" to """
                    package b

                    import a.All

                    @All
                    @Preview(name = "Plain")
                    @Composable
                    fun P() {}
                """.trimIndent(),
            ),
        )
        val p = previews.single()
        assertEquals(listOf("Small", "Big", "Light", "Dark", "Plain"), p.configs.map { it.name })
        assertEquals("P - Big", p.title(1))
        assertEquals("b.P~4", p.key(4))
        assertEquals("b.kt", p.source)
    }

    @Test
    fun previewParameterResolvesTheProvider() {
        val previews = PreviewScanner.scanAll(
            listOf(
                "p.kt" to """
                    package data
                    class UserProvider : PreviewParameterProvider<User> { override val values = sequenceOf() }
                """.trimIndent(),
                "ui.kt" to """
                    package ui

                    import data.*
                    import other.NamesProvider as Names

                    @Preview @Composable
                    fun UserPreview(@PreviewParameter(UserProvider::class) user: User) {}

                    @Preview @Composable
                    fun NamePreview(
                        @PreviewParameter(provider = Names::class, limit = 2) name: String,
                    ) {}

                    @Preview @Composable
                    fun LocalPreview(@PreviewParameter(Local::class) value: Map<String, Int>) {}
                """.trimIndent(),
            ),
        ).associateBy { it.functionName }
        assertEquals(PreviewParameterSpec("data.UserProvider", Int.MAX_VALUE), previews.getValue("UserPreview").parameter)
        assertEquals(PreviewParameterSpec("other.NamesProvider", 2), previews.getValue("NamePreview").parameter)
        assertEquals(PreviewParameterSpec("ui.Local", Int.MAX_VALUE), previews.getValue("LocalPreview").parameter)
    }

    @Test
    fun ignoresCommentedOutPreviews() {
        val previews = PreviewScanner.scan(
            """
            // @Preview @Composable fun A() {}
            /* @Preview
            @Composable
            fun B() {} */
            @Preview @Composable fun C() {} // https://example.com
            """.trimIndent(),
        )
        assertEquals(listOf("C"), previews.map { it.functionName })
        assertNull(previews.single().parameter)
    }

    @Test
    fun backtickedNamesAreCalledInBackticks() {
        val preview = PreviewScanner.scan("package a\n@Preview @Composable\nfun `Dark mode`() {}").single()
        assertEquals("a.Dark mode", preview.qualifiedName)
        val code = PreviewCodegen.kotlin(PreviewScanner.entries(listOf(preview)))
        assertTrue("public fun dark_mode(): ComposeSwiftUIHost = composeSwiftUI { a.`Dark mode`() }" in code, code)
    }

    @Test
    fun generatedKotlinLooksPreviewsUpByName() {
        val entries = PreviewScanner.entries(
            listOf(
                PreviewFunction("a", "P"),
                PreviewFunction("a", "Q", parameter = PreviewParameterSpec("a.Values", 3)),
            ),
        )
        val code = PreviewCodegen.kotlin(entries)
        assertTrue("public fun q(index: Int): ComposeSwiftUIHost =" in code, code)
        assertTrue("composeSwiftUI { a.Q(a.Values().values.take(3).elementAt(index)) }" in code, code)
        assertTrue("\"a.Q\" -> a.Values().values.take(3).count()" in code, code)
        assertTrue("\"a.P\" -> p()" in code, code)
        // A throwing provider fails its preview only.
        assertTrue("} catch (e: Throwable) {\n        countFailure = e.stackTraceToString()\n        -1" in code, code)
        assertTrue("0..<max(0, Int(ComposeSwiftUIPreviews.shared.count(id: \"a.Q\")))" in PreviewCodegen.swift(entries))
    }
}
