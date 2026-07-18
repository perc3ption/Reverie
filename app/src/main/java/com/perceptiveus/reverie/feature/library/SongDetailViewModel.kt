package com.perceptiveus.reverie.feature.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.core.entitlement.AppFeature
import com.perceptiveus.reverie.core.entitlement.FeatureAccessChecker
import com.perceptiveus.reverie.data.lyrics.LyricsImportResult
import com.perceptiveus.reverie.data.lyrics.LyricsLoader
import com.perceptiveus.reverie.data.lyrics.LyricsSidecarImporter
import com.perceptiveus.reverie.data.import.EditableTrackMetadata
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistLimitException
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.data.repository.SongTagRepository
import com.perceptiveus.reverie.data.repository.TagAccessException
import com.perceptiveus.reverie.domain.model.LyricsDocument
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.Tag
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SongDetailViewModel(
    application: Application,
    private val trackId: String,
    private val musicLibraryRepository: MusicLibraryRepository,
    private val playlistRepository: PlaylistRepository,
    private val songTagRepository: SongTagRepository,
    private val playbackRepository: PlaybackRepository,
    private val featureAccessChecker: FeatureAccessChecker,
) : AndroidViewModel(application) {

    private val songs: StateFlow<List<Track>> = musicLibraryRepository.songs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val track: StateFlow<Track?> = songs
        .map { list -> list.find { it.id == trackId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val tags: StateFlow<List<Tag>> = songTagRepository.observeTagsForTrack(trackId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availableTags: StateFlow<List<Tag>> =
        combine(songTagRepository.observeAllTags(), tags) { all, assigned ->
            val assignedIds = assigned.map { it.id }.toSet()
            all.filterNot { it.id in assignedIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playlistsContainingTrack: StateFlow<List<Playlist>> =
        playlistRepository.observePlaylistsForTrack(trackId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val availablePlaylists: StateFlow<List<Playlist>> =
        combine(playlistRepository.playlists, playlistsContainingTrack) { all, containing ->
            val containingIds = containing.map { it.id }.toSet()
            all.filterNot { it.id in containingIds }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lyrics = MutableStateFlow<LyricsDocument?>(null)
    val lyrics: StateFlow<LyricsDocument?> = _lyrics.asStateFlow()

    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    private val _metadataSaved = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val metadataSaved: SharedFlow<Unit> = _metadataSaved.asSharedFlow()

    private val _isSavingMetadata = MutableStateFlow(false)
    val isSavingMetadata: StateFlow<Boolean> = _isSavingMetadata.asStateFlow()

    init {
        viewModelScope.launch {
            track
                .map { it?.filePath.orEmpty() }
                .distinctUntilChanged()
                .collect { path ->
                    _lyrics.value = if (path.isBlank()) {
                        null
                    } else {
                        withContext(Dispatchers.IO) { LyricsLoader.loadForAudioFile(path) }
                    }
                }
        }
    }

    fun play() {
        val allSongs = songs.value
        val index = allSongs.indexOfFirst { it.id == trackId }.coerceAtLeast(0)
        if (allSongs.isEmpty()) return
        playbackRepository.play(allSongs, index, QueueSource.Library)
    }

    fun addToQueue() {
        val current = track.value ?: return
        playbackRepository.addToQueue(listOf(current))
        viewModelScope.launch {
            _userMessages.emit("Added to queue")
        }
    }

    fun saveMetadata(
        title: String,
        artist: String,
        album: String,
        yearText: String,
        genre: String,
    ) {
        if (_isSavingMetadata.value) return
        viewModelScope.launch {
            _isSavingMetadata.value = true
            val year = yearText.trim().toIntOrNull()?.takeIf { it in 1000..9999 } ?: 0
            val result = musicLibraryRepository.updateTrackMetadata(
                trackId = trackId,
                metadata = EditableTrackMetadata(
                    title = title,
                    artist = artist,
                    album = album,
                    year = year,
                    genre = genre,
                ),
            )
            _isSavingMetadata.value = false
            result
                .onSuccess {
                    _metadataSaved.emit(Unit)
                    _userMessages.emit("Metadata saved to file.")
                }
                .onFailure { error ->
                    _userMessages.emit(error.message ?: "Could not save metadata.")
                }
        }
    }

    fun canAccessTags(): Boolean = featureAccessChecker.canAccess(AppFeature.TAGS)

    fun canAccessLyrics(): Boolean = featureAccessChecker.canAccess(AppFeature.LYRICS)

    fun addTag(name: String) {
        viewModelScope.launch {
            songTagRepository.addTagToTrack(trackId, name)
                .onSuccess { _userMessages.emit("Tag added.") }
                .onFailure { error ->
                    _userMessages.emit(
                        when (error) {
                            TagAccessException -> "Tags are a Premium feature."
                            else -> error.message ?: "Could not add tag."
                        },
                    )
                }
        }
    }

    fun addExistingTag(tagId: String) {
        viewModelScope.launch {
            songTagRepository.addExistingTagToTrack(trackId, tagId)
                .onSuccess { _userMessages.emit("Tag added.") }
                .onFailure { error ->
                    _userMessages.emit(
                        when (error) {
                            TagAccessException -> "Tags are a Premium feature."
                            else -> error.message ?: "Could not add tag."
                        },
                    )
                }
        }
    }

    fun removeTag(tag: Tag) {
        viewModelScope.launch {
            songTagRepository.removeTagFromTrack(trackId, tag.id)
            _userMessages.emit("Tag removed.")
        }
    }

    fun addToPlaylist(playlistId: String) {
        viewModelScope.launch {
            playlistRepository.addTrackToPlaylist(playlistId, trackId)
            _userMessages.emit("Added to playlist.")
        }
    }

    fun createPlaylistAndAdd(name: String) {
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

    fun importLyrics(uri: Uri) {
        if (!canAccessLyrics()) {
            viewModelScope.launch {
                _userMessages.emit("Lyrics are a Premium feature.")
            }
            return
        }

        val audioPath = track.value?.filePath.orEmpty()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                LyricsSidecarImporter.importForTrack(
                    context = getApplication(),
                    audioPath = audioPath,
                    sourceUri = uri,
                )
            }
            when (result) {
                is LyricsImportResult.Success -> {
                    _lyrics.value = result.document
                    _userMessages.emit("Lyrics imported.")
                }
                is LyricsImportResult.Failure -> {
                    _userMessages.emit(result.message)
                }
            }
        }
    }

    companion object {
        fun factory(
            application: Application,
            trackId: String,
            musicLibraryRepository: MusicLibraryRepository,
            playlistRepository: PlaylistRepository,
            songTagRepository: SongTagRepository,
            playbackRepository: PlaybackRepository,
            featureAccessChecker: FeatureAccessChecker,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SongDetailViewModel(
                    application = application,
                    trackId = trackId,
                    musicLibraryRepository = musicLibraryRepository,
                    playlistRepository = playlistRepository,
                    songTagRepository = songTagRepository,
                    playbackRepository = playbackRepository,
                    featureAccessChecker = featureAccessChecker,
                ) as T
            }
        }
    }
}
