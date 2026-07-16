package com.perceptiveus.reverie.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class LibraryTab {
    SONGS,
    FOLDERS,
    ARTISTS,
    ALBUMS,
}

class LibraryViewModel(
    musicLibraryRepository: MusicLibraryRepository,
    private val playbackRepository: PlaybackRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val songs: StateFlow<List<Track>> = musicLibraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val folders: StateFlow<List<MusicFolder>> = musicLibraryRepository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = musicLibraryRepository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = musicLibraryRepository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun playSong(track: Track) {
        val allSongs = songs.value
        val index = allSongs.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(allSongs, index)
    }

    fun canAccess(feature: AppFeature): Boolean = featureAccessChecker.canAccess(feature)

    fun isPremium(): Boolean = featureAccessChecker.isPremium()
}
