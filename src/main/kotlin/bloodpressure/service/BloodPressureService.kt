package com.wellnessapp.bloodpressure.service

import com.wellnessapp.bloodpressure.dto.BloodPressureDayAverage
import com.wellnessapp.bloodpressure.dto.BloodPressureLogRequest
import com.wellnessapp.bloodpressure.dto.BloodPressureLogResponse
import com.wellnessapp.bloodpressure.dto.BloodPressureWeeklyResponse
import com.wellnessapp.bloodpressure.repository.BloodPressureRecord
import com.wellnessapp.bloodpressure.repository.BloodPressureRepository
import com.wellnessapp.common.exception.ForbiddenException
import com.wellnessapp.common.exception.NotFoundException
import com.wellnessapp.common.exception.ValidationException
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Validation ranges mirror the CHECK constraints already enforced in Postgres (systolic 70–250,
 * diastolic 40–150, pulse 30–220, systolic > diastolic) so bad input returns a clean 400
 * VALIDATION_ERROR instead of a raw DB constraint error. `notes` has no DB length limit (it's
 * `text`) — the 500-char cap here is an app-layer defensive bound, not a mirrored constraint.
 */
object BloodPressureService {

    private const val MAX_NOTES_LENGTH = 500

    fun logReading(userId: UUID, request: BloodPressureLogRequest): BloodPressureLogResponse {
        val errors = validate(request)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val record = BloodPressureRepository.insert(
            userId = userId,
            systolic = request.systolic,
            diastolic = request.diastolic,
            pulse = request.pulse,
            notes = request.notes?.trim()?.takeIf { it.isNotBlank() }
        )
        return record.toResponse()
    }

    fun listReadings(userId: UUID, date: String?, limit: Int): List<BloodPressureLogResponse> {
        val onDate = date?.let {
            try {
                LocalDate.parse(it)
            } catch (e: Exception) {
                throw ValidationException(listOf("date" to "Must be in YYYY-MM-DD format"))
            }
        }
        return BloodPressureRepository.findAllForUser(userId, onDate, limit).map { it.toResponse() }
    }

    fun getReading(userId: UUID, readingId: UUID): BloodPressureLogResponse {
        val record = BloodPressureRepository.findById(readingId) ?: throw NotFoundException("Blood pressure reading not found")
        requireOwnership(record, userId)
        return record.toResponse()
    }

    fun getLatest(userId: UUID): BloodPressureLogResponse {
        val record = BloodPressureRepository.findLatestForUser(userId)
            ?: throw NotFoundException("No blood pressure readings recorded yet")
        return record.toResponse()
    }

    fun updateReading(userId: UUID, readingId: UUID, request: BloodPressureLogRequest): BloodPressureLogResponse {
        val errors = validate(request)
        if (errors.isNotEmpty()) throw ValidationException(errors)

        val existing = BloodPressureRepository.findById(readingId) ?: throw NotFoundException("Blood pressure reading not found")
        requireOwnership(existing, userId)

        val updated = BloodPressureRepository.update(
            id = readingId,
            systolic = request.systolic,
            diastolic = request.diastolic,
            pulse = request.pulse,
            notes = request.notes?.trim()?.takeIf { it.isNotBlank() }
        ) ?: throw NotFoundException("Blood pressure reading not found")

        return updated.toResponse()
    }

    fun deleteReading(userId: UUID, readingId: UUID) {
        val existing = BloodPressureRepository.findById(readingId) ?: throw NotFoundException("Blood pressure reading not found")
        requireOwnership(existing, userId)
        BloodPressureRepository.delete(readingId)
    }

    /** Last 7 days (today + previous 6), oldest first, per-day averages — ready for the BP chart screen. */
    fun weeklySummary(userId: UUID): BloodPressureWeeklyResponse {
        val records = BloodPressureRepository.findLastSevenDays(userId)
        val byDay = records.groupBy { it.measuredAt.atZone(ZoneOffset.UTC).toLocalDate() }

        val today = LocalDate.now(ZoneOffset.UTC)
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val dayRecords = byDay[date] ?: emptyList()

            if (dayRecords.isEmpty()) {
                BloodPressureDayAverage(date = date.format(DateTimeFormatter.ISO_LOCAL_DATE))
            } else {
                BloodPressureDayAverage(
                    date = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                    avgSystolic = dayRecords.map { it.systolic }.average(),
                    avgDiastolic = dayRecords.map { it.diastolic }.average(),
                    avgPulse = dayRecords.mapNotNull { it.pulse }.takeIf { it.isNotEmpty() }?.average(),
                    readingCount = dayRecords.size
                )
            }
        }

        return BloodPressureWeeklyResponse(days)
    }

    private fun requireOwnership(record: BloodPressureRecord, userId: UUID) {
        if (record.userId != userId) throw ForbiddenException("You do not have access to this reading")
    }

    private fun validate(request: BloodPressureLogRequest): List<Pair<String, String>> {
        val errors = mutableListOf<Pair<String, String>>()

        if (request.systolic < 70 || request.systolic > 250) errors += "systolic" to "Must be between 70 and 250"
        if (request.diastolic < 40 || request.diastolic > 150) errors += "diastolic" to "Must be between 40 and 150"
        if (request.pulse != null && (request.pulse < 30 || request.pulse > 220)) errors += "pulse" to "Must be between 30 and 220"
        if (request.systolic <= request.diastolic) errors += "systolic" to "Must be greater than diastolic"
        if ((request.notes?.length ?: 0) > MAX_NOTES_LENGTH) errors += "notes" to "Must be at most $MAX_NOTES_LENGTH characters"

        return errors
    }

    private fun BloodPressureRecord.toResponse() = BloodPressureLogResponse(
        id = id.toString(),
        systolic = systolic,
        diastolic = diastolic,
        pulse = pulse,
        notes = notes,
        measuredAt = measuredAt.toString()
    )
}
