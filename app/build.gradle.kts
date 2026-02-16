plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.metro)
}

kotlin {
    androidLibrary {
        namespace = "com.codebutler.farebot.shared"
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

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "FareBotKit"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.maps.compose)
            implementation(libs.play.services.maps)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.activity.compose)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.compose.resources)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            api(project(":base"))
            api(project(":card"))
            api(project(":card:cepas"))
            api(project(":card:classic"))
            api(project(":card:desfire"))
            api(project(":card:felica"))
            api(project(":card:ultralight"))
            api(project(":card:iso7816"))
            api(project(":card:ksx6924"))
            api(project(":card:china"))
            api(project(":card:vicinity"))
            api(project(":transit:china"))
            api(project(":transit"))
            api(project(":transit:bilhete"))
            api(project(":transit:bip"))
            api(project(":transit:clipper"))
            api(project(":transit:easycard"))
            api(project(":transit:edy"))
            api(project(":transit:ezlink"))
            api(project(":transit:hsl"))
            api(project(":transit:kmt"))
            api(project(":transit:mrtj"))
            api(project(":transit:manly"))
            api(project(":transit:myki"))
            api(project(":transit:octopus"))
            api(project(":transit:opal"))
            api(project(":transit:orca"))
            api(project(":transit:ovc"))
            api(project(":transit:erg"))
            api(project(":transit:nextfare"))
            api(project(":transit:seqgo"))
            api(project(":transit:nextfareul"))
            api(project(":transit:amiibo"))
            api(project(":transit:ventra"))
            api(project(":transit:yvr-compass"))
            api(project(":transit:vicinity"))
            api(project(":transit:suica"))
            api(project(":transit:en1545"))
            api(project(":transit:calypso"))
            api(project(":transit:troika"))
            api(project(":transit:oyster"))
            api(project(":transit:charlie"))
            api(project(":transit:gautrain"))
            api(project(":transit:smartrider"))
            api(project(":transit:podorozhnik"))
            api(project(":transit:touchngo"))
            api(project(":transit:tfi-leap"))
            api(project(":transit:lax-tap"))
            api(project(":transit:ricaricami"))
            api(project(":transit:yargor"))
            api(project(":transit:chc-metrocard"))
            api(project(":transit:komuterlink"))
            api(project(":transit:magnacarta"))
            api(project(":transit:tampere"))
            api(project(":transit:msp-goto"))
            api(project(":transit:tmoney"))
            api(project(":transit:waikato"))
            api(project(":transit:bonobus"))
            api(project(":transit:cifial"))
            api(project(":transit:adelaide"))
            api(project(":transit:hafilat"))
            api(project(":transit:intercard"))
            api(project(":transit:kazan"))
            api(project(":transit:kiev"))
            api(project(":transit:metromoney"))
            api(project(":transit:metroq"))
            api(project(":transit:otago"))
            api(project(":transit:pilet"))
            api(project(":transit:selecta"))
            api(project(":transit:umarsh"))
            api(project(":transit:warsaw"))
            api(project(":transit:zolotayakorona"))
            api(project(":transit:serialonly"))
            api(project(":transit:krocap"))
            api(project(":transit:snapper"))
            api(project(":transit:ndef"))
            api(project(":transit:rkf"))
            implementation(libs.sqldelight.runtime)
            implementation(libs.kotlinx.serialization.json)
        }
    }
}

sqldelight {
    databases {
        create("FareBotDb") {
            packageName.set("com.codebutler.farebot.persist.db")
        }
    }
}
