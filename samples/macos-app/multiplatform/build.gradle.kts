plugins {
    kotlin("multiplatform")
    id("dev.nucleusframework.compose-swiftui")
}

kotlin {
    listOf(iosSimulatorArm64(), macosArm64()).forEach {
        it.binaries.framework {
            baseName = "Multi"
            isStatic = true
        }
    }
}
