package com.smartcal.app.data.subscription

// RevenueCat API key - platform specific
expect val API_KEY: String

object RevenueCatConfig {
    
    // Product identifiers (must match App Store Connect IDs)
    object Products {
        // Use the actual App Store Connect IDs that are being requested
        const val STARTER_MONTHLY = "com.calendar.starter"
        const val STARTER_YEARLY = "com.calendar.starter.yearly"
        const val PRO_MONTHLY = "com.calendar.pro.monthly"
        const val PRO_YEARLY = "com.calendar.pro.yearly"
        
        // Legacy IDs (deprecated)
        const val PREMIUM_MONTHLY = "premium_monthly"
        const val PREMIUM_YEARLY = "premium_yearly"
        const val FULL_SUBSCRIPTION = "full_subscription"
    }
    
    // Entitlement identifiers
    object Entitlements {
        const val STARTER = "starter"
        const val PRO = "pro"
    }
}