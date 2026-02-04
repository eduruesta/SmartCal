package com.smartcal.app.data.subscription

import com.revenuecat.purchases.kmp.LogLevel
import com.revenuecat.purchases.kmp.Purchases
import com.revenuecat.purchases.kmp.PurchasesDelegate
import com.revenuecat.purchases.kmp.configure
import com.revenuecat.purchases.kmp.ktx.awaitCustomerInfo
import com.revenuecat.purchases.kmp.ktx.awaitLogIn
import com.revenuecat.purchases.kmp.ktx.awaitOfferings
import com.revenuecat.purchases.kmp.ktx.awaitPurchase
import com.revenuecat.purchases.kmp.models.PurchasesException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.revenuecat.purchases.kmp.models.CustomerInfo as RCCustomerInfo
import com.revenuecat.purchases.kmp.models.Offering as RCOffering
import com.revenuecat.purchases.kmp.models.Package as RCPackage
import com.revenuecat.purchases.kmp.models.StoreProduct as RCStoreProduct

// Wrapper models para mantener la interfaz existente
data class CustomerInfo(
    val entitlements: Map<String, Entitlement> = emptyMap(),
    val original: RCCustomerInfo? = null
)

data class Entitlement(
    val isActive: Boolean = false
)

data class Offering(
    val identifier: String,
    val serverDescription: String,
    val availablePackages: List<Package>,
    val original: RCOffering? = null
)

data class Package(
    val identifier: String,
    val packageType: String,
    val storeProduct: StoreProduct,
    val original: RCPackage? = null
)

data class StoreProduct(
    val id: String,
    val title: String,
    val price: String,
    val original: RCStoreProduct? = null
)

// Detailed subscription info derived from RCCustomerInfo
// Contains enough to update backend and display a success message
data class SubscriptionInfo(
    val entitlementId: String,
    val productId: String,
    val latestPurchaseDateMillis: Long?,
    val expirationDateMillis: Long?,
    val isSandbox: Boolean
)

interface RevenueCatManager {
    val customerInfo: StateFlow<CustomerInfo?>
    val isUserSubscribed: StateFlow<Boolean>
    val isPremiumActive: StateFlow<Boolean>
    val activeSubscription: StateFlow<SubscriptionInfo?>
    
    suspend fun initialize(apiKey: String)
    suspend fun loginUser(googleSub: String): Result<CustomerInfo>
    suspend fun getOfferings(): Result<List<Offering>>
    suspend fun purchasePackage(packageToPurchase: Package): Result<CustomerInfo>
    suspend fun restorePurchases(): Result<CustomerInfo>
    suspend fun getCustomerInfo(): Result<CustomerInfo>
    fun checkEntitlement(entitlementId: String): Boolean
}

class RevenueCatManagerImpl : RevenueCatManager, PurchasesDelegate {
    
    private val _customerInfo = MutableStateFlow<CustomerInfo?>(null)
    override val customerInfo: StateFlow<CustomerInfo?> = _customerInfo.asStateFlow()
    
    private val _isUserSubscribed = MutableStateFlow(false)
    override val isUserSubscribed: StateFlow<Boolean> = _isUserSubscribed.asStateFlow()
    
    private val _isPremiumActive = MutableStateFlow(false)
    override val isPremiumActive: StateFlow<Boolean> = _isPremiumActive.asStateFlow()
    
    private val _activeSubscription = MutableStateFlow<SubscriptionInfo?>(null)
    override val activeSubscription: StateFlow<SubscriptionInfo?> = _activeSubscription.asStateFlow()
    
    private var isInitialized = false
    
    override suspend fun initialize(apiKey: String) {
        try {
            if (!isInitialized) {
                Purchases.logLevel = LogLevel.DEBUG
                Purchases.configure(apiKey = apiKey) {
                    // Opcional: establecer ID de usuario de la app
                    // appUserId = "<app_user_id>"
                }
                
                // Establecer el delegate para escuchar actualizaciones de customer info
                Purchases.sharedInstance.delegate = this
                
                isInitialized = true
                
                // Cargar información inicial del cliente
                updateCustomerInfo()
            }
        } catch (e: Exception) {
            println("Error de inicialización de RevenueCat: ${e.message}")
        }
    }
    
    override suspend fun loginUser(googleSub: String): Result<CustomerInfo> {
        return try {
            if (!isInitialized) {
                return Result.failure(Exception("RevenueCat no inicializado. Llama a initialize() primero."))
            }
            
            println("🔐 RevenueCat: Logging in user with Google Sub: $googleSub")
            val loginResult = Purchases.sharedInstance.awaitLogIn(googleSub)
            val convertedCustomerInfo = convertCustomerInfo(loginResult.customerInfo)
            
            // Actualizar el estado local
            updateCustomerInfoState(convertedCustomerInfo)
            
            println("✅ RevenueCat: Successfully logged in user $googleSub")
            Result.success(convertedCustomerInfo)
        } catch (e: Exception) {
            println("❌ RevenueCat: Error logging in user: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getOfferings(): Result<List<Offering>> {
        return try {
            if (!isInitialized) {
                return Result.failure(Exception("RevenueCat no inicializado. Llama a initialize() primero."))
            }
            val offerings = Purchases.sharedInstance.awaitOfferings()
            val convertedOfferings = offerings.all.values.map { rcOffering ->
                Offering(
                    identifier = rcOffering.identifier,
                    serverDescription = rcOffering.serverDescription,
                    availablePackages = rcOffering.availablePackages.map { rcPackage ->
                        Package(
                            identifier = rcPackage.identifier,
                            packageType = rcPackage.packageType.name,
                            storeProduct = StoreProduct(
                                id = rcPackage.storeProduct.id,
                                title = rcPackage.storeProduct.title,
                                price = rcPackage.storeProduct.price.formatted,
                                original = rcPackage.storeProduct
                            ),
                            original = rcPackage
                        )
                    },
                    original = rcOffering
                )
            }
            Result.success(convertedOfferings)
        } catch (e: PurchasesException) {
            println("Error al obtener offerings de RevenueCat: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun purchasePackage(packageToPurchase: Package): Result<CustomerInfo> {
        return try {
            val rcPackage = packageToPurchase.original
                ?: return Result.failure(Exception("Paquete original no encontrado"))
            
            val purchaseResult = Purchases.sharedInstance.awaitPurchase(rcPackage)
            val convertedCustomerInfo = convertCustomerInfo(purchaseResult.customerInfo)
            updateCustomerInfoState(convertedCustomerInfo)
            Result.success(convertedCustomerInfo)
        } catch (e: PurchasesException) {
            println("Error de compra de RevenueCat: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun restorePurchases(): Result<CustomerInfo> {
        return try {
            Purchases.sharedInstance.restorePurchases(
                onError = { error ->
                    println("Error al restaurar compras de RevenueCat: ${error.message}")
                },
                onSuccess = { customerInfo ->
                    println("Restauración de compras de RevenueCat exitosa")
                }
            )
            
            // Obtener información actualizada del cliente
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            val convertedCustomerInfo = convertCustomerInfo(customerInfo)
            updateCustomerInfoState(convertedCustomerInfo)
            Result.success(convertedCustomerInfo)
        } catch (e: Exception) {
            println("Error al restaurar compras de RevenueCat: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getCustomerInfo(): Result<CustomerInfo> {
        return try {
            val customerInfo = Purchases.sharedInstance.awaitCustomerInfo()
            val convertedCustomerInfo = convertCustomerInfo(customerInfo)
            Result.success(convertedCustomerInfo)
        } catch (e: PurchasesException) {
            println("Error al obtener customer info de RevenueCat: ${e.message}")
            Result.failure(e)
        }
    }
    
    override fun checkEntitlement(entitlementId: String): Boolean {
        val currentCustomerInfo = _customerInfo.value
        return currentCustomerInfo?.entitlements?.get(entitlementId)?.isActive == true
    }
    
    private suspend fun updateCustomerInfo() {
        try {
            val result = getCustomerInfo()
            result.getOrNull()?.let { customerInfo ->
                updateCustomerInfoState(customerInfo)
                // Update active subscription details from original if available
                customerInfo.original?.let { rc ->
                    _activeSubscription.value = mapToSubscriptionInfo(rc)
                }
            }
        } catch (e: Exception) {
            println("Error al actualizar customer info: ${e.message}")
        }
    }
    
    private fun convertCustomerInfo(rcCustomerInfo: RCCustomerInfo): CustomerInfo {
        val entitlements = rcCustomerInfo.entitlements.active.mapValues { (_, entitlement) ->
            Entitlement(isActive = true)
        }
        
        return CustomerInfo(
            entitlements = entitlements,
            original = rcCustomerInfo
        )
    }
    
    private fun mapToSubscriptionInfo(rcCustomerInfo: RCCustomerInfo): SubscriptionInfo? {
        return try {
            val active = rcCustomerInfo.entitlements.active
            if (active.isEmpty()) return null
            val entry = active.entries.first()
            val entitlementId = entry.key
            val info = entry.value
            SubscriptionInfo(
                entitlementId = entitlementId,
                productId = info.productIdentifier,
                latestPurchaseDateMillis = info.latestPurchaseDateMillis,
                expirationDateMillis = info.expirationDateMillis,
                isSandbox = info.isSandbox
            )
        } catch (e: Exception) {
            println("⚠️ Error mapping SubscriptionInfo: ${e.message}")
            null
        }
    }
    
    private fun updateCustomerInfoState(customerInfo: CustomerInfo) {
        _customerInfo.value = customerInfo
        _isUserSubscribed.value = customerInfo.entitlements.values.any { it.isActive }
        _isPremiumActive.value = customerInfo.entitlements[RevenueCatConfig.Entitlements.PRO]?.isActive == true
    }
    
    // Métodos PurchasesDelegate - llamados automáticamente cuando CustomerInfo cambia
    override fun onCustomerInfoUpdated(customerInfo: RCCustomerInfo) {
        val convertedCustomerInfo = convertCustomerInfo(customerInfo)
        updateCustomerInfoState(convertedCustomerInfo)
        _activeSubscription.value = mapToSubscriptionInfo(customerInfo)
        println("RevenueCat: Customer info actualizado automáticamente - isPremium: ${_isPremiumActive.value}")
        _activeSubscription.value?.let { sub ->
            println("🧾 Active subscription updated: entitlement='${sub.entitlementId}', productId='${sub.productId}'")
        }
    }
    
    override fun onPurchasePromoProduct(
        product: com.revenuecat.purchases.kmp.models.StoreProduct,
        startPurchase: (onError: (error: com.revenuecat.purchases.kmp.models.PurchasesError, userCancelled: Boolean) -> Unit, onSuccess: (storeTransaction: com.revenuecat.purchases.kmp.models.StoreTransaction, customerInfo: RCCustomerInfo) -> Unit) -> Unit
    ) {
        // Manejar compras promocionales si es necesario
        println("RevenueCat: Compra promocional solicitada para producto: ${product.id}")
    }
}