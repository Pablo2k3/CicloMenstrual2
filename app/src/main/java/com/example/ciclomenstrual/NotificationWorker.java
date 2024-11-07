package com.example.ciclomenstrual;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class NotificationWorker extends Worker {
    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String title = "Recordatorio de ciclo";
        String message = "Tu próximo ciclo está previsto para mañana";

        NotificationHelper.showNotification(getApplicationContext(), title, message);

        return Result.success();
    }
}