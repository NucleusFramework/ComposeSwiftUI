plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.vanniktechMavenPublish)
}

// Release version comes from the `vX.Y.Z` tag that triggers the publish workflow.
group = "dev.nucleusframework"

val publishVersion =
    providers
        .environmentVariable("GITHUB_REF")
        .orNull
        ?.removePrefix("refs/tags/v")
        ?: "0.8.0"

val coreOnly = providers.gradleProperty("csui.coreOnly").isPresent

kotlin {
    listOf(
        iosArm64(), iosSimulatorArm64(), iosX64(),
        macosArm64(), macosX64(),
        tvosArm64(), tvosSimulatorArm64(),
        watchosArm64(), watchosDeviceArm64(), watchosSimulatorArm64(),
    ).forEach { target ->
        // Core-only framework used by generator/verify.py to typecheck the generated Swift.
        if (coreOnly) target.binaries.framework { baseName = "BenchShared" }
    }

    sourceSets {
        commonMain {
            // generated bindings + hand-written ones built on them (excluded for core-only verification builds)
            if (!coreOnly) {
                kotlin.srcDir("src/generated/kotlin")
                kotlin.srcDir("src/extras/kotlin")
            }
            dependencies {
                api(libs.compose.runtime)
                api(libs.compose.runtime.saveable)
                api(libs.coroutines.core)
                api(libs.lifecycle.viewmodel)
            }
        }
    }
}

// Source information of the library's composables: the IDE previews map views to the app's code through it (call
// sites of a composition only line up when every composable records its source). No cost unless a host collects it.
composeCompiler {
    includeSourceInformation = true
}

mavenPublishing {
    coordinates("dev.nucleusframework", "compose-swiftui", publishVersion)

    pom {
        name.set("Compose SwiftUI")
        description.set("Write real SwiftUI UIs in Kotlin with the Compose runtime, for Kotlin/Native on iOS and macOS")
        url.set("https://github.com/NucleusFramework/ComposeSwiftUI")

        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }

        developers {
            developer {
                id.set("nucleusframework")
                name.set("NucleusFramework")
                url.set("https://github.com/NucleusFramework")
            }
        }

        scm {
            url.set("https://github.com/NucleusFramework/ComposeSwiftUI")
            connection.set("scm:git:git://github.com/NucleusFramework/ComposeSwiftUI.git")
            developerConnection.set("scm:git:ssh://git@github.com/NucleusFramework/ComposeSwiftUI.git")
        }
    }

    publishToMavenCentral()
    if (project.hasProperty("signingInMemoryKey")) {
        signAllPublications()
    }
}

/**
 * Copies the Swift renderer (ComposeSwiftUI) into an app project, re-exporting your Kotlin framework:
 *   ./gradlew :runtime:installSwiftRenderer -Pdest=/path/to/App/ComposeSwiftUI -Pframework=Shared
 */
tasks.register("installSwiftRenderer") {
    group = "compose swiftui"
    description = "Copies the Swift renderer sources into -Pdest, importing the Kotlin framework -Pframework."
    val src = rootProject.layout.projectDirectory.dir("swift/ComposeSwiftUI")
    val dest = providers.gradleProperty("dest")
    val framework = providers.gradleProperty("framework").orElse("Shared")
    doLast {
        val target = file(dest.get())
        copy {
            from(src)
            into(target)
            exclude("KotlinModule.swift")
        }
        target.resolve("KotlinModule.swift").writeText("@_exported import ${framework.get()}\n")
        println("Swift renderer installed in $target (Kotlin framework: ${framework.get()})")
    }
}
