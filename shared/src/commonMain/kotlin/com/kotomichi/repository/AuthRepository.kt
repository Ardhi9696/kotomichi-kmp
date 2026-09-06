package com.kotomichi.repository

import com.kotomichi.model.AuthTokens
import com.kotomichi.model.UserProfile
import com.kotomichi.model.LoginRequest
import com.kotomichi.model.RegisterRequest
import com.kotomichi.model.ResetPasswordRequest
import com.kotomichi.model.UpdatePasswordRequest
import com.kotomichi.model.UserRole
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
    
    fun observeCurrentUser(): Flow<UserProfile?>
}