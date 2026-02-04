package com.smartcal.app.reminders

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SettingsReminderIndex(
    private val settings: Settings
) : ReminderIndex {

    private val json = Json { ignoreUnknownKeys = true }
    
    private fun key(eventId: String) = "rem_idx_$eventId"
    private fun eventListKey() = "rem_events_list"

    override suspend fun getByEvent(eventId: String): List<ReminderInstance> {
        val jsonString = settings.getStringOrNull(key(eventId)) ?: return emptyList()
        return try {
            json.decodeFromString<List<ReminderInstance>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun put(eventId: String, list: List<ReminderInstance>) {
        if (list.isEmpty()) {
            remove(eventId)
            return
        }
        
        val jsonString = json.encodeToString(list)
        settings.putString(key(eventId), jsonString)
        
        // Mantener lista de eventos con recordatorios
        val events = getAllEvents().toMutableList()
        if (!events.contains(eventId)) {
            events.add(eventId)
            settings.putString(eventListKey(), json.encodeToString(events))
        }
    }

    override suspend fun remove(eventId: String) {
        settings.remove(key(eventId))
        
        // Remover de la lista de eventos
        val events = getAllEvents().toMutableList()
        if (events.remove(eventId)) {
            if (events.isEmpty()) {
                settings.remove(eventListKey())
            } else {
                settings.putString(eventListKey(), json.encodeToString(events))
            }
        }
    }

    override suspend fun getAllEvents(): List<String> {
        val jsonString = settings.getStringOrNull(eventListKey()) ?: return emptyList()
        return try {
            json.decodeFromString<List<String>>(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun clear() {
        val events = getAllEvents()
        events.forEach { eventId ->
            settings.remove(key(eventId))
        }
        settings.remove(eventListKey())
    }
}