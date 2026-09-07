pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("org.jetbrains.kotlin.multiplatform") version "2.1.0" apply false
        id("com.android.application") version "8.9.2" apply false
        id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
        id("app.cash.sqldelight") version "2.0.2" apply false
        id("org.jetbrains.kotlinx.kotlinx-serialization-json") version "1.7.3" apply false
        id("com.google.gms.google-services") version "4.4.1" apply false
        id("com.google.firebase.crashlytics") version "3.0.2" apply false
    }
}

rootProject.name = "Kotomichi"

include(":shared")
include(":androidApp")