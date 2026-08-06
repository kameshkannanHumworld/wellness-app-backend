package com.wellnessapp.activity.repository

import com.wellnessapp.activity.model.Activities
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

data class ActivityRecord(
    val id: UUID,
    val userId: UUID,
    val activityType: String,
    val value: BigDecimal?,
    val durationMinutes: Int?,
    val calories: Int?,
    val loggedAt: Instant
)

object ActivityRepository {

    fun insert(
        userId: UUID,
        activityType: String,
        value: BigDecimal?,
        durationMinutes: Int?,
        calories: Int?
    ): ActivityRecord = transaction {
        val newId = UUID.randomUUID()
        val now = Instant.now()
        Activities.insert {
            it[id] = newId
            it[Activities.userId] = userId
            it[Activities.activityType] = activityType
            it[Activities.value] = value
            it[Activities.durationMinutes] = durationMinutes
            it[Activities.calories] = calories
            it[loggedAt] = now
        }
        ActivityRecord(newId, userId, activityType, value, durationMinutes, calories, now)
    }

    fun findById(id: UUID): ActivityRecord? = transaction {
        Activities.selectAll().where { Activities.id eq id }
            .map { it.toRecord() }
            .singleOrNull()
    }

    /** Lists this user's logs, newest first. Optionally filtered by activity_type and/or a single calendar day (UTC). */
    fun findAllForUser(userId: UUID, type: String? = null, onDate: LocalDate? = null, limit: Int = 100): List<ActivityRecord> = transaction {
        var query = Activities.selectAll().where { Activities.userId eq userId }

        if (type != null) {
            query = query.andWhere { Activities.activityType eq type }
        }
        if (onDate != null) {
            val start = onDate.atStartOfDay(ZoneOffset.UTC).toInstant()
            val end = start.plusSeconds(86400)
            query = query.andWhere { (Activities.loggedAt greaterEq start) and (Activities.loggedAt less end) }
        }

        query.orderBy(Activities.loggedAt, SortOrder.DESC)
            .limit(limit)
            .map { it.toRecord() }
    }

    fun update(id: UUID, activityType: String, value: BigDecimal?, durationMinutes: Int?, calories: Int?): ActivityRecord? = transaction {
        val updatedRows = Activities.update({ Activities.id eq id }) {
            it[Activities.activityType] = activityType
            it[Activities.value] = value
            it[Activities.durationMinutes] = durationMinutes
            it[Activities.calories] = calories
        }
        if (updatedRows == 0) return@transaction null
        Activities.selectAll().where { Activities.id eq id }.map { it.toRecord() }.singleOrNull()
    }

    fun delete(id: UUID): Boolean = transaction {
        Activities.deleteWhere { Activities.id eq id } > 0
    }

    /** Raw rows from the last 7 UTC calendar days (today + previous 6) — aggregated per day/type in ActivityService. */
    fun findLastSevenDays(userId: UUID): List<ActivityRecord> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant()
        Activities.selectAll()
            .where { (Activities.userId eq userId) and (Activities.loggedAt greaterEq since) }
            .map { it.toRecord() }
    }

    private fun ResultRow.toRecord() = ActivityRecord(
        id = this[Activities.id],
        userId = this[Activities.userId],
        activityType = this[Activities.activityType],
        value = this[Activities.value],
        durationMinutes = this[Activities.durationMinutes],
        calories = this[Activities.calories],
        loggedAt = this[Activities.loggedAt]
    )
}
