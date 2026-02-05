package com.smartcal.app.presentation.subscription

import smartcalai.composeapp.generated.resources.Res
import smartcalai.composeapp.generated.resources.paywall_back_button
import smartcalai.composeapp.generated.resources.paywall_error
import smartcalai.composeapp.generated.resources.paywall_retry_button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import com.revenuecat.purchases.kmp.ui.revenuecatui.Paywall
import com.revenuecat.purchases.kmp.ui.revenuecatui.PaywallOptions
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RevenueCatPaywallScreen(
    onBackClick: () -> Unit,
    onSubscriptionSuccess: (planLabel: String?) -> Unit
) {
    val viewModel: RevenueCatPaywallViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isUserSubscribed by viewModel.isUserSubscribed.collectAsState()
    val latestSubscription by viewModel.latestSubscription.collectAsState()


    fun planLabel(): String {
        val info = latestSubscription
        if (info == null) return ""
        // Prefer entitlementId-based naming if you keep them aligned with backend
        return when (info.entitlementId.lowercase()) {
            "pro" -> "Pro"
            "starter" -> "Starter"
            else -> when {
                info.productId.contains("pro", ignoreCase = true) -> "Pro"
                info.productId.contains("starter", ignoreCase = true) -> "Starter"
                else -> info.productId
            }
        }
    }

    // Inicializar RevenueCat y cargar offering
    LaunchedEffect(Unit) {
        println("🚀 RevenueCatPaywallScreen - Starting to load offering")
        viewModel.loadOffering()
    }

    // Close paywall ONLY on a true subscription transition while this screen is open
    var prevSubscribed by remember { mutableStateOf(isUserSubscribed) }
    var handledSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(isUserSubscribed) {
        println("🔔 RevenueCatPaywallScreen - sub status: prev=$prevSubscribed -> current=$isUserSubscribed")
        if (!handledSuccess && !prevSubscribed && isUserSubscribed) {
            handledSuccess = true
            val plan = planLabel()
            onSubscriptionSuccess(plan.ifBlank { null })
        }
        prevSubscribed = isUserSubscribed
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            // UI del Paywall de RevenueCat
            when (val currentState = uiState) {
            is RevenueCatPaywallState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is RevenueCatPaywallState.Success -> {
                val hasPackages = currentState.offering.availablePackages.isNotEmpty()
                if (!hasPackages) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(stringResource(Res.string.paywall_error, "No hay paquetes disponibles para la oferta actual."))
                        Button(onClick = { viewModel.loadOffering() }) {
                            Text(stringResource(Res.string.paywall_retry_button))
                        }
                        OutlinedButton(onClick = { onBackClick() }) {
                            Text(stringResource(Res.string.paywall_back_button))
                        }
                    }
                } else {
                    val options = remember {
                        PaywallOptions(
                            dismissRequest = { onBackClick() }
                        ) {
                            offering = currentState.offering
                            shouldDisplayDismissButton = true
                        }
                    }

                    Paywall(options)
                }
            }

            is RevenueCatPaywallState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(stringResource(Res.string.paywall_error, currentState.message))
                        Button(
                            onClick = { viewModel.loadOffering() }
                        ) {
                            Text(stringResource(Res.string.paywall_retry_button))
                        }
                        OutlinedButton(
                            onClick = { onBackClick() }
                        ) {
                            Text(stringResource(Res.string.paywall_back_button))
                        }
                    }
                }
            }
            }
        }
    }
}