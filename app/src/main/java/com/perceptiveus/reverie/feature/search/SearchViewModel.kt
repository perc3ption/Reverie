package com.perceptiveus.reverie.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.perceptiveus.reverie.data.repository.MusicLibraryRepository
import com.perceptiveus.reverie.data.repository.PlaybackRepository
import com.perceptiveus.reverie.data.repository.PlaylistRepository
import com.perceptiveus.reverie.domain.model.Album
import com.perceptiveus.reverie.domain.model.Artist
import com.perceptiveus.reverie.domain.model.MusicFolder
import com.perceptiveus.reverie.domain.model.Playlist
import com.perceptiveus.reverie.domain.model.QueueSource
import com.perceptiveus.reverie.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SearchResults(
    /** Raw text-field value (not trimmed). */
    val query: String = "",
    val songs: List<Track> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val folders: List<MusicFolder> = emptyList(),
) {
    val isBlankQuery: Boolean get() = query.isBlank()
    val isEmpty: Boolean
        get() = songs.isEmpty() &&
            artists.isEmpty() &&
            albums.isEmpty() &&
            playlists.isEmpty() &&
            folders.isEmpty()
}

class SearchViewModel(
    musicLibraryRepository: MusicLibraryRepository,
    playlistRepository: PlaylistRepository,
    private val playbackRepository: PlaybackRepository,
) : ViewModel() {

    private val library = musicLibraryRepository
    private val _query = MutableStateFlow("")
    private val _userMessages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val userMessages: SharedFlow<String> = _userMessages.asSharedFlow()

    private val librarySnapshot = combine(
        _query,
        library.songs,
        library.artists,
        library.albums,
        library.folders,
    ) { query, songs, artists, albums, folders ->
        LibrarySnapshot(
            query = query,
            songs = songs,
            artists = artists,
            albums = albums,
            folders = folders,
        )
    }

    val results: StateFlow<SearchResults> = combine(
        librarySnapshot,
        playlistRepository.playlists,
    ) { snapshot, playlists ->
        filterLibrary(
            rawQuery = snapshot.query,
            songs = snapshot.songs,
            artists = snapshot.artists,
            albums = snapshot.albums,
            folders = snapshot.folders,
            playlists = playlists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchResults())

    fun setQuery(query: String) {
        _query.value = query
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun playSong(track: Track) {
        val allSongs = library.songs.value
        val index = allSongs.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playbackRepository.play(allSongs, index, QueueSource.Library)
    }

    fun playArtist(artist: Artist): Boolean {
        val queue = library.songs.value
            .filter { it.artist.equals(artist.name, ignoreCase = true) }
            .sortedBy { it.title.lowercase() }
        if (queue.isEmpty()) return false
        playbackRepository.play(
            queue,
            0,
            QueueSource.Artist(artist.name, artworkFrom(queue)),
        )
        return true
    }

    fun playAlbum(album: Album): Boolean {
        val queue = library.songs.value
            .filter {
                it.album.equals(album.title, ignoreCase = true) &&
                    it.artist.equals(album.artist, ignoreCase = true)
            }
            .sortedBy { it.title.lowercase() }
        if (queue.isEmpty()) return false
        playbackRepository.play(
            queue,
            0,
            QueueSource.Album(
                title = album.title,
                artist = album.artist,
                year = queue.map { it.year }.filter { it > 0 }.distinct().let { years ->
                    if (years.size == 1) years.first() else 0
                },
                artworkPath = artworkFrom(queue),
            ),
        )
        return true
    }

    fun playFolder(folder: MusicFolder): Boolean {
        val queue = library.songs.value
            .filter { it.folderId == folder.id }
            .sortedBy { it.title.lowercase() }
        if (queue.isEmpty()) return false
        playbackRepository.play(queue, 0, QueueSource.Folder(folder.name))
        return true
    }

    fun addToQueue(track: Track) {
        if (playbackRepository.addToQueue(listOf(track)) > 0) {
            viewModelScope.launch {
                _userMessages.emit("Added to queue")
            }
        }
    }

    private fun artworkFrom(tracks: List<Track>): String =
        tracks.firstOrNull { it.artworkPath.isNotBlank() }?.artworkPath.orEmpty()

    private data class LibrarySnapshot(
        val query: String,
        val songs: List<Track>,
        val artists: List<Artist>,
        val albums: List<Album>,
        val folders: List<MusicFolder>,
    )
}

internal fun filterLibrary(
    rawQuery: String,
    songs: List<Track>,
    artists: List<Artist>,
    albums: List<Album>,
    folders: List<MusicFolder>,
    playlists: List<Playlist>,
): SearchResults {
    val query = rawQuery.trim()
    if (query.isBlank()) {
        return SearchResults(query = rawQuery)
    }
    return SearchResults(
        query = rawQuery,
        songs = songs.filter { track ->
            track.title.contains(query, ignoreCase = true) ||
                track.artist.contains(query, ignoreCase = true) ||
                track.album.contains(query, ignoreCase = true) ||
                track.genre.contains(query, ignoreCase = true)
        },
        artists = artists.filter { it.name.contains(query, ignoreCase = true) },
        albums = albums.filter { album ->
            album.title.contains(query, ignoreCase = true) ||
                album.artist.contains(query, ignoreCase = true)
        },
        playlists = playlists.filter { playlist ->
            playlist.name.contains(query, ignoreCase = true) ||
                playlist.description.contains(query, ignoreCase = true)
        },
        folders = folders.filter { folder ->
            folder.relativePath.isNotEmpty() && (
                folder.name.contains(query, ignoreCase = true) ||
                    folder.relativePath.contains(query, ignoreCase = true)
            )
        },
    )
}
