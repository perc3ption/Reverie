package com.perceptiveus.reverie.core.entitlement

import android.app.Activity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory entitlement store for unit tests and previews.
 */
class MockEntitlementRepository(
    initiallyPremium: Boolean = false,
) : EntitlementRepository {

    private val _entitlements = MutableStateFlow(
        Entitlements(
            isPremium = initiallyPremium,
            purchaseToken = if (initiallyPremium) "mock_premium_token" else null,
            isDebugBypass = initiallyPremium,
        ),
    )
    override val entitlements: StateFlow<Entitlements> = _entitlements.asStateFlow()

    private val _premiumProduct = MutableStateFlow(
        PremiumProductInfo(
            productId = BillingProducts.PREMIUM_UNLOCK,
            title = "Reverie Premium",
            formattedPrice = "$4.99",
        ),
    )
    override val premiumProduct: StateFlow<PremiumProductInfo?> = _premiumProduct.asStateFlow()

    override suspend fun purchasePremium(activity: Activity): Result<Unit> {
        setPremiumForTesting(true)
        return Result.success(Unit)
    }

    override suspend fun restorePurchases(): Result<Boolean> {
        return Result.success(_entitlements.value.isPremium)
    }

    override suspend fun setPremiumForTesting(enabled: Boolean) {
        _entitlements.value = Entitlements(
            isPremium = enabled,
            purchaseToken = if (enabled) "mock_premium_token" else null,
            isDebugBypass = enabled,
        )
    }
}
