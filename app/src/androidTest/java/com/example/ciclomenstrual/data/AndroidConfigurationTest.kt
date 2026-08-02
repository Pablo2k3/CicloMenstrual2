package com.example.ciclomenstrual.data

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.ciclomenstrual.notifications.PillActionReceiver
import com.example.ciclomenstrual.notifications.PillAlarmReceiver
import com.example.ciclomenstrual.notifications.PillRescheduleReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AndroidConfigurationTest {
    @Suppress("DEPRECATION")
    @Test
    fun manifestDeclaresNotificationAndAlarmPermissions() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS,
        )
        val permissions = packageInfo.requestedPermissions.orEmpty().toSet()

        assertTrue(Manifest.permission.POST_NOTIFICATIONS in permissions)
        assertTrue(Manifest.permission.SCHEDULE_EXACT_ALARM in permissions)
        assertTrue(Manifest.permission.RECEIVE_BOOT_COMPLETED in permissions)
    }

    @Test
    fun notificationReceiversAreNotExported() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageManager = context.packageManager
        val receiverNames = listOf(
            PillAlarmReceiver::class.java,
            PillActionReceiver::class.java,
            PillRescheduleReceiver::class.java,
        )

        receiverNames.forEach { receiver ->
            val info = packageManager.getReceiverInfo(
                ComponentName(context, receiver),
                0,
            )
            assertFalse(info.exported)
        }
    }
}
