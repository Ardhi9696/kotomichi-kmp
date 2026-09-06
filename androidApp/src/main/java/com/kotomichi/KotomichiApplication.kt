package com.kotomichi.app

import android.app.Application
import com.kotomichi.di.androidModule
import com.kotomichi.di.sharedModule
import com.kotomichi.di.startKoin
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.Module

class KotomichiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@KotomichiApplication)
            modules(listOf(sharedModule, androidModule))
        }
    }
}