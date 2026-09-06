package com.kotomichi.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val displayName: String = "",
    val role: UserRole = UserRole.USER,
    val preferredLocale: String = "en",
    val level: Int = 1,
    val exp: Int = 0,
    val lastReviewDate: Long? = null,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val theme: String = "system",
    val lastSeenAt: Long? = null
) {
    val name: String get() = displayName
    val currentLevel: Int get() = level
    val totalExp: Long get() = exp.toLong()
    val lastActiveDate: Long get() = lastSeenAt ?: lastReviewDate ?: 0L
    val email: String get() = ""

    fun expForNextLevel(): Long {
        val base = 100L
        return (base * Math.pow(level.toDouble(), 1.5)).toLong()
    }
    
    fun expProgressPercent(): Double {
        val currentLevelExp = expForLevel(level)
        val nextLevelExp = expForLevel(level + 1)
        val progress = exp - currentLevelExp
        val needed = nextLevelExp - currentLevelExp
        return (progress.toDouble() / needed * 100).coerceIn(0.0, 100.0)
    }
    
    private fun expForLevel(level: Int): Long {
        val base = 100L
        return (base * Math.pow(level.toDouble(), 1.5)).toLong()
    }
}

enum class UserRole(val label: String) {
    SUPER_ADMIN("Super Admin"),
    ADMIN("Admin"),
    USER("User")
}

@Serializable
data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val tokenType: String = "bearer"
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class ResetPasswordRequest(
    val email: String
)

@Serializable
data class UpdatePasswordRequest(
    val password: String
)