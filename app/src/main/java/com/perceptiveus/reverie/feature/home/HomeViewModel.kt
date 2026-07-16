package com.perceptiveus.reverie.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    musicLibraryRepository: MusicLibraryRepository,
    private val playbackRepository: PlaybackRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    private val library = musicLibraryRepository

    val recentlyPlayed: StateFlow<List<Track>> = library.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val songs: StateFlow<List<Track>> = library.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playTrack(track: Track) {
        val queue = recentlyPlayed.value.ifEmpty { songs.value }
        val playQueue = if (queue.any { it.id == track.id }) {
            queue
        } else {
            listOf(track) + queue
        }
        val index = playQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(playQueue, index)
    }

    fun isPremium(): Boolean = featureAccessChecker.isPremium()
}
