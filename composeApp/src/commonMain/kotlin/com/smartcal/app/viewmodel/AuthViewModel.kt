package com.smartcal.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcal.app.models.AuthState
import com.smartcal.app.models.LoginResult
import com.smartcal.app.repository.CalendarRepository
import com.smartcal.app.services.AuthService
import com.smartcal.app.storage.SessionStorage
import com.smartcal.app.data.subscription.RevenueCatManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authService: AuthService,
    private val sessionStorage: SessionStorage,
    private val calendarRepository: CalendarRepository,
    private val revenueCatManager: RevenueCatManager
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isInOAuthFlow = MutableStateFlow(false)
    val isInOAuthFlow: StateFlow<Boolean> = _isInOAuthFlow.asStateFlow()
    
    private var oauthTimeoutJob: Job? = null

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            println("🚀 AuthViewModel: Checking for existing session...")
            _authState.value = _authState.value.copy(isLoading = true)
            
            val existingToken = sessionStorage.getSessionToken()
            
            if (existingToken != null) {
                println("🔍 AuthViewModel: Found existing token, validating...")
                authService.setSessionToken(existingToken)
                
                try {
                    val validationResult = authService.validateSession(existingToken)
                    validationResult.fold(
                        onSuccess = { isValid ->
                            if (isValid) {
                                println("✅ AuthViewModel: Session is valid")
                                calendarRepository.setSessionToken(existingToken)
                                
                                // Fetch user profile to get current subscription and credits
                                viewModelScope.launch {
                                    try {
                                        val profileResult = calendarRepository.getUserProfile()
                                        val profile = profileResult.getOrNull()
                                        
                                        _authState.value = _authState.value.copy(
                                            isAuthenticated = true,
                                            sessionToken = existingToken,
                                            userEmail = profile?.email,
                                            fullName = profile?.firstName?.let { firstName ->
                                                profile.lastName?.let { "$firstName ${profile.lastName}" } ?: firstName
                                            },
                                            profilePicture = profile?.profilePicture,
                                            creditsRemaining = profile?.creditsRemaining,
                                            subscriptionPlan = profile?.subscriptionPlan,
                                            isLoading = false,
                                            error = null
                                        )
                                        
                                        // Configure RevenueCat if Google Sub is available
                                        profile?.googleSub?.let { googleSub ->
                                            println("🆔 AuthViewModel: Configuring RevenueCat with Google Sub: $googleSub")
                                            try {
                                                val revenueCatResult = revenueCatManager.loginUser(googleSub)
                                                if (revenueCatResult.isSuccess) {
                                                    println("✅ AuthViewModel: RevenueCat configured successfully")
                                                }
                                                _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                            } catch (e: Exception) {
                                                println("❌ AuthViewModel: RevenueCat configuration failed: ${e.message}")
                                                _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                            }
                                        } ?: run {
                                            _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                        }
                                    } catch (e: Exception) {
                                        println("❌ AuthViewModel: Failed to fetch profile on session restore: ${e.message}")
                                        // Still authenticate but without profile data
                                        _authState.value = _authState.value.copy(
                                            isAuthenticated = true,
                                            sessionToken = existingToken,
                                            isLoading = false,
                                            error = null,
                                            isRevenueCatReady = true
                                        )
                                    }
                                }
                            } else {
                                println("❌ AuthViewModel: Session is invalid")
                                clearSession()
                            }
                        },
                        onFailure = { exception ->
                            println("❌ AuthViewModel: Session validation failed: ${exception.message}")
                            clearSession()
                        }
                    )
                } catch (e: Exception) {
                    println("❌ AuthViewModel: Session validation error: ${e.message}")
                    clearSession()
                }
            } else {
                println("🔍 AuthViewModel: No existing session found")
                _authState.value = _authState.value.copy(isLoading = false)
            }
        }
    }

    fun startLogin() {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, error = null)
            
            try {
                when (val result = authService.loginUser()) {
                    is LoginResult.Success -> {
                        handleLoginSuccess(result)
                    }
                    is LoginResult.NeedsBrowserAuth -> {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            needsBrowserAuth = true,
                            authUrl = result.authUrl,
                            sessionId = result.sessionId
                        )
                    }
                    is LoginResult.Error -> {
                        _authState.value = _authState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            } catch (e: Exception) {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    error = "Login error: ${e.message}"
                )
            }
        }
    }

    fun startOAuthFlow(authUrl: String, sessionId: String) {
        println("🌐 AuthViewModel: Starting OAuth flow with timeout...")
        _isInOAuthFlow.value = true
        _authState.value = _authState.value.copy(isLoading = true, error = null)
        
        // Start timeout job - if OAuth doesn't complete in 5 minutes, cancel it
        oauthTimeoutJob?.cancel()
        oauthTimeoutJob = viewModelScope.launch {
            delay(300_000) // 5 minutes timeout
            if (_isInOAuthFlow.value) {
                println("⏰ AuthViewModel: OAuth timeout - cancelling flow")
                cancelOAuthFlow("Authentication timed out. Please try again.")
            }
        }
    }

    fun handleOAuthCallback(resultUrl: String?, error: Throwable?) {
        // Cancel timeout since we got a callback
        oauthTimeoutJob?.cancel()
        
        viewModelScope.launch {
            if (resultUrl != null && error == null) {
                println("🔗 AuthViewModel: OAuth callback received: $resultUrl")
                
                try {
                    // Extract session token and isNewUser from callback URL
                    val callbackResult = authService.extractDataFromCallback(resultUrl)
                    
                    if (callbackResult != null) {
                        // Set the session token and validate it
                        authService.setSessionToken(callbackResult.sessionToken)
                        
                        // Validate the session to ensure it's working
                        val validationResult = authService.validateSession(callbackResult.sessionToken)
                        validationResult.fold(
                            onSuccess = { isValid ->
                                if (isValid) {
                                    println("✅ AuthViewModel: OAuth session token validated successfully")
                                    // Set session token in repository before calling getUserProfile
                                    calendarRepository.setSessionToken(callbackResult.sessionToken)
                                    
                                    // Get Google Sub from profile API after successful OAuth for RevenueCat
                                    viewModelScope.launch {
                                        try {
                                            val profileResult = calendarRepository.getUserProfile()
                                            val profile = profileResult.getOrNull()
                                            val googleSub = profile?.googleSub
                                            val userEmail = profile?.email
                                            
                                            if (googleSub != null) {
                                                println("🆔 AuthViewModel: Retrieved Google Sub for RevenueCat: $googleSub")
                                                // Configure RevenueCat with Google Sub immediately
                                                try {
                                                    val revenueCatResult = revenueCatManager.loginUser(googleSub)
                                                    if (revenueCatResult.isSuccess) {
                                                        println("✅ AuthViewModel: RevenueCat configured with Google Sub")
                                                        _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                                    } else {
                                                        println("❌ AuthViewModel: RevenueCat login with Google Sub failed")
                                                        _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                                    }
                                                } catch (e: Exception) {
                                                    println("❌ AuthViewModel: RevenueCat exception with Google Sub: ${e.message}")
                                                    _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                                }
                                            } else {
                                                println("⚠️ AuthViewModel: No Google Sub available, using email fallback: $userEmail")
                                                // Fallback to email if Google Sub not available
                                                if (userEmail != null) {
                                                    try {
                                                        val revenueCatResult = revenueCatManager.loginUser(userEmail)
                                                        if (revenueCatResult.isSuccess) {
                                                            println("✅ AuthViewModel: RevenueCat configured with email fallback")
                                                        }
                                                    } catch (e: Exception) {
                                                        println("❌ AuthViewModel: RevenueCat email fallback failed: ${e.message}")
                                                    }
                                                }
                                                _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                            }
                                            
                                            handleLoginSuccess(LoginResult.Success(userEmail = userEmail, isNewUser = callbackResult.isNewUser))
                                        } catch (e: Exception) {
                                            println("❌ AuthViewModel: Failed to get profile data: ${e.message}")
                                            // Continue without profile data but mark RevenueCat as ready
                                            _authState.value = _authState.value.copy(isRevenueCatReady = true)
                                            handleLoginSuccess(LoginResult.Success(isNewUser = callbackResult.isNewUser))
                                        }
                                    }
                                } else {
                                    println("❌ AuthViewModel: OAuth session token is invalid")
                                    cancelOAuthFlow("OAuth session validation failed")
                                }
                            },
                            onFailure = { exception ->
                                println("❌ AuthViewModel: OAuth session validation error: ${exception.message}")
                                cancelOAuthFlow("OAuth session validation failed: ${exception.message}")
                            }
                        )
                    } else {
                        println("❌ AuthViewModel: Could not extract session token from OAuth callback")
                        cancelOAuthFlow("OAuth process incomplete - no session token received")
                    }
                } catch (e: Exception) {
                    println("❌ AuthViewModel: OAuth callback processing failed: ${e.message}")
                    cancelOAuthFlow("OAuth callback processing failed: ${e.message}")
                }
            } else {
                println("❌ AuthViewModel: OAuth cancelled or failed")
                cancelOAuthFlow(error?.message ?: "Authentication was cancelled")
            }
        }
    }
    
    fun cancelOAuthFlow(errorMessage: String? = null) {
        println("🚫 AuthViewModel: Cancelling OAuth flow")
        oauthTimeoutJob?.cancel()
        _isInOAuthFlow.value = false
        _authState.value = _authState.value.copy(
            isLoading = false,
            error = errorMessage,
            needsBrowserAuth = false,
            authUrl = null,
            sessionId = null
        )
    }

    private fun handleLoginSuccess(result: LoginResult.Success) {
        // Cancel timeout since we succeeded
        oauthTimeoutJob?.cancel()
        
        val sessionToken = authService.getSessionToken()
        
        if (sessionToken != null) {
            println("💾 AuthViewModel: Saving session token...")
            sessionStorage.saveSessionToken(sessionToken)
            
            _isInOAuthFlow.value = false
            _authState.value = _authState.value.copy(
                isAuthenticated = true,
                sessionToken = sessionToken,
                userEmail = result.userEmail,
                isNewUser = result.isNewUser,
                isLoading = false,
                error = null,
                needsBrowserAuth = false,
                authUrl = null,
                sessionId = null
            )
            println("🔐 AuthViewModel: handleLoginSuccess completed")
            // Note: RevenueCat is configured earlier in the OAuth flow with Google Sub or email
            // If not already configured, mark as ready to not block UI
            if (!_authState.value.isRevenueCatReady) {
                println("⚠️ AuthViewModel: RevenueCat not configured yet, marking as ready to not block UI")
                _authState.value = _authState.value.copy(isRevenueCatReady = true)
            }
        } else {
            cancelOAuthFlow("Authentication succeeded but no token received")
        }
    }

    fun logout() {
        viewModelScope.launch {
            val currentToken = _authState.value.sessionToken
            
            if (currentToken != null) {
                try {
                    authService.logout(currentToken)
                } catch (e: Exception) {
                    println("❌ AuthViewModel: Logout API failed: ${e.message}")
                }
            }
            
            clearSession()
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            val currentToken = _authState.value.sessionToken
            
            if (currentToken != null) {
                try {
                    // Show loading state
                    _authState.value = _authState.value.copy(isLoading = true)
                    
                    val result = authService.deleteAccount(currentToken)
                    result.fold(
                        onSuccess = { response ->
                            println("✅ AuthViewModel: Account deleted successfully: ${response.message}")
                            clearSession()
                        },
                        onFailure = { error ->
                            println("❌ AuthViewModel: Delete account failed: ${error.message}")
                            _authState.value = _authState.value.copy(
                                isLoading = false,
                                error = error.message
                            )
                        }
                    )
                } catch (e: Exception) {
                    println("❌ AuthViewModel: Delete account API failed: ${e.message}")
                    _authState.value = _authState.value.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            } else {
                clearSession()
            }
        }
    }

    fun onTokenExpired() {
        println("🔒 AuthViewModel: Token expired - clearing session")
        clearSession()
    }

    private fun clearSession() {
        println("🧹 AuthViewModel: Clearing session and conversation")
        sessionStorage.clearSessionToken()
        // Note: Don't clear onboarding completion on logout - user should only see onboarding once per device
        calendarRepository.clearConversation()
        _authState.value = AuthState()
        _isInOAuthFlow.value = false
    }

    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
    
    fun updateCreditsAndSubscription(creditsRemaining: Int?, subscriptionPlan: String?) {
        println("🔄 AuthViewModel: updateCreditsAndSubscription called")
        println("   creditsRemaining: $creditsRemaining")
        println("   subscriptionPlan: $subscriptionPlan")
        _authState.value = _authState.value.copy(
            creditsRemaining = creditsRemaining,
            subscriptionPlan = subscriptionPlan
        )
    }
    
    fun updateUserProfile(fullName: String?, profilePicture: String?) {
        println("👤 AuthViewModel: updateUserProfile called")
        println("   fullName: $fullName")
        println("   profilePicture: $profilePicture")
        _authState.value = _authState.value.copy(
            fullName = fullName,
            profilePicture = profilePicture
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        oauthTimeoutJob?.cancel()
    }
}