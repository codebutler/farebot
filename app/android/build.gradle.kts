/*
 * build.gradle.kts
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2017 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
}

dependencies {
    implementation(project(":app"))

    implementation(libs.guava)
    implementation(libs.kotlin.stdlib)
    implementation(libs.play.services.maps)
    implementation(libs.material)
    implementation(libs.appcompat)

    // Koin
    implementation(libs.koin.android)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")

    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity Compose
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
}

fun askPassword(): String =
    Runtime
        .getRuntime()
        .exec(arrayOf("security", "-q", "find-generic-password", "-w", "-g", "-l", "farebot-release"))
        .inputStream
        .bufferedReader()
        .readText()
        .trim()

gradle.taskGraph.whenReady {
    if (hasTask(":app:android:packageRelease")) {
        val password = askPassword()
        android.signingConfigs.getByName("release").storePassword = password
        android.signingConfigs.getByName("release").keyPassword = password
    }
}

android {
    compileSdk =
        libs.versions.compileSdk
            .get()
            .toInt()
    namespace = "com.codebutler.farebot"

    defaultConfig {
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.targetSdk
                .get()
                .toInt()
        versionCode = 29
        versionName = "3.1.1"
        multiDexEnabled = true
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../../debug.keystore")
        }
        create("release") {
            storeFile = file("../../release.keystore")
            keyAlias = "ericbutler"
            storePassword = ""
            keyPassword = ""
        }
    }

    buildTypes {
        debug {
        }
        release {
            isShrinkResources = false
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "../../config/proguard/proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }

    packaging {
        resources {
            excludes += listOf("META-INF/LICENSE.txt", "META-INF/NOTICE.txt")
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }
}
