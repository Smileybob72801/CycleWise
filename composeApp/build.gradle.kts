import com.android.build.api.dsl.Packaging
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import androidx.room.gradle.RoomExtension

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.room)
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
            implementation(libs.encoding.base64)
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
                implementation(libs.androidx.test.core)
                implementation(libs.koin.test)
                implementation(libs.koin.test.junit4)
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
        testInstrumentationRunner = "com.veleda.cyclewise.CustomTestRunner"
    }
    // This is the standard, safe way to handle duplicate text files
    // found in many third-party libraries. Instead of excluding with
    // wildcards, we tell Gradle to just pick the first one it finds.
    // It is generally unsafe to exclude the DEPENDENCIES or NOTICE files
    // as some libraries rely on them for service loading.
    packaging {
        resources {
            // Prefer picking one over blanket excludes for multi-release files:
            pickFirsts += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
            excludes += "META-INF/versions/**"
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
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Required for Robolectric to access Android resources like themes.
            isIncludeAndroidResources = true
        }
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

extensions.configure<RoomExtension> {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    debugImplementation(compose.uiTooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    ksp(libs.room.compiler)
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.koin.test)
    androidTestImplementation(libs.koin.test.junit4)

    androidTestUtil(libs.androidx.orchestrator)
}

