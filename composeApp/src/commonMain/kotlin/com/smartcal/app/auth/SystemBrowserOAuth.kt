package com.smartcal.app.auth

typealias AuthResultCallback = (resultUrl: String?, error: Throwable?) -> Unit

expect fun startSystemBrowserAuth(
    authUrl: String,
    callbackScheme: String, // ej: "koogcalendar"
    onResult: AuthResultCallback
)