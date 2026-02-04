package com.smartcal.app.utils

import kotlinx.datetime.LocalDate
import platform.Foundation.*

actual fun formatLocalDateFriendly(date: LocalDate): String {
    val comps = NSDateComponents().apply {
        year = date.year.toLong()
        month = date.monthNumber.toLong()
        day = date.dayOfMonth.toLong()
    }
    val cal = NSCalendar.currentCalendar
    val nsDate = cal.dateFromComponents(comps)!!

    val formatter = NSDateFormatter().apply {
        dateFormat = "d MMM yyyy"           // "24 Oct 2025"
        locale = NSLocale.currentLocale     // respeta idioma del sistema
    }
    return formatter.stringFromDate(nsDate)
}