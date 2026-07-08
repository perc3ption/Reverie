package com.perceptiveus.reverie.core.settings

import kotlinx.coroutines.flow.StateFlow

/**
 * User preferences persisted in Room [user_settings] table.
 */
interface SettingsRepository {
    val displayName: StateFlow<String>
    val themePreference: StateFlow<AppThemePreference>

    suspend fun setDisplayName(name: String)
    suspend fun setThemePreference(preference: AppThemePreference)
}
