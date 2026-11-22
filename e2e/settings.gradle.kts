rootProject.name = "compose-swiftui-e2e"

pluginManagement {
    // The Gradle plugin, from the sources of this repository.
    includeBuild("../gradle-plugin")
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
includeBuild("..") {
    dependencySubstitution {
        substitute(module("dev.nucleusframework:compose-swiftui")).using(project(":runtime"))
    }
}

include(":shared")
