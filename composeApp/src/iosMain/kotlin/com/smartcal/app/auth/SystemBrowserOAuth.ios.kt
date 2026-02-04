package com.smartcal.app.auth

import platform.AuthenticationServices.*
import platform.Foundation.NSURL
import platform.darwin.NSObject
import platform.UIKit.UIWindow
import platform.UIKit.UIApplication

private class Presenter: NSObject(), ASWebAuthenticationPresentationContextProvidingProtocol {
    override fun presentationAnchorForWebAuthenticationSession(session: ASWebAuthenticationSession): UIWindow {
        return UIApplication.sharedApplication.keyWindow ?: UIWindow()
    }
}

actual fun startSystemBrowserAuth(
    authUrl: String,
    callbackScheme: String,
    onResult: AuthResultCallback
) {
    val url = NSURL.URLWithString(authUrl)
    if (url == null) {
        onResult(null, IllegalArgumentException("Bad URL"))
        return
    }
    
    val session = ASWebAuthenticationSession(
        uRL = url,
        callbackURLScheme = callbackScheme,
        completionHandler = { cbUrl: NSURL?, err: platform.Foundation.NSError? ->
            if (cbUrl != null) {
                onResult(cbUrl.absoluteString(), null)
            } else {
                val errorMsg = err?.localizedDescription() ?: "OAuth canceled or failed"
                onResult(null, Exception(errorMsg))
            }
        }
    )
    
    // For iOS 13.0+, set presentation context provider
    session.setPresentationContextProvider(Presenter())
    
    // Optional: Use ephemeral session for OAuth (clean cookies/cache)
    session.setPrefersEphemeralWebBrowserSession(true)
    
    session.start()
}