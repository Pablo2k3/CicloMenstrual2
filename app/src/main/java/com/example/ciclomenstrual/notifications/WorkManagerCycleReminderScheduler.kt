package com.example.ciclomenstrual.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class WorkManagerCycleReminderScheduler(context: Context) : CycleReminderScheduler {
    private val workManager = WorkManager.getInstance(context)

    override fun schedule(predictedCycleStart: Long) {
        val predicted = Calendar.getInstance().apply { timeInMillis = predictedCycleStart }
        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }
        if (!predicted.after(endOfToday)) return

        val notificationTime = (predicted.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 15)
            set(Calendar.SECOND, 0)
        }.timeInMillis
        cancel()
        val message = "Tu próximo ciclo está previsto para mañana"
        val request = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(maxOf(0, notificationTime - System.currentTimeMillis()), TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putLong(NotificationWorker.KEY_NOTIFICATION_TIME, notificationTime)
                    .putString(NotificationWorker.KEY_NOTIFICATION_MESSAGE, message)
                    .build(),
            )
            .addTag(WORK_TAG)
            .build()
        workManager.enqueue(request)
    }

    override fun cancel() {
        workManager.cancelAllWorkByTag(WORK_TAG)
    }

    companion object {
        const val WORK_TAG = "cycle_notification"
    }
}
