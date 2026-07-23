package com.example.ciclomenstrual.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ciclomenstrual.data.local.AppDatabase
import com.example.ciclomenstrual.data.repository.RoomContraceptiveRepository
import com.example.ciclomenstrual.domain.PillScheduleCalculator
import com.example.ciclomenstrual.domain.PillAlarmEvent
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeSource
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PillAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handle(context.applicationContext, intent)
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun handle(context: Context, intent: Intent) {
        val repository = RoomContraceptiveRepository(AppDatabase.getInstance(context))
        val regimenId = intent.getLongExtra(AlarmManagerPillReminderScheduler.EXTRA_REGIMEN_ID, 0)
        val regimen = repository.getRegimens().firstOrNull { it.id == regimenId } ?: return
        val date = intent.getLongExtra(AlarmManagerPillReminderScheduler.EXTRA_DATE, 0)
        val number = intent.getIntExtra(AlarmManagerPillReminderScheduler.EXTRA_PILL_NUMBER, 0)
        val event = runCatching {
            PillAlarmEvent.valueOf(intent.getStringExtra(AlarmManagerPillReminderScheduler.EXTRA_EVENT).orEmpty())
        }.getOrDefault(PillAlarmEvent.DOSE)
        if (date == 0L || number == 0) return
        val existing = repository.getIntakes().firstOrNull {
            it.regimenId == regimen.id && it.scheduledDate == date
        }
        val placebo = PillScheduleCalculator().isPlacebo(regimen, number)
        when {
            placebo -> {
                repository.saveIntake(
                    PillIntake(
                        regimen.id, date, number, PillIntakeStatus.AUTO_PLACEBO,
                        System.currentTimeMillis(), PillIntakeSource.SYSTEM,
                    ),
                )
                NotificationHelper.showPlacebo(context, number)
            }
            existing?.status == PillIntakeStatus.TAKEN -> NotificationHelper.cancelPill(context)
            event == PillAlarmEvent.DEADLINE -> {
                repository.saveIntake(
                    PillIntake(
                        regimen.id, date, number, PillIntakeStatus.MISSED,
                        null, PillIntakeSource.SYSTEM,
                    ),
                )
                NotificationHelper.cancelPill(context)
            }
            else -> NotificationHelper.showPill(context, regimen.id, date, number)
        }
        val intakes = repository.getIntakes()
        AlarmManagerPillReminderScheduler(context).scheduleNext(regimen, intakes, System.currentTimeMillis())
    }
}
