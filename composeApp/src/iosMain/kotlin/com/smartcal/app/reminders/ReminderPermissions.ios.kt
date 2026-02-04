package com.smartcal.app.reminders

import platform.UserNotifications.*
import platform.UIKit.*
import platform.Foundation.*

actual class ReminderPermissions {

    actual suspend fun requestPermissions(): Boolean {
        return IOSReminderScheduler.requestPermissions()
    }

    actual suspend fun hasPermissions(): Boolean {
        val status = IOSReminderScheduler.checkPermissions()
        return status == UNAuthorizationStatusAuthorized
    }

    actual suspend fun openPermissionSettings() {
        val settingsUrl = NSURL.URLWithString("app-settings:")
        if (settingsUrl != null && UIApplication.sharedApplication.canOpenURL(settingsUrl)) {
            UIApplication.sharedApplication.openURL(settingsUrl)
        } else {
            // Fallback a configuración general de iOS
            val generalSettingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
            if (generalSettingsUrl != null) {
                UIApplication.sharedApplication.openURL(generalSettingsUrl)
            }
        }
    }
}