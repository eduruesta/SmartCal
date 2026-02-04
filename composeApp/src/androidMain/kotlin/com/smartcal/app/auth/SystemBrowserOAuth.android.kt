package com.smartcal.app.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Global variables to hold the current callback
private var currentAuthCallback: AuthResultCallback? = null
private var expectedScheme: String? = null
private var appContext: Context? = null
private var authStartTime: Long = 0
private var foregroundCheckJob: Job? = null

fun setAppContext(context: Context) {
    appContext = context
}

actual fun startSystemBrowserAuth(
    authUrl: String,
    callbackScheme: String,
    onResult: AuthResultCallback
) {
    // Store callback and scheme for later use
    currentAuthCallback = onResult
    expectedScheme = callbackScheme
    authStartTime = System.currentTimeMillis()
    
    println("🔗 Android: Starting browser auth at $authStartTime")
    
    try {
        val context = appContext
        if (context != null) {
            // Open system browser
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            onResult(null, Exception("App context not set. Call setAppContext() first."))
        }
    } catch (e: Exception) {
        onResult(null, e)
    }
}

// Function to handle OAuth callback from Activity
fun handleOAuthCallback(resultUrl: String?) {
    val callback = currentAuthCallback
    val scheme = expectedScheme
    
    println("🔗 Android: OAuth callback received: $resultUrl")
    
    // Cancel any pending foreground check
    foregroundCheckJob?.cancel()
    
    if (callback != null) {
        if (resultUrl != null && scheme != null && resultUrl.startsWith("$scheme://")) {
            println("✅ Android: Valid OAuth callback")
            callback(resultUrl, null)
        } else {
            println("❌ Android: Invalid OAuth callback")
            callback(null, Exception("Invalid callback URL or scheme mismatch"))
        }
        
        // Clear callback after use
        clearAuthState()
    }
}

// Function to handle when app comes back to foreground
fun onAppForegrounded() {
    val callback = currentAuthCallback
    
    if (callback != null && authStartTime > 0) {
        val timeSinceAuth = System.currentTimeMillis() - authStartTime
        println("🏠 Android: App foregrounded after ${timeSinceAuth}ms since OAuth start")
        
        // Cancel any existing job
        foregroundCheckJob?.cancel()
        
        // Give a short grace period for the deep link to arrive
        foregroundCheckJob = CoroutineScope(Dispatchers.Main).launch {
            delay(2000) // 2 second grace period
            
            // If we still have a pending callback after grace period, consider it cancelled
            if (currentAuthCallback != null) {
                println("⚠️ Android: OAuth appears to be cancelled (no callback after foreground)")
                callback(null, Exception("Authentication was cancelled"))
                clearAuthState()
            }
        }
    }
}

// Function to clear auth state
private fun clearAuthState() {
    println("🧹 Android: Clearing OAuth state")
    currentAuthCallback = null
    expectedScheme = null
    authStartTime = 0
    foregroundCheckJob?.cancel()
    foregroundCheckJob = null
}