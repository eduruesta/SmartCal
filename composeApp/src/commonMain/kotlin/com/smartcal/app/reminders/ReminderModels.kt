package com.smartcal.app.reminders

import kotlinx.serialization.Serializable
import com.smartcal.app.models.EventReminders
import kotlinx.datetime.*
import com.smartcal.app.utils.NotificationStrings

@Serializable
data class EventLite(
    val eventId: String,
    val calendarId: String,
    val title: String,
    val startsAtUtcMillis: Long,
    val endsAtUtcMillis: Long? = null,
    val timeZone: String,
    val isAllDay: Boolean,
    val meetDeeplink: String? = null,
    val useDefaultReminders: Boolean,
    val overridesMinutes: List<Int> = emptyList()
) {
    companion object {
        fun from(event: com.smartcal.app.models.Event): EventLite? {
            val start = event.start?.dateTime ?: event.start?.date ?: return null
            val end = event.end?.dateTime ?: event.end?.date
            val timeZone = event.start?.timeZone ?: "UTC"
            val isAllDay = event.start?.date != null
            
            val startUtc = if (isAllDay) {
                // Para eventos de todo el día, usar la fecha con hora 00:00 UTC
                val localDate = LocalDate.parse(start)
                localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
            } else {
                Instant.parse(start).toEpochMilliseconds()
            }
            
            val endUtc = end?.let { endStr ->
                if (isAllDay) {
                    val localDate = LocalDate.parse(endStr)
                    localDate.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
                } else {
                    Instant.parse(endStr).toEpochMilliseconds()
                }
            }
            
            val reminders = event.reminders
            val useDefault = reminders?.useDefault == true
            val overrides = reminders?.overrides?.map { it.minutes } ?: emptyList()
            
            // Si usa recordatorios predeterminados pero no hay overrides,
            // significa que confía en los valores predeterminados del calendario.
            // Para evitar valores extremos, usamos valores razonables cuando useDefault es true.
            val actualUseDefault = useDefault
            
            return EventLite(
                eventId = event.id,
                calendarId = "primary", // Asumimos primary por defecto
                title = event.summary ?: "Sin título",
                startsAtUtcMillis = startUtc,
                endsAtUtcMillis = endUtc,
                timeZone = timeZone,
                isAllDay = isAllDay,
                meetDeeplink = event.hangoutLink ?: event.conferenceData?.entryPoints?.firstOrNull()?.uri,
                useDefaultReminders = actualUseDefault,
                overridesMinutes = if (!actualUseDefault) overrides else emptyList()
            )
        }
    }
}

typealias DefaultReminders = Map<String, List<Int>>

@Serializable
data class ReminderInstance(
    val eventId: String,
    val notificationId: Int,
    val fireAtUtcMillis: Long,
    val title: String,
    val body: String,
    val deeplink: String?,
    val eventStartUtcMillis: Long,
    val eventEndUtcMillis: Long? = null,
    val eventTimeZone: String,
    val isAllDay: Boolean
)

interface ReminderScheduler {
    suspend fun schedule(instance: ReminderInstance)
    suspend fun cancelByEvent(eventId: String, instances: List<ReminderInstance>)
}

interface ReminderIndex {
    suspend fun getByEvent(eventId: String): List<ReminderInstance>
    suspend fun put(eventId: String, list: List<ReminderInstance>)
    suspend fun remove(eventId: String)
    suspend fun getAllEvents(): List<String>
    suspend fun clear()
}

suspend fun computeInstances(
    ev: EventLite,
    calDefaults: List<Int>,
    allDayHourLocal: Int = 9
): List<ReminderInstance> {
    // Si el evento usa recordatorios predeterminados del calendario,
    // no mostrar notificaciones para evitar valores extremos
    if (ev.useDefaultReminders) {
        return emptyList()
    }
    
    val minutes = ev.overridesMinutes
    if (minutes.isEmpty()) return emptyList()

    val startUtc = if (!ev.isAllDay) ev.startsAtUtcMillis else {
        // Para eventos de todo el día, usar la hora especificada en la zona horaria del evento
        val timeZone = TimeZone.of(ev.timeZone)
        val instant = Instant.fromEpochMilliseconds(ev.startsAtUtcMillis)
        val localDateTime = instant.toLocalDateTime(timeZone)
        val adjustedDateTime = LocalDateTime(
            localDateTime.date,
            LocalTime(allDayHourLocal, 0)
        )
        adjustedDateTime.toInstant(timeZone).toEpochMilliseconds()
    }

    val now = Clock.System.now().toEpochMilliseconds()
    
    return minutes.distinct().mapNotNull { m ->
        val fireAt = startUtc - m * 60_000L
        
        // Solo crear recordatorios que:
        // 1. Sean en el futuro
        // 2. No sean más de 7 días antes del evento (para ser razonables)
        val maxAdvanceTime = 7 * 24 * 60 * 60 * 1000L // 7 días en milisegundos
        if (fireAt > now && (startUtc - fireAt) <= maxAdvanceTime) {
            ReminderInstance(
                eventId = ev.eventId,
                notificationId = (ev.eventId + "_$m").hashCode(),
                fireAtUtcMillis = fireAt,
                title = ev.title,
                body = NotificationStrings.formatReminderMessage(m),
                deeplink = ev.meetDeeplink,
                eventStartUtcMillis = ev.startsAtUtcMillis,
                eventEndUtcMillis = ev.endsAtUtcMillis,
                eventTimeZone = ev.timeZone,
                isAllDay = ev.isAllDay
            )
        } else null
    }
}

suspend fun syncRemindersForEvent(
    ev: EventLite,
    defaultsByCalendar: DefaultReminders,
    scheduler: ReminderScheduler,
    index: ReminderIndex
) {
    val defaults = defaultsByCalendar[ev.calendarId] ?: emptyList()
    val next = computeInstances(ev, defaults)
    val prev = index.getByEvent(ev.eventId)

    if (prev.isNotEmpty()) scheduler.cancelByEvent(ev.eventId, prev)
    next.forEach { scheduler.schedule(it) }

    if (next.isEmpty()) index.remove(ev.eventId) else index.put(ev.eventId, next)
}

suspend fun syncRollingWindow(
    fetchEvents: suspend (fromUtcMillis: Long, toUtcMillis: Long) -> List<EventLite>,
    defaultsByCalendar: DefaultReminders,
    scheduler: ReminderScheduler,
    index: ReminderIndex,
    weeks: Int = 2
) {
    val now = Clock.System.now().toEpochMilliseconds()
    val horizon = now + weeks * 7L * 24 * 60 * 60 * 1000
    val events = fetchEvents(now, horizon)
    events.forEach { ev -> syncRemindersForEvent(ev, defaultsByCalendar, scheduler, index) }
}