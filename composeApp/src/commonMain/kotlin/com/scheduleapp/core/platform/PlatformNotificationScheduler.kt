package com.scheduleapp.core.platform

expect class PlatformNotificationScheduler() {
    fun scheduleReminder(id: Long, title: String, message: String, triggerEpochMillis: Long)
    fun cancelReminder(id: Long)
    fun checkNotificationPermission(): Boolean
    suspend fun requestNotificationPermission(): Boolean
}
