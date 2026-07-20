package com.perceptiveus.reverie.feature.audiofx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.core.settings.SettingsRepository
import com.perceptiveus.reverie.playback.audiofx.AudioFxPreset
import com.perceptiveus.reverie.playback.audiofx.AudioFxSettings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AudioFxViewModel(
    private val settingsRepository: SettingsRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val settings: StateFlow<AudioFxSettings> = settingsRepository.audioFxSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AudioFxSettings.Default)

    fun canAccess(): Boolean = featureAccessChecker.canAccess(AppFeature.AUDIO_FX)

    fun setEqEnabled(enabled: Boolean) = update { it.copy(eqEnabled = enabled) }

    fun setPreamp(db: Float) = update {
        it.copy(
            preampDb = db.coerceIn(AudioFxSettings.MIN_GAIN_DB, AudioFxSettings.MAX_GAIN_DB),
            presetId = AudioFxSettings.PRESET_CUSTOM,
        )
    }

    fun setBand(index: Int, db: Float) = update { it.withBand(index, db) }

    fun setBassBoost(db: Float) = update {
        it.copy(
            bassBoostDb = db.coerceIn(0f, AudioFxSettings.MAX_BASS_BOOST_DB),
            presetId = AudioFxSettings.PRESET_CUSTOM,
            eqEnabled = true,
        )
    }

    fun applyPreset(preset: AudioFxPreset) = update { it.withPreset(preset) }

    /** Resets EQ bands / preamp / bass boost to flat; keeps playback-feel toggles. */
    fun resetFlat() = update {
        it.copy(
            preampDb = 0f,
            bandsDb = List(AudioFxSettings.BAND_COUNT) { 0f },
            presetId = AudioFxSettings.PRESET_FLAT,
            bassBoostDb = 0f,
        )
    }

    /** Restores every Audio FX setting to factory defaults. */
    fun restoreDefaults() = update { AudioFxSettings.Default }

    fun setLoudnessEnabled(enabled: Boolean) = update { it.copy(loudnessEnabled = enabled) }

    fun setCrossfadeMs(ms: Int) = update {
        val crossfade = ms.coerceIn(0, AudioFxSettings.MAX_CROSSFADE_MS)
        it.copy(
            crossfadeMs = crossfade,
            // Crossfade replaces gapless feel when active.
            gaplessEnabled = if (crossfade > 0) false else it.gaplessEnabled,
        )
    }

    fun setGaplessEnabled(enabled: Boolean) = update {
        it.copy(
            gaplessEnabled = enabled,
            crossfadeMs = if (enabled) 0 else it.crossfadeMs,
        )
    }

    private fun update(transform: (AudioFxSettings) -> AudioFxSettings) {
        if (!canAccess()) return
        viewModelScope.launch {
            val next = transform(settingsRepository.audioFxSettings.value)
            settingsRepository.setAudioFxSettings(next)
        }
    }
}
