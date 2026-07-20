package com.perceptiveus.reverie.playback.audiofx

import org.json.JSONObject

/**
 * Process-wide audio FX bridge: settings → ExoPlayer [EqualizerAudioProcessor].
 */
object AudioFxController {
    val equalizerProcessor = EqualizerAudioProcessor()

    @Volatile
    var settings: AudioFxSettings = AudioFxSettings.Default
        private set

    fun apply(settings: AudioFxSettings) {
        this.settings = settings
        equalizerProcessor.applySettings(settings)
    }
}

fun AudioFxSettings.toJson(): String = JSONObject().apply {
    put("eqEnabled", eqEnabled)
    put("preampDb", preampDb.toDouble())
    put("bandsDb", bandsDb.joinToString(","))
    put("presetId", presetId)
    put("bassBoostDb", bassBoostDb.toDouble())
    put("loudnessEnabled", loudnessEnabled)
    put("crossfadeMs", crossfadeMs)
    put("gaplessEnabled", gaplessEnabled)
}.toString()

fun parseAudioFxSettings(json: String?): AudioFxSettings {
    if (json.isNullOrBlank()) return AudioFxSettings.Default
    return try {
        val obj = JSONObject(json)
        val bandsRaw = obj.optString("bandsDb", "")
        val bands = if (bandsRaw.isBlank()) {
            List(AudioFxSettings.BAND_COUNT) { 0f }
        } else {
            bandsRaw.split(",")
                .mapNotNull { it.trim().toFloatOrNull() }
                .let { parsed ->
                    List(AudioFxSettings.BAND_COUNT) { i -> parsed.getOrElse(i) { 0f } }
                }
        }
        AudioFxSettings(
            eqEnabled = obj.optBoolean("eqEnabled", false),
            preampDb = obj.optDouble("preampDb", 0.0).toFloat()
                .coerceIn(AudioFxSettings.MIN_GAIN_DB, AudioFxSettings.MAX_GAIN_DB),
            bandsDb = bands.map {
                it.coerceIn(AudioFxSettings.MIN_GAIN_DB, AudioFxSettings.MAX_GAIN_DB)
            },
            presetId = obj.optString("presetId", AudioFxSettings.PRESET_FLAT),
            bassBoostDb = obj.optDouble("bassBoostDb", 0.0).toFloat()
                .coerceIn(0f, AudioFxSettings.MAX_BASS_BOOST_DB),
            loudnessEnabled = obj.optBoolean("loudnessEnabled", false),
            crossfadeMs = obj.optInt("crossfadeMs", 0)
                .coerceIn(0, AudioFxSettings.MAX_CROSSFADE_MS),
            gaplessEnabled = obj.optBoolean("gaplessEnabled", true),
        )
    } catch (_: Exception) {
        AudioFxSettings.Default
    }
}
