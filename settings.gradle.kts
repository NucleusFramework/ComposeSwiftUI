rootProject.name = "compose-swiftui"

pluginManagement {
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

include(":runtime")
include(":gradle-plugin")
include(":bench:shared")
