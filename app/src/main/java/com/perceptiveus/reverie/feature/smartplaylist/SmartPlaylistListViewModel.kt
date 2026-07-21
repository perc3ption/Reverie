package com.perceptiveus.reverie.feature.smartplaylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.SmartPlaylistAccessException
import com.perceptiveus.reverie.data.repository.SmartPlaylistRepository
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.SmartPlaylist
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartPlaylistListViewModel(
    private val smartPlaylistRepository: SmartPlaylistRepository,
    private val playbackRepository: PlaybackRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    val playlists: StateFlow<List<SmartPlaylist>> = smartPlaylistRepository.playlists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    fun canAccess(): Boolean = featureAccessChecker.canAccess(AppFeature.SMART_PLAYLISTS)

    fun play(playlist: SmartPlaylist, onStarted: () -> Unit = {}) {
        viewModelScope.launch {
            val tracks = runCatching {
                smartPlaylistRepository.evaluateTracks(playlist.id)
            }.getOrElse {
                _userMessages.emit("Could not play smart playlist.")
                return@launch
            }
            if (tracks.isEmpty()) {
                _userMessages.emit("No matching songs.")
                return@launch
            }
            playbackRepository.play(
                tracks,
                0,
                QueueSource.SmartPlaylist(name = playlist.name),
            )
            onStarted()
        }
    }

    fun delete(playlist: SmartPlaylist) {
        viewModelScope.launch {
            try {
                smartPlaylistRepository.deletePlaylist(playlist.id)
                _userMessages.emit("Deleted \"${playlist.name}\".")
            } catch (_: SmartPlaylistAccessException) {
                _userMessages.emit("Smart Playlists are a Premium feature.")
            }
        }
    }
}
