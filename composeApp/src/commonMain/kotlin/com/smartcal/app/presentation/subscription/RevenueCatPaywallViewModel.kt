package com.smartcal.app.presentation.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.smartcal.app.data.subscription.RevenueCatManager
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.models.Offering
import com.revenuecat.purchases.kmp.models.PurchasesException

sealed class RevenueCatPaywallState {
    object Loading : RevenueCatPaywallState()
    data class Success(val offering: Offering) : RevenueCatPaywallState()
    data class Error(val message: String) : RevenueCatPaywallState()
}

class RevenueCatPaywallViewModel(
    private val revenueCatManager: RevenueCatManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<RevenueCatPaywallState>(RevenueCatPaywallState.Loading)
    val uiState: StateFlow<RevenueCatPaywallState> = _uiState.asStateFlow()
    
    private val _isUserSubscribed = MutableStateFlow(false)
    val isUserSubscribed: StateFlow<Boolean> = _isUserSubscribed.asStateFlow()
    
    private val _latestSubscription = MutableStateFlow<com.smartcal.app.data.subscription.SubscriptionInfo?>(null)
    val latestSubscription: StateFlow<com.smartcal.app.data.subscription.SubscriptionInfo?> = _latestSubscription.asStateFlow()
    
    init {
        observeSubscriptionStatus()
        observeActiveSubscription()
    }
    
    private fun observeSubscriptionStatus() {
        viewModelScope.launch {
            revenueCatManager.isUserSubscribed.collect { isSubscribed ->
                println("🔄 RevenueCatPaywallViewModel - Subscription status changed: $isSubscribed")
                _isUserSubscribed.value = isSubscribed
            }
        }
    }
    
    private fun observeActiveSubscription() {
        viewModelScope.launch {
            revenueCatManager.activeSubscription.collect { info ->
                if (info != null) {
                    println("🧾 RevenueCatPaywallViewModel - Active subscription: entitlement='${info.entitlementId}', product='${info.productId}'")
                }
                _latestSubscription.value = info
            }
        }
    }
    
    fun loadOffering() {
        viewModelScope.launch {
            println("📦 RevenueCatPaywallViewModel - Loading offering...")
            _uiState.value = RevenueCatPaywallState.Loading
            
            try {
                val offerings = Purchases.sharedInstance.awaitOfferings()
                println("📋 RevenueCatPaywallViewModel - Got offerings: ${offerings.current?.identifier}")
                offerings.current?.let { currentOffering ->
                    println("✅ RevenueCatPaywallViewModel - Setting Success state with offering: ${currentOffering.identifier}")
                    _uiState.value = RevenueCatPaywallState.Success(currentOffering)
                } ?: run {
                    println("❌ RevenueCatPaywallViewModel - No current offering available")
                    _uiState.value = RevenueCatPaywallState.Error("No hay offering actual disponible")
                }
            } catch (e: PurchasesException) {
                println("❌ RevenueCatPaywallViewModel - Error loading offering: ${e.message}")
                _uiState.value = RevenueCatPaywallState.Error(e.message ?: "Error desconocido")
            }
        }
    }
}