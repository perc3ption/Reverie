package com.perceptiveus.reverie.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.domain.model.PlaybackState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlayerViewModel(
    private val playbackRepository: PlaybackRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    fun togglePlayPause() = playbackRepository.togglePlayPause()
    fun skipToNext() = playbackRepository.skipToNext()
    fun skipToPrevious() = playbackRepository.skipToPrevious()
    fun toggleShuffle() = playbackRepository.toggleShuffle()
    fun cycleRepeatMode() = playbackRepository.cycleRepeatMode()

    fun canAccessAdvancedVisualizers(): Boolean =
        featureAccessChecker.canAccess(AppFeature.ADVANCED_VISUALIZERS)
}
