package com.smartcal.app.presentation.components

import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import com.smartcal.app.data.subscription.EntitlementHelper
import com.smartcal.app.data.subscription.RevenueCatConfig

/**
 * ContentGate siguiendo el patrón de documentación de RevenueCat para validar entitlements
 */
@Composable
fun ContentGate(
    entitlementIdentifier: String = RevenueCatConfig.Entitlements.PRO,
    premiumContent: @Composable () -> Unit,
    freeContent: @Composable () -> Unit
) {
    var isEntitled by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(entitlementIdentifier) {
        scope.launch {
            isEntitled = EntitlementHelper.validateEntitlement(entitlementIdentifier)
            isLoading = false
        }
    }
    
    if (isLoading) {
        // Puedes mostrar un estado de carga aquí si es necesario
        freeContent()
    } else if (isEntitled) {
        premiumContent()
    } else {
        freeContent()
    }
}