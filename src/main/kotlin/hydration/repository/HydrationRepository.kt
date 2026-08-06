package com.wellnessapp.hydration.repository

import com.wellnessapp.hydration.model.WaterLogs
import com.wellnessapp.onboarding.model.DailyTargets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class WaterLogRecord(
    val id: UUID,
    val userId: UUID,
    val amountMl: Int,
    val createdAt: Instant
)

object HydrationRepository {

    fun insert(userId: UUID, amountMl: Int): WaterLogRecord = transaction {
        val newId = UUID.randomUUID()
        val now = Instant.now()
        WaterLogs.insert {
            it[id] = newId
            it[WaterLogs.userId] = userId
            it[WaterLogs.amountMl] = amountMl
            it[createdAt] = now
        }
        WaterLogRecord(newId, userId, amountMl, now)
    }

    fun findById(id: UUID): WaterLogRecord? = transaction {
        WaterLogs.selectAll().where { WaterLogs.id eq id }
            .map { it.toRecord() }
            .singleOrNull()
    }

    /** Lists this user's logs, newest first. Optionally restricted to a single calendar day (UTC). */
    fun findAllForUser(userId: UUID, onDate: LocalDate? = null, limit: Int = 100): List<WaterLogRecord> = transaction {
        var query = WaterLogs.selectAll().where { WaterLogs.userId eq userId }

        if (onDate != null) {
            val start = onDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            val end = start.plusSeconds(86400)
            query = query.andWhere { (WaterLogs.createdAt greaterEq start) and (WaterLogs.createdAt less end) }
        }

        query.orderBy(WaterLogs.createdAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toRecord() }
    }

    fun update(id: UUID, amountMl: Int): WaterLogRecord? = transaction {
        val updatedRows = WaterLogs.update({ WaterLogs.id eq id }) {
            it[WaterLogs.amountMl] = amountMl
        }
        if (updatedRows == 0) return@transaction null
        WaterLogs.selectAll().where { WaterLogs.id eq id }.map { it.toRecord() }.singleOrNull()
    }

    fun delete(id: UUID): Boolean = transaction {
        WaterLogs.deleteWhere { WaterLogs.id eq id } > 0
    }

    /** Per-day totals (UTC calendar days) for the last 7 days including today. Days with no logs are simply absent. */
    fun weeklyTotals(userId: UUID): Map<LocalDate, Int> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant()
        WaterLogs.selectAll()
            .where { (WaterLogs.userId eq userId) and (WaterLogs.createdAt greaterEq since) }
            .map { it[WaterLogs.createdAt].atZone(ZoneOffset.UTC).toLocalDate() to it[WaterLogs.amountMl] }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, amounts) -> amounts.sum() }
    }

    /** The user's daily water goal from onboarding (daily_targets.water_goal_ml), if they set one. */
    fun dailyGoalMl(userId: UUID): Int? = transaction {
        DailyTargets.selectAll().where { DailyTargets.userId eq userId }
            .map { it[DailyTargets.waterGoalMl] }
            .singleOrNull()
    }

    private fun ResultRow.toRecord() = WaterLogRecord(
        id = this[WaterLogs.id],
        userId = this[WaterLogs.userId],
        amountMl = this[WaterLogs.amountMl],
        createdAt = this[WaterLogs.createdAt]
    )
}
