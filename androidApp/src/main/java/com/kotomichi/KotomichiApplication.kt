package com.kotomichi.app

import android.app.Application
import com.kotomichi.di.androidModule
import com.kotomichi.di.sharedModule
import com.kotomichi.ui.theme.ThemePreference
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class KotomichiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ThemePreference.init(this)
        startKoin {
            androidContext(this@KotomichiApplication)
            modules(listOf(sharedModule, androidModule))
        }
    }
}