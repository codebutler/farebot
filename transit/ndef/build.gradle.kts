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
            implementation(project(":base"))
            implementation(project(":transit"))
            implementation(project(":card"))
            implementation(project(":card:ultralight"))
            implementation(project(":card:classic"))
            implementation(project(":card:felica"))
            implementation(project(":card:vicinity"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}
