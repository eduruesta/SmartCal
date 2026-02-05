package com.smartcal.app

import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.cancel_button
import smartcalai.composeapp.generated.resources.completing_authentication
import smartcalai.composeapp.generated.resources.loading
import smartcalai.composeapp.generated.resources.paywall_configuring_message
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.smartcal.app.auth.startSystemBrowserAuth
import com.smartcal.app.data.subscription.API_KEY
import com.smartcal.app.data.subscription.RevenueCatManager
import com.smartcal.app.presentation.subscription.RevenueCatPaywallScreen
import com.smartcal.app.reminders.ReminderLifecycleObserver
import com.smartcal.app.reminders.ReminderManager
import com.smartcal.app.storage.SessionStorage
import com.smartcal.app.ui.CalendarScreen
import com.smartcal.app.ui.LoginScreen
import com.smartcal.app.ui.OnboardingScreen
import com.smartcal.app.ui.theme.CalendarTheme
import com.smartcal.app.utils.VoiceTranscriberFactory
import com.smartcal.app.viewmodel.AuthViewModel
import com.smartcal.app.viewmodel.CalendarViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(voiceTranscriberFactory: VoiceTranscriberFactory? = null) {
    val authViewModel: AuthViewModel = koinViewModel()
    val authState by authViewModel.authState.collectAsState()
    val isInOAuthFlow by authViewModel.isInOAuthFlow.collectAsState()
    val revenueCatManager: RevenueCatManager = koinInject()
    val reminderManager: ReminderManager = koinInject()

    // Debug logging
    LaunchedEffect(authState.creditsRemaining, authState.subscriptionPlan) {
        println("🔍 App.kt - AuthState updated:")
        println("   creditsRemaining: ${authState.creditsRemaining}")
        println("   subscriptionPlan: ${authState.subscriptionPlan}")
        println("   isAuthenticated: ${authState.isAuthenticated}")
    }

    // Initialize RevenueCat when the app starts
    LaunchedEffect(Unit) {
        try {
            revenueCatManager.initialize(API_KEY)
        } catch (e: Exception) {
            println("Error al inicializar RevenueCat: ${e.message}")
        }
    }

    // Initialize reminder manager and request permissions when authenticated
    LaunchedEffect(authState.isAuthenticated) {
        if (authState.isAuthenticated) {
            reminderManager.initialize()
            if (!reminderManager.hasPermissions.value) {
                println("🔔 Solicitando permisos para recordatorios...")
                reminderManager.requestPermissions()
            }
        }
    }

    // Onboarding state - check from SessionStorage and user's isNewUser status
    var shouldShowOnboarding by remember { mutableStateOf(false) }

    // Paywall state for upgrade plan
    var showPaywall by remember { mutableStateOf(false) }

    // Navigation after subscription success
    var navigateToSettings by remember { mutableStateOf(false) }
    var settingsSuccessPlanLabel by remember { mutableStateOf<String?>(null) }

    // Determine if we should show onboarding based on:
    // 1. User hasn't completed onboarding before (stored locally)
    // 2. User is a new user (from backend)
    LaunchedEffect(authState.isAuthenticated, authState.isNewUser) {
        shouldShowOnboarding = when {
            // If user is authenticated and backend says they're new, show onboarding
            authState.isAuthenticated && authState.isNewUser == true -> true
            // If user is not authenticated and hasn't completed onboarding locally, show onboarding after login
            !authState.isAuthenticated && !SessionStorage.hasCompletedOnboarding() -> false // Wait for login first
            // All other cases: don't show onboarding
            else -> false
        }
    }

    CalendarTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            when {
                // Show onboarding for new users (after authentication)
                shouldShowOnboarding -> {
                    OnboardingScreen(
                        onOnboardingComplete = {
                            SessionStorage.setOnboardingCompleted(true)
                            shouldShowOnboarding = false
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                authState.isLoading || isInOAuthFlow -> {
                    // Loading state during session check or OAuth flow
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (isInOAuthFlow) stringResource(Res.string.completing_authentication)
                            else stringResource(Res.string.loading)
                        )
                    }
                }

                authState.isAuthenticated && authState.sessionToken != null -> {
                    when {
                        showPaywall -> {
                            println("🎯 App.kt - Showing paywall, isRevenueCatReady: ${authState.isRevenueCatReady}")
                            if (authState.isRevenueCatReady) {
                                RevenueCatPaywallScreen(
                                    onBackClick = {
                                        println("🔙 App.kt - Paywall back clicked")
                                        showPaywall = false
                                    },
                                    onSubscriptionSuccess = { planLabel ->
                                        println("✅ App.kt - Subscription success - closing paywall")
                                        showPaywall = false
                                        // Navigate to Settings and show success message
                                        settingsSuccessPlanLabel = planLabel
                                        navigateToSettings = true
                                    }
                                )
                            } else {
                                // Show loading while RevenueCat is setting up
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(stringResource(Res.string.paywall_configuring_message))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = { showPaywall = false }
                                    ) {
                                        Text(stringResource(Res.string.cancel_button))
                                    }
                                }
                            }
                        }

                        else -> {
                            val calendarViewModel = koinViewModel<CalendarViewModel>()

                            // Lifecycle observer para sincronizar recordatorios
                            ReminderLifecycleObserver(
                                reminderManager = reminderManager,
                                onSyncReminders = {
                                    // Trigger a rolling sync when app comes to foreground
                                    // This will be called automatically by the lifecycle observer
                                    calendarViewModel.refreshCalendarData()
                                }
                            )

                            CalendarScreen(
                                viewModel = calendarViewModel,
                                modifier = Modifier.fillMaxSize(),
                                sessionToken = authState.sessionToken!!,
                                userEmail = authState.userEmail,
                                fullName = authState.fullName,
                                profilePicture = authState.profilePicture,
                                subscriptionPlan = authState.subscriptionPlan,
                                creditsRemaining = authState.creditsRemaining,
                                voiceTranscriberFactory = voiceTranscriberFactory,
                                onTokenExpired = authViewModel::onTokenExpired,
                                onLogout = authViewModel::logout,
                                onDeleteAccount = authViewModel::deleteAccount,
                                onCreditsUpdate = authViewModel::updateCreditsAndSubscription,
                                onUserProfileUpdate = authViewModel::updateUserProfile,
                                onUpgradePlan = {
                                    println("⬆️ App.kt - onUpgradePlan triggered, setting showPaywall = true")
                                    showPaywall = true
                                },
                                navigateToSettings = navigateToSettings,
                                settingsSuccessPlanLabel = settingsSuccessPlanLabel,
                                onNavigatedToSettings = {
                                    println("🔁 App.kt - Navigated to Settings after subscription success")
                                    navigateToSettings = false
                                },
                                onSuccessMessageConsumed = {
                                    println("✅ App.kt - Subscription success message consumed, clearing label")
                                    settingsSuccessPlanLabel = null
                                }
                            )
                        }
                    }
                }

                else -> {
                    LoginScreen(
                        authState = authState,
                        onLoginStart = authViewModel::startLogin,
                        onError = authViewModel::clearError,
                        onAuthUrl = { authUrl, sessionId ->
                            authViewModel.startOAuthFlow(authUrl, sessionId)

                            // Open browser and wait for deep link callback
                            startSystemBrowserAuth(
                                authUrl = authUrl,
                                callbackScheme = "koogcalendar"
                            ) { resultUrl, error ->
                                authViewModel.handleOAuthCallback(resultUrl, error)
                            }
                        },
                        onCancelOAuth = if (isInOAuthFlow) {
                            { authViewModel.cancelOAuthFlow("Authentication cancelled by user") }
                        } else null
                    )
                }
            }
        }
    }
}


