package com.wellnessapp.dashboard.dto

import kotlinx.serialization.Serializable

@Serializable
data class DashboardMetric(
    val value: Double,   // today's actual value (water ml, step count, sleep hours)
    val goal: Double,    // the goal being measured against — from daily_targets, or a default if unset
    val score: Double    // value / goal, capped to [0.0, 1.0]
)

@Serializable
data class DashboardBloodPressure(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val measuredAt: String
)

@Serializable
data class DashboardResponse(
    val wellnessScore: Int,   // ((water.score + steps.score + sleep.score) / 3) * 100, rounded
    val water: DashboardMetric,
    val steps: DashboardMetric,
    val sleep: DashboardMetric,
    val latestBloodPressure: DashboardBloodPressure? = null
)

@Serializable
data class DashboardInsightResponse(
    val insights: List<String>   // rule-based tips, always at least one entry
)

@Serializable
data class StepsDayTotal(
    val date: String,   // "YYYY-MM-DD"
    val steps: Double,
    val goal: Int? = null
)

@Serializable
data class DashboardWeeklyStepsResponse(
    val days: List<StepsDayTotal>   // oldest → newest, always exactly 7 entries (today + previous 6 days)
)
