package com.smartcal.app.storage

expect object SessionStorage {
    fun saveSessionToken(token: String)
    fun getSessionToken(): String?
    fun clearSessionToken()
    
    // Device identification for security
    fun getDeviceId(): String
    fun saveDeviceId(deviceId: String)
    
    // Onboarding completion state
    fun hasCompletedOnboarding(): Boolean
    fun setOnboardingCompleted(completed: Boolean)
    
    // Pull-to-refresh usage tracking
    fun hasUserPerformedPTR(): Boolean
    fun setUserPerformedPTR(performed: Boolean)
}