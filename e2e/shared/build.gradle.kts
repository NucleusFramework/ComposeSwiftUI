// End-to-end tests of the runtime and the Swift renderer: a macOS app (e2e/app) mounts each scenario of this module
// in a real SwiftUI window and checks it. Run with scripts/run_e2e.sh.
plugins {
    kotlin("multiplatform")
    id("dev.nucleusframework.compose-swiftui")
    // Compose Multiplatform resources: `stringResource` must work without providing LocalDensity by hand.
    id("org.jetbrains.compose")
}

kotlin {
    // macOS for the e2e app and XCTest; iOS for the Xcode app that consumes the generated Swift package (e2e/iosApp)
    listOf(macosArm64(), iosArm64(), iosSimulatorArm64()).forEach {
        it.binaries.framework {
            baseName = "E2EShared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.compose.components:components-resources:1.12.2")
            // Standard DI: koinViewModel() reads the official LocalViewModelStoreOwner.
            implementation("io.insert-koin:koin-compose-viewmodel:4.2.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

composeSwiftUI {
    swiftOutputDir = layout.buildDirectory.dir("renderer/ComposeSwiftUI")
    // Registered by ComposeGenerated.setUp() without a register…() call in the app
    integrations = listOf("Charts")
}
