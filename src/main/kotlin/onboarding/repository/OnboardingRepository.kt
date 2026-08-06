package com.wellnessapp.onboarding.repository

import com.wellnessapp.onboarding.model.DailyTargets
import com.wellnessapp.onboarding.model.Goals
import com.wellnessapp.onboarding.model.HealthIntegrations
import com.wellnessapp.onboarding.model.Profiles
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

object OnboardingRepository {

    // ---- Profile (basic info) — upserted at the app layer; see note in OnboardingTables.kt ----

    fun upsertProfile(userId: UUID, age: Int, gender: String, heightCm: BigDecimal, weightKg: BigDecimal) = transaction {
        val existingId = Profiles.selectAll().where { Profiles.userId eq userId }
            .map { it[Profiles.id] }
            .singleOrNull()

        if (existingId != null) {
            Profiles.update({ Profiles.id eq existingId }) {
                it[Profiles.age] = age
                it[Profiles.gender] = gender
                it[Profiles.heightCm] = heightCm
                it[Profiles.weightKg] = weightKg
            }
        } else {
            Profiles.insert {
                it[id] = UUID.randomUUID()
                it[Profiles.userId] = userId
                it[Profiles.age] = age
                it[Profiles.gender] = gender
                it[Profiles.heightCm] = heightCm
                it[Profiles.weightKg] = weightKg
            }
        }
    }

    // ---- Goals (multi-select) — full set replaced on every save ----

    fun replaceGoals(userId: UUID, goalTypes: List<String>) = transaction {
        Goals.deleteWhere { Goals.userId eq userId }
        goalTypes.forEach { goalType ->
            Goals.insert {
                it[id] = UUID.randomUUID()
                it[Goals.userId] = userId
                it[Goals.goalType] = goalType
            }
        }
    }

    fun findGoals(userId: UUID): List<String> = transaction {
        Goals.selectAll().where { Goals.userId eq userId }
            .mapNotNull { it[Goals.goalType] }
    }

    // ---- Daily targets — single row per user (DB-enforced unique) ----

    fun upsertDailyTargets(userId: UUID, waterGoalMl: Int, stepGoal: Int, sleepGoalHours: BigDecimal) = transaction {
        val existingId = DailyTargets.selectAll().where { DailyTargets.userId eq userId }
            .map { it[DailyTargets.id] }
            .singleOrNull()

        if (existingId != null) {
            DailyTargets.update({ DailyTargets.id eq existingId }) {
                it[DailyTargets.waterGoalMl] = waterGoalMl
                it[DailyTargets.stepGoal] = stepGoal
                it[DailyTargets.sleepGoalHours] = sleepGoalHours
            }
        } else {
            DailyTargets.insert {
                it[id] = UUID.randomUUID()
                it[DailyTargets.userId] = userId
                it[DailyTargets.waterGoalMl] = waterGoalMl
                it[DailyTargets.stepGoal] = stepGoal
                it[DailyTargets.sleepGoalHours] = sleepGoalHours
            }
        }
    }

    // ---- Health integrations — single row per user (DB-enforced unique) ----

    fun upsertHealthIntegrations(userId: UUID, appleHealth: Boolean, googleFit: Boolean, samsungHealth: Boolean) = transaction {
        val existingId = HealthIntegrations.selectAll().where { HealthIntegrations.userId eq userId }
            .map { it[HealthIntegrations.id] }
            .singleOrNull()

        val now = Instant.now()
        if (existingId != null) {
            HealthIntegrations.update({ HealthIntegrations.id eq existingId }) {
                it[HealthIntegrations.appleHealth] = appleHealth
                it[HealthIntegrations.googleFit] = googleFit
                it[HealthIntegrations.samsungHealth] = samsungHealth
                it[HealthIntegrations.connectedAt] = now
            }
        } else {
            HealthIntegrations.insert {
                it[id] = UUID.randomUUID()
                it[HealthIntegrations.userId] = userId
                it[HealthIntegrations.appleHealth] = appleHealth
                it[HealthIntegrations.googleFit] = googleFit
                it[HealthIntegrations.samsungHealth] = samsungHealth
                it[HealthIntegrations.connectedAt] = now
            }
        }
    }
}
