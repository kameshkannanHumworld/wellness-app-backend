package com.wellnessapp.onboarding.dto

import kotlinx.serialization.Serializable

// --- Step 1: basic info ---

@Serializable
data class BasicInfoRequest(
    val age: Int,
    val gender: String,
    val heightCm: Double,
    val weightKg: Double,
    val stressLevel: String? = null   // one of: low, normal, high — optional; omit to leave unchanged
)

@Serializable
data class BasicInfoResponse(
    val age: Int,
    val gender: String,
    val heightCm: Double,
    val weightKg: Double,
    val stressLevel: String? = null
)

// --- Fetch profile (reads the `profiles` table directly, does not require onboarding to be "complete") ---

@Serializable
data class ProfileResponse(
    val age: Int?,
    val gender: String?,
    val heightCm: Double?,
    val weightKg: Double?,
    val stressLevel: String?
)

// --- Step 2: goals (multi-select) ---

@Serializable
data class GoalsRequest(val goalTypes: List<String>)

@Serializable
data class GoalsResponse(val goalTypes: List<String>)

// --- Step 3: daily targets ---

@Serializable
data class TargetsRequest(
    val waterGoalMl: Int,
    val stepGoal: Int,
    val sleepGoalHours: Double
)

@Serializable
data class TargetsResponse(
    val waterGoalMl: Int,
    val stepGoal: Int,
    val sleepGoalHours: Double
)

// --- Step 4: health integrations ---

@Serializable
data class IntegrationsRequest(
    val appleHealth: Boolean = false,
    val googleFit: Boolean = false,
    val samsungHealth: Boolean = false
)

@Serializable
data class IntegrationsResponse(
    val appleHealth: Boolean,
    val googleFit: Boolean,
    val samsungHealth: Boolean
)

// --- Step 5: complete ---

@Serializable
data class OnboardingCompleteResponse(
    val userId: String,
    val onboardingCompleted: Boolean
)
