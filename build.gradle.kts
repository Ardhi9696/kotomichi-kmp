plugins {
    id("org.jetbrains.kotlin.multiplatform") version "2.0.0" apply false
    id("com.android.application") version "8.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0" apply false
    id("com.squareup.sqldelight") version "2.0.1" apply false
}

allprojects {
    group = "com.kotomichi"
    version = "2.0.0"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}