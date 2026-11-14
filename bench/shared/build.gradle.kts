import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCompose)
}

kotlin {
    listOf(iosSimulatorArm64(), iosArm64(), macosArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "BenchShared"
            isStatic = true
            export(project(":runtime"))
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":runtime"))
        }
    }
}
