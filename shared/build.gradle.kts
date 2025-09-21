import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import kotlin.time.ExperimentalTime
import java.nio.file.Files
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    kotlin("plugin.serialization") version "2.2.0"
}

kotlin {
    val ktorVersion = "3.2.1"

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.koin.core)
            implementation(libs.uuid)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.android)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        getByName("androidUnitTest") {
            dependencies {
                // This maps the @Test annotation to the JUnit 4 runner.
                implementation(kotlin("test-junit"))

                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.veleda.cyclewise.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    testOptions {
        unitTests.all { test ->
            test.testLogging {
                events("passed", "skipped", "failed", "standardOut", "standardError")
            }
        }
    }
}
dependencies {
    testImplementation(libs.junit.junit)
}