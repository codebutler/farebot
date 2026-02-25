@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("com.codebutler.farebot.tools.hardnested.MainKt")
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(libs.kotlin.stdlib)
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    workingDir = rootProject.projectDir
}
