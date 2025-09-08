import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    sourceSets {

        all {
            languageSettings.optIn("kotlin.time.ExperimentalTime")
        }
        
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.koin.android)
            implementation(libs.koin.compose)
            implementation(libs.koin.core.viewmodel)
            implementation(libs.compose.navigation)
            implementation(libs.compose.uiTooling)
            implementation(libs.room.runtime)
            implementation(libs.room.ktx)
            implementation(libs.sqlcipher)
            implementation(libs.argon2)
            implementation(libs.security.crypto)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.datastore.preferences)
            implementation(libs.compose.calendar)
            implementation(libs.uuid)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(projects.shared)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        getByName("androidUnitTest") {
            dependencies {
                implementation(libs.kotlin.test)
                implementation(libs.kotlin.test.junit)
                implementation(libs.mockk)
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.androidx.arch.core.testing)
                implementation(libs.robolectric)
                implementation(libs.turbine)
            }
        }
    }
}

android {
    namespace = "com.veleda.cyclewise"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.veleda.cyclewise"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "META-INF/versions/**"

            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/DEPENDENCIES"
            )
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            // Required for Robolectric to access Android resources like themes.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    ksp(libs.room.compiler)
}

