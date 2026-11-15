rootProject.name = "compose-swiftui-sample"

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

// The library, from the sources of this repository. In your own project, apply the plugin with a version:
// it adds dev.nucleusframework:compose-swiftui from Maven Central.
includeBuild("../..") {
    dependencySubstitution {
        substitute(module("dev.nucleusframework:compose-swiftui")).using(project(":runtime"))
    }
}

include(":shared")
