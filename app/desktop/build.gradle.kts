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
            implementation(libs.usb4java)
            implementation(libs.usb4java.native)
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
        javaHome =
            javaToolchains
                .launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(25))
                }.map { it.metadata.installationPath.asFile.absolutePath }
                .get()
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

// usb4java's bundled libusb4java.dylib links against /opt/local/lib/libusb-1.0.0.dylib
// (MacPorts path). On Homebrew systems, libusb lives in /opt/homebrew/lib/ (Apple Silicon)
// or /usr/local/lib/ (Intel). Tell dyld where to find it.
afterEvaluate {
    tasks.withType<JavaExec> {
        environment(
            "DYLD_FALLBACK_LIBRARY_PATH",
            listOf("/opt/homebrew/lib", "/usr/local/lib").joinToString(":"),
        )
    }
}
