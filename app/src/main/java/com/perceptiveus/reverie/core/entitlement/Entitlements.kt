package com.perceptiveus.reverie.core.entitlement

/**
 * Snapshot of the user's purchase / unlock state.
 */
data class Entitlements(
    val isPremium: Boolean = false,
    val purchaseToken: String? = null,
    /** True when premium comes from a debug bypass, not a Play purchase. */
    val isDebugBypass: Boolean = false,
)

/**
 * Localized product info from Play for display on the upgrade UI.
 */
data class PremiumProductInfo(
    val productId: String,
    val title: String,
    val formattedPrice: String,
)
