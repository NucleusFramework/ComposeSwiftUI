package dev.nucleusframework.composeswiftui.gradle

import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class AssetCatalogsTest {
    @Test
    fun findsTheAppCatalogsButNotThoseOfOutputsOrBundles() {
        val app = Files.createTempDirectory("app").toFile()
        try {
            for (path in listOf(
                "Assets.xcassets/Brand.colorset",
                "Features/Onboarding/Onboarding.xcassets",
                "ComposeSwiftUI/Generated.xcassets",
                "build/Intermediates.xcassets",
                "Pods/Lib/Lib.xcassets",
                "Widget.appex.bundle/Widget.xcassets",
                "a/b/c/d/TooDeep.xcassets",
            )) {
                File(app, path).mkdirs()
            }
            val found = appAssetCatalogs(app).map { it.relativeTo(app).path }
            assertEquals(listOf("Assets.xcassets", "Features/Onboarding/Onboarding.xcassets"), found)
        } finally {
            app.deleteRecursively()
        }
    }
}
