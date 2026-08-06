package com.wellnessapp.onboarding.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * Step 1 of onboarding — basic profile info.
 * Note: `user_id` has no UNIQUE constraint at the DB level (unlike DailyTargets/HealthIntegrations
 * below), so "one profile per user" is enforced here at the application layer via upsert logic
 * in OnboardingRepository. Consider adding a UNIQUE constraint on profiles.user_id for
 * defense-in-depth if this table should always be strictly 1:1 with users.
 */
object Profiles : Table("profiles") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val age = integer("age").nullable()
    val gender = varchar("gender", 20).nullable()
    val heightCm = decimal("height_cm", 5, 2).nullable()
    val weightKg = decimal("weight_kg", 5, 2).nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Step 2 of onboarding — one row per selected goal_type; the full set is replaced on each save. */
object Goals : Table("goals") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val goalType = varchar("goal_type", 50).nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Step 3 of onboarding — single row per user (user_id is UNIQUE in the DB). */
object DailyTargets : Table("daily_targets") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").uniqueIndex()
    val waterGoalMl = integer("water_goal_ml").nullable()
    val stepGoal = integer("step_goal").nullable()
    val sleepGoalHours = decimal("sleep_goal_hours", 4, 2).nullable()

    override val primaryKey = PrimaryKey(id)
}

/** Step 4 of onboarding — single row per user (user_id is UNIQUE in the DB). */
object HealthIntegrations : Table("health_integrations") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id").uniqueIndex()
    val appleHealth = bool("apple_health").default(false)
    val googleFit = bool("google_fit").default(false)
    val samsungHealth = bool("samsung_health").default(false)
    val connectedAt = timestamp("connected_at").nullable()

    override val primaryKey = PrimaryKey(id)
}
