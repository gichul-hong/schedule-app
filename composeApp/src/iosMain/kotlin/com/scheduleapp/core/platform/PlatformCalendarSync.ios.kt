package com.scheduleapp.core.platform

import com.scheduleapp.domain.calendar.model.ScheduleEvent
import kotlinx.cinterop.ExperimentalForeignApi
import platform.EventKit.EKAuthorizationStatusAuthorized
import platform.EventKit.EKEntityTypeEvent
import platform.EventKit.EKEvent
import platform.EventKit.EKEventStore
import platform.EventKit.EKSpanThisEvent
import platform.Foundation.NSDate
import platform.Foundation.dateWithTimeIntervalSince1970
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

actual class PlatformCalendarSync actual constructor() {
    private val eventStore = EKEventStore()

    actual fun checkPermission(): Boolean {
        val status = EKEventStore.authorizationStatusForEntityType(EKEntityTypeEvent)
        return status == EKAuthorizationStatusAuthorized || status.toInt() == 4 /* FullAccess in iOS 17+ */
    }

    actual suspend fun requestPermission(): Boolean = suspendCoroutine { cont ->
        eventStore.requestAccessToEntityType(EKEntityTypeEvent) { granted, _ ->
            cont.resume(granted)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun syncEvent(event: ScheduleEvent): Boolean {
        if (!checkPermission()) {
            val granted = requestPermission()
            if (!granted) return false
        }

        return try {
            val ekEvent = if (!event.externalEventId.isNullOrEmpty()) {
                eventStore.eventWithIdentifier(event.externalEventId) ?: EKEvent.eventWithEventStore(eventStore)
            } else {
                EKEvent.eventWithEventStore(eventStore)
            }

            ekEvent.title = event.title
            ekEvent.startDate = NSDate.dateWithTimeIntervalSince1970(event.startTime / 1000.0)
            ekEvent.endDate = NSDate.dateWithTimeIntervalSince1970(event.endTime / 1000.0)
            ekEvent.allDay = event.isAllDay
            ekEvent.calendar = eventStore.defaultCalendarForNewEvents
            ekEvent.notes = "차계부 및 일정 앱 연동: ${event.category}"

            eventStore.saveEvent(ekEvent, span = EKSpanThisEvent, error = null)
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    actual suspend fun deleteEvent(externalId: String): Boolean {
        if (!checkPermission()) return false
        return try {
            val ekEvent = eventStore.eventWithIdentifier(externalId) ?: return false
            eventStore.removeEvent(ekEvent, span = EKSpanThisEvent, error = null)
        } catch (e: Exception) {
            false
        }
    }
}
