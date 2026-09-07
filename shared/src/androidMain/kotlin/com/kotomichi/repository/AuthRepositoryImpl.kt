package com.kotomichi.repository

import com.kotomichi.db.KotomichiDatabase
import com.kotomichi.model.UserProfile as ModelUserProfile
import com.kotomichi.model.AuthTokens as ModelAuthTokens
import com.kotomichi.model.LoginRequest
import com.kotomichi.model.RegisterRequest
import com.kotomichi.model.ResetPasswordRequest
import com.kotomichi.model.UpdatePasswordRequest
import com.kotomichi.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.text.SimpleDateFormat
import java.util.Locale

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
    private var userId: String? = null

    init {
        loadStoredTokens()
        loadCurrentUser()
    }

    private fun loadStoredTokens() {
        try {
            val sharedPrefs = encryptedPrefs()
            accessToken = sharedPrefs.getString("access_token", null)
            refreshToken = sharedPrefs.getString("refresh_token", null)
            userId = sharedPrefs.getString("user_id", null)

            _isAuthenticated.value = accessToken != null
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun saveTokens(access: String, refresh: String, uid: String? = null) {
        try {
            val sharedPrefs = encryptedPrefs()
            sharedPrefs.edit()
                .putString("access_token", access)
                .putString("refresh_token", refresh)
                .apply()
            if (uid != null) {
                sharedPrefs.edit().putString("user_id", uid).apply()
                userId = uid
            }
            accessToken = access
            refreshToken = refresh
            _isAuthenticated.value = true
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun clearTokens() {
        try {
            val sharedPrefs = encryptedPrefs()
            sharedPrefs.edit()
                .remove("access_token")
                .remove("refresh_token")
                .remove("user_id")
                .apply()
            accessToken = null
            refreshToken = null
            userId = null
            _isAuthenticated.value = false
            _currentUser.value = null
        } catch (e: Exception) {
            // Handle error
        }
    }

    private fun encryptedPrefs(): android.content.SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            "kotomichi_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun loadCurrentUser() {
        val uid = userId ?: return
        try {
            val row = userQueries.selectById(uid).executeAsOneOrNull() ?: return
            _currentUser.value = row.toModelProfile()
        } catch (e: Exception) {
            // Handle error
        }
    }

    override suspend fun login(request: LoginRequest): ModelAuthTokens = withContext(Dispatchers.IO) {
        val response = httpClient.post("$baseUrl/token") {
            contentType(ContentType.Application.Json)
            parameter("grant_type", "password")
            setBody(mapOf("email" to request.email, "password" to request.password))
        }

        if (response.status.isSuccess()) {
            val session = response.body<SupabaseSession>()
            val tokens = session.toTokens()
            if (tokens.accessToken.isBlank()) {
                throw Exception("Autentikasi gagal, coba lagi")
            }
            saveTokens(tokens.accessToken, tokens.refreshToken, session.user?.id)
            session.user?.let { storeSupabaseUser(it) }
            tokens
        } else {
            throw Exception("Login gagal: ${extractErrorMessage(response)}")
        }
    }

    override suspend fun register(request: RegisterRequest): ModelAuthTokens = withContext(Dispatchers.IO) {
        val response = httpClient.post("$baseUrl/signup") {
            contentType(ContentType.Application.Json)
            setBody(mapOf(
                "email" to request.email,
                "password" to request.password,
                "data" to mapOf("name" to request.name)
            ))
        }

        if (response.status.isSuccess()) {
            val session = response.body<SupabaseSession>()
            if (session.access_token.isBlank()) {
                throw Exception("Registrasi berhasil. Periksa email Anda untuk konfirmasi.")
            }
            val tokens = session.toTokens()
            saveTokens(tokens.accessToken, tokens.refreshToken, session.user?.id)
            session.user?.let { storeSupabaseUser(it) }
            tokens
        } else {
            throw Exception("Registrasi gagal: ${extractErrorMessage(response)}")
        }
    }

    override suspend fun logout() {
        withContext(Dispatchers.IO) {
            try {
                httpClient.post("$baseUrl/logout") {
                    contentType(ContentType.Application.Json)
                    setBody(mapOf("refresh_token" to (refreshToken ?: "")))
                    accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
                }
            } catch (e: Exception) {
                // Ignore network errors, clean locally anyway
            } finally {
                clearTokens()
            }
        }
    }

    override suspend fun refreshToken(): ModelAuthTokens = withContext(Dispatchers.IO) {
        val response = httpClient.post("$baseUrl/token") {
            contentType(ContentType.Application.Json)
            parameter("grant_type", "refresh_token")
            setBody(mapOf("refresh_token" to (refreshToken ?: "")))
        }

        if (response.status.isSuccess()) {
            val session = response.body<SupabaseSession>()
            val tokens = session.toTokens()
            saveTokens(tokens.accessToken, tokens.refreshToken, session.user?.id)
            tokens
        } else {
            clearTokens()
            throw Exception("Sesi berakhir, silakan masuk kembali")
        }
    }

    override suspend fun resetPassword(request: ResetPasswordRequest) = withContext(Dispatchers.IO) {
        val response = httpClient.post("$baseUrl/recover") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("email" to request.email))
        }

        if (!response.status.isSuccess()) {
            throw Exception("Gagal mengirim tautan reset: ${extractErrorMessage(response)}")
        }
    }

    override suspend fun updatePassword(request: UpdatePasswordRequest) = withContext(Dispatchers.IO) {
        val response = httpClient.put("$baseUrl/user") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("password" to request.password))
            accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }

        if (!response.status.isSuccess()) {
            throw Exception("Gagal mengubah kata sandi: ${extractErrorMessage(response)}")
        }
    }

    override suspend fun getCurrentUser(): ModelUserProfile? = withContext(Dispatchers.IO) {
        _currentUser.value
    }

    override suspend fun updateProfile(profile: ModelUserProfile): ModelUserProfile = withContext(Dispatchers.IO) {
        val response = httpClient.put("$baseUrl/user") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("data" to mapOf("name" to profile.displayName)))
            accessToken?.let { header(HttpHeaders.Authorization, "Bearer $it") }
        }

        if (response.status.isSuccess()) {
            _currentUser.value = profile
            persistUser(profile)
            profile
        } else {
            throw Exception("Gagal memperbarui profil")
        }
    }

    override suspend fun changeRole(userId: String, role: UserRole) = withContext(Dispatchers.IO) {
        throw Exception("Operasi admin memerlukan kunci layanan (service role)")
    }

    override suspend fun deleteUser(userId: String) = withContext(Dispatchers.IO) {
        throw Exception("Operasi admin memerlukan kunci layanan (service role)")
    }

    override fun observeCurrentUser(): Flow<ModelUserProfile?> {
        return _currentUser.asStateFlow()
    }

    override suspend fun getAccessToken(): String? = withContext(Dispatchers.IO) {
        accessToken
    }

    override suspend fun publishProfile(profile: ModelUserProfile) = withContext(Dispatchers.IO) {
        persistUser(profile)
        _currentUser.value = profile
    }

    private suspend fun fetchAndStoreUser() = withContext(Dispatchers.IO) {
        val token = accessToken ?: return@withContext
        val response = httpClient.get("$baseUrl/user") {
            header(HttpHeaders.Authorization, "Bearer $token")
        }

        if (response.status.isSuccess()) {
            val supabaseUser = response.body<SupabaseUser>()
            storeSupabaseUser(supabaseUser)
        }
    }

    private suspend fun storeSupabaseUser(user: SupabaseUser) {
        val profile = user.toModelProfile()
        _currentUser.value = profile
        persistUser(profile)
    }

    private fun SupabaseUser.toModelProfile(): ModelUserProfile {
        val metaName = user_metadata["name"]?.jsonPrimitive?.contentOrNull
        return ModelUserProfile(
            id = id,
            displayName = metaName ?: email ?: "Pengguna",
            role = UserRole.USER,
            preferredLocale = "id",
            createdAt = parseTimestamp(created_at),
            updatedAt = System.currentTimeMillis(),
            theme = "system"
        )
    }

    private fun SupabaseSession.toTokens(): ModelAuthTokens = ModelAuthTokens(
        accessToken = access_token,
        refreshToken = refresh_token,
        expiresAt = if (expires_in > 0) System.currentTimeMillis() + expires_in * 1000 else 0L,
        tokenType = token_type
    )

    private fun parseTimestamp(iso: String?): Long {
        if (iso.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                .parse(iso.substringBefore('.').substringBefore('Z'))
                ?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private suspend fun extractErrorMessage(response: HttpResponse): String {
        return try {
            val json = Json.parseToJsonElement(response.bodyAsText()).jsonObject
            json["msg"]?.jsonPrimitive?.contentOrNull
                ?: json["message"]?.jsonPrimitive?.contentOrNull
                ?: response.status.toString()
        } catch (e: Exception) {
            response.status.toString()
        }
    }

    private fun com.kotomichi.db.UserProfile.toModelProfile(): ModelUserProfile = ModelUserProfile(
        id = id,
        displayName = display_name,
        role = UserRole.entries.firstOrNull { it.name == role } ?: UserRole.USER,
        preferredLocale = preferred_locale,
        level = level.toInt(),
        exp = exp.toInt(),
        lastReviewDate = last_review_date,
        currentStreak = current_streak.toInt(),
        longestStreak = longest_streak.toInt(),
        createdAt = created_at,
        updatedAt = updated_at,
        theme = theme,
        lastSeenAt = last_seen_at
    )

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