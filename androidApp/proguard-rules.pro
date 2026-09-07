# Optimize aggressively for size
-allowaccessmodification
-repackageclasses ''

# Preserve generic signatures needed for reflection/serialization
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleAnnotations, RuntimeInvisibleParameterAnnotations

# ── kotlinx.serialization ─────────────────────────────────────────────
# Standard rules — keep @Serializable companion + serializer() lookups.
-keep,includedescriptorclasses class com.kotomichi.**$$serializer { *; }
-keepclassmembers class com.kotomichi.** {
    *** Companion;
}
-keepclasseswithmembers class com.kotomichi.** {
    kotlinx.serialization.KSerializer serializer(...);
}
# Keep @Serializable classes (model, repository DTO, bkt).
-keep @kotlinx.serialization.Serializable class com.kotomichi.model.** { *; }
-keep @kotlinx.serialization.Serializable class com.kotomichi.repository.** { *; }
-keep @kotlinx.serialization.Serializable class com.kotomichi.bkt.** { *; }

# ── Ktor ───────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keep class kotlinx.io.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.io.**
-dontwarn org.slf4j.**

# ── SQLDelight ─────────────────────────────────────────────────────────
-keep class com.squareup.sqldelight.** { *; }
-keep class com.kotomichi.db.** { *; }
-keep class io.requery.android.** { *; }

# ── Koin ───────────────────────────────────────────────────────────────
-keep class org.koin.** { *; }
-keep class * extends org.koin.core.module.Module
-keep class * extends org.koin.dsl.Module
-keepclassmembers class * {
    @org.koin.core.qualifier.Qualifier *;
}

# ── Firebase Crashlytics ───────────────────────────────────────────────
-keepattributes SourceFile,LineNumberTable
-keep public class com.google.firebase.crashlytics.** { *; }
-keep public class com.google.firebase.analytics.** { *; }

# ── Timber ─────────────────────────────────────────────────────────────
-dontwarn org.jetbrains.annotations.**

# ── App-specific keeps ────────────────────────────────────────────────
# Repository interfaces + usecases referenced reflectively by Koin.
-keep interface com.kotomichi.repository.** { *; }
-keep class com.kotomichi.usecase.** { *; }
-keep class com.kotomichi.di.** { *; }

# Keep BuildConfig for runtime introspection.
-keep class com.kotomichi.app.BuildConfig { *; }
