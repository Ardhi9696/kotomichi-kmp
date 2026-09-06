# Kotlin Multiplatform
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.qualifier.Qualifier *;
}

# SQLDelight
-keep class com.squareup.sqldelight.** { *; }
-keep class com.kotomichi.db.** { *; }

# Ktor
-keep class io.ktor.** { *; }

# Kotlinx Serialization
-keep class kotlinx.serialization.** { *; }

# AndroidX WorkManager
-keep class androidx.work.** { *; }

# Timber
-keep class timber.log.** { *; }

# Keep model classes
-keep class com.kotomichi.model.** { *; }

# Keep repository interfaces
-keep interface com.kotomichi.repository.** { *; }

# Keep usecases
-keep class com.kotomichi.usecase.** { *; }