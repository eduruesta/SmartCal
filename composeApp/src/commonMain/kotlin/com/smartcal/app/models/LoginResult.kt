package com.smartcal.app.models

sealed class LoginResult {
    data class Success(val userEmail: String? = null, val isNewUser: Boolean? = null) : LoginResult()
    data class NeedsBrowserAuth(val authUrl: String, val sessionId: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
}