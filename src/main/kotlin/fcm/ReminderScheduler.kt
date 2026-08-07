package com.wellnessapp.fcm

import com.wellnessapp.reminders.repository.ReminderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

/**
 * The "actual push-notification sending logic" the roadmap flagged as not-yet-designed. Polls
 * once a minute rather than using a real cron/job-queue library, which is the right amount of
 * infrastructure for this project's scale (a freshers' mini-project, single backend instance) —
 * revisit with a proper scheduler (Quartz, or a DB-backed job table) if this ever runs as more
 * than one instance, since two instances would each independently fire every due reminder.
 *
 * **Known limitation (see ReminderService's kdoc for the full explanation):** reminder_time has
 * no associated timezone anywhere in this schema, so "due" here means "matches the current UTC
 * clock time," not the user's local time. Fine for a single-timezone pilot; needs a stored
 * per-user timezone before this is correct for users in different regions.
 */
object ReminderScheduler {

    private val logger = LoggerFactory.getLogger(ReminderScheduler::class.java)
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                runCatching { tick() }.onFailure {
                    // A bad tick (e.g. a transient DB hiccup) must not kill the loop permanently.
                    logger.error("ReminderScheduler tick failed", it)
                }
                delay(60_000)
            }
        }
        logger.info("ReminderScheduler started (polling every 60s, UTC clock time).")
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun tick() {
        val now = LocalTime.now(ZoneOffset.UTC).withSecond(0).withNano(0)
        val today = LocalDate.now(ZoneOffset.UTC).dayOfWeek
        val due = ReminderRepository.findDueAt(now)

        for (reminder in due) {
            val shouldFireToday = when (reminder.repeatType) {
                "weekdays" -> today !in WEEKEND_DAYS
                "weekends" -> today in WEEKEND_DAYS
                else -> true // "daily" and "once" always fire when the time matches
            }
            if (!shouldFireToday) continue

            FcmService.sendToUser(reminder.userId, "Reminder", reminder.reminderType)

            if (reminder.repeatType == "once") {
                ReminderRepository.setEnabled(reminder.id, false)
            }
        }
    }

    private val WEEKEND_DAYS = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
}
