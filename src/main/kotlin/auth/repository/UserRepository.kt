package com.wellnessapp.auth.repository

import com.wellnessapp.auth.model.Users
import com.wellnessapp.common.security.VerifiedFirebaseUser
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.Instant
import java.util.UUID

data class UserRecord(
    val id: UUID,
    val firebaseUid: String,
    val email: String,
    val fullName: String?,
    val onboardingCompleted: Boolean
)

object UserRepository {

    fun findByFirebaseUid(firebaseUid: String): UserRecord? = transaction {
        Users.selectAll().where { Users.firebaseUid eq firebaseUid }
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    fun findById(id: UUID): UserRecord? = transaction {
        Users.selectAll().where { Users.id eq id }
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    /** Creates the user on first Firebase sign-in, or returns the existing one. */
    fun findOrCreate(verified: VerifiedFirebaseUser): UserRecord = transaction {
        val existing = Users.selectAll().where { Users.firebaseUid eq verified.firebaseUid }
            .map { it.toUserRecord() }
            .singleOrNull()

        if (existing != null) return@transaction existing

        val newId = UUID.randomUUID()
        val now = Instant.now()
        Users.insert {
            it[id] = newId
            it[firebaseUid] = verified.firebaseUid
            it[email] = verified.email ?: "${verified.firebaseUid}@no-email.local"
            it[fullName] = verified.fullName
            it[photoUrl] = verified.photoUrl
            it[authProvider] = verified.provider
            it[onboardingCompleted] = false
            it[createdAt] = now
            it[updatedAt] = now
        }

        UserRecord(newId, verified.firebaseUid, verified.email ?: "", verified.fullName, false)
    }

    fun markOnboardingComplete(userId: UUID) = transaction {
        Users.update({ Users.id eq userId }) {
            it[onboardingCompleted] = true
            it[updatedAt] = Instant.now()
        }
    }

    private fun ResultRow.toUserRecord() = UserRecord(
        id = this[Users.id],
        firebaseUid = this[Users.firebaseUid],
        email = this[Users.email],
        fullName = this[Users.fullName],
        onboardingCompleted = this[Users.onboardingCompleted]
    )
}
