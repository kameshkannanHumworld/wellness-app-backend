package com.wellnessapp.activity.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * One row per activity log entry — steps / workout / sleep / movement (see ActivityService.ALLOWED_TYPES).
 * `value`'s meaning depends on `activity_type`: step count, workout distance/reps, sleep hours, or movement units.
 *
 * Unlike water_logs, this table has no CHECK constraints beyond `activity_type varchar(20)` — the validation
 * ranges enforced in ActivityService are app-layer defensive bounds, not mirrored DB constraints. Revisit
 * once the Android app's Activity screen defines real product requirements for each type.
 */
object Activities : Table("activities") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val activityType = varchar("activity_type", 20)
    val value = decimal("value", 10, 2).nullable()
    val durationMinutes = integer("duration_minutes").nullable()
    val calories = integer("calories").nullable()
    val loggedAt = timestamp("logged_at")

    override val primaryKey = PrimaryKey(id)
}
