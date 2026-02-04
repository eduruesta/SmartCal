import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.DatePeriod

// Test data structures (simplified versions of the real models)
data class TestEvent(
    val id: String,
    val summary: String?,
    val start: TestDateTime?,
    val end: TestDateTime?
)

data class TestDateTime(
    val dateTime: String?,
    val date: String?
)

data class TestCalendar(
    val id: String,
    val backgroundColor: String?
)

// Test function to simulate the week view filtering logic
fun testWeekViewOverlapIssue() {
    println("[DEBUG_LOG] Testing week view overlap issue")
    
    val systemZone = TimeZone.currentSystemDefault()
    val selectedDate = LocalDate(2024, 10, 16) // Wednesday
    
    // Calculate week start (Monday)
    val weekStart = selectedDate.minus(DatePeriod(days = selectedDate.dayOfWeek.ordinal))
    val weekDays = (0..6).map { weekStart.plus(DatePeriod(days = it)) }
    
    println("[DEBUG_LOG] Week start: $weekStart")
    println("[DEBUG_LOG] Week days: $weekDays")
    
    // Create test events - two events on the same day at the same time
    val testDay = LocalDate(2024, 10, 16) // Wednesday
    val eventsWithCalendars = listOf(
        TestEvent(
            id = "event1",
            summary = "Meeting 1",
            start = TestDateTime(dateTime = "2024-10-16T10:00:00Z", date = null),
            end = TestDateTime(dateTime = "2024-10-16T11:00:00Z", date = null)
        ) to TestCalendar(id = "cal1", backgroundColor = "#FF0000"),
        
        TestEvent(
            id = "event2", 
            summary = "Meeting 2",
            start = TestDateTime(dateTime = "2024-10-16T10:00:00Z", date = null),
            end = TestDateTime(dateTime = "2024-10-16T11:00:00Z", date = null)
        ) to TestCalendar(id = "cal2", backgroundColor = "#00FF00")
    )
    
    println("[DEBUG_LOG] Created ${eventsWithCalendars.size} test events")
    
    // Simulate the week events filtering
    val weekEvents = eventsWithCalendars.filter { (event, _) ->
        val eventDate = event.start?.dateTime?.let { 
            runCatching { kotlinx.datetime.Instant.parse(it).toLocalDateTime(systemZone).date }.getOrNull()
        }
        eventDate != null && eventDate in weekDays
    }
    
    println("[DEBUG_LOG] Week events found: ${weekEvents.size}")
    
    // For each day, filter events (this is what happens in the WeekViewCalendarSection)
    weekDays.forEach { day ->
        val dayEvents = weekEvents.filter { (event, _) ->
            val eventDate = event.start?.dateTime?.let { 
                runCatching { kotlinx.datetime.Instant.parse(it).toLocalDateTime(systemZone).date }.getOrNull()
            }
            eventDate == day
        }
        println("[DEBUG_LOG] Day $day has ${dayEvents.size} events")
        dayEvents.forEach { (event, calendar) ->
            println("[DEBUG_LOG]   - ${event.summary} (${calendar.id})")
        }
    }
}

fun main() {
    testWeekViewOverlapIssue()
}