rootProject.name = "compose-swiftui-sample"

pluginManagement {
    repositories {
        mavenLocal() // local builds of the library and plugin; remove once published
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

include(":shared")
