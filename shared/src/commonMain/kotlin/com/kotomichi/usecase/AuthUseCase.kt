package com.kotomichi.usecase

import com.kotomichi.model.AuthTokens
import com.kotomichi.model.UserProfile
import com.kotomichi.model.LoginRequest
import com.kotomichi.model.RegisterRequest
import com.kotomichi.model.ResetPasswordRequest
import com.kotomichi.model.UpdatePasswordRequest
import com.kotomichi.model.UserRole
import com.kotomichi.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class AuthUseCase(
    private val authRepository: AuthRepository
) {
    val currentUser: Flow<UserProfile?> = authRepository.currentUser
    val isAuthenticated: Flow<Boolean> = authRepository.isAuthenticated
    
    suspend fun login(request: LoginRequest): AuthTokens {
        return authRepository.login(request)
    }
    
    suspend fun register(request: RegisterRequest): AuthTokens {
        return authRepository.register(request)
    }
    
    suspend fun logout() {
        authRepository.logout()
    }
    
    suspend fun refreshToken(): AuthTokens {
        return authRepository.refreshToken()
    }
    
    suspend fun resetPassword(request: ResetPasswordRequest) {
        authRepository.resetPassword(request)
    }
    
    suspend fun updatePassword(request: UpdatePasswordRequest) {
        authRepository.updatePassword(request)
    }
    
    suspend fun getCurrentUser(): UserProfile? {
        return authRepository.getCurrentUser()
    }
    
    suspend fun updateProfile(profile: UserProfile): UserProfile {
        return authRepository.updateProfile(profile)
    }
    
    suspend fun changeRole(userId: String, role: UserRole) {
        authRepository.changeRole(userId, role)
    }
    
    suspend fun deleteUser(userId: String) {
        authRepository.deleteUser(userId)
    }
    
    fun observeCurrentUser(): Flow<UserProfile?> {
        return authRepository.observeCurrentUser()
    }
}