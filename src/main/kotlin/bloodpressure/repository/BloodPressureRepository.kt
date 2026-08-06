package com.wellnessapp.bloodpressure.repository

import com.wellnessapp.bloodpressure.model.BloodPressureLogs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class BloodPressureRecord(
    val id: UUID,
    val userId: UUID,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val notes: String?,
    val measuredAt: Instant
)

object BloodPressureRepository {

    fun insert(userId: UUID, systolic: Int, diastolic: Int, pulse: Int?, notes: String?): BloodPressureRecord = transaction {
        val newId = UUID.randomUUID()
        val now = Instant.now()
        BloodPressureLogs.insert {
            it[id] = newId
            it[BloodPressureLogs.userId] = userId
            it[BloodPressureLogs.systolic] = systolic
            it[BloodPressureLogs.diastolic] = diastolic
            it[BloodPressureLogs.pulse] = pulse
            it[BloodPressureLogs.notes] = notes
            it[measuredAt] = now
        }
        BloodPressureRecord(newId, userId, systolic, diastolic, pulse, notes, now)
    }

    fun findById(id: UUID): BloodPressureRecord? = transaction {
        BloodPressureLogs.selectAll().where { BloodPressureLogs.id eq id }
            .map { it.toRecord() }
            .singleOrNull()
    }

    /** Lists this user's readings, newest first. Optionally restricted to a single calendar day (UTC). */
    fun findAllForUser(userId: UUID, onDate: LocalDate? = null, limit: Int = 100): List<BloodPressureRecord> = transaction {
        var query = BloodPressureLogs.selectAll().where { BloodPressureLogs.userId eq userId }

        if (onDate != null) {
            val start = onDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            val end = start.plusSeconds(86400)
            query = query.andWhere { (BloodPressureLogs.measuredAt greaterEq start) and (BloodPressureLogs.measuredAt less end) }
        }

        query.orderBy(BloodPressureLogs.measuredAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toRecord() }
    }

    fun findLatestForUser(userId: UUID): BloodPressureRecord? = transaction {
        BloodPressureLogs.selectAll().where { BloodPressureLogs.userId eq userId }
            .orderBy(BloodPressureLogs.measuredAt, SortOrder.DESC)
            .limit(1)
            .map { it.toRecord() }
            .singleOrNull()
    }

    fun update(id: UUID, systolic: Int, diastolic: Int, pulse: Int?, notes: String?): BloodPressureRecord? = transaction {
        val updatedRows = BloodPressureLogs.update({ BloodPressureLogs.id eq id }) {
            it[BloodPressureLogs.systolic] = systolic
            it[BloodPressureLogs.diastolic] = diastolic
            it[BloodPressureLogs.pulse] = pulse
            it[BloodPressureLogs.notes] = notes
        }
        if (updatedRows == 0) return@transaction null
        BloodPressureLogs.selectAll().where { BloodPressureLogs.id eq id }.map { it.toRecord() }.singleOrNull()
    }

    fun delete(id: UUID): Boolean = transaction {
        BloodPressureLogs.deleteWhere { BloodPressureLogs.id eq id } > 0
    }

    /** Raw rows from the last 7 UTC calendar days (today + previous 6) — averaged per day in BloodPressureService. */
    fun findLastSevenDays(userId: UUID): List<BloodPressureRecord> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant()
        BloodPressureLogs.selectAll()
            .where { (BloodPressureLogs.userId eq userId) and (BloodPressureLogs.measuredAt greaterEq since) }
            .map { it.toRecord() }
    }

    private fun ResultRow.toRecord() = BloodPressureRecord(
        id = this[BloodPressureLogs.id],
        userId = this[BloodPressureLogs.userId],
        systolic = this[BloodPressureLogs.systolic],
        diastolic = this[BloodPressureLogs.diastolic],
        pulse = this[BloodPressureLogs.pulse],
        notes = this[BloodPressureLogs.notes],
        measuredAt = this[BloodPressureLogs.measuredAt]
    )
}
