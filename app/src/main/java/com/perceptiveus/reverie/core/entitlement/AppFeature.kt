package com.perceptiveus.reverie.core.entitlement

/**
 * Premium-gated capabilities. Free-tier limits (song/playlist caps) are enforced
 * separately via [FeatureAccessChecker].
 */
enum class AppFeature {
    UNLIMITED_LIBRARY,
    METADATA_EDITING,
    ALBUM_ART_EDITING,
    TAGS,
    RATINGS,
    UNLIMITED_PLAYLISTS,
    LYRICS,
    PLAYBACK_SCOPE,
    COLLECTIONS,
    SMART_PLAYLISTS,
    ADVANCED_VISUALIZERS,
    ADVANCED_SEARCH,
    LIBRARY_STATS,
}
