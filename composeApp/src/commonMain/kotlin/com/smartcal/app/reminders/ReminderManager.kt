package com.smartcal.app.reminders

import com.smartcal.app.models.Event
import com.smartcal.app.models.EventsResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ReminderManager(
    private val scheduler: ReminderScheduler,
    private val index: ReminderIndex,
    private val permissions: ReminderPermissions
) {
    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _hasPermissions = MutableStateFlow(false)
    val hasPermissions: StateFlow<Boolean> = _hasPermissions

    suspend fun initialize() {
        _hasPermissions.value = permissions.hasPermissions()
        _isEnabled.value = _hasPermissions.value
    }

    suspend fun requestPermissions(): Boolean {
        val granted = permissions.requestPermissions()
        _hasPermissions.value = granted
        _isEnabled.value = granted
        return granted
    }

    suspend fun openPermissionSettings() {
        permissions.openPermissionSettings()
    }

    suspend fun syncEvents(
        eventsResponse: EventsResponse,
        defaultReminders: DefaultReminders = mapOf("primary" to listOf(10)) // 10 minutos por defecto
    ) {
        if (!_isEnabled.value) return
        
        val events = eventsResponse.events ?: return
        val eventLites = events.mapNotNull { EventLite.from(it) }
        
        eventLites.forEach { eventLite ->
            syncRemindersForEvent(
                ev = eventLite,
                defaultsByCalendar = defaultReminders,
                scheduler = scheduler,
                index = index
            )
        }
    }

    suspend fun syncRollingEvents(
        fetchEvents: suspend (fromUtcMillis: Long, toUtcMillis: Long) -> List<EventLite>,
        defaultReminders: DefaultReminders = mapOf("primary" to listOf(10)),
        weeks: Int = 2
    ) {
        if (!_isEnabled.value) return
        
        syncRollingWindow(
            fetchEvents = fetchEvents,
            defaultsByCalendar = defaultReminders,
            scheduler = scheduler,
            index = index,
            weeks = weeks
        )
    }

    suspend fun cancelEventReminders(eventId: String) {
        val instances = index.getByEvent(eventId)
        if (instances.isNotEmpty()) {
            scheduler.cancelByEvent(eventId, instances)
            index.remove(eventId)
        }
    }


    suspend fun clearAllReminders() {
        val allEvents = index.getAllEvents()
        allEvents.forEach { eventId ->
            val instances = index.getByEvent(eventId)
            scheduler.cancelByEvent(eventId, instances)
        }
        index.clear()
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled && _hasPermissions.value
    }
}