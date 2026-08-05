package com.wellnessapp.auth.model

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object Users : Table("users") {
    val id = uuid("id").autoGenerate()
    val firebaseUid = varchar("firebase_uid", 100).uniqueIndex()
    val email = varchar("email", 255).uniqueIndex()
    val fullName = varchar("full_name", 255).nullable()
    val photoUrl = text("photo_url").nullable()
    val authProvider = varchar("auth_provider", 20).default("email")
    val onboardingCompleted = bool("onboarding_completed").default(false)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
