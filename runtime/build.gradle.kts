plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.vanniktechMavenPublish)
}

// Release version comes from the `vX.Y.Z` tag that triggers the publish workflow.
val publishVersion =
    providers
        .environmentVariable("GITHUB_REF")
        .orNull
        ?.removePrefix("refs/tags/v")
        ?: "1.0.0"

kotlin {
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    sourceSets {
        commonMain {
            if (!providers.gradleProperty("csui.coreOnly").isPresent) kotlin.srcDir("src/generated/kotlin")
            dependencies {
                api(libs.compose.runtime)
                api(libs.coroutines.core)
                api(libs.lifecycle.viewmodel)
            }
        }
    }
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
