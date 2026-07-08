package com.perceptiveus.reverie.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

enum class LibraryTab {
    FOLDERS,
    ARTISTS,
    ALBUMS,
}

class LibraryViewModel(
    musicLibraryRepository: MusicLibraryRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val folders: StateFlow<List<MusicFolder>> = musicLibraryRepository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val artists: StateFlow<List<Artist>> = musicLibraryRepository.artists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val albums: StateFlow<List<Album>> = musicLibraryRepository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun canAccess(feature: AppFeature): Boolean = featureAccessChecker.canAccess(feature)

    fun isPremium(): Boolean = featureAccessChecker.isPremium()
}
