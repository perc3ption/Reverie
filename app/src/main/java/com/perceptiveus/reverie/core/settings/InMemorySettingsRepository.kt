package com.perceptiveus.reverie.core.settings

import com.perceptiveus.reverie.feature.tutorial.TutorialProgress
import com.perceptiveus.reverie.playback.audiofx.AudioFxSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory settings (tests / previews). */
class InMemorySettingsRepository : SettingsRepository {

    private val _displayName = MutableStateFlow("Listener")
    override val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _themePreference = MutableStateFlow(AppThemePreference.SYSTEM)
    override val themePreference: StateFlow<AppThemePreference> = _themePreference.asStateFlow()

    private val _audioFxSettings = MutableStateFlow(AudioFxSettings.Default)
    override val audioFxSettings: StateFlow<AudioFxSettings> = _audioFxSettings.asStateFlow()

    private val _tutorialProgress = MutableStateFlow(TutorialProgress.Default)
    override val tutorialProgress: StateFlow<TutorialProgress> = _tutorialProgress.asStateFlow()

    override suspend fun setDisplayName(name: String) {
        _displayName.value = name
    }

    override suspend fun setThemePreference(preference: AppThemePreference) {
        _themePreference.value = preference
    }

    override suspend fun setAudioFxSettings(settings: AudioFxSettings) {
        _audioFxSettings.value = settings
    }

    override suspend fun setTutorialProgress(progress: TutorialProgress) {
        _tutorialProgress.value = progress
    }
}
