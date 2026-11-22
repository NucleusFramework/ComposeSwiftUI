plugins {
    kotlin("multiplatform")
    id("dev.nucleusframework.compose-swiftui")
    // Compose Multiplatform resources (strings.xml), read from the host app's bundle.
    id("org.jetbrains.compose")
}

kotlin {
    macosArm64 {
        binaries.framework {
            baseName = "MacShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.components:components-resources:1.12.2")
        }
    }
}
