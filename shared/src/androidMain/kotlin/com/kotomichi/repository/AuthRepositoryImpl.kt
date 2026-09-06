package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.model.UserProfile as ModelUserProfile
import com.kotomichi.model.AuthTokens as ModelAuthTokens
import com.kotomichi.model.LoginRequest
import com.kotomichi.model.RegisterRequest
import com.kotomichi.model.ResetPasswordRequest
import com.kotomichi.model.UpdatePasswordRequest
import com.kotomichi.model.UserRole
import com.kotomichi.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.delete
import io.ktor.http.contentType
import io.ktor.client.request.setBody
import io.ktor.client.request.header
import io.ktor.client.call.body
import io.ktor.http.HttpStatusCode
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthRepositoryImpl(
    private val database: KotomichiDatabase,
    private val httpClient: HttpClient,
    private val baseUrl: String,
    private val context: Context
) : AuthRepository {
    
    private val userQueries = database.userProfileQueries
    
    private val _currentUser = MutableStateFlow<ModelUserProfile?>(null)
    override val currentUser: Flow<ModelUserProfile?> = _currentUser.asStateFlow()
    
    private val _isAuthenticated = MutableStateFlow(false)
    override val isAuthenticated: Flow<Boolean> = _isAuthenticated.asStateFlow()
    
    private var accessToken: String? = null
    private var refreshToken: String? = null
    
    init {
        loadStoredTokens()
        loadCurrentUser()
    }
    
    private fun loadStoredTokens() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val sharedPrefs = EncryptedSharedPreferences.create(
                context,
                "kotomichi_auth",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            accessToken = sharedPrefs.getString("access_token", null)
            refreshToken = sharedPrefs.getString("refresh_token", null)
            
            _isAuthenticated.value = accessToken != null
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun saveTokens(access: String, refresh: String) {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val sharedPrefs = EncryptedSharedPreferences.create(
                context,
                "kotomichi_auth",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPrefs.edit()
                .putString("access_token", access)
                .putString("refresh_token", refresh)
                .apply()
            
            accessToken = access
            refreshToken = refresh
            _isAuthenticated.value = true
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun clearTokens() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            val sharedPrefs = EncryptedSharedPreferences.create(
                context,
                "kotomichi_auth",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            
            sharedPrefs.edit().clear().apply()
            accessToken = null
            refreshToken = null
            _isAuthenticated.value = false
            _currentUser.value = null
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun loadCurrentUser() {
        // Load from local DB
        // For now, we'll fetch from remote after login
    }
    
    override suspend fun login(request: LoginRequest): ModelAuthTokens = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.post("$baseUrl/auth/login") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
        
        if (response.status == HttpStatusCode.OK) {
            val tokens = response.body<ModelAuthTokens>()
            saveTokens(tokens.accessToken, tokens.refreshToken)
            fetchAndStoreUser()
            tokens
        } else {
            throw Exception("Login failed: ${response.status}")
        }
    }
    
    override suspend fun register(request: RegisterRequest): ModelAuthTokens = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.post("$baseUrl/auth/register") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
        
        if (response.status == HttpStatusCode.Created || response.status == HttpStatusCode.OK) {
            val tokens = response.body<ModelAuthTokens>()
            saveTokens(tokens.accessToken, tokens.refreshToken)
            fetchAndStoreUser()
            tokens
        } else {
            throw Exception("Registration failed: ${response.status}")
        }
    }
    
    override suspend fun logout() = withContext(Dispatchers.IO) {
        clearTokens()
    }
    
    override suspend fun refreshToken(): ModelAuthTokens = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.post("$baseUrl/auth/refresh") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(mapOf("refresh_token" to refreshToken))
        }
        
        if (response.status == HttpStatusCode.OK) {
            val tokens = response.body<ModelAuthTokens>()
            saveTokens(tokens.accessToken, tokens.refreshToken)
            tokens
        } else {
            clearTokens()
            throw Exception("Token refresh failed")
        }
    }
    
    override suspend fun resetPassword(request: ResetPasswordRequest) = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.post("$baseUrl/auth/reset-password") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Password reset failed")
        }
    }
    
    override suspend fun updatePassword(request: UpdatePasswordRequest) = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.put("$baseUrl/auth/password") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(request)
            accessToken?.let { header("Authorization", "Bearer $it") }
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Password update failed")
        }
    }
    
    override suspend fun getCurrentUser(): ModelUserProfile? = withContext(Dispatchers.IO) {
        _currentUser.value
    }
    
    override suspend fun updateProfile(profile: ModelUserProfile): ModelUserProfile = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.put("$baseUrl/user/profile") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(profile)
            accessToken?.let { header("Authorization", "Bearer $it") }
        }
        
        if (response.status == HttpStatusCode.OK) {
            val updated = response.body<ModelUserProfile>()
            _currentUser.value = updated
            persistUser(updated)
            updated
        } else {
            throw Exception("Profile update failed")
        }
    }
    
    override suspend fun changeRole(userId: String, role: UserRole) = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.put("$baseUrl/admin/users/$userId/role") {
            contentType(io.ktor.http.ContentType.Application.Json)
            setBody(mapOf("role" to role.name))
            accessToken?.let { header("Authorization", "Bearer $it") }
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw Exception("Role change failed")
        }
    }
    
    override suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.delete("$baseUrl/admin/users/$userId") {
            accessToken?.let { header("Authorization", "Bearer $it") }
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw Exception("User deletion failed")
        }
    }
    
    override fun observeCurrentUser(): Flow<ModelUserProfile?> {
        return _currentUser.asStateFlow()
    }
    
    private suspend fun fetchAndStoreUser() = withContext(Dispatchers.IO) {
        val response: io.ktor.client.statement.HttpResponse = httpClient.get("$baseUrl/user/me") {
            accessToken?.let { header("Authorization", "Bearer $it") }
        }
        
        if (response.status == HttpStatusCode.OK) {
            val user = response.body<ModelUserProfile>()
            _currentUser.value = user
            persistUser(user)
        }
    }

    private fun persistUser(profile: ModelUserProfile) {
        userQueries.upsert(
            id = profile.id,
            display_name = profile.displayName,
            role = profile.role.name,
            preferred_locale = profile.preferredLocale,
            level = profile.level.toLong(),
            exp = profile.exp.toLong(),
            last_review_date = profile.lastReviewDate,
            current_streak = profile.currentStreak.toLong(),
            longest_streak = profile.longestStreak.toLong(),
            created_at = profile.createdAt,
            updated_at = System.currentTimeMillis(),
            theme = profile.theme,
            last_seen_at = profile.lastSeenAt
        )
    }
}