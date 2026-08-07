package com.wellnessapp.spo2.repository

import com.wellnessapp.spo2.model.Spo2Logs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class Spo2Record(
    val id: UUID,
    val userId: UUID,
    val spo2Percentage: Int,
    val source: String,
    val measuredAt: Instant
)

object Spo2Repository {

    fun insert(userId: UUID, spo2Percentage: Int, source: String): Spo2Record = transaction {
        val newId = UUID.randomUUID()
        val now = Instant.now()
        Spo2Logs.insert {
            it[id] = newId
            it[Spo2Logs.userId] = userId
            it[Spo2Logs.spo2Percentage] = spo2Percentage
            it[Spo2Logs.sourceType] = source
            it[measuredAt] = now
        }
        Spo2Record(newId, userId, spo2Percentage, source, now)
    }

    fun findById(id: UUID): Spo2Record? = transaction {
        Spo2Logs.selectAll().where { Spo2Logs.id eq id }
            .map { it.toRecord() }
            .singleOrNull()
    }

    /** Lists this user's readings, newest first. Optionally restricted to a single calendar day (UTC). */
    fun findAllForUser(userId: UUID, onDate: LocalDate? = null, limit: Int = 100): List<Spo2Record> = transaction {
        var query = Spo2Logs.selectAll().where { Spo2Logs.userId eq userId }

        if (onDate != null) {
            val start = onDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            val end = start.plusSeconds(86400)
            query = query.andWhere { (Spo2Logs.measuredAt greaterEq start) and (Spo2Logs.measuredAt less end) }
        }

        query.orderBy(Spo2Logs.measuredAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toRecord() }
    }

    fun findLatestForUser(userId: UUID): Spo2Record? = transaction {
        Spo2Logs.selectAll().where { Spo2Logs.userId eq userId }
            .orderBy(Spo2Logs.measuredAt, SortOrder.DESC)
            .limit(1)
            .map { it.toRecord() }
            .singleOrNull()
    }

    fun update(id: UUID, spo2Percentage: Int, source: String): Spo2Record? = transaction {
        val updatedRows = Spo2Logs.update({ Spo2Logs.id eq id }) {
            it[Spo2Logs.spo2Percentage] = spo2Percentage
            it[Spo2Logs.sourceType] = source
        }
        if (updatedRows == 0) return@transaction null
        Spo2Logs.selectAll().where { Spo2Logs.id eq id }.map { it.toRecord() }.singleOrNull()
    }

    fun delete(id: UUID): Boolean = transaction {
        Spo2Logs.deleteWhere { Spo2Logs.id eq id } > 0
    }

    /** Raw rows from the last N UTC calendar days (today + previous N-1) — bucketed per day in Spo2Service. */
    fun findLastNDays(userId: UUID, days: Int): List<Spo2Record> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).minusDays((days - 1).toLong()).atStartOfDay(ZoneOffset.UTC).toInstant()
        Spo2Logs.selectAll()
            .where { (Spo2Logs.userId eq userId) and (Spo2Logs.measuredAt greaterEq since) }
            .map { it.toRecord() }
    }

    /** Raw rows from the last 12 calendar months (UTC) — bucketed per month in Spo2Service. */
    fun findLastTwelveMonths(userId: UUID): List<Spo2Record> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusMonths(11).atStartOfDay(ZoneOffset.UTC).toInstant()
        Spo2Logs.selectAll()
            .where { (Spo2Logs.userId eq userId) and (Spo2Logs.measuredAt greaterEq since) }
            .map { it.toRecord() }
    }

    private fun ResultRow.toRecord() = Spo2Record(
        id = this[Spo2Logs.id],
        userId = this[Spo2Logs.userId],
        spo2Percentage = this[Spo2Logs.spo2Percentage],
        source = this[Spo2Logs.sourceType],
        measuredAt = this[Spo2Logs.measuredAt]
    )
}
