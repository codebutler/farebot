plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.metro)
}

kotlin {
    // The root build.gradle.kts automatically adds jvm() and wasmJs { browser() } to all KMP subprojects.
    // For the web module, we need wasmJs only, but having the jvm target added automatically is harmless
    // since we have no jvmMain sources. Just configure wasmJs specifics here.

    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "farebot.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":app"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            // kotlinx-datetime uses js-joda on JS/WASM targets, which needs the timezone DB
            implementation(npm("@js-joda/timezone", "2.22.0"))
        }
    }
}
