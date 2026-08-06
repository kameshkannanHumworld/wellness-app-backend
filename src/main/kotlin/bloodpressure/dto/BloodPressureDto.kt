package com.wellnessapp.bloodpressure.dto

import kotlinx.serialization.Serializable

@Serializable
data class BloodPressureLogRequest(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int? = null,
    val notes: String? = null
)

@Serializable
data class BloodPressureLogResponse(
    val id: String,
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val notes: String?,
    val measuredAt: String
)

@Serializable
data class BloodPressureDayAverage(
    val date: String,               // "YYYY-MM-DD"
    val avgSystolic: Double? = null,
    val avgDiastolic: Double? = null,
    val avgPulse: Double? = null,
    val readingCount: Int = 0
)

@Serializable
data class BloodPressureWeeklyResponse(
    val days: List<BloodPressureDayAverage>   // oldest → newest, always exactly 7 entries (today + previous 6 days)
)
