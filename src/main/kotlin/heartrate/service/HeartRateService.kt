package com.wellnessapp.heartrate.service

import com.wellnessapp.common.exception.ForbiddenException
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.heartrate.dto.HeartRateDailyResponse
import com.wellnessapp.heartrate.dto.HeartRateDayStat
import com.wellnessapp.heartrate.dto.HeartRateLogRequest
import com.wellnessapp.heartrate.dto.HeartRateLogResponse
import com.wellnessapp.heartrate.dto.HeartRateMonthStat
import com.wellnessapp.heartrate.dto.HeartRateYearlyResponse
import com.wellnessapp.heartrate.repository.HeartRateRecord
import com.wellnessapp.heartrate.repository.HeartRateRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Validation mirrors the heart_rate_logs CHECK constraint already enforced in Postgres (bpm 30-220),
 * so bad input returns a clean 400 VALIDATION_ERROR instead of a raw DB constraint error.
 *
 * Daily/Weekly/Monthly/Yearly views (per the WellSync AI spec): "Daily" is served by GET / with
 * ?date=, "Weekly" is the last 7 days per-day, "Monthly" is the last 30 days per-day (same shape as
 * weekly, just more entries — a judgment call in the absence of a stricter product spec for what
 * "monthly" should bucket by), and "Yearly" is the last 12 calendar months per-month.
 */
object HeartRateService {

    private const val MIN_BPM = 30
    private const val MAX_BPM = 220
    private val ALLOWED_SOURCES = setOf("manual", "health_connect", "ble")
    private const val DEFAULT_SOURCE = "manual"

    fun logReading(userId: UUID, request: HeartRateLogRequest): HeartRateLogResponse {
        val source = validate(request)
        return HeartRateRepository.insert(userId, request.bpm, source).toResponse()
    }

    fun listReadings(userId: UUID, date: String?, limit: Int): List<HeartRateLogResponse> {
        val onDate = parseDate(date)
        return HeartRateRepository.findAllForUser(userId, onDate, limit).map { it.toResponse() }
    }

    fun getReading(userId: UUID, readingId: UUID): HeartRateLogResponse {
        val record = HeartRateRepository.findById(readingId) ?: throw NotFoundException("Heart rate reading not found")
        requireOwnership(record, userId)
        return record.toResponse()
    }

    fun getLatest(userId: UUID): HeartRateLogResponse {
        val record = HeartRateRepository.findLatestForUser(userId)
            ?: throw NotFoundException("No heart rate readings recorded yet")
        return record.toResponse()
    }

    fun updateReading(userId: UUID, readingId: UUID, request: HeartRateLogRequest): HeartRateLogResponse {
        val source = validate(request)

        val existing = HeartRateRepository.findById(readingId) ?: throw NotFoundException("Heart rate reading not found")
        requireOwnership(existing, userId)

        val updated = HeartRateRepository.update(readingId, request.bpm, source)
            ?: throw NotFoundException("Heart rate reading not found")
        return updated.toResponse()
    }

    fun deleteReading(userId: UUID, readingId: UUID) {
        val existing = HeartRateRepository.findById(readingId) ?: throw NotFoundException("Heart rate reading not found")
        requireOwnership(existing, userId)
        HeartRateRepository.delete(readingId)
    }

    /** Last 7 days (today + previous 6), oldest first, per-day avg/min/max — feeds the Weekly chart. */
    fun weeklySummary(userId: UUID): HeartRateDailyResponse = dailySummary(userId, 7)

    /** Last 30 days, oldest first, per-day avg/min/max — feeds the Monthly chart. */
    fun monthlySummary(userId: UUID): HeartRateDailyResponse = dailySummary(userId, 30)

    /** Last 12 calendar months (UTC), oldest first, per-month avg/min/max — feeds the Yearly chart. */
    fun yearlySummary(userId: UUID): HeartRateYearlyResponse {
        val records = HeartRateRepository.findLastTwelveMonths(userId)
        val byMonth = records.groupBy { YearMonth.from(it.measuredAt.atZone(ZoneOffset.UTC)) }

        val thisMonth = YearMonth.now(ZoneOffset.UTC)
        val months = (11 downTo 0).map { offset ->
            val month = thisMonth.minusMonths(offset.toLong())
            val monthRecords = byMonth[month] ?: emptyList()

            if (monthRecords.isEmpty()) {
                HeartRateMonthStat(month = month.toString())
            } else {
                HeartRateMonthStat(
                    month = month.toString(),
                    avgBpm = monthRecords.map { it.bpm }.average(),
                    minBpm = monthRecords.minOf { it.bpm },
                    maxBpm = monthRecords.maxOf { it.bpm },
                    readingCount = monthRecords.size
                )
            }
        }

        return HeartRateYearlyResponse(months)
    }

    private fun dailySummary(userId: UUID, days: Int): HeartRateDailyResponse {
        val records = HeartRateRepository.findLastNDays(userId, days)
        val byDay = records.groupBy { it.measuredAt.atZone(ZoneOffset.UTC).toLocalDate() }

        val today = LocalDate.now(ZoneOffset.UTC)
        val result = ((days - 1) downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayRecords = byDay[date] ?: emptyList()

            if (dayRecords.isEmpty()) {
                HeartRateDayStat(date = date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            } else {
                HeartRateDayStat(
                    date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    avgBpm = dayRecords.map { it.bpm }.average(),
                    minBpm = dayRecords.minOf { it.bpm },
                    maxBpm = dayRecords.maxOf { it.bpm },
                    readingCount = dayRecords.size
                )
            }
        }

        return HeartRateDailyResponse(result)
    }

    private fun parseDate(date: String?): LocalDate? = date?.let {
        try {
            LocalDate.parse(it)
        } catch (e: Exception) {
            throw ValidationException(listOf("date" to "Must be in YYYY-MM-DD format"))
        }
    }

    private fun requireOwnership(record: HeartRateRecord, userId: UUID) {
        if (record.userId != userId) throw ForbiddenException("You do not have access to this reading")
    }

    /** Returns the normalized (lowercased, defaulted) source string, or throws if invalid. */
    private fun validate(request: HeartRateLogRequest): String {
        val errors = mutableListOf<Pair<String, String>>()

        if (request.bpm < MIN_BPM || request.bpm > MAX_BPM) {
            errors += "bpm" to "Must be between $MIN_BPM and $MAX_BPM"
        }
        val source = (request.source ?: DEFAULT_SOURCE).trim().lowercase()
        if (source !in ALLOWED_SOURCES) {
            errors += "source" to "Must be one of: ${ALLOWED_SOURCES.joinToString(", ")}"
        }

        if (errors.isNotEmpty()) throw ValidationException(errors)
        return source
    }

    private fun HeartRateRecord.toResponse() = HeartRateLogResponse(
        id = id.toString(),
        bpm = bpm,
        source = source,
        measuredAt = measuredAt.toString()
    )
}
