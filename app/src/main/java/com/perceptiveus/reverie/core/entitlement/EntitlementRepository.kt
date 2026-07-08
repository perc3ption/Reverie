package com.perceptiveus.reverie.core.entitlement

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction over premium entitlement state.
 * [MockEntitlementRepository] is used for now; swap for Play Billing later.
 */
interface EntitlementRepository {
    val entitlements: StateFlow<Entitlements>

    /** Placeholder for Google Play Billing restore flow. */
    suspend fun restorePurchases(): Result<Boolean>

    /** Dev-only toggle; remove or guard behind debug builds when billing ships. */
    suspend fun setPremiumForTesting(enabled: Boolean)
}
