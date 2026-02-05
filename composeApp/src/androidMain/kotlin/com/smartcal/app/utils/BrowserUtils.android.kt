package com.smartcal.app.utils

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

// Store application context
object ContextProvider {
    lateinit var applicationContext: Context
        private set
    
    fun init(context: Context) {
        applicationContext = context.applicationContext
    }
}

actual fun openBrowser(url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ContextProvider.applicationContext.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        println("❌ Failed to open browser: ${e.message}")
    }
}

actual fun getTermsAndConditionsUrl(): String {
    return "https://gendaai.com/terms.html"
}