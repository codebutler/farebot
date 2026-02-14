plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidLibrary {
        namespace = "com.codebutler.farebot.transit.ndef"
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
            implementation(project(":farebot-base"))
            implementation(project(":farebot-transit"))
            implementation(project(":farebot-card"))
            implementation(project(":farebot-card-ultralight"))
            implementation(project(":farebot-card-classic"))
            implementation(project(":farebot-card-felica"))
            implementation(project(":farebot-card-vicinity"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
