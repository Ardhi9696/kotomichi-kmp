pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.multiplatform") version "2.0.0" apply false
        id("com.android.application") version "8.4.0" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
        id("app.cash.sqldelight") version "2.0.1" apply false
        id("org.jetbrains.kotlinx.kotlinx-serialization-json") version "1.6.3" apply false
    }
}

rootProject.name = "Kotomichi"

include(":shared")
include(":androidApp")