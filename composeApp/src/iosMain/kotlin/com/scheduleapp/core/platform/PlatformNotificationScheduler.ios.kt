package com.scheduleapp.core.platform

import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSinceNow
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class PlatformNotificationScheduler actual constructor() {
    private val center = UNUserNotificationCenter.currentNotificationCenter()

    actual fun checkNotificationPermission(): Boolean {
        return true
    }

    actual suspend fun requestNotificationPermission(): Boolean = suspendCoroutine { cont ->
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        center.requestAuthorizationWithOptions(options) { granted, _ ->
            cont.resume(granted)
        }
    }

    actual fun scheduleReminder(id: Long, title: String, message: String, triggerEpochMillis: Long) {
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(message)
            setSound(UNNotificationSound.defaultSound)
        }

        val targetDate = NSDate.dateWithTimeIntervalSince1970(triggerEpochMillis / 1000.0)
        val timeInterval = targetDate.timeIntervalSinceNow()
        val triggerInterval = if (timeInterval > 0) timeInterval else 1.0

        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = triggerInterval,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = id.toString(),
            content = content,
            trigger = trigger
        )

        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Failed to schedule iOS notification: $error")
            }
        }
    }

    actual fun cancelReminder(id: Long) {
        val identifiers = listOf(id.toString())
        center.removePendingNotificationRequestsWithIdentifiers(identifiers)
    }
}
