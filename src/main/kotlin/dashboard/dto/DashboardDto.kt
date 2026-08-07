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
data class DashboardHeartRate(
    val bpm: Int,
    val source: String,
    val measuredAt: String
)

@Serializable
data class DashboardSpo2(
    val spo2Percentage: Int,
    val source: String,
    val measuredAt: String
)

// Today's activity totals by type, UTC calendar day — a dashboard-scoped cousin of ActivityDayTotal
// (activity module) without the `date` field, since the dashboard is always "today" by definition.
@Serializable
data class DashboardActivitySummary(
    val steps: Double = 0.0,
    val workout: Double = 0.0,
    val sleep: Double = 0.0,
    val movement: Double = 0.0,
    val caloriesBurned: Int = 0
)

@Serializable
data class DashboardResponse(
    val wellnessScore: Int,   // ((water.score + steps.score + sleep.score) / 3) * 100, rounded
    val water: DashboardMetric,
    val steps: DashboardMetric,
    val sleep: DashboardMetric,
    val latestBloodPressure: DashboardBloodPressure? = null,
    val latestHeartRate: DashboardHeartRate? = null,
    val latestSpo2: DashboardSpo2? = null,
    val todayActivities: DashboardActivitySummary = DashboardActivitySummary()
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
