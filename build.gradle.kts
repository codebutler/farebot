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
}

subprojects {
    apply(plugin = "checkstyle")

    dependencies {
        "checkstyle"(rootProject.libs.checkstyle)
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
            jvm()
            compilerOptions {
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }
        }
    }

    plugins.withId("org.jetbrains.compose") {
        plugins.withId("org.jetbrains.kotlin.multiplatform") {
            afterEvaluate {
                val composeExt = extensions.findByType<org.jetbrains.compose.ComposeExtension>()
                if (composeExt != null) {
                    extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension> {
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

    configurations.configureEach {
        resolutionStrategy {
            force("com.google.errorprone:error_prone_annotations:2.28.0")
        }
    }

    afterEvaluate {
        if (project.name.contains("farebot")) {
            tasks.named("check") {
                dependsOn("checkstyle")
            }
            tasks.register<Checkstyle>("checkstyle") {
                configFile = file("config/checkstyle/checkstyle.xml")
                source("src")
                include("**/*.java")
                exclude("**/gen/**")
                classpath = files()
            }
            extensions.findByType<CheckstyleExtension>()?.apply {
                isIgnoreFailures = false
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
