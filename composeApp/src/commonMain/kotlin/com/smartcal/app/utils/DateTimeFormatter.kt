package com.smartcal.app.utils

import androidx.compose.runtime.Composable
import kotlinx.datetime.*
import org.jetbrains.compose.resources.stringResource
import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.*

/**
 * Formats an ISO datetime string into a user-friendly format
 */
@Composable
fun formatEventDateTime(dateTimeString: String?): String {
    if (dateTimeString == null) return stringResource(Res.string.no_time_specified)
    
    val instant = try {
        Instant.parse(dateTimeString)
    } catch (e: Exception) {
        // If parsing fails, try to show something more readable than the raw string
        return dateTimeString.replace("T", " at ").replace("Z", "").replace(":00$".toRegex(), "")
    }
    
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    
    // Format as "Mon, Sep 22 at 11:00 AM" or "Lun, Sep 22 a las 11:00 AM"
    val dayOfWeek = when (localDateTime.dayOfWeek) {
        DayOfWeek.MONDAY -> stringResource(Res.string.day_monday)
        DayOfWeek.TUESDAY -> stringResource(Res.string.day_tuesday)
        DayOfWeek.WEDNESDAY -> stringResource(Res.string.day_wednesday)
        DayOfWeek.THURSDAY -> stringResource(Res.string.day_thursday)
        DayOfWeek.FRIDAY -> stringResource(Res.string.day_friday)
        DayOfWeek.SATURDAY -> stringResource(Res.string.day_saturday)
        DayOfWeek.SUNDAY -> stringResource(Res.string.day_sunday)
        else -> stringResource(Res.string.day_monday) // Fallback
    }
    
    val month = when (localDateTime.month) {
        Month.JANUARY -> stringResource(Res.string.month_january)
        Month.FEBRUARY -> stringResource(Res.string.month_february)
        Month.MARCH -> stringResource(Res.string.month_march)
        Month.APRIL -> stringResource(Res.string.month_april)
        Month.MAY -> stringResource(Res.string.month_may)
        Month.JUNE -> stringResource(Res.string.month_june)
        Month.JULY -> stringResource(Res.string.month_july)
        Month.AUGUST -> stringResource(Res.string.month_august)
        Month.SEPTEMBER -> stringResource(Res.string.month_september)
        Month.OCTOBER -> stringResource(Res.string.month_october)
        Month.NOVEMBER -> stringResource(Res.string.month_november)
        Month.DECEMBER -> stringResource(Res.string.month_december)
        else -> stringResource(Res.string.month_january) // Fallback
    }
    
    val hour = localDateTime.hour
    val minute = localDateTime.minute
    val amPm = if (hour >= 12) stringResource(Res.string.time_pm) else stringResource(Res.string.time_am)
    val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
    val minuteStr = if (minute < 10) "0$minute" else "$minute"
    val atText = stringResource(Res.string.time_at)
    
    return "$dayOfWeek, $month ${localDateTime.dayOfMonth} $atText $displayHour:$minuteStr $amPm"
}

/**
 * Formats a date string into a user-friendly format
 */
@Composable 
fun formatEventDate(dateString: String?): String {
    if (dateString == null) return stringResource(Res.string.no_time_specified)
    
    val date = try {
        LocalDate.parse(dateString)
    } catch (e: Exception) {
        return dateString
    }
    
    val month = when (date.month) {
        Month.JANUARY -> stringResource(Res.string.month_january)
        Month.FEBRUARY -> stringResource(Res.string.month_february)
        Month.MARCH -> stringResource(Res.string.month_march)
        Month.APRIL -> stringResource(Res.string.month_april)
        Month.MAY -> stringResource(Res.string.month_may)
        Month.JUNE -> stringResource(Res.string.month_june)
        Month.JULY -> stringResource(Res.string.month_july)
        Month.AUGUST -> stringResource(Res.string.month_august)
        Month.SEPTEMBER -> stringResource(Res.string.month_september)
        Month.OCTOBER -> stringResource(Res.string.month_october)
        Month.NOVEMBER -> stringResource(Res.string.month_november)
        Month.DECEMBER -> stringResource(Res.string.month_december)
        else -> stringResource(Res.string.month_january) // Fallback
    }
    
    val dayOfWeek = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> stringResource(Res.string.day_monday)
        DayOfWeek.TUESDAY -> stringResource(Res.string.day_tuesday)
        DayOfWeek.WEDNESDAY -> stringResource(Res.string.day_wednesday)
        DayOfWeek.THURSDAY -> stringResource(Res.string.day_thursday)
        DayOfWeek.FRIDAY -> stringResource(Res.string.day_friday)
        DayOfWeek.SATURDAY -> stringResource(Res.string.day_saturday)
        DayOfWeek.SUNDAY -> stringResource(Res.string.day_sunday)
        else -> stringResource(Res.string.day_monday) // Fallback
    }
    
    return "$dayOfWeek, $month ${date.dayOfMonth} - ${stringResource(Res.string.all_day)}"
}

/**
 * Checks if the event is happening today
 */
fun isEventToday(dateTimeString: String?): Boolean {
    if (dateTimeString == null) return false
    
    return try {
        val instant = Instant.parse(dateTimeString)
        val eventDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        eventDate == today
    } catch (e: Exception) {
        false
    }
}

/**
 * Checks if the event is happening tomorrow
 */
fun isEventTomorrow(dateTimeString: String?): Boolean {
    if (dateTimeString == null) return false
    
    return try {
        val instant = Instant.parse(dateTimeString)
        val eventDate = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val tomorrow = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.plus(1, DateTimeUnit.DAY)
        eventDate == tomorrow
    } catch (e: Exception) {
        false
    }
}