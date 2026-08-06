package com.wellnessapp.activity.dto

import kotlinx.serialization.Serializable

@Serializable
data class ActivityLogRequest(
    val activityType: String,       // one of: steps, workout, sleep, movement (case-insensitive)
    val value: Double? = null,      // meaning depends on type: step count, workout distance/reps, sleep hours, movement units
    val durationMinutes: Int? = null,
    val calories: Int? = null
)

@Serializable
data class ActivityLogResponse(
    val id: String,
    val activityType: String,
    val value: Double?,
    val durationMinutes: Int?,
    val calories: Int?,
    val loggedAt: String
)

@Serializable
data class ActivityDayTotal(
    val date: String,               // "YYYY-MM-DD"
    val steps: Double = 0.0,
    val workout: Double = 0.0,
    val sleep: Double = 0.0,
    val movement: Double = 0.0,
    val caloriesBurned: Int = 0
)

@Serializable
data class ActivityWeeklyResponse(
    val days: List<ActivityDayTotal>   // oldest → newest, always exactly 7 entries (today + previous 6 days)
)
