plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    sourceSets {
        jvmMain.dependencies {
            implementation(project(":app"))
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>("compileKotlinJvm") {
    compilerOptions {
        freeCompilerArgs.add("-Xadd-modules=java.smartcardio")
    }
}

compose.desktop {
    application {
        mainClass = "com.codebutler.farebot.desktop.MainKt"
        jvmArgs(
            "-Dsun.security.smartcardio.t0GetResponse=false",
            "-Dsun.security.smartcardio.t1GetResponse=false",
        )
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
            )
            packageName = "FareBot"
            packageVersion = "3.1.1"
            macOS {
                bundleID = "com.codebutler.farebot.desktop"
            }
        }
    }
}
