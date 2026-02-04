package com.smartcal.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.smartcal.app.auth.handleOAuthCallback

class OAuthCallbackActivity : Activity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val uri = intent.data
        println("🔗 OAuthCallbackActivity: Received URI: $uri")
        
        if (uri != null) {
            val scheme = uri.scheme
            val host = uri.host
            val path = uri.path
            
            println("🔗 OAuthCallbackActivity: scheme=$scheme, host=$host, path=$path")
            
            // More flexible validation - just check scheme
            if (scheme == "koogcalendar") {
                println("✅ OAuthCallbackActivity: Valid OAuth callback")
                // Handle OAuth callback through the new system
                handleOAuthCallback(uri.toString())
            } else {
                println("❌ OAuthCallbackActivity: Invalid scheme: $scheme")
                // Invalid scheme
                handleOAuthCallback(null)
            }
        } else {
            println("❌ OAuthCallbackActivity: No URI received")
            // No URI, handle as error
            handleOAuthCallback(null)
        }
        
        // Always redirect to MainActivity
        val mainIntent = Intent(this, MainActivity::class.java)
        mainIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(mainIntent)
        
        finish()
    }
}