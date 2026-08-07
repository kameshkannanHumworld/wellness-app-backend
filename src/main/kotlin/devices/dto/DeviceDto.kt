package com.wellnessapp.devices.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceRegisterRequest(
    val fcmToken: String,
    val platform: String? = null   // one of: android, ios, web — defaults to "android"
)

@Serializable
data class DeviceTokenResponse(
    val id: String,
    val fcmToken: String,
    val platform: String,
    val createdAt: String
)

@Serializable
data class DeviceUnregisterRequest(
    val fcmToken: String
)

@Serializable
data class UnregisterResponse(
    val unregistered: Boolean   // false if this token wasn't registered to the caller (still a 200, not an error)
)

@Serializable
data class TestPushRequest(
    val title: String? = null,
    val body: String? = null
)

@Serializable
data class TestPushResponse(
    val sentCount: Int,
    val failedCount: Int
)
