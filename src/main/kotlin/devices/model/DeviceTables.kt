package com.wellnessapp.devices.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

/**
 * One row per registered FCM device token. Matches the live `device_tokens` table exactly
 * (verified via information_schema before writing this module):
 *   - fcm_token is `text`, NOT NULL, no length limit and no UNIQUE constraint — uniqueness is
 *     enforced at the app layer instead (DeviceTokenRepository.upsert re-homes an existing token
 *     row to the calling user rather than inserting a duplicate; see its kdoc for why).
 *   - platform varchar(10), default 'android' — app layer restricts to android/ios/web.
 */
object DeviceTokens : Table("device_tokens") {
    val id = uuid("id").autoGenerate()
    val userId = uuid("user_id")
    val fcmToken = text("fcm_token")
    val platform = varchar("platform", 10)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
