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

data class ProfileRecord(
    val id: UUID,
    val userId: UUID,
    val age: Int?,
    val gender: String?,
    val heightCm: BigDecimal?,
    val weightKg: BigDecimal?,
    val stressLevel: String?
)

object OnboardingRepository {

    // ---- Profile (basic info) — upserted at the app layer; see note in OnboardingTables.kt ----

    fun findProfile(userId: UUID): ProfileRecord? = transaction {
        Profiles.selectAll().where { Profiles.userId eq userId }
            .map { it.toProfileRecord() }
            .singleOrNull()
    }

    /**
     * `stressLevel` is nullable in the request (see BasicInfoRequest) — when omitted, this
     * preserves whatever stress level was previously saved rather than wiping it to null, since
     * age/gender/height/weight are always fully replaced but stressLevel may legitimately be set
     * once and not resent on every subsequent basic-info save.
     */
    fun upsertProfile(userId: UUID, age: Int, gender: String, heightCm: BigDecimal, weightKg: BigDecimal, stressLevel: String?): ProfileRecord = transaction {
        val existing = Profiles.selectAll().where { Profiles.userId eq userId }
            .map { it.toProfileRecord() }
            .singleOrNull()

        val resolvedStressLevel = stressLevel ?: existing?.stressLevel
        val profileId = existing?.id ?: UUID.randomUUID()

        if (existing != null) {
            Profiles.update({ Profiles.id eq existing.id }) {
                it[Profiles.age] = age
                it[Profiles.gender] = gender
                it[Profiles.heightCm] = heightCm
                it[Profiles.weightKg] = weightKg
                it[Profiles.stressLevel] = resolvedStressLevel
            }
        } else {
            Profiles.insert {
                it[id] = profileId
                it[Profiles.userId] = userId
                it[Profiles.age] = age
                it[Profiles.gender] = gender
                it[Profiles.heightCm] = heightCm
                it[Profiles.weightKg] = weightKg
                it[Profiles.stressLevel] = resolvedStressLevel
            }
        }

        ProfileRecord(profileId, userId, age, gender, heightCm, weightKg, resolvedStressLevel)
    }

    private fun ResultRow.toProfileRecord() = ProfileRecord(
        id = this[Profiles.id],
        userId = this[Profiles.userId],
        age = this[Profiles.age],
        gender = this[Profiles.gender],
        heightCm = this[Profiles.heightCm],
        weightKg = this[Profiles.weightKg],
        stressLevel = this[Profiles.stressLevel]
    )

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
