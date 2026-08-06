package com.wellnessapp.activity.service

import com.wellnessapp.activity.dto.ActivityDayTotal
import com.wellnessapp.activity.dto.ActivityLogRequest
import com.wellnessapp.activity.dto.ActivityLogResponse
import com.wellnessapp.activity.dto.ActivityWeeklyResponse
import com.wellnessapp.activity.repository.ActivityRecord
import com.wellnessapp.activity.repository.ActivityRepository
import com.wellnessapp.common.exception.ForbiddenException
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.exception.ValidationException
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * `activities` has no CHECK constraints beyond `activity_type varchar(20)` (verified live in Postgres
 * before writing this module) — the ranges below are app-layer defensive bounds, not mirrored DB
 * constraints like in onboarding/hydration. Revisit alongside real product requirements once the
 * Android app's Activity screen is actually built.
 */
object ActivityService {

    private val ALLOWED_TYPES = setOf("steps", "workout", "sleep", "movement")

    private val VALUE_RANGES = mapOf(
        "steps" to (0.0 to 100000.0),
        "workout" to (0.0 to 10000.0),
        "sleep" to (0.0 to 24.0),
        "movement" to (0.0 to 10000.0)
    )

    fun logActivity(userId: UUID, request: ActivityLogRequest): ActivityLogResponse {
        val (type, errors) = validate(request)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val record = ActivityRepository.insert(
            userId = userId,
            activityType = type,
            value = request.value?.toScaledBigDecimal(),
            durationMinutes = request.durationMinutes,
            calories = request.calories
        )
        return record.toResponse()
    }

    fun listActivities(userId: UUID, type: String?, date: String?, limit: Int): List<ActivityLogResponse> {
        val normalizedType = type?.trim()?.lowercase()?.also {
            if (it !in ALLOWED_TYPES) {
                throw ValidationException(listOf("type" to "Must be one of: ${ALLOWED_TYPES.joinToString(", ")}"))
            }
        }
        val onDate = date?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw ValidationException(listOf("date" to "Must be in YYYY-MM-DD format"))
            }
        }
        return ActivityRepository.findAllForUser(userId, normalizedType, onDate, limit).map { it.toResponse() }
    }

    fun getActivity(userId: UUID, activityId: UUID): ActivityLogResponse {
        val record = ActivityRepository.findById(activityId) ?: throw NotFoundException("Activity log not found")
        requireOwnership(record, userId)
        return record.toResponse()
    }

    fun updateActivity(userId: UUID, activityId: UUID, request: ActivityLogRequest): ActivityLogResponse {
        val (type, errors) = validate(request)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val existing = ActivityRepository.findById(activityId) ?: throw NotFoundException("Activity log not found")
        requireOwnership(existing, userId)

        val updated = ActivityRepository.update(
            id = activityId,
            activityType = type,
            value = request.value?.toScaledBigDecimal(),
            durationMinutes = request.durationMinutes,
            calories = request.calories
        ) ?: throw NotFoundException("Activity log not found")

        return updated.toResponse()
    }

    fun deleteActivity(userId: UUID, activityId: UUID) {
        val existing = ActivityRepository.findById(activityId) ?: throw NotFoundException("Activity log not found")
        requireOwnership(existing, userId)
        ActivityRepository.delete(activityId)
    }

    /** Last 7 days (today + previous 6), oldest first, zero-filled per type — ready for the activity chart screen. */
    fun weeklySummary(userId: UUID): ActivityWeeklyResponse {
        val records = ActivityRepository.findLastSevenDays(userId)
        val byDay = records.groupBy { it.loggedAt.atZone(ZoneOffset.UTC).toLocalDate() }

        val today = LocalDate.now(ZoneOffset.UTC)
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayRecords = byDay[date] ?: emptyList()

            ActivityDayTotal(
                date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                steps = dayRecords.sumValueFor("steps"),
                workout = dayRecords.sumValueFor("workout"),
                sleep = dayRecords.sumValueFor("sleep"),
                movement = dayRecords.sumValueFor("movement"),
                caloriesBurned = dayRecords.sumOf { it.calories ?: 0 }
            )
        }

        return ActivityWeeklyResponse(days)
    }

    private fun List<ActivityRecord>.sumValueFor(type: String): Double =
        filter { it.activityType == type }.sumOf { it.value?.toDouble() ?: 0.0 }

    private fun requireOwnership(record: ActivityRecord, userId: UUID) {
        if (record.userId != userId) throw ForbiddenException("You do not have access to this log")
    }

    private fun validate(request: ActivityLogRequest): Pair<String, List<Pair<String, String>>> {
        val errors = mutableListOf<Pair<String, String>>()
        val type = request.activityType.trim().lowercase()

        if (type !in ALLOWED_TYPES) {
            errors += "activityType" to "Must be one of: ${ALLOWED_TYPES.joinToString(", ")}"
        }
        if (request.value == null && request.durationMinutes == null && request.calories == null) {
            errors += "value" to "Provide at least one of value, durationMinutes, or calories"
        }
        if (request.value != null) {
            val range = VALUE_RANGES[type]
            if (range != null && (request.value < range.first || request.value > range.second)) {
                errors += "value" to "Must be between ${range.first} and ${range.second} for \"$type\""
            }
        }
        if (request.durationMinutes != null && (request.durationMinutes < 0 || request.durationMinutes > 1440)) {
            errors += "durationMinutes" to "Must be between 0 and 1440 minutes"
        }
        if (request.calories != null && (request.calories < 0 || request.calories > 20000)) {
            errors += "calories" to "Must be between 0 and 20000"
        }

        return type to errors
    }

    private fun Double.toScaledBigDecimal(): BigDecimal = BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP)

    private fun ActivityRecord.toResponse() = ActivityLogResponse(
        id = id.toString(),
        activityType = activityType,
        value = value?.toDouble(),
        durationMinutes = durationMinutes,
        calories = calories,
        loggedAt = loggedAt.toString()
    )
}
