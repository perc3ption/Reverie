package com.perceptiveus.reverie.feature.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.LibraryStatsRepository
import com.perceptiveus.reverie.domain.model.LibraryStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryStatsUiState(
    val isLoading: Boolean = true,
    val stats: LibraryStats = LibraryStats(),
)

class LibraryStatsViewModel(
    private val libraryStatsRepository: LibraryStatsRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryStatsUiState())
    val uiState: StateFlow<LibraryStatsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun canAccessStats(): Boolean =
        featureAccessChecker.canAccess(AppFeature.LIBRARY_STATS)

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val stats = libraryStatsRepository.loadStats()
            _uiState.value = LibraryStatsUiState(isLoading = false, stats = stats)
        }
    }
}
