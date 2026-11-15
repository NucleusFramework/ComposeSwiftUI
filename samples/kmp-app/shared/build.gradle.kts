plugins {
    kotlin("multiplatform") version "2.4.20"
    // The only Compose SwiftUI line: adds the library, the Compose compiler, the framework export,
    // and keeps the Swift renderer in iosApp/ComposeSwiftUI up to date on every Xcode build.
    id("dev.nucleusframework.compose-swiftui") version "0.1.0"
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
}
