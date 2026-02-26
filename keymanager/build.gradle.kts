plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidLibrary {
        namespace = "com.codebutler.farebot.keymanager"
        compileSdk =
            libs.versions.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.minSdk
                .get()
                .toInt()
    }

    // NO iOS targets — crypto code must not ship to iOS

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        commonMain.dependencies {
            implementation(libs.compose.resources)
            implementation(libs.compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(project(":base"))
            implementation(project(":card"))
            implementation(project(":card:classic"))
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-Xadd-modules=jdk.incubator.vector")
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs(
        "--add-modules=jdk.incubator.vector",
        "--enable-native-access=ALL-UNNAMED",
    )
}

// --- Metal GPU brute force native compilation (macOS only) ---
if (System.getProperty("os.name")?.contains("Mac") == true) {
    val metalSrc = layout.projectDirectory.dir("src/jvmMain/metal")
    val objcSrc = layout.projectDirectory.dir("src/jvmMain/objc")
    val nativeBuildDir = layout.buildDirectory.dir("native")
    val nativeResourcesDir = layout.buildDirectory.dir("native-resources/native")

    val compileMetalAir by tasks.registering(Exec::class) {
        description = "Compile Metal shader to AIR"
        inputs.file(metalSrc.file("brute_force.metal"))
        outputs.file(nativeBuildDir.map { it.file("brute_force.air") })
        doFirst { nativeBuildDir.get().asFile.mkdirs() }
        commandLine(
            "xcrun",
            "metal",
            "-c",
            metalSrc.file("brute_force.metal").asFile.absolutePath,
            "-o",
            nativeBuildDir
                .get()
                .file("brute_force.air")
                .asFile.absolutePath,
        )
    }

    val linkMetallib by tasks.registering(Exec::class) {
        description = "Link Metal AIR to metallib"
        dependsOn(compileMetalAir)
        inputs.file(nativeBuildDir.map { it.file("brute_force.air") })
        outputs.file(nativeResourcesDir.map { it.file("brute_force.metallib") })
        doFirst { nativeResourcesDir.get().asFile.mkdirs() }
        commandLine(
            "xcrun",
            "metallib",
            nativeBuildDir
                .get()
                .file("brute_force.air")
                .asFile.absolutePath,
            "-o",
            nativeResourcesDir
                .get()
                .file("brute_force.metallib")
                .asFile.absolutePath,
        )
    }

    val compileObjC by tasks.registering(Exec::class) {
        description = "Compile Objective-C Metal bridge"
        inputs.file(objcSrc.file("metal_bridge.m"))
        inputs.file(objcSrc.file("metal_bridge.h"))
        outputs.file(nativeBuildDir.map { it.file("metal_bridge.o") })
        doFirst { nativeBuildDir.get().asFile.mkdirs() }
        commandLine(
            "xcrun",
            "clang",
            "-c",
            "-fobjc-arc",
            "-O2",
            "-I",
            objcSrc.asFile.absolutePath,
            objcSrc.file("metal_bridge.m").asFile.absolutePath,
            "-o",
            nativeBuildDir
                .get()
                .file("metal_bridge.o")
                .asFile.absolutePath,
        )
    }

    val linkDylib by tasks.registering(Exec::class) {
        description = "Link Metal bridge dynamic library"
        dependsOn(compileObjC)
        inputs.file(nativeBuildDir.map { it.file("metal_bridge.o") })
        outputs.file(nativeResourcesDir.map { it.file("libmetal_bridge.dylib") })
        doFirst { nativeResourcesDir.get().asFile.mkdirs() }
        commandLine(
            "xcrun",
            "clang",
            "-dynamiclib",
            "-fobjc-arc",
            "-framework",
            "Metal",
            "-framework",
            "Foundation",
            nativeBuildDir
                .get()
                .file("metal_bridge.o")
                .asFile.absolutePath,
            "-o",
            nativeResourcesDir
                .get()
                .file("libmetal_bridge.dylib")
                .asFile.absolutePath,
        )
    }

    val buildMetalNative by tasks.registering {
        description = "Build Metal shader and bridge native libraries"
        dependsOn(linkMetallib, linkDylib)
    }

    // Add native-resources to JVM resources so they end up on the classpath
    kotlin.sourceSets.getByName("jvmMain") {
        resources.srcDir(layout.buildDirectory.dir("native-resources"))
    }

    // Wire native build into resource processing
    tasks.named("jvmProcessResources") {
        dependsOn(buildMetalNative)
    }
}
