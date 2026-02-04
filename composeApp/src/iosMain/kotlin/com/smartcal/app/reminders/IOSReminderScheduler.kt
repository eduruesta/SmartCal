package com.smartcal.app.reminders

import platform.UserNotifications.*
import platform.Foundation.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlinx.datetime.*
import com.smartcal.app.utils.NotificationStrings

class IOSReminderScheduler : ReminderScheduler {

    override suspend fun schedule(instance: ReminderInstance) {
        // Formatear información detallada del evento
        val eventDetails = formatEventDetails(
            instance.eventStartUtcMillis,
            instance.eventEndUtcMillis,
            instance.eventTimeZone,
            instance.isAllDay
        )
        val detailedBody = "${instance.body}\n$eventDetails"
        
        // Configurar categoría con acciones si es necesario
        ensureNotificationCategory(instance.deeplink != null)
        
        val content = UNMutableNotificationContent().apply {
            setTitle(instance.title)
            setBody(detailedBody)
            setSound(UNNotificationSound.defaultSound)
            setCategoryIdentifier(if (instance.deeplink != null) "CALENDAR_REMINDER_WITH_JOIN" else "CALENDAR_REMINDER")
            
            // Agregar datos adicionales
            val userInfo = mutableMapOf<Any?, Any?>()
            userInfo["eventId"] = instance.eventId
            userInfo["notificationId"] = instance.notificationId.toString()
            userInfo["eventStartUtcMillis"] = instance.eventStartUtcMillis.toString()
            userInfo["eventEndUtcMillis"] = (instance.eventEndUtcMillis ?: -1L).toString()
            userInfo["eventTimeZone"] = instance.eventTimeZone
            userInfo["isAllDay"] = instance.isAllDay.toString()
            instance.deeplink?.let { userInfo["deeplink"] = it }
            setUserInfo(userInfo as Map<Any?, Any?>)
        }

        val date = NSDate.dateWithTimeIntervalSince1970(instance.fireAtUtcMillis.toDouble() / 1000.0)
        val calendar = NSCalendar.currentCalendar
        val components = calendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or 
            NSCalendarUnitHour or NSCalendarUnitMinute or NSCalendarUnitSecond,
            fromDate = date
        )
        
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = components,
            repeats = false
        )

        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = instance.notificationId.toString(),
            content = content,
            trigger = trigger
        )

        suspendCancellableCoroutine<Unit> { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Error scheduling notification: ${error.localizedDescription}")
                }
                continuation.resume(Unit)
            }
        }
    }

    override suspend fun cancelByEvent(eventId: String, instances: List<ReminderInstance>) {
        val identifiers = instances.map { it.notificationId.toString() }
        UNUserNotificationCenter.currentNotificationCenter().apply {
            removePendingNotificationRequestsWithIdentifiers(identifiers)
            removeDeliveredNotificationsWithIdentifiers(identifiers)
        }
    }

    companion object {
        suspend fun requestPermissions(): Boolean {
            return suspendCancellableCoroutine { continuation ->
                UNUserNotificationCenter.currentNotificationCenter()
                    .requestAuthorizationWithOptions(
                        UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                    ) { granted, error ->
                        if (error != null) {
                            println("Error requesting notification permissions: ${error.localizedDescription}")
                        }
                        continuation.resume(granted)
                    }
            }
        }
        
        suspend fun checkPermissions(): UNAuthorizationStatus {
            return suspendCancellableCoroutine { continuation ->
                UNUserNotificationCenter.currentNotificationCenter()
                    .getNotificationSettingsWithCompletionHandler { settings ->
                        continuation.resume(settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined)
                    }
            }
        }
    }
    
    private suspend fun ensureNotificationCategory(hasJoinLink: Boolean) {
        val viewAction = UNNotificationAction.actionWithIdentifier(
            identifier = "VIEW_EVENT",
            title = NotificationStrings.getViewEventAction(),
            options = UNNotificationActionOptionForeground
        )
        
        val actions = mutableListOf(viewAction)
        
        if (hasJoinLink) {
            val joinAction = UNNotificationAction.actionWithIdentifier(
                identifier = "JOIN_MEETING",
                title = NotificationStrings.getJoinMeetingAction(),
                options = UNNotificationActionOptionForeground
            )
            actions.add(joinAction)
        }
        
        val categoryWithJoin = UNNotificationCategory.categoryWithIdentifier(
            identifier = "CALENDAR_REMINDER_WITH_JOIN",
            actions = actions,
            intentIdentifiers = emptyList<String>(),
            options = 0u
        )
        
        val categoryBasic = UNNotificationCategory.categoryWithIdentifier(
            identifier = "CALENDAR_REMINDER",
            actions = listOf(viewAction),
            intentIdentifiers = emptyList<String>(),
            options = 0u
        )
        
        UNUserNotificationCenter.currentNotificationCenter().setNotificationCategories(
            setOf(categoryWithJoin, categoryBasic)
        )
    }

    private fun formatEventDetails(
        eventStartUtcMillis: Long, 
        eventEndUtcMillis: Long?, 
        eventTimeZone: String, 
        isAllDay: Boolean
    ): String {
        return try {
            val startDate = NSDate.dateWithTimeIntervalSince1970(eventStartUtcMillis.toDouble() / 1000.0)
            
            if (isAllDay) {
                val dateFormatter = NSDateFormatter().apply {
                    setDateFormat("EEEE, d 'de' MMMM")
                    setLocale(NSLocale.localeWithLocaleIdentifier("es_ES"))
                }
                dateFormatter.stringFromDate(startDate)
            } else {
                val timeFormatter = NSDateFormatter().apply {
                    setDateFormat("HH:mm")
                }
                val dateFormatter = NSDateFormatter().apply {
                    setDateFormat("EEEE, d 'de' MMMM")
                    setLocale(NSLocale.localeWithLocaleIdentifier("es_ES"))
                }
                
                val startTime = timeFormatter.stringFromDate(startDate)
                
                val endTime = if (eventEndUtcMillis != null && eventEndUtcMillis > 0) {
                    val endDate = NSDate.dateWithTimeIntervalSince1970(eventEndUtcMillis.toDouble() / 1000.0)
                    " - ${timeFormatter.stringFromDate(endDate)}"
                } else ""
                
                "${dateFormatter.stringFromDate(startDate)}\n$startTime$endTime"
            }
        } catch (e: Exception) {
            "Evento programado"
        }
    }
}