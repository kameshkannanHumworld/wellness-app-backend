package com.wellnessapp.spo2.service

import com.wellnessapp.common.exception.ForbiddenException
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.spo2.dto.Spo2DailyResponse
import com.wellnessapp.spo2.dto.Spo2DayStat
import com.wellnessapp.spo2.dto.Spo2LogRequest
import com.wellnessapp.spo2.dto.Spo2LogResponse
import com.wellnessapp.spo2.dto.Spo2MonthStat
import com.wellnessapp.spo2.dto.Spo2YearlyResponse
import com.wellnessapp.spo2.repository.Spo2Record
import com.wellnessapp.spo2.repository.Spo2Repository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Validation mirrors the spo2_logs CHECK constraint already enforced in Postgres (spo2Percentage
 * 50-100), so bad input returns a clean 400 VALIDATION_ERROR instead of a raw DB constraint error.
 *
 * Daily/Weekly/Monthly/Yearly views (per the WellSync AI spec): "Daily" is served by GET / with
 * ?date=, "Weekly" is the last 7 days per-day, "Monthly" is the last 30 days per-day (same shape as
 * weekly, just more entries), and "Yearly" is the last 12 calendar months per-month — identical
 * structure to the Heart Rate module for consistency across the two vital-sign endpoints.
 */
object Spo2Service {

    private const val MIN_SPO2 = 50
    private const val MAX_SPO2 = 100
    private val ALLOWED_SOURCES = setOf("manual", "health_connect", "ble")
    private const val DEFAULT_SOURCE = "manual"

    fun logReading(userId: UUID, request: Spo2LogRequest): Spo2LogResponse {
        val source = validate(request)
        return Spo2Repository.insert(userId, request.spo2Percentage, source).toResponse()
    }

    fun listReadings(userId: UUID, date: String?, limit: Int): List<Spo2LogResponse> {
        val onDate = parseDate(date)
        return Spo2Repository.findAllForUser(userId, onDate, limit).map { it.toResponse() }
    }

    fun getReading(userId: UUID, readingId: UUID): Spo2LogResponse {
        val record = Spo2Repository.findById(readingId) ?: throw NotFoundException("SpO2 reading not found")
        requireOwnership(record, userId)
        return record.toResponse()
    }

    fun getLatest(userId: UUID): Spo2LogResponse {
        val record = Spo2Repository.findLatestForUser(userId)
            ?: throw NotFoundException("No SpO2 readings recorded yet")
        return record.toResponse()
    }

    fun updateReading(userId: UUID, readingId: UUID, request: Spo2LogRequest): Spo2LogResponse {
        val source = validate(request)

        val existing = Spo2Repository.findById(readingId) ?: throw NotFoundException("SpO2 reading not found")
        requireOwnership(existing, userId)

        val updated = Spo2Repository.update(readingId, request.spo2Percentage, source)
            ?: throw NotFoundException("SpO2 reading not found")
        return updated.toResponse()
    }

    fun deleteReading(userId: UUID, readingId: UUID) {
        val existing = Spo2Repository.findById(readingId) ?: throw NotFoundException("SpO2 reading not found")
        requireOwnership(existing, userId)
        Spo2Repository.delete(readingId)
    }

    /** Last 7 days (today + previous 6), oldest first, per-day avg/min/max — feeds the Weekly chart. */
    fun weeklySummary(userId: UUID): Spo2DailyResponse = dailySummary(userId, 7)

    /** Last 30 days, oldest first, per-day avg/min/max — feeds the Monthly chart. */
    fun monthlySummary(userId: UUID): Spo2DailyResponse = dailySummary(userId, 30)

    /** Last 12 calendar months (UTC), oldest first, per-month avg/min/max — feeds the Yearly chart. */
    fun yearlySummary(userId: UUID): Spo2YearlyResponse {
        val records = Spo2Repository.findLastTwelveMonths(userId)
        val byMonth = records.groupBy { YearMonth.from(it.measuredAt.atZone(ZoneOffset.UTC)) }

        val thisMonth = YearMonth.now(ZoneOffset.UTC)
        val months = (11 downTo 0).map { offset ->
            val month = thisMonth.minusMonths(offset.toLong())
            val monthRecords = byMonth[month] ?: emptyList()

            if (monthRecords.isEmpty()) {
                Spo2MonthStat(month = month.toString())
            } else {
                Spo2MonthStat(
                    month = month.toString(),
                    avgSpo2 = monthRecords.map { it.spo2Percentage }.average(),
                    minSpo2 = monthRecords.minOf { it.spo2Percentage },
                    maxSpo2 = monthRecords.maxOf { it.spo2Percentage },
                    readingCount = monthRecords.size
                )
            }
        }

        return Spo2YearlyResponse(months)
    }

    private fun dailySummary(userId: UUID, days: Int): Spo2DailyResponse {
        val records = Spo2Repository.findLastNDays(userId, days)
        val byDay = records.groupBy { it.measuredAt.atZone(ZoneOffset.UTC).toLocalDate() }

        val today = LocalDate.now(ZoneOffset.UTC)
        val result = ((days - 1) downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayRecords = byDay[date] ?: emptyList()

            if (dayRecords.isEmpty()) {
                Spo2DayStat(date = date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            } else {
                Spo2DayStat(
                    date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    avgSpo2 = dayRecords.map { it.spo2Percentage }.average(),
                    minSpo2 = dayRecords.minOf { it.spo2Percentage },
                    maxSpo2 = dayRecords.maxOf { it.spo2Percentage },
                    readingCount = dayRecords.size
                )
            }
        }

        return Spo2DailyResponse(result)
    }

    private fun parseDate(date: String?): LocalDate? = date?.let {
        try {
            LocalDate.parse(it)
        } catch (e: Exception) {
            throw ValidationException(listOf("date" to "Must be in YYYY-MM-DD format"))
        }
    }

    private fun requireOwnership(record: Spo2Record, userId: UUID) {
        if (record.userId != userId) throw ForbiddenException("You do not have access to this reading")
    }

    /** Returns the normalized (lowercased, defaulted) source string, or throws if invalid. */
    private fun validate(request: Spo2LogRequest): String {
        val errors = mutableListOf<Pair<String, String>>()

        if (request.spo2Percentage < MIN_SPO2 || request.spo2Percentage > MAX_SPO2) {
            errors += "spo2Percentage" to "Must be between $MIN_SPO2 and $MAX_SPO2"
        }
        val source = (request.source ?: DEFAULT_SOURCE).trim().lowercase()
        if (source !in ALLOWED_SOURCES) {
            errors += "source" to "Must be one of: ${ALLOWED_SOURCES.joinToString(", ")}"
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return source
    }

    private fun Spo2Record.toResponse() = Spo2LogResponse(
        id = id.toString(),
        spo2Percentage = spo2Percentage,
        source = source,
        measuredAt = measuredAt.toString()
    )
}
