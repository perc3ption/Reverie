package com.perceptiveus.reverie.feature.player.visualizer

/**
 * Retro visualizer skins inspired by early-2000s desktop players (Winamp / WMP era).
 */
enum class VisualizerStyle(
    val label: String,
    val isPremium: Boolean,
) {
    SPECTRUM(label = "SPECTRUM", isPremium = false),
    SCOPE(label = "SCOPE", isPremium = true),
    RADIAL(label = "RADIAL", isPremium = true),
    VU(label = "VU", isPremium = true),
    STARBURST(label = "STARBURST", isPremium = true),
}
