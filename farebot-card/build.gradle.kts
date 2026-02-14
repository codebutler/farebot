plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "com.codebutler.farebot.card"
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
            api(project(":farebot-base"))
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

// javax.smartcardio is in the java.smartcardio JDK module (not auto-resolved).
// Add it to the Kotlin JVM compilation classpath.
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>("compileKotlinJvm") {
    compilerOptions {
        freeCompilerArgs.add("-Xadd-modules=java.smartcardio")
    }
}
