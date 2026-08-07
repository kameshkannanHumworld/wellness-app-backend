package com.wellnessapp.dashboard.repository

import com.wellnessapp.activity.model.Activities
import com.wellnessapp.bloodpressure.model.BloodPressureLogs
import com.wellnessapp.heartrate.model.HeartRateLogs
import com.wellnessapp.hydration.model.WaterLogs
import com.wellnessapp.onboarding.model.DailyTargets
import com.wellnessapp.spo2.model.Spo2Logs
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * The Dashboard module owns no table of its own — it's a read-only aggregation layer over
 * water_logs, activities, blood_pressure_logs, heart_rate_logs, spo2_logs, and daily_targets (all
 * already modeled by the hydration/activity/bloodpressure/heartrate/spo2/onboarding modules).
 * Reusing their Exposed `Table` objects here mirrors how `hydration`/`activity` already reuse
 * `onboarding.model.DailyTargets` for goals.
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

data class LatestHeartRateSnapshot(
    val bpm: Int,
    val source: String,
    val measuredAt: Instant
)

data class LatestSpo2Snapshot(
    val spo2Percentage: Int,
    val source: String,
    val measuredAt: Instant
)

data class TodayActivityTotals(
    val steps: Double,
    val workout: Double,
    val sleep: Double,
    val movement: Double,
    val caloriesBurned: Int
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

    fun latestHeartRate(userId: UUID): LatestHeartRateSnapshot? = transaction {
        HeartRateLogs.selectAll().where { HeartRateLogs.userId eq userId }
            .orderBy(HeartRateLogs.measuredAt, SortOrder.DESC)
            .limit(1)
            .map {
                LatestHeartRateSnapshot(
                    bpm = it[HeartRateLogs.bpm],
                    source = it[HeartRateLogs.sourceType],
                    measuredAt = it[HeartRateLogs.measuredAt]
                )
            }
            .singleOrNull()
    }

    fun latestSpo2(userId: UUID): LatestSpo2Snapshot? = transaction {
        Spo2Logs.selectAll().where { Spo2Logs.userId eq userId }
            .orderBy(Spo2Logs.measuredAt, SortOrder.DESC)
            .limit(1)
            .map {
                LatestSpo2Snapshot(
                    spo2Percentage = it[Spo2Logs.spo2Percentage],
                    source = it[Spo2Logs.sourceType],
                    measuredAt = it[Spo2Logs.measuredAt]
                )
            }
            .singleOrNull()
    }

    /** Today's (UTC calendar day) activity totals by type, plus total calories — one query, aggregated in Kotlin. */
    fun activityTotalsToday(userId: UUID): TodayActivityTotals = transaction {
        val (start, end) = todayRangeUtc()
        val rows = Activities.selectAll()
            .where { (Activities.userId eq userId) and (Activities.loggedAt greaterEq start) and (Activities.loggedAt less end) }
            .toList()

        fun sumFor(type: String) = rows.filter { it[Activities.activityType] == type }
            .sumOf { it[Activities.value]?.toDouble() ?: 0.0 }

        TodayActivityTotals(
            steps = sumFor("steps"),
            workout = sumFor("workout"),
            sleep = sumFor("sleep"),
            movement = sumFor("movement"),
            caloriesBurned = rows.sumOf { it[Activities.calories] ?: 0 }
        )
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
