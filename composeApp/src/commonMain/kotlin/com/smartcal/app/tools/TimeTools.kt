package com.smartcal.app.tools

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolResult
import ai.koog.agents.core.tools.ToolResultUtils
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.offsetAt
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

/**
 * Time tools for the SmartCal agent.
 * These tools help the LLM work with dates, times, and timezones.
 */
object TimeTools {

    private val clock = Clock.System

    // Helper functions
    private fun atStartOfDay(d: LocalDate) = d.atTime(0, 0, 0)
    private fun atEndOfDay(d: LocalDate) = d.atTime(23, 59, 59)

    private fun toRfc3339At(ldt: LocalDateTime, zone: TimeZone): String =
        ldt.toInstant(zone).toString()

    /**
     * Tool for getting the current date and time
     */
    object GetCurrentDateTimeTool : Tool<GetCurrentDateTimeTool.Args, GetCurrentDateTimeTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("The timezone to get the current date and time in (e.g., 'UTC', 'America/New_York'). Defaults to system timezone.")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val isoNowUtc: String,
            val localDate: String,
            val localTime: String,
            val timezone: String,
            val utcOffset: String,
            val dayOfWeek: String
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                return "Current datetime - UTC: $isoNowUtc, Local date: $localDate, Local time: $localTime, Timezone: $timezone, UTC offset: $utcOffset, Day: $dayOfWeek"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "getCurrentDateTime"
        override val description = "Get current date and time in user's local timezone with detailed info"

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            val now = clock.now()
            val local = now.toLocalDateTime(tz)
            val offset = tz.offsetAt(now).toString().replace("Z", "+00:00")

            return Result(
                isoNowUtc = now.toString(),
                localDate = local.date.toString(),
                localTime = local.time.toString(),
                timezone = tz.id,
                utcOffset = offset,
                dayOfWeek = local.dayOfWeek.name
            )
        }
    }

    /**
     * Tool for getting current time formatted for calendar operations
     */
    object GetCurrentTimeForCalendarTool : Tool<GetCurrentTimeForCalendarTool.Args, GetCurrentTimeForCalendarTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("The timezone (optional, defaults to system timezone)")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val timezone: String,
            val startOfToday: String,
            val endOfToday: String,
            val nowLocalRfc3339: String,
            val nowUtc: String
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                return "Calendar times - Timezone: $timezone, Start of today: $startOfToday, End of today: $endOfToday, Now (RFC3339): $nowLocalRfc3339, Now UTC: $nowUtc"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "getCurrentTimeForCalendar"
        override val description = "Return RFC3339 strings for start/end of today and current time, in user's timezone"

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            val now = clock.now()
            val local = now.toLocalDateTime(tz)
            val start = atStartOfDay(local.date)
            val end = atEndOfDay(local.date)

            return Result(
                timezone = tz.id,
                startOfToday = toRfc3339At(start, tz),
                endOfToday = toRfc3339At(end, tz),
                nowLocalRfc3339 = toRfc3339At(local, tz),
                nowUtc = now.toString()
            )
        }
    }

    /**
     * Tool for finding the next occurrence of a specific day of week
     */
    object FindNextDayOfWeekTool : Tool<FindNextDayOfWeekTool.Args, FindNextDayOfWeekTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("Day of week: MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, or SUNDAY")
            val dayOfWeek: String,
            @property:LLMDescription("The timezone (optional, defaults to system timezone)")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val requestedDay: String,
            val targetDate: String,
            val dayOfWeek: String,
            val daysFromToday: Int,
            val startRfc3339: String,
            val endRfc3339: String,
            val timezone: String,
            val error: String? = null
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                if (error != null) return "Error: $error"
                return "Next $requestedDay: $targetDate ($dayOfWeek), $daysFromToday days from today. Start: $startRfc3339, End: $endRfc3339, Timezone: $timezone"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "findNextDayOfWeek"
        override val description = "Find the next occurrence of a specific day of week. Returns RFC3339 day bounds."

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            return try {
                val targetDow = DayOfWeek.valueOf(args.dayOfWeek.uppercase())
                val now = clock.now()
                val local = now.toLocalDateTime(tz)
                val today = local.date
                val diff = ((targetDow.ordinal - today.dayOfWeek.ordinal + 7) % 7).let { if (it == 0) 7 else it }
                val targetDate = today.plus(diff, DateTimeUnit.DAY)
                val start = toRfc3339At(atStartOfDay(targetDate), tz)
                val end = toRfc3339At(atEndOfDay(targetDate), tz)

                Result(
                    requestedDay = targetDow.name,
                    targetDate = targetDate.toString(),
                    dayOfWeek = targetDate.dayOfWeek.name,
                    daysFromToday = diff,
                    startRfc3339 = start,
                    endRfc3339 = end,
                    timezone = tz.id
                )
            } catch (_: Exception) {
                Result(
                    requestedDay = args.dayOfWeek,
                    targetDate = "",
                    dayOfWeek = "",
                    daysFromToday = 0,
                    startRfc3339 = "",
                    endRfc3339 = "",
                    timezone = tz.id,
                    error = "Invalid dayOfWeek '${args.dayOfWeek}'"
                )
            }
        }
    }

    /**
     * Tool for getting day of week and RFC3339 bounds for a specific date
     */
    object GetDateDayOfWeekTool : Tool<GetDateDayOfWeekTool.Args, GetDateDayOfWeekTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("Year, e.g. 2025")
            val year: Int,
            @property:LLMDescription("Month 1-12")
            val month: Int,
            @property:LLMDescription("Day 1-31")
            val day: Int,
            @property:LLMDescription("The timezone (optional, defaults to system timezone)")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val date: String,
            val dayOfWeek: String,
            val startRfc3339: String,
            val endRfc3339: String,
            val timezone: String,
            val error: String? = null
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                if (error != null) return "Error: $error"
                return "Date: $date ($dayOfWeek). Start: $startRfc3339, End: $endRfc3339, Timezone: $timezone"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "getDateDayOfWeek"
        override val description = "Given a specific date (year, month, day), return its day-of-week and RFC3339 start/end"

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            return try {
                val date = LocalDate(args.year, args.month, args.day)
                val start = toRfc3339At(atStartOfDay(date), tz)
                val end = toRfc3339At(atEndOfDay(date), tz)

                Result(
                    date = date.toString(),
                    dayOfWeek = date.dayOfWeek.name,
                    startRfc3339 = start,
                    endRfc3339 = end,
                    timezone = tz.id
                )
            } catch (_: Exception) {
                Result(
                    date = "",
                    dayOfWeek = "",
                    startRfc3339 = "",
                    endRfc3339 = "",
                    timezone = tz.id,
                    error = "Invalid date ${args.day}/${args.month}/${args.year}"
                )
            }
        }
    }

    /**
     * Tool for calculating date offsets from today
     */
    object CalculateDateTool : Tool<CalculateDateTool.Args, CalculateDateTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("Days to add/subtract from today")
            val days: Int = 0,
            @property:LLMDescription("Weeks to add/subtract from today")
            val weeks: Int = 0,
            @property:LLMDescription("Months to add/subtract from today")
            val months: Int = 0,
            @property:LLMDescription("The timezone (optional, defaults to system timezone)")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val targetDate: String,
            val dayOfWeek: String,
            val startRfc3339: String,
            val endRfc3339: String,
            val timezone: String,
            val daysOffset: Int,
            val weeksOffset: Int,
            val monthsOffset: Int
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                return "Calculated date: $targetDate ($dayOfWeek). Offset: $daysOffset days, $weeksOffset weeks, $monthsOffset months. Start: $startRfc3339, End: $endRfc3339, Timezone: $timezone"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "calculateDate"
        override val description = "Calculate date offsets (days/weeks/months) from today, return RFC3339 start/end"

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            val now = clock.now()
            val local = now.toLocalDateTime(tz)
            val base = local.date
                .plus(args.days, DateTimeUnit.DAY)
                .plus(args.weeks * 7, DateTimeUnit.DAY)
                .plus(args.months, DateTimeUnit.MONTH)

            val start = toRfc3339At(atStartOfDay(base), tz)
            val end = toRfc3339At(atEndOfDay(base), tz)

            return Result(
                targetDate = base.toString(),
                dayOfWeek = base.dayOfWeek.name,
                startRfc3339 = start,
                endRfc3339 = end,
                timezone = tz.id,
                daysOffset = args.days,
                weeksOffset = args.weeks,
                monthsOffset = args.months
            )
        }
    }

    /**
     * Tool for getting timezone info
     */
    object GetTimezoneInfoTool : Tool<GetTimezoneInfoTool.Args, GetTimezoneInfoTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("The timezone to get info for (optional, defaults to system timezone)")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val timezone: String,
            val utcOffset: String,
            val localIso: String,
            val utcIso: String,
            val hoursFromUtc: Double
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                return "Timezone: $timezone, UTC offset: $utcOffset ($hoursFromUtc hours), Local: $localIso, UTC: $utcIso"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "getTimezoneInfo"
        override val description = "Return timezone info: offset, local/UTC time, hours difference"

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            val now = clock.now()
            val local = now.toLocalDateTime(tz)
            val offset = tz.offsetAt(now)

            return Result(
                timezone = tz.id,
                utcOffset = offset.toString().replace("Z", "+00:00"),
                localIso = local.toString(),
                utcIso = now.toString(),
                hoursFromUtc = offset.totalSeconds / 3600.0
            )
        }
    }

    /**
     * Tool for formatting time for calendar operations
     */
    object FormatTimeForCalendarTool : Tool<FormatTimeForCalendarTool.Args, FormatTimeForCalendarTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("Hour 0-23")
            val hour: Int,
            @property:LLMDescription("Minute 0-59")
            val minute: Int = 0,
            @property:LLMDescription("Day offset from today (0=today, 1=tomorrow, -1=yesterday)")
            val dayOffset: Int = 0,
            @property:LLMDescription("The timezone (optional, defaults to system timezone)")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val timezone: String,
            val targetDateTimeLocal: String,
            val rfc3339: String,
            val dayOffset: Int,
            val isToday: Boolean,
            val isTomorrow: Boolean,
            val isYesterday: Boolean
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                val dayDesc = when {
                    isToday -> "today"
                    isTomorrow -> "tomorrow"
                    isYesterday -> "yesterday"
                    else -> "$dayOffset days from today"
                }
                return "Formatted time: $targetDateTimeLocal ($dayDesc). RFC3339: $rfc3339, Timezone: $timezone"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "formatTimeForCalendar"
        override val description = "Format a local date-time for calendar ops. Returns RFC3339 with correct DST"

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            val now = clock.now()
            val local = now.toLocalDateTime(tz)
            val targetDate = local.date.plus(args.dayOffset, DateTimeUnit.DAY)
            val targetLdt = targetDate.atTime(args.hour, args.minute)
            val rfc3339 = toRfc3339At(targetLdt, tz)

            return Result(
                timezone = tz.id,
                targetDateTimeLocal = targetLdt.toString(),
                rfc3339 = rfc3339,
                dayOffset = args.dayOffset,
                isToday = args.dayOffset == 0,
                isTomorrow = args.dayOffset == 1,
                isYesterday = args.dayOffset == -1
            )
        }
    }

    /**
     * Tool for getting event start and end times - the most useful tool for creating events
     */
    object GetEventTimesTool : Tool<GetEventTimesTool.Args, GetEventTimesTool.Result>() {
        @Serializable
        data class Args(
            @property:LLMDescription("Hour 0-23 for event start")
            val hour: Int,
            @property:LLMDescription("Minute 0-59 for event start")
            val minute: Int = 0,
            @property:LLMDescription("Day offset from today: 0=today, 1=tomorrow, -1=yesterday, 7=next week, etc.")
            val dayOffset: Int = 0,
            @property:LLMDescription("Event duration in minutes (default 60)")
            val durationMinutes: Int = 60,
            @property:LLMDescription("The timezone (optional, defaults to system timezone)")
            val timezone: String = ""
        )

        @Serializable
        data class Result(
            val timezone: String,
            val startRfc3339: String,
            val endRfc3339: String,
            val startLocal: String,
            val endLocal: String,
            val date: String,
            val dayOfWeek: String,
            val durationMinutes: Int
        ) : ToolResult.TextSerializable() {
            override fun textForLLM(): String {
                return "Event times - Start: $startRfc3339 ($startLocal), End: $endRfc3339 ($endLocal), Date: $date ($dayOfWeek), Duration: $durationMinutes minutes, Timezone: $timezone"
            }
        }

        override val argsSerializer = Args.serializer()
        override val resultSerializer: KSerializer<Result> = ToolResultUtils.toTextSerializer<Result>()

        override val name = "getEventTimes"
        override val description = "Get RFC3339 start and end times for creating calendar events. Use this single tool instead of multiple time tool calls."

        override suspend fun execute(args: Args): Result {
            val tz = if (args.timezone.isNotBlank()) {
                try { TimeZone.of(args.timezone) } catch (_: Exception) { TimeZone.currentSystemDefault() }
            } else {
                TimeZone.currentSystemDefault()
            }

            val now = clock.now()
            val local = now.toLocalDateTime(tz)
            val targetDate = local.date.plus(args.dayOffset, DateTimeUnit.DAY)

            val startLdt = targetDate.atTime(args.hour, args.minute)
            val endLdt = startLdt.toInstant(tz).plus(args.durationMinutes.toLong(), DateTimeUnit.MINUTE).toLocalDateTime(tz)

            val startRfc3339 = toRfc3339At(startLdt, tz)
            val endRfc3339 = toRfc3339At(endLdt, tz)

            return Result(
                timezone = tz.id,
                startRfc3339 = startRfc3339,
                endRfc3339 = endRfc3339,
                startLocal = startLdt.toString(),
                endLocal = endLdt.toString(),
                date = targetDate.toString(),
                dayOfWeek = targetDate.dayOfWeek.name,
                durationMinutes = args.durationMinutes
            )
        }
    }

    /**
     * Get all time tools as a list for registration in ToolRegistry
     */
    fun getAllTools(): List<Tool<*, *>> = listOf(
        GetCurrentDateTimeTool,
        GetCurrentTimeForCalendarTool,
        FindNextDayOfWeekTool,
        GetDateDayOfWeekTool,
        CalculateDateTool,
        GetTimezoneInfoTool,
        FormatTimeForCalendarTool,
        GetEventTimesTool
    )
}
