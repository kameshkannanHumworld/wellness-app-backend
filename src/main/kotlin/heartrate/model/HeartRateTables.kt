package com.wellnessapp.heartrate.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * One row per heart-rate reading. Mirrors the blood_pressure_logs pattern.
 *   - bpm BETWEEN 30 AND 220 (CHECK constraint already enforced in Postgres; same range as the
 *     existing `pulse` column on blood_pressure_logs)
 *   - source IN ('manual', 'health_connect', 'ble') — defaults to 'manual'. Added ahead of the
 *     Health Connect sync and BLE pulse-oximeter integrations from the WellSync AI spec so this
 *     table doesn't need a schema change once those land; app-layer judgment call, not yet backed
 *     by any real ingestion pathway other than manual entry.
 */
object HeartRateLogs : Table("heart_rate_logs") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val bpm = integer("bpm")

    val sourceType = varchar("source", 20)

    val measuredAt = timestamp("measured_at")

    override val primaryKey = PrimaryKey(id)
}
