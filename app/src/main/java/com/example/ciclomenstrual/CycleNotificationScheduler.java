package com.example.ciclomenstrual;

import android.content.Context;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Encapsulates notification scheduling logic related to cycle predictions.
 */
public class CycleNotificationScheduler {

    public static final String CYCLE_NOTIFICATION_TAG = "cycle_notification";

    private final WorkManager workManager;

    public CycleNotificationScheduler(Context context) {
        this.workManager = WorkManager.getInstance(context);
    }

    public void cancelAllNotifications() {
        workManager.cancelAllWorkByTag(CYCLE_NOTIFICATION_TAG);
    }

    public void scheduleNotification(Calendar notificationDate, int hour, int minute, String message) {
        if (notificationDate == null) {
            return;
        }
        Calendar notificationTime = (Calendar) notificationDate.clone();
        notificationTime.set(Calendar.HOUR_OF_DAY, hour);
        notificationTime.set(Calendar.MINUTE, minute);
        notificationTime.set(Calendar.SECOND, 0);
        notificationTime.set(Calendar.MILLISECOND, 0);

        long currentTime = System.currentTimeMillis();
        long notificationTimeMillis = notificationTime.getTimeInMillis();
        long initialDelay = Math.max(0, notificationTimeMillis - currentTime);

        Data inputData = new Data.Builder()
                .putLong("notification_time", notificationTimeMillis)
                .putString("notification_message", message)
                .build();

        OneTimeWorkRequest notificationWork = new OneTimeWorkRequest.Builder(NotificationWorker.class)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(CYCLE_NOTIFICATION_TAG)
                .build();

        workManager.enqueue(notificationWork);
    }
}
