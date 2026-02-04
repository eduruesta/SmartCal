package com.smartcal.app.reminders

import android.content.Context
import android.os.Build
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

actual fun createReminderScheduler(): ReminderScheduler {
    return object : KoinComponent {
        fun create(): ReminderScheduler {
            val context: Context = get()
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                AndroidReminderScheduler(context)
            } else {
                // Para versiones anteriores de Android, crear implementación simplificada
                object : ReminderScheduler {
                    override suspend fun schedule(instance: ReminderInstance) {}
                    override suspend fun cancelByEvent(eventId: String, instances: List<ReminderInstance>) {}
                }
            }
        }
    }.create()
}

actual fun createReminderPermissions(): ReminderPermissions {
    return object : KoinComponent {
        fun create(): ReminderPermissions {
            val context: Context = get()
            return ReminderPermissions(context)
        }
    }.create()
}

actual fun createSettings(): Settings {
    return object : KoinComponent {
        fun create(): Settings {
            val context: Context = get()
            val sharedPrefs = context.getSharedPreferences("calendar_reminders", Context.MODE_PRIVATE)
            return SharedPreferencesSettings(sharedPrefs)
        }
    }.create()
}