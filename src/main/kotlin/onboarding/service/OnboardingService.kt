package com.wellnessapp.onboarding.service

import com.wellnessapp.auth.repository.UserRepository
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.onboarding.dto.*
import com.wellnessapp.onboarding.repository.OnboardingRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

/**
 * Validation ranges below mirror the CHECK constraints already enforced in Postgres
 * (see the DB schema doc) so bad input gets a clean 400 VALIDATION_ERROR from the API
 * instead of a raw DB constraint-violation error.
 */
object OnboardingService {

    fun saveBasicInfo(userId: UUID, request: BasicInfoRequest): BasicInfoResponse {
        val errors = mutableListOf<Pair<String, String>>()

        if (request.age < 13 || request.age > 120) errors += "age" to "Must be between 13 and 120"
        if (request.gender.isBlank()) errors += "gender" to "Must not be blank"
        else if (request.gender.trim().length > 20) errors += "gender" to "Must be at most 20 characters"
        if (request.heightCm < 50.0 || request.heightCm > 250.0) errors += "heightCm" to "Must be between 50 and 250 cm"
        if (request.weightKg < 20.0 || request.weightKg > 300.0) errors += "weightKg" to "Must be between 20 and 300 kg"

        if (errors.isNotEmpty()) throw ValidationException(errors)

        val gender = request.gender.trim()
        OnboardingRepository.upsertProfile(
            userId = userId,
            age = request.age,
            gender = gender,
            heightCm = request.heightCm.toScaledBigDecimal(2),
            weightKg = request.weightKg.toScaledBigDecimal(2)
        )

        return BasicInfoResponse(request.age, gender, request.heightCm, request.weightKg)
    }

    fun saveGoals(userId: UUID, request: GoalsRequest): GoalsResponse {
        val cleaned = request.goalTypes.map { it.trim() }.filter { it.isNotBlank() }.distinct()

        val errors = mutableListOf<Pair<String, String>>()
        if (cleaned.isEmpty()) errors += "goalTypes" to "Select at least one goal"
        cleaned.forEach { goal ->
            if (goal.length > 50) errors += "goalTypes" to "\"$goal\" exceeds 50 characters"
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)

        OnboardingRepository.replaceGoals(userId, cleaned)
        return GoalsResponse(cleaned)
    }

    fun saveTargets(userId: UUID, request: TargetsRequest): TargetsResponse {
        val errors = mutableListOf<Pair<String, String>>()

        if (request.waterGoalMl < 500 || request.waterGoalMl > 10000) errors += "waterGoalMl" to "Must be between 500 and 10000 ml"
        if (request.stepGoal < 1000 || request.stepGoal > 50000) errors += "stepGoal" to "Must be between 1000 and 50000"
        if (request.sleepGoalHours < 3.0 || request.sleepGoalHours > 16.0) errors += "sleepGoalHours" to "Must be between 3 and 16 hours"

        if (errors.isNotEmpty()) throw ValidationException(errors)

        OnboardingRepository.upsertDailyTargets(
            userId = userId,
            waterGoalMl = request.waterGoalMl,
            stepGoal = request.stepGoal,
            sleepGoalHours = request.sleepGoalHours.toScaledBigDecimal(2)
        )

        return TargetsResponse(request.waterGoalMl, request.stepGoal, request.sleepGoalHours)
    }

    fun saveIntegrations(userId: UUID, request: IntegrationsRequest): IntegrationsResponse {
        OnboardingRepository.upsertHealthIntegrations(
            userId = userId,
            appleHealth = request.appleHealth,
            googleFit = request.googleFit,
            samsungHealth = request.samsungHealth
        )
        return IntegrationsResponse(request.appleHealth, request.googleFit, request.samsungHealth)
    }

    fun completeOnboarding(userId: UUID): OnboardingCompleteResponse {
        UserRepository.findById(userId) ?: throw NotFoundException("User not found")
        UserRepository.markOnboardingComplete(userId)
        return OnboardingCompleteResponse(userId.toString(), true)
    }

    private fun Double.toScaledBigDecimal(scale: Int): BigDecimal =
        BigDecimal.valueOf(this).setScale(scale, RoundingMode.HALF_UP)
}
