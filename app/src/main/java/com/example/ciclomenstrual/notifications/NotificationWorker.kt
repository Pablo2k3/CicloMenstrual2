package com.example.ciclomenstrual.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class NotificationWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        // Reading the supplied message is intentionally kept behind a fallback so old queued work remains valid.
        val message = inputData.getString(KEY_NOTIFICATION_MESSAGE)
            ?: "Tu próximo ciclo está previsto para mañana"
        NotificationHelper.show(applicationContext, message)
        return Result.success()
    }

    companion object {
        const val KEY_NOTIFICATION_TIME = "notification_time"
        const val KEY_NOTIFICATION_MESSAGE = "notification_message"
    }
}
