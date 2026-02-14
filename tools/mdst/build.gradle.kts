plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("com.codebutler.farebot.tools.mdst.MainKt")
        }
    }

    sourceSets {
        jvmMain.dependencies {
            implementation(project(":base"))
            implementation(libs.kotlinx.serialization.protobuf)
        }
    }
}

tasks.withType<JavaExec>().configureEach {
    workingDir = rootProject.projectDir
}
