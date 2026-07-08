package com.perceptiveus.reverie.core.entitlement

/**
 * Central gate for premium features and free-tier limits.
 */
class FeatureAccessChecker(
    private val entitlementsProvider: () -> Entitlements,
) {
    companion object {
        const val FREE_MAX_SONGS = 500
        const val FREE_MAX_PLAYLISTS = 3
    }

    fun isPremium(): Boolean = entitlementsProvider().isPremium

    fun canAccess(feature: AppFeature): Boolean = isPremium()

    fun maxSongs(): Int = if (isPremium()) Int.MAX_VALUE else FREE_MAX_SONGS

    fun maxPlaylists(): Int = if (isPremium()) Int.MAX_VALUE else FREE_MAX_PLAYLISTS

    fun canAddSongs(currentCount: Int): Boolean = currentCount < maxSongs()

    fun canCreatePlaylist(currentCount: Int): Boolean = currentCount < maxPlaylists()
}
