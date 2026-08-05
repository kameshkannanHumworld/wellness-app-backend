package com.wellnessapp.auth.service

import com.wellnessapp.auth.dto.AuthResponse
import com.wellnessapp.auth.dto.SessionResponse
import com.wellnessapp.auth.repository.UserRepository
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.security.FirebaseAuthService
import com.wellnessapp.common.security.JwtService
import java.util.UUID

object AuthService {

    /**
     * Used by BOTH /auth/google and /auth/login (email/password) —
     * in both cases the Android app authenticates with Firebase first and
     * hands us the resulting Firebase ID token. We never see raw passwords.
     */
    fun authenticateWithFirebaseToken(firebaseToken: String): AuthResponse {
        val verifiedUser = FirebaseAuthService.verifyIdToken(firebaseToken)
        val user = UserRepository.findOrCreate(verifiedUser)

        return AuthResponse(
            accessToken = JwtService.generateAccessToken(user.id.toString()),
            refreshToken = JwtService.generateRefreshToken(user.id.toString()),
            userId = user.id.toString(),
            onboardingCompleted = user.onboardingCompleted
        )
    }

    fun getSession(userId: UUID): SessionResponse {
        val user = UserRepository.findById(userId) ?: throw NotFoundException("User not found")
        return SessionResponse(
            userId = user.id.toString(),
            email = user.email,
            fullName = user.fullName,
            onboardingCompleted = user.onboardingCompleted
        )
    }

    fun refresh(userId: UUID): AuthResponse {
        val user = UserRepository.findById(userId) ?: throw NotFoundException("User not found")
        return AuthResponse(
            accessToken = JwtService.generateAccessToken(user.id.toString()),
            refreshToken = JwtService.generateRefreshToken(user.id.toString()),
            userId = user.id.toString(),
            onboardingCompleted = user.onboardingCompleted
        )
    }
}
