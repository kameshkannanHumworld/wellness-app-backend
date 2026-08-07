package com.wellnessapp.reminders.service

import com.wellnessapp.common.exception.ForbiddenException
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.reminders.dto.ReminderRequest
import com.wellnessapp.reminders.dto.ReminderResponse
import com.wellnessapp.reminders.repository.ReminderRecord
import com.wellnessapp.reminders.repository.ReminderRepository
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Validation: `reminder_type` mirrors the varchar(30) column (non-blank, ≤30 chars) but is
 * intentionally NOT restricted to a fixed enum — reminders are freeform user-facing text
 * ("Drink water", "Take medication", "Evening walk"), unlike activity_type's closed set.
 * `repeat_type` mirrors varchar(20) and IS restricted to a small app-layer enum (see
 * ALLOWED_REPEAT_TYPES below) since the scheduler needs to know how to interpret it.
 *
 * **Known limitation, flagged explicitly rather than silently assumed:** `reminder_time` is
 * `time without time zone` — a clock time with no date and no UTC offset. Nothing in this schema
 * (not `users`, not `profiles`) stores a per-user timezone. As built, ReminderScheduler compares
 * this value against the current UTC clock time, so a reminder set for "08:00" fires at 08:00 UTC
 * for every user regardless of where they actually are. This is a real gap, not a judgment call —
 * fix it by adding a timezone column (e.g. `profiles.timezone`) before this ships to users outside
 * UTC, and have the scheduler convert accordingly.
 *
 * **`repeat_type = "weekly"` was deliberately left out** of the allowed set (unlike a typical
 * reminders app) because `reminders` has no day-of-week column — there's no way to know which day
 * a "weekly" reminder should fire on. `once`, `daily`, `weekdays`, and `weekends` are all fully
 * expressible with just a time-of-day, so those are what's supported. Add a `day_of_week` column
 * before offering "weekly" as an option.
 */
object ReminderService {

    private const val MAX_REMINDER_TYPE_LENGTH = 30
    private val ALLOWED_REPEAT_TYPES = setOf("once", "daily", "weekdays", "weekends")
    private const val DEFAULT_REPEAT_TYPE = "daily"
    private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

    fun createReminder(userId: UUID, request: ReminderRequest): ReminderResponse {
        val (time, repeatType) = validate(request)
        return ReminderRepository.insert(userId, request.reminderType.trim(), time, repeatType).toResponse()
    }

    fun listReminders(userId: UUID, enabledFilter: Boolean?): List<ReminderResponse> =
        ReminderRepository.findAllForUser(userId, enabledFilter).map { it.toResponse() }

    fun getReminder(userId: UUID, reminderId: UUID): ReminderResponse {
        val record = ReminderRepository.findById(reminderId) ?: throw NotFoundException("Reminder not found")
        requireOwnership(record, userId)
        return record.toResponse()
    }

    fun updateReminder(userId: UUID, reminderId: UUID, request: ReminderRequest): ReminderResponse {
        val (time, repeatType) = validate(request)

        val existing = ReminderRepository.findById(reminderId) ?: throw NotFoundException("Reminder not found")
        requireOwnership(existing, userId)

        val enabled = request.enabled ?: existing.enabled
        val updated = ReminderRepository.update(reminderId, request.reminderType.trim(), time, repeatType, enabled)
            ?: throw NotFoundException("Reminder not found")
        return updated.toResponse()
    }

    /** Flips `enabled`. This is the one write this module exposes that doesn't require a full body. */
    fun toggleReminder(userId: UUID, reminderId: UUID): ReminderResponse {
        val existing = ReminderRepository.findById(reminderId) ?: throw NotFoundException("Reminder not found")
        requireOwnership(existing, userId)

        val updated = ReminderRepository.setEnabled(reminderId, !existing.enabled)
            ?: throw NotFoundException("Reminder not found")
        return updated.toResponse()
    }

    fun deleteReminder(userId: UUID, reminderId: UUID) {
        val existing = ReminderRepository.findById(reminderId) ?: throw NotFoundException("Reminder not found")
        requireOwnership(existing, userId)
        ReminderRepository.delete(reminderId)
    }

    private fun requireOwnership(record: ReminderRecord, userId: UUID) {
        if (record.userId != userId) throw ForbiddenException("You do not have access to this reminder")
    }

    /** Returns (parsedTime, normalizedRepeatType), or throws ValidationException. */
    private fun validate(request: ReminderRequest): Pair<LocalTime, String> {
        val errors = mutableListOf<Pair<String, String>>()

        val trimmedType = request.reminderType.trim()
        if (trimmedType.isBlank()) {
            errors += "reminderType" to "Must not be blank"
        } else if (trimmedType.length > MAX_REMINDER_TYPE_LENGTH) {
            errors += "reminderType" to "Must be at most $MAX_REMINDER_TYPE_LENGTH characters"
        }

        val time = try {
            LocalTime.parse(request.reminderTime, TIME_FORMAT)
        } catch (e: Exception) {
            errors += "reminderTime" to "Must be in HH:mm 24-hour format (e.g. \"08:00\")"
            null
        }

        val repeatType = (request.repeatType ?: DEFAULT_REPEAT_TYPE).trim().lowercase()
        if (repeatType !in ALLOWED_REPEAT_TYPES) {
            errors += "repeatType" to "Must be one of: ${ALLOWED_REPEAT_TYPES.joinToString(", ")}"
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return (time ?: LocalTime.MIDNIGHT) to repeatType
    }

    private fun ReminderRecord.toResponse() = ReminderResponse(
        id = id.toString(),
        reminderType = reminderType,
        reminderTime = reminderTime.format(TIME_FORMAT),
        repeatType = repeatType,
        enabled = enabled,
        createdAt = createdAt.toString()
    )
}
