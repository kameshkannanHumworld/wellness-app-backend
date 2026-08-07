package com.wellnessapp.reminders.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReminderRequest(
    val reminderType: String,
    val reminderTime: String,        // "HH:mm", 24-hour, interpreted as UTC — see ReminderService kdoc
    val repeatType: String? = null,  // one of: once, daily, weekdays, weekends — defaults to "daily"
    val enabled: Boolean? = null     // only honored by PUT (full replace); POST always creates enabled=true
)

@Serializable
data class ReminderResponse(
    val id: String,
    val reminderType: String,
    val reminderTime: String,        // "HH:mm"
    val repeatType: String,
    val enabled: Boolean,
    val createdAt: String
)
