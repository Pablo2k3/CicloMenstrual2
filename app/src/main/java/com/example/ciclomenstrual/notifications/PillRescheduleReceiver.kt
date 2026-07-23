package com.example.ciclomenstrual.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ciclomenstrual.data.local.AppDatabase
import com.example.ciclomenstrual.data.repository.RoomContraceptiveRepository
import com.example.ciclomenstrual.domain.DateNormalizer
import com.example.ciclomenstrual.domain.PillScheduleCalculator
import com.example.ciclomenstrual.domain.PillStatusResolver
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeSource
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PillRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in SUPPORTED_ACTIONS) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = RoomContraceptiveRepository(AppDatabase.getInstance(context))
                repository.getActiveRegimen()?.let { regimen ->
                    val now = System.currentTimeMillis()
                    val calculator = PillScheduleCalculator()
                    val knownDates = repository.getIntakes()
                        .filter { it.regimenId == regimen.id }
                        .mapTo(hashSetOf()) { it.scheduledDate }
                    val today = DateNormalizer.normalize(now)
                    var date = regimen.startDate
                    while (date <= today) {
                        if (date !in knownDates) {
                            val number = calculator.pillNumber(regimen, date) ?: break
                            val dose = calculator.doseTime(regimen, date)
                            val placebo = calculator.isPlacebo(regimen, number)
                            val status = when {
                                placebo && now >= dose -> PillIntakeStatus.AUTO_PLACEBO
                                !placebo && now >= dose + PillStatusResolver.REMINDER_WINDOW_MILLIS ->
                                    PillIntakeStatus.MISSED
                                else -> null
                            }
                            status?.let {
                                repository.saveIntake(
                                    PillIntake(
                                        regimen.id,
                                        date,
                                        number,
                                        it,
                                        now.takeIf { _ -> placebo },
                                        PillIntakeSource.SYSTEM,
                                    ),
                                )
                                if (date == today && placebo) NotificationHelper.showPlacebo(context, number)
                            }
                        }
                        date = DateNormalizer.addDays(date, 1)
                    }
                    AlarmManagerPillReminderScheduler(context).scheduleNext(
                        regimen,
                        repository.getIntakes(),
                        now,
                    )
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED",
        )
    }
}
