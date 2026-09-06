plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    val hostOs = System.getProperty("os.name")
    val isMingwX64 = hostOs.startsWith("Windows")
    val nativeTarget = when {
        hostOs == "Mac OS X" -> listOf(iosX64(), iosArm64(), iosSimulatorArm64())
        hostOs == "Linux" -> listOf(iosX64(), iosArm64(), iosSimulatorArm64())
        else -> listOf()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.serialization)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.sqldelight.runtime)
                implementation(libs.koin.core)
                implementation(libs.koin.android)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.turbine)
                implementation(libs.junit)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.lifecycle.runtime)
                implementation(libs.androidx.lifecycle.viewmodel.compose)
                implementation(libs.koin.android)
                implementation(libs.androidx.security.crypto)
                implementation(libs.ktor.client.android)
                implementation(libs.sqldelight.android.driver)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(libs.ktor.client.darwin)
                implementation(libs.sqldelight.native.driver)
            }
        }
    }
}

sqldelight {
    databases {
        create("KotomichiDatabase") {
            packageName.set("com.kotomichi.db")
        }
    }
}

kotlin {
    jvmToolchain(21)
}

android {
    namespace = "com.kotomichi.shared"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
        targetSdk = 34
    }
}