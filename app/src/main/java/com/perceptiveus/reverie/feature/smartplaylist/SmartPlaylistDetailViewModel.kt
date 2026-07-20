package com.perceptiveus.reverie.feature.smartplaylist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.SmartPlaylistAccessException
import com.perceptiveus.reverie.data.repository.SmartPlaylistRepository
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.SmartPlaylist
import com.perceptiveus.reverie.domain.model.SmartPlaylistRule
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SmartPlaylistDetailViewModel(
    private val playlistId: String,
    private val smartPlaylistRepository: SmartPlaylistRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    val playlist: StateFlow<SmartPlaylist?> =
        smartPlaylistRepository.observePlaylist(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val rules: StateFlow<List<SmartPlaylistRule>> =
        smartPlaylistRepository.observeRules(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    private val _deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleted: SharedFlow<Unit> = _deleted.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _tracks.value = smartPlaylistRepository.evaluateTracks(playlistId)
            } catch (_: SmartPlaylistAccessException) {
                _tracks.value = emptyList()
                _userMessages.emit("Smart Playlists are a Premium feature.")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun playAll() {
        val all = _tracks.value
        if (all.isEmpty()) {
            viewModelScope.launch { _userMessages.emit("No matching songs.") }
            return
        }
        playbackRepository.play(tracks = all, startIndex = 0, source = playlistSource())
    }

    fun playFrom(track: Track) {
        val all = _tracks.value
        if (all.isEmpty()) return
        val index = all.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(tracks = all, startIndex = index, source = playlistSource())
    }

    fun delete() {
        viewModelScope.launch {
            try {
                smartPlaylistRepository.deletePlaylist(playlistId)
                _deleted.emit(Unit)
            } catch (_: SmartPlaylistAccessException) {
                _userMessages.emit("Smart Playlists are a Premium feature.")
            }
        }
    }

    private fun playlistSource(): QueueSource.SmartPlaylist =
        QueueSource.SmartPlaylist(name = playlist.value?.name ?: "Smart Playlist")

    companion object {
        fun factory(
            playlistId: String,
            smartPlaylistRepository: SmartPlaylistRepository,
            playbackRepository: PlaybackRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SmartPlaylistDetailViewModel(
                    playlistId = playlistId,
                    smartPlaylistRepository = smartPlaylistRepository,
                    playbackRepository = playbackRepository,
                ) as T
            }
        }
    }
}
