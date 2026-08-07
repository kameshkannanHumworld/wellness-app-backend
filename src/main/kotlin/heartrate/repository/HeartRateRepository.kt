package com.wellnessapp.heartrate.repository

import com.wellnessapp.heartrate.model.HeartRateLogs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class HeartRateRecord(
    val id: UUID,
    val userId: UUID,
    val bpm: Int,
    val source: String,
    val measuredAt: Instant
)

object HeartRateRepository {

    fun insert(userId: UUID, bpm: Int, source: String): HeartRateRecord = transaction {
        val newId = UUID.randomUUID()
        val now = Instant.now()
        HeartRateLogs.insert {
            it[id] = newId
            it[HeartRateLogs.userId] = userId
            it[HeartRateLogs.bpm] = bpm
            it[HeartRateLogs.sourceType] = source
            it[measuredAt] = now
        }
        HeartRateRecord(newId, userId, bpm, source, now)
    }

    fun findById(id: UUID): HeartRateRecord? = transaction {
        HeartRateLogs.selectAll().where { HeartRateLogs.id eq id }
            .map { it.toRecord() }
            .singleOrNull()
    }

    /** Lists this user's readings, newest first. Optionally restricted to a single calendar day (UTC). */
    fun findAllForUser(userId: UUID, onDate: LocalDate? = null, limit: Int = 100): List<HeartRateRecord> = transaction {
        var query = HeartRateLogs.selectAll().where { HeartRateLogs.userId eq userId }

        if (onDate != null) {
            val start = onDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            val end = start.plusSeconds(86400)
            query = query.andWhere { (HeartRateLogs.measuredAt greaterEq start) and (HeartRateLogs.measuredAt less end) }
        }

        query.orderBy(HeartRateLogs.measuredAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toRecord() }
    }

    fun findLatestForUser(userId: UUID): HeartRateRecord? = transaction {
        HeartRateLogs.selectAll().where { HeartRateLogs.userId eq userId }
            .orderBy(HeartRateLogs.measuredAt, SortOrder.DESC)
            .limit(1)
            .map { it.toRecord() }
            .singleOrNull()
    }

    fun update(id: UUID, bpm: Int, source: String): HeartRateRecord? = transaction {
        val updatedRows = HeartRateLogs.update({ HeartRateLogs.id eq id }) {
            it[HeartRateLogs.bpm] = bpm
            it[HeartRateLogs.sourceType] = source
        }
        if (updatedRows == 0) return@transaction null
        HeartRateLogs.selectAll().where { HeartRateLogs.id eq id }.map { it.toRecord() }.singleOrNull()
    }

    fun delete(id: UUID): Boolean = transaction {
        HeartRateLogs.deleteWhere { HeartRateLogs.id eq id } > 0
    }

    /** Raw rows from the last N UTC calendar days (today + previous N-1) — bucketed per day in HeartRateService. */
    fun findLastNDays(userId: UUID, days: Int): List<HeartRateRecord> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).minusDays((days - 1).toLong()).atStartOfDay(ZoneOffset.UTC).toInstant()
        HeartRateLogs.selectAll()
            .where { (HeartRateLogs.userId eq userId) and (HeartRateLogs.measuredAt greaterEq since) }
            .map { it.toRecord() }
    }

    /** Raw rows from the last 12 calendar months (UTC) — bucketed per month in HeartRateService. */
    fun findLastTwelveMonths(userId: UUID): List<HeartRateRecord> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(11).atStartOfDay(ZoneOffset.UTC).toInstant()
        HeartRateLogs.selectAll()
            .where { (HeartRateLogs.userId eq userId) and (HeartRateLogs.measuredAt greaterEq since) }
            .map { it.toRecord() }
    }

    private fun ResultRow.toRecord() = HeartRateRecord(
        id = this[HeartRateLogs.id],
        userId = this[HeartRateLogs.userId],
        bpm = this[HeartRateLogs.bpm],
        source = this[HeartRateLogs.sourceType],
        measuredAt = this[HeartRateLogs.measuredAt]
    )
}
