package com.smartcal.app.utils

import kotlinx.datetime.LocalDate

fun formatDateForDisplay(dateString: String): String =
    runCatching {
        val date = LocalDate.parse(dateString) // "2025-10-24"
        formatLocalDateFriendly(date)          // -> "24 Oct 2025" (según locale)
    }.getOrElse { dateString }

// Declaración expect
expect fun formatLocalDateFriendly(date: LocalDate): String