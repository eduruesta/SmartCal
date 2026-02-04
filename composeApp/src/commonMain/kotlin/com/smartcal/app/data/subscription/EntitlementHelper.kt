package com.smartcal.app.data.subscription

import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.models.PurchasesException

/**
 * Helper object para validar entitlements siguiendo las mejores prácticas de RevenueCat
 */
object EntitlementHelper {
    
    /**
     * Valida si el usuario tiene un entitlement activo usando el patrón de RevenueCat:
     * val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
     * val isEntitled = customerInfo?.entitlements[ENTITLEMENT_IDENTIFIER]?.isActive == true
     */
    suspend fun validateEntitlement(entitlementIdentifier: String): Boolean {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            customerInfo.entitlements.active[entitlementIdentifier]?.isActive == true
        } catch (e: PurchasesException) {
            println("Error validando entitlement: ${e.message}")
            false
        }
    }
    
    /**
     * Valida el entitlement pro específicamente
     */
    suspend fun validateProEntitlement(): Boolean {
        return validateEntitlement(RevenueCatConfig.Entitlements.PRO)
    }
    
    /**
     * Valida el entitlement starter específicamente
     */
    suspend fun validateStarterEntitlement(): Boolean {
        return validateEntitlement(RevenueCatConfig.Entitlements.STARTER)
    }
    
    /**
     * Verifica si el usuario tiene algún entitlement activo
     */
    suspend fun hasAnyActiveEntitlement(): Boolean {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            customerInfo.entitlements.active.isNotEmpty()
        } catch (e: PurchasesException) {
            println("Error verificando entitlements activos: ${e.message}")
            false
        }
    }
}