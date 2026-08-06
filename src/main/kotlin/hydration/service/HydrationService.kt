package com.wellnessapp.hydration.service

import com.wellnessapp.common.exception.ForbiddenException
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.hydration.dto.WaterDayTotal
import com.wellnessapp.hydration.dto.WaterLogRequest
import com.wellnessapp.hydration.dto.WaterLogResponse
import com.wellnessapp.hydration.dto.WaterWeeklyResponse
import com.wellnessapp.hydration.repository.HydrationRepository
import com.wellnessapp.hydration.repository.WaterLogRecord
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Validation range mirrors the water_logs_amount_ml_check CHECK constraint already
 * enforced in Postgres (50–5000 ml) so bad input returns a clean 400 VALIDATION_ERROR
 * instead of a raw DB constraint error.
 */
object HydrationService {

    private const val MIN_ML = 50
    private const val MAX_ML = 5000

    fun logWater(userId: UUID, request: WaterLogRequest): WaterLogResponse {
        validateAmount(request.amountMl)
        return HydrationRepository.insert(userId, request.amountMl).toResponse()
    }

    fun listLogs(userId: UUID, date: String?, limit: Int): List<WaterLogResponse> {
        val onDate = date?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw ValidationException(listOf("date" to "Must be in YYYY-MM-DD format"))
            }
        }
        return HydrationRepository.findAllForUser(userId, onDate, limit).map { it.toResponse() }
    }

    fun getLog(userId: UUID, logId: UUID): WaterLogResponse {
        val record = HydrationRepository.findById(logId) ?: throw NotFoundException("Water log not found")
        requireOwnership(record, userId)
        return record.toResponse()
    }

    fun updateLog(userId: UUID, logId: UUID, request: WaterLogRequest): WaterLogResponse {
        validateAmount(request.amountMl)

        val existing = HydrationRepository.findById(logId) ?: throw NotFoundException("Water log not found")
        requireOwnership(existing, userId)

        val updated = HydrationRepository.update(logId, request.amountMl)
            ?: throw NotFoundException("Water log not found")
        return updated.toResponse()
    }

    fun deleteLog(userId: UUID, logId: UUID) {
        val existing = HydrationRepository.findById(logId) ?: throw NotFoundException("Water log not found")
        requireOwnership(existing, userId)
        HydrationRepository.delete(logId)
    }

    /** Last 7 days (today + previous 6), oldest first, zero-filled for days with no logs — ready for the chart screen. */
    fun weeklySummary(userId: UUID): WaterWeeklyResponse {
        val totals = HydrationRepository.weeklyTotals(userId)
        val goal = HydrationRepository.dailyGoalMl(userId)

        val today = LocalDate.now(ZoneOffset.UTC)
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            WaterDayTotal(date.format(DateTimeFormatter.ISO_LOCAL_DATE), totals[date] ?: 0)
        }

        return WaterWeeklyResponse(days = days, dailyGoalMl = goal)
    }

    private fun requireOwnership(record: WaterLogRecord, userId: UUID) {
        if (record.userId != userId) throw ForbiddenException("You do not have access to this log")
    }

    private fun validateAmount(amountMl: Int) {
        if (amountMl < MIN_ML || amountMl > MAX_ML) {
            throw ValidationException(listOf("amountMl" to "Must be between $MIN_ML and $MAX_ML ml"))
        }
    }

    private fun WaterLogRecord.toResponse() = WaterLogResponse(
        id = id.toString(),
        amountMl = amountMl,
        createdAt = createdAt.toString()
    )
}
