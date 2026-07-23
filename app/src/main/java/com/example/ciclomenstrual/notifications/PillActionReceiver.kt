package com.example.ciclomenstrual.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ciclomenstrual.data.local.AppDatabase
import com.example.ciclomenstrual.data.repository.RoomContraceptiveRepository
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeSource
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PillActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = RoomContraceptiveRepository(AppDatabase.getInstance(context))
                val regimenId = intent.getLongExtra(
                    AlarmManagerPillReminderScheduler.EXTRA_REGIMEN_ID,
                    0,
                )
                val regimen = repository.getRegimens().firstOrNull { it.id == regimenId } ?: return@launch
                val date = intent.getLongExtra(AlarmManagerPillReminderScheduler.EXTRA_DATE, 0)
                val number = intent.getIntExtra(AlarmManagerPillReminderScheduler.EXTRA_PILL_NUMBER, 0)
                if (date == 0L || number == 0) return@launch
                repository.saveIntake(
                    PillIntake(
                        regimen.id, date, number, PillIntakeStatus.TAKEN,
                        System.currentTimeMillis(), PillIntakeSource.NOTIFICATION,
                    ),
                )
                NotificationHelper.cancelPill(context)
                AlarmManagerPillReminderScheduler(context).scheduleNext(
                    regimen,
                    repository.getIntakes(),
                    System.currentTimeMillis(),
                )
            } finally {
                pending.finish()
            }
        }
    }
}
