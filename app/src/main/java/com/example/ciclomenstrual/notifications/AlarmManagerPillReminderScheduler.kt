package com.example.ciclomenstrual.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ciclomenstrual.domain.DateNormalizer
import com.example.ciclomenstrual.domain.PillScheduleCalculator
import com.example.ciclomenstrual.domain.PillStatusResolver
import com.example.ciclomenstrual.domain.PillReminderPlanner
import com.example.ciclomenstrual.domain.PillAlarmEvent
import com.example.ciclomenstrual.domain.model.ContraceptiveRegimen
import com.example.ciclomenstrual.domain.model.PillIntake
import com.example.ciclomenstrual.domain.model.PillIntakeStatus
import java.util.Calendar

class AlarmManagerPillReminderScheduler(
    private val context: Context,
    private val calculator: PillScheduleCalculator = PillScheduleCalculator(),
    private val planner: PillReminderPlanner = PillReminderPlanner(calculator),
) : PillReminderScheduler {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleNext(regimen: ContraceptiveRegimen, intakes: List<PillIntake>, now: Long) {
        cancel()
        val plan = planner.next(regimen, intakes, now) ?: return
        schedule(regimen, plan.date, plan.pillNumber, plan.event, plan.triggerAt)
    }

    private fun schedule(
        regimen: ContraceptiveRegimen,
        date: Long,
        number: Int,
        event: PillAlarmEvent,
        triggerAt: Long,
    ) {
        val operation = alarmPendingIntent(context, regimen.id, date, number, event)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            return
        }
        if (canScheduleExact()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
                return
            } catch (_: SecurityException) {
                // Permission can be revoked between checking and scheduling.
            }
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
    }

    override fun cancel() = alarmManager.cancel(basePendingIntent(context))

    override fun cancelCurrentNotification() = NotificationHelper.cancelPill(context)

    override fun canScheduleExact(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()

    companion object {
        const val EXTRA_REGIMEN_ID = "regimen_id"
        const val EXTRA_DATE = "scheduled_date"
        const val EXTRA_PILL_NUMBER = "pill_number"
        const val EXTRA_EVENT = "alarm_event"
        private const val REQUEST_CODE = 7401

        fun alarmPendingIntent(
            context: Context,
            regimenId: Long,
            date: Long,
            number: Int,
            event: PillAlarmEvent,
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, PillAlarmReceiver::class.java).apply {
                action = "com.example.ciclomenstrual.PILL_ALARM"
                putExtra(EXTRA_REGIMEN_ID, regimenId)
                putExtra(EXTRA_DATE, date)
                putExtra(EXTRA_PILL_NUMBER, number)
                putExtra(EXTRA_EVENT, event.name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        private fun basePendingIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, PillAlarmReceiver::class.java).apply {
                action = "com.example.ciclomenstrual.PILL_ALARM"
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, PillAlarmReceiver::class.java).apply {
                action = "com.example.ciclomenstrual.PILL_ALARM"
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
