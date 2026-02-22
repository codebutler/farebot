plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.metro)
}

kotlin {
    jvmToolchain(25)

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":app"))
            implementation(project(":keymanager"))
            implementation(project(":app-keymanager"))
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
            modules("java.sql", "java.smartcardio")
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

// --- libusb bundling for usb4java ---
//
// usb4java's bundled libusb4java.dylib dynamically links against
// /opt/local/lib/libusb-1.0.0.dylib (MacPorts path). Rather than requiring
// users to install libusb separately, we bundle it in the app.
//
// Strategy: At build time, copy libusb from Homebrew and patch its install
// name to match what usb4java expects. At app startup, we preload the bundled
// libusb via System.load() — dyld then reuses the already-loaded image when
// processing libusb4java's dependency, since the install names match.

val bundleLibusb by tasks.registering {
    val outputDir = layout.buildDirectory.dir("bundled-native")
    outputs.dir(outputDir)

    val candidates =
        listOf(
            "/opt/homebrew/lib/libusb-1.0.0.dylib", // Apple Silicon Homebrew
            "/usr/local/lib/libusb-1.0.0.dylib", // Intel Homebrew
            "/opt/local/lib/libusb-1.0.0.dylib", // MacPorts
        )

    doLast {
        val source = candidates.map(::File).firstOrNull { it.exists() }
        if (source == null) {
            logger.warn("libusb not found — USB NFC readers won't work in packaged app")
            return@doLast
        }
        val destDir = outputDir.get().asFile.resolve("native")
        destDir.mkdirs()
        val dest = destDir.resolve("libusb-1.0.0.dylib")
        source.copyTo(dest, overwrite = true)
        // Patch install name to match what usb4java's libusb4java.dylib expects
        ProcessBuilder(
            "install_name_tool",
            "-id",
            "/opt/local/lib/libusb-1.0.0.dylib",
            dest.absolutePath,
        ).inheritIO().start().waitFor()
        logger.lifecycle("Bundled libusb from ${source.absolutePath}")
    }
}

kotlin.sourceSets.jvmMain {
    resources.srcDir(bundleLibusb.map { it.outputs.files.singleFile })
}
