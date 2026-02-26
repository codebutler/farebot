plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvmToolchain(25)

    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":app"))
            implementation(project(":keymanager"))
            implementation(project(":app-keymanager"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.usb4java)
            implementation(libs.usb4java.native)
        }
    }
}

tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>("compileKotlinJvm") {
    compilerOptions {
        freeCompilerArgs.add("-Xadd-modules=java.smartcardio,jdk.incubator.vector")
    }
}

// --- libusb bundling for usb4java ---
//
// usb4java's bundled libusb4java.dylib dynamically links against
// /opt/local/lib/libusb-1.0.0.dylib (MacPorts path). Rather than requiring
// users to install libusb separately, we bundle it in the app.
//
// Strategy: At build time, copy libusb from Homebrew, patch its install
// name to match what usb4java expects, and re-sign it. At app startup,
// we preload the bundled libusb via System.load() — dyld then reuses the
// already-loaded image when processing libusb4java's dependency.

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
            logger.warn("libusb not found — USB NFC readers won't work")
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
        // Re-sign after patching to satisfy macOS code signature validation
        ProcessBuilder(
            "codesign",
            "--force",
            "--sign",
            "-",
            dest.absolutePath,
        ).inheritIO().start().waitFor()
        logger.lifecycle("Bundled libusb from ${source.absolutePath}")
    }
}

kotlin.sourceSets.jvmMain {
    resources.srcDir(bundleLibusb.map { it.outputs.files.singleFile })
}

tasks.register<JavaExec>("run") {
    mainClass.set("com.codebutler.farebot.cli.MainKt")
    classpath = kotlin.jvm().compilations["main"].runtimeDependencyFiles +
        kotlin
            .jvm()
            .compilations["main"]
            .output.allOutputs
    jvmArgs(
        "-Xmx8g",
        "-Dsun.security.smartcardio.t0GetResponse=false",
        "-Dsun.security.smartcardio.t1GetResponse=false",
        "--add-modules=java.smartcardio,jdk.incubator.vector",
        "--enable-native-access=ALL-UNNAMED",
    )
}
