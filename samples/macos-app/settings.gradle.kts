rootProject.name = "compose-swiftui-macos-sample"

pluginManagement {
    // The Gradle plugin, from the sources of this repository.
    includeBuild("../../gradle-plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

// The library, from the sources of this repository.
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("dev.nucleusframework:compose-swiftui")).using(project(":runtime"))
    }
}

// A native macOS module (previews render on the Mac), and a module for iOS and macOS (iOS by default,
// `device = "mac"` for the Mac).
include(":shared", ":multiplatform")
