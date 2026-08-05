package com.wellnessapp.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class FirebaseTokenRequest(val firebaseToken: String)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val onboardingCompleted: Boolean
)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class SessionResponse(
    val userId: String,
    val email: String,
    val fullName: String?,
    val onboardingCompleted: Boolean
)
