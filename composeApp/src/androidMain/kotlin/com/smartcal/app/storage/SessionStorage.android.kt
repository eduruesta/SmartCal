package com.smartcal.app.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

actual object SessionStorage {
    private const val PREFS_NAME = "calendar_agent_prefs"
    private const val SESSION_TOKEN_KEY = "session_token"
    private const val DEVICE_ID_KEY = "device_id"
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    private const val PTR_PERFORMED_KEY = "ptr_performed"
    
    private val prefs: SharedPreferences by lazy {
        com.smartcal.app.CalendarApplication.instance
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    actual fun saveSessionToken(token: String) {
        println("📱 Android: Saving session token: ${token.take(20)}...")
        prefs.edit { putString(SESSION_TOKEN_KEY, token) }
    }
    
    actual fun getSessionToken(): String? {
        val token = prefs.getString(SESSION_TOKEN_KEY, null)
        println("📱 Android: Retrieved session token: ${token?.take(20) ?: "null"}")
        return token
    }
    
    actual fun clearSessionToken() {
        println("📱 Android: Clearing session token")
        prefs.edit { remove(SESSION_TOKEN_KEY) }
    }
    
    actual fun getDeviceId(): String {
        var deviceId = prefs.getString(DEVICE_ID_KEY, null)
        if (deviceId == null) {
            // Generate a unique device ID using UUID
            deviceId = "android_${java.util.UUID.randomUUID().toString()}"
            saveDeviceId(deviceId)
            println("📱 Android: Generated new device ID: $deviceId")
        } else {
            println("📱 Android: Retrieved existing device ID: $deviceId")
        }
        return deviceId
    }
    
    actual fun saveDeviceId(deviceId: String) {
        println("📱 Android: Saving device ID: $deviceId")
        prefs.edit { putString(DEVICE_ID_KEY, deviceId) }
    }
    
    actual fun hasCompletedOnboarding(): Boolean {
        val completed = prefs.getBoolean(ONBOARDING_COMPLETED_KEY, false)
        println("📱 Android: Onboarding completed: $completed")
        return completed
    }
    
    actual fun setOnboardingCompleted(completed: Boolean) {
        println("📱 Android: Setting onboarding completed: $completed")
        prefs.edit { putBoolean(ONBOARDING_COMPLETED_KEY, completed) }
    }
    
    actual fun hasUserPerformedPTR(): Boolean {
        val performed = prefs.getBoolean(PTR_PERFORMED_KEY, false)
        println("📱 Android: User performed PTR: $performed")
        return performed
    }
    
    actual fun setUserPerformedPTR(performed: Boolean) {
        println("📱 Android: Setting user performed PTR: $performed")
        prefs.edit { putBoolean(PTR_PERFORMED_KEY, performed) }
    }
}