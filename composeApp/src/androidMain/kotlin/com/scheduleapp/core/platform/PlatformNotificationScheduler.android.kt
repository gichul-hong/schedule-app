package com.scheduleapp.core.platform

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

actual class PlatformNotificationScheduler actual constructor() {
    private val context get() = PlatformContextHolder.appContext

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val ctx = context ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "일정 및 정비 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "차량 정비 주기 및 일정 리마인더 알림"
                enableVibration(true)
            }
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    actual fun checkNotificationPermission(): Boolean {
        val ctx = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.areNotificationsEnabled() ?: false
        }
    }

    actual suspend fun requestNotificationPermission(): Boolean {
        return checkNotificationPermission()
    }

    actual fun scheduleReminder(id: Long, title: String, message: String, triggerEpochMillis: Long) {
        val ctx = context ?: return
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(ctx, ctx.javaClass).apply {
            putExtra("notification_id", id)
            putExtra("title", title)
            putExtra("message", message)
        }

        val pendingIntent = PendingIntent.getActivity(
            ctx,
            id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerEpochMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    actual fun cancelReminder(id: Long) {
        val ctx = context ?: return
        val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(ctx, ctx.javaClass)
        val pendingIntent = PendingIntent.getActivity(
            ctx,
            id.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    companion object {
        const val CHANNEL_ID = "schedule_car_reminders"
    }
}
