package com.perceptiveus.reverie.feature.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.data.playlist.PlaylistCoverStore
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlaylistDetailViewModel(
    application: Application,
    private val playlistId: String,
    private val playlistRepository: PlaylistRepository,
    musicLibraryRepository: MusicLibraryRepository,
    private val playbackRepository: PlaybackRepository,
) : AndroidViewModel(application) {

    val playlist: StateFlow<Playlist?> = playlistRepository.observePlaylist(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tracks: StateFlow<List<Track>> = playlistRepository.observePlaylistTracks(playlistId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableTracks: StateFlow<List<Track>> =
        combine(musicLibraryRepository.songs, tracks) { all, inPlaylist ->
            val inPlaylistIds = inPlaylist.map { it.id }.toSet()
            all.filterNot { it.id in inPlaylistIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    private val _deleted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deleted: SharedFlow<Unit> = _deleted.asSharedFlow()

    fun playAll() {
        val all = tracks.value
        if (all.isEmpty()) return
        playbackRepository.play(all, 0)
    }

    fun playFrom(track: Track) {
        val all = tracks.value
        val index = all.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        if (all.isEmpty()) return
        playbackRepository.play(all, index)
    }

    fun addTrack(track: Track) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, track.id)
            _userMessages.emit("Added to playlist.")
        }
    }

    fun removeTrack(track: Track) {
        viewModelScope.launch {
            playlistRepository.removeTrackFromPlaylist(playlistId, track.id)
            _userMessages.emit("Removed from playlist.")
        }
    }

    fun saveDescription(description: String) {
        viewModelScope.launch {
            playlistRepository.updatePlaylist(id = playlistId, description = description)
                .onSuccess { _userMessages.emit("Description saved.") }
                .onFailure { _userMessages.emit(it.message ?: "Could not save description.") }
        }
    }

    fun importCover(uri: Uri) {
        viewModelScope.launch {
            try {
                val path = withContext(Dispatchers.IO) {
                    PlaylistCoverStore.importCover(getApplication(), playlistId, uri)
                }
                playlistRepository.updatePlaylist(id = playlistId, coverPath = path)
                    .onSuccess { _userMessages.emit("Cover updated.") }
                    .onFailure { _userMessages.emit(it.message ?: "Could not save cover.") }
            } catch (e: Exception) {
                _userMessages.emit(e.message ?: "Could not import cover.")
            }
        }
    }

    fun deletePlaylist() {
        viewModelScope.launch {
            playlistRepository.deletePlaylist(playlistId)
            _deleted.emit(Unit)
        }
    }

    companion object {
        fun factory(
            application: Application,
            playlistId: String,
            playlistRepository: PlaylistRepository,
            musicLibraryRepository: MusicLibraryRepository,
            playbackRepository: PlaybackRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return PlaylistDetailViewModel(
                    application = application,
                    playlistId = playlistId,
                    playlistRepository = playlistRepository,
                    musicLibraryRepository = musicLibraryRepository,
                    playbackRepository = playbackRepository,
                ) as T
            }
        }
    }
}
