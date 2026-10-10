package com.kotomichi.app.di

import android.content.Context
import com.kotomichi.app.BuildConfig
import com.kotomichi.shared.core.database.driver.DbFactory
import com.kotomichi.shared.core.repository.AuthRepository
import com.kotomichi.shared.feature.auth.data.AuthRepositoryImpl
import com.kotomichi.shared.core.repository.DeckRepository
import com.kotomichi.shared.feature.content.data.DeckRepositoryImpl
import com.kotomichi.shared.core.repository.ProgressRepository
import com.kotomichi.shared.feature.progress.data.ProgressRepositoryImpl
import com.kotomichi.shared.core.repository.SyncRepository
import com.kotomichi.shared.feature.sync.data.SyncRepositoryImpl
import com.kotomichi.shared.core.repository.VocabRepository
import com.kotomichi.shared.feature.content.data.VocabRepositoryImpl
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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { DbFactory.create(androidContext()) }
    
    @OptIn(ExperimentalSerializationApi::class)
    single { HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                namingStrategy = JsonNamingStrategy.SnakeCase
            })
        }
        install(Logging) {
            level = io.ktor.client.plugins.logging.LogLevel.BODY
        }
        // Supabase headers for all requests (anon role comes from `apikey`;
        // `Authorization` must be set per-request so user-data pulls use the user JWT)
        defaultRequest {
            header("apikey", BuildConfig.SUPABASE_ANON_KEY)
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
        supabaseRestUrl = supabaseRestUrl,
        context = androidContext()
    ) }
    
    single<SyncRepository> { SyncRepositoryImpl(
        database = get(),
        httpClient = get(),
        baseUrl = supabaseRestUrl,
        authRepository = get()
    ) }
}