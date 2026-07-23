package com.example.ciclomenstrual.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.annotation.SuppressLint
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ciclomenstrual.R
import com.example.ciclomenstrual.MainActivity

object NotificationHelper {
    private const val CYCLE_CHANNEL_ID = "cycle_reminder"
    private const val PILL_CHANNEL_ID = "pill_reminder"
    private const val PILL_NOTIFICATION_ID = 28

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CYCLE_CHANNEL_ID,
                "Recordatorios de ciclo",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notificaciones de recordatorio del ciclo menstrual"
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
            val pillChannel = NotificationChannel(
                PILL_CHANNEL_ID,
                "Recordatorio de pastilla",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Avisos diarios para la toma de la pastilla anticonceptiva"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(pillChannel)
        }
    }

    fun show(context: Context, message: String) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) return
        val notification = NotificationCompat.Builder(context, CYCLE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Recordatorio de ciclo")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(1, notification)
    }

    @SuppressLint("MissingPermission")
    fun showPill(context: Context, regimenId: Long, date: Long, number: Int) {
        if (!canNotify(context)) return
        val actionIntent = PendingIntent.getBroadcast(
            context,
            7800 + number,
            Intent(context, PillActionReceiver::class.java).apply {
                putExtra(AlarmManagerPillReminderScheduler.EXTRA_REGIMEN_ID, regimenId)
                putExtra(AlarmManagerPillReminderScheduler.EXTRA_DATE, date)
                putExtra(AlarmManagerPillReminderScheduler.EXTRA_PILL_NUMBER, number)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            7800,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, PILL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pastilla $number")
            .setContentText("Es hora de tomarla")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(contentIntent)
            .addAction(0, "Tomada", actionIntent)
            .build()
        NotificationManagerCompat.from(context).notify(PILL_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    fun showPlacebo(context: Context, number: Int) {
        if (!canNotify(context)) return
        val notification = NotificationCompat.Builder(context, PILL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Pastilla $number")
            .setContentText("Placebo — registrada automáticamente")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(PILL_NOTIFICATION_ID, notification)
    }

    fun cancelPill(context: Context) {
        NotificationManagerCompat.from(context).cancel(PILL_NOTIFICATION_ID)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}
