package com.wellnessapp.spo2.dto

import kotlinx.serialization.Serializable

@Serializable
data class Spo2LogRequest(
    val spo2Percentage: Int,
    val source: String? = null   // one of: manual, health_connect, ble — defaults to "manual" if omitted
)

@Serializable
data class Spo2LogResponse(
    val id: String,
    val spo2Percentage: Int,
    val source: String,
    val measuredAt: String
)

@Serializable
data class Spo2DayStat(
    val date: String,               // "YYYY-MM-DD"
    val avgSpo2: Double? = null,
    val minSpo2: Int? = null,
    val maxSpo2: Int? = null,
    val readingCount: Int = 0
)

@Serializable
data class Spo2MonthStat(
    val month: String,               // "YYYY-MM"
    val avgSpo2: Double? = null,
    val minSpo2: Int? = null,
    val maxSpo2: Int? = null,
    val readingCount: Int = 0
)

@Serializable
data class Spo2DailyResponse(val days: List<Spo2DayStat>)        // used for both /weekly (7) and /monthly (30)

@Serializable
data class Spo2YearlyResponse(val months: List<Spo2MonthStat>)   // always exactly 12 entries
