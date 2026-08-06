package com.wellnessapp.dashboard.repository

import com.wellnessapp.activity.model.Activities
import com.wellnessapp.bloodpressure.model.BloodPressureLogs
import com.wellnessapp.hydration.model.WaterLogs
import com.wellnessapp.onboarding.model.DailyTargets
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * The Dashboard module owns no table of its own — it's a read-only aggregation layer over
 * water_logs, activities, blood_pressure_logs, and daily_targets (all already modeled by the
 * hydration/activity/bloodpressure/onboarding modules). Reusing their Exposed `Table` objects here
 * mirrors how `hydration`/`activity` already reuse `onboarding.model.DailyTargets` for goals.
 */
data class DailyTargetsSnapshot(
    val waterGoalMl: Int?,
    val stepGoal: Int?,
    val sleepGoalHours: BigDecimal?
)

data class LatestBloodPressureSnapshot(
    val systolic: Int,
    val diastolic: Int,
    val pulse: Int?,
    val measuredAt: Instant
)

object DashboardRepository {

    fun dailyTargets(userId: UUID): DailyTargetsSnapshot? = transaction {
        DailyTargets.selectAll().where { DailyTargets.userId eq userId }
            .map {
                DailyTargetsSnapshot(
                    waterGoalMl = it[DailyTargets.waterGoalMl],
                    stepGoal = it[DailyTargets.stepGoal],
                    sleepGoalHours = it[DailyTargets.sleepGoalHours]
                )
            }
            .singleOrNull()
    }

    fun waterConsumedToday(userId: UUID): Int = transaction {
        val (start, end) = todayRangeUtc()
        WaterLogs.selectAll()
            .where { (WaterLogs.userId eq userId) and (WaterLogs.createdAt greaterEq start) and (WaterLogs.createdAt less end) }
            .sumOf { it[WaterLogs.amountMl] }
    }

    fun stepsToday(userId: UUID): Double = activityValueToday(userId, "steps")

    fun sleepHoursToday(userId: UUID): Double = activityValueToday(userId, "sleep")

    private fun activityValueToday(userId: UUID, type: String): Double = transaction {
        val (start, end) = todayRangeUtc()
        Activities.selectAll()
            .where {
                (Activities.userId eq userId) and
                    (Activities.activityType eq type) and
                    (Activities.loggedAt greaterEq start) and
                    (Activities.loggedAt less end)
            }
            .sumOf { it[Activities.value]?.toDouble() ?: 0.0 }
    }

    fun latestBloodPressure(userId: UUID): LatestBloodPressureSnapshot? = transaction {
        BloodPressureLogs.selectAll().where { BloodPressureLogs.userId eq userId }
            .orderBy(BloodPressureLogs.measuredAt, SortOrder.DESC)
            .limit(1)
            .map {
                LatestBloodPressureSnapshot(
                    systolic = it[BloodPressureLogs.systolic],
                    diastolic = it[BloodPressureLogs.diastolic],
                    pulse = it[BloodPressureLogs.pulse],
                    measuredAt = it[BloodPressureLogs.measuredAt]
                )
            }
            .singleOrNull()
    }

    /** Step totals for the last 7 UTC calendar days (today + previous 6). Days with no logs are simply absent. */
    fun stepsLastSevenDays(userId: UUID): Map<LocalDate, Double> = transaction {
        val since = LocalDate.now(ZoneOffset.UTC).minusDays(6).atStartOfDay(ZoneOffset.UTC).toInstant()
        Activities.selectAll()
            .where {
                (Activities.userId eq userId) and
                    (Activities.activityType eq "steps") and
                    (Activities.loggedAt greaterEq since)
            }
            .map { it[Activities.loggedAt].atZone(ZoneOffset.UTC).toLocalDate() to (it[Activities.value]?.toDouble() ?: 0.0) }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, values) -> values.sum() }
    }

    private fun todayRangeUtc(): Pair<Instant, Instant> {
        val start = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant()
        return start to start.plusSeconds(86400)
    }
}
