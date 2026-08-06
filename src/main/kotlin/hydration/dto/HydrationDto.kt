package com.wellnessapp.hydration.dto

import kotlinx.serialization.Serializable

@Serializable
data class WaterLogRequest(
    val amountMl: Int
)

@Serializable
data class WaterLogResponse(
    val id: String,
    val amountMl: Int,
    val createdAt: String
)

@Serializable
data class WaterDayTotal(
    val date: String,   // "YYYY-MM-DD"
    val totalMl: Int
)

@Serializable
data class WaterWeeklyResponse(
    val days: List<WaterDayTotal>,   // oldest → newest, always exactly 7 entries (today + previous 6 days)
    val dailyGoalMl: Int? = null     // from daily_targets, if the user set one during onboarding
)
