package com.smartcal.app.utils

import com.smartcal.app.models.Event
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

// Date utility functions
fun daysInMonth(year: Int, month: Int): Int {
    return when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeapYear(year)) 29 else 28
        else -> 30
    }
}

fun isLeapYear(year: Int): Boolean =
    (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)

fun nextMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 12) year + 1 to 1 else year to (month + 1)

fun prevMonth(year: Int, month: Int): Pair<Int, Int> =
    if (month == 1) year - 1 to 12 else year to (month - 1)

// Event utility functions
fun eventStartInstant(event: Event, defaultZone: TimeZone): Instant? {
    val start = event.start ?: return null
    return when {
        start.dateTime != null -> runCatching { Instant.parse(start.dateTime) }.getOrNull()
        start.date != null -> {
            val zone =
                start.timeZone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: defaultZone
            val ld = runCatching { LocalDate.parse(start.date) }.getOrNull() ?: return null
            ld.atStartOfDayIn(zone)
        }
        else -> null
    }
}

fun eventStartLocalDate(event: Event, defaultZone: TimeZone): LocalDate? {
    val start = event.start ?: return null
    return when {
        start.dateTime != null -> {
            val inst = runCatching { Instant.parse(start.dateTime) }.getOrNull() ?: return null
            val zone =
                start.timeZone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: defaultZone
            inst.toLocalDateTime(zone).date
        }
        start.date != null -> runCatching { LocalDate.parse(start.date) }.getOrNull()
        else -> null
    }
}

fun eventEndLocalDate(event: Event, defaultZone: TimeZone): LocalDate? {
    val end = event.end ?: return null
    return when {
        end.dateTime != null -> {
            val inst = runCatching { Instant.parse(end.dateTime) }.getOrNull() ?: return null
            val zone =
                end.timeZone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: defaultZone
            inst.toLocalDateTime(zone).date
        }
        end.date != null -> runCatching { LocalDate.parse(end.date) }.getOrNull()
        else -> null
    }
}

fun eventCoversDate(event: Event, date: LocalDate, defaultZone: TimeZone): Boolean {
    val start = event.start ?: return false
    val end = event.end ?: return false

    // All-day events: start.date / end.date (end is exclusive per Google Calendar API)
    if (start.date != null && end.date != null) {
        val startDate = runCatching { LocalDate.parse(start.date) }.getOrNull() ?: return false
        val endDate = runCatching { LocalDate.parse(end.date) }.getOrNull() ?: return false
        return date >= startDate && date < endDate
    }

    // Timed events: check if the event's instant range overlaps with the day
    val startInstant = eventStartInstant(event, defaultZone) ?: return false
    val endInstant = eventEndInstant(event, defaultZone) ?: startInstant
    val dayStart = date.atStartOfDayIn(defaultZone)
    val dayEnd = date.plus(DatePeriod(days = 1)).atStartOfDayIn(defaultZone)
    return startInstant < dayEnd && endInstant > dayStart
}

fun eventEndInstant(event: Event, defaultZone: TimeZone): Instant? {
    val end = event.end ?: return null
    return when {
        end.dateTime != null -> runCatching { Instant.parse(end.dateTime) }.getOrNull()
        end.date != null -> {
            val zone =
                end.timeZone?.let { runCatching { TimeZone.of(it) }.getOrNull() } ?: defaultZone
            val ld = runCatching { LocalDate.parse(end.date) }.getOrNull() ?: return null
            ld.atStartOfDayIn(zone)
        }
        else -> null
    }
}

// Extract Meet/conference URL from event
fun getMeetUrl(event: Event): String? {
    // Priority order for finding Meet URL:
    // 1. conferenceData.entryPoints with video type (modern events)
    // 2. hangoutLink as fallback (older events)
    // 3. description containing meet.google.com (last resort)
    
    // Check conferenceData.entryPoints first (highest priority)
    event.conferenceData?.entryPoints?.forEach { entryPoint ->
        if (entryPoint.entryPointType == "video" && entryPoint.uri != null) {
            return entryPoint.uri
        }
    }
    
    // Check hangoutLink as fallback for older events
    event.hangoutLink?.let { hangoutLink ->
        if (hangoutLink.contains("meet.google.com") || hangoutLink.contains("hangouts.google.com")) {
            return hangoutLink
        }
    }
    
    // Check description for Meet URLs as last resort
    event.description?.let { description ->
        val meetRegex = Regex("https://meet\\.google\\.com/[a-z0-9-]+")
        val hangoutsRegex = Regex("https://hangouts\\.google\\.com/[a-z0-9-_?&=]+")
        
        meetRegex.find(description)?.value?.let { return it }
        hangoutsRegex.find(description)?.value?.let { return it }
    }
    
    return null
}