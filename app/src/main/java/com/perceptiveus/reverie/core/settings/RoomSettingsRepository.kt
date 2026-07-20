package com.perceptiveus.reverie.core.settings

import com.perceptiveus.reverie.data.local.dao.UserSettingsDao
import com.perceptiveus.reverie.data.local.entity.UserSettingsEntity
import com.perceptiveus.reverie.data.local.mapper.toThemePreference
import com.perceptiveus.reverie.feature.tutorial.TutorialProgress
import com.perceptiveus.reverie.feature.tutorial.parseTutorialProgress
import com.perceptiveus.reverie.feature.tutorial.toJson as tutorialProgressToJson
import com.perceptiveus.reverie.playback.audiofx.AudioFxSettings
import com.perceptiveus.reverie.playback.audiofx.parseAudioFxSettings
import com.perceptiveus.reverie.playback.audiofx.toJson as audioFxSettingsToJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RoomSettingsRepository(
    private val userSettingsDao: UserSettingsDao,
    scope: CoroutineScope,
) : SettingsRepository {

    override val displayName: StateFlow<String> = userSettingsDao.observeSettings()
        .map { settings -> settings?.displayName ?: "Listener" }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), "Listener")

    override val themePreference: StateFlow<AppThemePreference> = userSettingsDao.observeSettings()
        .map { settings -> settings?.toThemePreference() ?: AppThemePreference.SYSTEM }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), AppThemePreference.SYSTEM)

    override val audioFxSettings: StateFlow<AudioFxSettings> = userSettingsDao.observeSettings()
        .map { settings -> parseAudioFxSettings(settings?.audioFxJson) }
        .stateIn(scope, SharingStarted.Eagerly, AudioFxSettings.Default)

    override val tutorialProgress: StateFlow<TutorialProgress> = userSettingsDao.observeSettings()
        .map { settings -> parseTutorialProgress(settings?.tutorialJson) }
        .stateIn(scope, SharingStarted.Eagerly, TutorialProgress.Default)

    override suspend fun setDisplayName(name: String) {
        val current = userSettingsDao.getSettings() ?: UserSettingsEntity()
        userSettingsDao.upsert(current.copy(displayName = name))
    }

    override suspend fun setThemePreference(preference: AppThemePreference) {
        val current = userSettingsDao.getSettings() ?: UserSettingsEntity()
        userSettingsDao.upsert(current.copy(themePreference = preference.name))
    }

    override suspend fun setAudioFxSettings(settings: AudioFxSettings) {
        val current = userSettingsDao.getSettings() ?: UserSettingsEntity()
        userSettingsDao.upsert(current.copy(audioFxJson = settings.audioFxSettingsToJson()))
    }

    override suspend fun setTutorialProgress(progress: TutorialProgress) {
        val current = userSettingsDao.getSettings() ?: UserSettingsEntity()
        userSettingsDao.upsert(current.copy(tutorialJson = progress.tutorialProgressToJson()))
    }
}
