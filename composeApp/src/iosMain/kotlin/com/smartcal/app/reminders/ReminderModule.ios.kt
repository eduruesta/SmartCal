package com.smartcal.app.reminders

import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import platform.Foundation.NSUserDefaults

actual fun createReminderScheduler(): ReminderScheduler {
    return IOSReminderScheduler()
}

actual fun createReminderPermissions(): ReminderPermissions {
    return ReminderPermissions()
}

actual fun createSettings(): Settings {
    return NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
}