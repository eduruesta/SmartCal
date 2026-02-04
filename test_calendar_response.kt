// Simple test to verify calendar response parsing
import com.smartcal.app.models.CalendarsResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

fun main() {
    val jsonResponse = """
    {
      "success": true,
      "calendars": [
        {
          "kind": "calendar#calendarListEntry",
          "etag": "\"1666217101142000\"",
          "id": "es.ar#holiday@group.v.calendar.google.com",
          "summary": "Festivos en Argentina",
          "description": "Festivos y celebraciones de Argentina",
          "timeZone": "America/Argentina/Buenos_Aires",
          "accessRole": "reader",
          "selected": true,
          "primary": null,
          "backgroundColor": "#16a765",
          "foregroundColor": "#000000"
        },
        {
          "kind": "calendar#calendarListEntry",
          "etag": "\"1751298237045407\"",
          "id": "bebiruesta90@gmail.com",
          "summary": "bebiruesta90@gmail.com",
          "description": null,
          "timeZone": "America/Argentina/Buenos_Aires",
          "accessRole": "owner",
          "selected": true,
          "primary": true,
          "backgroundColor": "#9a9cff",
          "foregroundColor": "#000000"
        }
      ],
      "userEmail": "bebiruesta90@gmail.com",
      "timestamp": "2025-09-14T14:50:46.530511Z"
    }
    """
    
    try {
        val response = Json.decodeFromString<CalendarsResponse>(jsonResponse)
        println("[DEBUG_LOG] Success: ${response.success}")
        println("[DEBUG_LOG] Calendar count: ${response.calendars?.size ?: 0}")
        response.calendars?.forEach { calendar ->
            println("[DEBUG_LOG] Calendar: ${calendar.summary} (${calendar.id})")
        }
        println("[DEBUG_LOG] Parsing successful - calendars should now display in UI")
    } catch (e: Exception) {
        println("[DEBUG_LOG] Error parsing response: ${e.message}")
    }
}