package com.wellnessapp.bloodpressure.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * One row per blood-pressure reading. All three CHECK constraints below are already enforced in
 * Postgres — BloodPressureService re-validates the same ranges so bad input returns a clean 400
 * VALIDATION_ERROR instead of a raw DB constraint error:
 *   - systolic BETWEEN 70 AND 250
 *   - diastolic BETWEEN 40 AND 150
 *   - pulse BETWEEN 30 AND 220
 *   - systolic > diastolic
 */
object BloodPressureLogs : Table("blood_pressure_logs") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val systolic = integer("systolic")
    val diastolic = integer("diastolic")
    val pulse = integer("pulse").nullable()
    val notes = text("notes").nullable()
    val measuredAt = timestamp("measured_at")

    override val primaryKey = PrimaryKey(id)
}
