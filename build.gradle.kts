plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.metro) apply false
}

allprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        filter {
            exclude { element ->
                element.file.path.contains("/build/")
            }
        }
    }

    // Workaround: ktlint-gradle filter doesn't reliably exclude generated sources
    // added via KMP source sets. Exclude them by removing build dir from task sources.
    afterEvaluate {
        tasks.withType<org.jlleitschuh.gradle.ktlint.tasks.BaseKtLintCheckTask>().configureEach {
            val buildDir = layout.buildDirectory.get().asFile
            exclude { it.file.startsWith(buildDir) }
        }
    }
}

subprojects {
    apply(plugin = "checkstyle")

    dependencies {
        "checkstyle"(rootProject.libs.checkstyle)
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            jvm()
            @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
            wasmJs {
                browser()
            }
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }

        // Library modules only need wasmJs compilation (klib) for app:web to consume.
        // Disable wasmJs executable linking and test tasks — the IR linker is extremely
        // slow and these modules get sufficient test coverage from jvmTest.
        if (project.path != ":app:web") {
            afterEvaluate {
                tasks.configureEach {
                    if (name.contains("WasmJs", ignoreCase = true) &&
                        (name.contains("Test", ignoreCase = true) ||
                            name.contains("Executable", ignoreCase = true))
                    ) {
                        enabled = false
                    }
                }
            }
        }
    }

    plugins.withId("org.jetbrains.compose") {
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            afterEvaluate {
                val composeExt = extensions.findByType<org.jetbrains.compose.ComposeExtension>()
                if (composeExt != null) {
                    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                        if (targets.findByName("jvm") != null) {
                            sourceSets.named("jvmMain") {
                                dependencies {
                                    implementation(composeExt.dependencies.desktop.currentOs)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    configurations.configureEach {
        resolutionStrategy {
            force("com.google.errorprone:error_prone_annotations:2.28.0")
        }
    }

    afterEvaluate {
        tasks.named("check") {
            dependsOn("checkstyle")
        }
        tasks.register<Checkstyle>("checkstyle") {
            configFile = rootProject.file("config/checkstyle/checkstyle.xml")
            source("src")
            include("**/*.java")
            exclude("**/gen/**")
            classpath = files()
        }
        extensions.findByType<CheckstyleExtension>()?.apply {
            isIgnoreFailures = false
        }
    }

    // Pin compose resource package names to match the module path hierarchy
    // (e.g. :transit:orca → farebot.transit.orca.generated.resources).
    plugins.withId("org.jetbrains.compose") {
        extensions.configure<org.jetbrains.compose.ComposeExtension> {
            val pkg = "farebot" + project.path.replace(":", ".").replace("-", "_") + ".generated.resources"
            (this as ExtensionAware).extensions.configure<org.jetbrains.compose.resources.ResourcesExtension> {
                packageOfResClass = pkg
            }
        }
    }

    // Workaround: Compose Multiplatform's CopyResourcesToAndroidAssetsTask doesn't
    // configure its outputDirectory with com.android.kotlin.multiplatform.library (AGP 9).
    // Fix by assembling compose resources as Java classpath resources instead of Android
    // assets. DefaultAndroidResourceReader falls back to ClassLoader.getResourceAsStream().
    plugins.withId("com.android.kotlin.multiplatform.library") {
        plugins.withId("org.jetbrains.compose") {
            plugins.withId("org.jetbrains.kotlin.multiplatform") {
                val resourceNamespace =
                    "farebot" + project.path.replace(":", ".").replace("-", "_") + ".generated.resources"
                val outputBaseDir = layout.buildDirectory.dir("compose-android-classpath-resources")

                val copyTask =
                    tasks.register<Copy>("copyComposeResourcesToAndroidClasspath") {
                        from(
                            layout.buildDirectory.dir(
                                "generated/compose/resourceGenerator/preparedResources/commonMain/composeResources",
                            ),
                        )
                        into(outputBaseDir.map { it.dir("composeResources/$resourceNamespace") })
                        dependsOn("prepareComposeResourcesTaskForCommonMain")
                    }

                afterEvaluate {
                    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
                        sourceSets.named("androidMain") {
                            resources.srcDir(outputBaseDir)
                        }
                    }
                    tasks.named("processAndroidMainJavaRes") {
                        dependsOn(copyTask)
                    }
                }
            }
        }
    }

    plugins.withType<com.android.build.gradle.BasePlugin> {
        @Suppress("DEPRECATION")
        extensions.findByType<com.android.build.gradle.BaseExtension>()?.apply {
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }

            lintOptions {
                isAbortOnError = true
                disable("InvalidPackage", "MissingTranslation")
            }
        }
    }
}
