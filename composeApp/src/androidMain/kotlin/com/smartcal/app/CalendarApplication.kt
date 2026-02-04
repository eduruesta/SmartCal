package com.smartcal.app

import android.app.Application
import com.smartcal.app.auth.setAppContext
import com.smartcal.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class CalendarApplication : Application() {
    
    companion object {
        lateinit var instance: CalendarApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize Koin
        startKoin {
            androidContext(this@CalendarApplication)
            modules(appModule)
        }
        
        // Set the app context for OAuth
        setAppContext(this)
    }
}