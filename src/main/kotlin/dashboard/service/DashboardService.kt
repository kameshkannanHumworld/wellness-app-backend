package com.wellnessapp.dashboard.service

import com.wellnessapp.dashboard.dto.DashboardBloodPressure
import com.wellnessapp.dashboard.dto.DashboardInsightResponse
import com.wellnessapp.dashboard.dto.DashboardMetric
import com.wellnessapp.dashboard.dto.DashboardResponse
import com.wellnessapp.dashboard.dto.DashboardWeeklyStepsResponse
import com.wellnessapp.dashboard.dto.StepsDayTotal
import com.wellnessapp.dashboard.repository.DashboardRepository
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Implements the wellness score formula from the original plan:
 *   ((water_score + step_score + sleep_score) / 3) × 100
 * where each *_score is today's actual value divided by the user's goal, capped to [0, 1].
 *
 * If the user hasn't set a goal yet (skipped that onboarding step, or set targets before this
 * module existed), a sensible default goal is used instead so the score always computes rather
 * than dividing by zero or excluding that metric — DEFAULT_* below. This is a product judgment
 * call, not a mirrored constraint; revisit once there's real usage data.
 */
object DashboardService {

    private const val DEFAULT_WATER_GOAL_ML = 2000.0
    private const val DEFAULT_STEP_GOAL = 8000.0
    private const val DEFAULT_SLEEP_GOAL_HOURS = 8.0

    fun getDashboard(userId: UUID): DashboardResponse {
        val targets = DashboardRepository.dailyTargets(userId)

        val waterGoal = targets?.waterGoalMl?.toDouble() ?: DEFAULT_WATER_GOAL_ML
        val stepGoal = targets?.stepGoal?.toDouble() ?: DEFAULT_STEP_GOAL
        val sleepGoal = targets?.sleepGoalHours?.toDouble() ?: DEFAULT_SLEEP_GOAL_HOURS

        val waterMetric = metric(DashboardRepository.waterConsumedToday(userId).toDouble(), waterGoal)
        val stepsMetric = metric(DashboardRepository.stepsToday(userId), stepGoal)
        val sleepMetric = metric(DashboardRepository.sleepHoursToday(userId), sleepGoal)

        val wellnessScore = (((waterMetric.score + stepsMetric.score + sleepMetric.score) / 3.0) * 100).roundToInt()

        val bp = DashboardRepository.latestBloodPressure(userId)?.let {
            DashboardBloodPressure(it.systolic, it.diastolic, it.pulse, it.measuredAt.toString())
        }

        return DashboardResponse(
            wellnessScore = wellnessScore,
            water = waterMetric,
            steps = stepsMetric,
            sleep = sleepMetric,
            latestBloodPressure = bp
        )
    }

    /** Rule-based tips — no ML, just threshold checks against today's dashboard metrics. */
    fun getInsight(userId: UUID): DashboardInsightResponse {
        val dashboard = getDashboard(userId)
        val insights = mutableListOf<String>()

        if (dashboard.water.score < 0.5) {
            insights += "You've had ${(dashboard.water.score * 100).roundToInt()}% of your water goal today — try to drink more."
        }
        if (dashboard.steps.score < 0.5) {
            insights += "You're at ${(dashboard.steps.score * 100).roundToInt()}% of your step goal today — a short walk could help."
        }
        if (dashboard.sleep.score < 0.7) {
            insights += "You logged ${"%.1f".format(dashboard.sleep.value)}h of sleep — aim closer to your ${"%.1f".format(dashboard.sleep.goal)}h goal."
        }
        dashboard.latestBloodPressure?.let { bp ->
            if (bp.systolic >= 140 || bp.diastolic >= 90) {
                insights += "Your last blood pressure reading (${bp.systolic}/${bp.diastolic}) was elevated — consider checking in with a doctor."
            }
        }

        if (insights.isEmpty()) {
            insights += "Great job! You're on track with your wellness goals today."
        }

        return DashboardInsightResponse(insights)
    }

    /** Last 7 days (today + previous 6), oldest first, zero-filled — for the dashboard's steps chart. */
    fun getWeeklySteps(userId: UUID): DashboardWeeklyStepsResponse {
        val totals = DashboardRepository.stepsLastSevenDays(userId)
        val goal = DashboardRepository.dailyTargets(userId)?.stepGoal

        val today = LocalDate.now(ZoneOffset.UTC)
        val days = (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            StepsDayTotal(date.format(DateTimeFormatter.ISO_LOCAL_DATE), totals[date] ?: 0.0, goal)
        }

        return DashboardWeeklyStepsResponse(days)
    }

    private fun metric(value: Double, goal: Double): DashboardMetric {
        val score = if (goal > 0) (value / goal).coerceIn(0.0, 1.0) else 0.0
        return DashboardMetric(value = value, goal = goal, score = score)
    }
}
