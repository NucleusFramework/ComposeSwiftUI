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
        ?: "0.4.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // `-PlocalIde=/Applications/IntelliJ IDEA.app` builds against an installed IDE instead of downloading one.
        val localIde = providers.gradleProperty("localIde").orNull
        if (localIde != null) local(localIde) else intellijIdea("2026.2.1")
        bundledPlugins("org.jetbrains.kotlin", "com.intellij.gradle")
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion { sinceBuild = "262" }
    }
    buildSearchableOptions = false
}

// `./gradlew -p intellij-plugin runIde` opens the sample in a sandbox IDE with the plugin.
tasks.runIde {
    args(layout.projectDirectory.dir("../samples/kmp-app").asFile.absolutePath)
}
