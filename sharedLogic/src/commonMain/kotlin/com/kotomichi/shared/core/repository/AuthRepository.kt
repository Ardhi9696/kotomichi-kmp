package com.kotomichi.shared.core.repository

import com.kotomichi.shared.core.model.AuthTokens
import com.kotomichi.shared.core.model.UserProfile
import com.kotomichi.shared.core.model.LoginRequest
import com.kotomichi.shared.core.model.RegisterRequest
import com.kotomichi.shared.core.model.ResetPasswordRequest
import com.kotomichi.shared.core.model.UpdatePasswordRequest
import com.kotomichi.shared.core.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface AuthRepository {
    val currentUser: Flow<UserProfile?>
    val isAuthenticated: Flow<Boolean>
    
    suspend fun login(request: LoginRequest): AuthTokens
    suspend fun register(request: RegisterRequest): AuthTokens
    suspend fun logout()
    suspend fun refreshToken(): AuthTokens
    suspend fun resetPassword(request: ResetPasswordRequest)
    suspend fun updatePassword(request: UpdatePasswordRequest)
    suspend fun getCurrentUser(): UserProfile?
    suspend fun updateProfile(profile: UserProfile): UserProfile
    suspend fun changeRole(userId: String, role: UserRole)
    suspend fun deleteUser(userId: String)
    suspend fun getAccessToken(): String?
    suspend fun currentUserId(): String?
    suspend fun publishProfile(profile: UserProfile)
    suspend fun syncRemoteProfile()
    
    fun observeCurrentUser(): Flow<UserProfile?>
}