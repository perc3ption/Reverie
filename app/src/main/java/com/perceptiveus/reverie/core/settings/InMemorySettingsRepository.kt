package com.perceptiveus.reverie.core.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory settings until DataStore/Room persistence is added. */
class InMemorySettingsRepository : SettingsRepository {

    private val _displayName = MutableStateFlow("Listener")
    override val displayName: StateFlow<String> = _displayName.asStateFlow()

    private val _themePreference = MutableStateFlow(AppThemePreference.SYSTEM)
    override val themePreference: StateFlow<AppThemePreference> = _themePreference.asStateFlow()

    override suspend fun setDisplayName(name: String) {
        _displayName.value = name
    }

    override suspend fun setThemePreference(preference: AppThemePreference) {
        _themePreference.value = preference
    }
}
