package com.perceptiveus.reverie.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.EntitlementRepository
import com.perceptiveus.reverie.core.entitlement.Entitlements
import com.perceptiveus.reverie.core.settings.AppThemePreference
import com.perceptiveus.reverie.core.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val entitlementRepository: EntitlementRepository,
) : ViewModel() {

    val displayName: StateFlow<String> = settingsRepository.displayName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "Listener")

    val themePreference: StateFlow<AppThemePreference> = settingsRepository.themePreference
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppThemePreference.SYSTEM)

    val entitlements: StateFlow<Entitlements> = entitlementRepository.entitlements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Entitlements())

    fun setDisplayName(name: String) {
        viewModelScope.launch { settingsRepository.setDisplayName(name) }
    }

    fun setThemePreference(preference: AppThemePreference) {
        viewModelScope.launch { settingsRepository.setThemePreference(preference) }
    }

    fun restorePurchases(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            // TODO: Replace with Play Billing restorePurchases().
            val result = entitlementRepository.restorePurchases()
            onResult(result.getOrDefault(false))
        }
    }

    /** Dev helper to preview premium UI states. */
    fun togglePremiumForTesting() {
        viewModelScope.launch {
            val current = entitlements.value.isPremium
            entitlementRepository.setPremiumForTesting(!current)
        }
    }
}
