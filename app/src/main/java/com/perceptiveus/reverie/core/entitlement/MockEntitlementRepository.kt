package com.perceptiveus.reverie.core.entitlement

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory entitlement store for development.
 * Replace with Play Billing purchase verification when ready.
 */
class MockEntitlementRepository : EntitlementRepository {

    private val _entitlements = MutableStateFlow(Entitlements(isPremium = false))
    override val entitlements: StateFlow<Entitlements> = _entitlements.asStateFlow()

    override suspend fun restorePurchases(): Result<Boolean> {
        // TODO: Query Play Billing for existing purchases and update entitlements.
        return Result.success(_entitlements.value.isPremium)
    }

    override suspend fun setPremiumForTesting(enabled: Boolean) {
        _entitlements.value = Entitlements(
            isPremium = enabled,
            purchaseToken = if (enabled) "mock_premium_token" else null,
        )
    }
}
