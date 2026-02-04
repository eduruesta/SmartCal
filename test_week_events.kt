import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import com.smartcal.app.models.Event
import com.smartcal.app.models.Calendar
import com.smartcal.app.models.EventDateTime
import com.smartcal.app.utils.eventStartLocalDate

fun main() {
    println("[DEBUG_LOG] Testing week view overlapping events")
    
    val systemZone = TimeZone.currentSystemDefault()
    val testDate = LocalDate(2025, 10, 16) // Today's date
    
    // Create two events at the same time on the same day
    val event1 = Event(
        id = "event1",
        summary = "Meeting 1",
        start = EventDateTime(dateTime = "2025-10-16T10:00:00Z"),
        end = EventDateTime(dateTime = "2025-10-16T11:00:00Z")
    )
    
    val event2 = Event(
        id = "event2", 
        summary = "Meeting 2",
        start = EventDateTime(dateTime = "2025-10-16T10:30:00Z"),
        end = EventDateTime(dateTime = "2025-10-16T11:30:00Z")
    )
    
    val calendar1 = Calendar(
        id = "cal1",
        summary = "Calendar 1",
        backgroundColor = "#FF0000"
    )
    
    val calendar2 = Calendar(
        id = "cal2",
        summary = "Calendar 2", 
        backgroundColor = "#0000FF"
    )
    
    val eventsWithCalendars = listOf(
        event1 to calendar1,
        event2 to calendar2
    )
    
    println("[DEBUG_LOG] Created ${eventsWithCalendars.size} events")
    
    // Test event filtering for week view
    val weekStart = testDate.minus(kotlinx.datetime.DatePeriod(days = testDate.dayOfWeek.ordinal))
    val weekDays = (0..6).map { weekStart.plus(kotlinx.datetime.DatePeriod(days = it)) }
    
    println("[DEBUG_LOG] Week starts: $weekStart")
    println("[DEBUG_LOG] Week days: $weekDays")
    
    val weekEvents = eventsWithCalendars.filter { (event, _) ->
        val d = eventStartLocalDate(event, systemZone)
        d != null && d in weekDays
    }
    
    println("[DEBUG_LOG] Week events found: ${weekEvents.size}")
    weekEvents.forEach { (event, _) ->
        println("[DEBUG_LOG] - ${event.summary} at ${event.start?.dateTime}")
    }
    
    // Test day-specific filtering (what happens in WeekViewCalendarSection)
    val dayEvents = weekEvents.filter { (event, _) ->
        eventStartLocalDate(event, systemZone) == testDate
    }
    
    println("[DEBUG_LOG] Events for specific day ($testDate): ${dayEvents.size}")
    dayEvents.forEach { (event, _) ->
        println("[DEBUG_LOG] - ${event.summary} at ${event.start?.dateTime}")
    }
    
    if (dayEvents.size < 2) {
        println("[DEBUG_LOG] ERROR: Should have 2 overlapping events but only found ${dayEvents.size}")
    } else {
        println("[DEBUG_LOG] SUCCESS: Found ${dayEvents.size} overlapping events as expected")
    }
}