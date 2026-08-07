package com.wellnessapp.spo2.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * One row per SpO2 (blood oxygen saturation) reading.
 *   - spo2Percentage BETWEEN 50 AND 100 (CHECK constraint already enforced in Postgres) — readings
 *     below ~70% are clinically critical and readings below 50% are almost certainly sensor error;
 *     50 was chosen as a defensive lower bound, not a mirrored clinical threshold.
 *   - source IN ('manual', 'health_connect', 'ble') — same forward-looking rationale as
 *     heart_rate_logs (see HeartRateTables.kt); a BLE pulse oximeter reports both SpO2 and heart rate.
 */
object Spo2Logs : Table("spo2_logs") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val spo2Percentage = integer("spo2_percentage")
    val sourceType = varchar("source", 20)
    val measuredAt = timestamp("measured_at")

    override val primaryKey = PrimaryKey(id)
}
