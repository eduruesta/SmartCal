package com.smartcal.app.reminders

import com.russhwolf.settings.Settings
import org.koin.dsl.module

val reminderModule = module {
    single { createSettings() }
    single<ReminderIndex> { SettingsReminderIndex(get()) }
    single<ReminderScheduler> { createReminderScheduler() }
    single<ReminderPermissions> { createReminderPermissions() }
    single<ReminderManager> { 
        ReminderManager(
            scheduler = get(),
            index = get(),
            permissions = get()
        ) 
    }
}

expect fun createReminderScheduler(): ReminderScheduler
expect fun createReminderPermissions(): ReminderPermissions  
expect fun createSettings(): Settings