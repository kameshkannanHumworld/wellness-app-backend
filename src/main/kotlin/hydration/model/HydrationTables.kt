package com.wellnessapp.hydration.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * One row per water-intake log entry (not one row per user — this is a log, so users
 * accumulate many rows over time). CHECK (amount_ml BETWEEN 50 AND 5000) is already
 * enforced in Postgres; HydrationService re-validates the same range so bad input gets
 * a clean 400 VALIDATION_ERROR instead of a raw DB constraint error.
 */
object WaterLogs : Table("water_logs") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val amountMl = integer("amount_ml")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
