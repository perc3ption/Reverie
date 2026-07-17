package com.perceptiveus.reverie.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.QueueSource
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

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    fun playTrack(track: Track) {
        val recent = recentlyPlayed.value
        val librarySongs = songs.value
        val usingRecent = recent.isNotEmpty()
        val queue = recent.ifEmpty { librarySongs }
        val playQueue = if (queue.any { it.id == track.id }) {
            queue
        } else {
            listOf(track) + queue
        }
        val index = playQueue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val source = if (usingRecent) QueueSource.RecentlyPlayed else QueueSource.Library
        playbackRepository.play(playQueue, index, source)
    }

    fun togglePlayPause() = playbackRepository.togglePlayPause()
    fun skipToNext() = playbackRepository.skipToNext()
    fun skipToPrevious() = playbackRepository.skipToPrevious()
    fun seekTo(positionMs: Long) = playbackRepository.seekTo(positionMs)

    fun isPremium(): Boolean = featureAccessChecker.isPremium()
}
