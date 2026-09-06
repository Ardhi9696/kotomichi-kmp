package com.kotomichi.di

import android.content.Context
import com.kotomichi.db.DbFactory
import com.kotomichi.repository.AuthRepository
import com.kotomichi.repository.AuthRepositoryImpl
import com.kotomichi.repository.DeckRepository
import com.kotomichi.repository.ProgressRepository
import com.kotomichi.repository.ProgressRepositoryImpl
import com.kotomichi.repository.SyncRepository
import com.kotomichi.repository.SyncRepositoryImpl
import com.kotomichi.repository.VocabRepository
import com.kotomichi.repository.VocabRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DbFactory.create(androidContext()) }
    
    single { HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = io.ktor.client.plugins.logging.LogLevel.BODY
        }
    } }
    
    single<VocabRepository> { VocabRepositoryImpl(
        database = get(),
        httpClient = get(),
        baseUrl = "https://api.kotomichi.app", // Replace with actual Supabase URL
        context = androidContext()
    ) }
    
    single<ProgressRepository> { ProgressRepositoryImpl(database = get()) }
    
    single<AuthRepository> { AuthRepositoryImpl(
        database = get(),
        httpClient = get(),
        baseUrl = "https://api.kotomichi.app",
        context = androidContext()
    ) }
    
    single<SyncRepository> { SyncRepositoryImpl(
        database = get(),
        httpClient = get(),
        baseUrl = "https://api.kotomichi.app",
        authRepository = get()
    ) }
}