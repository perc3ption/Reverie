package com.perceptiveus.reverie.core.entitlement

/**
 * Google Play product IDs. Create matching one-time products in Play Console
 * (Monetize → Products → In-app products) before release testing.
 */
object BillingProducts {
    /** One-time Premium unlock. Must match the product ID in Play Console. */
    const val PREMIUM_UNLOCK = "reverie_premium"
}
