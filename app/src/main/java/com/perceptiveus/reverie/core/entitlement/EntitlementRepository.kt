package com.perceptiveus.reverie.core.entitlement

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

/**
 * Premium entitlement + Google Play Billing.
 */
interface EntitlementRepository {
    val entitlements: StateFlow<Entitlements>

    /** Localized price/title from Play, or null while unavailable. */
    val premiumProduct: StateFlow<PremiumProductInfo?>

    /**
     * Launches the Play purchase sheet for the one-time Premium product.
     * Must be called from the main thread with a foreground [Activity].
     */
    suspend fun purchasePremium(activity: Activity): Result<Unit>

    /** Re-queries Play for existing Premium ownership (reinstall / new device). */
    suspend fun restorePurchases(): Result<Boolean>

    /**
     * Debug-only: force premium on/off for Studio testing.
     * No-op in release builds.
     */
    suspend fun setPremiumForTesting(enabled: Boolean)
}
