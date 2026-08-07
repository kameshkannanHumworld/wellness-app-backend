package com.wellnessapp.heartrate.dto

import kotlinx.serialization.Serializable

@Serializable
data class HeartRateLogRequest(
    val bpm: Int,
    val source: String? = null   // one of: manual, health_connect, ble — defaults to "manual" if omitted
)

@Serializable
data class HeartRateLogResponse(
    val id: String,
    val bpm: Int,
    val source: String,
    val measuredAt: String
)

@Serializable
data class HeartRateDayStat(
    val date: String,           // "YYYY-MM-DD"
    val avgBpm: Double? = null,
    val minBpm: Int? = null,
    val maxBpm: Int? = null,
    val readingCount: Int = 0
)

@Serializable
data class HeartRateMonthStat(
    val month: String,          // "YYYY-MM"
    val avgBpm: Double? = null,
    val minBpm: Int? = null,
    val maxBpm: Int? = null,
    val readingCount: Int = 0
)

@Serializable
data class HeartRateDailyResponse(val days: List<HeartRateDayStat>)      // used for both /weekly (7) and /monthly (30)

@Serializable
data class HeartRateYearlyResponse(val months: List<HeartRateMonthStat>) // always exactly 12 entries
