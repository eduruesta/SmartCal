package com.smartcal.app.reminders

expect class ReminderPermissions {
    suspend fun requestPermissions(): Boolean
    suspend fun hasPermissions(): Boolean
    suspend fun openPermissionSettings()
}