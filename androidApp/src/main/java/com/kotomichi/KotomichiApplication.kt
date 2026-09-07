package com.kotomichi.app

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.kotomichi.di.androidModule
import com.kotomichi.di.sharedModule
import com.kotomichi.ui.theme.ThemePreference
import com.kotomichi.util.CrashCatcher
import com.kotomichi.util.SyncScheduler
import com.kotomichi.util.SyncTtlManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class KotomichiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inisialisasi Firebase/Crashlytics DULU supaya Crashlytics memasang uncaught-handler
        // internalnya. CrashCatcher dibungkus setelahnya & meneruskan crash ke handler tsb
        // => tetap tercatat sebagai FATAL di Crashlytics, sekaligus file lokal tetap ditulis.
        runCatching { FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true) }
        CrashCatcher.install(this)
        ThemePreference.init(this)
        startKoin {
            androidContext(this@KotomichiApplication)
            modules(listOf(sharedModule, androidModule))
        }
        SyncScheduler.schedulePeriodicSync(this)
        SyncTtlManager.init(this)
    }
}