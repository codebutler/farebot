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
    implementation(project(":farebot-base"))
implementation(project(":farebot-card"))
    implementation(project(":farebot-card-cepas"))
    implementation(project(":farebot-card-classic"))
    implementation(project(":farebot-card-desfire"))
    implementation(project(":farebot-card-felica"))
    implementation(project(":farebot-card-ultralight"))
    implementation(project(":farebot-card-iso7816"))
    implementation(project(":farebot-card-ksx6924"))
    implementation(project(":farebot-card-china"))
    implementation(project(":farebot-transit"))
    implementation(project(":farebot-transit-china"))
    implementation(project(":farebot-transit-bilhete"))
    implementation(project(":farebot-transit-bip"))
    implementation(project(":farebot-transit-clipper"))
    implementation(project(":farebot-transit-easycard"))
    implementation(project(":farebot-transit-edy"))
    implementation(project(":farebot-transit-kmt"))
    implementation(project(":farebot-transit-ezlink"))
    implementation(project(":farebot-transit-hsl"))
    implementation(project(":farebot-transit-manly"))
    implementation(project(":farebot-transit-mrtj"))
    implementation(project(":farebot-transit-myki"))
    implementation(project(":farebot-transit-nextfare"))
    implementation(project(":farebot-transit-octopus"))
    implementation(project(":farebot-transit-opal"))
    implementation(project(":farebot-transit-orca"))
    implementation(project(":farebot-transit-ovc"))
    implementation(project(":farebot-transit-seqgo"))
    implementation(project(":farebot-transit-suica"))
    implementation(project(":farebot-transit-nextfareul"))
    implementation(project(":farebot-transit-ventra"))
    implementation(project(":farebot-transit-yvr-compass"))
    implementation(project(":farebot-transit-troika"))
    implementation(project(":farebot-transit-oyster"))
    implementation(project(":farebot-transit-charlie"))
    implementation(project(":farebot-transit-gautrain"))
    implementation(project(":farebot-transit-smartrider"))
    implementation(project(":farebot-transit-podorozhnik"))
    implementation(project(":farebot-transit-touchngo"))
    implementation(project(":farebot-transit-tfi-leap"))
    implementation(project(":farebot-transit-lax-tap"))
    implementation(project(":farebot-transit-ricaricami"))
    implementation(project(":farebot-transit-yargor"))
    implementation(project(":farebot-transit-chc-metrocard"))
    implementation(project(":farebot-transit-erg"))
    implementation(project(":farebot-transit-komuterlink"))
    implementation(project(":farebot-transit-magnacarta"))
    implementation(project(":farebot-transit-tampere"))
    implementation(project(":farebot-transit-bonobus"))
    implementation(project(":farebot-transit-cifial"))
    implementation(project(":farebot-transit-adelaide"))
    implementation(project(":farebot-transit-hafilat"))
    implementation(project(":farebot-transit-intercard"))
    implementation(project(":farebot-transit-kazan"))
    implementation(project(":farebot-transit-kiev"))
    implementation(project(":farebot-transit-metromoney"))
    implementation(project(":farebot-transit-metroq"))
    implementation(project(":farebot-transit-otago"))
    implementation(project(":farebot-transit-pilet"))
    implementation(project(":farebot-transit-selecta"))
    implementation(project(":farebot-transit-umarsh"))
    implementation(project(":farebot-transit-warsaw"))
    implementation(project(":farebot-transit-zolotayakorona"))
    implementation(project(":farebot-transit-serialonly"))
    implementation(project(":farebot-transit-tmoney"))
    implementation(project(":farebot-transit-krocap"))
    implementation(project(":farebot-transit-ndef"))
    implementation(project(":farebot-transit-rkf"))
    implementation(project(":farebot-transit-amiibo"))
    implementation(project(":farebot-shared"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.sqldelight.android.driver)
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
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Navigation Compose
    implementation(libs.navigation.compose)

    // Activity Compose
    implementation(libs.activity.compose)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Compose Multiplatform Resources (needed for StringResource interface)
    implementation(libs.compose.resources)
}

fun askPassword(): String {
    return Runtime.getRuntime().exec(arrayOf("security", "-q", "find-generic-password", "-w", "-g", "-l", "farebot-release"))
        .inputStream.bufferedReader().readText().trim()
}

gradle.taskGraph.whenReady {
    if (hasTask(":farebot-android:packageRelease")) {
        val password = askPassword()
        android.signingConfigs.getByName("release").storePassword = password
        android.signingConfigs.getByName("release").keyPassword = password
    }
}

android {
    compileSdk = libs.versions.compileSdk.get().toInt()
    namespace = "com.codebutler.farebot"

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 29
        versionName = "3.1.1"
        multiDexEnabled = true
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../debug.keystore")
        }
        create("release") {
            storeFile = file("../release.keystore")
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
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "../config/proguard/proguard-rules.pro")
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
