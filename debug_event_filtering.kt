import kotlinx.datetime.*
import com.smartcal.app.models.*
import com.smartcal.app.utils.*

// Test data structures matching the real models
fun main() {
    println("[DEBUG_LOG] Testing event filtering logic")
    
    val systemZone = TimeZone.currentSystemDefault()
    val today = Clock.System.now().toLocalDateTime(systemZone).date
    
    // Create test events similar to real calendar events
    val testEvents = listOf(
        // Event with dateTime (most common format)
        Event(
            id = "event1",
            summary = "Meeting with dateTime",
            start = EventDateTime(dateTime = "2025-10-17T10:00:00Z"),
            end = EventDateTime(dateTime = "2025-10-17T11:00:00Z")
        ) to Calendar(id = "cal1", summary = "Calendar 1"),
        
        // Event with date only (all-day event)
        Event(
            id = "event2",
            summary = "All day event",
            start = EventDateTime(date = "2025-10-17"),
            end = EventDateTime(date = "2025-10-17")
        ) to Calendar(id = "cal2", summary = "Calendar 2"),
        
        // Event with malformed dateTime
        Event(
            id = "event3",
            summary = "Event with bad dateTime",
            start = EventDateTime(dateTime = "bad-datetime-format"),
            end = EventDateTime(dateTime = "2025-10-17T12:00:00Z")
        ) to Calendar(id = "cal3", summary = "Calendar 3"),
        
        // Event with null start
        Event(
            id = "event4",
            summary = "Event with null start",
            start = null,
            end = EventDateTime(dateTime = "2025-10-17T13:00:00Z")
        ) to Calendar(id = "cal4", summary = "Calendar 4"),
        
        // Event from yesterday
        Event(
            id = "event5",
            summary = "Yesterday's event",
            start = EventDateTime(dateTime = "2025-10-16T14:00:00Z"),
            end = EventDateTime(dateTime = "2025-10-16T15:00:00Z")
        ) to Calendar(id = "cal5", summary = "Calendar 5"),
        
        // Event from next month
        Event(
            id = "event6", 
            summary = "Next month event",
            start = EventDateTime(dateTime = "2025-11-15T16:00:00Z"),
            end = EventDateTime(dateTime = "2025-11-15T17:00:00Z")
        ) to Calendar(id = "cal6", summary = "Calendar 6")
    )
    
    println("[DEBUG_LOG] Created ${testEvents.size} test events")
    
    // Test eventStartInstant function
    println("\n[DEBUG_LOG] Testing eventStartInstant function:")
    testEvents.forEachIndexed { index, (event, calendar) ->
        val startInstant = eventStartInstant(event, systemZone)
        println("  Event ${index + 1} (${event.summary}): startInstant = $startInstant")
        if (startInstant == null) {
            println("    ❌ WARNING: eventStartInstant returned null!")
            println("    Event start: ${event.start}")
        }
    }
    
    // Test EventFilteringHelper with DAY filter
    println("\n[DEBUG_LOG] Testing DAY filter for today ($today):")
    val dayFilteredEvents = testFilterEvents(testEvents, today.year, today.monthNumber, today, EventFilterType.DAY, systemZone)
    println("  Filtered events: ${dayFilteredEvents.size}")
    dayFilteredEvents.forEach { (event, _) ->
        println("    - ${event.summary}")
    }
    
    // Test EventFilteringHelper with WEEK filter
    println("\n[DEBUG_LOG] Testing WEEK filter for today ($today):")
    val weekFilteredEvents = testFilterEvents(testEvents, today.year, today.monthNumber, today, EventFilterType.WEEK, systemZone)
    println("  Filtered events: ${weekFilteredEvents.size}")
    weekFilteredEvents.forEach { (event, _) ->
        println("    - ${event.summary}")
    }
    
    // Test EventFilteringHelper with MONTH filter
    println("\n[DEBUG_LOG] Testing MONTH filter for today ($today):")
    val monthFilteredEvents = testFilterEvents(testEvents, today.year, today.monthNumber, today, EventFilterType.MONTH, systemZone)
    println("  Filtered events: ${monthFilteredEvents.size}")
    monthFilteredEvents.forEach { (event, _) ->
        println("    - ${event.summary}")
    }
    
    // Test EventSeparationHelper
    println("\n[DEBUG_LOG] Testing EventSeparationHelper:")
    val currentTime = Clock.System.now()
    val (pastEvents, upcomingEvents) = testSeparateEvents(dayFilteredEvents, currentTime, systemZone)
    println("  Past events: ${pastEvents.size}")
    pastEvents.forEach { (event, _) ->
        println("    - ${event.summary}")
    }
    println("  Upcoming events: ${upcomingEvents.size}")
    upcomingEvents.forEach { (event, _) ->
        println("    - ${event.summary}")
    }
}

// Copy of EventFilteringHelper.filterEvents for testing
fun testFilterEvents(
    allEvents: List<Pair<Event, Calendar>>,
    year: Int,
    month: Int,
    selectedDate: LocalDate,
    filterType: EventFilterType,
    systemZone: TimeZone
): List<Pair<Event, Calendar>> {
    val (startDate, endDate) = when (filterType) {
        EventFilterType.DAY -> selectedDate to selectedDate
        EventFilterType.WEEK -> {
            val weekStart = selectedDate.minus(DatePeriod(days = selectedDate.dayOfWeek.ordinal))
            val weekEnd = weekStart.plus(DatePeriod(days = 6))
            weekStart to weekEnd
        }
        EventFilterType.MONTH -> {
            val monthStart = LocalDate(year, month, 1)
            val monthEnd = LocalDate(year, month, daysInMonth(year, month))
            monthStart to monthEnd
        }
    }
    
    println("[DEBUG_LOG] Filter date range: $startDate to $endDate")

    return allEvents
        .mapNotNull { (event, calendar) ->
            val startInstant = eventStartInstant(event, systemZone)
            if (startInstant == null) {
                println("[DEBUG_LOG] Filtering out event '${event.summary}' - null startInstant")
                null
            } else {
                Triple(event, calendar, startInstant)
            }
        }
        .filter { (event, _, inst) ->
            val date = inst.toLocalDateTime(systemZone).date
            val inRange = date >= startDate && date <= endDate
            if (!inRange) {
                println("[DEBUG_LOG] Filtering out event '${event.summary}' - date $date not in range $startDate to $endDate")
            }
            inRange
        }
        .sortedBy { it.third }
        .map { (event, calendar, _) -> event to calendar }
}

// Copy of EventSeparationHelper.separateEvents for testing
fun testSeparateEvents(
    filteredEvents: List<Pair<Event, Calendar>>,
    currentTime: Instant,
    systemZone: TimeZone
): Pair<List<Pair<Event, Calendar>>, List<Pair<Event, Calendar>>> {
    val (past, upcoming) = filteredEvents.partition { (event, _) ->
        val eventEndTime = eventEndInstant(event, systemZone) ?: eventStartInstant(event, systemZone)
        val isPast = eventEndTime?.let { it < currentTime } ?: false
        println("[DEBUG_LOG] Event '${event.summary}' - endTime: $eventEndTime, isPast: $isPast")
        isPast
    }

    // Past events: most recent first (descending), limit to 50 for performance
    val sortedPastEvents = past.sortedByDescending { (event, _) ->
        eventStartInstant(event, systemZone)
    }.take(50)

    // Upcoming events: closest first (ascending) - already in correct order
    val sortedUpcomingEvents = upcoming

    return sortedPastEvents to sortedUpcomingEvents
}

// Enum for filter types
enum class EventFilterType {
    DAY, WEEK, MONTH
}