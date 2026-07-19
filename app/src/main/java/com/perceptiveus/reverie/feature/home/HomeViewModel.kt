package com.perceptiveus.reverie.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistLimitException
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.domain.model.PlaybackState
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    musicLibraryRepository: MusicLibraryRepository,
    private val playbackRepository: PlaybackRepository,
    private val playlistRepository: PlaylistRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : ViewModel() {

    private val library = musicLibraryRepository

    val recentlyPlayed: StateFlow<List<Track>> = library.recentlyPlayed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val songs: StateFlow<List<Track>> = library.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playbackState: StateFlow<PlaybackState> = playbackRepository.playbackState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlaybackState())

    val availablePlaylists: StateFlow<List<Playlist>> =
        playbackState
            .map { it.currentTrack?.id }
            .distinctUntilChanged()
            .flatMapLatest { trackId ->
                if (trackId == null) {
                    flowOf(emptyList())
                } else {
                    combine(
                        playlistRepository.playlists,
                        playlistRepository.observePlaylistsForTrack(trackId),
                    ) { all, containing ->
                        val containingIds = containing.map { it.id }.toSet()
                        all.filterNot { it.id in containingIds }
                    }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

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
    fun playQueueIndex(index: Int) = playbackRepository.playQueueIndex(index)
    fun toggleQueueTrackEnabled(trackId: String) =
        playbackRepository.toggleQueueTrackEnabled(trackId)
    fun moveQueueItem(fromIndex: Int, toIndex: Int) =
        playbackRepository.moveQueueItem(fromIndex, toIndex)

    fun addCurrentTrackToPlaylist(playlistId: String) {
        val trackId = playbackState.value.currentTrack?.id ?: return
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, trackId)
            _userMessages.emit("Added to playlist.")
        }
    }

    fun createPlaylistAndAddCurrentTrack(name: String) {
        val trackId = playbackState.value.currentTrack?.id ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            viewModelScope.launch { _userMessages.emit("Playlist name cannot be empty.") }
            return
        }
        viewModelScope.launch {
            playlistRepository.createPlaylist(trimmed)
                .onSuccess { playlist ->
                    playlistRepository.addTrackToPlaylist(playlist.id, trackId)
                    _userMessages.emit("Playlist created.")
                }
                .onFailure { error ->
                    _userMessages.emit(
                        when (error) {
                            is PlaylistLimitException -> error.message ?: "Playlist limit reached."
                            else -> error.message ?: "Could not create playlist."
                        },
                    )
                }
        }
    }

    fun isPremium(): Boolean = featureAccessChecker.isPremium()
}
