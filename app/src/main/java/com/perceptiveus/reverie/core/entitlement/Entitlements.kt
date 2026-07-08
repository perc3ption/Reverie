package com.perceptiveus.reverie.core.entitlement

/**
 * Snapshot of the user's purchase state.
 * Replace sourcing with Google Play Billing when billing is implemented.
 */
data class Entitlements(
    val isPremium: Boolean = false,
    val purchaseToken: String? = null,
)
