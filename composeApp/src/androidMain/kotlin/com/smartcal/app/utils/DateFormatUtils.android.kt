package com.smartcal.app.utils

import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.datetime.LocalDate
import java.time.format.DateTimeFormatter
import java.time.LocalDate as JLocalDate
import java.util.Locale

@RequiresApi(Build.VERSION_CODES.O)
actual fun formatLocalDateFriendly(date: LocalDate): String {
    val jDate = JLocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
    return jDate.format(fmt)
}