package com.wellnessapp.devices.repository

import com.wellnessapp.devices.model.DeviceTokens
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

data class DeviceTokenRecord(
    val id: UUID,
    val userId: UUID,
    val fcmToken: String,
    val platform: String,
    val createdAt: Instant
)

object DeviceTokenRepository {

    fun findByToken(fcmToken: String): DeviceTokenRecord? = transaction {
        DeviceTokens.selectAll().where { DeviceTokens.fcmToken eq fcmToken }
            .map { it.toRecord() }
            .singleOrNull()
    }

    /**
     * Same physical FCM token can legitimately belong to a different user over time (app reinstall,
     * different account signed in on the same device) — there's no UNIQUE constraint on fcm_token in
     * the DB, so this re-homes an existing row to the calling user/platform instead of inserting a
     * duplicate that would receive pushes meant for someone else.
     */
    fun upsert(userId: UUID, fcmToken: String, platform: String): DeviceTokenRecord = transaction {
        val existing = DeviceTokens.selectAll().where { DeviceTokens.fcmToken eq fcmToken }.map { it.toRecord() }.singleOrNull()

        if (existing != null) {
            DeviceTokens.update({ DeviceTokens.fcmToken eq fcmToken }) {
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.platform] = platform
            }
            existing.copy(userId = userId, platform = platform)
        } else {
            val newId = UUID.randomUUID()
            val now = Instant.now()
            DeviceTokens.insert {
                it[id] = newId
                it[DeviceTokens.userId] = userId
                it[DeviceTokens.fcmToken] = fcmToken
                it[DeviceTokens.platform] = platform
                it[createdAt] = now
            }
            DeviceTokenRecord(newId, userId, fcmToken, platform, now)
        }
    }

    fun findAllForUser(userId: UUID): List<DeviceTokenRecord> = transaction {
        DeviceTokens.selectAll().where { DeviceTokens.userId eq userId }
            .orderBy(DeviceTokens.createdAt, SortOrder.DESC)
            .map { it.toRecord() }
    }

    /** Deletes a token only if it belongs to this user — returns whether a row was actually removed. */
    fun deleteByTokenForUser(userId: UUID, fcmToken: String): Boolean = transaction {
        DeviceTokens.deleteWhere { (DeviceTokens.userId eq userId) and (DeviceTokens.fcmToken eq fcmToken) } > 0
    }

    private fun ResultRow.toRecord() = DeviceTokenRecord(
        id = this[DeviceTokens.id],
        userId = this[DeviceTokens.userId],
        fcmToken = this[DeviceTokens.fcmToken],
        platform = this[DeviceTokens.platform],
        createdAt = this[DeviceTokens.createdAt]
    )
}
