package com.perceptiveus.reverie.core.settings

import com.perceptiveus.reverie.playback.audiofx.AudioFxSettings
import kotlinx.coroutines.flow.StateFlow

/**
 * User preferences persisted in Room [user_settings] table.
 */
interface SettingsRepository {
    val displayName: StateFlow<String>
    val themePreference: StateFlow<AppThemePreference>
    val audioFxSettings: StateFlow<AudioFxSettings>

    suspend fun setDisplayName(name: String)
    suspend fun setThemePreference(preference: AppThemePreference)
    suspend fun setAudioFxSettings(settings: AudioFxSettings)
}
