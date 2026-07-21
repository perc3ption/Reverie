package com.perceptiveus.reverie.core.entitlement

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.perceptiveus.reverie.BuildConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Google Play Billing-backed entitlements for the one-time Premium unlock.
 *
 * Debug builds additionally support:
 * - Auto-grant on first launch (default on)
 * - Manual Unlock/Revoke via [setPremiumForTesting] (persisted)
 */
class PlayBillingEntitlementRepository(
    context: Context,
    private val scope: CoroutineScope,
) : EntitlementRepository, PurchasesUpdatedListener {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _entitlements = MutableStateFlow(Entitlements())
    override val entitlements: StateFlow<Entitlements> = _entitlements.asStateFlow()

    private val _premiumProduct = MutableStateFlow<PremiumProductInfo?>(null)
    override val premiumProduct: StateFlow<PremiumProductInfo?> = _premiumProduct.asStateFlow()

    private val connectMutex = Mutex()
    private var connectionDeferred: CompletableDeferred<BillingResult>? = null
    private var purchaseDeferred: CompletableDeferred<Result<Unit>>? = null

    @Volatile
    private var playOwned = false

    @Volatile
    private var playPurchaseToken: String? = null

    private var cachedProductDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    init {
        publishEntitlements()
        scope.launch {
            runCatching { refreshFromPlay() }
                .onFailure { Log.w(TAG, "Initial Play Billing refresh failed", it) }
        }
    }

    override suspend fun purchasePremium(activity: Activity): Result<Unit> {
        if (playOwned) {
            publishEntitlements()
            return Result.success(Unit)
        }
        if (!ensureConnected()) {
            return Result.failure(
                IllegalStateException("Unable to connect to Google Play Billing."),
            )
        }

        val details = loadPremiumProductDetails()
            ?: return Result.failure(
                IllegalStateException(
                    "Premium product unavailable. Create \"${BillingProducts.PREMIUM_UNLOCK}\" " +
                        "as a one-time product in Play Console, or use a Play-distributed test build.",
                ),
            )

        val offerToken = details.resolveOfferToken()
            ?: return Result.failure(IllegalStateException("No purchase offer available for Premium."))

        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()

        val deferred = CompletableDeferred<Result<Unit>>()
        purchaseDeferred = deferred
        val launchResult = withContext(Dispatchers.Main) {
            billingClient.launchBillingFlow(activity, params)
        }
        if (launchResult.responseCode != BillingResponseCode.OK) {
            purchaseDeferred = null
            return Result.failure(
                IllegalStateException(billingMessage(launchResult, "Could not start purchase")),
            )
        }
        return deferred.await()
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        return runCatching {
            refreshFromPlay()
            playOwned
        }
    }

    override suspend fun setPremiumForTesting(enabled: Boolean) {
        if (!BuildConfig.DEBUG) return
        prefs.edit().putBoolean(KEY_DEBUG_PREMIUM, enabled).apply()
        publishEntitlements()
    }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: MutableList<Purchase>?) {
        val deferred = purchaseDeferred
        purchaseDeferred = null

        when (billingResult.responseCode) {
            BillingResponseCode.OK -> {
                scope.launch {
                    val handled = runCatching {
                        processPurchases(purchases.orEmpty())
                        refreshFromPlay()
                    }
                    deferred?.complete(
                        handled.fold(
                            onSuccess = { Result.success(Unit) },
                            onFailure = { Result.failure(it) },
                        ),
                    )
                }
            }
            BillingResponseCode.USER_CANCELED -> {
                deferred?.complete(Result.failure(PurchaseCanceledException()))
            }
            else -> {
                deferred?.complete(
                    Result.failure(
                        IllegalStateException(billingMessage(billingResult, "Purchase failed")),
                    ),
                )
            }
        }
    }

    private suspend fun refreshFromPlay() {
        if (!ensureConnected()) {
            publishEntitlements()
            return
        }
        loadPremiumProductDetails()
        val (billingResult, purchases) = queryInAppPurchases()
        if (billingResult.responseCode == BillingResponseCode.OK) {
            processPurchases(purchases)
        } else {
            Log.w(TAG, "queryPurchasesAsync failed: ${billingResult.debugMessage}")
        }
        publishEntitlements()
    }

    private suspend fun processPurchases(purchases: List<Purchase>) {
        var owned = false
        var token: String? = null
        for (purchase in purchases) {
            if (!purchase.products.contains(BillingProducts.PREMIUM_UNLOCK)) continue
            if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) continue
            if (!purchase.isAcknowledged) {
                val ack = acknowledge(purchase.purchaseToken)
                if (ack.responseCode != BillingResponseCode.OK) {
                    Log.w(TAG, "Acknowledge failed: ${ack.debugMessage}")
                    continue
                }
            }
            owned = true
            token = purchase.purchaseToken
        }
        playOwned = owned
        playPurchaseToken = token
    }

    private suspend fun loadPremiumProductDetails(): ProductDetails? {
        cachedProductDetails?.let { return it }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(BillingProducts.PREMIUM_UNLOCK)
                        .setProductType(ProductType.INAPP)
                        .build(),
                ),
            )
            .build()

        val (billingResult, detailsList) = queryProductDetails(params)
        if (billingResult.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryProductDetails failed: ${billingResult.debugMessage}")
            _premiumProduct.value = null
            return null
        }
        val details = detailsList.firstOrNull { it.productId == BillingProducts.PREMIUM_UNLOCK }
        cachedProductDetails = details
        _premiumProduct.value = details?.toPremiumProductInfo()
        return details
    }

    private suspend fun queryProductDetails(
        params: QueryProductDetailsParams,
    ): Pair<BillingResult, List<ProductDetails>> =
        suspendCancellableCoroutine { cont ->
            billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsResult ->
                if (cont.isActive) {
                    cont.resume(billingResult to productDetailsResult.productDetailsList.orEmpty())
                }
            }
        }

    private suspend fun queryInAppPurchases(): Pair<BillingResult, List<Purchase>> =
        suspendCancellableCoroutine { cont ->
            billingClient.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder()
                    .setProductType(ProductType.INAPP)
                    .build(),
            ) { billingResult, purchases ->
                if (cont.isActive) {
                    cont.resume(billingResult to purchases.orEmpty())
                }
            }
        }

    private suspend fun acknowledge(purchaseToken: String): BillingResult =
        suspendCancellableCoroutine { cont ->
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchaseToken)
                    .build(),
            ) { billingResult ->
                if (cont.isActive) cont.resume(billingResult)
            }
        }

    private suspend fun ensureConnected(): Boolean = connectMutex.withLock {
        if (billingClient.isReady) return@withLock true

        val existing = connectionDeferred
        if (existing != null) {
            return@withLock existing.await().responseCode == BillingResponseCode.OK
        }

        val deferred = CompletableDeferred<BillingResult>()
        connectionDeferred = deferred
        billingClient.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connectionDeferred = null
                    deferred.complete(billingResult)
                }

                override fun onBillingServiceDisconnected() {
                    connectionDeferred = null
                }
            },
        )
        deferred.await().responseCode == BillingResponseCode.OK
    }

    private fun debugPremiumEnabled(): Boolean {
        if (!BuildConfig.DEBUG) return false
        // Default true → auto-grant Premium on Studio debug installs.
        return prefs.getBoolean(KEY_DEBUG_PREMIUM, true)
    }

    private fun publishEntitlements() {
        val debugBypass = debugPremiumEnabled()
        val premium = playOwned || debugBypass
        _entitlements.value = Entitlements(
            isPremium = premium,
            purchaseToken = playPurchaseToken,
            isDebugBypass = debugBypass && !playOwned,
        )
    }

    private fun ProductDetails.toPremiumProductInfo(): PremiumProductInfo =
        PremiumProductInfo(
            productId = productId,
            title = title,
            formattedPrice = resolveFormattedPrice() ?: "",
        )

    private fun ProductDetails.resolveOfferToken(): String? =
        oneTimePurchaseOfferDetailsList?.firstOrNull()?.offerToken
            ?: @Suppress("DEPRECATION") oneTimePurchaseOfferDetails?.offerToken

    private fun ProductDetails.resolveFormattedPrice(): String? =
        oneTimePurchaseOfferDetailsList?.firstOrNull()?.formattedPrice
            ?: @Suppress("DEPRECATION") oneTimePurchaseOfferDetails?.formattedPrice

    private fun billingMessage(result: BillingResult, fallback: String): String {
        val detail = result.debugMessage.trim()
        return if (detail.isNotEmpty()) "$fallback (${result.responseCode}: $detail)" else fallback
    }

    companion object {
        private const val TAG = "PlayBilling"
        private const val PREFS_NAME = "reverie_billing"
        private const val KEY_DEBUG_PREMIUM = "debug_premium_enabled"
    }
}

class PurchaseCanceledException : Exception("Purchase canceled")
