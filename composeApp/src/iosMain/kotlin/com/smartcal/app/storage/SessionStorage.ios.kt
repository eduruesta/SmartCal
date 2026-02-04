package com.smartcal.app.storage

import platform.Foundation.NSUserDefaults

actual object SessionStorage {
    private const val SESSION_TOKEN_KEY = "session_token"
    private const val DEVICE_ID_KEY = "device_id"
    private const val ONBOARDING_COMPLETED_KEY = "onboarding_completed"
    private const val PTR_PERFORMED_KEY = "ptr_performed"
    
    private val userDefaults = NSUserDefaults.standardUserDefaults
    
    actual fun saveSessionToken(token: String) {
        println("🍎 iOS: Saving session token: ${token.take(20)}...")
        userDefaults.setObject(token, SESSION_TOKEN_KEY)
    }
    
    actual fun getSessionToken(): String? {
        val token = userDefaults.stringForKey(SESSION_TOKEN_KEY)
        println("🍎 iOS: Retrieved session token: ${token ?: "null"}")
        return token
    }
    
    actual fun clearSessionToken() {
        println("🍎 iOS: Clearing session token")
        userDefaults.removeObjectForKey(SESSION_TOKEN_KEY)
    }
    
    actual fun getDeviceId(): String {
        var deviceId = userDefaults.stringForKey(DEVICE_ID_KEY)
        if (deviceId == null) {
            // Generate a unique device ID using UUID
            deviceId = "ios_${platform.Foundation.NSUUID().UUIDString}"
            saveDeviceId(deviceId)
            println("🍎 iOS: Generated new device ID: $deviceId")
        } else {
            println("🍎 iOS: Retrieved existing device ID: $deviceId")
        }
        return deviceId
    }
    
    actual fun saveDeviceId(deviceId: String) {
        println("🍎 iOS: Saving device ID: $deviceId")
        userDefaults.setObject(deviceId, DEVICE_ID_KEY)
    }
    
    actual fun hasCompletedOnboarding(): Boolean {
        val completed = userDefaults.boolForKey(ONBOARDING_COMPLETED_KEY)
        println("🍎 iOS: Onboarding completed: $completed")
        return completed
    }
    
    actual fun setOnboardingCompleted(completed: Boolean) {
        println("🍎 iOS: Setting onboarding completed: $completed")
        userDefaults.setBool(completed, ONBOARDING_COMPLETED_KEY)
    }
    
    actual fun hasUserPerformedPTR(): Boolean {
        val performed = userDefaults.boolForKey(PTR_PERFORMED_KEY)
        println("🍎 iOS: User performed PTR: $performed")
        return performed
    }
    
    actual fun setUserPerformedPTR(performed: Boolean) {
        println("🍎 iOS: Setting user performed PTR: $performed")
        userDefaults.setBool(performed, PTR_PERFORMED_KEY)
    }
}