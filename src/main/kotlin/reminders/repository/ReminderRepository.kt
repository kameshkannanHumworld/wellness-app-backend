package com.wellnessapp.reminders.repository

import com.wellnessapp.reminders.model.Reminders
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.time.LocalTime
import java.util.UUID

data class ReminderRecord(
    val id: UUID,
    val userId: UUID,
    val reminderType: String,
    val reminderTime: LocalTime,
    val repeatType: String,
    val enabled: Boolean,
    val createdAt: Instant
)

object ReminderRepository {

    fun insert(userId: UUID, reminderType: String, reminderTime: LocalTime, repeatType: String): ReminderRecord = transaction {
        val newId = UUID.randomUUID()
        val now = Instant.now()
        Reminders.insert {
            it[id] = newId
            it[Reminders.userId] = userId
            it[Reminders.reminderType] = reminderType
            it[Reminders.reminderTime] = reminderTime
            it[Reminders.repeatType] = repeatType
            it[enabled] = true
            it[createdAt] = now
        }
        ReminderRecord(newId, userId, reminderType, reminderTime, repeatType, true, now)
    }

    fun findById(id: UUID): ReminderRecord? = transaction {
        Reminders.selectAll().where { Reminders.id eq id }
            .map { it.toRecord() }
            .singleOrNull()
    }

    /** All of this user's reminders, soonest time-of-day first. Optional filter by enabled state. */
    fun findAllForUser(userId: UUID, enabledFilter: Boolean? = null): List<ReminderRecord> = transaction {
        var query = Reminders.selectAll().where { Reminders.userId eq userId }
        if (enabledFilter != null) {
            query = query.andWhere { Reminders.enabled eq enabledFilter }
        }
        query.orderBy(Reminders.reminderTime, SortOrder.ASC).map { it.toRecord() }
    }

    fun update(id: UUID, reminderType: String, reminderTime: LocalTime, repeatType: String, enabled: Boolean): ReminderRecord? = transaction {
        val updatedRows = Reminders.update({ Reminders.id eq id }) {
            it[Reminders.reminderType] = reminderType
            it[Reminders.reminderTime] = reminderTime
            it[Reminders.repeatType] = repeatType
            it[Reminders.enabled] = enabled
        }
        if (updatedRows == 0) return@transaction null
        Reminders.selectAll().where { Reminders.id eq id }.map { it.toRecord() }.singleOrNull()
    }

    fun setEnabled(id: UUID, enabled: Boolean): ReminderRecord? = transaction {
        val updatedRows = Reminders.update({ Reminders.id eq id }) {
            it[Reminders.enabled] = enabled
        }
        if (updatedRows == 0) return@transaction null
        Reminders.selectAll().where { Reminders.id eq id }.map { it.toRecord() }.singleOrNull()
    }

    fun delete(id: UUID): Boolean = transaction {
        Reminders.deleteWhere { Reminders.id eq id } > 0
    }

    /** Enabled reminders whose reminder_time exactly matches `at` (minute precision) — used by ReminderScheduler. */
    fun findDueAt(at: LocalTime): List<ReminderRecord> = transaction {
        Reminders.selectAll()
            .where { (Reminders.reminderTime eq at) and (Reminders.enabled eq true) }
            .map { it.toRecord() }
    }

    private fun ResultRow.toRecord() = ReminderRecord(
        id = this[Reminders.id],
        userId = this[Reminders.userId],
        reminderType = this[Reminders.reminderType],
        reminderTime = this[Reminders.reminderTime],
        repeatType = this[Reminders.repeatType],
        enabled = this[Reminders.enabled],
        createdAt = this[Reminders.createdAt]
    )
}
