package com.smartcal.app.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openBrowser(url: String) {
    val nsUrl = NSURL.URLWithString(url)
    if (nsUrl != null) {
        val sharedApplication = UIApplication.sharedApplication
        if (sharedApplication.canOpenURL(nsUrl)) {
            sharedApplication.openURL(
                url = nsUrl,
                options = emptyMap<Any?, Any?>(),
                completionHandler = { success ->
                    if (success) {
                        println("✅ iOS: Successfully opened URL: $url")
                    } else {
                        println("❌ iOS: Failed to open URL: $url")
                    }
                }
            )
        } else {
            println("❌ iOS: Cannot open URL: $url")
        }
    } else {
        println("❌ iOS: Invalid URL: $url")
    }
}

actual fun getTermsAndConditionsUrl(): String {
    return "https://www.apple.com/legal/internet-services/itunes/dev/stdeula/"
}