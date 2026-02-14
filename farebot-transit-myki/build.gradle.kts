plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidLibrary {
        namespace = "com.codebutler.farebot.transit.myki"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.resources)
            implementation(libs.compose.runtime)
            api(project(":farebot-transit"))
            implementation(project(":farebot-card-desfire"))
            implementation(project(":farebot-transit-serialonly"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
