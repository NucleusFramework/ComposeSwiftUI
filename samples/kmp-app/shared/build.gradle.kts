plugins {
    kotlin("multiplatform") version "2.4.20"
    // Compose SwiftUI: adds the library, the Compose compiler and the framework export, and writes the Swift
    // renderer into iosApp/ComposeSwiftUI on every Xcode build.
    id("dev.nucleusframework.compose-swiftui")
    // Only needed for the Compose Multiplatform page of the sample.
    id("org.jetbrains.compose") version "1.12.2"
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform resources (strings.xml), used from Compose SwiftUI screens
            implementation("org.jetbrains.compose.components:components-resources:1.12.2")
        }
        iosMain.dependencies {
            implementation("androidx.navigation3:navigation3-runtime:1.3.0-alpha04")
            implementation("org.jetbrains.compose.foundation:foundation:1.12.2")
        }
    }
}
