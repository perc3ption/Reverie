package com.perceptiveus.reverie.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    musicLibraryRepository: MusicLibraryRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val recentlyPlayed: StateFlow<List<Track>> = musicLibraryRepository.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun isPremium(): Boolean = featureAccessChecker.isPremium()
}
