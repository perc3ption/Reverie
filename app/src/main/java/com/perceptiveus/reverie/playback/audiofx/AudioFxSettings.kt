package com.perceptiveus.reverie.playback.audiofx

/**
 * User-facing audio processing settings (EQ v1 + loudness/crossfade/gapless v2).
 */
data class AudioFxSettings(
    val eqEnabled: Boolean = false,
    /** Overall preamp in dB, typically -12..+12. */
    val preampDb: Float = 0f,
    /** 10 graphic-EQ band gains in dB (-12..+12). */
    val bandsDb: List<Float> = List(BAND_COUNT) { 0f },
    /** Active preset id, or [PRESET_CUSTOM] when sliders diverge. */
    val presetId: String = PRESET_FLAT,
    /** Extra low-shelf bass boost in dB (0..14). */
    val bassBoostDb: Float = 0f,
    /** Soft automatic loudness leveling. */
    val loudnessEnabled: Boolean = false,
    /** Crossfade length in ms (0 = off). Mutually exclusive with gapless feel. */
    val crossfadeMs: Int = 0,
    /**
     * Prefer gapless transitions when crossfade is off.
     * When false, a short silence is inserted between auto-advanced tracks.
     */
    val gaplessEnabled: Boolean = true,
) {
    fun band(index: Int): Float = bandsDb.getOrElse(index) { 0f }

    fun withBand(index: Int, db: Float): AudioFxSettings {
        val next = bandsDb.toMutableList()
        if (index in next.indices) next[index] = db.coerceIn(MIN_GAIN_DB, MAX_GAIN_DB)
        return copy(bandsDb = next, presetId = PRESET_CUSTOM)
    }

    fun withPreset(preset: AudioFxPreset): AudioFxSettings = copy(
        eqEnabled = true,
        preampDb = preset.preampDb,
        bandsDb = preset.bandsDb,
        presetId = preset.id,
        bassBoostDb = preset.bassBoostDb,
    )

    companion object {
        const val BAND_COUNT = 10
        const val MIN_GAIN_DB = -12f
        const val MAX_GAIN_DB = 12f
        const val MAX_BASS_BOOST_DB = 14f
        const val MAX_CROSSFADE_MS = 12_000
        const val PRESET_FLAT = "flat"
        const val PRESET_CUSTOM = "custom"

        val BAND_FREQUENCIES_HZ = listOf(
            31, 62, 125, 250, 500, 1_000, 2_000, 4_000, 8_000, 16_000,
        )

        val BAND_LABELS = listOf(
            "31", "62", "125", "250", "500", "1k", "2k", "4k", "8k", "16k",
        )

        val Default = AudioFxSettings()
    }
}

data class AudioFxPreset(
    val id: String,
    val label: String,
    val preampDb: Float = 0f,
    val bandsDb: List<Float>,
    val bassBoostDb: Float = 0f,
)

object AudioFxPresets {
    val ALL: List<AudioFxPreset> = listOf(
        AudioFxPreset(AudioFxSettings.PRESET_FLAT, "Flat", bandsDb = zeros()),
        AudioFxPreset(
            id = "bass_boost",
            label = "Bass Boost",
            bandsDb = listOf(6f, 5f, 4f, 2f, 0f, 0f, 0f, 0f, 0f, 0f),
            bassBoostDb = 4f,
            preampDb = -2f,
        ),
        AudioFxPreset(
            id = "treble_boost",
            label = "Treble Boost",
            bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 1f, 2f, 4f, 5f, 6f),
            preampDb = -2f,
        ),
        AudioFxPreset(
            id = "vocal",
            label = "Vocal",
            bandsDb = listOf(-2f, -1f, 0f, 2f, 4f, 4f, 3f, 1f, 0f, -1f),
            preampDb = -1f,
        ),
        AudioFxPreset(
            id = "electronic",
            label = "Electronic",
            bandsDb = listOf(5f, 4f, 2f, 0f, -1f, 1f, 2f, 3f, 4f, 4f),
            bassBoostDb = 2f,
            preampDb = -2f,
        ),
        AudioFxPreset(
            id = "rock",
            label = "Rock",
            bandsDb = listOf(4f, 3f, 2f, 1f, 0f, 1f, 2f, 3f, 3f, 2f),
            bassBoostDb = 2f,
            preampDb = -2f,
        ),
        AudioFxPreset(
            id = "classical",
            label = "Classical",
            bandsDb = listOf(0f, 0f, 0f, 0f, 0f, 0f, -1f, -1f, -1f, -2f),
        ),
        AudioFxPreset(
            id = "acoustic",
            label = "Acoustic",
            bandsDb = listOf(3f, 2f, 1f, 0f, 1f, 2f, 3f, 2f, 1f, 0f),
            preampDb = -1f,
        ),
        AudioFxPreset(
            id = "podcast",
            label = "Spoken",
            bandsDb = listOf(-4f, -3f, -1f, 2f, 4f, 4f, 3f, 1f, -2f, -4f),
            preampDb = -1f,
        ),
    )

    fun byId(id: String): AudioFxPreset? = ALL.find { it.id == id }

    private fun zeros() = List(AudioFxSettings.BAND_COUNT) { 0f }
}
