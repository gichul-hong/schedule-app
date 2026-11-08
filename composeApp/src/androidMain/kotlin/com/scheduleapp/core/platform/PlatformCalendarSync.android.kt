package com.scheduleapp.core.platform

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.scheduleapp.domain.calendar.model.ScheduleEvent
import java.util.TimeZone

actual class PlatformCalendarSync actual constructor() {
    private val context get() = PlatformContextHolder.appContext

    actual fun checkPermission(): Boolean {
        val ctx = context ?: return false
        return ContextCompat.checkSelfPermission(
            ctx,
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun requestPermission(): Boolean {
        return checkPermission()
    }

    actual suspend fun syncEvent(event: ScheduleEvent): Boolean {
        val ctx = context ?: return false
        if (!checkPermission()) return false

        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, event.title)
                put(CalendarContract.Events.DTSTART, event.startTime)
                put(CalendarContract.Events.DTEND, event.endTime)
                put(CalendarContract.Events.ALL_DAY, if (event.isAllDay) 1 else 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.DESCRIPTION, "차계부 및 일정 앱 연동: ${event.category}")
            }

            if (!event.externalEventId.isNullOrEmpty()) {
                val uri = ContentUris.withAppendedId(
                    CalendarContract.Events.CONTENT_URI,
                    event.externalEventId.toLongOrNull() ?: return false
                )
                val rows = ctx.contentResolver.update(uri, values, null, null)
                rows > 0
            } else {
                val uri = ctx.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                uri != null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    actual suspend fun deleteEvent(externalId: String): Boolean {
        val ctx = context ?: return false
        if (!checkPermission()) return false

        return try {
            val id = externalId.toLongOrNull() ?: return false
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id)
            val rows = ctx.contentResolver.delete(uri, null, null)
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
