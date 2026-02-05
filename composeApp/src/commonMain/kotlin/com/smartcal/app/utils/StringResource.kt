package com.smartcal.app.utils

import org.jetbrains.compose.resources.getString
import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.notification_action_join_meeting
import smartcalai.composeapp.generated.resources.notification_action_view_event
import smartcalai.composeapp.generated.resources.reminder_starts_now
import smartcalai.composeapp.generated.resources.reminder_starts_in_minutes
import smartcalai.composeapp.generated.resources.reminder_starts_in_1_minute
import smartcalai.composeapp.generated.resources.reminder_starts_in_hours
import smartcalai.composeapp.generated.resources.reminder_starts_in_1_hour
import smartcalai.composeapp.generated.resources.reminder_starts_in_days
import smartcalai.composeapp.generated.resources.reminder_starts_in_1_day
import smartcalai.composeapp.generated.resources.reminder_starts_in_hour_minutes
import smartcalai.composeapp.generated.resources.reminder_starts_in_hours_minutes

object NotificationStrings {
    suspend fun getViewEventAction(): String = getString(Res.string.notification_action_view_event)
    suspend fun getJoinMeetingAction(): String = getString(Res.string.notification_action_join_meeting)
    
    private fun formatString(template: String, vararg args: Any): String {
        var result = template
        args.forEachIndexed { index, arg ->
            result = result.replace("%${index + 1}\$d", arg.toString())
        }
        return result
    }
    
    suspend fun formatReminderMessage(minutesBefore: Int): String {
        return when {
            minutesBefore == 0 -> getString(Res.string.reminder_starts_now)
            minutesBefore < 60 -> {
                if (minutesBefore == 1) {
                    getString(Res.string.reminder_starts_in_1_minute)
                } else {
                    formatString(getString(Res.string.reminder_starts_in_minutes), minutesBefore)
                }
            }
            minutesBefore < 1440 -> { // Less than 24 hours (1440 minutes)
                val hours = minutesBefore / 60
                val remainingMinutes = minutesBefore % 60
                
                when {
                    remainingMinutes == 0 -> {
                        if (hours == 1) {
                            getString(Res.string.reminder_starts_in_1_hour)
                        } else {
                            formatString(getString(Res.string.reminder_starts_in_hours), hours)
                        }
                    }
                    hours == 1 -> {
                        formatString(getString(Res.string.reminder_starts_in_hour_minutes), hours, remainingMinutes)
                    }
                    else -> {
                        formatString(getString(Res.string.reminder_starts_in_hours_minutes), hours, remainingMinutes)
                    }
                }
            }
            else -> { // 24 hours or more
                val days = minutesBefore / 1440
                if (days == 1) {
                    getString(Res.string.reminder_starts_in_1_day)
                } else {
                    formatString(getString(Res.string.reminder_starts_in_days), days)
                }
            }
        }
    }
}