package com.kotomichi.di

import android.content.Context
import com.kotomichi.app.BuildConfig
import com.kotomichi.db.DbFactory
import com.kotomichi.repository.AuthRepository
import com.kotomichi.repository.AuthRepositoryImpl
import com.kotomichi.repository.DeckRepository
import com.kotomichi.repository.DeckRepositoryImpl
import com.kotomichi.repository.ProgressRepository
import com.kotomichi.repository.ProgressRepositoryImpl
import com.kotomichi.repository.SyncRepository
import com.kotomichi.repository.SyncRepositoryImpl
import com.kotomichi.repository.VocabRepository
import com.kotomichi.repository.VocabRepositoryImpl
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
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
        // Supabase headers for all requests
        defaultRequest {
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
            header(HttpHeaders.Authorization, "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            header("Content-Type", "application/json")
            header("Prefer", "return=representation")
        }
    } }
    
    // Base URLs for different Supabase services
    val supabaseRestUrl = "${BuildConfig.SUPABASE_URL}/rest/v1"
    val supabaseAuthUrl = "${BuildConfig.SUPABASE_URL}/auth/v1"
    
    single<VocabRepository> { VocabRepositoryImpl(
        database = get(),
        httpClient = get(),
        baseUrl = supabaseRestUrl,
        context = androidContext()
    ) }
    
    single<ProgressRepository> { ProgressRepositoryImpl(database = get()) }

    single<DeckRepository> { DeckRepositoryImpl(database = get()) }
    
    single<AuthRepository> { AuthRepositoryImpl(
        database = get(),
        httpClient = get(),
        baseUrl = supabaseAuthUrl,
        context = androidContext()
    ) }
    
    single<SyncRepository> { SyncRepositoryImpl(
        database = get(),
        httpClient = get(),
        baseUrl = supabaseRestUrl,
        authRepository = get()
    ) }
}