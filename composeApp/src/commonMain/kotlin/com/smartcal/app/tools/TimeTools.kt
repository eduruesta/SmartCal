package com.smartcal.app.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.Tool
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Time tools for the SmartCal agent compatible with Koog 0.6.1.
 *
 * Exposes a set of SimpleTool<TArgs> that return a String result for the LLM.
 */
object TimeTools {

    private val clock = Clock.System

    // Helpers
    private fun atStartOfDay(d: LocalDate) = d.atTime(0, 0, 0)
    private fun atEndOfDay(d: LocalDate) = d.atTime(23, 59, 59)
    private fun toRfc3339At(ldt: LocalDateTime, zone: TimeZone): String = ldt.toInstant(zone).toString()
    private fun resolveTz(id: String): TimeZone =
        if (id.isNotBlank()) runCatching { TimeZone.of(id) }.getOrElse { TimeZone.currentSystemDefault() }
        else TimeZone.currentSystemDefault()

    // --------------------------------------------------------------------------------------------
    // getCurrentDateTime
    // --------------------------------------------------------------------------------------------
    @Serializable
    data class GetCurrentDateTimeArgs(
        // Timezone ID like "UTC" or "America/Argentina/Buenos_Aires". Empty → system default.
        val timezone: String = ""
    )

    val getCurrentDateTime: SimpleTool<GetCurrentDateTimeArgs> =
        object : SimpleTool<GetCurrentDateTimeArgs>(
            GetCurrentDateTimeArgs.serializer(),
            "getCurrentDateTime",
            "Get the current datetime in a timezone with details (date, time, day-of-week)."
        ) {
            override suspend fun execute(args: GetCurrentDateTimeArgs): String {
                val tz = resolveTz(args.timezone)
                val now = clock.now()
                val local = now.toLocalDateTime(tz)
                return "Current datetime - UTC: ${'$'}now, Local date: ${'$'}{local.date}, Local time: ${'$'}{local.time}, " +
                    "Timezone: ${'$'}{tz.id}, Day: ${'$'}{local.dayOfWeek}"
            }
        }

    // --------------------------------------------------------------------------------------------
    // getCurrentTimeForCalendar
    // --------------------------------------------------------------------------------------------
    @Serializable
    data class GetCurrentTimeForCalendarArgs(
        val timezone: String = ""
    )

    val getCurrentTimeForCalendar: SimpleTool<GetCurrentTimeForCalendarArgs> =
        object : SimpleTool<GetCurrentTimeForCalendarArgs>(
            GetCurrentTimeForCalendarArgs.serializer(),
            "getCurrentTimeForCalendar",
            "Return RFC3339 strings for start/end of today and current time in the user's timezone."
        ) {
            override suspend fun execute(args: GetCurrentTimeForCalendarArgs): String {
                val tz = if (args.timezone.isNotBlank()) {
                    runCatching { TimeZone.of(args.timezone) }.getOrElse { TimeZone.currentSystemDefault() }
                } else TimeZone.currentSystemDefault()
                val now = clock.now()
                val local = now.toLocalDateTime(tz)
                val start = atStartOfDay(local.date)
                val end = atEndOfDay(local.date)
                return "Calendar times - Timezone: ${'$'}{tz.id}, Start of today: ${'$'}{toRfc3339At(start, tz)}, " +
                    "End of today: ${'$'}{toRfc3339At(end, tz)}, Now (RFC3339): ${'$'}{toRfc3339At(local, tz)}, Now UTC: ${'$'}now"
            }
        }

    // --------------------------------------------------------------------------------------------
    // findNextDayOfWeek
    // --------------------------------------------------------------------------------------------
    @Serializable
    data class FindNextDayOfWeekArgs(
        val dayOfWeek: String, // MONDAY..SUNDAY
        val timezone: String = ""
    )

    val findNextDayOfWeek: SimpleTool<FindNextDayOfWeekArgs> =
        object : SimpleTool<FindNextDayOfWeekArgs>(
            FindNextDayOfWeekArgs.serializer(),
            "findNextDayOfWeek",
            "Find the next date for a given day of week and return RFC3339 bounds for that day."
        ) {
            override suspend fun execute(args: FindNextDayOfWeekArgs): String {
                val tz = if (args.timezone.isNotBlank()) {
                    runCatching { TimeZone.of(args.timezone) }.getOrElse { TimeZone.currentSystemDefault() }
                } else TimeZone.currentSystemDefault()
                val targetDow = runCatching { DayOfWeek.valueOf(args.dayOfWeek.uppercase()) }.getOrNull()
                    ?: return "Error: Invalid dayOfWeek '${'$'}{args.dayOfWeek}'"

                val now = clock.now()
                val local = now.toLocalDateTime(tz)
                val today = local.date
                val diff = ((targetDow.ordinal - today.dayOfWeek.ordinal + 7) % 7).let { if (it == 0) 7 else it }
                val targetDate = today.plus(diff, DateTimeUnit.DAY)
                val start = toRfc3339At(atStartOfDay(targetDate), tz)
                val end = toRfc3339At(atEndOfDay(targetDate), tz)
                return "Next ${'$'}{targetDow.name}: ${'$'}targetDate (${ '$'}{targetDate.dayOfWeek}), ${'$'}diff days from today. " +
                    "Start: ${'$'}start, End: ${'$'}end, Timezone: ${'$'}{tz.id}"
            }
        }

    // --------------------------------------------------------------------------------------------
    // get_named_range - natural language date ranges (mañana, esta semana, próxima semana, etc.)
    // --------------------------------------------------------------------------------------------
    @Serializable
    data class GetNamedRangeArgs(
        val name: String, // today, tomorrow, yesterday, this_week, next_week, this_month, next_month
        val timezone: String = ""
    )

    val getNamedRange: SimpleTool<GetNamedRangeArgs> =
        object : SimpleTool<GetNamedRangeArgs>(
            GetNamedRangeArgs.serializer(),
            "get_named_range",
            "Return RFC3339 timeMin/timeMax for natural date ranges. Use for: mañana/tomorrow, hoy/today, este viernes/this_friday, próximo viernes/next_friday, esta semana, próxima semana, este mes, próximo mes. Values: today, tomorrow, yesterday, this_friday, next_friday, this_week, next_week, this_month, next_month."
        ) {
            override suspend fun execute(args: GetNamedRangeArgs): String {
                val tz = if (args.timezone.isNotBlank()) {
                    runCatching { TimeZone.of(args.timezone) }.getOrElse { TimeZone.currentSystemDefault() }
                } else TimeZone.currentSystemDefault()
                val now = clock.now().toLocalDateTime(tz)
                val normalized = args.name.lowercase().replace(" ", "_")
                    .replace("mañana", "tomorrow").replace("ayer", "yesterday").replace("hoy", "today")
                    .replace("esta_semana", "this_week").replace("próxima_semana", "next_week")
                    .replace("proxima_semana", "next_week")
                    .replace("este_mes", "this_month").replace("próximo_mes", "next_month").replace("proximo_mes", "next_month")
                    .replace("este_viernes", "this_friday").replace("próximo_viernes", "next_friday").replace("proximo_viernes", "next_friday")
                val (start, end) = when (normalized) {
                    "today" -> atStartOfDay(now.date) to atEndOfDay(now.date)
                    "tomorrow" -> {
                        val tomorrow = now.date.plus(1, DateTimeUnit.DAY)
                        atStartOfDay(tomorrow) to atEndOfDay(tomorrow)
                    }
                    "yesterday", "ayer" -> {
                        val yesterday = now.date.plus(-1, DateTimeUnit.DAY)
                        atStartOfDay(yesterday) to atEndOfDay(yesterday)
                    }
                    "this_week", "esta_semana" -> {
                        val monday = now.date.plus(-now.date.dayOfWeek.ordinal, DateTimeUnit.DAY)
                        val sunday = monday.plus(6, DateTimeUnit.DAY)
                        atStartOfDay(monday) to atEndOfDay(sunday)
                    }
                    "next_week", "próxima_semana" -> {
                        val mondayNext = now.date.plus(-now.date.dayOfWeek.ordinal, DateTimeUnit.DAY).plus(7, DateTimeUnit.DAY)
                        val sundayNext = mondayNext.plus(6, DateTimeUnit.DAY)
                        atStartOfDay(mondayNext) to atEndOfDay(sundayNext)
                    }
                    "this_month", "este_mes" -> {
                        val first = LocalDate(now.date.year, now.date.monthNumber, 1)
                        val last = first.plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY)
                        atStartOfDay(first) to atEndOfDay(last)
                    }
                    "next_month", "próximo_mes" -> {
                        val firstNext = LocalDate(now.date.year, now.date.monthNumber, 1).plus(1, DateTimeUnit.MONTH)
                        val lastNext = firstNext.plus(1, DateTimeUnit.MONTH).plus(-1, DateTimeUnit.DAY)
                        atStartOfDay(firstNext) to atEndOfDay(lastNext)
                    }
                    "this_friday", "este_viernes" -> {
                        val today = now.date
                        val friday = DayOfWeek.FRIDAY
                        val targetDate = if (today.dayOfWeek == friday) today
                        else {
                            val diff = ((friday.ordinal - today.dayOfWeek.ordinal + 7) % 7).let { if (it == 0) 7 else it }
                            today.plus(diff, DateTimeUnit.DAY)
                        }
                        atStartOfDay(targetDate) to atEndOfDay(targetDate)
                    }
                    "next_friday", "próximo_viernes" -> {
                        val today = now.date
                        val friday = DayOfWeek.FRIDAY
                        val thisFriday = if (today.dayOfWeek == friday) today
                        else {
                            val diff = ((friday.ordinal - today.dayOfWeek.ordinal + 7) % 7).let { if (it == 0) 7 else it }
                            today.plus(diff, DateTimeUnit.DAY)
                        }
                        val nextFriday = thisFriday.plus(7, DateTimeUnit.DAY)
                        atStartOfDay(nextFriday) to atEndOfDay(nextFriday)
                    }
                    else -> return "Error: Unsupported range '${args.name}'. Use: today, tomorrow, yesterday, this_friday, next_friday, this_week, next_week, this_month, next_month."
                }
                return "Named range '${args.name}' -> timeMin: ${toRfc3339At(start, tz)}, timeMax: ${toRfc3339At(end, tz)}, tz: ${tz.id}"
            }
        }

    // --------------------------------------------------------------------------------------------
    // getEventTimes (hour, minute, dayOffset, durationMinutes) → start/end RFC3339
    // --------------------------------------------------------------------------------------------
    @Serializable
    data class GetEventTimesArgs(
        val hour: Int,                 // 0..23
        val minute: Int,               // 0..59
        val dayOffset: Int = 0,        // 0=today, 1=tomorrow, etc.
        val durationMinutes: Int = 60, // event duration
        val timezone: String = ""
    )

    val getEventTimes: SimpleTool<GetEventTimesArgs> =
        object : SimpleTool<GetEventTimesArgs>(
            GetEventTimesArgs.serializer(),
            "getEventTimes",
            "Given hour/minute/dayOffset/duration, compute start/end RFC3339 in the user's timezone."
        ) {
            override suspend fun execute(args: GetEventTimesArgs): String {
                val tz = resolveTz(args.timezone)
                val now = clock.now().toLocalDateTime(tz)
                val baseDate = now.date.plus(args.dayOffset, DateTimeUnit.DAY)
                val startLdt = baseDate.atTime(args.hour, args.minute)
                val endInstant = startLdt.toInstant(tz).plus(args.durationMinutes.toLong(), DateTimeUnit.MINUTE)
                val endLdt = endInstant.toLocalDateTime(tz)
                return "start: ${'$'}{toRfc3339At(startLdt, tz)}, end: ${'$'}{toRfc3339At(endLdt, tz)}, tz: ${'$'}{tz.id}"
            }
        }

    // --------------------------------------------------------------------------------------------
    // getDateRange (year, month, day) → timeMin/timeMax RFC3339 for that full day
    // --------------------------------------------------------------------------------------------
    @Serializable
    data class GetDateRangeArgs(
        val year: Int,
        val month: Int,   // 1-12
        val day: Int,
        val timezone: String = ""
    )

    val getDateRange: SimpleTool<GetDateRangeArgs> =
        object : SimpleTool<GetDateRangeArgs>(
            GetDateRangeArgs.serializer(),
            "getDateRange",
            "Get RFC3339 timeMin and timeMax for a specific calendar date (year, month, day). Use for queries like 'February 20' or '20 de febrero'. Returns the full-day range for list-events."
        ) {
            override suspend fun execute(args: GetDateRangeArgs): String {
                val tz = resolveTz(args.timezone)
                val date = LocalDate(args.year, args.month, args.day)
                val start = atStartOfDay(date)
                val end = atEndOfDay(date)
                return "Date: ${'$'}date -> timeMin: ${'$'}{toRfc3339At(start, tz)}, timeMax: ${'$'}{toRfc3339At(end, tz)}, tz: ${'$'}{tz.id}"
            }
        }

    /**
     * Return all tools as a list for easy registration in ToolRegistry: `tools(TimeTools.asTools())`.
     */
    fun asTools(): List<Tool<*, *>> = listOf(
        getCurrentDateTime,
        getCurrentTimeForCalendar,
        findNextDayOfWeek,
        getNamedRange,
        getEventTimes,
        getDateRange
    )
}
