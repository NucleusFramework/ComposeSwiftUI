import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinJvm)
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "dev.nucleusframework"
// Same version as the library: the release tag on CI.
version =
    providers
        .environmentVariable("GITHUB_REF")
        .orNull
        ?.removePrefix("refs/tags/v")
        ?: "0.9.1"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // `-PlocalIde=/Applications/IntelliJ IDEA.app` builds against an installed IDE instead of downloading one.
        val localIde = providers.gradleProperty("localIde").orNull
        if (localIde != null) local(localIde) else intellijIdea("2026.2.1")
        // The MCP server is optional: the agent tools are registered only when it is installed.
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.gradle", "com.intellij.mcpServer")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    // The Kotlin plugin in K2 mode, like the IDE.
    systemProperty("idea.kotlin.plugin.use.k2", "true")
    // End-to-end test against the simulator (SimulatorEndToEndTest), when given.
    providers.systemProperty("csui.e2e.module").orNull?.let { systemProperty("csui.e2e.module", it) }
    // The same against the Mac host (MacEndToEndTest), when given.
    providers.systemProperty("csui.e2e.mac.module").orNull?.let { systemProperty("csui.e2e.mac.module", it) }
    providers.systemProperty("csui.e2e.dump").orNull?.let { systemProperty("csui.e2e.dump", it) }
    // The same from Linux or Windows against a remote Mac (RemoteMacEndToEndTest), when given.
    providers.systemProperty("csui.e2e.remote").orNull?.let { systemProperty("csui.e2e.remote", it) }
    for (name in listOf("module", "xcode", "scheme", "bundle")) {
        providers.systemProperty("csui.e2e.remote.$name").orNull?.let { systemProperty("csui.e2e.remote.$name", it) }
    }
    // The preview pane through many edits against a copy of the sample (EditorTortureTest), when given.
    providers.systemProperty("csui.torture.module").orNull?.let { systemProperty("csui.torture.module", it) }
}

kotlin {
    // Preview analysis for agents (component tree, UI checks, image comparison), shared with the Gradle plugin.
    sourceSets.main { kotlin.srcDir("../gradle-plugin/src/shared/kotlin") }
    jvmToolchain(25)
    compilerOptions.jvmTarget.set(JvmTarget.JVM_25)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "262" }
    }
    buildSearchableOptions = false
}

// `./gradlew -p intellij-plugin runIde` opens the sample in a sandbox IDE with the plugin.
tasks.runIde {
    val sample = layout.projectDirectory.dir("../samples/kmp-app").asFile
    args(sample.absolutePath, sample.resolve("shared/src/iosMain/kotlin/sample/PreviewShowcase.kt").absolutePath)
    // The sample is part of this repository: no trust dialog in the sandbox.
    jvmArgs("-Didea.trust.all.projects=true")
}

// `-PandroidStudio=<its install directory>` adds `runAndroidStudio`: the plugin (built against IntelliJ IDEA) in a
// sandbox Android Studio, with the sample, to check what is specific to Android Studio.
providers.gradleProperty("androidStudio").orNull?.let { studio ->
    intellijPlatformTesting.runIde.register("runAndroidStudio") {
        localPath.set(file(studio))
        task {
            val sample = layout.projectDirectory.dir("../samples/kmp-app").asFile
            args(sample.absolutePath, sample.resolve("shared/src/iosMain/kotlin/sample/PreviewShowcase.kt").absolutePath)
            // No trust dialog, no first-run setup wizard in the sandbox.
            jvmArgs("-Didea.trust.all.projects=true", "-Ddisable.android.first.run=true")
        }
    }
}
