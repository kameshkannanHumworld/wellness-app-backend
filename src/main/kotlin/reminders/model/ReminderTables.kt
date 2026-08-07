package com.wellnessapp.reminders.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.time
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * One row per reminder. Matches the live `reminders` table exactly (verified via
 * information_schema before writing this module):
 *   - reminder_type varchar(30), NOT NULL, no CHECK constraint — freeform label chosen by the
 *     app layer (see ReminderService.ALLOWED_REPEAT_TYPES for repeat_type; reminder_type itself
 *     is intentionally NOT restricted to an enum, unlike activity_type, since reminders are
 *     naturally freeform user-facing text like "Drink water" or "Take medication").
 *   - reminder_time "time without time zone" — stores a clock time with NO date and NO timezone.
 *     See ReminderService's kdoc for the resulting timezone limitation.
 *   - repeat_type varchar(20), default 'daily', no CHECK constraint — app layer restricts to
 *     once/daily/weekdays/weekends (see ReminderService — "weekly" was deliberately left out,
 *     the schema has no day-of-week column to support it honestly).
 *   - enabled boolean, default true.
 */
object Reminders : Table("reminders") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val reminderType = varchar("reminder_type", 30)
    val reminderTime = time("reminder_time")
    val repeatType = varchar("repeat_type", 20)
    val enabled = bool("enabled")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
