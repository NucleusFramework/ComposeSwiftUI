import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    `java-gradle-plugin`
    alias(libs.plugins.pluginPublish)
}

group = "dev.nucleusframework"
base.archivesName.set("compose-swiftui-gradle-plugin")

// Same version as the runtime library: the plugin adds `dev.nucleusframework:compose-swiftui:<version>`.
version =
    providers
        .environmentVariable("GITHUB_REF")
        .orNull
        ?.removePrefix("refs/tags/v")
        ?: "0.1.0"

dependencies {
    compileOnly(libs.kotlin.gradle.plugin)
    // Lets the plugin apply the Compose compiler plugin for the user.
    implementation(libs.compose.compiler.gradle.plugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

// ---- Embed the Swift renderer sources (swift/ComposeSwiftUI) as plugin resources ----------------

val rendererDir = rootProject.layout.projectDirectory.dir("swift/ComposeSwiftUI")
val rendererResources = layout.buildDirectory.dir("generated/renderer-resources")
val rendererResourcePath = "dev/nucleusframework/composeswiftui/renderer"

val embedSwiftRenderer = tasks.register("embedSwiftRenderer") {
    description = "Copies the Swift renderer sources into the plugin resources, with an index."
    inputs.dir(rendererDir)
    outputs.dir(rendererResources)
    // local values only: the configuration cache can't serialize script references
    val root = rendererDir.asFile
    val outRoot = rendererResources
    val resourcePath = rendererResourcePath
    doLast {
        val out = outRoot.get().asFile.resolve(resourcePath)
        out.deleteRecursively()
        out.mkdirs()
        val files = root.walkTopDown()
            .filter { it.isFile && it.extension == "swift" && it.name != "KotlinModule.swift" }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()
        files.forEach { rel -> root.resolve(rel).copyTo(out.resolve(rel), overwrite = true) }
        out.resolve("index.txt").writeText(files.joinToString("\n"))
    }
}

// Build-time constants (library version)
val generatedSources = layout.buildDirectory.dir("generated/build-config")
val generateBuildConfig = tasks.register("generateBuildConfig") {
    val versionValue = project.version.toString()
    val outRoot = generatedSources
    val resourcePath = rendererResourcePath
    inputs.property("version", versionValue)
    outputs.dir(generatedSources)
    doLast {
        val dir = outRoot.get().asFile.resolve("dev/nucleusframework/composeswiftui/gradle")
        dir.mkdirs()
        dir.resolve("BuildConfig.kt").writeText(
            """
            |package dev.nucleusframework.composeswiftui.gradle
            |
            |/* GENERATED, DO NOT EDIT. */
            |internal object BuildConfig {
            |    const val LIBRARY_VERSION = "$versionValue"
            |    const val RENDERER_RESOURCES = "$resourcePath"
            |}
            |""".trimMargin(),
        )
    }
}

sourceSets.main {
    kotlin.srcDir(generateBuildConfig)
    resources.srcDir(embedSwiftRenderer)
}

// ---- Gradle Plugin Portal --------------------------------------------------------------------------

gradlePlugin {
    website.set("https://github.com/NucleusFramework/ComposeSwiftUI")
    vcsUrl.set("https://github.com/NucleusFramework/ComposeSwiftUI")
    plugins {
        create("composeSwiftUI") {
            id = "dev.nucleusframework.compose-swiftui"
            implementationClass = "dev.nucleusframework.composeswiftui.gradle.ComposeSwiftUIPlugin"
            displayName = "Compose SwiftUI"
            description = "Write real SwiftUI UIs in Kotlin with the Compose runtime (iOS, macOS, tvOS, watchOS): " +
                "adds the library, configures the Compose compiler and framework exports, and keeps the Swift " +
                "renderer in sync in the Xcode project."
            tags.set(listOf("kotlin", "multiplatform", "compose", "swiftui", "ios", "macos"))
        }
    }
}

publishing {
    publications.withType<MavenPublication>().configureEach {
        if (name == "pluginMaven") artifactId = "compose-swiftui-gradle-plugin"
    }
}

tasks.register("setupPluginUploadFromEnvironment") {
    doLast {
        val key = System.getenv("GRADLE_PUBLISH_KEY")
        val secret = System.getenv("GRADLE_PUBLISH_SECRET")
        if (key == null || secret == null) {
            throw GradleException("GRADLE_PUBLISH_KEY and/or GRADLE_PUBLISH_SECRET are not defined environment variables")
        }
        System.setProperty("gradle.publish.key", key)
        System.setProperty("gradle.publish.secret", secret)
    }
}
