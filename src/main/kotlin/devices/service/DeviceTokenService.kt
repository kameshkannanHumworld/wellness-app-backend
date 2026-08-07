package com.wellnessapp.devices.service

import com.wellnessapp.common.exception.ValidationException
import com.wellnessapp.devices.dto.DeviceRegisterRequest
import com.wellnessapp.devices.dto.DeviceTokenResponse
import com.wellnessapp.devices.dto.TestPushResponse
import com.wellnessapp.devices.repository.DeviceTokenRecord
import com.wellnessapp.devices.repository.DeviceTokenRepository
import com.wellnessapp.fcm.FcmService
import java.util.UUID

object DeviceTokenService {

    private val ALLOWED_PLATFORMS = setOf("android", "ios", "web")
    private const val DEFAULT_PLATFORM = "android"

    fun registerDevice(userId: UUID, request: DeviceRegisterRequest): DeviceTokenResponse {
        val token = request.fcmToken.trim()
        val platform = (request.platform ?: DEFAULT_PLATFORM).trim().lowercase()

        val errors = mutableListOf<Pair<String, String>>()
        if (token.isBlank()) errors += "fcmToken" to "Must not be blank"
        if (platform !in ALLOWED_PLATFORMS) errors += "platform" to "Must be one of: ${ALLOWED_PLATFORMS.joinToString(", ")}"
        if (errors.isNotEmpty()) throw ValidationException(errors)

        return DeviceTokenRepository.upsert(userId, token, platform).toResponse()
    }

    fun listDevices(userId: UUID): List<DeviceTokenResponse> =
        DeviceTokenRepository.findAllForUser(userId).map { it.toResponse() }

    /** Idempotent by design — logout should never fail just because a token was already gone. */
    fun unregisterDevice(userId: UUID, fcmToken: String): Boolean {
        val token = fcmToken.trim()
        if (token.isBlank()) throw ValidationException(listOf("fcmToken" to "Must not be blank"))
        return DeviceTokenRepository.deleteByTokenForUser(userId, token)
    }

    fun sendTestPush(userId: UUID, title: String?, body: String?): TestPushResponse {
        val result = FcmService.sendToUser(
            userId,
            title?.takeIf { it.isNotBlank() } ?: "Test Notification",
            body?.takeIf { it.isNotBlank() } ?: "This is a test push from the Wellness App backend."
        )
        return TestPushResponse(sentCount = result.sent, failedCount = result.failed)
    }

    private fun DeviceTokenRecord.toResponse() = DeviceTokenResponse(
        id = id.toString(),
        fcmToken = fcmToken,
        platform = platform,
        createdAt = createdAt.toString()
    )
}
